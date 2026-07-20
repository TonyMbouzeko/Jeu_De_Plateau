import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class IntelligenceArtificielle {

    private long debut;

    private static final long LIMITE_TEMPS = 3500;
    private static final int TAILLE_HISTORIQUE_COUPS = 8;

    private final Map<String, Integer> positionsVisitees = new HashMap<>();
    private final Deque<Move> derniersCoupsIA = new ArrayDeque<>();

    private Move dernierCoupIA;

    Move getBestMove(Board board, Mark maCouleur, int profondeur) {

        if (profondeur <= 0) {
            throw new IllegalArgumentException("La profondeur doit être strictement supérieure à 0.");
        }

        debut = System.currentTimeMillis();

        List<Move> coups = board.coupsPossibles(maCouleur);

        if (coups.isEmpty()) {
            return null;
        }

        Move meilleurCoup = null;
        int meilleurScore = Integer.MIN_VALUE;

        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        for (Move coup : coups) {

            if (temps()) {
                break;
            }

            Board copie = new Board(board);
            copie.play(coup, maCouleur);

            Mark prochainJoueur = adversaire(maCouleur);

            String signature =copie.obtenirSignature(prochainJoueur);

            int nombreVisites =
                    positionsVisitees.getOrDefault(signature, 0);

            int score = alphaBeta(copie, profondeur - 1, alpha, beta, false, maCouleur);

            //score -= nombreVisites * 50_000;

            int occurrencesRecentes = nombreOccurrencesRecentes(coup);

            //score -= occurrencesRecentes * 20_000;

            /*if (estCoupInverse(coup, dernierCoupIA)) {
                score -= 30_000;
            }*/

            //score -= penaliteMemePiece(coup);

            if (score > meilleurScore) {
                meilleurScore = score;
                meilleurCoup = coup;
            }

            alpha = Math.max(alpha, meilleurScore);
        }

        if (meilleurCoup == null) {
            meilleurCoup = coups.get(0);
        }

        enregistrerCoupChoisi( board, meilleurCoup, maCouleur);

        return meilleurCoup;
    }

    public int alphaBeta(Board board, int profondeur, int alpha, int beta, boolean isMax, Mark maCouleur) {

        if (profondeur == 0 || board.estfini() || temps()) {
            return board.evaluate(maCouleur);
        }

        Mark couleurAdversaire = adversaire(maCouleur);

        Mark joueurActuel = isMax ? maCouleur : couleurAdversaire;

        List<Move> coups = board.coupsPossibles(joueurActuel);

        if (coups.isEmpty()) {
            return board.evaluate(maCouleur);
        }

        if (isMax) {

            int maxEval = Integer.MIN_VALUE;

            for (Move coup : coups) {

                if (temps()) {
                    return board.evaluate(maCouleur);
                }

                Board copie = new Board(board);
                copie.play(coup, joueurActuel);

                int eval = alphaBeta(copie, profondeur - 1, alpha, beta, false, maCouleur);

                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, maxEval);

                if (beta <= alpha) {
                    break;
                }
            }

            return maxEval;

        } else {
            int minEval = Integer.MAX_VALUE;

            for (Move coup : coups) {

                if (temps()) {
                    return board.evaluate(maCouleur);
                }

                Board copie = new Board(board);
                copie.play(coup, joueurActuel);

                int eval = alphaBeta(copie, profondeur - 1, alpha, beta,true, maCouleur);

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, minEval);

                if (beta <= alpha) {
                    break;
                }
            }

            return minEval;
        }
    }

    private void enregistrerCoupChoisi(Board board, Move meilleurCoup, Mark maCouleur) {
        if (meilleurCoup == null) {
            return;
        }

        Board positionChoisie = new Board(board);
        positionChoisie.play(meilleurCoup, maCouleur);

        Mark prochainJoueur = adversaire(maCouleur);

        String signatureChoisie = positionChoisie.obtenirSignature(prochainJoueur);

        positionsVisitees.put(signatureChoisie, positionsVisitees.getOrDefault( signatureChoisie, 0) + 1);

        derniersCoupsIA.addLast(meilleurCoup);

        if (derniersCoupsIA.size()> TAILLE_HISTORIQUE_COUPS) {
            derniersCoupsIA.removeFirst();
        }

        dernierCoupIA = meilleurCoup;
    }

    private int penaliteMemePiece(Move coup) {
        if (coup == null || dernierCoupIA == null) {
            return 0;
        }
        boolean memePieceQueDernierTour = coup.getRowDepart() == dernierCoupIA.getRowArrive() && coup.getColDepart() == dernierCoupIA.getColArrive();

        if (!memePieceQueDernierTour) {
            return 0;
        }
        int penalite = 5_000;

        for (Move ancienCoup : derniersCoupsIA) {

            boolean destinationRecemmentVisitee =coup.getRowArrive() == ancienCoup.getRowDepart() && coup.getColArrive() == ancienCoup.getColDepart();

            if (destinationRecemmentVisitee) {
                penalite += 5_000;
            }
        }

        return penalite;
    }

    public boolean temps() {
        return System.currentTimeMillis() - debut >= LIMITE_TEMPS;
    }

    private boolean estCoupInverse(Move coup, Move precedent) {
        if (coup == null || precedent == null) {
            return false;
        }

        return coup.getRowDepart()
                        == precedent.getRowArrive()
                && coup.getColDepart()
                        == precedent.getColArrive()
                && coup.getRowArrive()
                        == precedent.getRowDepart()
                && coup.getColArrive()
                        == precedent.getColDepart();
    }

    private Mark adversaire(Mark joueur) {
        if (joueur == Mark.ROUGE) {
            return Mark.NOIR;
        }

        if (joueur == Mark.NOIR) {
            return Mark.ROUGE;
        }

        throw new IllegalArgumentException(
                "Joueur invalide : " + joueur
        );
    }

    private boolean memeCoup(
            Move premier,
            Move deuxieme
    ) {
        if (premier == null || deuxieme == null) {
            return false;
        }

        return premier.getRowDepart()
                        == deuxieme.getRowDepart()
                && premier.getColDepart()
                        == deuxieme.getColDepart()
                && premier.getRowArrive()
                        == deuxieme.getRowArrive()
                && premier.getColArrive()
                        == deuxieme.getColArrive();
    }

    private int nombreOccurrencesRecentes(Move coup) {
        int compteur = 0;

        for (Move ancienCoup : derniersCoupsIA) {
            if (memeCoup(coup, ancienCoup)) {
                compteur++;
            }
        }

        return compteur;
    }
}