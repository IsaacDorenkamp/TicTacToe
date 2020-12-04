package anti.projects.tictactoe.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;

public class GameServer {
  
  public static final int DEFAULT_GAME_LIMIT = 5;
  
  private ServerSocket server;
  private final String host;
  private final int port;
  private volatile boolean running = true;
  
  private ArrayList<GameThread> threads;
  
  private Game populating = null;
  private Socket populatingSocket = null;
  
  private Logger logger = Logger.getLogger(Common.LOGGER);
  
  private Runnable onStop = null;
  private Consumer<Exception> onException = (exc) -> exc.printStackTrace();
  
  private final DefaultListModel<GameThread> gameListModel = new DefaultListModel<GameThread>();
  
  private final int gameLimit;
  
  public GameServer(String host, int port, int gameLimit) throws IOException {
    server = new ServerSocket();
    server.setSoTimeout(25);
    this.host = host;
    this.port = port;
    this.gameLimit = gameLimit;
    
    threads = new ArrayList<>();
    
    logger.setUseParentHandlers(false);
  }
  
  public Logger getLogger() {
    return logger;
  }
  
  public void stopServer(Runnable cbk) {
    assert running : "Cannot stop server, it is not running!";
    running = false;
    onStop = cbk;
  }
  
  public void setErrorCbk(Consumer<Exception> excCbk) {
    synchronized (onException) {
      onException = excCbk;
    }
  }
  
  public ListModel<GameThread> getGameListModel() {
    return gameListModel;
  }
  
  public void run() {
    
    try {
      server.bind(new InetSocketAddress(host, port));
    } catch (IOException ioe) {
      onException.accept(ioe);
      return;
    }
    
    logger.info(String.format("Listening on %s:%d.\n", host, port));
    
    while (running) {
      // check which games need to be cleaned up
      synchronized (threads) {
        @SuppressWarnings("unchecked")
        ArrayList<GameThread> checkClean = (ArrayList<GameThread>)threads.clone();
        for (GameThread thr : checkClean) {
          if (thr.shouldCleanup()) {
            System.out.println("cleaning up " + thr);
            gameListModel.removeElement(thr);
            threads.remove(thr);
          }
        }
      }
      
      if (threads.size() == gameLimit) {
        try {
          Thread.sleep(25);
        } catch (InterruptedException ie) {}
        continue;
      }
      
      try {
        Socket s = server.accept();
        OutputStream os = s.getOutputStream();
        InputStream is = s.getInputStream();
        
        char side = populating == null ? 'x' : 'o';

        String playerUuid = UUID.randomUUID().toString().replaceAll("-", "");
        
        os.write((playerUuid + side).getBytes());
        os.flush();
        
        StringBuilder sb = new StringBuilder();
        while (sb.length() < 2) {
          while (is.available() > 0) {
            char c = (char)is.read();
            sb.append(c);
          }
        }
        
        if (sb.toString().equals("ok")) {
          if (populating == null) {
            populating = new Game();
            populating.setPlayerX(playerUuid);
            populatingSocket = s;
            logger.info(String.format("Assigned player at address %s to UUID %s and playing position x", s.getInetAddress().toString(), playerUuid));
          } else {
            populating.setPlayerO(playerUuid);
            logger.info(String.format("Assigned player at address %s to UUID %s and playing position o", s.getInetAddress().toString(), playerUuid));
            
            GameThread gt = new GameThread(populatingSocket, s, populating);
            gt.start(); // dispatch a new game thread
            synchronized (threads) {
              threads.add(gt);
            }
            gameListModel.addElement(gt);
            
            logger.info("Started game thread " + gt.toString());
            
            populating = null;
            populatingSocket = null;
          }
        } else {
          logger.warning("Invalid response from client. Ignoring.");
          s.close();
        }
      } catch (SocketTimeoutException se) {
        // no socket was accepted
        continue;
      } catch (IOException ioe) {
        onException.accept(ioe);
      }
    }
    
    logger.info("Cleaning up interrupted games...");
    for (GameThread gt : threads) {
      System.out.println("killing " + gt);
      gt.kill();
    }
    
    logger.info("Closing server socket...");
    
    try {
      server.close();
    } catch (IOException ioe) {
      onException.accept(ioe);
    }
    
    logger.info("Server socket closed.");
    
    if (onStop != null) {
      onStop.run();
    }
  }
  
  public static void main(String[] args) throws IOException {
    GameServer server = new GameServer(Common.HOST, Common.PORT, DEFAULT_GAME_LIMIT);
    server.run();
  }
}
