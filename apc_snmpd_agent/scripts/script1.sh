#!/bin/bash
if [ "$1" = "-n" ]; then
  OID=$2
  case $OID in
    ".1.3.6.1.4.1.236.4.3.220.3.3.7536929.1.1.5")
      cat /opt/snmp-files/converted_apName.txt
      ;;
    *)
      echo "NONE"
      ;;
  esac
fi