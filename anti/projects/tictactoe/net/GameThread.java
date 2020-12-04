package anti.projects.tictactoe.net;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class GameThread extends Thread {
  
  private static int threadNo = 1;
  
  private Socket px;
  private Socket po;
  private AsyncBufferedReader pxin;
  private AsyncBufferedReader poin;
  
  private Game game;
  
  private boolean alive = true;
  private boolean shouldRun = true;
  
  private final int thisThreadNo = GameThread.threadNo++;
  
  public GameThread(Socket px, Socket po, Game game) throws IOException {
    this.px = px;
    this.po = po;
    this.pxin = new AsyncBufferedReader(px.getInputStream(), Common.MSG_END);
    this.poin = new AsyncBufferedReader(po.getInputStream(), Common.MSG_END);
    this.game = game;
  }
  
  public Game getGame() {
    return game;
  }
  
  public boolean shouldCleanup() {
    return !alive;
  }
  
  public void kill() {
    shouldRun = false;
  }
  
  @Override
  public void run() {
    OutputStream pxos;
    OutputStream poos;
    try {
      pxos = px.getOutputStream();
      poos = po.getOutputStream();
    } catch (IOException ioe) {
      System.err.println("Could not get output streams! Ending thread.");
      return;
    }
    
    while (game.isAlive() && shouldRun) {
      // structure of requests is this: 32 byte UUID (no hyphens), 1 byte x coordinate, 1 byte y coordinate
      // structure of responses is 1 byte status code (0 for success, 1 for failure), 4 bytes message length
      // (aka 'n'), n bytes message data
      try {
        byte[] cur = null;
        
        ArrayList<ClientMessage> msgs = new ArrayList<>();
        while ((cur = pxin.read()) != null) {
          msgs.add(processMove(cur));
        }
        while ((cur = poin.read()) != null) {
          msgs.add(processMove(cur));
        }
        
        for (ClientMessage msg : msgs) {
          if (msg == ClientMessage.CLOSE) {
            shouldRun = false;
            break; // break before sending the message as it will be sent in the cleanup phase.
          }
          broadcast(msg);
        }
      } catch(IOException ioe) {
        // TODO - use logger from GameServer
        System.err.println("An I/O exception occurred in a game, aborting");
        // trigger cleanup
        shouldRun = false;
      }
    }
    
    try {
      pxos.write(ClientMessage.CLOSE.getBytes());
      pxos.flush();
    } catch (IOException ioe) {
      System.err.println("Error in closing player X's socket :/");
    }
    
    try {
      poos.write(ClientMessage.CLOSE.getBytes());
      poos.flush();
    } catch (IOException ioe) {
      System.err.println("Error in closing player O's socket :/");
    }
    
    alive = false;
  }
  
  private void broadcast(ClientMessage msg) throws IOException {
    if (msg == null) return;
    byte[] msgdata = msg.getBytes();
    px.getOutputStream().write(msgdata);
    px.getOutputStream().flush();
    po.getOutputStream().write(msgdata);
    po.getOutputStream().flush();
  }
  
  private ClientMessage processMove(byte[] request) {
    // check if request is the close message
    if (request.length > 0 && (char)request[0] == '-') {
      return ClientMessage.CLOSE;
    }
    
    if (request.length != Game.REQUEST_LENGTH) {
      return null;
    } else {
      String uuid = NetUtil.fromBytes(request, 0, Game.UUID_LENGTH);
      char player = game.getPlayer(uuid);
      if (player == '-') {
        return null;
      } else {
        if (game.isTurn(player)) {
          int x = request[Game.X_INDEX];
          int y = request[Game.Y_INDEX];
          boolean moved = game.onMoveReceived(player, x, y);
          if (moved) {
            return new ClientMessage(player, x, y);
          } else {
            return null;
          }
        } else {
          return null;
        }
      }
    }
  }
  
  @Override
  public String toString() {
    return String.format("Game #%d", thisThreadNo);
  }
}
