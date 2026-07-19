import java.util.ArrayList;
import java.util.List;

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

    // Ne pas changer la signature de cette méthode.
    /**
     * @param m
     * @param mark
     */
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

        if (roiAuCoin()) {
            if( mark == Mark.NOIR){
                return 1000000;
            }else{
                return -1000000;
            }
        }

        if (!roiSurPlateau()) {
            if (mark == Mark.ROUGE){
                return 1000000;
            }else{
                return -1000000;
            }
        }

        // --------------------- Nombre de pièces sur le terrain et distance par rapport au coin venant du roi ------------------------------------------------
        int nombreNoirs =0;
        int nombreRouges =0;

        int ligneRoi = -1;
        int colonneRoi = -1;
        
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == Mark.ROUGE) {
                    nombreRouges++;
                } else if (board[i][j] == Mark.NOIR) {
                    nombreNoirs++;
                } else if(board[i][j] == Mark.ROI){
                    ligneRoi = i;
                    colonneRoi = j;
                }
            }
        }
        int scoreRouge = 0;
        scoreRouge += evaluationMateriel(nombreRouges, nombreNoirs);
        scoreRouge += evaluationDistanceRoi(ligneRoi, colonneRoi);
        scoreRouge += evaluationCheminLibreRoi(ligneRoi, colonneRoi);
        scoreRouge += evaluationRougesAutourRoi(ligneRoi, colonneRoi);
        scoreRouge += evaluationMobiliteRoi(ligneRoi, colonneRoi);
        scoreRouge += directionBloquee(ligneRoi, colonneRoi) * 1000;
        scoreRouge -= nombreRougesMenaces() * 2500;
        // -----------------------------------------------------------------------------------------------------------
        if (mark == Mark.ROUGE){
            return scoreRouge;
        }else{
            return -scoreRouge;
        }
    }

    // ---------------------------------------------------------------- Méthode pour la fonction evaluation ----------------------------------------------------------------

    public int evaluationMateriel(int nombreRouges, int nombreNoirs) {
        return (nombreRouges * 1500) - (nombreNoirs * 1000);
    };

    public int evaluationDistanceRoi(int ligneRoi, int colonneRoi) {
        int distanceCoin = distanceCoinPlusProche(ligneRoi, colonneRoi);
        return distanceCoin * 5000;
    }

    public int evaluationCheminLibreRoi(int ligneRoi, int colonneRoi) {
        if (cheminLibreRoi(ligneRoi, colonneRoi)) {
            return -700000;
        }
        return 0;
    }

    public int evaluationRougesAutourRoi(int ligneRoi, int colonneRoi) {
        int ennemiRoi = rougesAutourDuRoi(ligneRoi, colonneRoi);
        if (ennemiRoi == 1) {
            return 2000;
        } else if (ennemiRoi == 2) {
            return 5000;
        } else if (ennemiRoi == 3) {
            return 15000;
        } else if (ennemiRoi == 4) {
            return 30000;
        }
        return 0;
    }

    public int evaluationMobiliteRoi(int ligneRoi, int colonneRoi) {
        int roimobile = mobiliteRoi(ligneRoi, colonneRoi) * 350;
        return -roimobile;
    }

    public int directionBloquee(int ligneRoi, int colonneRoi) {
        int directionsBloquees = 0;
        int[][] directions = {{-1, 0},{1, 0},{0, -1},{0, 1}};

        for (int[] direction : directions) {
            int ligne = ligneRoi + direction[0];
            int colonne = colonneRoi + direction[1];

            if (!estDansPlateau(ligne, colonne)) {
                directionsBloquees++;
                continue;
            }

            Mark pieceVoisine = board[ligne][colonne];

            if (pieceVoisine == Mark.ROUGE || isClosedBox(ligne, colonne)) {
                directionsBloquees++;
            }
        }

        return directionsBloquees;
    }

    public int nombreRougesMenaces() {
        int nombreRougesActuel = compterPieces(Mark.ROUGE);
        int perteMaximale = 0;

        List<Move> coupsNoirs = coupsPossibles(Mark.NOIR);

        for (Move coup : coupsNoirs) {
            Board copie = new Board(this);

            try {
                copie.play(coup, Mark.NOIR);

                int rougesRestants = copie.compterPieces(Mark.ROUGE);
                int rougesCaptures = nombreRougesActuel - rougesRestants;

                perteMaximale = Math.max(perteMaximale, rougesCaptures);
            } catch (IllegalArgumentException e) {
            // On ignore un éventuel mouvement invalide.
            }
        }

        return perteMaximale;
    }

    public int compterPieces(Mark piece) {
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

    public int distanceCoinPlusProche(int ligneRoi, int colonneRoi) {
        
        int distanceHautGauche = Math.abs(ligneRoi)+ Math.abs(colonneRoi);
        int distanceHautDroite = Math.abs(ligneRoi)+ Math.abs(colonneRoi - 12);

        int distanceBasGauche = Math.abs(ligneRoi - 12) + Math.abs(colonneRoi);
        int distanceBasDroite = Math.abs(ligneRoi - 12) + Math.abs(colonneRoi - 12);

        return Math.min(Math.min(distanceHautGauche, distanceHautDroite), Math.min(distanceBasGauche, distanceBasDroite));
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

    public boolean cheminLibreRoi(int ligneRoi, int colonneRoi) {
    int derniereCase = board.length - 1;
    return cheminLibreVersCoin(ligneRoi, colonneRoi, 0, 0) || cheminLibreVersCoin( ligneRoi,colonneRoi, 0,derniereCase)
                || cheminLibreVersCoin(
                    ligneRoi,
                    colonneRoi,
                    derniereCase,
                    0
            )
            || cheminLibreVersCoin(
                    ligneRoi,
                    colonneRoi,
                    derniereCase,
                    derniereCase
            );
    }

    public int rougesAutourDuRoi(int ligneRoi, int colonneRoi) {
        int[][] directions = {{-1, 0},{1, 0},{0, -1},{0, 1}};
        int compteur = 0;
        for (int[] direction : directions) {
            int ligneVoisine = ligneRoi + direction[0];
            int colonneVoisine = colonneRoi + direction[1];

            if (!estDansPlateau(ligneVoisine, colonneVoisine)) {
                continue;
            }

            if (board[ligneVoisine][colonneVoisine] == Mark.ROUGE) {
                compteur++;
            }
        }

        return compteur;
    }

    public int mobiliteRoi(int ligneRoi, int colonneRoi) {
        int[][] directions = {{-1, 0},{1, 0},{0, -1},{0, 1}};
        int compteur = 0;
        for (int[] direction : directions) {
            int ligne = ligneRoi + direction[0];
            int colonne = colonneRoi + direction[1];

            while (estDansPlateau(ligne, colonne) && board[ligne][colonne] == Mark.EMPTY) {

                compteur++;

                ligne += direction[0];
                colonne += direction[1];
            }
        }

        return compteur;
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

        
           /*  if (isClosedBox(ligne, colonne) && pion != Mark.ROI) {
                System.out.println("mouvement invalide: case fermée sur le chemin");
                return false;
            }*/

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


}