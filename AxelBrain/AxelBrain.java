package connectx.AxelBrain;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;
import java.util.TreeSet;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.lang.Math;

public class AxelBrain implements CXPlayer{
    private static Boolean is_first;
	private Integer Columns;
	private Integer Rows;
	private Integer ToWin;
    private final int MAX_DEPTH = 7;
    private int  TIMEOUT;
    private long START;
    private Integer player;
    private CXGameState myWin;

    public AxelBrain(){}

	// M = rows
	// N = columns
	// X = allignement number to win
	// first = True if it is the first player, False otherwise
	// timeout_in_secs = max time in seconds for each plays
 
    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        is_first=first;
        Columns=N;
        Rows=M;
        ToWin=K;
        TIMEOUT = timeout_in_secs;
        myWin   = first ? CXGameState.WINP1 : CXGameState.WINP2;
    }

    public int selectColumn(CXBoard B){
        START = System.currentTimeMillis(); // Save starting time
        /*Integer[] L = B.getAvailableColumns();
        int col = singleMoveWin(B,L);
        if(col!=-1)
            return col;
        else*/
            return play(B,MAX_DEPTH);

    }  

    private boolean checktime(){
		if((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            return true;
        else
            return false;
	}
    
	/**
	 * Check if we can win in a single move
	 *
	 * Returns the winning column if there is one, otherwise -1
	 	
	private int singleMoveWin(CXBoard B, Integer[] L) {
        for(int i : L) {
            if(checktime())
                break; // Check timeout at every iteration
            CXGameState state = B.markColumn(i);
            if (state == myWin)
                return i; // Winning column found: return immediately
            B.unmarkColumn();
        }
            return -1;
    }*/

    public int play(CXBoard board, int maxDepth) {
        // Get the available columns
        Integer[] columns = board.getAvailableColumns();
        // Initialize best move to first available column
        int bestMove = columns[columns.length/2];    
        // If only one column is available, return it immediately
        if (columns.length == 1) {
            return bestMove;
        }    
        // Initialize best score to negative infinity
        int bestScore = Integer.MIN_VALUE;
    
        // Iterate through available columns
        for (int i = 0; i < columns.length; i++) {
            int col = columns[i];   
            // Apply move to the board
            board.markColumn(col);   
            // Evaluate the move using iterative deepening
            int score = iterativeDeepening(board, maxDepth);   
            // Undo the move
            board.unmarkColumn();   
            // If the score is better than the bestscore so far, 
            //update best score and best move
            if (score > bestScore) {
                bestScore = score;
                bestMove = col;
            }
        }
    
        return bestMove;
    }
    
    public int iterativeDeepening(CXBoard board, int maxDepth) {
        // Initialize best score to negative infinity
        int bestScore = Integer.MIN_VALUE;
    
        // Iterate through depths
        for (int depth = 0; depth < maxDepth; depth++) {
            //check time every iteration
            if(checktime())
                break;
            // Evaluate the move using alpha-beta pruning with current depth
            int score = alphabeta(board.copy(), depth, Integer.MIN_VALUE, Integer.MAX_VALUE, board.currentPlayer() - 1, is_first);    
            // If the score is better than the best score so far, update best score
            if (score > bestScore) {
                bestScore = score;
            }
        }
    
        return bestScore;
    }
    
    public int alphabeta(CXBoard board, int depth, int alpha, int beta, int player, boolean maximizingPlayer) {
        // Base case: if depth limit is reached or game is over, return the score of the board
        if (depth == 0 || board.gameState() != CXGameState.OPEN) {
            return evaluate(board, player);
        }
    
        // If maximizing player's turn
        if (maximizingPlayer) {
            int maxScore = Integer.MIN_VALUE;
            // Iterate through available columns
            for (int col : board.getAvailableColumns()) {
                if(checktime())
                break;
                // Apply move to the board
                board.markColumn(col);
                // Recursively evaluate the move
                int score = alphabeta(board.copy(), depth - 1, alpha, beta, player, false);
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
                if(checktime())
                break;
                // Apply move to the board
                board.markColumn(col);
                // Recursively evaluate the move
                int score = alphabeta(board.copy(), depth - 1, alpha, beta, player, true);
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
    public int evaluate(CXBoard board, int player) {
        // If the game is a tie, return 0
        if (board.gameState() == CXGameState.DRAW) {
        return 0;
        }
        // If the game is won by the player1, return a high score
        if (board.gameState() == CXGameState.WINP1 && player == 1 ||
                board.gameState() == CXGameState.WINP2 && player == 2) {
            return Integer.MAX_VALUE;
        }
        // If the game is won by the player2, return a low score
        if (board.gameState() == CXGameState.WINP1 && player == 2 ||
                board.gameState() == CXGameState.WINP2 && player == 1) {
            return Integer.MIN_VALUE;
        }

        // Otherwise, calculate the score based on the number of marked cells in each row, 
        //column and diagonal
        /*int score = 0;
        // Evaluate rows
        for (int i = 0; i < Rows; i++) {
            int numPlayer1Cells = 0;
            int numPlayer2Cells = 0;

            for (int j = 0; j < Columns; j++) {
                if (board.cellState(i, j) == CXCellState.P1) {
                    numPlayer1Cells++;
                } else if (board.cellState(i, j) == CXCellState.P2) {
                    numPlayer2Cells++;
                }
            }

            score += evaluateLine(numPlayer1Cells, numPlayer2Cells);
        }

        // Evaluate columns
        for (int j = 0; j < Columns; j++) {
            int numPlayer1Cells = 0;
            int numPlayer2Cells = 0;

            for (int i = 0; i < Rows; i++) {
                if (board.cellState(i, j) == CXCellState.P1) {
                    numPlayer1Cells++;
                } else if (board.cellState(i, j) == CXCellState.P2) {
                    numPlayer2Cells++;
                }
            }

            score += evaluateLine(numPlayer1Cells, numPlayer2Cells);
        }

        // Evaluate diagonals
        for (int i = 0; i < Rows; i++) {
            for (int j = 0; j < Columns; j++) {
                if (i + 3 < Rows && j + 3 < Columns) {
                    int numPlayer1Cells = 0;
                    int numPlayer2Cells = 0;

                    for (int k = 0; k < 4; k++) {
                        if (board.cellState(i + k, j + k) == CXCellState.P1) {
                            numPlayer1Cells++;
                        } else if (board.cellState(i + k, j + k) == CXCellState.P2) {
                            numPlayer2Cells++;
                        }
                    }

                    score += evaluateLine(numPlayer1Cells, numPlayer2Cells);
                }

                if (i + 3 < Rows && j - 3 >= 0) {
                    int numPlayer1Cells = 0;
                    int numPlayer2Cells = 0;

                    for (int k = 0; k < 4; k++) {
                        if (board.cellState(i + k, j - k) == CXCellState.P1) {
                            numPlayer1Cells++;
                        } else if (board.cellState(i + k, j - k) == CXCellState.P2) {
                            numPlayer2Cells++;
                        }
                    }
                    score += evaluateLine(numPlayer1Cells, numPlayer2Cells);
                }
            }
        }        */
        int score=0;
        // difference is necessary for unordinary board (for example for 6x7 board is equal to 3)
        int difference = Columns - ToWin;

        //horizontal check
        for (int i=0; i < Rows ; i++){
            for (int j=0; j < Columns - difference; j++){
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
        for (int i=0; i < Rows - difference; i++){
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
        for (int i = 0; i < Rows - difference; i++){
            for (int j = 0; j< Columns -difference; j++){
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
        for (int i = difference; i < Rows ; i++){
            for (int j = 0; j< Columns - difference; j++){
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

        return (is_first ) ? score : -score;
        // Return the calculated score
       // return score;  
        
        
    }

    private int evaluateLine(int numPlayer1Cells, int numPlayer2Cells) {
        int score = 0;
        /* 
        // If there are no player 2 cells in the line, 
        //return a score based on the number of player 1 cells
        if (numPlayer2Cells == 0) {
            /*switch (numPlayer1Cells) {
                case 1:
                    score = 1;
                    break;
                case 2:
                    score = 10;
                    break;
                case 3:
                    score = 100;
                    break;
                case 4:
                    score = 1000;
                    break;
            }//
            
                if(numPlayer1Cells==1)
                    score=1;
                else{
                    double tmp_scr = Math.pow(10,numPlayer1Cells);
                    score = (int)tmp_scr;
                }
            
        }
        // If there are no player 1 cells in the line, 
        //return a negative score based on the number of player 2 cells
        else if (numPlayer1Cells == 0) {
            /*switch (numPlayer2Cells) {
                case 1:
                    score = -1;
                    break;
                case 2:
                    score = -10;
                    break;
                case 3:
                    score = -100;
                    break;
                case 4:
                    score = -1000;
                    break;
            }//
            
                if(numPlayer2Cells==1)
                    score=-1;
                else{
                    double tmp_scr = Math.pow(10,numPlayer2Cells);
                    score =  (int)tmp_scr;
                    score = score * (-1);
            }
        }*/

        if(is_first){
            double tmp_scr = Math.pow(10,numPlayer1Cells);
            score = (int)tmp_scr;
        }else{
            double tmp_scr = Math.pow(10,numPlayer2Cells);
            score = (int)tmp_scr;            
        }
    
        return (is_first) ? score : -score;
    }
    
    public String playerName(){
        return "AxelBrain";
    }
}