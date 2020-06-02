FROM openjdk:11-jre-slim

ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    JHIPSTER_SLEEP=0 \
    JAVA_OPTS=""

RUN apt update && \
    apt install -y git git-svn subversion

COPY target/svn-2-git-1.18.2.jar /usr/svn2git/

WORKDIR /usr/svn2git

EXPOSE 8080

CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "svn-2-git-1.18.2.jar"]
