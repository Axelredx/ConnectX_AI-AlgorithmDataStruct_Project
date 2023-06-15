package connectx.AxelBrain3;
import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;
import java.util.Random;
import java.util.TreeSet;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import javax.lang.model.util.ElementScanner6;
import java.lang.Math;
import java.util.HashMap;

public class AxelBrain3 implements CXPlayer{
    private Integer Columns;
    private Integer Rows;
    private Boolean is_first;
    private final int MAX_DEPTH = 5;
    private int  TIMEOUT;
    private long START;  
    private Integer ToWin;  

    public AxelBrain3(){}

	// M = numero di righe nella matrice
	// N = numero di colonne nella matrice
	// X = numero di gettoni da allineare
	// first = true se è il primo a giocare
	// timeout_in_secs = numero massimo di secondi per una mossa
 
    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        is_first=first;
        Columns=N;
        Rows=M;
        ToWin=K;
        TIMEOUT = timeout_in_secs;
    }

    public int selectColumn(CXBoard B){
        START = System.currentTimeMillis(); // Save starting time
        return findBestMove(B);
    } 

    public int findBestMove(CXBoard board){
        Integer[] col_avaible = board.getAvailableColumns();
        int best_move = col_avaible[col_avaible.length/2];
        int best_score = -1;
        int alpha = Integer.MAX_VALUE;
        int beta = Integer.MIN_VALUE;
        // HashMap to store board positions and scores (this type of hashtable can store null by default)
        HashMap<CXBoard, Integer> scoreMap = new HashMap<>();

        //best move if matrix is empty or 1 cell is marked by opponent
        if(board.numOfMarkedCells()==0)
            return col_avaible.length/2;

        for(int i : board.getAvailableColumns()){
            //check time every iteration
            if(checktime())
                break;
            board.markColumn(i);
            int score = alphabeta(board,MAX_DEPTH,alpha,beta,false, scoreMap);
            board.unmarkColumn();

            if(score > best_score){
                best_score = score;
                best_move = i;
            }

        }
        return best_move;
    }

    /*public int iterativeDeepening(CXBoard board) {
        int alpha = Integer.MAX_VALUE;
        int beta = Integer.MIN_VALUE;
        int best_score = Integer.MIN_VALUE;
        // HashMap to store board positions and scores (this type of hashtable can store null by default)
        HashMap<CXBoard, Integer> scoreMap = new HashMap<>();
    
        // Iterate through max_depth (higher depth, higher is the number visited in width)
        for (int depth = 0; depth < MAX_DEPTH; depth++) {
            //check time every iteration
            if(checktime())
                break;
            // Evaluate the move using alpha-beta pruning with a branching depth
            int score = alphabeta(board,MAX_BRANCHING,alpha,beta,false, scoreMap);    
            // If the score is better than the best score so far, update best score
            if (score > best_score) {
                best_score = score;
            }
        }
    
        return best_score;
    }*/

    private int alphabeta(CXBoard board, int depth, int alpha, int beta, boolean maximizing, HashMap<CXBoard, Integer> scoreMap){
        // Check if the board position's score is already in the HashMap
        Integer map_score = scoreMap.get(board); 
        if (map_score != null) {
            return map_score;
        }

        //terminal node, return of heuristic evaluation
        if(depth == 0 || board.gameState() != CXGameState.OPEN){  
            return evaluation(board);
        }

        if(maximizing){
            int max_value = Integer.MIN_VALUE;
            for(int i : board.getAvailableColumns()){
                //check time every iteration
                if(checktime())
                    break;
                board.markColumn(i);
                int score = alphabeta(board, depth - 1, alpha, beta, false, scoreMap);
                board.unmarkColumn();

                max_value = Math.max(max_value, score);
                alpha = Math.max(alpha, max_value);
                if(alpha >= beta)
                    break;
            }

            return max_value;
        }else{
            int min_value = Integer.MAX_VALUE;
            for(int i : board.getAvailableColumns()){
                //check time every iteration
                if(checktime())
                    break;
                board.markColumn(i);
                int score = alphabeta(board, depth - 1, alpha, beta, true, scoreMap);
                board.unmarkColumn();

                min_value = Math.min(min_value, score);
                beta = Math.min(beta, min_value);
                if(alpha >= beta)
                    break;
            }

            return min_value;            
        }
    }

//versione migliore in AxelBrain2
    private int evaluation(CXBoard board){
        int score = 0;
        int player = board.currentPlayer();
        int difference = ToWin - 1;

        if (board.gameState() == CXGameState.WINP1 && is_first) {
            return Integer.MAX_VALUE;
        } else if (board.gameState() == CXGameState.WINP2 && !is_first) {
            return Integer.MAX_VALUE;
        } else if (board.gameState() == CXGameState.WINP1 && !is_first) {
            return Integer.MIN_VALUE;
        } else if (board.gameState() == CXGameState.WINP2 && is_first) {
            return Integer.MIN_VALUE;
        } else if (board.gameState() == CXGameState.DRAW) {
            return 0;
        }
    
        /*int ai_consecutive_vertical_count = 0;
        //prioritize center column play
        for(int i = 0; i < Rows; i++){
            if(is_first){
                if(board.cellState(i,Columns/2)==CXCellState.P1)
                    ai_consecutive_vertical_count++;
            }else{
                if(board.cellState(i,Columns/2)==CXCellState.P2)
                    ai_consecutive_vertical_count++;
            }
        }
        if(ai_consecutive_vertical_count>2)
            score+=ai_consecutive_vertical_count*3;*/

        // horizontal check
        for (int i = 0; i < Rows; i++){
            for (int j = 0; j < Columns - difference; j++){
                int ai_consecutive_count = 0;
                int oppo_consecutive_count = 0;
                for (int k = 0; k < ToWin; k++){
                    if (board.cellState(i, j+k) == CXCellState.P1) {
                        if(is_first)
                            ai_consecutive_count++;
                        else
                            oppo_consecutive_count++;
                    } else if (board.cellState(i, j+k) == CXCellState.P2) {
                        if(is_first)
                            oppo_consecutive_count++;
                        else
                            ai_consecutive_count++;
                    }
                }
                score += evaluateLine(ai_consecutive_count, oppo_consecutive_count, player);

            }
        }
    
        // vertical check
        for (int i = 0; i < Rows - difference; i++){
            for (int j = 0; j < Columns; j++){
                int ai_consecutive_count = 0;
                int oppo_consecutive_count = 0;
                for (int k = 0; k < ToWin; k++){
                    if (board.cellState(i+k, j) == CXCellState.P1) {
                        if(is_first)
                            ai_consecutive_count++;
                        else
                            oppo_consecutive_count++;
                    } else if (board.cellState(i+k, j) == CXCellState.P2) {
                        if(is_first)
                            oppo_consecutive_count++;
                        else
                            ai_consecutive_count++;
                    }
                }
                score += evaluateLine(ai_consecutive_count, oppo_consecutive_count, player);
            }
        }
    
        // diagonal check (top-left to bottom-right)
        for (int i = 0; i < Rows - difference; i++){
            for (int j = 0; j < Columns - difference; j++){
                int ai_consecutive_count = 0;
                int oppo_consecutive_count = 0;
                for (int k = 0; k < ToWin; k++){
                    if (board.cellState(i+k, j+k) == CXCellState.P1) {
                        if(is_first)
                            ai_consecutive_count++;
                        else
                            oppo_consecutive_count++;
                    } else if (board.cellState(i+k, j+k) == CXCellState.P2) {
                        if(is_first)
                            oppo_consecutive_count++;
                        else
                            ai_consecutive_count++;
                    }
                }
                score += evaluateLine(ai_consecutive_count, oppo_consecutive_count, player);
            }
        }
    
        // diagonal check (bottom-left to top-right)
        for (int i = difference; i < Rows; i++){
            for (int j = 0; j < Columns - difference; j++){
                int ai_consecutive_count = 0;
                int oppo_consecutive_count = 0;
                for (int k = 0; k < ToWin; k++){
                    if (board.cellState(i-k, j+k) == CXCellState.P1) {
                        if(is_first)
                            ai_consecutive_count++;
                        else
                            oppo_consecutive_count++;
                    } else if (board.cellState(i-k, j+k) == CXCellState.P2) {
                        if(is_first)
                            oppo_consecutive_count++;
                        else
                            ai_consecutive_count++;
                    }

                }
                score += evaluateLine(ai_consecutive_count, oppo_consecutive_count, player);
            }
        }

        return score; //(is_first)? score:-score;
    }

    private int evaluateLine(int playerCount, int opponentCount, int currentPlayer) {
        int score = 0;
    
        if (playerCount > 0 && opponentCount == 0) {
            // Player has a line without opponent's pieces
            score += Math.pow(10, playerCount);
        } else if (playerCount == 0 && opponentCount > 0) {
            // Opponent has a line without player's pieces
            score -= Math.pow(10, opponentCount);
        } else if (playerCount > 0 && opponentCount > 0) {
            // Both player and opponent have pieces in the line
            // Adjust score based on the current player's advantage
            if (currentPlayer == 1) {
                score += Math.pow(10, playerCount - 1);
            } else {
                score -= Math.pow(10, opponentCount - 1);
            }
        }
    
        // Additional considerations
        if (playerCount + opponentCount >= 2) {
            // Encourage making longer lines
            int lineLength = playerCount + opponentCount;
            score += Math.pow(10, lineLength - 2);
        }
    
        return score;
    }
    

    private boolean checktime(){
		return ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0));
	}
    
    public String playerName(){
        return "AxelBrain3";
    }
}
