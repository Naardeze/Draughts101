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
import javax.swing.ImageIcon;
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
    
    final private static Color LIGHT = Color.white;
    final private static Color DARK = Color.lightGray;
    
    final static Board BOARD = new Board(GRID * 40);
    
    final static JSlider AI = new JSlider(1, 7);
    final static JButton ARROW = new JButton(new ImageIcon("arrow.png"));
    final static JLabel WINNER = new JLabel();
    final static JCheckBox HINT = new JCheckBox();
    
    private static Game game = new Game(WHITE);
    
    final Rectangle[] tile = new Rectangle[GRID * GRID / 2];
    
    private Board(int boardSize) {
        super(new BorderLayout());
        
        setBackground(LIGHT);
        setForeground(DARK);
        
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

        JMenu gameMenu = menuBar.add(new JMenu("Game"));
        JMenu aiMenu = menuBar.add(new JMenu("AI"));
        
        JButton rotation = new JButton("\u21bb");
        
        JPanel left = new JPanel();
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JPanel center = new JPanel();
        JPanel south = new JPanel(new GridLayout(1, 3));
                
        for (int color : new int[] {WHITE, BLACK}) {
            gameMenu.add(COLOR[color]).addActionListener(e -> {
                BOARD.remove(game);
                
                game = new Game(color);
                
                BOARD.add(game);
                BOARD.validate();
            });
        }
        
        aiMenu.add(AI);

        AI.setMajorTickSpacing(1);
        AI.setPaintLabels(true);
         
        BOARD.addContainerListener(new ContainerAdapter() {
            @Override
            public void componentAdded(ContainerEvent e) {
                ARROW.addActionListener((Game) e.getChild());
            }
            @Override
            public void componentRemoved(ContainerEvent e) {
                ARROW.removeActionListener((Game) e.getChild());
                
                WINNER.setText("");
            }
        });
        BOARD.add(game);
        
        ARROW.setContentAreaFilled(false);
        ARROW.setBorder(null);
        ARROW.setFocusable(false);
        
        WINNER.setHorizontalAlignment(JLabel.CENTER);
        
        HINT.addItemListener(e -> game.repaint());        
        
        rotation.setContentAreaFilled(false);
        rotation.setFont(rotation.getFont().deriveFont(16f));
        rotation.setBorder(null);
        rotation.setFocusable(false);
        rotation.addActionListener(BOARD);
        
        left.add(ARROW);
        
        right.add(HINT);
        right.add(rotation);
        
        center.add(BOARD);

        south.add(left);
        south.add(WINNER);
        south.add(right);
        
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
