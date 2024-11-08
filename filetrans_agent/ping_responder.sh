#!/bin/sh

while true; do
    fping -a -I eth0 >/dev/null 2>&1
    sleep 1
done
