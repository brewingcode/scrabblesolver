package com.alex.scrabblesolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
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
		
		if (args.length < 3) {
			System.err.println("usage: java Main <dictionary file> <board file> [command]");
			System.err.println("valid commands: print play find load");
			System.exit(1);
		}

		String dictionaryFile = args[0];
		String boardFile = args[1];
		String command = args[2];

		// read in dictionary of words. dictionary keys are the sorted letters
		// of the word,
		// dictionary value is an ArrayList of all the words that sort into the
		// key
		HashMap<String, ArrayList<String>> words = new HashMap<String, ArrayList<String>>();
		BufferedReader input = null;
		int wordCount = 0;
		try {
			input = new BufferedReader(new FileReader(dictionaryFile));
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

		System.out.println("loaded " + wordCount + " words into "
				+ words.size() + " keys");

		Board b = loadBoard(boardFile);
		b.dictionary = words;

		if (command.equals("print")) {
			if (args.length < 3) {
				System.err.println("usage: print [ 'bonuses' ]");
			} else {
				if (args.length == 3) {
					b.print("letters");
				} else {
					b.print(args[3]);
				}
			}
		} else if (command.equals("play")) {
			if (args.length < 7) {
				System.err
						.println("usage: play <row> <col> <word> < 'row' | 'col' >");
			} else {
				int score = b.play(Integer.parseInt(args[3]),
						Integer.parseInt(args[4]), args[5], args[6]);
				if (score > 0) {
					b.print("letters");
					System.out.println("'" + args[5] + "' was worth " + score
							+ " points");
				}
			}
		} else if (command.equals("find")) {
			if (args.length < 5) {
				System.err.println("usage: find <letters> <limit>");
			} else {
				b.print("letters");
				b.findBest(args[3], Integer.parseInt(args[4]));
			}
		} else if (command.equals("load")) {
			if (args.length < 4) {
				System.err.println("usage: load <filename>");
			} else {
				if (b.load(args[3])) {
					System.out.println("board loaded successfully");
				}
				else {
					System.exit(1);
				}
			}
		} else {
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
