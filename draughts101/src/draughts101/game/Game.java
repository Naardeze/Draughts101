package draughts101.game;

import draughts101.Draughts101;
import draughts101.ai.AI;
import draughts101.board.PositionBoard;
import static draughts101.board.PositionBoard.BLACK_KING;
import static draughts101.board.PositionBoard.BLACK_PAWN;
import static draughts101.board.PositionBoard.EMPTY;
import static draughts101.board.PositionBoard.WHITE_KING;
import static draughts101.board.PositionBoard.WHITE_PAWN;
import draughts101.board.HintBoard;
import draughts101.board.SquareBoard;
import static draughts101.board.SquareBoard.SIZE;
import static draughts101.board.SquareBoard.x;
import static draughts101.board.SquareBoard.y;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Stack;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

final public class Game extends JLayeredPane {
    final public static char[] PAWN = {WHITE_PAWN, BLACK_PAWN};
    final public static char[] KING = {WHITE_KING, BLACK_KING};

    final private static int FRAMES = 50;
    final private static int MILLI = 3;
    final private static int DELAY = 100;
    
    final private Stack<String> positions = new Stack();

    final private SquareBoard squareBoard;
    final private PositionBoard positionBoard;
    final private HintBoard hintBoard;
    
    final private int player;

    private HashMap<Integer, Move[]> moves;
    
    private int captureCount;
    
    public Game(int player) {
        this.player = player;
        
        Draughts101.UNDO.setEnabled(false);        
        Draughts101.GAME_OVER.setVisible(false);

        squareBoard = new SquareBoard(player);
        positionBoard = new PositionBoard(squareBoard.getSquares());
        hintBoard = new HintBoard(squareBoard.getSquares());
        
        hintBoard.addMouseListener(new MouseAdapter() {
            Rectangle[] square = squareBoard.getSquares();
            
            private Move getMove(int from, Move move) {
                hintBoard.setVisible(false);

                positions.push(positionBoard.getPosition());

                Draughts101.UNDO.setEnabled(false);
                
                move.add(0, from);

                return move;
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                for (int index = 0; index < square.length; index++) {
                    if (square[index].contains(e.getPoint())) {
                        int selected = hintBoard.getSelected();
                        
                        if (selected != HintBoard.NONE && !positionBoard.getMove().isEmpty() && (positionBoard.getIndex(index) == EMPTY || index == selected)) {
                            ArrayList<Integer> captures = new ArrayList(positionBoard.getMove());                            
                            int next = captures.remove(captures.size() - 1);
                            
                            if (index != next) {
                                int x = x(index) - x(next);
                                int y = y(index) - y(next);
                                
                                if (Math.abs(x) == Math.abs(y)) {
                                    Direction direction = new Direction(Integer.signum(x), Integer.signum(y));

                                    next = direction.getNext(next);
                                    
                                    if (positionBoard.getIndex(selected) == KING[player]) {
                                        while (next != index && (positionBoard.getIndex(next) == EMPTY || next == selected)) {
                                            next = direction.getNext(next);
                                        }
                                    }
                                    
                                    if (isColor(1 - player, positionBoard.getIndex(next)) && !captures.contains(next)) {
                                        captures.add(next);
                                        
                                        next = direction.getNext(next);
                                        
                                        if (positionBoard.getIndex(selected) == KING[player]) {
                                            while (next != index && (positionBoard.getIndex(next) == EMPTY || next == selected)) {
                                                next = direction.getNext(next);
                                            }
                                        }
                                        
                                        if (next == index) {
                                            if (captures.size() == captureCount) {
                                                new BoardMove(getMove(selected, new Move(captures, index))).start();
                                            } else {
                                                captures.add(index);
                                                
                                                positionBoard.setMove(captures);
                                                positionBoard.repaint();
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (positionBoard.getIndex(index) != EMPTY) {
                            positionBoard.getMove().clear();
                            
                            if (moves.containsKey(index)) {
                                Move[] move = moves.get(index);
                                
                                if (move.length == 1) {
                                    new BoardMove(getMove(index, move[0])).start();
                                } else {
                                    hintBoard.setSelected(index);
                                    
                                    loop : for (int i = 1; i < move.length; i++) {
                                        for (int j = 0; j < i; j++) {
                                            if (Objects.equals(move[i].peek(), move[j].peek())) {
                                                positionBoard.getMove().add(index);
                                                
                                                break loop;
                                            }
                                        }
                                    }
                                }
                            } else {
                                hintBoard.setSelected(HintBoard.NONE);
                            }

                            positionBoard.repaint();
                        } else if (moves.containsKey(selected)) {
                            for (Move move : moves.get(selected)) {
                                if (move.peek() == index) {
                                    new BoardMove(getMove(selected, move)).start();
                                }
                            }
                        }
                        
                        break;
                    }
                }
            }
        });
        
        add(squareBoard);
        add(hintBoard, new Integer(1));
        add(positionBoard, new Integer(2));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                squareBoard.setSize(getSize());
                positionBoard.setSize(getSize());
                hintBoard.setSize(getSize());
            }
        });        

        setPreferredSize(new Dimension(Math.min(Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit.getDefaultToolkit().getScreenSize().height) / 2 / SIZE * SIZE, Math.min(Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit.getDefaultToolkit().getScreenSize().height) / 2 / SIZE * SIZE));
        
        turn(Draughts101.WHITE);
    }
    
    private void turn(int color) {
        moves = new HashMap();        
        captureCount = 0;

        char[] position = positionBoard.getPosition().toCharArray();
        
        for (int index = 0; index < position.length; index++) {
            if (isColor(color, position[index])) {
                char piece = position[index];                
                ArrayList<Move> pieceMoves = new ArrayList();
                
                for (Direction[] horizontal : new Direction[][] {{Direction.MIN_X_MIN_Y, Direction.MIN_X_PLUS_Y}, {Direction.PLUS_X_MIN_Y, Direction.PLUS_X_PLUS_Y}}) {
                    for (Direction vertical : horizontal) {
                        if (vertical.hasNext(index)) {
                            int next = vertical.getNext(index);
                            
                            if (position[next] == EMPTY && (piece == KING[color] || vertical == horizontal[color])) {
                                if (captureCount == 0) {
                                    pieceMoves.add(new Move(next));
                                }
                                
                                if (piece == KING[color] && vertical.hasNext(next)) {
                                    do {
                                        next = vertical.getNext(next);
                                        
                                        if (position[next] == EMPTY && captureCount == 0) {
                                            pieceMoves.add(new Move(next));
                                        }
                                    } while (position[next] == EMPTY && vertical.hasNext(next));
                                }
                            }
                            
                            if (vertical.hasNext(next) && isColor(1 - color, position[next]) && position[vertical.getNext(next)] == EMPTY) {
                                int capture = next;
                            
                                next = vertical.getNext(capture);
                                
                                ArrayList<Integer> captureLine = new ArrayList(Arrays.asList(new Integer[] {capture, next}));

                                if (piece == KING[color]) {
                                    while (vertical.hasNext(next) && position[vertical.getNext(next)] == EMPTY) {
                                        next = vertical.getNext(next);

                                        captureLine.add(next);
                                    }
                                }

                                ArrayList<ArrayList<Integer>> captureLines = new ArrayList(Arrays.asList(new ArrayList[] {captureLine}));

                                position[index] = EMPTY;

                                do {
                                    ArrayList<Integer> line = captureLines.remove(0);
                                    ArrayList<Integer> captured = new ArrayList();

                                    do {
                                        captured.add(line.remove(0));
                                    } while (isColor(1 - color, position[line.get(0)]));

                                    if (captured.size() > captureCount) {
                                        pieceMoves.clear();
                                        moves.clear();

                                        captureCount++;
                                    }

                                    for (int step : line) {
                                        if (captured.size() == captureCount) {
                                            pieceMoves.add(new Move(captured, step));
                                        }

                                        for (Direction diagonal : new Direction[] {Direction.MIN_X_MIN_Y, Direction.MIN_X_PLUS_Y, Direction.PLUS_X_MIN_Y, Direction.PLUS_X_PLUS_Y}) {
                                            if (diagonal.hasNext(step)) {
                                                next = diagonal.getNext(step);

                                                if (piece == KING[color] && !line.contains(next)) {
                                                    while (position[next] == EMPTY && diagonal.hasNext(next)) {
                                                        next = diagonal.getNext(next);
                                                    }
                                                }

                                                if (diagonal.hasNext(next) && isColor(1 - color, position[next]) && !captured.contains(next) && position[diagonal.getNext(next)] == EMPTY) {
                                                    capture = next;                                                        
                                                    next = diagonal.getNext(capture);

                                                    captureLine = new ArrayList(captured);
                                                    captureLine.addAll(Arrays.asList(new Integer[] {capture, next}));

                                                    if (piece == KING[color]) {
                                                        while (diagonal.hasNext(next) && position[diagonal.getNext(next)] == EMPTY) {
                                                            next = diagonal.getNext(next);

                                                            captureLine.add(next);
                                                        }
                                                    }

                                                    captureLines.add(captureLine);
                                                }
                                            }
                                        }
                                    }
                                } while (!captureLines.isEmpty());

                                position[index] = piece;
                            }
                        }
                    }
                }
                
                if (!pieceMoves.isEmpty()) {
                    moves.put(index, pieceMoves.toArray(new Move[pieceMoves.size()]));
                }
            }
        }
        
        if (moves.isEmpty()) {
            Draughts101.GAME_OVER.setVisible(true);
        } else if (color == player) {
            hintBoard.setKeySet(moves.keySet());
        } else {
            new BoardMove(new AI(Draughts101.LEVEL.getValue()).getMove(color, position, moves)).start();
        }
        
        if (hintBoard.isVisible() || moves.isEmpty()) {
            Draughts101.UNDO.setEnabled(!positions.isEmpty());
        }
    }
    
    public void undo() {
        Draughts101.UNDO.setEnabled(false);
        Draughts101.GAME_OVER.setVisible(false);

        positionBoard.getMove().clear();
        positionBoard.setPosition(positions.pop());

        hintBoard.setVisible(false);

        turn(player);
    }
    
    private static boolean isColor(int color, char piece) {
        return piece == PAWN[color] || piece == KING[color];
    }
    
    private class BoardMove extends Thread {
        Rectangle[] square = squareBoard.getSquares();

        int index;
        char piece;
        
        BoardMove(Move move) {
            index = move.remove(0);
            piece = positionBoard.getIndex(index);
            
            positionBoard.setMove(move);
            positionBoard.repaint();
        }
        
        @Override
        public void run() {
            JPanel slider = new JPanel() {
                @Override
                public void paint(Graphics g) {
                    g.drawString(String.valueOf(piece), (getWidth() - g.getFontMetrics().charWidth(piece)) / 2, (int) (PositionBoard.Y * getHeight()));
                }
            };

            slider.setOpaque(false);
            slider.setFont(positionBoard.getFont());
            slider.setForeground(PositionBoard.COLOR[PositionBoard.WBWB.indexOf(piece) % PositionBoard.COLOR.length]);
            slider.setBounds(square[index]);

            positionBoard.setIndex(index, EMPTY);
            positionBoard.add(slider);

            Direction direction = new Direction(Integer.signum(x(positionBoard.getMove().get(0)) - x(index)), Integer.signum(y(positionBoard.getMove().get(0)) - y(index)));

            for (int step : positionBoard.getMove()) {
                do {
                    index = direction.getNext(index);

                    for (int horizontal = square[index].x - slider.getX(), vertical = square[index].y - slider.getY(), i = FRAMES - 1; i >= 0; i--) {
                        slider.setLocation(square[index].x - (int) (i * (double) horizontal / FRAMES), square[index].y - (int) (i * (double) vertical / FRAMES));

                        try {
                            Thread.sleep(MILLI);
                        } catch (Exception ex) {}
                    }
                } while (direction.x * (x(step) - x(index)) != -direction.y * (y(step) - y(index)));

                if (index != step) {
                    direction.setLocation(Integer.signum(x(step) - x(index)), Integer.signum(y(step) - y(index)));

                    try {
                        Thread.sleep(DELAY);
                    } catch (Exception ex) {}
                }
            }

            if (piece == WHITE_PAWN && index < SIZE / 2) {
                piece = WHITE_KING;
            } else if (piece == BLACK_PAWN && index >= square.length - SIZE / 2) {
                piece = BLACK_KING;
            }

            positionBoard.remove(slider);
            positionBoard.setIndex(index, piece);                
            positionBoard.repaint();

            try {
                Thread.sleep(DELAY);
            } catch (Exception ex) {}

            for (int i = 0; i < captureCount; i++) {
                positionBoard.setIndex(positionBoard.getMove().remove(0), EMPTY);
                positionBoard.repaint();

                try {
                    Thread.sleep(DELAY);
                } catch (Exception ex) {}
            }

            turn(1 - PositionBoard.WBWB.indexOf(piece) % PositionBoard.COLOR.length);
        }
    }
   
}
