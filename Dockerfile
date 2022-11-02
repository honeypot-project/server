FROM openjdk:11-jre-slim
EXPOSE 8080
VOLUME /utility
ADD build/libs/*fat.jar utility.jar
CMD ["java", "-jar", "/utility.jar"]
