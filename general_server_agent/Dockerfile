FROM ubuntu:latest

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

# Optional: Separate snmp-mibs-downloader installation, with error handling
RUN apt-get update && apt-get install -y snmp-mibs-downloader || echo "Failed to install snmp-mibs-downloader"

COPY snmpd.conf /etc/snmp/snmpd.conf

EXPOSE 161/udp

CMD ["/usr/sbin/snmpd", "-f", "-Lo"]
