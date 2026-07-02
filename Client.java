import java.io.*;
import java.net.*;

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

		
		int[][] board = new int[13][13];
		Move dernierCoupEnvoye = null;
		String dernierCoupEnvoyeServeur = null;

		try {
			Client client = new Client();
			Board b = new Board();
			IntelligenceArtificielle iA = new IntelligenceArtificielle();
			BufferedInputStream input = client.input;
			BufferedOutputStream output = client.output;
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			
			while (1 == 1) {
				char cmd = 0;
				cmd = (char) input.read();
				System.out.println(cmd);
				// Debut de la partie en joueur rouge
				if (cmd == '1') {
					b.SetCurrentPlayer(Mark.ROUGE);
					byte[] aBuffer = new byte[1024];
					int size = input.available();
					// System.out.println("size " + size);
					input.read(aBuffer, 0, size);
					String s = new String(aBuffer).trim();
					System.out.println(s);
					String[] boardValues;
					boardValues = s.split(" ");
					b.loadFromServer(boardValues);

					System.out.println("Nouvelle partie! Vous jouer rouge, entrez votre premier coup : ");
					Move mouvement = iA.jouer(b, b.GetCurrentPlayer());
					String move = formatMoveToServer(mouvement);
					output.write(move.getBytes(), 0, move.length());
					output.flush();
				}
				// Debut de la partie en joueur Noir
				if (cmd == '2') {
					b.SetCurrentPlayer(Mark.NOIR);
					System.out.println("Nouvelle partie! Vous jouer noir, attendez le coup des rouges");
					byte[] aBuffer = new byte[1024];

					int size = input.available();
					// System.out.println("size " + size);
					input.read(aBuffer, 0, size);
					String s = new String(aBuffer).trim();
					System.out.println(s);
					String[] boardValues;
					boardValues = s.split(" ");
					b.loadFromServer(boardValues);
				}

				// Le serveur demande le prochain coup
				// Le message contient aussi le dernier coup joue.
				if (cmd == '3') {
					byte[] aBuffer = new byte[16];

					int size = input.available();
					System.out.println("size :" + size);
					input.read(aBuffer, 0, size);

					String s = new String(aBuffer);
					Mark adversaire;
					if (b.GetCurrentPlayer() == Mark.NOIR){
						adversaire = Mark.ROUGE;
					}else{
						adversaire = Mark.NOIR;
					}

					Move coupAdverse = formatServerToMove(s);
					b.play(coupAdverse,  adversaire);
					System.out.println("Dernier coup :" + s);
					System.out.println("Entrez votre coup : ");
					Move mouvement = iA.jouer(b, b.GetCurrentPlayer());

					b.play(mouvement, b.GetCurrentPlayer());

					dernierCoupEnvoye = mouvement;
					dernierCoupEnvoyeServeur = formatMoveToServer(mouvement);

					System.out.println("Coup envoyé au serveur : " + dernierCoupEnvoyeServeur);

					String move = formatMoveToServer(mouvement);
					output.write(move.getBytes(), 0, move.length());
					output.flush();

				}
				// Le dernier coup est invalide
				if (cmd == '4') {
					System.out.println("Coup invalide, entrez un nouveau coup :");
					System.out.println("Dernière chaîne envoyée : " + dernierCoupEnvoyeServeur);
				
					Move mouvement = iA.jouer(b, b.GetCurrentPlayer());
					String move = formatMoveToServer(mouvement);
					output.write(move.getBytes(), 0, move.length());
					output.flush();

				}
				// La partie est terminée
				if (cmd == '5') {
					byte[] aBuffer = new byte[16];
					int size = input.available();
					input.read(aBuffer, 0, size);
					String s = new String(aBuffer);
					System.out.println("Partie Terminé. Le dernier coup joué est: " + s);
					String move = null;
					move = console.readLine();
					output.write(move.getBytes(), 0, move.length());
					output.flush();

				}
			}
		} catch (IOException e) {
			System.out.println(e);
		}

	}

	public String[] boardValues() throws IOException {

		byte[] aBuffer = new byte[1024];
		int size = input.available();
		input.read(aBuffer, 0, size);
		String s = new String(aBuffer).trim();
		String[] boardValues;
		boardValues = s.split(" ");

		return boardValues;
	}

	private static String formatMoveToServer(Move m) {
    char colDep = (char) ('A' + m.getColDepart());
    char colArr = (char) ('A' + m.getColArrive());

    
    int ligDep = 13 - m.getRowDepart();
    int ligArr = 13 - m.getRowArrive();

    // Format attendu par le serveur : A13-B13
    return "" + colDep + ligDep + "-" + colArr + ligArr;
}

	public static Move formatServerToMove(String s){
		s = s.trim();
		s = s.replaceAll("\\s+","");
		String[] parties = s.split("-");
		if (parties.length != 2){
			return null;
		}

		char depart = parties[0].charAt(0);
		int coldepart = depart -'A';

		int ligneDepart = 13 - Integer.parseInt(parties[0].substring(1));

		char arrivee = parties[1].charAt(0);
		int colArrive = arrivee - 'A';

		int ligneArrive = 13 - Integer.parseInt(parties[1].substring(1));

		return new Move( ligneDepart, coldepart, ligneArrive, colArrive);
	}

}
