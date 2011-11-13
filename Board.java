package com.alex.scrabblesolver;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class Board implements Serializable {
	
	// dictionary of words (see Main.java)
	public transient HashMap<String, ArrayList<String>> dictionary;
	
	// bingo rules
	private int bingoCount;
	private int bingoBonus;
	
	// the tiles on the board (ie, the state of the game)
	private Tile[][] tiles;
	
	// point values of the letters (changes for different rule sets)
	private int[] letterValues;
	
	// what turn we are on
	private int turn;
	
	// be noisy on stderr
	// (Some functions have commented out tracing on System.err, these were 
	// development-time tracing for debugging)
	private transient boolean noisy;
	
	// constructor
	public Board(int size) {
		tiles = new Tile[size][size];
		for (int row = 0; row < size; row++) {
			for (int col = 0; col < size; col++) {
				tiles[row][col] = new Tile(row,col);
			}
		}
		
		letterValues = null;
		turn = 0;
		bingoCount = -1;
		bingoBonus = 0;
		noisy = false;
		
		wordsWithFriends();
	}

	// setup board with WWF rules
	private void wordsWithFriends() {
		letterValues = new int[] { 1, 4, 4, 2, 1, 4, 3, 3, 1, 10, 5, 2, 4, 2, 1, 4, 10, 1, 1, 1, 2, 5, 4, 8, 3, 10 };
		bingoCount = 7;
		bingoBonus = 35;
		
		wordsWithFriendsQuadrant(1,1);
		wordsWithFriendsQuadrant(-1,1);
		wordsWithFriendsQuadrant(1,-1);
		wordsWithFriendsQuadrant(-1,-1);
	}

	// helper function, assigns bonus tile values in a quadrant of the board
	private void wordsWithFriendsQuadrant(int x, int y) {
		cartesianIndex(4*x, 0).wordBonus = 2;
		cartesianIndex(3*x, 1*y).letterBonus = 2;
		cartesianIndex(7*x, 1*y).letterBonus = 3;
		cartesianIndex(2*x, 2*y).letterBonus = 3;
		cartesianIndex(6*x, 2*y).wordBonus = 2;
		cartesianIndex(1*x, 3*y).letterBonus = 2;
		cartesianIndex(5*x, 3*y).letterBonus = 2;
		cartesianIndex(4*x, 4*y).letterBonus = 3;
		cartesianIndex(7*x, 4*y).wordBonus = 3;
		cartesianIndex(3*x, 5*y).letterBonus = 2;
		cartesianIndex(6*x, 5*y).letterBonus = 2;
		cartesianIndex(2*x, 6*y).wordBonus = 2;
		cartesianIndex(5*x, 6*y).letterBonus = 2;
		cartesianIndex(1*x, 7*y).letterBonus = 3;
		cartesianIndex(4*x, 7*y).wordBonus = 3;
		cartesianIndex(0, 4*y).wordBonus = 2;
	}
	
	// access a tile based on Cartesian geometry, with {0,0} in the center of the board
	private Tile cartesianIndex(int x, int y) {
		int center = tiles.length / 2;
		int row = center + y;
		int col = center + x;
		
		return tiles[row][col];
	}
	
	// "letters" or "bonuses", defaults to "letters"
	public void print(String which) {
		if (which == null) {
			which = "letters";
		}
		System.out.print(toString(which));
	}
	
	
	public String toString() {
		return toString("letters");
	}

	// dumping method
	private String toString(String which) {
		String b = "";
		int size = tiles.length;
		
		if (which.equals("bonuses")) {
			// print out the bonus tiles on the board
			for (int row = 0; row < size; row++) {
				for (int col = 0; col < size; col++) {
					if (tiles[row][col].letterBonus == 2) {
						b += "DL";
					}
					else if (tiles[row][col].letterBonus == 3) {
						b += "TL";
					}
					else if (tiles[row][col].wordBonus == 2) {
						b += "DW";
					}
					else if (tiles[row][col].wordBonus == 3) {
						b += "TW";
					}
					else {
						b += "  ";
					}
					
					if (col == size - 1) {
						b += "\n\n";
					}
					else {
						b += "  ";
					}
				}
			}
		}
		else if (which.equals("letters")) {
			for (int row = 0; row < size; row++) {
				for (int col = 0; col < size; col++) {
					char c = tiles[row][col].letter;
					if (c == Tile.Empty) {
						if (tiles[row][col].letterBonus > 1 || tiles[row][col].wordBonus > 1) {
							b += "*";
						}
						else {
							b += Tile.Empty;
						}
					}
					else {
						b += tiles[row][col].fresh ? Character.toUpperCase(c) : c;
					}
					if (col == size - 1) {
						b += "\n";
					}
					else {
						b += " ";
					}
				}
			}
		}
		else {
			if (noisy) System.out.printf("unknown 'which': %s (try 'bonuses' or 'letters')\n", which);
		}
		
		return b;
	}

	// plays the given word at the row and col specified, returns the score for the play, or
	// 0 if the play is invalid
	// WARNING: row and col are 1-based indexes NOT 0-based!!!
	public int play(int row, int col, String word, String dir) {
		int score = 0;
		if (fits(row, col, word, dir)) {
			score = score(true, null);
		}
		clearPending();
		
		return score;
	}
	
	// this function does two things:
	// 1. calculates the score of the current play (based on the "pending" flag on each tile)
	// 2. if "commit" is true, then:
	// 		a. clear the old "fresh" tiles from the previous turn
	// 		b. increment the "turn" counter by 1
	// 		c. clear all "pending" flags
	// 3. if "word" is non-null, then:
	//   	a. assign the score to it
	//      b. assign the bonuses that were hit
	//      c. assign any secondary words that are created by 'word'
	private int score(boolean commit, Word word) {
		// accumulator
		int score = 0;
		
		// if the bingo threshold is hit with pending letters, they get the bingo bonus
		int pendingCount = 0; 

		// buffers
		ArrayList<Tile> rowBuffer = new ArrayList<Tile>();
		ArrayList<Tile> colBuffer = new ArrayList<Tile>();
		
		for (int i = 0; i < tiles.length; i++) {
			for (int j = 0; j < tiles.length; j++) {
				// buffer up row i
				if (tiles[i][j].letter == Tile.Empty) {
					score += scoreWord(rowBuffer, word, "row");
					rowBuffer.clear();
				}
				else {
					rowBuffer.add(tiles[i][j]);
				}
				
				// buffer up column i
				if (tiles[j][i].letter == Tile.Empty){
					score += scoreWord(colBuffer, word, "col");
					colBuffer.clear();
				}
				else {
					colBuffer.add(tiles[j][i]);
				}
				
				// count pending
				if (tiles[i][j].pending) pendingCount++;
				
				// update state?
				if (commit) {
					// "pending" will have to wait until the end of this function, since
					// scoreWord will use it
					tiles[i][j].fresh = tiles[i][j].pending ? true : false;
				}
			}
		}
		
		score += scoreWord(rowBuffer, word, "row");
		score += scoreWord(colBuffer, word, "col");
		
		if (pendingCount == bingoCount) {
			if (noisy) System.out.println("Bingo!");
			score += bingoBonus;
		}
		
		if (commit) { 
			turn++;
			clearPending();
		}

		if (word != null) word.score = score;
		return score;
	}

	// 0-based indexes on 'row' and 'col'
	// This method does the scoring work, it operates on a buffer of Tile objects
	private int scoreWord(ArrayList<Tile> tiles, Word w, String orientation) {
		if (tiles.size() <= 1) return 0;
		
		int wordBonus = 1;
		boolean counts = false;
		Word thisWord = new Word("", tiles.get(0).row, tiles.get(0).col, orientation);
		
		//System.err.printf("scoreWord: %d,%d %s %s\n", tiles.get(0).row, tiles.get(0).col, tiles, orientation);
		
		for (Tile t : tiles) {
			if (t.pending) counts = true;

			wordBonus *= t.wordBonus;

			thisWord.addBonuses(t.wordBonus, t.letterBonus);
			
			thisWord.score += letterValues[t.letter - 'a'] * t.letterBonus;

			thisWord.word += t.letter;

			if (w != null) w.addBonuses(t.wordBonus, t.letterBonus);
		}

		thisWord.score *= wordBonus;
		
		if (counts && w != null && !thisWord.equals(w.word)) {
			w.attached.add(thisWord);
		}
		
		//if (counts) System.err.printf("scoreWord: %d,%d %s %s %s\n", row, col, thisWord.word, orientation, ( counts ? "true" : "false"));
		return counts ? thisWord.score : 0;
	}
	
	// WARNING: startRow and startCol are 1-based indexes not 0-based!!!
	// checks that the given word is a valid play at startRow,startCol in the direction dir
	// - geometry (fits on the board)
	// - touches existing played tile
	// - all words are found in the dictionary
	private boolean fits(int startRow, int startCol, String word, String dir) {
		//System.err.printf("fits: %d,%d %s %s\n", startRow, startCol, dir, word);
		// break the word into its characters
		ArrayList<Character> letters = toArray(word);
		
		// as we're placing letters, we'll also check that we touch an existing letter
		boolean touchesExisting = false;
		if (turn == 0) {
			// the very first word is always counted as touching
			// TODO: this allows the first word to be played anywhere, ie it's not guaranteed 
			// to be the center tile
			touchesExisting = true;
		}

		for (int i = 0; i < word.length(); i++) {
			int row = startRow - 1 + (dir.equals("col") ? i : 0);
			int col = startCol - 1 + (dir.equals("row") ? i : 0);

			if (row < 0 || row >= tiles.length) {
				if (noisy) System.out.println("row is out of bounds: " + (row + 1));
				break;
			}
			if (col < 0 || col >= tiles.length) {
				if (noisy) System.out.println("column is out of bounds: " + (col + 1));
				break;
			}
			
			//System.err.printf("checking %d,%d\n", row+1, col+1);
			
			if (tiles[row][col].letter == Tile.Empty) {
				tiles[row][col].letter = letters.remove(0);
				tiles[row][col].pending = true;
			}
			else if (tiles[row][col].letter == letters.get(0)) {
				letters.remove(0);
			}
			else {
				if (noisy) System.out.printf("couldn't play on (%d,%d), contains %c\n", row+1, col+1, tiles[row][col].letter);
				break;
			}
			
			if (!touchesExisting) {
				touchesExisting = validTile(row, col);
			}
		}

		if (!letters.isEmpty()) {
			if (noisy) System.out.println("couldn't use all letters: " + letters.toString());
			return false;
		}
		
		if (!touchesExisting) {
			if (noisy) System.out.println("word doesn't touch an existing letter");
			return false;
		}
		
		if (!checkWords()) {
			return false;
		}
		
		// at this point, the word is a valid play
		//System.err.println("true");	
		return true;
	}
	
	// 0-based indexes
	// checks to see if the given tile has a neighboring tile that is played
	private boolean validTile(int row, int col) {
		if (row - 1 >= 0 && tiles[row-1][col].letter != Tile.Empty && tiles[row-1][col].pending == false) {
			return true;
		}
		if (row + 1 < tiles.length && tiles[row+1][col].letter != Tile.Empty && tiles[row+1][col].pending == false) {
			return true;
		}
		if (col - 1 >= 0 && tiles[row][col-1].letter != Tile.Empty && tiles[row][col-1].pending == false) {
			return true;
		}
		if (col + 1 < tiles.length && tiles[row][col+1].letter != Tile.Empty && tiles[row][col+1].pending == false) {
			return true;
		}

		//System.err.printf("validTile: %d,%d: false\n", row, col);
		return false;
	}
	
	// set the "pending" flag on all tiles to false
	private void clearPending() {
		for (int i = 0; i < tiles.length; i++) {
			for (int j = 0; j < tiles.length; j++) {
				if (tiles[i][j].pending && !tiles[i][j].fresh) {
					tiles[i][j].letter = Tile.Empty;
				}
				tiles[i][j].pending = false;
			}
		}
	}
	
	// finds all words that are based on pending tiles, and checks that they exist
	// TODO: checkWord gets called a lot of repeated times, room for optimization
	private boolean checkWords() {
		for (int i = 0; i < tiles.length; i++) {
			for (int j = 0; j < tiles.length; j++) {
				if (tiles[i][j].pending){
					// keep going left until we run out of letters, and check that word
					int col = j;
					while (col >= 0 && tiles[i][col].letter != Tile.Empty) {
						col--;
					}
					if (!checkWord(i, col+1, "row")) {
						return false;
					}
					
					// keep going up until we run out of letters, and check that word
					int row = i;
					while (row >= 0 && tiles[row][j].letter != Tile.Empty) {
						row--;
					}
					if (!checkWord(row+1, j, "col")) {
						return false;
					}
				}
			}
		}
		
		return true;
	}

	// given a tile (assumed to be the FIRST tile of a word), this function finds the
	// connected tiles, and checks that the resulting word is in the dictionary
	private boolean checkWord(int row, int col, String dir) {
		ArrayList<Character> letters = new ArrayList<Character>();
		StringBuilder builder = new StringBuilder();
		
		//System.err.printf("checkWord: %d %d %s\n", row, col, dir);
		
		while (true) {
			if (row < tiles.length && col < tiles.length && tiles[row][col].letter != Tile.Empty) {
				letters.add(tiles[row][col].letter);
				builder.append(tiles[row][col].letter);
			}
			else {
				break;
			}
			
			if (dir.equals("row")) {
				col++;
			}
			else if (dir.equals("col")) {
				row++;
			}
			else {
				if (noisy) System.out.println("bad dir: " + dir);
				return false;
			}
		}

		// builder has the original word
		String word = builder.toString();
		if (word.length() <= 1) {
			return true;
		}
		
		//System.out.println("checkWord: " + word);
		
		// now sort the letters in the word, and assemble the key to lookup in the dictionary
		builder = new StringBuilder();
		Collections.sort(letters);
		for (char c : letters) {
			builder.append(c);
		}
		String key = builder.toString();
		
		if (dictionary.containsKey(key)) {
			ArrayList<String> words = dictionary.get(key);
			if (words.contains(word)) {
				return true;
			}
		}
		
		if (noisy) System.out.printf("'%s' is not a valid word\n", word);
		return false;
		
	}
	
	// finds the best place to play a given word, and prints the top <limit> 
	// candidates to System.out
	public void findBest(String letters, int limit) {
		// 1. find all places that are valid (neighboring tile is played)
		// 2. take the row (or column)
		// 3. add existing letters to the set of available letters
		// 4. get a list of all valid words
		// 5. pick words that fit in the row/column, given the existing letters
		// 6. score remaining words
		// 7. sort by score and print

		// downcase the incoming letters
		letters = letters.toLowerCase();
		
		// the hash of Word objects that we will build, the hash will
		// cut out duplicates
		HashSet<Word> wordsHash = new HashSet<Word>();
		
		for (int i = 0; i < tiles.length; i++) {
			// check row i
			for (Word w : searchFile(i,0,"row", letters)) {
				wordsHash.add(w);
			}
			// check col i
			for (Word w : searchFile(0,i,"col", letters)) {
				wordsHash.add(w);
			}
		}

		// convert the hash to a list so we can sort
		ArrayList<Word> words = new ArrayList<Word>();
		words.addAll(wordsHash);
		Collections.sort(words);
		
		// and print what we found!
		for (Word w : words) {
			System.out.println(w);
			if (limit > 0) {
				limit--;
				if (limit == 0) break;
			}
		}
	}

	// given a row OR a column, search and score all valid words
	private ArrayList<Word> searchFile(int row, int col, String dir, String letters) {
		//System.err.printf("searchFile: %d,%d\n", row, col);
		ArrayList<Word> words = new ArrayList<Word>();
		ArrayList<Character> rowLetters = new ArrayList<Character>();
		ArrayList<Character> colLetters = new ArrayList<Character>();
		boolean checkRow = false;
		boolean checkCol = false;
		
		for (int i = 0; i < tiles.length; i++) {
			if (dir.equals("row")) {
				rowLetters.add(tiles[row][i].letter);
				if (validTile(row, i)) checkRow = true;
			}
			else if (dir.equals("col")) {
				colLetters.add(tiles[i][col].letter);
				if (validTile(i, col)) checkCol = true;
			}
		}
		
		if (checkRow) {
			words.addAll(findWords(letters, rowLetters, "row", row));
		}
		
		if (checkCol) {
			words.addAll(findWords(letters, colLetters, "col", col));
		}
		
		return words;
	}
	
	// given a bunch of letters, fit them into a series of buckets (with and/or
	// without existing letters)
	private ArrayList<Word> findWords(String letters, ArrayList<Character> buckets, String dir, int index) {
		//System.err.printf("findWords: %s %d\n", dir, index);
		ArrayList<Word> words = new ArrayList<Word>();

		ArrayList<Character> lettersArray = toArray(letters);

		for (char c : buckets) {
			if (c != Tile.Empty) {
				lettersArray.add(c);
			}
		}
		
		//System.err.printf("findWords: %s in %s\n", lettersArray.toString(), buckets.toString());
		
		for (String word : allKnownWords(lettersArray)) {
			for (int i = 0; i < tiles.length; i++) {
				if (linesUp(word, letters, buckets, i)) {
					int row = dir.equals("row") ? index : i;
					int col = dir.equals("row") ? i : index;
					if (fits(row+1, col+1, word, dir)) {
						Word w = new Word(word, row, col, dir);
						if (score(false, w) > 0) {
							w.where = String.format("%s %d,%d", dir, row+1, col+1);
							words.add(w);
						}
					}
					clearPending();
				}
			}
		}
		
		return words;
	}
	
	// given a word and a location, checks that the letters in 'word' 
	// match up with the letters already on the board
	private boolean linesUp(String word, String available, ArrayList<Character> buckets, int i) {
		//System.err.printf("linesUp: %s %s %s[%d]\n", word, available, buckets, i);
		ArrayList<Character> letters = toArray(word);
		ArrayList<Character> avail = toArray(available);
		
		for (Character c : letters) {
			if (i >= tiles.length) break;
			
			if (buckets.get(i) == Tile.Empty) {
				if (avail.remove(c)) {
					// good
					i++;
				}
				else {
					return false;
				}
			}
			else if (c == buckets.get(i)) {
				i++;
			}
			else {
				break;
			}
		}					
		
		//System.err.println(avail.size() == 0 ? "true" : "false");
		return avail.size() == 0;
	}
	
	// given a bunch of letters, return all the words that can be created with them
	// 1. our dictionary has keys of sorted strings
	// 2. for each of those keys, check if the letters in the key are in 'letters'
	private ArrayList<String> allKnownWords(ArrayList<Character> letters) {
		//System.err.printf("allKnownWords: %s\n", letters.toString());
		ArrayList<String> words = new ArrayList<String>();
		
		for (String key : dictionary.keySet()) {
			ArrayList<Character> set = (ArrayList<Character>) letters.clone();
			boolean hit = true;
			for (char c : key.toCharArray()) {
				int i = set.indexOf(c);
				if (i >= 0) {
					set.remove(i);
				}
				else {
					hit = false;
					break;
				}
			}
			
			if (hit) {
				words.addAll(dictionary.get(key));
			}
		}

		//System.err.println("allKnownWords: " + words);
		return words;
	}
	
	// wow there's no built-in method for converting char[] to ArrayList<Character>? fail
	private ArrayList<Character> toArray(String s) {
		ArrayList<Character> a = new ArrayList<Character>(s.length());
		for (char c : s.toCharArray()) a.add(c);
		return a;
	}

	// load the board with the state in the file "fileName", it should be equivalent
	// to the output of Board::print("letters")
	public boolean load(String fileName) {
		FileInputStream fis = null;
		boolean success = false;
		
		try {
			fis = new FileInputStream(fileName);
			Reader r = new InputStreamReader(fis, "UTF-8");
			
			int intch;
			int row = 0;
			int col = 0;
			
			while ((intch = r.read()) != -1) {
				Character ch = (char) intch;
				System.err.printf("%c", ch);
				
				if (col >= tiles.length) {
					row++;
					col = 0;
				}
				
				if (ch >= 'a' && ch <= 'z') {
					tiles[row][col].letter = ch;
					col++;
				}
				else if (ch >= 'A' && ch <= 'Z') {
					tiles[row][col].letter = Character.toLowerCase(ch);
					tiles[row][col].fresh = true;
					col++;
				}
				else if (ch == '*' || ch == Tile.Empty) {
					tiles[row][col].letter = Tile.Empty;
					col++;
				}
				else if (ch == '\n') {
					if (col != tiles.length - 1) {
						//System.err.printf("unexpected newline in input file at coord %d,%d\n", row, col);
					}
				}
				else {
					// just skip everything else
				}
			}
			
			if (row < tiles.length) {
				System.err.printf("didn't get enough tiles! only got to %d,%d", row, col);
			}
			else {
				clearPending();
				success = true;
			}
			
			fis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return success;
	}
}
