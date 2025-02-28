# Użyj obrazu z OpenJDK
FROM openjdk:21-jdk

WORKDIR /app

COPY . .

EXPOSE 8080

COPY target/*.jar app.jar

CMD ["java", "-jar", "app.jar"]