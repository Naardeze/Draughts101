package draughts101;

import static draughts101.Game.IMAGE;
import static draughts101.Game.WB;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSlider;

final class Board extends JPanel implements ActionListener {
    final static int WHITE = 0;
    final static int BLACK = 1;
    
    final static String[] COLOR = {"White", "Black"};
    
    final static int GRID = 10;
    
    final static Board BOARD = new Board(GRID * 40);
    
    final static JSlider LEVEL = new JSlider(1, 5);
    final static JButton UNDO = new JButton("\ud83e\udc44");
    final static JLabel WINNER = new JLabel();
    final static JCheckBox MOVEABLE = new JCheckBox();
    
    private static Game game = new Game(WHITE);
    
    final Rectangle[] tile = new Rectangle[GRID * GRID / 2];
    
    private Board(int boardSize) {
        super(new BorderLayout());
        
        setBackground(Color.white);
        setForeground(Color.lightGray);
        
        setPreferredSize(new Dimension(boardSize, boardSize));
        
        int tileSize = boardSize / GRID;
        
        for (int i = 0; i < tile.length; i++) {
            tile[i] = new Rectangle(x(i) * tileSize, y(i) * tileSize, tileSize, tileSize);
        }

        for (char color : WB.toCharArray()) {
            IMAGE[WB.indexOf(color)][0] = Toolkit.getDefaultToolkit().createImage(color + ".png").getScaledInstance(tileSize, tileSize, Image.SCALE_SMOOTH);
            IMAGE[WB.indexOf(color)][1] = Toolkit.getDefaultToolkit().createImage(color + "k.png").getScaledInstance(tileSize, tileSize, Image.SCALE_SMOOTH);
        }
    }
    
    static int x(int index) {
        return index % (GRID / 2) * 2 + 1 - index / (GRID / 2) % 2;
    }
    
    static int y(int index) {
        return index / (GRID / 2);
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (Rectangle tile : tile) {
            g.fillRect(tile.x, tile.y, tile.width, tile.height);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (Rectangle tile : tile) {
            tile.setLocation(getWidth() - tile.x - tile.width, getHeight() - tile.y - tile.height);
        }
        
        repaint();
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Draughts101");
        
        JMenuBar menuBar = new JMenuBar();
        
        JMenu menuGame = menuBar.add(new JMenu("Game"));
        JMenu menuAI = menuBar.add(new JMenu("AI"));
        
        JButton rotation = new JButton("\u21bb");
        
        JPanel left = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, -1));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JPanel center = new JPanel();
        JPanel south = new JPanel(new GridLayout(1, 3));
        
        for (int color : new int[] {WHITE, BLACK}) {
            menuGame.add(COLOR[color]).addActionListener(e -> {
                BOARD.remove(game);
                
                game = new Game(color);
                
                BOARD.add(game);
                BOARD.validate();
            });
        }
                
        menuAI.add(LEVEL);
                
        left.add(UNDO);
        
        right.add(MOVEABLE);
        right.add(rotation);
        
        center.add(BOARD);

        south.add(left);
        south.add(WINNER);
        south.add(right);

        LEVEL.setMajorTickSpacing(1);
        LEVEL.setPaintLabels(true);
         
        BOARD.add(game);
        BOARD.addContainerListener(new ContainerAdapter() {
            @Override
            public void componentRemoved(ContainerEvent e) {
                UNDO.removeActionListener((Game) e.getChild());
                
                WINNER.setText("");
            }
        });
        
        UNDO.setContentAreaFilled(false);
        UNDO.setBorder(null);
        UNDO.setFont(UNDO.getFont().deriveFont(22f));
        UNDO.setFocusable(false);
        
        WINNER.setHorizontalAlignment(JLabel.CENTER);
        
        MOVEABLE.setFocusable(false);
        MOVEABLE.addItemListener(e -> game.repaint());        
        
        rotation.setContentAreaFilled(false);
        rotation.setFont(rotation.getFont().deriveFont(16f));
        rotation.setBorder(null);
        rotation.setFocusable(false);
        rotation.addActionListener(BOARD);
        
        frame.setIconImage(Toolkit.getDefaultToolkit().createImage("bk.png").getScaledInstance(32, 32, Image.SCALE_SMOOTH));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setJMenuBar(menuBar);
        frame.add(center);
        frame.add(south, BorderLayout.SOUTH);
        frame.pack();
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
    }

}
