package com.yeollim;

import org.snmp4j.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.mp.SnmpConstants;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ScmeTrapSender {

    private static String filePath;
    private static long interval;
    private static String targetIp = "127.0.0.1";
    private static int targetPort = 11162;
    private static String community = "Office";

    public static void main(String[] args) {
        // 환경변수에서 파일 경로, interval, target IP, port 값을 읽음
        String filePathEnv = System.getenv("TRAP_FILE_PATH");
        String intervalEnv = System.getenv("TRAP_INTERVAL");
        String ipEnv = System.getenv("TARGET_IP");
        String portEnv = System.getenv("TARGET_PORT");

        // 파일 경로 설정
        if (filePathEnv != null) {
            filePath = filePathEnv;
        } else {
            // 환경 변수가 설정되지 않은 경우, 운영 환경에 따라 기본 경로를 설정
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                filePath = "trapsend.dat"; // 윈도우: 프로젝트 루트에서 파일 검색
            } else {
                filePath = "/app/data/trapsend.dat"; // Docker (Linux): /app/data 디렉터리에서 파일 검색
            }
        }

        // interval 환경 변수 처리
        if (intervalEnv != null) {
            try {
                interval = Long.parseLong(intervalEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid TRAP_INTERVAL value. Using default interval of 5 seconds.");
                interval = 5; // 기본값 5초
            }
        } else {
            System.out.println("TRAP_INTERVAL environment variable not set. Using default interval of 5 seconds.");
            interval = 5; // 기본값 5초
        }

        // target IP 환경 변수 처리
        if (ipEnv != null) {
            targetIp = ipEnv;
        } else {
            System.out.println("TARGET_IP environment variable not set. Using default target IP of localhost.");
            targetIp = "127.0.0.1"; // 기본값 localhost
        }

        // target Port 환경 변수 처리
        if (portEnv != null) {
            try {
                targetPort = Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid TARGET_PORT value. Using default port 11162.");
                targetPort = 11162; // 기본값 포트 11162
            }
        } else {
            System.out.println("TARGET_PORT environment variable not set. Using default port 11162.");
            targetPort = 11162; // 기본값 포트 11162
        }

        try {
            List<String[]> trapData = loadTrapData();
            sendTraps(trapData);
        } catch (InterruptedException | IOException e) {
            System.err.println("Error in sending traps: " + e.getMessage());
        }
    }

    // 파일 내용을 메모리에 로드하는 메서드 (콤마 구분자로 파싱)
    private static List<String[]> loadTrapData() throws IOException {
        List<String[]> trapData = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2); // 콤마로 분리
                if (parts.length < 2) {
                    System.err.println("Invalid line format: " + line);
                    continue;
                }
                trapData.add(new String[] { parts[0].trim(), parts[1].trim() });
            }
        }
        return trapData;
    }

    // 메모리에 로드된 데이터를 일정 간격으로 전송
    private static void sendTraps(List<String[]> trapData) throws InterruptedException {
        for (String[] trap : trapData) {
            String code = trap[0];
            String message = trap[1];
            sendTrap(code, message);
            TimeUnit.SECONDS.sleep(interval);
        }
    }

    private static void sendTrap(String code, String message) {
        try {
            Address targetAddress = new UdpAddress(targetIp + "/" + targetPort);
            TransportMapping<?> transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(community));
            target.setVersion(SnmpConstants.version2c);
            target.setAddress(targetAddress);
            target.setRetries(2);
            target.setTimeout(1500);

            PDU pdu = new PDU();
            pdu.setType(PDU.TRAP);

            pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.1.3.0"), new OctetString("16 days, 16:51:31.92")));
            pdu.add(new VariableBinding(new OID("1.3.6.1.6.3.1.1.4.1.0"), new OID("1.3.6.1.4.1.14008.27300.2.8.2.5.4")));
            pdu.add(new VariableBinding(new OID("1.3.6.1.4.1.14008.27300.2.8.1.1.1.0"), new OctetString("Not-Registered")));
            pdu.add(new VariableBinding(new OID("1.3.6.1.4.1.14008.27300.1.1.2.0"), new OctetString("c4:cb:e1:cd:a5:96")));
            pdu.add(new VariableBinding(new OID("1.3.6.1.4.1.14008.27300.1.1.7.2.0"), new IpAddress("172.16.0.20")));
            pdu.add(new VariableBinding(new OID("1.3.6.1.4.1.14008.27300.2.8.2.3.1.0"), new OctetString(code)));
            pdu.add(new VariableBinding(new OID("1.3.6.1.4.1.14008.27300.2.8.2.3.2.0"), new Integer32(1)));
            pdu.add(new VariableBinding(new OID("1.3.6.1.4.1.14008.27300.2.8.2.3.3.0"), new OctetString(message)));
            pdu.add(new VariableBinding(new OID("1.3.6.1.4.1.14008.27300.2.8.2.3.4.0"), new Integer32(0)));

            System.out.println("Sending Trap to " + targetIp + ":" + targetPort);
            for (VariableBinding vb : pdu.getVariableBindings()) {
                System.out.println("OID: " + vb.getOid().toString() + ", Value: " + vb.getVariable().toString());
            }

            snmp.send(pdu, target);
            System.out.println("Trap sent successfully.");
            snmp.close();
        } catch (Exception e) {
            System.out.println("Error sending trap: " + e.getMessage());
        }
    }

}
