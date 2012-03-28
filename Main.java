import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class Main {

	/**
	 * @param args
	 * @throws FileNotFoundException
	 *             , IOException
	 */
	public static void main(String[] args) throws FileNotFoundException,
			IOException {

		//test();
		
		if (args.length < 2) {
			System.err.println("usage: <board file> <command> [args]");
			System.err.println("valid commands: print play find load blank words undo distribution");
			System.exit(1);
		}

		String boardFile = args[0];
		String command = args[1];

		// read in dictionary of words. dictionary keys are the sorted letters
		// of the word,
		// dictionary value is an ArrayList of all the words that sort into the
		// key
		HashMap<String, ArrayList<String>> words = new HashMap<String, ArrayList<String>>();
		BufferedReader input = null;
		@SuppressWarnings("unused")
		int wordCount = 0;
		try {
			InputStream in = Main.class.getResourceAsStream("dictionary.txt");
			input = new BufferedReader(new InputStreamReader(in));
			String word;
			while ((word = input.readLine()) != null) {
				// sort the letters in the word
				char[] chars = word.toCharArray();
				java.util.Arrays.sort(chars);
				String key = new String(chars);

				if (words.containsKey(key)) {
					ArrayList<String> list = (ArrayList<String>) words.get(key);
					list.add(word);
				} else {
					ArrayList<String> list = new ArrayList<String>();
					list.add(word);
					words.put(key, list);
				}

				wordCount++;
			}
		} finally {
			if (input != null) {
				input.close();
			}
		}

		//System.out.println("loaded " + wordCount + " words into " + words.size() + " keys");

		Board b = loadBoard(boardFile);
		b.dictionary = words;

		if (command.equals("print")) {
			if (args.length < 2) {
				System.err.println("usage: print [ 'bonuses' ]");
			} else {
				if (args.length == 2) {
					b.print("letters");
				} else {
					b.print(args[2]);
				}
			}
		} else if (command.equals("play")) {
			if (args.length < 6) {
				System.err.println("usage: play <row> <col> <word> < 'row' | 'col' >");
			} else {
				int score = b.play(Integer.parseInt(args[2]),
						Integer.parseInt(args[3]), args[4], args[5]);
				if (score > 0) {
					b.print("letters");
					System.out.println("'" + args[4] + "' was worth " + score + " points");
				}
				else {
					System.err.println("That was not a valid play");
					System.exit(1);
				}
			}
		} else if (command.equals("find")) {
			if (args.length < 3) {
				System.err.println("usage: find <letters> <limit>");
			} else {
				b.print("letters");
				int limit = args.length == 4 ? Integer.parseInt(args[3]) : 0;
				b.findBest(args[2], limit);
			}
		} else if (command.equals("load")) {
			if (args.length < 3) {
				System.err.println("usage: load <filename>");
			} else {
				if (b.load(args[2])) {
					System.out.println("board loaded successfully");
				}
				else {
					System.exit(1);
				}
			}
		} else if (command.equals("blank")) {
			if (args.length < 4) {
				System.err.println("usage: blank <row> <col>");
			} else {
				b.toggleBlank(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
				b.print("letters");
			}
			
		} else if (command.equals("words")) {
			if (args.length < 3) {
				System.err.println("usage: words <letters>");
			}
			else {
				ArrayList<Character> letters = new ArrayList<Character>();
				for (char c : args[2].toCharArray()) letters.add(c);
					
				ArrayList<Word> list = b.allKnownWords(letters, null);
				Collections.sort(list);
				for (Word w : list) {
					System.out.println(w);
				}
			}
		}
		else if (command.equals("undo")) {
			int count = b.undo();
			if (count > 0) {
				System.out.printf("Undid %d tiles\n", count);
				b.print("letters");
			}
			else {
				System.out.println("Nothing to undo");
			}
		}
		else if (command.equals("distribution")) {
			String leftovers = args.length >= 3 ? args[2] : "";
			b.letterDistribution(leftovers);
		}
		else {
			System.err.println("unknown command: " + command);
		}

		storeBoard(b, boardFile);
	}

	private static void storeBoard(Board b, String name) {
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(name);
			out = new ObjectOutputStream(fos);
			out.writeObject(b);
			out.close();

			System.out.println("stored board in file '" + name + "'");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private static Board loadBoard(String name) {
		Board b = null;

		boolean exists = (new File(name)).exists();
		if (exists) {
			FileInputStream fis = null;
			ObjectInputStream in = null;

			try {
				fis = new FileInputStream(name);
				in = new ObjectInputStream(fis);
				b = (Board) in.readObject();
				in.close();
				System.out.println("loaded board '" + name + "'");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			b = new Board(15);
			storeBoard(b, name);
		}

		return b;
	}
	
	@SuppressWarnings("unused")
	private static void test() {
		HashSet<Word> h = new HashSet<Word>();
		
		Word w = new Word("foobar");
		h.add(w);
		w = new Word("baz");
		h.add(w);
		w = new Word("foobar");
		h.add(w);
		System.out.println(h);
		System.exit(0);
	}
}
