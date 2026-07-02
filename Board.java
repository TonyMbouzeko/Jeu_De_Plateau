import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// IMPORTANT: Il ne faut pas changer la signature des méthodes
// de cette classe, ni le nom de la classe.
// Vous pouvez par contre ajouter d'autres méthodes (ça devrait 
// être le cas)
class Board {
    private Mark[][] board;
    //Variable utilisée pour savoir quel joueur est en train de jouer (ROUGE ou NOIR)
    private Mark currentPlayer;

    // Ne pas changer la signature de cette méthode
    public Board() {
        board = new Mark[13][13];
    }

    public void SetCurrentPlayer(Mark player) {
        this.currentPlayer = player;
    }

    public Mark GetCurrentPlayer() {
        return this.currentPlayer;
    }

    // Place la pièce 'mark' sur le plateau, à la
    
    // position spécifiée dans Move
    //
    // Ne pas changer la signature de cette méthode

    // La case de départ est vide après le coup et la case d'arrivée contient la pièce.
    public void play(Move m, Mark mark) {
        board[m.getRowDepart()][m.getColDepart()] = mark.EMPTY;
        board[m.getRowArrive()][m.getColArrive()] = mark;
    }

    // Vérifie si la pièce 'mark' appartient à un certains camp (ROUGE ou NOIR)
    private boolean belongsTo(Mark mark, Mark camp) {
        if(mark == null || camp == Mark.EMPTY) {
            return false;
        }
        if(camp == Mark.NOIR) {
            return mark == Mark.NOIR;
        } 
        return mark == Mark.ROUGE || mark == Mark.ROI;
    }

    //vérifie si une case de cordonnées (r, c) est une case fermée.
    private boolean isClosedBox(int r, int c) {
        int n = board.length;
        boolean coin = (r == 0 || r == n - 1) && (c == 0 || c == n - 1);
        boolean trone = (r == n / 2 && c == n / 2);
        return coin || trone;
    }

    // retourne  100 pour une victoire
    //          -100 pour une défaite
    //           0   pour un match nul
    // Ne pas changer la signature de cette méthode
    public int evaluate(Mark mark) {
        if (roiAuCoin()){
            if (mark == Mark.NOIR){
                return 100;
            }else{
                return -100;
            }
        }else if (!roiSurPlateau()){
            if (mark == Mark.ROUGE){
                return 100;
            }else{
                return -100;
            }
        }
        List<Move> coups = coupsPossibles(mark);

        if (!coups.isEmpty()){
            return 0;
        }

        return 0;
    }

    public List<Move> coupsPossibles(Mark camp) {
        List<Move> moves = new ArrayList<>();
        int n = board.length;
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if(!belongsTo(board[i][j], camp)) {
                    continue;
                }
                Mark piece = board[i][j];
                for(int[] d : directions) {
                    int r = i, c = j ;
                    while(true) {
                        r += d[0];
                        c += d[1];
                        // Vérifie si la case est en dehors du plateau
                        if( r < 0 || r >= n || c < 0 || c >= n) {
                            break;
                        }
                        // Vérifie si la case est occupée par une pièce
                        if(board[r][c] != Mark.EMPTY) {
                            break;
                        } 
                        //Vérifie si la case est une case fermée pour une pièce qui n'est pas le roi.
                        if(isClosedBox(r, c) && piece != Mark.ROI) {
                            break;
                        }
                        moves.add(new Move(i, j, r, c));
                    }
                }
            }
        }
        return moves;
    }
    public List<Move> coupsPossibles(){
        return coupsPossibles(currentPlayer);
    }

    public boolean estfini() {
        return false;
    }

    public Mark[][] loadFromServer(String[] boardValues){
        Mark[][] tableau = new Mark[13][13];
        int x = 0, y = 0;
        for (int i = 0; i < boardValues.length; i++) {
            tableau[x][y] = conversion(Integer.parseInt(boardValues[i]));
            x++;
            if (x == 13) {
                x = 0;
                y++;
            }
        }

        this.board = tableau;
        return tableau;
    }

    public Mark conversion(int valeur){
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

    public boolean roiAuCoin(){
         if (board[0][0] == Mark.ROI || board[0][12] == Mark.ROI || board[12][12]== Mark.ROI || board[12][0] == Mark.ROI ){
            return true;
        }
        return false;
    }

    public boolean roiSurPlateau(){
        for (int i =0; i< board.length; i++){
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == Mark.ROI){
                    return true;
                }
            }
        }
        return false;
    }
}
