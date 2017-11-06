# progressed.io Docker image
# VERSION 1.0

FROM java:8

MAINTAINER Fehmi Can Saglam, fehmican.saglam@gmail.com

RUN mkdir /root/bin

WORKDIR /root/bin

RUN \
  wget https://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/0.13.16/sbt-launch.jar && \
  echo 'SBT_OPTS="-Xms256M -Xmx384M -Xss1M -XX:+CMSClassUnloadingEnabled"' > sbt && \
  echo 'java $SBT_OPTS -jar `dirname $0`/sbt-launch.jar "$@"' >> sbt && \
  chmod u+x sbt

ADD . /app/source

WORKDIR /app/source

RUN /root/bin/sbt assembly

# copy the locally built fat-jar to the image
RUN cp target/scala-2.12/progressed.io.jar /app/progressed.io.jar

# run the (java) server as the daemon user
USER daemon

# run the server when a container based on this image is being run
ENTRYPOINT [ "java", "-jar", "/app/progressed.io.jar" ]

# the server binds to 8080 - expose that port
EXPOSE 8080

