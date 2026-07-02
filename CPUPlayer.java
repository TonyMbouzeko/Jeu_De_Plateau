import java.util.ArrayList;
import java.util.List;

// IMPORTANT: Il ne faut pas changer la signature des méthodes
// de cette classe, ni le nom de la classe.
// Vous pouvez par contre ajouter d'autres méthodes (ça devrait 
// être le cas)
class CPUPlayer {

    // Contient le nombre de noeuds visités (le nombre
    // d'appel à la fonction MinMax ou Alpha Beta)
    // Normalement, la variable devrait être incrémentée
    // au début de votre MinMax ou Alpha Beta.
    private int numExploredNodes;
    private Mark cpu;
    private Mark adversaire;

    // Le constructeur reçoit en paramètre le
    // joueur MAX (X ou O)
    public CPUPlayer(Mark cpu) {
        this.cpu = cpu;
        if (cpu == Mark.ROUGE) {
            adversaire = Mark.NOIR;
        } else {
            adversaire = Mark.ROUGE;
        }
    }

    // Ne pas changer cette méthode
    public int getNumOfExploredNodes() {
        return numExploredNodes;
    }

    // Retourne la liste des coups possibles. Cette liste contient
    // plusieurs coups possibles si et seuleument si plusieurs coups
    // ont le même score.
    public ArrayList<Move> getNextMoveMinMax(Board board) {
        numExploredNodes = 0;
        int meilleurScore = -200;
        List<Move> list = board.coupsPossibles(cpu);
        ArrayList<Move> bonCoups = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            board.play(list.get(i), cpu);
            int score = minMax(board, adversaire);
            board.play(list.get(i), Mark.EMPTY);
            if (score > meilleurScore) {
                bonCoups.clear();
                bonCoups.add(list.get(i));
                meilleurScore = score;
            } else if (meilleurScore == score) {
                bonCoups.add(list.get(i));
            }
        }
        return bonCoups;
    }

    // Retourne la liste des coups possibles. Cette liste contient
    // plusieurs coups possibles si et seuleument si plusieurs coups
    // ont le même score.
    public ArrayList<Move> getNextMoveAB(Board board) {
        numExploredNodes = 0;
        int meilleurScrore = -200;
        List<Move> list = board.coupsPossibles(cpu);
        ArrayList<Move> bonCoups = new ArrayList<>();
        for (Move move : list) {
            board.play(move, cpu);
            int score = alphaBeta(board, adversaire, -999, 999);
            board.play(move, Mark.EMPTY);
            if (score > meilleurScrore) {
                bonCoups.clear();
                bonCoups.add(move);
                meilleurScrore = score;
            } else if (meilleurScrore == score) {
                bonCoups.add(move);
            }
        }
        return bonCoups;
    }

    public int minMax(Board board, Mark mark) {
        numExploredNodes++;
        int meilleur = 0;
        int score = 0;
        if (board.estfini())
            return board.evaluate(cpu);

        List<Move> list = board.coupsPossibles();

        if (mark == cpu) {
            meilleur = -9999;
            for (int i = 0; i < list.size(); i++) {
                board.play(list.get(i), cpu);
                score = minMax(board, adversaire);
                board.play(list.get(i), Mark.EMPTY);
                meilleur = Math.max(meilleur, score);
            }
            return meilleur;
        } else {
            meilleur = 9999;
            for (int i = 0; i < list.size(); i++) {
                board.play(list.get(i), adversaire);
                score = minMax(board, cpu);
                board.play(list.get(i), Mark.EMPTY);
                meilleur = Math.min(meilleur, score);
            }
            return meilleur;
        }
    }

    public int alphaBeta(Board board, Mark mark, int alpha, int beta) {
        numExploredNodes++;
        if (board.estfini())
            return board.evaluate(cpu);
        int valeur = 0;
        int score = 0;
        List<Move> list = board.coupsPossibles();
        if (mark == cpu) {
            valeur = -999;
            for (Move move : list) {
                board.play(move, cpu);
                score = alphaBeta(board, adversaire, alpha, beta);
                board.play(move, Mark.EMPTY);
                valeur = Math.max(valeur, score);
                alpha = Math.max(alpha, valeur);
                if (alpha >= beta)
                    break;
            }
            return valeur;
        } else {
            valeur = 999;
            for (Move move : list) {
                board.play(move, adversaire);
                score = alphaBeta(board, cpu, alpha, beta);
                board.play(move, Mark.EMPTY);
                valeur = Math.min(valeur, score);
                beta = Math.min(beta, valeur);
                if (alpha >= beta)
                    break;
            }
            return valeur;
        }
    }

}
