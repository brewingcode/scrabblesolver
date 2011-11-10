package com.alex.scrabblesolver;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

	/**
	 * @param args
	 * @throws FileNotFoundException, IOException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		
		if (args.length < 1) {
			System.err.println("filename of dictionary required");
			System.exit(1);
		}
		
		// read in dictionary of words. dictionary keys are the sorted letters of the word,
		// dictionary value is an ArrayList of all the words that sort into the key
		HashMap<String, ArrayList<String>> words = new HashMap<String, ArrayList<String>>();
		BufferedReader input = null;
		int wordCount = 0;
		try {
			input = new BufferedReader(new FileReader(args[0]));
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
		Board b = new Board(15);
		//System.out.println(b.toString());
	}

}
