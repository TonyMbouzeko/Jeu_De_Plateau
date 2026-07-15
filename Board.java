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

    public void SetCurrentPlayer(Mark player) {
        this.currentPlayer = player;
    }

    public Mark GetCurrentPlayer() {
        return this.currentPlayer;
    }

    // Ne pas changer la signature de cette méthode.
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
                return 100000;
            }else{
                return -100000;
            }
        }

        if (!roiSurPlateau()) {
            if (mark == Mark.ROUGE){
                return 100000;
            }else{
                return -100000;
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
        int scoreRouge = nombreRouges - nombreNoirs;
        int distanceCoin = distanceCoinPlusProche(ligneRoi, colonneRoi);
        int scoreDistanceRoi = (12 - distanceCoin)*5;

        scoreRouge -= scoreDistanceRoi;

        // -----------------------------------------------------------------------------------------------------------
        if (mark == Mark.ROUGE){
            return scoreRouge;
        }else{
            return -scoreRouge;
        }
    }


    
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
                            break;
                        }

                        moves.add(new Move(i, j, r, c));
                    }
                }
            }
        }

        return moves;
    }

    public List<Move> coupsPossibles() {
        return coupsPossibles(currentPlayer);
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
        return board[0][0] == Mark.ROI
                || board[0][12] == Mark.ROI
                || board[12][12] == Mark.ROI
                || board[12][0] == Mark.ROI;
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

            if (pieceEnnemie == null
                    || pieceEnnemie == Mark.EMPTY
                    || belongsTo(pieceEnnemie, camp)
                    || pieceEnnemie == Mark.ROI) {
                continue;
            }

            if (!estDansPlateau(ligneAppui, colonneAppui)) {
                continue;
            }

            Mark pieceAppui = board[ligneAppui][colonneAppui];
            boolean capture = belongsTo(pieceAppui, camp)
                    || isClosedBox(ligneAppui, colonneAppui);

            if (capture) {
                board[ligneEnnemi][colonneEnnemi] = Mark.EMPTY;
            }
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
}