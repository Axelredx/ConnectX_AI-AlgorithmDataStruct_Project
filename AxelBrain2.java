package connectx.AxelBrain2;

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

public class AxelBrain2 implements CXPlayer{
    private static Boolean is_first;
	private Integer Columns;
	private Integer Rows;
	private Integer ToWin;
    private final int MAX_DEPTH = 6;
    private Random rand;
    private int  TIMEOUT;
    private long START;
    private Integer player;

    public AxelBrain2(){}

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

        //aggiungere x il timing

        return play(B);

    }    

    private boolean checktime(){
		if((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            return true;
        else
            return false;
	}
    
    public int play(CXBoard board) {
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        // Get the available columns
        Integer[] columns = board.getAvailableColumns();
        // Initialize best move to first available column
        int bestMove = columns[columns.length/2];
        // Initialize best score to negative infinity
        int bestScore = Integer.MIN_VALUE;
    
        // Iterate through available columns
        for (int i = 0; i < columns.length; i++) {
            int col = columns[i];
            // Apply move to the board
            board.markColumn(col);
            // Evaluate the move using alpha-beta pruning
            int score = alphabeta(board, MAX_DEPTH, alpha, beta, is_first);
            // Undo the move
            board.unmarkColumn();
    
            // If the score is better than the best score so far, update best score and best move
            if (score > bestScore) {
                bestScore = score;
                bestMove = col;
            }
    
        }
        return bestMove;
    }
    
    public int alphabeta(CXBoard board, int depth, int alpha, int beta, boolean maximizingPlayer) {
        // Base case: if depth limit is reached or game is over, return the score of the board
        if (depth == 0 || board.gameState() != CXGameState.OPEN) {
            if(board.gameState()==CXGameState.WINP1)
                return Integer.MAX_VALUE;
            else if (board.gameState()==CXGameState.WINP2)
                return Integer.MIN_VALUE;
            else if (board.gameState()==CXGameState.DRAW)
                return 0;
            else
                return evaluate(board);
        }
    
        // If maximizing player's turn
        if (maximizingPlayer == true) {
            int maxScore = Integer.MIN_VALUE;
    
            // Iterate through available columns
            for (int col : board.getAvailableColumns()) {
                board.markColumn(col);
                // Recursively evaluate the move
                int score = alphabeta(board, depth - 1, alpha, beta, false);
                // Undo the move
                board.unmarkColumn();
    
                // Update max score
                maxScore = Math.max(maxScore, score);
    
                // Update alpha
                alpha = Math.max(alpha, score);
    
                // Check if beta cutoff is possible
                if (alpha >= beta) {
                    break;
                }
            }
            return maxScore;
        } 
        // If minimizing player's turn
        else {
            int minScore = Integer.MAX_VALUE;
    
            // Iterate through available columns
            for (int col : board.getAvailableColumns()) {
                board.markColumn(col);
                // Recursively evaluate the move
                int score = alphabeta(board, depth - 1, alpha, beta, true);
                // Undo the move
                board.unmarkColumn();
    
                // Update min score
                minScore = Math.min(minScore, score);
    
                // Update beta
                beta = Math.min(beta, score);
    
                // Check if alpha cutoff is possible
                if (alpha >= beta) {
                    break;
                }
            }
            return minScore;
        }
    }

    public int evaluate(CXBoard board){
        int score=0;
        int diffference = Columns - ToWin;

        //horizontal check
        for (int i=0; i < Rows ; i++){
            for (int j=0; j < Columns - diffference; j++){
                int consecutive_count=0;
                for (int k=0; k< ToWin; k++){
                    if(is_first){
                        if(board.cellState(i,j+k)==CXCellState.P1)
                            consecutive_count++;
                        else
                            consecutive_count=0;
                    }else{
                        if(board.cellState(i,j+k)==CXCellState.P2)
                            consecutive_count++;
                        else
                            consecutive_count=0;                        
                    }
                }
                if (consecutive_count > 0) {
                    score += Math.pow(10, consecutive_count);
                }
            }
        }

        //vertical check
        for (int i=0; i < Rows - diffference; i++){
            for (int j=0; j < Columns; j++){ 
                int consecutive_count=0;
                for (int k=0; k< ToWin; k++){
                    if(is_first){
                        if(board.cellState(i+k,j)==CXCellState.P1)
                            consecutive_count++;
                        else
                            consecutive_count=0;
                    }else{
                        if(board.cellState(i+k,j)==CXCellState.P2)
                            consecutive_count++;
                        else
                            consecutive_count=0;                        
                    }                    
                }
                if (consecutive_count > 0) {
                    score += Math.pow(10, consecutive_count);
                }    
            }
        }

        //positive slope diagonal check (low-left to high-right)
        for (int i = 0; i < Rows - diffference; i++){
            for (int j = 0; j< Columns -diffference; j++){
                int consecutive_count=0;
                for (int k=0; k< ToWin; k++){
                    if(is_first){
                        if(board.cellState(i+k,j+k)==CXCellState.P1)
                            consecutive_count++;
                        else
                            consecutive_count=0;
                    }else{
                        if(board.cellState(i+k,j+k)==CXCellState.P2)
                            consecutive_count++;
                        else
                            consecutive_count=0;                        
                    }                    
                }
                if (consecutive_count > 0) {
                    score += Math.pow(10, consecutive_count);
                }               
            }
        }

        //negative slope diagonal check (low-right to high-left)
        for (int i = diffference; i < Rows ; i++){
            for (int j = 0; j< Columns - diffference; j++){
                int consecutive_count=0;
                for (int k=0; k< ToWin; k++){
                    if(is_first){
                        if(board.cellState(i-k,j+k)==CXCellState.P1)
                            consecutive_count++;
                        else
                            consecutive_count=0;
                    }else{
                        if(board.cellState(i-k,j+k)==CXCellState.P2)
                            consecutive_count++;
                        else
                            consecutive_count=0;                        
                    }                    
                }
                if (consecutive_count > 0) {
                    score += Math.pow(10, consecutive_count);
                }               
            }
        }

        return (is_first) ? score : -score;
    }

    public String playerName(){
        return "AxelBrain2";
    }
}