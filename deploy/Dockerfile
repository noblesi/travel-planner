FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 withtrip \
    && useradd --system --uid 10001 --gid withtrip --home-dir /app --shell /usr/sbin/nologin withtrip

WORKDIR /app

RUN install -d --owner=withtrip --group=withtrip /app/uploads/profile

ARG JAR_FILE=backend/build/libs/travel-planner.jar
COPY --chown=withtrip:withtrip ${JAR_FILE} /app/travel-planner.jar

USER withtrip

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/travel-planner.jar"]
