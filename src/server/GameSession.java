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
      position = Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      player.send(Protocol.command(Protocol.ERROR, "Jogada invalida. Use um numero de 0 a 8."));
      return;
    }

    if (!board.makeMove(position, player.symbol)) {
      player.send(Protocol.command(Protocol.ERROR, "Posição invalida ou ocupada."));
      sendState();
      return;
    }

    if (board.hasWinner(player.symbol)) {
      finished = true;
      sendBoard();
      player.send(Protocol.command(Protocol.STATUS, Protocol.WIN));
      opponentOf(player).send(Protocol.command(Protocol.STATUS, Protocol.LOSE));
      broadcast(Protocol.command(Protocol.MESSAGE, "Jogador " + player.symbol + " venceu."));
      closeSockets();
      return;
    }

    if (board.isFull()) {
      finished = true;
      sendBoard();
      broadcast(Protocol.command(Protocol.STATUS, Protocol.DRAW));
      broadcast(Protocol.command(Protocol.MESSAGE, "Empate."));
      closeSockets();
      return;
    }

    currentTurn = currentTurn == 'X' ? 'O' : 'X';
    sendState();
  }

  private synchronized void handleDisconnect(PlayerHandler player) {
    if (finished) {
      return;
    }

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
          } else {
            send(Protocol.command(Protocol.ERROR, "Comando desconhecido."));
          }
        }
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
