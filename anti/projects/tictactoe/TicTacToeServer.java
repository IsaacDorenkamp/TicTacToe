package anti.projects.tictactoe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import anti.projects.tictactoe.gui.GameBoard;
import anti.projects.tictactoe.net.Common;
import anti.projects.tictactoe.net.GameServer;
import anti.projects.tictactoe.net.GameThread;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;

public class TicTacToeServer {
  
  public static final String TITLE = "TicTacToe Game Server";
  
  // gui
  private JFrame frame;
  private JList<GameThread> games;
  private GameBoard display;
  private JTextPane logs;
  
  // server
  private GameServer server;
  private Thread serverThread;
  
  public TicTacToeServer() throws IOException {
    // TODO - display game limit somewhere lol
    
    // init gui
    frame = new JFrame(TITLE);
    frame.setSize(550, 500);
    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent evt) {
        server.setErrorCbk((exc) -> {
          JOptionPane.showMessageDialog(frame, "Error shutting down server: " + exc.getMessage(), "Shutdown Error", JOptionPane.ERROR_MESSAGE);
        });
        stopServer(() -> {
          frame.dispose();
        });
      }
    });
    
    frame.setLayout(new GridBagLayout());
    
    JPanel gamesPanel = new JPanel();
    gamesPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.BLACK));
    
    gamesPanel.setLayout(new BorderLayout());
    gamesPanel.setBackground(Color.WHITE);
    JLabel gamesLabel = new JLabel("Active Games");
    gamesLabel.setOpaque(false);
    gamesLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    
    gamesPanel.add(gamesLabel, BorderLayout.NORTH);
    games = new JList<GameThread>();
    games.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK),
        BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    games.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    games.addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        GameThread sel = games.getSelectedValue();
        if (sel != null) display.setGameState(sel.getGame().getState());
        else display.setGameState(null);
      }
      
    });
    gamesPanel.add(games, BorderLayout.CENTER);
    
    logs = new JTextPane();
    logs.setEditable(false);
    logs.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK),
        BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    logs.setFont(Common.COURIER_NEW);
    
    display = new GameBoard();
    display.setEnabled(false);
    
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridwidth = 1;
    gbc.weightx = 0.2;
    gbc.weighty = 0.6;
    gbc.fill = GridBagConstraints.BOTH;
    
    frame.add(gamesPanel, gbc);
    
    gbc.gridx = 1;
    gbc.weightx = 0.8;
    
    frame.add(display, gbc);
    
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 2;
    gbc.weighty = 0.4;
    
    frame.add(new JScrollPane(logs), gbc);
    
    // init server
    server = new GameServer(Common.HOST, Common.PORT, 2);
    games.setModel(server.getGameListModel());
    
    server.getLogger().addHandler(new Handler() {

      @Override
      public void publish(LogRecord record) {
        String line = String.format("[%s]: %s", record.getLoggerName(), record.getMessage());
        
        StyledDocument doc = logs.getStyledDocument();
        Style s = doc.getStyle("_default");
        if (s == null) {
          s = doc.addStyle("_default", null);
        }
        if (doc.getLength() == 0) {
          try {
            doc.insertString(0, line, s);
          } catch (BadLocationException e) {}
        } else {
          try {
            doc.insertString(doc.getLength(), "\n" + line, s);
          } catch (BadLocationException e) {}
        }
      }

      @Override
      public void flush() {}

      @Override
      public void close() throws SecurityException {}
      
    });
  }
  
  private void stopServer(Runnable cbk) {
    server.stopServer(cbk);
  }
  
  public void start() {
    serverThread = new Thread(() -> {
      server.run();
    });
    serverThread.start();
    frame.setVisible(true);
  }
  
  public void show() {
    frame.setVisible(true);
  }
  
  public static void main(String[] args) throws IOException {
    TicTacToeServer ttts = new TicTacToeServer();
    ttts.start();
  }
}
