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
    iproute2 \
    iptables && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/ScmeTrapSender.jar /app/ScmeTrapSender.jar
COPY trapsend.dat /app/data/trapsend.dat
COPY snmpd.conf /etc/snmp/snmpd.conf

EXPOSE 161/udp

CMD /usr/sbin/snmpd -f -Lo -C -c /etc/snmp/snmpd.conf & exec java -jar /app/ScmeTrapSender.jar
