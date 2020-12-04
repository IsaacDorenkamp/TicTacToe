package anti.projects.tictactoe.game;

import java.util.ArrayList;
import java.util.Scanner;

public class GameState {
  private char[][] board;
  private int size;
  private char turn = 'x'; // X's always start
  private char winner = '-';
  
  private ArrayList<GameStateListener> stateListeners;
  
  public GameState(int size) {
    assert size >= 3 : "Board size must be at least 3.";
    this.size = size;
    board = new char[size][size];
    for (int x = 0; x < size; x++) {
      for (int y = 0; y < size; y++) {
        board[y][x] = '-';
      }
    }
    
    stateListeners = new ArrayList<>();
  }
  
  private void fireStateListeners() {
    for (GameStateListener list : stateListeners) {
      list.gameStateChanged();
    }
  }
  
  public void addStateListener(GameStateListener list) {
    stateListeners.add(list);
  }
  
  public boolean removeStateListener(GameStateListener list) {
    return stateListeners.remove(list);
  }
  
  public boolean isValidMove(char player, int x, int y) {
    if (player != turn) return false;
    if ((x < 0 || y < 0) || (x >= size || y >= size)) return false;
    
    if( board[y][x] == '-' ) {
      return true;
    } else {
      return false;
    }
  }
  
  public boolean move(char player, int x, int y) {
    if (winner != '-') return false;
    
    boolean valid = isValidMove(player, x, y);
    if (valid) {
      board[y][x] = player;
      winner = calculateWinner();
      turn = winner != '-' ? '-' : GameState.getNextPlayer(player);
      fireStateListeners();
      return true;
    } else {
      return false;
    }
  }
  
  public char getTurn() {
    return turn;
  }
  
  public int getBoardSize() {
    return size;
  }
  
  public char get(int x, int y) {
    return board[y][x];
  }
  
  public char getWinner() {
    return winner;
  }
  
  private char calculateWinner() {
    // check rows
    for (int y = 0; y < size; y++) {
      char starter = board[y][0];
      if (starter == '-') continue;
      
      boolean isWin = true;
      
      for (int x = 1; x < size; x++) {
        if (board[y][x] != starter) {
          isWin = false;
          break;
        }
      }
      
      if (isWin) {
        return starter;
      }
    }
    
    // check columns
    for (int x = 0; x < size; x++) {
      char starter = board[0][x];
      if (starter == '-') continue;
      
      boolean isWin = true;
      for (int y = 1; y < size; y++) {
        if (board[y][x] != starter) {
          isWin = false;
          break;
        }
      }
      
      if (isWin) {
        return starter;
      }
    }
    
    // check diags
    char firstDiagStart = board[0][0];
    if (firstDiagStart != '-') {
      boolean isWin = true;
      for (int xy = 1; xy < size; xy++) {
        if (board[xy][xy] != firstDiagStart) {
          isWin = false;
          break;
        }
      }
      
      if (isWin) {
        return firstDiagStart;
      }
    }
    
    char secondDiagStart = board[0][size-1];
    if (secondDiagStart != '-') {
      boolean isWin = true;
      for (int xy = 1; xy < size; xy++) {
        if (board[xy][size-(xy+1)] != secondDiagStart) {
          isWin = false;
          break;
        }
      }
      
      if (isWin) {
        return secondDiagStart;
      }
    }
    
    return '-';
  }
  
  public boolean boardIsFull() {
    for (int y = 0; y < size; y++) {
      for (int x = 0; x < size; x++) {
        if (board[y][x] == '-') return false;
      }
    }
    return true;
  }
  
  /* STATIC METHODS */
  public static char getNextPlayer(char cur) {
    if (cur == 'x') {
      return 'o';
    } else {
      return 'x';
    }
  }
  
  public static void printBoard(GameState state) {
    int size = state.getBoardSize();
    for (int y = 0; y < 4 * size; y++) {
      for (int x = 0; x < 4 * size; x++) {
        if (x == 0 || y == 0) {
          System.out.print(' ');
          continue;
        }
        if (x % 4 == 2 && y % 4 == 2) {
          int xIdx = (x - 2) / 4;
          int yIdx = (y - 2) / 4;
          char toPrint = state.get(xIdx, yIdx);
          if (toPrint == '-') {
            System.out.print(' ');
          } else {
            System.out.print(toPrint);
          }
        } else if (x % 4 == 0 && y % 4 == 0) {
          System.out.print('+');
        } else if (x % 4 == 0) {
          System.out.print('|');
        } else if (y % 4 == 0) {
          System.out.print('-');
        } else {
          System.out.print(' ');
        }
      }
      System.out.println();
    }
    
    System.out.println();
  }
  
  public static void main(String[] args) {
    int boardSize;
    if (args.length < 1) {
      System.out.println("No board size specified, defaulting to three");
      boardSize = 3;
    } else {
      try {
        boardSize = Integer.parseInt(args[0]);
      } catch(NumberFormatException nfe) {
        System.out.printf("Invalid board size '%s.' Defaulting to three\n", args[0]);
        boardSize = 3;
      }
    }
    
    Scanner in = new Scanner(System.in);
    
    GameState state = new GameState(boardSize);
    while (!(state.getWinner() != '-' || state.boardIsFull())) {
      System.out.printf("It's %c's turn.\n", state.getTurn());
      GameState.printBoard(state);
      System.out.printf("x y > ");
      String[] parts = in.nextLine().split(" ");
      int x;
      int y;
      if (parts.length != 2) {
        System.out.println("Invalid input. Please input the x-y coordinates as a pair of integers separated by a single space.");
        continue;
      } else {
        try {
          x = Integer.parseInt(parts[0]);
          y = Integer.parseInt(parts[1]);
        } catch(NumberFormatException nfe) {
          System.out.println("One or both coordinates were invalid. Try again.");
          continue;
        }
      }
      
      boolean moved = state.move(state.getTurn(), x, y);
      if (!moved) {
        System.out.println("Invalid move.");
      } else {
        if (state.getWinner() != '-') {
          System.out.printf("%c wins!\n", state.getWinner());
          GameState.printBoard(state);
        } else if (state.boardIsFull()) {
          System.out.println("Cat's game :/");
          GameState.printBoard(state);
        }
      }
    }
    
    in.close();
  }
}
