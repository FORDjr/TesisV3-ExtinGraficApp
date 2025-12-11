FROM eclipse-temurin:17-jre
WORKDIR /app
# Copia el JAR shadow generado (server.jar)
COPY build/libs/server.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
