package com.yeollim;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.mp.StateReference;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

public class ApcAgent {

    private static final String COMMUNITY = "public";
    private static final String AGENT_IP = "0.0.0.0";
    private static final int AGENT_PORT = 161;  // 외부 리슨
    private static final int SNMPD_PORT = 1161;  // net-snmp 내부 포트

    private static final String BASE_OID_NAME   = "1.3.6.1.4.1.236.4.3.220.3.3.7536929.1.1.5";
    private static final String BASE_OID_IP     = "1.3.6.1.4.1.236.4.3.220.3.3.7536930.1.1.13";
    private static final String BASE_OID_STATUS = "1.3.6.1.4.1.236.4.3.220.3.3.7536930.1.1.24";

    private static final String NAME_FILE_PATH   = getFilePath("converted_apName.txt");
    private static final String IP_FILE_PATH     = getFilePath("converted_mgntIpv4Addr.txt");
    private static final String STATUS_FILE_PATH = getFilePath("converted_activeAp.txt");

    private Map<String, Variable> names;
    private Map<String, Variable> ips;
    private Map<String, Variable> statuses;
    private Snmp snmpdSnmp;

    public ApcAgent() {
        try {
            names = readFile(NAME_FILE_PATH, BASE_OID_NAME);
            ips = readFile(IP_FILE_PATH, BASE_OID_IP);
            statuses = readFile(STATUS_FILE_PATH, BASE_OID_STATUS);

            TransportMapping<UdpAddress> snmpdTransport = new DefaultUdpTransportMapping();
            snmpdTransport.listen();
            snmpdSnmp = new Snmp(snmpdTransport);
        } catch (IOException e) {
            System.err.println("Error loading AP data or initializing snmpdSnmp: " + e.getMessage());
        }
    }


    private static String getFilePath(String fileName) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return Paths.get(System.getProperty("user.dir"), fileName).toString();
        } else {
            return "/app/data/" + fileName;
        }
    }

    public void startAgent() throws IOException {
        TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping(new UdpAddress(AGENT_IP + "/" + AGENT_PORT));
        Snmp snmp = new Snmp(transport);
        snmp.getMessageDispatcher().addMessageProcessingModel(new MPv2c());
        transport.listen();
        System.out.println("Proxy SNMP Agent is listening on " + AGENT_IP + ":" + AGENT_PORT);

        snmp.addCommandResponder((event) -> {
            System.out.println("SNMP Request received");

            try {
                PDU requestPDU = event.getPDU();
                if (requestPDU != null) {
                    PDU responsePDU = new PDU();
                    responsePDU.setType(PDU.RESPONSE);
                    responsePDU.setErrorStatus(PDU.noError);
                    responsePDU.setErrorIndex(0);

                    String requestedOid = requestPDU.getVariableBindings().get(0).getOid().toString();
                    VariableBinding vb = null;

                    if (requestPDU.getType() == PDU.GET) {
                        vb = getVariableBindingForExactOid(requestedOid);
                        if (vb == null) {
                            // 기본 OID가 아닌 경우 snmpd로 전달
                            vb = forwardToSnmpd(requestedOid, PDU.GET);
                        }
                    } else if (requestPDU.getType() == PDU.GETNEXT) {
                        vb = getNextVariableBinding(requestedOid);
                        if (vb == null) {
                            // 기본 OID가 아닌 경우 snmpd로 전달
                            vb = forwardToSnmpd(requestedOid, PDU.GETNEXT);
                        }
                    }

                    if (vb != null) {
                        responsePDU.add(vb);
                    } else {
                        responsePDU.add(new VariableBinding(new OID(requestedOid), new Null()));
                    }

                    StatusInformation statusInformation = new StatusInformation();
                    StateReference ref = event.getStateReference();
                    snmp.getMessageDispatcher().returnResponsePdu(
                            event.getMessageProcessingModel(),
                            event.getSecurityModel(),
                            event.getSecurityName(),
                            event.getSecurityLevel(),
                            responsePDU,
                            event.getMaxSizeResponsePDU(),
                            ref,
                            statusInformation);

                    System.out.println("Response sent successfully with status information: " + statusInformation);
                }
            } catch (IOException e) {
                System.err.println("Exception in response: " + e.getMessage());
            }
        });

        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            System.err.println("Agent interrupted: " + e.getMessage());
        } finally {
            snmp.close();
            transport.close();
            System.out.println("SNMP Agent stopped.");
        }
    }



    private List<VariableBinding> getResponseVariableBindings(String baseOid, List<String> dataList) {
        List<VariableBinding> variableBindings = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i++) {
            String currentOid = baseOid + "." + (i + 1);
            VariableBinding vb = null;

            try {
                if (baseOid.equals(BASE_OID_NAME)) {
                    String hexString = dataList.get(i).replace("Hex-STRING: ", "").trim();
                    System.out.println("Processing Hex-STRING for OID " + currentOid + ": " + hexString); // 디버깅 로그
                    vb = new VariableBinding(new OID(currentOid), OctetString.fromHexString(hexString));
                } else if (baseOid.equals(BASE_OID_IP)) {
                    String ipAddress = dataList.get(i).trim();
                    System.out.println("Processing IP address for OID " + currentOid + ": " + ipAddress); // 디버깅 로그
                    vb = new VariableBinding(new OID(currentOid), new IpAddress(ipAddress));
                } else if (baseOid.equals(BASE_OID_STATUS)) {
                    int status = Integer.parseInt(dataList.get(i).trim());
                    System.out.println("Processing status for OID " + currentOid + ": " + status); // 디버깅 로그
                    vb = new VariableBinding(new OID(currentOid), new Gauge32(status));
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid Hex-STRING format for OID " + currentOid + ": " + dataList.get(i));
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                System.err.println("Error creating VariableBinding for OID " + currentOid + ": " + e.getMessage());
                e.printStackTrace();
            }

            // 유효한 VariableBinding만 리스트에 추가
            if (vb != null) {
                variableBindings.add(vb);
            }
        }
        return variableBindings;
    }


    private VariableBinding getNextVariableBinding(String requestedOid) {
        TreeMap<String, Variable> targetMap = null;

        // 요청된 OID가 어느 범위에 속하는지 확인하여 해당 맵 선택
        if (requestedOid.startsWith(BASE_OID_NAME)) {
            targetMap = new TreeMap<>(names);
        } else if (requestedOid.startsWith(BASE_OID_IP)) {
            targetMap = new TreeMap<>(ips);
        } else if (requestedOid.startsWith(BASE_OID_STATUS)) {
            targetMap = new TreeMap<>(statuses);
        } else {
            System.err.println("GETNEXT request: No valid data source for requested OID: " + requestedOid);
            return null;
        }

        // 요청된 OID보다 큰 첫 번째 OID를 찾기
        String nextOid = targetMap.higherKey(requestedOid);

        if (nextOid != null) {
            Variable nextValue = targetMap.get(nextOid);
            System.out.println("GETNEXT request: Found next OID " + nextOid + " with value: " + nextValue);
            return new VariableBinding(new OID(nextOid), nextValue);
        } else {
            System.err.println("GETNEXT request: No valid next OID found for requested OID: " + requestedOid);
            return null;
        }
    }

    private VariableBinding getVariableBindingForExactOid(String requestedOid) {
        System.out.println("Requested OID for GET: " + requestedOid);

        if (names.containsKey(requestedOid)) {
            return new VariableBinding(new OID(requestedOid), names.get(requestedOid));
        } else if (ips.containsKey(requestedOid)) {
            return new VariableBinding(new OID(requestedOid), ips.get(requestedOid));
        } else if (statuses.containsKey(requestedOid)) {
            return new VariableBinding(new OID(requestedOid), statuses.get(requestedOid));
        } else {
            System.err.println("OID not found locally, forwarding to snmpd: " + requestedOid);
            return null;
        }
    }


    private VariableBinding forwardToSnmpd(String requestedOid, int pduType) {
        try {
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(COMMUNITY));
            target.setVersion(SnmpConstants.version2c);
            target.setAddress(new UdpAddress("localhost/" + SNMPD_PORT));
            target.setRetries(2);
            target.setTimeout(1500);

            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(requestedOid)));
            pdu.setType(pduType);

            ResponseEvent responseEvent = snmpdSnmp.send(pdu, target);
            PDU responsePDU = responseEvent.getResponse();

            if (responsePDU != null && responsePDU.size() > 0) {
                return responsePDU.get(0);
            } else {
                System.err.println("No response from snmpd for OID: " + requestedOid);
            }
        } catch (Exception e) {
            System.err.println("Error forwarding OID to snmpd: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void close() throws IOException {
        if (snmpdSnmp != null) {
            snmpdSnmp.close();
        }
    }

    private int extractIndex(String oid, String baseOid) {
        try {
            String indexStr = oid.substring(baseOid.length() + 1);
            return Integer.parseInt(indexStr) - 1;
        } catch (Exception e) {
            return -1;
        }
    }


    private Map<String, Variable> readFile(String filePath, String baseOid) throws IOException {
        Map<String, Variable> dataMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                int splitIndex = line.indexOf("=");
                if (splitIndex != -1) {
                    String oid = line.substring(0, splitIndex).trim();
                    String valueStr = line.substring(splitIndex + 1).trim();

                    Variable value;
                    if (baseOid.equals(BASE_OID_NAME)) {
                        value = new OctetString(valueStr.replace("STRING: ", "").trim());
                    } else if (baseOid.equals(BASE_OID_IP)) {
                        value = new IpAddress(valueStr.replace("IpAddress: ", "").trim());
                    } else if (baseOid.equals(BASE_OID_STATUS)) {
                        value = new Gauge32(Integer.parseInt(valueStr.replace("Gauge32: ", "").trim()));
                    } else {
                        value = new Null();
                    }

                    dataMap.put(oid, value);
                }
            }
        }
        return dataMap;
    }



    public static void main(String[] args) {
        ApcAgent agent = new ApcAgent();
        try {
            agent.startAgent();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                agent.close();  // 종료 시 자원 해제
            } catch (IOException e) {
                System.err.println("Failed to close SNMP resources: " + e.getMessage());
            }
        }
    }
}
