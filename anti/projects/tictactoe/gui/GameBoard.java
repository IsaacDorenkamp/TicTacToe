package anti.projects.tictactoe.gui;

import javax.swing.JComponent;

import anti.projects.tictactoe.game.GameState;
import anti.projects.tictactoe.game.GameStateListener;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.awt.BasicStroke;
import java.awt.Color;

public class GameBoard extends JComponent {
  
  private static final long serialVersionUID = -2679737483556950340L;
  
  private static final BasicStroke DRAW_STROKE = new BasicStroke(3);
  private static final RenderingHints HINTS    = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON);
  
  public static final int MIN_CELL_PADDING = 10;
  public static final int MIN_CELL_SIZE = 10;
  
  private GameState state;
  private Point selected = null;
  private boolean cursorPress = false;
  
  private ArrayList<BoardEventListener> boardEvts = new ArrayList<>();
  
  private final GameStateListener doRepaint = new GameStateListener() {
    public void gameStateChanged() {
      repaint();
    }
  };
  
  public GameBoard(GameState state) {
    super();
    this.state = state;
    if (this.state != null) {
      this.state.addStateListener(doRepaint);
    }
    setBackground(Color.WHITE);
    
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
        selected = null;
        cursorPress = false;
        repaint();
      }
      
      @Override
      public void mousePressed(MouseEvent e) {
        if (isEnabled()) {
          cursorPress = true;
          repaint();
        }
      }
      
      @Override
      public void mouseReleased(MouseEvent e) {
        if (isEnabled()) {
          cursorPress = false;
          // call on cell clicked
          for (BoardEventListener list : boardEvts) {
            list.onCellClicked(selected);
          }
          repaint();
        }
      }
    });
    addMouseMotionListener(new MouseMotionListener() {
      @Override
      public void mouseDragged(MouseEvent e) {}

      @Override
      public void mouseMoved(MouseEvent e) {
        if (isEnabled() && state != null) {
          int w = getWidth();
          int h = getHeight();
          
          int cells = state.getBoardSize();
          int cellSize = Math.max(Math.min(w, h) / state.getBoardSize(), MIN_CELL_SIZE);
          
          int offsetX = (w - (cells * cellSize)) / 2;
          int offsetY = (h - (cells * cellSize)) / 2;
          
          int boardX = e.getX() - offsetX;
          int boardY = e.getY() - offsetY;
          
          if (boardX < 0 || boardX > (cells * cellSize) || boardY < 0 || boardY > (cells * cellSize)) {
            selected = null;
            repaint();
            return;
          }
          
          int cellX = boardX / cellSize;
          int cellY = boardY / cellSize;
          
          if ( selected == null || (selected.x != cellX || selected.y != cellY) ) {
            selected = new Point(cellX, cellY);
            repaint();
          }
        }
      }
      
    });
  }
  
  public GameBoard() {
    this(null);
  }
  
  public void setGameState(GameState state) {
    if (this.state != null) {
      this.state.removeStateListener(doRepaint);
    }
    this.state = state;
    if (this.state != null) {
      this.state.addStateListener(doRepaint);
    }
    selected = null;
    repaint();
  }
  
  public void addBoardEventListener(BoardEventListener list) {
    boardEvts.add(list);
  }
  
  @Override
  public void setEnabled(boolean b) {
    super.setEnabled(b);
    
    if (!b) {
      selected = null;
    }
  }
  
  @Override
  public void paintComponent(Graphics graph) {
    Insets borderInsets = getBorder() != null ? getBorder().getBorderInsets(this) : new Insets(0, 0, 0, 0);
    int w = getWidth() - (borderInsets.left + borderInsets.right);
    int h = getHeight() - (borderInsets.top + borderInsets.bottom);
    
    Graphics2D g = (Graphics2D) graph;
    g.setStroke(DRAW_STROKE);
    g.setRenderingHints(HINTS);
    
    g.clearRect(0, 0, getWidth(), getHeight());
    
    if (state == null) return; // render nothing
    
    int cells = state.getBoardSize();
    int cellSize = Math.max(Math.min(w, h) / state.getBoardSize(), MIN_CELL_SIZE);
    
    int offsetX = borderInsets.left + ((w - (cells * cellSize)) / 2);
    int offsetY = borderInsets.top + ((h - (cells * cellSize)) / 2);
    
    // draw lines
    g.setColor(Color.BLACK);
    for (int cell = 0; cell < cells - 1; cell++) {
      g.drawLine(offsetX + (cell+1) * cellSize - 1, offsetY, offsetX + (cell+1) * cellSize - 1, offsetY + cellSize * cells);
      g.drawLine(offsetX, offsetY + (cell+1) * cellSize - 1, offsetX + cellSize * cells, offsetY + (cell+1) * cellSize - 1);
    }
    
    // draw cells
    for (int y = 0; y < cells; y++) {
      for (int x = 0; x < cells; x++) {
        char placed = state.get(x, y);
        if (placed == 'x') {
          drawX(g, x, y, cellSize, offsetX, offsetY);
        } else if (placed == 'o') {
          drawO(g, x, y, cellSize, offsetX, offsetY);
        }
      }
    }
    
    // draw selection cursor, if component is enabled
    if (isEnabled() && selected != null) {
      drawSelectionCursor(g, cellSize, offsetX, offsetY);
    }
  }
  
  private int getCellPadding(int cellSize) {
    return Math.max(MIN_CELL_PADDING, (int)Math.ceil(cellSize * 0.2));
  }
  
  private void drawSelectionCursor(Graphics2D g, int cellSize, int offsetX, int offsetY) {
    int padding = getCellPadding(cellSize) / 2;
    
    Color draw = cursorPress ? Color.RED : Color.LIGHT_GRAY;
    g.setColor(draw);
    g.drawRect(selected.x * cellSize + padding + offsetX, selected.y * cellSize + padding + offsetY, cellSize - 2 * padding,
        cellSize - 2 * padding);
  }
  
  private void drawX(Graphics2D g, int x, int y, int cellSize, int offsetX, int offsetY) {
    int padding = getCellPadding(cellSize);
    
    int startX = (x * cellSize) + padding + offsetX;
    int startY = (y * cellSize) + padding + offsetY;
    int endX   = ((x + 1) * cellSize) - padding - 1 + offsetX;
    int endY   = ((y + 1) * cellSize) - padding - 1 + offsetY;
    
    g.setColor(Color.BLACK);
    g.drawLine(startX, startY, endX, endY);
    g.drawLine(startX, endY, endX, startY);
  }
  
  private void drawO(Graphics2D g, int x, int y, int cellSize, int offsetX, int offsetY) {
    int padding = getCellPadding(cellSize);
    
    int startX = (x * cellSize) + padding + offsetX;
    int startY = (y * cellSize) + padding + offsetY;
    int diam = cellSize - (padding * 2) - 1;
    
    g.setColor(Color.BLACK);
    g.drawOval(startX, startY, diam, diam);
  }
}
