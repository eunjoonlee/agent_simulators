FROM ubuntu:latest

RUN apt-get update && \
    apt-get install -y openssh-server fping iproute2 iputils-ping && \
    mkdir /var/run/sshd

RUN useradd -m -d /home/DBsync -s /bin/false DBsync && \
    echo 'DBsync:Sync123!@#' | chpasswd

RUN mkdir -p /home/DBsync/hrx && \
    chown root:root /home/DBsync && \
    chmod 755 /home/DBsync && \
    mkdir -p /home/DBsync/hrx/data/sync_info && \
    chown DBsync:DBsync /home/DBsync/hrx/data/sync_info

RUN echo "Match User DBsync\n\
    ChrootDirectory /home/DBsync\n\
    ForceCommand internal-sftp\n\
    AllowTcpForwarding no\n\
    X11Forwarding no" >> /etc/ssh/sshd_config && \
    echo "LogLevel VERBOSE" >> /etc/ssh/sshd_config

COPY ping_responder.sh /ping_responder.sh
RUN chmod +x /ping_responder.sh

EXPOSE 22

CMD ["/bin/bash", "-c", "/ping_responder.sh & /usr/sbin/sshd -D"]
