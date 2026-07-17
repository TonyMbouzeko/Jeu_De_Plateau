import java.util.List;

 class IntelligenceArtificielle {

    //private static final int MAX_profondeur = 2;
    private long debut;
    private static final long LIMITE_TEMPS = 4900;

   /*  Move jouer(Board board, Mark maCouleur) {
    List<Move> coups = board.coupsPossibles(maCouleur);

    if (coups.isEmpty()) {
        return null;
    }

    int index = (int) (Math.random() * coups.size());
    return coups.get(index);
}*/

     Move getBestMove(Board board, Mark maCouleur, int profondeur) {

        if(profondeur <= 0){
            throw new IllegalArgumentException("La profondeur doit être strictement supérieure à 0.");
        }

        debut = System.currentTimeMillis();
        List<Move> coups = board.coupsPossibles(maCouleur);

        if (coups.isEmpty()) return null;

        Move meilleurCoup = coups.get(0);
        int meilleurScore = Integer.MIN_VALUE;

        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        for (Move coup : coups) {

            if(temps()){
                break;
            }

            Board copie = new Board(board);
            copie.play(coup, maCouleur);

            int score = alphaBeta(copie, profondeur - 1, alpha, beta, false, maCouleur);

            if (score > meilleurScore) {
                meilleurScore = score;
                meilleurCoup = coup;
            }
            alpha = Math.max(alpha, score);
        }

        return meilleurCoup;
    }

    public int alphaBeta(Board board, int profondeur, int alpha, int beta, boolean isMax, Mark maCouleur) {
       
        if (profondeur == 0 || board.estfini()|| temps()) {
            return board.evaluate(maCouleur);
        }

       
        Mark couleurAdversaire = (maCouleur == Mark.ROUGE) ? Mark.NOIR : Mark.ROUGE;

        Mark joueurActuel = isMax? maCouleur : couleurAdversaire;

        List<Move> coups = board.coupsPossibles(joueurActuel);

        if (coups.isEmpty()) {
            return board.evaluate(maCouleur);
        }

        if (isMax) {
            int maxEval = Integer.MIN_VALUE;

            for (Move coup : coups) {
                
                if(temps()){
                    break;
                }
            

                Board copie = new Board(board);
                copie.play(coup, joueurActuel);
                int eval = alphaBeta(copie, profondeur - 1, alpha, beta, false, maCouleur);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
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
                
                if(temps()){
                    break;
                }
            
                Board copie = new Board(board);
                copie.play(coup, joueurActuel);

                int eval = alphaBeta(board, profondeur - 1, alpha, beta, true, maCouleur);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
            
                if (beta <= alpha) {
                    break; 
                }
            }
            return minEval;
        }
    }

    public boolean temps(){
        return System.currentTimeMillis() - debut >= LIMITE_TEMPS;
    }
}