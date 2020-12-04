package anti.projects.tictactoe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import anti.projects.tictactoe.game.GameStateListener;
import anti.projects.tictactoe.gui.GameBoard;
import anti.projects.tictactoe.net.Common;
import anti.projects.tictactoe.net.GameClient;

/**
 * 
 * @author Isaac Dorenkamp
 * Tic Tac Toe game - main running class
 *
 */

// TODO - Connect to specified host instead of defaulting to localhost always :)

public class TicTacToe {
  public static final String TITLE = "TicTacToe";
  
  private JFrame frame;
  private JLabel side;
  private GameBoard board;
  
  private GameClient cli;
  
  public TicTacToe() {
    frame = new JFrame(TITLE);
    frame.setSize(500, 500);
    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent evt) {
        int status = 0;
        try {
          if (cli.getGameState().getWinner() == '-' && !cli.getGameState().boardIsFull()) cli.prematureExit(); // TODO - don't send close if no connection was established!
        } catch (IOException ioe) {
          status = 1;
          System.err.println("Failed to send close signal!");
        } finally {
          frame.dispose();
          System.exit(status);
        }
      }
    });
    
    side = new JLabel("You are: <connecting...>");
    side.setOpaque(true);
    side.setBackground(Color.WHITE);
    side.setFont(new Font("Arial", Font.PLAIN, 18));
    side.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK),
        BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    
    cli = new GameClient(Common.HOST, Common.PORT);
    
    // TODO - show win/lose/tie overlay after game is over
    // probably do this in GameBoard.java
    board = new GameBoard(cli.getGameState());
    board.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    board.setEnabled(false);
    
    frame.setLayout(new BorderLayout());
    frame.add(side, BorderLayout.NORTH);
    frame.add(board, BorderLayout.CENTER);
    
    // configure board events
    board.addBoardEventListener(cli);
    cli.getGameState().addStateListener(new GameStateListener() {
      @Override
      public void gameStateChanged() {
        if (cli.getGameState().getTurn() == cli.getSide()) {
          board.setEnabled(true);
        } else {
          board.setEnabled(false);
        }
      }
    });
  }
  
  public void run() {
    frame.setVisible(true);
    try {
      cli.handshake();
    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(frame, "Client handshake failed. Cannot start game :(", "Connection Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    
    side.setText("You are: " + cli.getSide());
    
    if (cli.getGameState().getTurn() == cli.getSide()) {
      board.setEnabled(true);
    }
    
    new Thread(() -> {
      try {
        cli.poll();
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog(frame, "The connection to the server was broken.", "Connection Error", JOptionPane.ERROR_MESSAGE);
      }
    }).start();
  }
  
  public static void main(String[] args) {
    TicTacToe game = new TicTacToe();
    game.run();
  }
}
