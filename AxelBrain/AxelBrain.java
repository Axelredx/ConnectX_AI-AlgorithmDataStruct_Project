package connectx.AxelBrain;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;
import java.util.Random;
import java.util.TreeSet;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.lang.Math;
import java.util.HashMap;

public class AxelBrain implements CXPlayer {
    private Boolean is_first;
    private Integer Columns;
    private Integer Rows;
    private Integer ToWin;
    private int MAX_DEPTH = 5; // if x > 5 excedeed time limit
    private int MAX_BRANCHING = 6; // if x > 6 excedeed time limit
    private int TIMEOUT;
    private long START;

    public AxelBrain() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        is_first = first;
        Columns = N;
        Rows = M;
        ToWin = K;
        TIMEOUT = timeout_in_secs;
    }

    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis(); // Save starting time
        Integer[] col_avaible = B.getAvailableColumns();
        int bestColumn = col_avaible[col_avaible.length / 2];
        int bestScore = Integer.MIN_VALUE;
        try {
            Integer[] availableColumns = B.getAvailableColumns();
            for (int column : availableColumns) {
                B.markColumn(column);
                int score = findBestMove(B);
                B.unmarkColumn();
                if (score > bestScore) {
                    bestScore = score;
                    bestColumn = column;
                }
            }
        } catch (TimeoutException e) {
            System.out.println("Timeout! Returning the best column found so far. :(");
        }
        return bestColumn;
    }

    public int findBestMove(CXBoard board) throws TimeoutException {
        int bestScore = Integer.MIN_VALUE;

        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            checktime();
            int score = iterativeDeepening(board, depth, Integer.MIN_VALUE, Integer.MAX_VALUE);
            if (score > bestScore) {
                bestScore = score;
            }
        }

        return bestScore;
    }

    public int iterativeDeepening(CXBoard board, int depth, int alpha, int beta) throws TimeoutException {
        HashMap<CXBoard, Integer> visited = new HashMap<>();

        int bestScore = Integer.MIN_VALUE;

        while (depth!=MAX_BRANCHING) {
            checktime();
            int score = alphaBetaWithMemory(board, depth, alpha, beta, true, visited);
            if (score == Integer.MAX_VALUE || score == Integer.MIN_VALUE || score == 0) {
                // Found a winning move, stop searching
                return score;
            }

            bestScore = score;
            depth++;

            if (bestScore >= beta) {
                // Prune remaining branches
                break;
            }
        }

        return bestScore;
    }

    private int alphaBetaWithMemory(CXBoard board, int depth, int alpha, int beta, boolean maximizingPlayer,
            HashMap<CXBoard, Integer> visited) throws TimeoutException {
        if (depth == 0 || board.gameState() != CXGameState.OPEN) {
            return evaluation(board);
        }

        if (maximizingPlayer) {
            int maxScore = Integer.MIN_VALUE;
            for (int col : board.getAvailableColumns()) {
                checktime();
                board.markColumn(col);
                int score;
                //check presence of score of current board in hashMap
                //else insert it
                if (visited.containsKey(board)) {
                    score = visited.get(board);
                } else {
                    score = alphaBetaWithMemory(board, depth - 1, alpha, beta, false, visited);
                    visited.put(board.copy(), score);
                }
                board.unmarkColumn();

                maxScore = Math.max(maxScore, score);
                alpha = Math.max(alpha, maxScore);
                if (alpha >= beta) {
                    break;
                }
            }
            return maxScore;
        } else {
            int minScore = Integer.MAX_VALUE;
            for (int col : board.getAvailableColumns()) {
                checktime();
                board.markColumn(col);
                int score;
                //check presence of score of current board in hashMap
                //else insert it
                if (visited.containsKey(board)) {
                    score = visited.get(board);
                } else {
                    score = alphaBetaWithMemory(board, depth - 1, alpha, beta, true, visited);
                    visited.put(board.copy(), score);
                }
                board.unmarkColumn();

                minScore = Math.min(minScore, score);
                beta = Math.min(beta, minScore);
                if (alpha >= beta) {
                    break;
                }
            }
            return minScore;
        }
    }

    private int evaluation(CXBoard board) {
        int score = 0;
        int countToWin = ToWin;
        int rows = Rows;
        int cols = Columns;

        // Check horizontal lines
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j <= cols - countToWin; j++) {
                int playerCount = 0;
                int opponentCount = 0;
                int emptyCount = 0;

                for (int k = 0; k < countToWin; k++) {
                    CXCellState cellState = board.cellState(i, j + k);
                    if (cellState == CXCellState.FREE) {
                        emptyCount++;
                    } else if ((cellState == CXCellState.P1 && is_first)
                            || (cellState == CXCellState.P2 && !is_first)) {
                        playerCount++;
                    } else if ((cellState == CXCellState.P1 && !is_first)
                            || (cellState == CXCellState.P2 && is_first)) {
                        opponentCount++;
                    }
                }

                if (playerCount > 0 && emptyCount > 0) {
                    score += Math.pow(10, playerCount);
                } else if (opponentCount > 0 && emptyCount > 0) {
                    score -= Math.pow(10, opponentCount);
                }
            }
        }

        // Check vertical lines
        for (int j = 0; j < cols; j++) {
            for (int i = 0; i <= rows - countToWin; i++) {
                int playerCount = 0;
                int opponentCount = 0;
                int emptyCount = 0;

                for (int k = 0; k < countToWin; k++) {
                    CXCellState cellState = board.cellState(i + k, j);
                    if (cellState == CXCellState.FREE) {
                        emptyCount++;
                    } else if ((cellState == CXCellState.P1 && is_first)
                            || (cellState == CXCellState.P2 && !is_first)) {
                        playerCount++;
                    } else if ((cellState == CXCellState.P1 && !is_first)
                            || (cellState == CXCellState.P2 && is_first)) {
                        opponentCount++;
                    }
                }

                if (playerCount > 0 && emptyCount > 0) {
                    score += Math.pow(10, playerCount);
                } else if (opponentCount > 0 && emptyCount > 0) {
                    score -= Math.pow(10, opponentCount);
                }
            }
        }

        // Check diagonal lines (top-left to bottom-right)
        for (int i = 0; i <= rows - countToWin; i++) {
            for (int j = 0; j <= cols - countToWin; j++) {
                int playerCount = 0;
                int opponentCount = 0;
                int emptyCount = 0;

                for (int k = 0; k < countToWin; k++) {
                    CXCellState cellState = board.cellState(i + k, j + k);
                    if (cellState == CXCellState.FREE) {
                        emptyCount++;
                    } else if ((cellState == CXCellState.P1 && is_first)
                            || (cellState == CXCellState.P2 && !is_first)) {
                        playerCount++;
                    } else if ((cellState == CXCellState.P1 && !is_first)
                            || (cellState == CXCellState.P2 && is_first)) {
                        opponentCount++;
                    }
                }

                if (playerCount > 0 && emptyCount > 0) {
                    score += Math.pow(10, playerCount);
                } else if (opponentCount > 0 && emptyCount > 0) {
                    score -= Math.pow(10, opponentCount);
                }
            }
        }

        // Check diagonal lines (bottom-left to top-right)
        for (int i = countToWin - 1; i < rows; i++) {
            for (int j = 0; j <= cols - countToWin; j++) {
                int playerCount = 0;
                int opponentCount = 0;
                int emptyCount = 0;

                for (int k = 0; k < countToWin; k++) {
                    CXCellState cellState = board.cellState(i - k, j + k);
                    if (cellState == CXCellState.FREE) {
                        emptyCount++;
                    } else if ((cellState == CXCellState.P1 && is_first)
                            || (cellState == CXCellState.P2 && !is_first)) {
                        playerCount++;
                    } else if ((cellState == CXCellState.P1 && !is_first)
                            || (cellState == CXCellState.P2 && is_first)) {
                        opponentCount++;
                    }
                }

                if (playerCount > 0 && emptyCount > 0) {
                    score += Math.pow(10, playerCount);
                } else if (opponentCount > 0 && emptyCount > 0) {
                    score -= Math.pow(10, opponentCount);
                }
            }
        }

        if ((board.gameState() == CXGameState.WINP1 && is_first)
                || (board.gameState() == CXGameState.WINP2 && !is_first))
            score = Integer.MAX_VALUE; //player winning
        else if ((board.gameState() == CXGameState.WINP1 && !is_first)
                || (board.gameState() == CXGameState.WINP2 && is_first))
            score = Integer.MIN_VALUE; //player losing
        else if (board.gameState() == CXGameState.DRAW)
            score = 0; //draw

        return score;
    }

    private void checktime() throws TimeoutException {
        long elapsed = System.currentTimeMillis() - START;
        if (elapsed >= TIMEOUT * 1000) {
            throw new TimeoutException();
        }
    }

    public String playerName(){
        return "AxelBrain";
    }    
}
