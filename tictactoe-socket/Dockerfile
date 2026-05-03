FROM eclipse-temurin:21-jdk

WORKDIR /app
COPY src ./src

RUN mkdir -p out \
    && javac -d out src/common/Protocol.java src/server/Board.java src/server/GameSession.java src/server/Server.java src/client/Client.java

EXPOSE 5000

CMD ["java", "-cp", "out", "server.Server", "5000"]
