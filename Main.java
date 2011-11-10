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

public class Main {

	/**
	 * @param args
	 * @throws FileNotFoundException, IOException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		
		if (args.length < 2) {
			System.err.println("usage: java Main <dictionary> <board>");
			System.exit(1);
		}
		
		String dictionaryFile = args[0];
		String boardFile = args[1];
		
		// read in dictionary of words. dictionary keys are the sorted letters of the word,
		// dictionary value is an ArrayList of all the words that sort into the key
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
				}
				else {
					ArrayList<String> list = new ArrayList<String>();
					list.add(word);
					words.put(key, list);
				}
				
				wordCount++;
			}
		}
		finally {
			if (input != null) {
				input.close();
			}
		}
		
		System.out.println("loaded " + wordCount + " words into " + words.size() + " keys");

		Board b = loadBoard(boardFile);
		
		// do stuff
		
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
		}
		catch (IOException ex) {
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
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		else {
			b = new Board(15);
		}
		
		return b;
	}
}
