package server;

import common.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GameSession {
  private final PlayerHandler playerX;
  private final PlayerHandler playerO;
  private final Board board = new Board();
  private char currentTurn = 'X';
  private boolean finished = false;
  private boolean closing = false;
  private Boolean rematchX;
  private Boolean rematchO;

  public GameSession(Socket socketX, Socket socketO) throws IOException {
    this.playerX = new PlayerHandler(socketX, 'X');
    this.playerO = new PlayerHandler(socketO, 'O');
  }

  public void start() {
    playerX.send(Protocol.command(Protocol.ASSIGN, "X"));
    playerO.send(Protocol.command(Protocol.ASSIGN, "O"));
    broadcast(Protocol.command(Protocol.MESSAGE, "Partida iniciada. Jogador X começa."));
    sendState();

    new Thread(playerX, "player-x-handler").start();
    new Thread(playerO, "player-o-handler").start();
  }

  private synchronized void handleMove(PlayerHandler player, String value) {
    if (finished) {
      return;
    }

    if (player.symbol != currentTurn) {
      player.send(Protocol.command(Protocol.ERROR, "Espere sua vez!"));
      sendState();
      return;
    }

    int position;
    try {
      position = Integer.parseInt(value.trim()) - 1;
    } catch (NumberFormatException e) {
      player.send(Protocol.command(Protocol.ERROR, "Jogada invalida. Use um numero de 1 a 9."));
      return;
    }

    if (!board.makeMove(position, player.symbol)) {
      player.send(Protocol.command(Protocol.ERROR, "Posição invalida ou ocupada. Use uma posição de 1 a 9."));
      sendState();
      return;
    }

    if (board.hasWinner(player.symbol)) {
      finished = true;
      sendBoard();
      player.send(Protocol.command(Protocol.STATUS, Protocol.WIN));
      opponentOf(player).send(Protocol.command(Protocol.STATUS, Protocol.LOSE));
      broadcast(Protocol.command(Protocol.MESSAGE, "Jogador " + player.symbol + " venceu."));
      requestRematch();
      return;
    }

    if (board.isFull()) {
      finished = true;
      sendBoard();
      broadcast(Protocol.command(Protocol.STATUS, Protocol.DRAW));
      requestRematch();
      return;
    }

    currentTurn = currentTurn == 'X' ? 'O' : 'X';
    sendState();
  }

  private synchronized void handleDisconnect(PlayerHandler player) {
    if (closing) {
      return;
    }

    closing = true;
    finished = true;
    PlayerHandler opponent = opponentOf(player);
    opponent.send(Protocol.command(Protocol.STATUS, Protocol.OPPONENT_LEFT));
    opponent.send(Protocol.command(Protocol.MESSAGE, "O outro jogador desconectou."));
    closeSockets();
  }

  private void sendState() {
    sendBoard();
    playerX.send(Protocol.command(Protocol.STATUS, currentTurn == 'X' ? Protocol.YOUR_TURN : Protocol.WAIT));
    playerO.send(Protocol.command(Protocol.STATUS, currentTurn == 'O' ? Protocol.YOUR_TURN : Protocol.WAIT));
  }

  private void sendBoard() {
    broadcast(Protocol.command(Protocol.BOARD, board.serialize()));
  }

  private synchronized void requestRematch() {
    rematchX = null;
    rematchO = null;
    broadcast(Protocol.command(Protocol.STATUS, Protocol.REMATCH_REQUEST));
  }

  private synchronized void handleRematch(PlayerHandler player, String value) {
    if (!finished) {
      player.send(Protocol.command(Protocol.ERROR, "A partida ainda nao terminou."));
      return;
    }

    boolean accepted = isAffirmative(value);
    if (player.symbol == 'X') {
      rematchX = accepted;
    } else {
      rematchO = accepted;
    }

    player.send(Protocol.command(Protocol.MESSAGE, accepted ? "Voce aceitou a revanche." : "Voce recusou a revanche."));

    if (rematchX == null || rematchO == null) {
      player.send(Protocol.command(Protocol.MESSAGE, "Aguardando resposta do outro jogador..."));
      return;
    }

    if (rematchX && rematchO) {
      board.reset();
      currentTurn = 'X';
      finished = false;
      rematchX = null;
      rematchO = null;
      broadcast(Protocol.command(Protocol.MESSAGE, "Revanche iniciada. Jogador X começa."));
      sendState();
      return;
    }

    closing = true;
    broadcast(Protocol.command(Protocol.MESSAGE, "Revanche recusada. Partida encerrada."));
    closeSockets();
  }

  private boolean isAffirmative(String value) {
    String answer = value.trim().toLowerCase();
    return answer.equals("s") || answer.equals("sim") || answer.equals("y") || answer.equals("yes");
  }

  private void broadcast(String message) {
    playerX.send(message);
    playerO.send(message);
  }

  private PlayerHandler opponentOf(PlayerHandler player) {
    return player == playerX ? playerO : playerX;
  }

  private void closeSockets() {
    playerX.close();
    playerO.close();
  }

  private class PlayerHandler implements Runnable {
    private final Socket socket;
    private final char symbol;
    private final BufferedReader in;
    private final PrintWriter out;

    private PlayerHandler(Socket socket, char symbol) throws IOException {
      this.socket = socket;
      this.symbol = symbol;
      this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
      try {
        String line;
        while ((line = in.readLine()) != null) {
          if (line.equals(Protocol.QUIT)) {
            handleDisconnect(this);
            return;
          }

          if (line.startsWith(Protocol.MOVE + Protocol.SEPARATOR)) {
            handleMove(this, line.substring((Protocol.MOVE + Protocol.SEPARATOR).length()));
          } else if (line.startsWith(Protocol.REMATCH + Protocol.SEPARATOR)) {
            handleRematch(this, line.substring((Protocol.REMATCH + Protocol.SEPARATOR).length()));
          } else {
            send(Protocol.command(Protocol.ERROR, "Comando desconhecido."));
          }
        }
        handleDisconnect(this);
      } catch (IOException e) {
        handleDisconnect(this);
      } finally {
        close();
      }
    }

    private void send(String message) {
      out.println(message);
    }

    private void close() {
      try {
        socket.close();
      } catch (IOException ignored) {
      }
    }
  }
}
