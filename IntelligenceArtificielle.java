import java.util.List;

class IntelligenceArtificielle {

    private static final int MAX_DEPTH = 2; 

    Move getBestMove(Board board, Mark maCouleur) {
        board.SetCurrentPlayer(maCouleur);
        List<Move> coups = board.coupsPossibles();
        if (coups.isEmpty()) return null;

        Move meilleurCoup = coups.get(0);
        int meilleurScore = Integer.MIN_VALUE;

        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        Mark couleurAdversaire = (maCouleur == Mark.ROUGE) ? Mark.NOIR : Mark.ROUGE;

        for (Move coup : coups) {
            Board copieBoard = board.clone(); 
            copieBoard.play(coup, maCouleur);
            copieBoard.SetCurrentPlayer(couleurAdversaire);
            int score = alphaBeta(copieBoard, MAX_DEPTH - 1, alpha, beta, false, maCouleur);

            if (score > meilleurScore) {
                meilleurScore = score;
                meilleurCoup = coup;
            }
            alpha = Math.max(alpha, score);
        }

        return meilleurCoup;
    }

    private int alphaBeta(Board board, int depth, int alpha, int beta, boolean isMax, Mark maCouleur) {
        if (depth == 0 || board.estfini()) {
            return board.evaluate(maCouleur);
        }

        List<Move> coups = board.coupsPossibles();
        if (coups.isEmpty()) {
            return board.evaluate(maCouleur);
        }

        Mark couleurAdversaire = (maCouleur == Mark.ROUGE) ? Mark.NOIR : Mark.ROUGE;

        if (isMax) {
            int maxEval = Integer.MIN_VALUE;
            for (Move coup : coups) {
                Board copieBoard = board.clone(); 
                copieBoard.play(coup, maCouleur);
                
                copieBoard.SetCurrentPlayer(couleurAdversaire);
                
                int eval = alphaBeta(copieBoard, depth - 1, alpha, beta, false, maCouleur);
                
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) {
                    break; 
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (Move coup : coups) {
                Board copieBoard = board.clone(); 
                copieBoard.play(coup, couleurAdversaire);
                
                copieBoard.SetCurrentPlayer(maCouleur);
                
                int eval = alphaBeta(copieBoard, depth - 1, alpha, beta, true, maCouleur);
                
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) {
                    break; 
                }
            }
            return minEval;
        }
    }
}
