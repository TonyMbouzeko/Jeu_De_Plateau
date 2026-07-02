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

		try {
			Client client = new Client();
			Board b = new Board();
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
					String move = null;
					move = console.readLine();
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
					System.out.println("Dernier coup :" + s);
					System.out.println("Entrez votre coup : ");
					String move = null;
					move = console.readLine();
					output.write(move.getBytes(), 0, move.length());
					output.flush();

				}
				// Le dernier coup est invalide
				if (cmd == '4') {
					System.out.println("Coup invalide, entrez un nouveau coup : ");
					String move = null;
					move = console.readLine();
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

}
