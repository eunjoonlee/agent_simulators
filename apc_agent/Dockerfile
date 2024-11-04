FROM ubuntu:latest

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    openjdk-8-jre \
    iputils-ping \
    snmp \
    snmpd \
    snmp-mibs-downloader \
    lsof \
    net-tools \
    dnsutils \
    tcpdump \
    iproute2 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/ApcAgent.jar /app/ApcAgent.jar
COPY converted_apName.txt /app/data/converted_apName.txt
COPY converted_mgntIpv4Addr.txt /app/data/converted_mgntIpv4Addr.txt
COPY converted_activeAp.txt /app/data/converted_activeAp.txt
COPY snmpd.conf /etc/snmp/snmpd.conf

EXPOSE 161/udp 1161/udp

CMD /usr/sbin/snmpd -f -Lo -C -c /etc/snmp/snmpd.conf & exec java -jar /app/ApcAgent.jar
