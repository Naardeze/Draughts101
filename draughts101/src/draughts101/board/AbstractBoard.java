package draughts101.board;

import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JPanel;

abstract public class AbstractBoard extends JPanel {
    final protected Rectangle[] square;
    
    protected AbstractBoard(Rectangle[] square) {
        this.square = square;
    }
    
    protected static void paintSquare(Graphics g, Rectangle square) {
        g.fillRect(square.x, square.y, square.width, square.height);
    }
    
}
