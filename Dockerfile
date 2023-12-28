FROM sbtscala/scala-sbt:graalvm-community-21.0.1_1.9.8_2.13.12 AS builder

WORKDIR /code
COPY . /code
RUN sbt stage


FROM ghcr.io/graalvm/jdk-community:21
COPY --from=builder /code/target/universal/stage/ /app
EXPOSE 80
ENTRYPOINT ["/app/bin/bus-info-bot"]