FROM openjdk:21-jdk-slim

WORKDIR /app

RUN apt-get update && apt-get install -y wget && \
    wget https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz && \
    tar -xvzf apache-maven-3.9.9-bin.tar.gz && \
    mv apache-maven-3.9.9 /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/bin/mvn

COPY . .


RUN mvn clean install -DskipTests

RUN cp target/*.jar /app/app.jar

EXPOSE 8000

CMD ["java", "-jar", "app.jar"]
