FROM ubuntu:latest

# 설치 명령만 수행하고 설치 오류가 발생하는 경우 바로 확인
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    iputils-ping \
    snmp \
    snmpd \
    lsof \
    net-tools \
    dnsutils \
    tcpdump \
    iproute2 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Optional: snmp-mibs-downloader 설치 분리, 오류 무시
RUN apt-get update && apt-get install -y snmp-mibs-downloader || echo "Failed to install snmp-mibs-downloader"

COPY snmpd.conf /etc/snmp/snmpd.conf

EXPOSE 161/udp

CMD ["/usr/sbin/snmpd", "-f", "-Lo"]
