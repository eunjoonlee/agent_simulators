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

COPY snmpd.conf /etc/snmp/snmpd.conf

EXPOSE 161/udp

COPY scripts /opt/snmp-scripts
RUN chmod +x /opt/snmp-scripts/*

CMD ["/usr/sbin/snmpd", "-f", "-Le"]

