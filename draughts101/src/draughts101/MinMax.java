package draughts101;

import static draughts101.Board.GRID;
import static draughts101.Game.EMPTY;
import static draughts101.Game.KING;
import static draughts101.Game.MAN;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

final class MinMax extends HashMap<String, Integer> {
    final private static int INFINITY = Integer.MAX_VALUE;

    private static enum Node {
        ALFA {
            @Override
            int toAlfaBeta(int alfa, int value) {
                return Math.max(alfa, value);
            }

            @Override
            int valueOf(int value) {
                return value;
            }
        },
        BETA {
            @Override
            int toAlfaBeta(int beta, int value) {
                return Math.min(beta, value);
            }

            @Override
            int valueOf(int value) {
                return -value;
            }
        };
        
        abstract int toAlfaBeta(int alfaBeta, int value);
        abstract int valueOf(int value);
    }
    
    final private static int COLUMN = GRID / 2;
    final private static int ROW = GRID - 1;
    
    private static enum Diagonal {
        MIN_X_MIN_Y(COLUMN, 0, -COLUMN) {
            @Override
            long getKingSteps(int index, long occupied, long from) {
                long mask = LEFT_RIGHT[COLUMN - 1 - index % COLUMN + index / COLUMN % 2 + index / GRID];

                return mask & (occupied ^ Long.reverse(Long.reverse(mask & occupied) - Long.reverse(from)));
            }
        }, 
        PLUS_X_MIN_Y(COLUMN - 1, 0, -COLUMN + 1) {
            @Override
            long getKingSteps(int index, long occupied, long from) {
                long mask = RIGHT_LEFT[index % COLUMN + index / GRID];

                return mask & (occupied ^ Long.reverse(Long.reverse(mask & occupied) - Long.reverse(from)));
            }
        }, 
        MIN_X_PLUS_Y(COLUMN, ROW, COLUMN) {
            @Override
            long getKingSteps(int index, long occupied, long from) {
                long mask = RIGHT_LEFT[index % COLUMN + index / GRID];

                return mask & (occupied ^ ((mask & occupied) - from));
            }
        }, 
        PLUS_X_PLUS_Y(COLUMN - 1, ROW, COLUMN + 1) {
            @Override
            long getKingSteps(int index, long occupied, long from) {
                long mask = LEFT_RIGHT[COLUMN - 1 - index % COLUMN + index / COLUMN % 2 + index / GRID];

                return mask & (occupied ^ ((mask & occupied) - from));
            }
        };

        final int column;
        final int row;
        final int step;

        Diagonal(int column, int row, int step) {
            this.column = column;
            this.row = row;
            this.step = step;
        }

        boolean canStep(int index) {
            return index % GRID != column && index / COLUMN != row;
        }

        long getStep(int index) {
            return 1l << index + step - index / COLUMN % 2;
        }

        abstract long getKingSteps(int index, long occupied, long from);

        final private static long[] LEFT_RIGHT = new long[GRID];
        final private static long[] RIGHT_LEFT = new long[GRID - 1];

        static {
            for (int i = 0; i < LEFT_RIGHT.length; i++) {
                LEFT_RIGHT[i] = 0l;

                for (int j = COLUMN - 1 - Math.min(i, COLUMN - 1) + i / COLUMN * COLUMN + Math.max(0, i - COLUMN) * GRID; Long.bitCount(LEFT_RIGHT[i]) < 1 + (Math.min(i, COLUMN - 1) - Math.max(0, i - COLUMN)) * 2; j += COLUMN + 1 - j / COLUMN % 2) {
                    LEFT_RIGHT[i] ^= 1l << j;
                }
            }

            for (int i = 0; i < RIGHT_LEFT.length; i++) {
                RIGHT_LEFT[i] = 0l;

                for (int j = Math.min(i, COLUMN - 1) + Math.max(0, i - (COLUMN - 1)) * GRID; Long.bitCount(RIGHT_LEFT[i]) < 2 + (Math.min(i, COLUMN - 1) - Math.max(0, i - (COLUMN - 1))) * 2; j += COLUMN - j / COLUMN % 2) {
                    RIGHT_LEFT[i] ^= 1l << j;
                }
            }
        }
    }

    private static long middle = 0l;

    static {
        for (int i = COLUMN; i < ROW * COLUMN; i++) {
            if (i % GRID != COLUMN - 1 && i % GRID != COLUMN) {
                middle ^= 1l << i;
            }
        }
    }
    
    final private Node node;
    final private int color;
    
    private MinMax(Node node, int color) {
        this.node = node;
        this.color = color;
    }
    
    private int valueOf(char[] board, long isColor, long opponent, MinMax minMax, int[] alfaBeta, int value, int depth) {
        HashMap<Integer, HashSet<Long>> moves = new HashMap();
        int maxCapture = 0;
    
        for (long empty = ~(isColor ^ opponent), pieces = isColor; pieces != 0l; pieces ^= Long.lowestOneBit(pieces)) {
            int from = Long.numberOfTrailingZeros(pieces);
            boolean isKing = board[from] == KING[color];
 
            HashSet<Long> movesPiece = new HashSet();
            int maxCapturePiece = maxCapture;

            for (Diagonal[] horizontal : new Diagonal[][] {{Diagonal.MIN_X_MIN_Y, Diagonal.MIN_X_PLUS_Y}, {Diagonal.PLUS_X_MIN_Y, Diagonal.PLUS_X_PLUS_Y}}) {
                for (Diagonal vertical : horizontal) {
                    if (vertical.canStep(from)) {
                        long move = vertical.getStep(from);

                        if (isKing && (move & empty & middle) == move) {
                            move = vertical.getKingSteps(from, ~empty, move);
                        }
                       
                        long capture = move & opponent;
                        
                        if ((capture & middle) != 0l) {
                            long step = vertical.getStep(Long.numberOfTrailingZeros(capture));
                            
                            if ((step & empty) == step) {
                                if (isKing && (step & middle) == step) {
                                    step = vertical.getKingSteps(from, ~empty, step) & empty;
                                }
                                
                                ArrayList<Long> captureMoves = new ArrayList(Arrays.asList(new Long[] {capture ^ step}));
                                
                                empty ^= 1l << from;
                                
                                do {
                                    move = captureMoves.remove(0);

                                    long captures = move & opponent;
                
                                    if (Long.bitCount(captures) >= maxCapturePiece) {
                                        if (Long.bitCount(captures) > maxCapturePiece) {
                                            movesPiece.clear();
                                            maxCapturePiece++;
                                        }
                                        
                                        movesPiece.add(move);
                                    }
                                    
                                    for (long destination = move ^ captures; destination != 0l; destination ^= Long.lowestOneBit(destination)) {
                                        int to = Long.numberOfTrailingZeros(destination);

                                        for (Diagonal diagonal : Diagonal.values()) {
                                            if (diagonal.canStep(to)) {
                                                step = diagonal.getStep(to);
                                                
                                                if (isKing && (step & empty & middle) == step) {
                                                    step = diagonal.getKingSteps(to, ~empty, step);
                                                }

                                                if ((step & move) == 0l) {
                                                    capture = step & opponent;

                                                    if ((capture & middle) != 0l) {
                                                        step = diagonal.getStep(Long.numberOfTrailingZeros(capture));

                                                        if ((step & empty) == step) {
                                                            if (isKing && (step & middle) == step) {
                                                                step = diagonal.getKingSteps(to, ~empty, step) & empty;
                                                            }

                                                            captureMoves.add(captures ^ capture ^ step);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } while (!captureMoves.isEmpty());
                                
                                empty ^= 1l << from;
                            }
                        }
            
                        if (maxCapturePiece == 0 && (isKing || vertical == horizontal[color])) {
                            move &= empty;

                            if (move != 0l) {
                                movesPiece.add(move);
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
                
                moves.put(from, movesPiece);
            }
        }
        
        if (moves.isEmpty()) {
            return alfaBeta[node.ordinal()];
        } else if (depth > 0) {
            depth--;
        } else if (maxCapture == 0) {
            return value;
        }
        
        value += node.valueOf(maxCapture);
        
        for (int from : moves.keySet()) {
            char piece = board[from];
            
            board[from] = EMPTY;
            
            for (long move : moves.get(from)) {
                long captures = move & opponent;
                ArrayList<Integer> captured = new ArrayList();
                
                for (long l = captures; l != 0l; l ^= Long.lowestOneBit(l)) {
                    captured.add(Long.numberOfTrailingZeros(l));
                }
                
                for (long destination = move ^ captures; destination != 0l; destination ^= Long.lowestOneBit(destination)) {
                    int to = Long.numberOfTrailingZeros(destination);
                    String key = String.valueOf(getBoard(color, board.clone(), piece, captured, to));
                    
                    if (!containsKey(key)) {
                        put(key, minMax.valueOf(key.toCharArray(), opponent ^ captures, isColor ^ (1l << from ^ 1l << to), this, alfaBeta.clone(), value, depth));
                    }
                    
                    alfaBeta[node.ordinal()] = node.toAlfaBeta(alfaBeta[node.ordinal()], get(key));
                    
                    if (alfaBeta[Node.ALFA.ordinal()] >= alfaBeta[Node.BETA.ordinal()]) {
                        return alfaBeta[node.ordinal()];
                    }
                }
            }
            
            board[from] = piece;
        }
        
        return alfaBeta[node.ordinal()];
    }
    
    private static char[] getBoard(int color, char[] board, char piece, ArrayList<Integer> captured, int to) {
        board[to] = piece == MAN[color] && to / COLUMN == color * ROW ? KING[color] : piece;
        captured.forEach(capture -> board[capture] = EMPTY);
        
        return board;
    }
    
    static ArrayList<Integer> getAIMove(int ai, char[] board, HashSet<Integer>[] pieces, HashMap<Integer, ArrayList<Integer>[]> moves, int maxCapture, int depth) {
        int player = 1 - ai;
        
        long isColor = 0l;
        long opponent = 0l;
        
        for (int index : pieces[ai]) {
            isColor ^= 1l << index;
        }
        
        for (int index : pieces[player]) {
            opponent ^= 1l << index;
        }
        
        MinMax minMaxMax = new MinMax(Node.ALFA, ai);
        MinMax minMaxMin = new MinMax(Node.BETA, player);
            
        ArrayList<ArrayList<Integer>> aiMoves = new ArrayList();
        int max = -INFINITY;
        
        for (int from : moves.keySet()) {
            char piece = board[from];
            
            board[from] = EMPTY;
            
            for (ArrayList<Integer> move : moves.get(from)) {
                int to = move.remove(maxCapture);
                long captures = 0l;
                
                for (int capture : move) {
                    captures ^= 1l << capture;
                }
                
                int min = minMaxMin.valueOf(getBoard(ai, board.clone(), piece, move, to), opponent ^ captures, isColor ^ (1l << from ^ 1l << to), minMaxMax, new int[] {-INFINITY, INFINITY}, maxCapture, depth);
                
                if (min >= max) {
                    if (min > max) {
                        aiMoves.clear();
                        max = min;
                    }
                    
                    move.add(0, from);
                    move.add(to);
                    
                    aiMoves.add(move);
                }
            }

            board[from] = piece;
        }
        
        return aiMoves.get((int) (Math.random() * aiMoves.size()));
    }
    
}
