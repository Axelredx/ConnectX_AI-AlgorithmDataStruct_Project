package connectx.AxelBrain2;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCellState;

import java.util.concurrent.TimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AxelBrain2 implements CXPlayer {
    private boolean isFirst;
    private int columns;
    private int rows;
    private int toWin;
    private int maxDepth = 6; // Increase depth for larger board
    private int maxBranching = 10; // Increase branching factor for larger board
    private int timeout;
    private long startTime;
    private int maxCacheSize = 2 * 1024 * 1024 * 1024; // 2GB of memory
    private static final int BYTES_PER_ENTRY = 16; // assuming general object costs 16 bytes
    private Random random;
    private final int centerColumnWeight = 10;

    public AxelBrain2() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        isFirst = first;
        columns = N;
        rows = M;
        toWin = K;
        timeout = timeout_in_secs;
        random = new Random(System.currentTimeMillis());
    }

    public int selectColumn(CXBoard board) {
        startTime = System.currentTimeMillis(); // Save starting time
        Integer[] availableColumns = board.getAvailableColumns();
        // base column: randomic one from the list of not completely full
        int bestColumn = availableColumns[random.nextInt(availableColumns.length)];
        int bestScore = Integer.MIN_VALUE;

        for (int column : availableColumns) {
            int score;
            try {
                checkTime();
                board.markColumn(column);
                score = findBestMove(board);
                board.unmarkColumn();
            } catch (TimeoutException e) {
                //System.out.println("Timeout! Returning the best column found so far. :(");
                return bestColumn;
            }
            if (score > bestScore) {
                bestScore = score;
                bestColumn = column;
            }
        }
        return bestColumn;
    }

    public int findBestMove(CXBoard board) throws TimeoutException {
        int bestScore = Integer.MIN_VALUE;

        for (int depth = 1; depth <= maxDepth; depth++) {
            checkTime();
            int score = iterativeDeepening(board, depth, Integer.MIN_VALUE, Integer.MAX_VALUE);
            if (score > bestScore) {
                bestScore = score;
            }
        }

        return bestScore;
    }

    public int iterativeDeepening(CXBoard board, int depth, int alpha, int beta) throws TimeoutException {
        Map<Long, Integer> visited = new HashMap<>();
        int bestScore = Integer.MIN_VALUE;

        while (depth <= maxBranching) {
            checkTime();
            int score;
            long hash = generateBoardHash(board);
            if (visited.containsKey(hash)) {
                score = visited.get(hash);
            } else {
                score = alphaBetaWithMemory(board, depth, alpha, beta, true, visited);
                visited.put(hash, score);
                // Check if the cache size exceeds the limit, and if so,
                // remove the least recently accessed elements
                if (visited.size() * BYTES_PER_ENTRY > maxCacheSize) {
                    int numToRemove = Math.max(visited.size() - maxCacheSize / BYTES_PER_ENTRY, 1);
                    visited.entrySet().removeIf(entry -> entry.getKey() != hash);
                }
            }
            if (score == Integer.MAX_VALUE || score == Integer.MIN_VALUE || score == 0) {
                // Found a winning move, stop searching
                return score;
            }
            bestScore = score;
            // depth increase each time
            depth++;

            if (bestScore >= beta) {
                // Prune remaining branches
                break;
            }
        }

        return bestScore;
    }

    private int alphaBetaWithMemory(CXBoard board, int depth, int alpha, int beta, boolean maximizingPlayer,
                                    Map<Long, Integer> visited) throws TimeoutException {
        if (depth == 0 || board.gameState() != CXGameState.OPEN) {
            return evaluation(board);
        }

        if (maximizingPlayer) {
            int maxScore = Integer.MIN_VALUE;
            for (int col : board.getAvailableColumns()) {
                checkTime();
                board.markColumn(col);
                int score;
                long hash = generateBoardHash(board);
                if (visited.containsKey(hash)) {
                    score = visited.get(hash);
                } else {
                    score = alphaBetaWithMemory(board, depth - 1, alpha, beta, false, visited);
                    visited.put(hash, score);
                    // Check if the cache size exceeds the limit, and if so,
                    // remove the least recently accessed elements
                    if (visited.size() * BYTES_PER_ENTRY > maxCacheSize) {
                        int numToRemove = Math.max(visited.size() - maxCacheSize / BYTES_PER_ENTRY, 1);
                        visited.entrySet().removeIf(entry -> entry.getKey() != hash);
                    }
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
                checkTime();
                board.markColumn(col);
                int score;
                long hash = generateBoardHash(board);
                if (visited.containsKey(hash)) {
                    score = visited.get(hash);
                } else {
                    score = alphaBetaWithMemory(board, depth - 1, alpha, beta, true, visited);
                    visited.put(hash, score);
                    // Check if the cache size exceeds the limit, and if so,
                    // remove the least recently accessed elements
                    if (visited.size() * BYTES_PER_ENTRY > maxCacheSize) {
                        int numToRemove = Math.max(visited.size() - maxCacheSize / BYTES_PER_ENTRY, 1);
                        visited.entrySet().removeIf(entry -> entry.getKey() != hash);
                    }
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

    private long generateBoardHash(CXBoard board) {
        // Generate a unique hash code for the current board state
        long hash = 0L;
        int cols = columns;
        int rows = this.rows;

        // Zobrist hashing
        long[][][] zobristTable = new long[rows][cols][3];
        Random random = new Random();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                zobristTable[i][j][0] = random.nextLong();
                zobristTable[i][j][1] = random.nextLong();
                zobristTable[i][j][2] = random.nextLong();
            }
        }

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board.cellState(i, j) == CXCellState.P1) {
                    hash ^= zobristTable[i][j][0];
                } else if (board.cellState(i, j) == CXCellState.P2) {
                    hash ^= zobristTable[i][j][1];
                }
            }
        }

        return hash;
    }

    private int evaluation(CXBoard board) {
            int score = 0;
            int countToWin = toWin;
            int rows = this.rows;
            int cols = this.columns;
        
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
                        } else if ((cellState == CXCellState.P1 && isFirst)
                                || (cellState == CXCellState.P2 && !isFirst)) {
                            playerCount++;
                        } else if ((cellState == CXCellState.P1 && !isFirst)
                                || (cellState == CXCellState.P2 && isFirst)) {
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
                        } else if ((cellState == CXCellState.P1 && isFirst)
                                || (cellState == CXCellState.P2 && !isFirst)) {
                            playerCount++;
                        } else if ((cellState == CXCellState.P1 && !isFirst)
                                || (cellState == CXCellState.P2 && isFirst)) {
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
                        } else if ((cellState == CXCellState.P1 && isFirst)
                                || (cellState == CXCellState.P2 && !isFirst)) {
                            playerCount++;
                        } else if ((cellState == CXCellState.P1 && !isFirst)
                                || (cellState == CXCellState.P2 && isFirst)) {
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
                        } else if ((cellState == CXCellState.P1 && isFirst)
                                || (cellState == CXCellState.P2 && !isFirst)) {
                            playerCount++;
                        } else if ((cellState == CXCellState.P1 && !isFirst)
                                || (cellState == CXCellState.P2 && isFirst)) {
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
        
            // Consider center columns to be more valuable
            int centerCol = cols / 2;
            for (int i = 0; i < rows; i++) {
                if (board.cellState(i, centerCol) == CXCellState.P1 && isFirst) {
                    score += centerColumnWeight;
                } else if (board.cellState(i, centerCol) == CXCellState.P2 && !isFirst) {
                    score -= centerColumnWeight;
                }
            }
        
            // Winning and losing states
            if ((board.gameState() == CXGameState.WINP1 && isFirst)
                    || (board.gameState() == CXGameState.WINP2 && !isFirst))
                score = Integer.MAX_VALUE; // player winning
            else if ((board.gameState() == CXGameState.WINP1 && !isFirst)
                    || (board.gameState() == CXGameState.WINP2 && isFirst))
                score = Integer.MIN_VALUE; // player losing
            else if (board.gameState() == CXGameState.DRAW)
                score = 0; // draw
        
            return score;
        
    }

    private void checkTime() throws TimeoutException {
        if ((System.currentTimeMillis() - startTime) / 1000.0 >= timeout * (99.0 / 100.0))
            throw new TimeoutException();
    }

    public String playerName() {
        return "AxelBrain2";
    }
}
