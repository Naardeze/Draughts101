package draughts101;

import static draughts101.Board.BLACK;
import static draughts101.Board.BOARD;
import static draughts101.Board.COLOR;
import static draughts101.Board.GRID;
import static draughts101.Board.LEVEL;
import static draughts101.Board.MOVEABLE;
import static draughts101.Board.UNDO;
import static draughts101.Board.WHITE;
import static draughts101.Board.WINNER;
import static draughts101.Board.x;
import static draughts101.Board.y;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

final class Game extends Component implements ActionListener, MouseListener {
    final private static char W = 'w';
    final private static char B = 'b';

    final static String WB = W + "" + B;

    final static char[] MAN = WB.toCharArray();
    final static char[] KING = WB.toUpperCase().toCharArray();

    final static char EMPTY = '_';

    final static Image[][] IMAGE = new Image[COLOR.length][2];
    
    private static enum Direction {
        MIN_X_MIN_Y(-1, -1),
        PLUS_X_MIN_Y(1, -1),
        MIN_X_PLUS_Y(-1, 1),
        PLUS_X_PLUS_Y(1, 1);

        final int x;
        final int y;
        
        Direction(int x, int y) {
            this.x = x;
            this.y = y;
        }

        boolean canStep(int index) {
            int x = x(index) + this.x;
            int y = y(index) + this.y;

            return x >= 0 && x < GRID && y >= 0 && y < GRID;
        }

        int getStep(int index) {
            return (x(index) + x) / 2 + (y(index) + y) * (GRID / 2);
        }
        
        static Direction getDirection(int from, int to) {
            if (x(from) > x(to)) {
                if (from > to) {
                    return MIN_X_MIN_Y;
                } else {
                    return MIN_X_PLUS_Y;
                }
            } else {
                if (from > to) {
                    return PLUS_X_MIN_Y;
                } else {
                    return PLUS_X_PLUS_Y;
                }
            }
        }
    }
    
    final private static int NONE = -1;
    final private static int DELAY = 300;

    final private Stack<String> boards = new Stack();
    
    final private int player;
    
    private char[] board = new char[BOARD.tile.length];
    private ArrayList<Integer> move = new ArrayList();
    
    private HashSet<Integer>[] pieces = new HashSet[COLOR.length];
    private HashMap<Integer, ArrayList<Integer>[]> moves;
    private int maxCapture;
    
    private int selected;
    
    Game(int player) {
        this.player = player;
        
        UNDO.setEnabled(false);
        
        Arrays.fill(board, 0, board.length / 2 - GRID / 2, B);
        Arrays.fill(board, board.length / 2 - GRID / 2, board.length / 2 + GRID / 2, EMPTY);
        Arrays.fill(board, board.length / 2 + GRID / 2, board.length, W);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                turn(WHITE);
            }
        });
    }
    
    private void turn(int color) {
        pieces[WHITE] = new HashSet();
        pieces[BLACK] = new HashSet();
        
        for (int i = 0; i < board.length; i++) {
            if (board[i] != EMPTY) {
                pieces[WB.indexOf(Character.toLowerCase(board[i]))].add(i);
            }
        }
        
        int opponent = 1 - color;

        moves = new HashMap();
        maxCapture = 0;
        
        for (int from : pieces[color]) {
            char piece = board[from];

            HashSet<ArrayList<Integer>> movesPiece = new HashSet();
            int maxCapturePiece = maxCapture;
            
            for (Direction[] horizontal : new Direction[][] {{Direction.MIN_X_MIN_Y, Direction.MIN_X_PLUS_Y}, {Direction.PLUS_X_MIN_Y, Direction.PLUS_X_PLUS_Y}}) {
                for (Direction vertical : horizontal) {
                    if (vertical.canStep(from)) {
                        int step = vertical.getStep(from);
                        
                        if(board[step] == EMPTY) {
                            if (maxCapturePiece == 0 && (piece == KING[color] || vertical == horizontal[color])) {
                                movesPiece.add(new ArrayList(Arrays.asList(new Integer[] {step})));
                            }

                            if (piece == KING[color] && vertical.canStep(step)) {
                                do {
                                    step = vertical.getStep(step);

                                    if (maxCapturePiece == 0 && board[step] == EMPTY) {
                                        movesPiece.add(new ArrayList(Arrays.asList(new Integer[] {step})));
                                    }
                                } while (board[step] == EMPTY && vertical.canStep(step));
                            }
                        }

                        if (pieces[opponent].contains(step) && vertical.canStep(step)) {
                            int capture = step;

                            step = vertical.getStep(capture);
                            
                            if (board[step] == EMPTY) {
                                ArrayList<Integer> captureMove = new ArrayList(Arrays.asList(new Integer[] {capture, step}));

                                if (piece == KING[color] && vertical.canStep(step)) {
                                    do {
                                        step = vertical.getStep(step);

                                        if (board[step] == EMPTY) {
                                            captureMove.add(step);
                                        }
                                    } while (board[step] == EMPTY && vertical.canStep(step));
                                }

                                ArrayList<ArrayList<Integer>> captureMoves = new ArrayList(Arrays.asList(new ArrayList[] {captureMove}));

                                board[from] = EMPTY;

                                do {
                                    ArrayList<Integer> destination = captureMoves.remove(0);
                                    ArrayList<Integer> captures = new ArrayList();

                                    do {
                                        captures.add(destination.remove(0));
                                    } while (pieces[opponent].contains(destination.get(0)));

                                    if (captures.size() > maxCapturePiece) {
                                        movesPiece.clear();                                       
                                        maxCapturePiece++;
                                    }

                                    for (int to : destination) {
                                        if (captures.size() == maxCapturePiece) {
                                            ArrayList<Integer> move = new ArrayList(captures);
                                            
                                            move.add(to);
                                            movesPiece.add(move);
                                        }

                                        for (Direction diagonal : Direction.values()) {
                                            if (diagonal.canStep(to)) {
                                                step = diagonal.getStep(to);                                                
                                                
                                                if (piece == KING[color] && !destination.contains(step)) {
                                                    while (board[step] == EMPTY && diagonal.canStep(step)) {
                                                        step = diagonal.getStep(step);
                                                    }
                                                 }

                                                if (pieces[opponent].contains(step) && !captures.contains(step) && diagonal.canStep(step)) {
                                                    capture = step;
                                                    step = diagonal.getStep(capture);

                                                    if (board[step] == EMPTY) {
                                                        captureMove = new ArrayList(captures);
                                                        captureMove.addAll(Arrays.asList(new Integer[] {capture, step}));

                                                        if (piece == KING[color] && diagonal.canStep(step)) {
                                                            do {
                                                                step = diagonal.getStep(step);
                                                                
                                                                if (board[step] == EMPTY) {
                                                                    captureMove.add(step);
                                                                }
                                                            } while (board[step] == EMPTY && diagonal.canStep(step));
                                                        }

                                                        captureMoves.add(captureMove);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } while (!captureMoves.isEmpty());

                                board[from] = piece;
                            }
                        }
                    }
                }
            }
            
            if (!movesPiece.isEmpty()) {
                if (maxCapturePiece > maxCapture) {
                    moves.clear();
                    maxCapture = maxCapturePiece;
                }

                moves.put(from, movesPiece.toArray(new ArrayList[movesPiece.size()]));
            }
        }

        if (BOARD.isAncestorOf(this)) {
            if (moves.isEmpty()) {
                WINNER.setText(COLOR[opponent] + " is Winner");
            } else if (color == player) {
                selected = NONE;
                addMouseListener(this);

                repaint();
            } else {                
                setCursor(new Cursor(Cursor.WAIT_CURSOR));
                        
                new Thread() {
                    @Override
                    public void run() {
                        ArrayList<Integer> move = MinMax.getAIMove(color, board.clone(), pieces, moves, maxCapture, LEVEL.getValue());
                
                        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        turn(move(color, move.remove(0), move));
                    }
                }.start();
            }
            
            if (moves.isEmpty() || color == player) {
                UNDO.setEnabled(true);
            }
        }
    }
 
    private int move(int color, int from, ArrayList<Integer> move) {
        this.move.clear();

        char piece = board[from];
        
        for (int i = 1; i < maxCapture; from = this.move.remove(i++)) {
            int capture = move.remove(0);
            Direction direction = Direction.getDirection(from, capture);
            int capture2 = move.get(0);
            int step = direction.x * (x(capture2) - x(capture)) == direction.y * (y(capture2) - y(capture)) ? direction.getStep(capture) : (x(capture) + direction.x * (direction.x * (x(capture2) - x(capture)) + direction.y * (y(capture2) - y(capture))) / 2) / 2 + (y(capture) + direction.y * (direction.x * (x(capture2) - x(capture)) + direction.y * (y(capture2) - y(capture))) / 2) * (GRID / 2);

            this.move.addAll(Arrays.asList(new Integer[] {capture, step}));

            board[from] = EMPTY;
            board[step] = piece;

            repaint();

            try {
                Thread.sleep(DELAY);
            } catch (Exception ex) {}
        }
        
        this.move.addAll(move);
        
        int to = this.move.get(maxCapture);
        
        board[from] = EMPTY;
        board[to] = piece == MAN[color] && to / (GRID / 2) == color * (GRID - 1) ? KING[color] : piece;
        
        repaint();
        
        try {
            Thread.sleep(DELAY);
        } catch (Exception ex) {}
        
        for (int i = 0; i < maxCapture; i++) {
            board[this.move.remove(0)] = EMPTY;
            
            repaint();
        
            try {
                Thread.sleep(DELAY);
            } catch (Exception ex) {}
        }
        
        return 1 - color;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        UNDO.setEnabled(false);

        move.clear();
        board = boards.pop().toCharArray();
        
        if (moves.isEmpty()) {
            WINNER.setText("");
        } else {
            removeMouseListener(this);
        
            if (boards.isEmpty()) {
                UNDO.removeActionListener(this);
            }
        }
        
        turn(player);
    }

    @Override
    public void paint(Graphics g) {        
        for (int i = 0; i < board.length; i++) {
            if (move.contains(i) || (UNDO.isEnabled() && (i == selected || (selected == NONE && moves.containsKey(i) && MOVEABLE.isSelected())))) {
                g.setColor(move.contains(i) ? i == move.get(move.size() - 1) ? Color.green : Color.yellow : Color.orange);
                g.fillRect(BOARD.tile[i].x, BOARD.tile[i].y, BOARD.tile[i].width, BOARD.tile[i].height);
            }
            
            if (board[i] != EMPTY) {
                g.drawImage(IMAGE[WB.indexOf(Character.toLowerCase(board[i]))][(Character.toLowerCase(board[i]) + "" + Character.toUpperCase(board[i])).indexOf(board[i])], BOARD.tile[i].x, BOARD.tile[i].y, this);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        for (int pressed = 0; pressed < BOARD.tile.length; pressed++) {
            if (BOARD.tile[pressed].contains(e.getPoint())) {
                if (selected != NONE && (board[pressed] == EMPTY || pressed == selected)) {
                    ArrayList<Integer> move = new ArrayList(this.move);
                    int step = move.remove(move.size() - 1);
                    
                    if (pressed != step && Math.abs(x(pressed) - x(step)) == Math.abs(y(pressed) - y(step))) {
                        Direction direction = Direction.getDirection(step, pressed);
                        
                        step = direction.getStep(step);

                        if (board[selected] == KING[player]) {
                            while (step != pressed && (board[step] == EMPTY || step == selected)) {
                                step = direction.getStep(step);
                            }
                        }

                        if (pieces[1 - player].contains(step) && !move.contains(step)) {
                            move.add(step);                                
                            step = direction.getStep(step);

                            if (board[selected] == KING[player]) {
                                while (step != pressed && (board[step] == EMPTY || step == selected)) {
                                    step = direction.getStep(step);
                                }
                            }
                        } else if (maxCapture > 0 || (board[selected] == W && direction.y == 1) || (board[selected] == B && direction.y == -1)) {
                            break;
                        }

                        if (step == pressed) {
                            move.add(pressed);

                            if (move.indexOf(pressed) == maxCapture) {
                                removeMouseListener(this);               
                                UNDO.setEnabled(false);

                                if (boards.isEmpty()) {
                                    UNDO.addActionListener(this);
                                }

                                boards.add(String.valueOf(board));
                                
                                new Thread() {
                                    @Override
                                    public void run() {
                                        turn(move(player, selected, move));
                                    }
                                }.start();
                            } else {
                                this.move = move;

                                repaint();
                            }
                        }
                    } 
                } else if (board[pressed] != EMPTY) {
                    move.clear();
                    
                    if (moves.containsKey(pressed)) {
                        selected = pressed;
                        move.add(selected);
                    } else {
                        selected = NONE;
                    }
                    
                    repaint();
                }
                
                break;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

}
