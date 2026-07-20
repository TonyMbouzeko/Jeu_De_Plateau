import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class IntelligenceArtificielle {

    /*
     * Le serveur accorde 5 secondes. La recherche est arrêtée avant
     * cette limite afin de conserver du temps pour l'envoi du coup.
     */
    private static final long LIMITE_TEMPS = 1000;
    private static final int INFINI = 1_000_000_000;

    private long debut;
    private Move dernierCoupIA;

    /*
     * Exception interne utilisée pour abandonner immédiatement une
     * profondeur incomplète. Le résultat partiel n'est jamais conservé.
     */
    private static final class TempsEcouleException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static final class ResultatRecherche {
        private final Move coup;
        private final int score;

        private ResultatRecherche(Move coup, int score) {
            this.coup = coup;
            this.score = score;
        }
    }

    Move getBestMove(Board board, Mark maCouleur, int profondeurMax) {
        if (board == null) {
            throw new IllegalArgumentException("Le plateau ne peut pas être null.");
        }

        if (maCouleur != Mark.ROUGE && maCouleur != Mark.NOIR) {
            throw new IllegalArgumentException("Couleur invalide : " + maCouleur);
        }

        if (profondeurMax <= 0) {
            throw new IllegalArgumentException(
                    "La profondeur doit être strictement supérieure à 0."
            );
        }

        debut = System.currentTimeMillis();

        List<Move> coups = board.coupsPossibles(maCouleur);
        if (coups.isEmpty()) {
            return null;
        }

        Move victoireImmediate = chercherVictoireImmediate(
                board,
                maCouleur,
                coups
        );

        if (victoireImmediate != null) {
            dernierCoupIA = victoireImmediate;
            return victoireImmediate;
        }

        /*
         * Coup de secours toujours légal. Il est remplacé uniquement après
         * qu'une profondeur a été entièrement terminée.
         */
        Move meilleurCoupTermine = choisirCoupDeSecours(coups);
        int meilleurScoreTermine = board.evaluate(maCouleur);
        int profondeurTerminee = 0;

        /*
         * Approfondissement itératif :
         * profondeur 1, puis 2, puis 3, etc.
         */
        for (int profondeur = 1;
             profondeur <= profondeurMax;
             profondeur++) {

            try {
                ResultatRecherche resultat = rechercherRacine(
                        board,
                        maCouleur,
                        profondeur
                );

                /*
                 * Ce résultat est enregistré seulement si toute la profondeur
                 * a été calculée avant la limite de temps.
                 */
                meilleurCoupTermine = resultat.coup;
                meilleurScoreTermine = resultat.score;
                profondeurTerminee = profondeur;

            } catch (TempsEcouleException exception) {
                break;
            }
        }

        dernierCoupIA = meilleurCoupTermine;

        System.out.println(
                "Profondeur terminée : " + profondeurTerminee
                        + " | Score : " + meilleurScoreTermine
                        + " | Coup : " + meilleurCoupTermine
        );

        return meilleurCoupTermine;
    }

    private ResultatRecherche rechercherRacine(
            Board board,
            Mark maCouleur,
            int profondeur
    ) {
        verifierTemps();

        List<Move> coups = new ArrayList<>(
                board.coupsPossibles(maCouleur)
        );

        /*
         * L'ordre des coups est surtout utile à la racine :
         * les meilleurs coups probables sont examinés en premier.
         */
        ordonnerCoupsRacine(board, coups, maCouleur);

        Move meilleurCoup = null;
        int meilleurScore = -INFINI;
        int alpha = -INFINI;
        int beta = INFINI;

        for (Move coup : coups) {
            verifierTemps();

            Board copie = new Board(board);
            copie.play(coup, maCouleur);

            int score = alphaBeta(
                    copie,
                    profondeur - 1,
                    alpha,
                    beta,
                    false,
                    maCouleur
            );

            /*
             * Protection tactique à la racine. Même si une recherche peu
             * profonde ne voit pas toute la séquence, l'IA refuse de sacrifier
             * facilement un attaquant pour réduire momentanément la mobilité.
             */
            if (maCouleur == Mark.ROUGE) {
                int rougesCapturables = copie.maxRougesCapturablesEnUnCoup();

                if (rougesCapturables == 1) {
                    score -= 350_000;
                } else if (rougesCapturables >= 2) {
                    score -= rougesCapturables * 1_500_000;
                }
            }

            if (score > meilleurScore
                    || (score == meilleurScore
                    && prefererAuDepartage(coup, meilleurCoup))) {
                meilleurScore = score;
                meilleurCoup = coup;
            }

            alpha = Math.max(alpha, meilleurScore);
        }

        if (meilleurCoup == null) {
            throw new TempsEcouleException();
        }

        return new ResultatRecherche(meilleurCoup, meilleurScore);
    }

    public int alphaBeta(
            Board board,
            int profondeur,
            int alpha,
            int beta,
            boolean isMax,
            Mark maCouleur
    ) {
        verifierTemps();

        if (board.estfini() || profondeur <= 0) {
            return board.evaluate(maCouleur);
        }

        Mark joueurActuel = isMax
                ? maCouleur
                : adversaire(maCouleur);

        List<Move> coups = board.coupsPossibles(joueurActuel);

        if (coups.isEmpty()) {
            return board.evaluate(maCouleur);
        }

        if (isMax) {
            int meilleurScore = -INFINI;

            for (Move coup : coups) {
                verifierTemps();

                Board copie = new Board(board);
                copie.play(coup, joueurActuel);

                int score = alphaBeta(
                        copie,
                        profondeur - 1,
                        alpha,
                        beta,
                        false,
                        maCouleur
                );

                meilleurScore = Math.max(meilleurScore, score);
                alpha = Math.max(alpha, meilleurScore);

                if (alpha >= beta) {
                    break;
                }
            }

            return meilleurScore;
        }

        int meilleurScore = INFINI;

        for (Move coup : coups) {
            verifierTemps();

            Board copie = new Board(board);
            copie.play(coup, joueurActuel);

            int score = alphaBeta(
                    copie,
                    profondeur - 1,
                    alpha,
                    beta,
                    true,
                    maCouleur
            );

            meilleurScore = Math.min(meilleurScore, score);
            beta = Math.min(beta, meilleurScore);

            if (alpha >= beta) {
                break;
            }
        }

        return meilleurScore;
    }

    private void ordonnerCoupsRacine(
            Board board,
            List<Move> coups,
            Mark maCouleur
    ) {
        coups.sort(
                Comparator.comparingInt(
                        (Move coup) -> scoreOrdreRacine(
                                board,
                                coup,
                                maCouleur
                        )
                ).reversed()
        );
    }

    private int scoreOrdreRacine(
            Board board,
            Move coup,
            Mark maCouleur
    ) {
        /*
         * Cette méthode est appelée avant la recherche chronométrée de chaque
         * profondeur. Elle reste volontairement simple.
         */
        Board copie = new Board(board);
        copie.play(coup, maCouleur);

        if (copie.estfini()) {
            return copie.evaluate(maCouleur);
        }

        int score = copie.evaluate(maCouleur);

        /*
         * Pour les rouges, un coup qui laisse au roi un accès direct à un coin
         * est placé à la fin de la liste.
         */
        if (maCouleur == Mark.ROUGE
                && copie.roiPeutGagnerEnUnCoup()) {
            score -= 95_000_000;
        }

        if (maCouleur == Mark.ROUGE) {
            int rougesCapturables = copie.maxRougesCapturablesEnUnCoup();
            score -= rougesCapturables * 500_000;
        }

        /*
         * Évite de remettre immédiatement une pièce à son ancienne position
         * lorsque plusieurs coups ont une valeur semblable.
         */
        if (estCoupInverse(coup, dernierCoupIA)) {
            score -= 1_000;
        }

        return score;
    }

    private Move chercherVictoireImmediate(
            Board board,
            Mark maCouleur,
            List<Move> coups
    ) {
        for (Move coup : coups) {
            verifierTemps();

            Board copie = new Board(board);
            copie.play(coup, maCouleur);

            if (copie.estfini()
                    && copie.evaluate(maCouleur) > 0) {
                return coup;
            }
        }

        return null;
    }

    private Move choisirCoupDeSecours(List<Move> coups) {
        if (dernierCoupIA == null) {
            return coups.get(0);
        }

        for (Move coup : coups) {
            if (!estCoupInverse(coup, dernierCoupIA)) {
                return coup;
            }
        }

        return coups.get(0);
    }

    private boolean prefererAuDepartage(
            Move candidat,
            Move meilleurActuel
    ) {
        if (candidat == null) {
            return false;
        }

        if (meilleurActuel == null) {
            return true;
        }

        boolean candidatEstInverse =
                estCoupInverse(candidat, dernierCoupIA);

        boolean meilleurEstInverse =
                estCoupInverse(meilleurActuel, dernierCoupIA);

        return !candidatEstInverse && meilleurEstInverse;
    }

    public boolean temps() {
        return System.currentTimeMillis() - debut
                >= LIMITE_TEMPS;
    }

    private void verifierTemps() {
        if (temps()) {
            throw new TempsEcouleException();
        }
    }

    private boolean estCoupInverse(
            Move coup,
            Move precedent
    ) {
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
}