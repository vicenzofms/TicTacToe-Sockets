package client;

import common.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        new Client().start(host, port);
    }

    private void start(String host, int port) {
        try (Socket socket = new Socket(host, port);
             BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Conectado ao servidor " + host + ":" + port + ".");
            System.out.println("Digite uma posição de 0 a 8 quando for sua vez, ou 'sair' para encerrar.");

            String line;
            while ((line = serverIn.readLine()) != null) {
                boolean shouldContinue = handleServerMessage(line, keyboard, out);
                if (!shouldContinue) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Erro no cliente: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Porta inválida. Use um número inteiro.");
        }
    }

    private boolean handleServerMessage(String line, BufferedReader keyboard, PrintWriter out) throws IOException {
        if (line.startsWith(Protocol.ASSIGN + Protocol.SEPARATOR)) {
            System.out.println("Você é o jogador " + valueOf(line) + ".");
            return true;
        }

        if (line.startsWith(Protocol.BOARD + Protocol.SEPARATOR)) {
            printBoard(valueOf(line));
            return true;
        }

        if (line.startsWith(Protocol.STATUS + Protocol.SEPARATOR)) {
            return handleStatus(valueOf(line), keyboard, out);
        }

        if (line.startsWith(Protocol.MESSAGE + Protocol.SEPARATOR)) {
            System.out.println(valueOf(line));
            return true;
        }

        if (line.startsWith(Protocol.ERROR + Protocol.SEPARATOR)) {
            System.out.println("Erro: " + valueOf(line));
            return true;
        }

        System.out.println(line);
        return true;
    }

    private boolean handleStatus(String status, BufferedReader keyboard, PrintWriter out) throws IOException {
        switch (status) {
            case Protocol.YOUR_TURN:
                System.out.print("Sua vez. Escolha uma posição (0-8): ");
                String input = keyboard.readLine();
                if (input == null || input.trim().equalsIgnoreCase("sair") || input.trim().equalsIgnoreCase("quit")) {
                    out.println(Protocol.QUIT);
                    return false;
                }
                out.println(Protocol.command(Protocol.MOVE, input.trim()));
                return true;
            case Protocol.WAIT:
                System.out.println("Aguardando jogada do outro jogador...");
                return true;
            case Protocol.WIN:
                System.out.println("Você venceu!");
                return false;
            case Protocol.LOSE:
                System.out.println("Você perdeu.");
                return false;
            case Protocol.DRAW:
                System.out.println("Empate.");
                return false;
            case Protocol.OPPONENT_LEFT:
                System.out.println("O outro jogador desconectou. Partida encerrada.");
                return false;
            default:
                System.out.println("Status: " + status);
                return true;
        }
    }

    private String valueOf(String line) {
        int separatorIndex = line.indexOf(Protocol.SEPARATOR);
        return separatorIndex >= 0 ? line.substring(separatorIndex + 1) : "";
    }

    private void printBoard(String board) {
        System.out.println();
        for (int row = 0; row < 3; row++) {
            int base = row * 3;
            System.out.println(" " + cell(board, base) + " | " + cell(board, base + 1) + " | " + cell(board, base + 2));
            if (row < 2) {
                System.out.println("---+---+---");
            }
        }
        System.out.println();
    }

    private String cell(String board, int index) {
        if (index >= board.length() || board.charAt(index) == '_') {
            return String.valueOf(index);
        }

        return String.valueOf(board.charAt(index));
    }
}
