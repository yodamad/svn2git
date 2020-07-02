FROM openjdk:11-jre-slim

ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    JHIPSTER_SLEEP=0 \
    JAVA_OPTS=""

RUN apt update && \
    apt install -y git git-svn subversion

COPY target/svn2git.jar /usr/svn2git/

WORKDIR /usr/svn2git

EXPOSE 8080

VOLUME /tmp/svn2git

CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "svn2git.jar"]
