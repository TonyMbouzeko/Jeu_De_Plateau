import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// IMPORTANT: Il ne faut pas changer la signature des méthodes
// de cette classe, ni le nom de la classe.
class Board {
    private Mark[][] board;
    private Mark currentPlayer;

    public Board() {
        board = new Mark[13][13];

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                board[i][j] = Mark.EMPTY;
            }
        }
    }

    // Constructeur qui va faire une copie profonde d'un autre Board
    
    public Board(Board autreboard){
        this.board = new Mark[autreboard.board.length][];

        for (int i = 0; i < this.board.length; i++) {
            this.board[i] = new Mark[autreboard.board[i].length];
        
            for (int j = 0; j < board[i].length; j++) {
                this.board[i][j] = autreboard.board[i][j];
            }
        }
        this.currentPlayer = autreboard.getCurrentPlayer();
    }

    // --------------------------------------------------------------

    public void setCurrentPlayer(Mark player) {
        this.currentPlayer = player;
    }

    public Mark getCurrentPlayer() {
        return this.currentPlayer;
    }

    public void play(Move m, Mark mark) {
        if (m == null) {
            throw new IllegalArgumentException("Le mouvement ne peut pas être null.");
        }

        int ligneDepart = m.getRowDepart();
        int colonneDepart = m.getColDepart();
        int ligneArrivee = m.getRowArrive();
        int colonneArrivee = m.getColArrive();

        if (!estDansPlateau(ligneDepart, colonneDepart)
                || !estDansPlateau(ligneArrivee, colonneArrivee)) {
            throw new IllegalArgumentException("Le mouvement sort du plateau.");
        }

        Mark pion = board[ligneDepart][colonneDepart];

        if (pion == null || pion == Mark.EMPTY) {
            throw new IllegalArgumentException("Aucune pièce sur la case de départ.");
        }
        if (!belongsTo(pion, mark)) {
            throw new IllegalArgumentException("La pièce déplacée n'appartient pas au camp " + mark);
        }

        if (board[ligneArrivee][colonneArrivee] != Mark.EMPTY) {
            throw new IllegalArgumentException("La case d'arrivée n'est pas vide.");
        }

       
        if(!mouvementValide(m, pion)){
            throw new IllegalArgumentException("Le mouvement est invalide");
        }

        board[ligneDepart][colonneDepart] = Mark.EMPTY;
        board[ligneArrivee][colonneArrivee] = pion;

        appliquerCaptures(ligneArrivee, colonneArrivee, mark);
    }

    private boolean belongsTo(Mark mark, Mark camp) {
        if (mark == null || camp == null || mark == Mark.EMPTY || camp == Mark.EMPTY) {
            return false;
        }

        if (camp == Mark.NOIR) {
            return mark == Mark.NOIR || mark == Mark.ROI;
        }

        return camp == Mark.ROUGE && mark == Mark.ROUGE;
    }

    private boolean isClosedBox(int r, int c) {
        int n = board.length;
        boolean coin = (r == 0 || r == n - 1) && (c == 0 || c == n - 1);
        boolean trone = r == n / 2 && c == n / 2;

        return coin || trone;
    }

    // Ne pas changer la signature de cette méthode.
    public int evaluate(Mark mark) {
        final int VICTOIRE = 100_000_000;

        if (roiAuCoin()) {
            return mark == Mark.NOIR ? VICTOIRE : -VICTOIRE;
        }

        if (!roiSurPlateau()) {
            return mark == Mark.ROUGE ? VICTOIRE : -VICTOIRE;
        }

        int[] positionRoi = trouverRoi();
        int ligneRoi = positionRoi[0];
        int colonneRoi = positionRoi[1];
        int scoreRouge = 0;

        int sortiesDirectes = compterCheminsLibresVersCoins(ligneRoi, colonneRoi);
        if (sortiesDirectes > 0) {
            scoreRouge -= 90_000_000;
            scoreRouge -= sortiesDirectes * 1_000_000;
        }

        int nombreRouges = compterPieces(Mark.ROUGE);
        int nombreNoirs = compterPieces(Mark.NOIR);
        scoreRouge += nombreRouges * 45_000;
        scoreRouge -= nombreNoirs * 15_000;

    
        int axesFermes = compterAxesFermesDuRoi(ligneRoi, colonneRoi);
        scoreRouge += axesFermes * 30_000;

        int mobilite = mobiliteRoi(ligneRoi, colonneRoi);
        scoreRouge -= mobilite * 2_500;

        if (mobilite <= 8) {
            scoreRouge += 25_000;
        }
        if (mobilite <= 4) {
            scoreRouge += 60_000;
        }

        
        int cotesDangereux = nombreCotesDangereuxRoi();
        scoreRouge += cotesDangereux * 8_000;

        if (cotesDangereux == 2) {
            scoreRouge += 80_000;
        } else if (cotesDangereux == 3) {
            scoreRouge += 650_000;
        }

        int casesCaptureAccessibles = nombreCasesCaptureRoiAccessibles();

     
        if (cotesDangereux == 3 && casesCaptureAccessibles > 0) {
            scoreRouge += 6_000_000;
        } else if (cotesDangereux == 2 && casesCaptureAccessibles >= 2) {
            scoreRouge += 350_000;
        } else {
            scoreRouge += casesCaptureAccessibles * 12_000;
        }

        scoreRouge += scoreCordonAutourRoi(ligneRoi, colonneRoi);

        return mark == Mark.ROUGE ? scoreRouge : -scoreRouge;
    }

    private int[] trouverRoi() {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == Mark.ROI) {
                    return new int[]{i, j};
                }
            }
        }

        return new int[]{-1, -1};
    }

    public Move coupCaptureRoiImmediat() {
        int[] positionRoi = trouverRoi();

        if (positionRoi[0] < 0) {
            return null;
        }

        int ligneRoi = positionRoi[0];
        int colonneRoi = positionRoi[1];

        int[][] directions = {
                {-1, 0},
                {1, 0},
                {0, -1},
                {0, 1}
        };

        for (int[] directionCible : directions) {
            int ligneCible = ligneRoi + directionCible[0];
            int colonneCible = colonneRoi + directionCible[1];

            if (!estDansPlateau(ligneCible, colonneCible)
                    || board[ligneCible][colonneCible] != Mark.EMPTY
                    || isClosedBox(ligneCible, colonneCible)) {
                continue;
            }

            int autresCotesDangereux = 0;

            for (int[] direction : directions) {
                if (direction[0] == directionCible[0]
                        && direction[1] == directionCible[1]) {
                    continue;
                }

                if (cotedangereuxRoi(
                        ligneRoi + direction[0],
                        colonneRoi + direction[1]
                )) {
                    autresCotesDangereux++;
                }
            }

            if (autresCotesDangereux != 3) {
                continue;
            }


            for (int[] directionRecherche : directions) {
                Move candidat = trouverPremierRougeSurLigne(
                        ligneCible,
                        colonneCible,
                        directionRecherche[0],
                        directionRecherche[1]
                );

                if (candidat == null) {
                    continue;
                }

                Board copie = new Board(this);
                copie.play(candidat, Mark.ROUGE);

                if (!copie.roiSurPlateau()) {
                    return candidat;
                }
            }
        }

        return null;
    }

    public int nombreCotesDangereuxRoi() {
        int[] positionRoi = trouverRoi();

        if (positionRoi[0] < 0) {
            return 4;
        }

        int ligneRoi = positionRoi[0];
        int colonneRoi = positionRoi[1];

        int compteur = 0;

        if (cotedangereuxRoi(ligneRoi - 1, colonneRoi)) {
            compteur++;
        }
        if (cotedangereuxRoi(ligneRoi + 1, colonneRoi)) {
            compteur++;
        }
        if (cotedangereuxRoi(ligneRoi, colonneRoi - 1)) {
            compteur++;
        }
        if (cotedangereuxRoi(ligneRoi, colonneRoi + 1)) {
            compteur++;
        }

        return compteur;
    }

    private int nombreCasesCaptureRoiAccessibles() {
        int[] positionRoi = trouverRoi();

        if (positionRoi[0] < 0) {
            return 0;
        }

        int ligneRoi = positionRoi[0];
        int colonneRoi = positionRoi[1];

        int[][] directions = {
                {-1, 0},
                {1, 0},
                {0, -1},
                {0, 1}
        };

        int compteur = 0;

        for (int[] directionCible : directions) {
            int ligneCible = ligneRoi + directionCible[0];
            int colonneCible = colonneRoi + directionCible[1];

            if (!estDansPlateau(ligneCible, colonneCible)
                    || board[ligneCible][colonneCible] != Mark.EMPTY
                    || isClosedBox(ligneCible, colonneCible)) {
                continue;
            }

            if (rougePeutAtteindreLocalement(ligneCible, colonneCible)) {
                compteur++;
            }
        }

        return compteur;
    }

    private boolean rougePeutAtteindreLocalement(int ligneCible,int colonneCible) {
        int[][] directions = { {-1, 0},{1, 0}, {0, -1},{0, 1}};

        for (int[] direction : directions) {
            if (trouverPremierRougeSurLigne(ligneCible, colonneCible,direction[0],direction[1]) != null) {
                return true;
            }
        }

        return false;
    }

    private Move trouverPremierRougeSurLigne(int ligneCible,int colonneCible,int directionLigne,int directionColonne) {
        int ligne = ligneCible + directionLigne;
        int colonne = colonneCible + directionColonne;

        while (estDansPlateau(ligne, colonne)) {
            Mark piece = board[ligne][colonne];

            if (piece == Mark.ROUGE) {
                return new Move(ligne, colonne,ligneCible,colonneCible);
            }

            if (piece != Mark.EMPTY) {
                return null;
            }

            ligne += directionLigne;
            colonne += directionColonne;
        }

        return null;
    }

    private int compterPieces(Mark piece) {
        int compteur = 0;

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == piece) {
                    compteur++;
                }
            }
        }

        return compteur;
    }

    private int scoreCordonAutourRoi(int ligneRoi, int colonneRoi) {
        int score = 0;
        int rougesDistance3A5 = 0;

        for (int ligne = 0; ligne < board.length; ligne++) {
            for (int colonne = 0; colonne < board[ligne].length; colonne++) {
                if (board[ligne][colonne] != Mark.ROUGE) {
                    continue;
                }

                int distance = Math.abs(ligne - ligneRoi)
                        + Math.abs(colonne - colonneRoi);

                if (distance == 1) {
                    score += 500;
                } else if (distance == 2) {
                    score += 1_500;
                } else if (distance == 3) {
                    score += 4_500;
                    rougesDistance3A5++;
                } else if (distance == 4) {
                    score += 5_000;
                    rougesDistance3A5++;
                } else if (distance == 5) {
                    score += 3_500;
                    rougesDistance3A5++;
                } else if (distance == 6) {
                    score += 1_000;
                }
            }
        }

        if (rougesDistance3A5 >= 5) {
            score += 25_000;
        }
        if (rougesDistance3A5 >= 8) {
            score += 35_000;
        }

        return score;
    }

    private int compterAxesFermesDuRoi(int ligneRoi, int colonneRoi) {
        int[][] directions = {
                {-1, 0},
                {1, 0},
                {0, -1},
                {0, 1}
        };

        int axesFermes = 0;

        for (int[] direction : directions) {
            int ligne = ligneRoi + direction[0];
            int colonne = colonneRoi + direction[1];
            boolean ferme = false;

            while (estDansPlateau(ligne, colonne)) {
                Mark caseActuelle = board[ligne][colonne];

                if (caseActuelle == Mark.ROUGE || isClosedBox(ligne, colonne)) {
                    ferme = true;
                    break;
                }

                if (caseActuelle == Mark.NOIR) {
                    break;
                }

                ligne += direction[0];
                colonne += direction[1];
            }

            if (ferme) {
                axesFermes++;
            }
        }

        return axesFermes;
    }

    public int maxRougesCapturablesEnUnCoup() {
        int rougesAvant = compterPieces(Mark.ROUGE);
        int maximum = 0;

        for (Move coup : coupsPossibles(Mark.NOIR)) {
            Board copie = new Board(this);
            copie.play(coup, Mark.NOIR);

            int captures = rougesAvant - copie.compterPieces(Mark.ROUGE);
            maximum = Math.max(maximum, captures);

            if (maximum >= 2) {
                // Deux captures en un coup constituent déjà un danger majeur.
                return maximum;
            }
        }

        return maximum;
    }

  
    public boolean rougeCapturableAuProchainCoup() {
        return maxRougesCapturablesEnUnCoup() > 0;
    }

    public boolean roiPeutGagnerEnUnCoup() {
        int[] positionRoi = trouverRoi();
        int ligneRoi = positionRoi[0];
        int colonneRoi = positionRoi[1];

        if (ligneRoi < 0 || colonneRoi < 0) {
            return false;
        }

        return compterCheminsLibresVersCoins(ligneRoi, colonneRoi) > 0;
    }

    private int compterCheminsLibresVersCoins(int ligneRoi, int colonneRoi) {
        int derniereCase = board.length - 1;
        int compteur = 0;

        if (cheminLibreVersCoin(ligneRoi, colonneRoi, 0, 0)) {
            compteur++;
        }
        if (cheminLibreVersCoin(ligneRoi, colonneRoi, 0, derniereCase)) {
            compteur++;
        }
        if (cheminLibreVersCoin(ligneRoi, colonneRoi, derniereCase, 0)) {
            compteur++;
        }
        if (cheminLibreVersCoin(ligneRoi, colonneRoi, derniereCase, derniereCase)) {
            compteur++;
        }

        return compteur;
    }

    private int mobiliteRoi(int ligneRoi, int colonneRoi) {
        int[][] directions = {
                {-1, 0},
                {1, 0},
                {0, -1},
                {0, 1}
        };

        int compteur = 0;

        for (int[] direction : directions) {
            int ligne = ligneRoi + direction[0];
            int colonne = colonneRoi + direction[1];

            while (estDansPlateau(ligne, colonne)
                    && board[ligne][colonne] == Mark.EMPTY) {
                compteur++;
                ligne += direction[0];
                colonne += direction[1];
            }
        }

        return compteur;
    }

    // --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    public List<Move> coupsPossibles(Mark camp) {
        List<Move> moves = new ArrayList<>();
        int n = board.length;
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (!belongsTo(board[i][j], camp)) {
                    continue;
                }

                Mark piece = board[i][j];

                for (int[] direction : directions) {
                    int r = i;
                    int c = j;

                    while (true) {
                        r += direction[0];
                        c += direction[1];

                        if (!estDansPlateau(r, c)) {
                            break;
                        }

                        if (board[r][c] != Mark.EMPTY) {
                            break;
                        }

                        if (isClosedBox(r, c) && piece != Mark.ROI) {
                            continue;
                        }

                        moves.add(new Move(i, j, r, c));
                    }
                }
            }
        }

        return moves;
    }

    public boolean estfini() {
        return roiAuCoin() || !roiSurPlateau();
    }

    public Mark[][] loadFromServer(String[] boardValues) {
        if (boardValues == null || boardValues.length != 169) {
            throw new IllegalArgumentException(
                    "Le plateau doit contenir exactement 169 valeurs.");
        }

        Mark[][] tableau = new Mark[13][13];

        for (int i = 0; i < boardValues.length; i++) {
            int ligne = i / 13;
            int colonne = i % 13;
            tableau[ligne][colonne] = conversion(Integer.parseInt(boardValues[i]));
        }

        this.board = tableau;
        return tableau;
    }

    public Mark conversion(int valeur) {
        switch (valeur) {
            case 0:
                return Mark.EMPTY;
            case 2:
                return Mark.NOIR;
            case 4:
                return Mark.ROUGE;
            case 5:
                return Mark.ROI;
            default:
                throw new IllegalArgumentException("Valeur inconnue : " + valeur);
        }
    }

    public boolean roiAuCoin() {
        return board[0][0] == Mark.ROI || board[0][12] == Mark.ROI || board[12][12] == Mark.ROI || board[12][0] == Mark.ROI;
    }

    public boolean roiSurPlateau() {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == Mark.ROI) {
                    return true;
                }
            }
        }

        return false;
    }

    public void appliquerCaptures(int ligne, int colonne, Mark camp) {
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] direction : directions) {
            int ligneEnnemi = ligne + direction[0];
            int colonneEnnemi = colonne + direction[1];
            int ligneAppui = ligne + 2 * direction[0];
            int colonneAppui = colonne + 2 * direction[1];

            if (!estDansPlateau(ligneEnnemi, colonneEnnemi)) {
                continue;
            }

            Mark pieceEnnemie = board[ligneEnnemi][colonneEnnemi];

            if (pieceEnnemie == null || pieceEnnemie == Mark.EMPTY || belongsTo(pieceEnnemie, camp) || pieceEnnemie == Mark.ROI) {
                continue;
            }

            if (!estDansPlateau(ligneAppui, colonneAppui)) {
                continue;
            }

            Mark pieceAppui = board[ligneAppui][colonneAppui];
            boolean capture = belongsTo(pieceAppui, camp) || isClosedBox(ligneAppui, colonneAppui);

            if (capture) {
                board[ligneEnnemi][colonneEnnemi] = Mark.EMPTY;
            }

        }
        
        if (camp == Mark.ROUGE){
                appliquerCaptureRoi();
            }
    }

    public void appliquerCaptureRoi(){
        int ligneRoi = -1;
        int colonneRoi = -1;

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == Mark.ROI){
                    ligneRoi = i;
                    colonneRoi =j;
                    break;
                }
            }
            if (ligneRoi != -1){
            break;
            }
        }

        if (ligneRoi == -1){
            return;
        }

        if(roiAuCoin()){
            return;
        }
        boolean haut = cotedangereuxRoi(ligneRoi-1, colonneRoi);
        boolean bas = cotedangereuxRoi(ligneRoi+1, colonneRoi);
        boolean droite = cotedangereuxRoi(ligneRoi, colonneRoi+1);
        boolean gauche = cotedangereuxRoi(ligneRoi, colonneRoi-1);

        if (haut && bas && gauche && droite){
            board[ligneRoi][colonneRoi] = Mark.EMPTY;
        }
       
    }

    public boolean estDansPlateau(int ligne, int colonne) {
        return ligne >= 0 && ligne < board.length && colonne >= 0 && colonne < board.length;
    }

    public boolean cheminLibreVersCoin(int ligneRoi, int colonneRoi, int ligneCoin, int colonneCoin) {

        if (ligneRoi != ligneCoin && colonneRoi != colonneCoin) {
            return false;
        }
        int directionLigne = Integer.compare(ligneCoin, ligneRoi);
        int directionColonne = Integer.compare(colonneCoin, colonneRoi);

        int ligne = ligneRoi + directionLigne;
        int colonne = colonneRoi + directionColonne;

        while (ligne != ligneCoin || colonne != colonneCoin) {
            if (board[ligne][colonne] != Mark.EMPTY) {
                return false;
            }

            ligne += directionLigne;
            colonne += directionColonne;
        }

        return board[ligneCoin][colonneCoin] == Mark.EMPTY;
    }

    public boolean mouvementValide(Move m, Mark pion) {
        int ligneDepart = m.getRowDepart();
        int colonneDepart = m.getColDepart();
        int ligneArrivee = m.getRowArrive();
        int colonneArrivee = m.getColArrive();

    
        if (ligneDepart == ligneArrivee && colonneDepart == colonneArrivee) {
            return false;
            
        }

        if (ligneDepart != ligneArrivee && colonneDepart != colonneArrivee) {
            return false;
        }
    
        if (isClosedBox(ligneArrivee, colonneArrivee) && pion != Mark.ROI) {
            return false;
        }

        int directionLigne = Integer.compare(ligneArrivee, ligneDepart);

        int directionColonne = Integer.compare(colonneArrivee, colonneDepart);

        int ligne = ligneDepart + directionLigne;
        int colonne = colonneDepart + directionColonne;

    
        while (ligne != ligneArrivee || colonne != colonneArrivee) {

            if (board[ligne][colonne] != Mark.EMPTY) {
                return false;
            }
            ligne += directionLigne;
            colonne += directionColonne;
        }

        return true;
    }

    public boolean cotedangereuxRoi(int ligne, int colonne){
        if (!estDansPlateau(ligne, colonne)){
            return true;
        }

        return board[ligne][colonne] ==  Mark.ROUGE || isClosedBox(ligne, colonne);
    }

    // ---------------------------------------------------------------------- Méthode pour éviter les coups repetés ----------------------------------------------------------------------
    public String obtenirSignature(Mark joueurActuel) {
        StringBuilder signature = new StringBuilder();
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                signature.append(board[i][j].ordinal());
            }
        }
        signature.append('|');
        signature.append(joueurActuel);
        
        return signature.toString();
    }

}