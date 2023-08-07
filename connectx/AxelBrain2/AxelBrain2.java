package connectx.AxelBrain2;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;

import java.util.concurrent.TimeoutException;
import java.lang.Math;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class AxelBrain2 implements CXPlayer {
    private Boolean isFirst;
    private Integer Columns;
    private Integer Rows;
    private Integer ToWin;
    private int MAX_BRANCHING = 8; // if x > 6 excedeed time limit
    private int TIMEOUT;
    private long START;
    private int MAX_CACHE_SIZE = 2 * 1024 * 1024 * 1024; // 2gb of memory
    private static final int BYTES_PER_ENTRY = 16; //assuming general object costs 16 bytes
    private Random rand;
    private final int CENTER_COLUMN_WEIGHT = 10;


    public AxelBrain2() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        isFirst = first;
        Columns = N;
        Rows = M;
        ToWin = K;
        TIMEOUT = timeout_in_secs;
        rand = new Random(System.currentTimeMillis());
    }

    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis(); // Save starting time
        Integer[] availableColumns = B.getAvailableColumns();
        //base column: randomic one from the list of not completly full
        int bestColumn = availableColumns[rand.nextInt(availableColumns.length)];
        int bestScore = Integer.MIN_VALUE;
        LinkedHashMap<CXBoard, Integer> visited = new LinkedHashMap<>();

        //best starting column if matrix is free
        if(B.numOfMarkedCells() == 0 && ToWin != Rows)
            return availableColumns.length/2;

        for (int column : availableColumns) {
            int score;
            try {
                checktime();
                B.markColumn(column);
                if(checkGameState(B) == -1)
                    score = findBestMove(B, MAX_BRANCHING, Integer.MIN_VALUE, Integer.MAX_VALUE, false, visited);
                else
                    score = checkGameState(B);
                B.unmarkColumn();
            
            } catch (TimeoutException e) {
                //System.out.println("Timeout! Returning the best column found so far. :(");
                return bestColumn;
            }
            if (score > bestScore) {
                bestScore = score;
                bestColumn = column;
            }
        }
        return  bestColumn;
    }

    private int findBestMove(CXBoard board, int depth, int alpha, int beta, boolean maximizingPlayer,
            LinkedHashMap<CXBoard, Integer> visited) throws TimeoutException {
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
                    score = findBestMove(board, depth - 1, alpha, beta, false, visited);
                    visited.put(board.copy(), score);
                    //Check if the cache size exceeds the limit, and if so, 
                    //remove the least recently accessed element
                    if (visited.size() * BYTES_PER_ENTRY > MAX_CACHE_SIZE) { 
                        for (Map.Entry<CXBoard, Integer> entry : visited.entrySet()) {
                            visited.remove(entry.getKey());
                            break;
                        }
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
                checktime();
                board.markColumn(col);
                int score;
                //check presence of score of current board in hashMap
                //else insert it
                if (visited.containsKey(board)) {
                    score = visited.get(board);
                } else {
                    score = findBestMove(board, depth - 1, alpha, beta, true, visited);
                    visited.put(board.copy(), score);
                    //Check if the cache size exceeds the limit, and if so, 
                    //remove the least recently accessed element
                    if (visited.size() * BYTES_PER_ENTRY > MAX_CACHE_SIZE) { 
                        for (Map.Entry<CXBoard, Integer> entry : visited.entrySet()) {
                            visited.remove(entry.getKey());
                            break;
                        }
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

    private int evaluation(CXBoard board) {
        int score = 0;
        int countToWin = ToWin;
        int rows = Rows;
        int cols = Columns;

        //check for win, lose or draw
        if(checkGameState(board) != -1)
            return checkGameState(board);

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
            if (board.cellState(i, centerCol) == CXCellState.P1 && isFirst ||
                board.cellState(i, centerCol) == CXCellState.P2 && !isFirst) {
                score += CENTER_COLUMN_WEIGHT;
            } else if (board.cellState(i, centerCol) == CXCellState.P2 && isFirst ||
                        board.cellState(i, centerCol) == CXCellState.P1 && !isFirst) {
                score -= CENTER_COLUMN_WEIGHT;
            }
// DA AGGIUSTARE
            if(ToWin == 10){
                int colAdjust = 0;
                if(cols == 20){
                    colAdjust = 1;
                }else if(cols == 30){
                    colAdjust = 5;
                }else if(cols == 40){
                    colAdjust = 4;
                }else if(cols == 50){
                    colAdjust = 1;
                }

                for(int j = 0; j < colAdjust; j++){
                    if (board.cellState(i + j, centerCol) == CXCellState.P1 && isFirst ||
                        board.cellState(i +j, centerCol) == CXCellState.P2 && !isFirst) {
                        score += CENTER_COLUMN_WEIGHT;
                    } else if (board.cellState(i + j, centerCol) == CXCellState.P2 && isFirst ||
                                board.cellState(i + j, centerCol) == CXCellState.P1 && !isFirst) {
                        score -= CENTER_COLUMN_WEIGHT;
                    } else if (board.cellState(i - j, centerCol) == CXCellState.P1 && isFirst ||
                                board.cellState(i - j, centerCol) == CXCellState.P2 && !isFirst) {
                        score += CENTER_COLUMN_WEIGHT;
                    } else if (board.cellState(i - j, centerCol) == CXCellState.P2 && isFirst ||
                                board.cellState(i - j, centerCol) == CXCellState.P1 && !isFirst) {
                        score -= CENTER_COLUMN_WEIGHT;
                    }                
                }                
            }
        } 

        return score;
    }

    int checkGameState(CXBoard board){
        if ((board.gameState() == CXGameState.WINP1 && isFirst)
                || (board.gameState() == CXGameState.WINP2 && !isFirst))
            return Integer.MAX_VALUE; //player winning
        else if ((board.gameState() == CXGameState.WINP1 && !isFirst)
                || (board.gameState() == CXGameState.WINP2 && isFirst))
            return Integer.MIN_VALUE; //player losing
        else if (board.gameState() == CXGameState.DRAW)
            return 0; //draw

        return -1;
    }

    private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

    public String playerName(){
        return "AxelBrain";
    }    
}