FROM openjdk:17-slim
WORKDIR /app
COPY ChatServer.java ClientHandler.java index.html ./
RUN javac ChatServer.java ClientHandler.java
EXPOSE 8080
CMD ["java", "ChatServer"]