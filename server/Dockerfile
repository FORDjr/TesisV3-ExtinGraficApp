FROM eclipse-temurin:17-jre
WORKDIR /app
COPY build/libs/server-all.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
