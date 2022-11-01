FROM openjdk:11-jre-slim
EXPOSE 8888
VOLUME /app
ADD build/libs/*fat.jar app.jar
CMD ["java", "-jar", "/app.jar"]
