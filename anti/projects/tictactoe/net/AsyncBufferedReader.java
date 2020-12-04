package anti.projects.tictactoe.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

public class AsyncBufferedReader {
  private InputStream in;
  private LinkedList<Byte> loaded;
  private byte delimiter;
  
  public AsyncBufferedReader(InputStream in, byte delimiter) {
    this.in = in;
    this.delimiter = delimiter;
    this.loaded = new LinkedList<>();
  }
  
  /**
   * 
   * @return the next chunk of bytes if it is loaded, null otherwise
   */
  public byte[] read() throws IOException {
    while (in.available() > 0) {
      int i = in.read();
      if (i == -1) {
        break;
      }
      loaded.add((byte)i);
    }
    
    if (loaded.size() == 0) {
      return null;
    }
    
    // check if delimiter is in list
    int delimiterIdx = -1;
    
    int idx = 0;
    for (byte b : loaded) {
      if (b == delimiter) {
        delimiterIdx = idx;
        break;
      }
      idx++;
    }
    
    if (delimiterIdx != -1) {
      byte[] bytes = new byte[delimiterIdx];
      
      idx = 0;
      for (int i = 0; i < delimiterIdx; i++) {
        bytes[idx++] = loaded.get(0).byteValue();
        loaded.remove();
      }
      
      loaded.remove(); // remove the delimiter! duh
      
      return bytes;
    } else {
      return null;
    }
  }
}
