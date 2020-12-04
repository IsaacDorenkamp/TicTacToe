package anti.projects.tictactoe.net;

public class ClientMessage {
  /**
   * Client Message Structure
   * 
   * 1 byte: Move turn (x or o)
   * 1 byte: x coordinate on board
   * 1 byte: y coordinate on board
   */
  
  public static final ClientMessage CLOSE = new ClientMessage('-', 0, 0);
  
  private char turn;
  private int x;
  private int y;
  
  public ClientMessage(char turn, int x, int y) {
    assert turn == 'x' || turn == 'o' || turn == '-' : "Turn must be either x or o (or - for the close message)";
    assert x == (x & 0xff) : "x must only be one byte in size";
    assert y == (y & 0xff) : "y must only be one byte in size";
      
    this.turn = turn;
    this.x = x;
    this.y = y;
  }
  
  public byte[] getBytes() {
    return new byte[] { (byte)turn, (byte)x, (byte)y };
  }
}
