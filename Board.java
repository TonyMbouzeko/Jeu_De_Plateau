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
    public Board()throws IOException {
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
        Mark adversaire;
        if (mark == Mark.ROUGE) {
            adversaire = Mark.NOIR;
        } else {
            adversaire = Mark.ROUGE;
        }
        for (int i = 0; i < 13; i++) {
            if ((board[i][0] == mark && board[i][1] == mark && board[i][2] == mark) ||
                    (board[0][i] == mark && board[1][i] == mark && board[2][i] == mark) ||
                    (board[0][0] == mark && board[1][1] == mark && board[2][2] == mark)) {
                return 100;
            } else if ((board[i][0] == adversaire && board[i][1] == adversaire && board[i][2] == adversaire) ||
                    (board[0][i] == adversaire && board[1][i] == adversaire && board[2][i] == adversaire) ||
                    (board[0][0] == adversaire && board[1][1] == adversaire && board[2][2] == adversaire)) {
                return -100;
            }

            if (board[0][2] == mark && board[1][1] == mark && board[2][0] == mark) {
                return 100;
            }
            if (board[0][2] == adversaire && board[1][1] == adversaire && board[2][0] == adversaire) {
                return -100;
            }

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
}
