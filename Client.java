import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

class Client {

    Socket MyClient;
    BufferedInputStream input;
    BufferedOutputStream output;

    public Client() throws IOException {
        MyClient = new Socket("localhost", 8888);
        input = new BufferedInputStream(MyClient.getInputStream());
        output = new BufferedOutputStream(MyClient.getOutputStream());
    }

    public static void main(String[] args) {

        int coupsInvalide = 0;

        Move dernierCoupEnvoye = null;
        String dernierCoupEnvoyeServeur = null;
        boolean partieTerminee = false;

        try {
            Client client = new Client();
            Board b = new Board();

            IntelligenceArtificielle iA =
                new IntelligenceArtificielle();

            BufferedInputStream input = client.input;
            BufferedOutputStream output = client.output;

            while (true) {

                int valeur = input.read();

                if (valeur == -1) {

                    if (partieTerminee) {
                        System.out.println(
                            "Connexion fermée normalement après la partie."
                        );
                    } else {
                        System.out.println(
                            "Le serveur a fermé la connexion pendant la partie."
                        );
                    }

                    break;
                }

                char cmd = (char) valeur;

                System.out.println(
                    "Commande reçue : " + cmd
                );

                if (cmd == '1') {

                    partieTerminee = false;
                    coupsInvalide = 0;
                    dernierCoupEnvoye = null;
                    dernierCoupEnvoyeServeur = null;

                    b.SetCurrentPlayer(Mark.ROUGE);

                    String[] boardValues = lirePlateauExactement(input);

                    System.out.println("Plateau reçu : 169 valeurs.");

                    if (boardValues.length != 169) {
                        System.out.println(
                            "Erreur : 169 valeurs étaient attendues, "
                            + "mais "
                            + boardValues.length
                            + " ont été reçues."
                        );

                        break;
                    }

                    b.loadFromServer(boardValues);

                    System.out.println(
                        "Nouvelle partie! Vous jouez rouge."
                    );

                    Move mouvement =
                        iA.jouer(
                            b,
                            b.GetCurrentPlayer()
                        );

                    if (mouvement == null) {
                        System.out.println(
                            "Aucun coup possible."
                        );

                        break;
                    }

                    String move =
                        formatMoveToServer(mouvement);
                    dernierCoupEnvoye = mouvement;
                    dernierCoupEnvoyeServeur = move;

                    System.out.println(
                        "Premier coup envoyé : " + move
                    );

                    envoyerCoup(output, move);
                }

                if (cmd == '2') {

                    partieTerminee = false;
                    coupsInvalide = 0;
                    dernierCoupEnvoye = null;
                    dernierCoupEnvoyeServeur = null;

                    b.SetCurrentPlayer(Mark.NOIR);

                    String[] boardValues = lirePlateauExactement(input);

                    System.out.println("Plateau reçu : 169 valeurs.");

                    if (boardValues.length != 169) {
                        System.out.println(
                            "Erreur : 169 valeurs étaient attendues, "
                            + "mais "
                            + boardValues.length
                            + " ont été reçues."
                        );

                        break;
                    }

                    b.loadFromServer(boardValues);

                    System.out.println("Nouvelle partie! Vous jouez noir. "+ "Attendez le premier coup des rouges.");

                }

                if (cmd == '3') {

                    if (dernierCoupEnvoye != null) {
                        b.play(dernierCoupEnvoye,b.GetCurrentPlayer());
                        System.out.println("Notre coup précédent a été accepté : "+ dernierCoupEnvoyeServeur);
                        dernierCoupEnvoye = null;
                        dernierCoupEnvoyeServeur = null;
                    }

                    coupsInvalide = 0;

                    String s = lireCoupServeur(input);
                    System.out.println("Dernier coup adverse reçu : ["+ s+ "]");

                    Move coupAdverse =
                        formatServerToMove(s);

                    if (coupAdverse == null) {
                        System.out.println("Impossible de convertir le coup adverse.");
                        break;
                    }

                    Mark adversaire;

                    if (b.GetCurrentPlayer() == Mark.NOIR) {
                        adversaire = Mark.ROUGE;
                    } else {
                        adversaire = Mark.NOIR;
                    }

                    b.play(coupAdverse, adversaire);

                    System.out.println(
                        "Calcul du prochain coup..."
                    );

                    long debut = System.currentTimeMillis();

                    Move mouvement = iA.jouer(b,b.GetCurrentPlayer());

                    long fin = System.currentTimeMillis();

                    System.out.println("Temps de calcul de l'IA : "+ (fin - debut)+ " ms");

                    if (mouvement == null) {
                        System.out.println(
                            "Aucun coup possible."
                        );

                        break;
                    }

                    String move =
                        formatMoveToServer(mouvement);

                    /*
                     * Le coup reste en attente de confirmation.
                     */
                    dernierCoupEnvoye = mouvement;
                    dernierCoupEnvoyeServeur = move;

                    System.out.println(
                        "Coup envoyé au serveur : " + move
                    );

                    envoyerCoup(output, move);
                }

                if (cmd == '4') {

                    coupsInvalide++;

                    System.out.println(
                        "Coup invalide : "
                        + dernierCoupEnvoyeServeur
                    );

                    dernierCoupEnvoye = null;
                    dernierCoupEnvoyeServeur = null;

                    if (coupsInvalide >= 7) {
                        System.out.println(
                            "Trop de coups invalides consécutifs."
                        );

                        break;
                    }

                    Move mouvement =
                        iA.jouer(
                            b,
                            b.GetCurrentPlayer()
                        );

                    if (mouvement == null) {
                        System.out.println(
                            "Aucun autre coup possible."
                        );

                        break;
                    }

                    String move =
                        formatMoveToServer(mouvement);

                    dernierCoupEnvoye = mouvement;
                    dernierCoupEnvoyeServeur = move;

                    System.out.println(
                        "Nouveau coup envoyé : " + move
                    );

                    envoyerCoup(output, move);
                }

                if (cmd == '5') {

                    String s = lireCoupServeur(input);

                    System.out.println(
                        "Partie terminée. "
                        + "Dernier coup joué : "
                        + s
                    );

                    partieTerminee = true;

                    coupsInvalide = 0;
                    dernierCoupEnvoye = null;
                    dernierCoupEnvoyeServeur = null;
                    continue;
                }
            }

            client.fermer();

        } catch (IOException e) {

            System.out.println(
                "Erreur de communication : "
                + e.getMessage()
            );

        } catch (RuntimeException e) {

            System.out.println(
                "Erreur pendant l'exécution : "
                + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    private static String[] lirePlateauExactement(
        BufferedInputStream input
    ) throws IOException {

        String[] valeurs = new String[169];
        int index = 0;
        StringBuilder token = new StringBuilder();

        while (index < 169) {
            int lu = input.read();

            if (lu == -1) {
                throw new EOFException(
                    "Connexion fermée pendant la lecture du plateau."
                );
            }

            char c = (char) lu;

            if (Character.isWhitespace(c)) {
                if (token.length() > 0) {
                    valeurs[index++] = token.toString();
                    token.setLength(0);
                }
            } else {
                token.append(c);
            }
        }

        return valeurs;
    }

    private static String lireCoupServeur(
        BufferedInputStream input
    ) throws IOException {

        final long delaiMaxMs = 1000;
        final long silenceFinMs = 15;
        final int tailleMax = 16;

        long debut = System.currentTimeMillis();

        while (input.available() == 0) {
            if (System.currentTimeMillis() - debut >= delaiMaxMs) {
                throw new IOException(
                    "Aucun coup reçu après la commande du serveur."
                );
            }

            dormirLecture();
        }

        ByteArrayOutputStream message = new ByteArrayOutputStream();
        long derniereDonnee = System.currentTimeMillis();

        while (message.size() < tailleMax) {
            while (input.available() > 0 && message.size() < tailleMax) {
                int lu = input.read();

                if (lu == -1) {
                    throw new EOFException(
                        "Connexion fermée pendant la lecture d'un coup."
                    );
                }

                message.write(lu);
                derniereDonnee = System.currentTimeMillis();
            }

            if (System.currentTimeMillis() - derniereDonnee >= silenceFinMs) {
                break;
            }

            dormirLecture();
        }

        String coup = message.toString(StandardCharsets.US_ASCII).trim();

        if (coup.isEmpty()) {
            throw new IOException("Le coup reçu est vide.");
        }

        return coup;
    }

    private static void dormirLecture() throws IOException {
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Lecture interrompue.", e);
        }
    }

    private static String lireMessageDisponible(
        BufferedInputStream input,
        int tailleMax
    ) throws IOException {

        int essais = 0;

        while (
            input.available() == 0
            && essais < 100
        ) {
            try {
                Thread.sleep(2);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                throw new IOException(
                    "Lecture interrompue.",
                    e
                );
            }

            essais++;
        }

        int size = input.available();

        if (size <= 0) {
            throw new IOException(
                "Aucune donnée reçue après la commande."
            );
        }

        int tailleLecture =
            Math.min(size, tailleMax);

        byte[] buffer =
            new byte[tailleLecture];

        int bytesRead =
            input.read(
                buffer,
                0,
                tailleLecture
            );

        if (bytesRead == -1) {
            throw new IOException(
                "Connexion fermée pendant la lecture."
            );
        }

        return new String(
            buffer,
            0,
            bytesRead,
            StandardCharsets.US_ASCII
        ).trim();
    }

    private static void envoyerCoup(
        BufferedOutputStream output,
        String move
    ) throws IOException {

        byte[] donnees =
            move.getBytes(
                StandardCharsets.US_ASCII
            );

        output.write(
            donnees,
            0,
            donnees.length
        );

        output.flush();
    }

    private static String formatMoveToServer(
        Move m
    ) {

        if (m == null) {
            throw new IllegalArgumentException(
                "Le mouvement ne peut pas être null."
            );
        }

        char colDep =
            (char) (
                'A' + m.getColDepart()
            );

        char colArr =
            (char) (
                'A' + m.getColArrive()
            );

        int ligDep =
            13 - m.getRowDepart();

        int ligArr =
            13 - m.getRowArrive();

        return ""
            + colDep
            + ligDep
            + "-"
            + colArr
            + ligArr;
    }

    public static Move formatServerToMove(
        String s
    ) {

        if (s == null) {
            return null;
        }

        try {
            s = s.trim();
            s = s.replaceAll("\\s+", "");

            String[] parties =
                s.split("-");

            if (parties.length != 2) {
                return null;
            }

            if (
                parties[0].length() < 2
                || parties[1].length() < 2
            ) {
                return null;
            }

            char depart =
                parties[0].charAt(0);

            char arrivee =
                parties[1].charAt(0);

            int colDepart =
                depart - 'A';

            int colArrive =
                arrivee - 'A';

            int ligneDepart =
                13 - Integer.parseInt(
                    parties[0].substring(1)
                );

            int ligneArrive =
                13 - Integer.parseInt(
                    parties[1].substring(1)
                );

            if (
                ligneDepart < 0
                || ligneDepart >= 13
                || ligneArrive < 0
                || ligneArrive >= 13
                || colDepart < 0
                || colDepart >= 13
                || colArrive < 0
                || colArrive >= 13
            ) {
                return null;
            }

            return new Move(
                ligneDepart,
                colDepart,
                ligneArrive,
                colArrive
            );

        } catch (
            NumberFormatException
            | StringIndexOutOfBoundsException e
        ) {

            System.out.println(
                "Format de coup invalide reçu : ["
                + s
                + "]"
            );

            return null;
        }
    }

    public String[] boardValues()
        throws IOException {

        String s =
            lireMessageDisponible(
                input,
                1024
            );

        return s
            .trim()
            .split("\\s+");
    }

    /*
     * Ferme proprement les flux et le socket.
     */
    public void fermer() {

        try {
            input.close();

        } catch (IOException e) {

            System.out.println(
                "Erreur lors de la fermeture de l'entrée."
            );
        }

        try {
            output.close();

        } catch (IOException e) {

            System.out.println(
                "Erreur lors de la fermeture de la sortie."
            );
        }

        try {
            MyClient.close();

        } catch (IOException e) {

            System.out.println(
                "Erreur lors de la fermeture du socket."
            );
        }
    }
}