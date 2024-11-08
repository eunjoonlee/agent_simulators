#!/bin/bash
# 초기 디버그 로그
echo "$(date) - script1.sh started" >> /tmp/snmp_script1_debug.log

# 데이터 파일 경로 및 데이터 로드
DATA_FILE="/opt/snmp-files/converted_apName.txt"
if [[ -f "$DATA_FILE" ]]; then
    mapfile -t file_lines < "$DATA_FILE"
    line_count=${#file_lines[@]}
    echo "$(date) - Loaded $line_count lines from $DATA_FILE" >> /tmp/snmp_script1_debug.log
else
    echo "$(date) - Data file $DATA_FILE not found" >> /tmp/snmp_script1_debug.log
    exit 1
fi

# 무한 루프 시작 (pass_persist에서 필요)
while read -r command; do
    echo "$(date) - Received command: $command" >> /tmp/snmp_script1_debug.log
    case "$command" in
        "PING")
            echo "PONG"
            echo "$(date) - Responded to PING with PONG" >> /tmp/snmp_script1_debug.log
            ;;
        "-g" | "get")
            read -r oid
            echo "$(date) - Handling GET request for OID: $oid" >> /tmp/snmp_script1_debug.log
            if [[ "$oid" == ".1.3.6.1.4.1.236.4.3.220.3.3.7536929.1.1.5" ]]; then
                if (( line_count > 0 )); then
                    echo "$oid"
                    echo "${file_lines[0]}"
                    echo "$(date) - Responded to GET request with OID: $oid and value: ${file_lines[0]}" >> /tmp/snmp_script1_debug.log
                else
                    echo "NONE"
                    echo "$(date) - No data available, responded with NONE" >> /tmp/snmp_script1_debug.log
                fi
            else
                echo "NONE"
                echo "$(date) - OID $oid does not match, responded with NONE" >> /tmp/snmp_script1_debug.log
            fi
            ;;
        "-n" | "getnext")
            read -r oid
            echo "$(date) - Handling GETNEXT request for OID: $oid" >> /tmp/snmp_script1_debug.log
            found_next=false
            for (( i=0; i<line_count; i++ )); do
                current_oid=".1.3.6.1.4.1.236.4.3.220.3.3.7536929.1.1.5.$((i+1))"
                echo "$(date) - Comparing requested OID: $oid with current OID: $current_oid" >> /tmp/snmp_script1_debug.log
                if $found_next; then
                    echo "$current_oid"
                    echo "${file_lines[$i]}"
                    echo "$(date) - Found next OID: $current_oid with value: ${file_lines[$i]}" >> /tmp/snmp_script1_debug.log
                    break
                fi
                if [[ "$oid" == "$current_oid" ]]; then
                    found_next=true
                    echo "$(date) - Match found for OID: $oid, looking for next" >> /tmp/snmp_script1_debug.log
                fi
            done
            if ! $found_next; then
                echo "NONE"
                echo "$(date) - No next OID found, responded with NONE" >> /tmp/snmp_script1_debug.log
            fi
            ;;
        *)
            echo "NONE"
            echo "$(date) - Unknown command: $command, responded with NONE" >> /tmp/snmp_script1_debug.log
            ;;
    esac
done
