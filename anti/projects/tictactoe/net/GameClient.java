package anti.projects.tictactoe.net;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import anti.projects.tictactoe.game.GameState;
import anti.projects.tictactoe.gui.BoardEventListener;

public class GameClient implements BoardEventListener {
  
  private String host;
  private int port;
  
  private Socket cli;
  private String playerUuid = null;
  private char side = '-';
  
  private GameState game;
  
  private volatile boolean awaitingResponse = false;
  
  public GameClient(String host, int port) {
    cli = new Socket();
    this.host = host;
    this.port = port;
    game = new GameState(3);
  }
  
  public GameState getGameState() {
    return game;
  }
  
  public void handshake() throws IOException {
    cli.connect(new InetSocketAddress(host, port));
    
    InputStream is = cli.getInputStream();
    StringBuilder playerId = new StringBuilder();
    while (playerId.length() < 32) {
      while (is.available() > 0 && playerId.length() < 32) {
        playerId.append((char)is.read());
      }
    }
    
    playerUuid = playerId.toString();
    System.out.println("Player UUID: " + playerUuid);
    
    while (!(is.available() > 0));
    side = (char)is.read();
    
    System.out.println("Side: " + side);
    
    OutputStream os = cli.getOutputStream();
    os.write("ok".getBytes());
    os.flush();
  }
  
  public char getSide() {
    return side;
  }
  
  public void poll() throws IOException {
    InputStream is = cli.getInputStream();
    
    while (game.getWinner() == '-' && !game.boardIsFull()) {
      
      if (is.available() == 0) continue;
      
      char turn = (char)is.read();
      int x = is.read();
      int y = is.read();
      
      if (turn == '-') {
        // close signal
        System.out.println("Received close signal!");
        break;
      }
      
      awaitingResponse = false;
      
      game.move(turn, x, y);
    }
    
    if (game.getWinner() != '-') {
      System.out.printf("%c wins!\n", game.getWinner());
      GameState.printBoard(game);
    }
    else if (game.boardIsFull()) System.out.println("Cat's game :/");
    else {
      // game was killed before completion
      System.exit(1);
    }
  }
  
  public void prematureExit() throws IOException {
    cli.getOutputStream().write(new byte[] { (byte)'-', Common.MSG_END });
    cli.getOutputStream().flush();
    cli.close();
  }

  @Override
  public void onCellClicked(Point cell) {
    if (game.getTurn() == side) {
      // send move to server
      byte[] message = Arrays.copyOf(playerUuid.getBytes(), Game.REQUEST_LENGTH + 1);
      message[Game.X_INDEX] = (byte)cell.x;
      message[Game.Y_INDEX] = (byte)cell.y;
      message[Game.Y_INDEX + 1] = Common.MSG_END;
      
      try {
        cli.getOutputStream().write(message);
        cli.getOutputStream().flush();
      } catch (IOException ioe) {
        System.err.println("Could not send move!");
      }
    }
  }
  
  public void awaitResponse() {
    awaitingResponse = true;
    while (awaitingResponse) {
      try {
        Thread.sleep(25);
      } catch (InterruptedException ie) {}
    }
  }
  
  private ArrayList<Runnable> onMain = new ArrayList<>();
  public void executeOnMainThread(Runnable r) {
    synchronized(onMain) {
      onMain.add(r);
    }
  }
  
  public void runAllPending() {
    synchronized(onMain) {
      for (int i = 0; i < onMain.size(); i++) {
        onMain.get(0).run();
        onMain.remove(0);
      }
    }
  }
  
  public static void main(String[] args) throws IOException {
    GameClient cli = new GameClient(Common.HOST, Common.PORT);
    cli.handshake();
    Thread t = new Thread(() -> {
      try {
        cli.poll();
      } catch (IOException ioe) {
        System.err.println("poll machine broke :(");
        System.exit(1);
      }
    });
    t.start();
    
    Scanner s = new Scanner(System.in);

    while (cli.game.getWinner() == '-' && !cli.game.boardIsFull()) {
      cli.runAllPending();
      if (cli.game.getWinner() != '-' || cli.game.boardIsFull()) break;
      if (cli.game.getTurn() == cli.side) { 
        GameState.printBoard(cli.game);
        System.out.print("x > ");
        int x = s.nextInt();
        System.out.print("y > ");
        int y = s.nextInt();
        cli.onCellClicked(new Point(x, y));
        cli.awaitResponse();
      } else {
        try {
          Thread.sleep(25);
        } catch (InterruptedException e) {}
      }
    }
    
    s.close();
  }

}
