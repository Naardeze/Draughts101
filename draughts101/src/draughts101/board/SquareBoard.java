package draughts101.board;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

final public class SquareBoard extends AbstractBoard {
    final public static int SIZE = 10;
    
    public SquareBoard(int color) {
        super(new Rectangle[SIZE * SIZE / 2]);
        
        for (int i = 0; i < square.length; i++) {
            square[i] = new Rectangle();
        }
        
        setBackground(new Color(255, 255, 186));
        setForeground(new Color(170, 126, 89));
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                for (int i = 0; i < square.length; i++) {
                    square[new int[] {i, square.length - 1 - i}[color]].setBounds(x(i) * getWidth() / SIZE, y(i) * getHeight() / SIZE, getWidth() / SIZE, getHeight() / SIZE);
                }
            }
        });
    }
    
    public static int x(int index) {
        return index % (SIZE / 2) * 2 + 1 - index / (SIZE / 2) % 2;
    }
    
    public static int y(int index) {
        return index / (SIZE / 2);
    }

    public Rectangle[] getSquares() {
        return square;
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        for (Rectangle square : square) {
            paintSquare(g, square);
        }
    }
    
}
