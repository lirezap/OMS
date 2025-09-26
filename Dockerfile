FROM ghcr.io/graalvm/jdk-community:25

RUN microdnf install dnf && \
    dnf install -y lz4 lz4-devel && \
    dnf clean all

RUN mkdir /opt/app
WORKDIR /opt/app

COPY target/oms-*.jar oms.jar

CMD ["java", "-jar", "-XX:+UseCompactObjectHeaders", "oms.jar"]
