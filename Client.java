import java.io.*;
import java.net.*;
import java.util.List;

class Client {

    Socket myClient;
    BufferedInputStream input;
    BufferedOutputStream output;

    public Client() throws IOException {
        myClient = new Socket("localhost", 8888);
        input = new BufferedInputStream(myClient.getInputStream());
        output = new BufferedOutputStream(myClient.getOutputStream());
    }

    public static void main(String[] args) {
        try {
            Client client = new Client();
            Board b = new Board();
            BufferedInputStream input = client.input;
            BufferedOutputStream output = client.output;
            
            Mark monCamp = Mark.EMPTY;

            while (1 == 1) {
                int cmdByte = input.read();
                char cmd = (char) cmdByte;
                System.out.println(cmd);
                // Debut de la partie en joueur rouge
                if (cmd == '1') {
                    monCamp = Mark.ROUGE;
                    b.SetCurrentPlayer(Mark.ROUGE);

                    String s = client.readAllAvailableOrWait(1024);
                    System.out.println("Plateau initial reçu :\n" + s);
                    
                    String[] boardValues = s.split(" ");
                    b.loadFromServer(boardValues);

                    System.out.println("Nouvelle partie! Vous jouer rouge, entrez votre premier coup : ");
                    
                    IntelligenceArtificielle ai = new IntelligenceArtificielle();
                    Move coupChoisi = ai.getBestMove(b, Mark.ROUGE);
                    
                    if (coupChoisi != null) {
                        b.play(coupChoisi, Mark.ROUGE);
                        String stringCoup = formatMoveToServer(coupChoisi);
                        System.out.println("L'IA joue (Premier coup Rouge) : " + stringCoup);
                        
                        output.write(stringCoup.getBytes(), 0, stringCoup.length());
                        output.flush();
                    }
                }
                // Debut de la partie en joueur Noir
                if (cmd == '2') {
                    monCamp = Mark.NOIR;
                    b.SetCurrentPlayer(Mark.NOIR);
                    System.out.println("Nouvelle partie! Vous jouer noir, attendez le coup des rouges");

                    String s = client.readAllAvailableOrWait(1024);
                    
                    String[] boardValues = s.split(" ");
                    b.loadFromServer(boardValues);
                }

                // Le serveur demande le prochain coup
				// Le message contient aussi le dernier coup joue.
                if (cmd == '3') {
                    String s = client.readAllAvailableOrWait(32);
                    System.out.println("Dernier coup reçu du serveur : " + s);

                    Mark maCouleur = monCamp;
                    Mark couleurAdversaire = (maCouleur == Mark.ROUGE) ? Mark.NOIR : Mark.ROUGE;

                    if (s.contains("-")) {
                        Move coupAdversaire = parseServerMove(s);
                        if (coupAdversaire != null) {
                            b.play(coupAdversaire, couleurAdversaire);
                        }
                    }

                    b.SetCurrentPlayer(maCouleur);

                    IntelligenceArtificielle ai = new IntelligenceArtificielle();
                    Move coupChoisi = ai.getBestMove(b, maCouleur);

                    if (coupChoisi != null) {
                        b.play(coupChoisi, maCouleur);
                        String stringCoup = formatMoveToServer(coupChoisi);
                        System.out.println("L'IA décide de jouer : " + stringCoup);
                        
                        output.write(stringCoup.getBytes(), 0, stringCoup.length());
                        output.flush();
                    } else {
                        System.out.println("Erreur : Aucun coup trouvé par l'IA.");
                    }
                }
                // Le dernier coup est invalide
                if (cmd == '4') {
                    System.out.println("Coup invalide, entrez un nouveau coup : ");
                    
                    List<Move> coupsLegaux = b.coupsPossibles(monCamp);
                    if (coupsLegaux != null && !coupsLegaux.isEmpty()) {
                        Move coupSecours = coupsLegaux.get(0);
                        b.play(coupSecours, monCamp);
                        String stringCoup = formatMoveToServer(coupSecours);
                        System.out.println("Envoi du coup de secours débloquant : " + stringCoup);
                        output.write(stringCoup.getBytes(), 0, stringCoup.length());
                        output.flush();
                    }
                }
                // La partie est terminée
                if (cmd == '5') {
                    String s = client.readAllAvailableOrWait(64);
                    System.out.println("Partie Terminé. Le dernier coup joué est: " + s);
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Exception de communication réseau rencontrée : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String readAllAvailableOrWait(int bufferSize) throws IOException {
        ByteArrayOutputStream bufferMessage = new ByteArrayOutputStream();
        byte[] temporaryBuffer = new byte[bufferSize];
        
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        int totalRead = 0;
        while (input.available() > 0) {
            int readNow = input.read(temporaryBuffer, 0, Math.min(temporaryBuffer.length, input.available()));
            if (readNow == -1) break;
            bufferMessage.write(temporaryBuffer, 0, readNow);
            totalRead += readNow;
            
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        }
        
        return bufferMessage.toString("UTF-8").trim();
    }

    private static String formatMoveToServer(Move m) {
        char colDep = (char) ('A' + m.getColDepart());
        int ligDep = 13 - m.getRowDepart();
        char colArr = (char) ('A' + m.getColArrive());
        int ligArr = 13 - m.getRowArrive();
        return "" + colDep + ligDep + "-" + colArr + ligArr;
    }

    private static Move parseServerMove(String s) {
        try {
            s = s.replaceAll("\\s+", "");
            s = s.replaceAll("[^A-M0-9-]", "");

            String[] parts = s.split("-");
            if (parts.length != 2) return null;

            int colDep = parts[0].charAt(0) - 'A';
            int ligDep = 13 - Integer.parseInt(parts[0].substring(1));
            int colArr = parts[1].charAt(0) - 'A';
            int ligArr = 13 - Integer.parseInt(parts[1].substring(1));

            return new Move(ligDep, colDep, ligArr, colArr);
        } catch (Exception e) {
            System.out.println("Erreur de parsing sur le coup adverse reçu : " + e.getMessage());
            return null;
        }
    }
}