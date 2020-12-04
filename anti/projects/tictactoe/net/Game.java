package anti.projects.tictactoe.net;

import anti.projects.tictactoe.game.GameState;

public class Game {
  
  public static final int REQUEST_LENGTH = 34;
  public static final int UUID_LENGTH    = 32;
  public static final int X_INDEX        = 32;
  public static final int Y_INDEX        = 33;
  
  private String playerX = null;
  private String playerO = null;
  private GameState state;
  
  public Game() {
    state = new GameState(3); // TODO - custom board sizes?
  }
  
  public GameState getState() {
    return state;
  }
  
  public void setPlayerX(String playerX) {
    this.playerX = playerX;
  }
  
  public String getPlayerX() {
    return playerX;
  }
  
  public void setPlayerO(String playerO) {
    this.playerO = playerO;
  }
  
  public String getPlayerO() {
    return playerO;
  }
  
  public boolean onMoveReceived(char player, int x, int y) {
    return state.move(player, x, y);
  }
  
  public boolean isTurn(char player) {
    return state.getTurn() == player;
  }
  
  public char getPlayer(String uuid) {
    if (uuid.equals(playerX)) {
      return 'x';
    } else if (uuid.equals(playerO)) {
      return 'o';
    } else {
      return '-';
    }
  }
  
  public boolean isAlive() {
    return !(state.getWinner() != '-' || state.boardIsFull());
  }
}
