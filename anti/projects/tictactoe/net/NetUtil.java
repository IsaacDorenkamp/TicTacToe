package anti.projects.tictactoe.net;

public class NetUtil {
  public static String fromBytes(byte[] bytes) {
    if (bytes == null) return null;
    return fromBytes(bytes, 0, bytes.length);
  }
  public static String fromBytes(byte[] bytes, int startIdx, int endIdx) {
    if (bytes == null) return null;
    
    char[] c = new char[endIdx - startIdx];
    for (int i = startIdx; i < endIdx; i++) {
      c[i-startIdx] = (char)bytes[i];
    }
    
    return String.valueOf(c);
  }
}
