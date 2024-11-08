#!/bin/bash
echo "$(date) - script2.sh called with command: $1 and OID: $2" >> /tmp/snmp_script2_debug.log

DATA_FILE="/opt/snmp-files/converted_mgntIpv4Addr.txt"

mapfile -t file_lines < "$DATA_FILE"
line_count=${#file_lines[@]}

while read -r command; do
    case "$command" in
        "PING")
            echo "PONG"
            ;;
        "-g" | "get")
            read -r oid
            if [[ "$oid" == ".1.3.6.1.4.1.236.4.3.220.3.3.7536930.1.1.13" ]]; then
                if (( line_count > 0 )); then
                    echo "$oid"
                    echo "${file_lines[0]}"
                else
                    echo "NONE"
                fi
            else
                echo "NONE"
            fi
            ;;
        "-n" | "getnext")
            read -r oid
            found_next=false
            for (( i=0; i<line_count; i++ )); do
                if $found_next; then
                    echo ".1.3.6.1.4.1.236.4.3.220.3.3.7536930.1.1.13.$((i+1))"
                    echo "${file_lines[$i]}"
                    break
                fi
                if [[ "$oid" == ".1.3.6.1.4.1.236.4.3.220.3.3.7536930.1.1.13.$((i+1))" ]]; then
                    found_next=true
                fi
            done
            if ! $found_next; then
                echo "NONE"
            fi
            ;;
        *)
            echo "NONE"
            ;;
    esac
done
