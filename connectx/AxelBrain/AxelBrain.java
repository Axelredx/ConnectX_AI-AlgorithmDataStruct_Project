/* 
 * "AxelBrain", un progetto di Alex Rossi mat:0001089916;
 *  CST course a.y. 22/23, Alma Mater Studiorum, University of Bologna, Italy.
 *
 *  This  is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details; see <https://www.gnu.org/licenses/>.
 */
package connectx.AxelBrain;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;

import java.util.concurrent.TimeoutException;

import javax.swing.text.Position;

import java.lang.Math;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class AxelBrain implements CXPlayer {
    private Boolean isFirst;
    private Integer Columns;
    private Integer Rows;
    private Integer ToWin;
    private int MAX_BRANCHING = 6; // 6 is nearly perfect against L1
    private int TIMEOUT;
    private long START;
    private int MAX_CACHE_SIZE = 2 * 1024 * 1024 * 1024; // 2gb of memory
    private static final int BYTES_PER_ENTRY = 16; //assuming general object costs 16 bytes
    private Random rand;
    private final int CENTER_COLUMN_WEIGHT = 10;


    public AxelBrain() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        isFirst = first;
        Columns = N;
        Rows = M;
        ToWin = K;
        TIMEOUT = timeout_in_secs;
        rand = new Random(System.currentTimeMillis());
        if(ToWin == 10){
            MAX_BRANCHING = 12; //recalibration for larger boards
        }
    }

    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis(); // Save starting time
        Integer[] availableColumns = B.getAvailableColumns();
        //base column: randomic one from the list of not completly full
        int bestColumn = availableColumns[rand.nextInt(availableColumns.length)];
        int bestScore = Integer.MIN_VALUE;
        LinkedHashMap<CXBoard, Integer> visited = new LinkedHashMap<>();
        Integer[] colOrder = new Integer[Columns];
        for(int i = 0; i < Columns; i++){
            colOrder[i] = Columns/2 + (1-2*(i%2))*(i+1)/2;
        }

        //best starting column if matrix is free
        /*if(ToWin <= 5){
            if(B.numOfMarkedCells() == 0 && ToWin != Rows)
            return availableColumns.length/2;
        }else if(ToWin == 10){
            return 1; //DA FARE
        }*/

        for (int column = 0; column < Columns; column++) {
            if(!B.fullColumn(colOrder[column])){
                int score;
                try {
                    checktime();
                    B.markColumn(colOrder[column]);
                    if(checkGameState(B) == -1)
                        score = findBestMove(B.copy(), MAX_BRANCHING, Integer.MIN_VALUE, Integer.MAX_VALUE, false, visited, colOrder);
                    else
                        score = checkGameState(B);
                    B.unmarkColumn();  
                    if (score > bestScore) {
                    bestScore = score;
                    bestColumn = column;
                }              
                } catch (TimeoutException e) {
                    //System.out.println("Timeout! Returning the best column found so far. :(");
                    return bestColumn;
                }              
            }
        }
        return  bestColumn;
    }

    private int findBestMove(CXBoard board, int depth, int alpha, int beta, boolean maximizingPlayer,
            LinkedHashMap<CXBoard, Integer> visited, Integer[] colOrder) throws TimeoutException {
        if (depth == 0 || board.gameState() != CXGameState.OPEN) {
            return evaluation(board);
        }

        if (maximizingPlayer) {
            int maxScore = Integer.MIN_VALUE;
            for (int column = 0; column < Columns; column++) {
                checktime();
                if(!board.fullColumn(colOrder[column])){
                    board.markColumn(colOrder[column]);
                    int score;
                    //check presence of score of current board in hashMap
                    //else insert it
                    if (visited.containsKey(board)) {
                        score = visited.get(board);
                    } else {
                        score = findBestMove(board.copy(), depth - 1, alpha, beta, false, visited, colOrder);
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
            }
            return maxScore;
        } else {
            int minScore = Integer.MAX_VALUE;
            for (int column = 0; column < Columns; column++) {
                checktime();
                if(!board.fullColumn(colOrder[column])){
                    board.markColumn(colOrder[column]);
                    int score;
                    //check presence of score of current board in hashMap
                    //else insert it
                    if (visited.containsKey(board)) {
                        score = visited.get(board);
                    } else {
                        score = findBestMove(board.copy(), depth - 1, alpha, beta, true, visited, colOrder);
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
        //System.out.println("center column: " + centerCol);
        for (int i = 0; i < rows; i++) {
            if(ToWin <= 5){    
                if (board.cellState(i, centerCol) == CXCellState.P1 && isFirst ||
                    board.cellState(i, centerCol) == CXCellState.P2 && !isFirst) {
                    score += CENTER_COLUMN_WEIGHT;
                } else if (board.cellState(i, centerCol) == CXCellState.P2 && isFirst ||
                            board.cellState(i, centerCol) == CXCellState.P1 && !isFirst) {
                    score -= CENTER_COLUMN_WEIGHT;
                }
             } else if(ToWin == 10){
                //center column recalibration for larger boards
                int colAdjust = 1;
                int colCenterIndex = 10;
                    if(cols == 20){
                        colAdjust = 3;
                    }else if(cols == 30){
                        colAdjust = 13;
                    }else if(cols == 40){
                        colAdjust = 23;
                    }else if(cols == 50){
                        colAdjust = 32;
                    }
                for(int j = 0; j < colAdjust; j++){
                    if (board.cellState(i, colCenterIndex + j) == CXCellState.P1 && isFirst ||
                        board.cellState(i, colCenterIndex + j) == CXCellState.P2 && !isFirst) {
                        score += CENTER_COLUMN_WEIGHT;
                    } else if (board.cellState(i, colCenterIndex + j) == CXCellState.P2 && isFirst ||
                                board.cellState(i, colCenterIndex + j) == CXCellState.P1 && !isFirst) {
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