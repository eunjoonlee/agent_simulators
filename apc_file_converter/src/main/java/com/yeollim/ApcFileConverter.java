package com.yeollim;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ApcFileConverter {

    public static void main(String[] args) {
        try {
            convertAndSaveFile("apc_apName.txt", "converted_apName.txt", "1.3.6.1.4.1.236.4.3.220.3.3.7536929.1.1.5", "OctetString");
            convertAndSaveFile("apc_mgntIpv4Addr.txt", "converted_mgntIpv4Addr.txt", "1.3.6.1.4.1.236.4.3.220.3.3.7536930.1.1.13", "IpAddress");
            convertAndSaveFile("apc_activeAp.txt", "converted_activeAp.txt", "1.3.6.1.4.1.236.4.3.220.3.3.7536930.1.1.24", "Gauge32");

            System.out.println("Data conversion completed successfully for all files.");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Data conversion failed: " + e.getMessage());
        }
    }

    private static void convertAndSaveFile(String inputFilePath, String outputFilePath, String baseOid, String dataType) throws IOException {
        List<String> convertedData = convertSnmpData(inputFilePath, baseOid, dataType);
        saveConvertedData(outputFilePath, convertedData);
    }

    private static List<String> convertSnmpData(String filePath, String baseOid, String dataType) throws IOException {
        List<String> convertedData = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int index = 1; // OID의 인덱스를 증가시키기 위한 변수
            StringBuilder currentData = new StringBuilder();
            String currentOid = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // 새로운 OID가 시작되었는지 확인
                if (line.startsWith("SNMPv2-SMI::")) {
                    // 현재 OID가 존재하고 누적된 데이터가 있다면 저장
                    if (currentOid != null && currentData.length() > 0) {
                        String processedData = processDataForType(dataType, currentData.toString().trim());

                        String typeToWrite = dataType.equals("OctetString") ? "STRING" : dataType;
                        convertedData.add(currentOid + " = " + typeToWrite + ": " + processedData);

                        currentData.setLength(0); // 데이터 초기화
                    }

                    // 새로운 OID와 데이터 시작
                    int indexOfEquals = line.indexOf(" = ");
                    if (indexOfEquals > -1) {
                        currentOid = baseOid + "." + index; // baseOid에 인덱스를 붙여 OID 생성
                        index++; // 인덱스 증가
                        currentData.append(line.substring(indexOfEquals + 3).trim()); // 데이터 부분 추가
                    }
                } else {
                    // 기존 OID의 데이터가 여러 줄로 나뉘어 있는 경우 이어서 추가
                    currentData.append(" ").append(line);
                }
            }

            // 마지막으로 누적된 데이터를 저장
            if (currentOid != null && currentData.length() > 0) {
                String processedData = processDataForType(dataType, currentData.toString().trim());
                convertedData.add(currentOid + " = " + dataType + ": " + processedData);
            }
        }

        return convertedData;
    }

    // 데이터 타입별로 불필요한 부분을 제거하고 정리하는 메서드
    private static String processDataForType(String dataType, String data) {
        if (dataType.equals("OctetString")) {
            // HEX 값을 UTF-8 문자열로 변환
            String hexString = data.replace("Hex-STRING: ", "").replace("STRING: ", "").trim();
            return processApName(hexString);
        } else if (dataType.equals("IpAddress")) {
            return data.replace("IpAddress: ", "").trim();
        } else if (dataType.equals("Gauge32")) {
            return data.replace("Gauge32: ", "").trim();
        }
        return data;
    }

    private static String processApName(String apName) {
        // AP 이름이 16진수(hex) 형식일 경우 (스트링 형식으로 오는 경우도 있음)
        if (apName.matches("([0-9A-Fa-f]{2}[:\\s])+[0-9A-Fa-f]{2}")) {
            return hexStringToStringUptoParen(apName);
            //return hexStringToString(apName);
        } else {
            return apName;
        }
    }

    private static String hexStringToStringUptoParen(String hexString) {
        // 41:50:5F:53 ... or 41 50 5F 53 55 5F 30 30 32 5F 30 34 46 5F 30 32
        hexString = hexString.replace(":", "").replaceAll("\\s", "");

        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hexString.substring(i, i + 2), 16);
        }

        try {
            String result = new String(bytes, "UTF-8");
            // '(' 기호가 있으면 이후 버림
//            int bracketIndex = result.indexOf('(');
//            if (bracketIndex != -1) {
//                result = result.substring(0, bracketIndex);
//            }
            return result;
        } catch (UnsupportedEncodingException e) {
            String result = new String(bytes, java.nio.charset.StandardCharsets.US_ASCII);
            // ASCII 에서도 '(' 기호 이후 버림
            int bracketIndex = result.indexOf('(');
            if (bracketIndex != -1) {
                result = result.substring(0, bracketIndex);
            }
            return result;
        }
    }
    // HEX 문자열을 UTF-8 문자열로 변환하는 메서드
    private static String hexToUtf8String(String hexStr) {
        byte[] bytes = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hexStr.substring(i, i + 2), 16);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void saveConvertedData(String outputFilePath, List<String> convertedData) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            for (String line : convertedData) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
}
