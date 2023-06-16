package connectx.AxelBrain1;

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

public class AxelBrain1 implements CXPlayer{
    private Boolean is_first;
	private Integer Columns;
	private Integer Rows;
	private Integer ToWin;
    private int MAX_DEPTH = 100;
    private int MAX_BRANCHING = 3;
    private int  TIMEOUT;
    private long START;

    public AxelBrain1(){}

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
        return play(B);
    }    

    public int play(CXBoard board){
        Integer[] col_avaible = board.getAvailableColumns();
        int best_move = col_avaible[col_avaible.length/2];
        int best_score = Integer.MIN_VALUE;

        for(int i=0; i < col_avaible.length; i++){
            int col = col_avaible[i];
            board.markColumn(col);
            int score = iterativeDeepening(board);
            board.unmarkColumn();

            if(score > best_score){
                best_score = score;
                best_move = col;
            }

        }
        return best_move;
    }

    public int iterativeDeepening(CXBoard board) {
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
    }

    private int alphabeta(CXBoard board, int depth, int alpha, int beta, boolean maximizing, HashMap<CXBoard, Integer> scoreMap){
        // Check if the board position's score is already in the HashMap
        Integer map_score = scoreMap.get(board); 
        if (map_score != null) {
            return map_score;
        }

        if(depth == 0 || board.gameState() != CXGameState.OPEN){
            /*if(board.gameState() != CXGameState.OPEN){
                if(is_first){
                    if(board.gameState()==CXGameState.WINP1)
                        return Integer.MAX_VALUE;
                    else if(board.gameState()==CXGameState.WINP2)
                        return Integer.MIN_VALUE;
                    else if(board.gameState()==CXGameState.DRAW)
                        //draw
                        return 0;
                }else{
                    if(board.gameState()==CXGameState.WINP2)
                        return Integer.MAX_VALUE;
                    else if(board.gameState()==CXGameState.WINP1)
                        return Integer.MIN_VALUE;
                    else if(board.gameState()==CXGameState.DRAW)
                        //draw
                        return 0;                    
                }
            }else*/
                return evaluation(board);
        }

        if(maximizing){
            int max_value = Integer.MIN_VALUE;
            for(int i : board.getAvailableColumns()){
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

    private int evaluation(CXBoard board){
        int score = 0;
        int difference = ToWin - 1;

        //higher evaluation for center column of the matrix
        for(int i=0; i < Rows - difference ;i++){
                int free_cells = 0;
                boolean ai_consecutive_piece = true;
                boolean oppo_consecutive_piece = true;
                int ai_true_consecutive_piece = 0;
                int oppo_true_consecutive_piece = 0; 
                for(int k = 0; k < ToWin; k++){
                    if(is_first){
                        if(board.cellState(i+k, Columns/2) == CXCellState.P1){
                            oppo_consecutive_piece=false;
                            if(ai_consecutive_piece==true)
                                ai_true_consecutive_piece+=1;
                        }
                        else if(board.cellState(i+k, Columns/2) == CXCellState.FREE)
                            free_cells+=1;
                        else{
                            ai_consecutive_piece=false;
                            if(oppo_consecutive_piece==true)
                                oppo_true_consecutive_piece+=1;
                        }
                    }else{
                        if(board.cellState(i+k, Columns/2) == CXCellState.P2){
                            oppo_consecutive_piece=false;
                            if(ai_consecutive_piece==true)
                                ai_true_consecutive_piece+=1;
                        }
                        else if(board.cellState(i+k, Columns/2) == CXCellState.FREE)
                            free_cells+=1;
                        else{
                            ai_consecutive_piece=false;
                            if(oppo_consecutive_piece==true)
                                oppo_true_consecutive_piece+=1;
                        }
                    }
                }
                score+=10*counting_consecutive_count(ToWin,free_cells,ai_consecutive_piece,oppo_consecutive_piece,ai_true_consecutive_piece,oppo_true_consecutive_piece);
        }
    
        // horizontal check
        for (int i = 0; i < Rows; i++){
            for (int j = 0; j < Columns - difference; j++){
                int free_cells = 0;
                boolean ai_consecutive_piece = true;
                boolean oppo_consecutive_piece = true;
                int ai_true_consecutive_piece = 0;
                int oppo_true_consecutive_piece = 0; 
                for(int k = 0; k < ToWin; k++){
                    if(is_first){
                        if(board.cellState(i, j+k) == CXCellState.P1){
                            oppo_consecutive_piece=false;
                            if(ai_consecutive_piece==true)
                                ai_true_consecutive_piece+=1;
                        }
                        else if(board.cellState(i, j+k) == CXCellState.FREE)
                            free_cells+=1;
                        else{
                            ai_consecutive_piece=false;
                            if(oppo_consecutive_piece==true)
                                oppo_true_consecutive_piece+=1;
                        }
                    }else{
                        if(board.cellState(i, j+k) == CXCellState.P2){
                            oppo_consecutive_piece=false;
                            if(ai_consecutive_piece==true)
                                ai_true_consecutive_piece+=1;
                        }
                        else if(board.cellState(i, j+k) == CXCellState.FREE)
                            free_cells+=1;
                        else{
                            ai_consecutive_piece=false;
                            if(oppo_consecutive_piece==true)
                                oppo_true_consecutive_piece+=1;
                        }
                    }
                }
                score+=counting_consecutive_count(ToWin,free_cells,ai_consecutive_piece,oppo_consecutive_piece,ai_true_consecutive_piece,oppo_true_consecutive_piece);
            }
        }
    
        // vertical check
        for (int i = 0; i < Rows - difference; i++){
            for (int j = 0; j < Columns; j++){
                int free_cells = 0;
                boolean ai_consecutive_piece = true;
                boolean oppo_consecutive_piece = true;
                int ai_true_consecutive_piece = 0;
                int oppo_true_consecutive_piece = 0; 
                for(int k = 0; k < ToWin; k++){
                    if(is_first){
                        if(board.cellState(i+k, j) == CXCellState.P1){
                            oppo_consecutive_piece=false;
                            if(ai_consecutive_piece==true)
                                ai_true_consecutive_piece+=1;
                        }
                        else if(board.cellState(i+k, j) == CXCellState.FREE)
                            free_cells+=1;
                        else{
                            ai_consecutive_piece=false;
                            if(oppo_consecutive_piece==true)
                                oppo_true_consecutive_piece+=1;
                        }
                    }else{
                        if(board.cellState(i+k, j) == CXCellState.P2){
                            oppo_consecutive_piece=false;
                            if(ai_consecutive_piece==true)
                                ai_true_consecutive_piece+=1;
                        }
                        else if(board.cellState(i+k, j) == CXCellState.FREE)
                            free_cells+=1;
                        else{
                            ai_consecutive_piece=false;
                            if(oppo_consecutive_piece==true)
                                oppo_true_consecutive_piece+=1;
                        }
                    }
                }
                score+=counting_consecutive_count(ToWin,free_cells,ai_consecutive_piece,oppo_consecutive_piece,ai_true_consecutive_piece,oppo_true_consecutive_piece);
            }
        }
    
        // diagonal check (top-left to bottom-right)
        for (int i = 0; i < Rows - difference; i++){
            for (int j = 0; j < Columns - difference; j++){
                int free_cells = 0;
                boolean ai_consecutive_piece = true;
                boolean oppo_consecutive_piece = true;
                int ai_true_consecutive_piece = 0;
                int oppo_true_consecutive_piece = 0; 
                for(int k = 0; k < ToWin; k++){
                    if(is_first){
                        if(board.cellState(i+k, j+k) == CXCellState.P1){
                            oppo_consecutive_piece=false;
                            if(ai_consecutive_piece==true)
                                ai_true_consecutive_piece+=1;
                        }
                        else if(board.cellState(i+k, j+k) == CXCellState.FREE)
                            free_cells+=1;
                        else{
                            ai_consecutive_piece=false;
                            if(oppo_consecutive_piece==true)
                                oppo_true_consecutive_piece+=1;
                        }
                    }else{
                        if(board.cellState(i+k, j+k) == CXCellState.P2){
                            oppo_consecutive_piece=false;
                            if(ai_consecutive_piece==true)
                                ai_true_consecutive_piece+=1;
                        }
                        else if(board.cellState(i+k, j+k) == CXCellState.FREE)
                            free_cells+=1;
                        else{
                            ai_consecutive_piece=false;
                            if(oppo_consecutive_piece==true)
                                oppo_true_consecutive_piece+=1;
                        }
                    }
                }
                score+=counting_consecutive_count(ToWin,free_cells,ai_consecutive_piece,oppo_consecutive_piece,ai_true_consecutive_piece,oppo_true_consecutive_piece);
            }
        }
    
        // diagonal check (bottom-left to top-right)
        for (int i = difference; i < Rows; i++){
            for (int j = 0; j < Columns - difference; j++){
                int free_cells = 0;
                boolean ai_consecutive_piece = true;
                boolean oppo_consecutive_piece = true;
                int ai_true_consecutive_piece = 0;
                int oppo_true_consecutive_piece = 0; 
                for(int k = 0; k < ToWin; k++){
                    if(is_first){
                        if(board.cellState(i-k, j+k) == CXCellState.P1){
                            oppo_consecutive_piece=false;
                            if(ai_consecutive_piece==true)
                                ai_true_consecutive_piece+=1;
                        }
                        else if(board.cellState(i-k, j+k) == CXCellState.FREE)
                            free_cells+=1;
                        else{
                            ai_consecutive_piece=false;
                            if(oppo_consecutive_piece==true)
                                oppo_true_consecutive_piece+=1;
                        }
                    }else{
                        if(board.cellState(i-k, j+k) == CXCellState.P2){
                            oppo_consecutive_piece=false;
                            if(ai_consecutive_piece==true)
                                ai_true_consecutive_piece+=1;
                        }
                        else if(board.cellState(i-k, j+k) == CXCellState.FREE)
                            free_cells+=1;
                        else{
                            ai_consecutive_piece=false;
                            if(oppo_consecutive_piece==true)
                                oppo_true_consecutive_piece+=1;
                        }
                    }
                }
                score+=counting_consecutive_count(ToWin,free_cells,ai_consecutive_piece,oppo_consecutive_piece,ai_true_consecutive_piece,oppo_true_consecutive_piece);
            }
        }

        return  score;
    }

    private int counting_consecutive_count(int ToWin, int count_free, boolean true_ai, boolean true_oppo, int true_count_ai, int true_count_oppo){
        int score=0;
        if(count_free==0){
            if(true_count_ai==ToWin)
                score=100000000;
            else if(true_count_oppo==ToWin)
                score=-100000000;
            else{
                if(true_count_ai>true_count_oppo)
                    score=true_count_ai*10-true_count_oppo*5;
                else
                    score=true_count_ai*5-true_count_oppo*10;
            }
        }else if(count_free==4){
            score=5;
        }else{
            if(true_count_oppo==0)
                score=true_count_ai*10;
            else if(true_count_ai==0)
                score=-true_count_oppo*10;
            else{
                if(true_count_ai>true_count_oppo)
                    score=true_count_ai*10-true_count_oppo*5;
                else
                    score=true_count_ai*5-true_count_oppo*10;
            }
        }
        return score;
    }

    private boolean checktime(){
		return ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0));
	}
    
    public String playerName(){
        return "AxelBrain1";
    }
}