import java.util.ArrayList;
import java.util.List;

// IMPORTANT: Il ne faut pas changer la signature des méthodes
// de cette classe, ni le nom de la classe.
// Vous pouvez par contre ajouter d'autres méthodes (ça devrait 
// être le cas)
class Board {
    private Mark[][] board;

    // Ne pas changer la signature de cette méthode
    public Board() {
        board = new Mark[13][13];
        board[6][6] = Mark.ROI;

        
    }

    // Place la pièce 'mark' sur le plateau, à la
    // position spécifiée dans Move
    //
    // Ne pas changer la signature de cette méthode
    public void play(Move m, Mark mark) {
        int colonne = m.getCol();
        int ligne = m.getRow();
        board[ligne][colonne] = mark;

    }


    // retourne  100 pour une victoire
    //          -100 pour une défaite
    //           0   pour un match nul
    // Ne pas changer la signature de cette méthode
    public int evaluate(Mark mark) {
        Mark adversaire;
        if (mark == Mark.X) {
            adversaire = Mark.O;
        } else {
            adversaire = Mark.X;
        }
        for (int i = 0; i < 3; i++) {
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

    public List<Move> coupsPossibles() {
        List<Move> moves = new ArrayList<>();

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == Mark.EMPTY) {
                    moves.add(new Move(i, j));
                }
            }
        }
        return moves;
    }

    public boolean estfini() {
        if (evaluate(Mark.X) == 100 || evaluate(Mark.X) == -100)
            return true;
        List<Move> list = coupsPossibles();
        if (list.isEmpty())
            return true;
        return false;
    }

    public boolean loadFromServer(String boardValues) {
        String[] values = boardValues.split(" ");
        int x = 0, y = 0;
        for (int i = 0; i < values.length; i++) {
            board[x][y] = Mark.fromInt(Integer.parseInt(values[i]));
            x++;
            if (x == 13) {
                x = 0;
                y++;
            }
        }
        return true;
    }
}

