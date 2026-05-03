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

  private static final String RESET  = "\033[0m";
  private static final String BOLD   = "\033[1m";
  private static final String DIM    = "\033[2m";
  private static final String RED    = "\033[1;31m";
  private static final String GREEN  = "\033[1;32m";
  private static final String YELLOW = "\033[1;33m";
  private static final String BLUE   = "\033[1;34m";
  private static final String CYAN   = "\033[1;36m";
  private static final String GREY   = "\033[2;37m";

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

      System.out.println(CYAN + "Conectado ao servidor " + host + ":" + port + "." + RESET);
      System.out.println(DIM + "Digite uma posicao de 1 a 9 quando for sua vez, ou 'sair' para encerrar." + RESET);

      String line;
      while ((line = serverIn.readLine()) != null) {
        boolean shouldContinue = handleServerMessage(line, keyboard, out);
        if (!shouldContinue) {
          break;
        }
      }
    } catch (IOException e) {
      System.err.println(RED + "Erro no cliente: " + e.getMessage() + RESET);
    } catch (NumberFormatException e) {
      System.err.println(RED + "Porta invalida. Use um numero inteiro." + RESET);
    }
  }

  private boolean handleServerMessage(String line, BufferedReader keyboard, PrintWriter out) throws IOException {
    if (line.startsWith(Protocol.ASSIGN + Protocol.SEPARATOR)) {
      String symbol = valueOf(line);
      String colored = symbol.equals("X") ? RED + "X" + RESET : BLUE + "O" + RESET;
      System.out.println("Voce e o jogador " + colored + ".");
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
      System.out.println(RED + "Erro: " + valueOf(line) + RESET);
      return true;
    }

    System.out.println(line);
    return true;
  }

  private boolean handleStatus(String status, BufferedReader keyboard, PrintWriter out) throws IOException {
    switch (status) {
      case Protocol.YOUR_TURN:
        System.out.print(GREEN + "Sua vez. Escolha uma posicao (1-9): " + RESET);
        String input = keyboard.readLine();
        if (input == null || input.trim().equalsIgnoreCase("sair") || input.trim().equalsIgnoreCase("quit")) {
          out.println(Protocol.QUIT);
          return false;
        }
        out.println(Protocol.command(Protocol.MOVE, input.trim()));
        return true;
      case Protocol.WAIT:
        System.out.println(YELLOW + "Aguardando jogada do outro jogador..." + RESET);
        return true;
      case Protocol.WIN:
        System.out.println(GREEN + BOLD + "Você venceu!" + RESET);
        return true;
      case Protocol.LOSE:
        System.out.println(RED + "Você perdeu." + RESET);
        return true;
      case Protocol.DRAW:
        System.out.println(YELLOW + "Empate!" + RESET);
        return true;
      case Protocol.REMATCH_REQUEST:
        System.out.print(CYAN + "Deseja jogar novamente? (s/n): " + RESET);
        String rematchInput = keyboard.readLine();
        out.println(Protocol.command(Protocol.REMATCH, isAffirmative(rematchInput) ? "sim" : "nao"));
        return true;
      case Protocol.OPPONENT_LEFT:
        System.out.println(YELLOW + "O outro jogador desconectou. Partida encerrada." + RESET);
        return false;
      default:
        System.out.println("Status: " + status);
        return true;
    }
  }

  private boolean isAffirmative(String value) {
    if (value == null) {
      return false;
    }

    String answer = value.trim().toLowerCase();
    return answer.equals("s") || answer.equals("sim") || answer.equals("y") || answer.equals("yes");
  }

  private String valueOf(String line) {
    int separatorIndex = line.indexOf(Protocol.SEPARATOR);
    return separatorIndex >= 0 ? line.substring(separatorIndex + 1) : "";
  }

  private void printBoard(String board) {
    System.out.print("\033[H\033[2J");
    System.out.flush();

    for (int row = 0; row < 3; row++) {
      int base = row * 3;
      System.out.println(" " + cell(board, base) + DIM + " | " + RESET + cell(board, base + 1) + DIM + " | " + RESET + cell(board, base + 2));
      if (row < 2) {
        System.out.println(DIM + "---+---+---" + RESET);
      }
    }
    System.out.println();
  }

  private String cell(String board, int index) {
    if (index >= board.length() || board.charAt(index) == '_') {
      return GREY + (index + 1) + RESET;
    }

    char c = board.charAt(index);
    if (c == 'X') {
      return RED + c + RESET;
    } else if (c == 'O') {
      return BLUE + c + RESET;
    }
    return String.valueOf(c);
  }
}
