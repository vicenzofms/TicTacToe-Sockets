package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor iniciado na porta " + port + ".");
            System.out.println("Aguardando jogadores...");

            while (true) {
                Socket playerX = serverSocket.accept();
                System.out.println("Jogador X conectado: " + playerX.getRemoteSocketAddress());

                Socket playerO = serverSocket.accept();
                System.out.println("Jogador O conectado: " + playerO.getRemoteSocketAddress());

                GameSession session = new GameSession(playerX, playerO);
                session.start();
                System.out.println("Nova partida iniciada.");
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Porta inválida. Use um número inteiro.");
        }
    }
}
