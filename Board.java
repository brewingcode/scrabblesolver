package com.alex.scrabblesolver;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class Board implements Serializable {
	private final static long serialVersionUID = -7508045570854076475L;
	
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
	
	// be noisy on stdout
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
					Tile t = tiles[row][col];
					if (t.letter == Tile.Empty) {
						if (t.letterBonus > 1 || t.wordBonus > 1) {
							b += "# ";
						}
						else {
							b += Tile.Empty + " ";
						}
					}
					else {
						b += t.fresh ? Character.toUpperCase(t.letter) : t.letter;
						b += t.blank ? "*" : " ";
					}
					
					if (col == size - 1) {
						b += "\n";
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
		ArrayList<Character> letters = toArray(word);
		Word w = new Word(word.replaceAll("\\*", "").toLowerCase());
		
		for (int i = 0; i < word.length(); i++) {
			if (letters.get(i) ==  '*') {
				w.blankLetters().add(i-1);
			}
		}

		if (fits(row, col, w, dir)) {
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
			rowBuffer.clear();
			colBuffer.clear();
			for (int j = 0; j < tiles.length; j++) {
				// buffer up row i
				if (tiles[i][j].letter == Tile.Empty) {
					score += scoreWord(rowBuffer, word, "row");
					//System.err.printf("hit empty on row: %d\n", score);
					rowBuffer.clear();
				}
				else {
					rowBuffer.add(tiles[i][j]);
				}
				
				// buffer up column i
				if (tiles[j][i].letter == Tile.Empty){
					score += scoreWord(colBuffer, word, "col");
					//System.err.printf("hit empty on col: %d\n", score);
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
			
			score += scoreWord(rowBuffer, word, "row");
			//System.err.printf("last rowBuffer: %d\n", score);
			score += scoreWord(colBuffer, word, "col");
			//System.err.printf("last colBuffer: %d\n", score);
		}
		
		if (pendingCount == bingoCount) {
			if (noisy) System.out.println("Bingo!");
			score += bingoBonus;
			if (word != null) word.bingo();
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
			thisWord.word += t.letter;
			//System.err.printf("scoreWord: tile %d,%d %c %b %b\n", t.row, t.col, t.letter, t.pending, t.blank);
			if (t.pending)  {
				// this word counts...
				counts = true;

				if (t.blank) {
					// ...but this tile doesn't score anything
				}
				else {
					// ...and this word scores points!
					//System.err.printf("scoring letter: %c\n", t.letter);
					wordBonus *= t.wordBonus;
					thisWord.addBonuses(t.wordBonus, t.letterBonus);
					if (w != null) w.addBonuses(t.wordBonus, t.letterBonus);
					thisWord.score += letterValues[t.letter - 'a'] * t.letterBonus;
				}
			}
			else {
				// if the tile isn't pending, it's just worth the value of the letter, 
				// UNLESS it's blank, in which case it is worth jack
				//System.err.printf("non-scoring letter: %c\n", t.letter);
				if (!t.blank) thisWord.score += letterValues[t.letter - 'a'];
			}
		}

		if (counts)	thisWord.score *= wordBonus;

		if (counts && w != null && !thisWord.word.equals(w.word)) {
			w.attach(thisWord);
		}
		
		//if (counts) System.err.printf("scoreWord: %s %s\n", thisWord, ( counts ? "true" : "false"));
		return counts ? thisWord.score : 0;
	}
	
	// WARNING: startRow and startCol are 1-based indexes not 0-based!!!
	// checks that the given word is a valid play at startRow,startCol in the direction dir
	// - geometry (fits on the board)
	// - touches existing played tile
	// - all words are found in the dictionary
	// SIDE EFFECT: sets pending on the word that is played, in prep for calling score()
	private boolean fits(int startRow, int startCol, Word word, String dir) {
		//System.err.printf("fits: %d,%d %s %s\n", startRow, startCol, dir, word.word);
		// break the word into its characters
		ArrayList<Character> letters = toArray(word.word);

		// as we're placing letters, we'll also check that we touch an existing letter
		boolean touchesExisting = false;
		int end = letters.size();
		for (int i = 0; i < end; i++) {
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
				if (noisy) System.out.printf("fits: %c on empty\n", tiles[row][col].letter);

				// check if we just placed a blank tile
				if (word.blankLetters().contains(i)) {
					tiles[row][col].blank = true;
				}
			}
			else if (tiles[row][col].letter == letters.get(0)) {
				if (noisy) System.out.printf("fits: %c exists\n", tiles[row][col].letter);
				letters.remove(0);
			}
			else {
				if (noisy) System.out.printf("fits: couldn't play on (%d,%d), contains %c\n", row+1, col+1, tiles[row][col].letter);
				break;
			}
			
			if (!touchesExisting) {
				touchesExisting = validTile(row, col);
			}
		}

		if (!letters.isEmpty()) {
			if (noisy) System.out.println("fits: couldn't use all letters: " + letters.toString());
			return false;
		}
		
		if (!touchesExisting) {
			if (noisy) System.out.println("fits: word doesn't touch an existing letter");
			return false;
		}
		
		if (!checkWords()) {
			if (noisy) System.out.println("fits: checkWords didn't pass");
			return false;
		}
		
		// at this point, the word is a valid play
		//System.err.println("fits: true");
		return true;
	}
	
	// 0-based indexes
	// checks to see if the given tile has a neighboring tile that is played
	private boolean validTile(int row, int col) {
		if (turn == 0) {
			// there is only 1 valid tile to play on turn 0, that is the center
			int center = tiles.length / 2;
			return row == center && col == center;
		}
		else {
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
	
	// set the "blank" flag on pending all tiles to false
	private void clearBlank() {
		for (int i = 0; i < tiles.length; i++) {
			for (int j = 0; j < tiles.length; j++) {
				if (tiles[i][j].pending) {
					tiles[i][j].blank = false;
				}
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
				if (noisy) System.out.printf("checkWord: true %s\n", word);
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
		for (char c : letters.toCharArray()) {
			if (c >= 'a' && c <= 'z') {
				// lower case letters are valid
			}
			else if (c == Tile.Blank) {
				// blank tile 
			}
			else {
				System.err.printf("invalid character in tile set: %c\n", c);
				return;
			}
		}

		ArrayList<Character> lettersArray = toArray(letters);
		
		// the hash of Word objects that we will build, the hash will
		// cut out duplicates
		HashSet<Word> wordsHash = new HashSet<Word>();
		int c = 0;
		
		for (int i = 0; i < tiles.length; i++) {
			// check row i
			for (Word w : searchFile(i,0,"row", lettersArray)) {
				wordsHash.add(w);
				c++;
			}
			// check col i
			for (Word w : searchFile(0,i,"col", lettersArray)) {
				wordsHash.add(w);
				c++;
			}
		}

		// convert the hash to a list so we can sort
		ArrayList<Word> words = new ArrayList<Word>();
		words.addAll(wordsHash);
		Collections.sort(words);
		
		if (noisy) System.out.printf("wordsHash: %d, words: %d\n", c, words.size());
		
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
	private ArrayList<Word> searchFile(int row, int col, String dir, ArrayList<Character> letters) {
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
	private ArrayList<Word> findWords(ArrayList<Character> letters, ArrayList<Character> buckets, String dir, int index) {
		//System.err.printf("findWords: %s %d\n", dir, index);
		ArrayList<Word> words = new ArrayList<Word>();

		//System.err.printf("findWords: %s in %s\n", lettersArray.toString(), buckets.toString());
		
		for (Word possible : allKnownWords(letters, buckets)) {
			for (int i = 0; i < tiles.length; i++) {
				//System.err.printf("findWords: %d %s\n", i, possible);
				if (linesUp(possible, letters, buckets, i)) {
					int row = dir.equals("row") ? index : i;
					int col = dir.equals("row") ? i : index;
					if (fits(row+1, col+1, possible, dir)) {
						//System.err.printf("findWords: true %s %d,%d\n", possible, row, col);
						Word valid = possible.clone();
						valid.setLocation(row, col, dir);
						if (score(false, valid) > 0) {
							words.add(valid);
						}
					}
					else {
						//System.err.printf("findWords: false %s %d,%d\n", possible, row, col);
					}
					clearBlank();
					clearPending();
				}
			}
		}
		
		return words;
	}
	
	// given a word and a location, checks that the letters in 'word' 
	// match up with the letters already on the board
	private boolean linesUp(Word word, ArrayList<Character> available, ArrayList<Character> buckets, int start) {
		boolean linesUp = true;
		@SuppressWarnings("unchecked")
		ArrayList<Character> avail = (ArrayList<Character>) available.clone();
		
		for (int i = 0; i < word.word.length(); i++) {
			if (start + i >= buckets.size()) {
				linesUp = false;
				break;
			}
			
			char target = word.word.charAt(i);
			
			if (buckets.get(start + i) == Tile.Empty) {
				// nothing on the board
				
				if (avail.remove((Character)target)) {
					// we had the character
				}
				else {
					// the target wasn't in available...but what about a blank tile?
					if (word.blankLetters().contains(i)) {
						// ok we're still good
					}
					else {
						// ok now we're hosed
						linesUp = false;
						break;
					}
				}
			}
			else {
				if (buckets.get(start + i) == target) {
					// the letter is on the board already
				}
				else {
					// the wrong letter is on the board, no joy
					linesUp = false;
					break;
				}
			}
		}
		
		//System.err.printf("linesUp: %s %s %s %s[%d] %d %s\n", (linesUp ? "true" : "false"), word.word, available, buckets, start, i, avail);
		return linesUp;
	}
	
	// given a bunch of letters, return all the words that can be created with them
	// 1. our dictionary has keys of sorted strings
	// 2. for each of those keys, check if the letters in the key are in 'letters'
	public ArrayList<Word> allKnownWords(ArrayList<Character> letters, ArrayList<Character> buckets) {
		//System.err.printf("allKnownWords: %s\n", letters.toString());
		ArrayList<Word> words = new ArrayList<Word>();
		if (buckets == null) buckets = new ArrayList<Character>();
		
		if (letters.remove((Character)Tile.Blank)) {
			// now re-add a real tile for each letter of the alphabet

			for (char c = 'a'; c <= 'z'; c++) {
				letters.add(c);
				for (Word w : allKnownWords(letters, buckets)) {
					if (w.word.indexOf(c) >= 0) {
						// we expand w into all the valid words it could be
						for (int i = 0; i < w.word.length(); i++) {
							if (w.word.charAt(i) == c) {
								Word x = w.clone();
								x.blankLetters().add(i);
								//System.err.printf("allKnownWords: %s with blank %c at %d\n", x.word, c, i);
								words.add(x);
							}
						}
					}
					else {
						//System.err.printf("allKnownWords: %s\n", w.word);
						words.add(w);
					}
				}
				letters.remove(letters.indexOf(c));
			}
			
			// ...and put the blank back
			letters.add(Tile.Blank);
		}
		else {
			for (String key : dictionary.keySet()) {
				@SuppressWarnings("unchecked")
				ArrayList<Character> set = (ArrayList<Character>) letters.clone();
				for (char c : buckets) {
					if (c != Tile.Empty) set.add(c); 
				}
				
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
					for (String s : dictionary.get(key)) {
						words.add(new Word(s));
					}
				}
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
				//System.err.printf("%c", ch);
				
				if (col >= tiles.length) {
					row++;
					col = 0;
				}
				
				// process different chars
				if (ch >= 'a' && ch <= 'z') {
					if (row == tiles.length) System.err.println("too many characters");
					tiles[row][col].letter = ch;
					if (noisy) System.out.printf("%d,%d: %c\n", row+1, col+1, tiles[row][col].letter);
					col++;
				}
				else if (ch >= 'A' && ch <= 'Z') {
					if (row == tiles.length) System.err.println("too many characters");
					tiles[row][col].letter = Character.toLowerCase(ch);
					tiles[row][col].fresh = true;
					if (noisy) System.out.printf("%d,%d: %c (just played)\n", row+1, col+1, tiles[row][col].letter);
					col++;
				}
				else if (ch == '#' || ch == Tile.Empty) {
					if (row == tiles.length) System.err.println("too many characters");
					tiles[row][col].letter = Tile.Empty;
					if (noisy) System.out.printf("%d,%d: %c\n", row+1, col+1, tiles[row][col].letter);
					col++;
				}
				else if (ch == '*') {
					int prevRow = row;
					int prevCol = col - 1;
					
					if (prevCol < 0) {
						prevCol = tiles.length - 1;
						prevRow--;
					}
					
					tiles[prevRow][prevCol].blank = true;
					if (noisy) System.out.printf("%d,%d: %c* (blank)\n", prevRow+1, prevCol+1, tiles[prevRow][prevCol].letter);
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
				turn++;
				success = true;
			}
			
			fis.close();
		} catch (Exception ex) {
			System.err.println("\nerror during load");
			ex.printStackTrace();
		}
		
		return success;
	}
	
	// 1-based indexes
	public void toggleBlank(int row, int col) {
		if (tiles[row-1][col-1].letter != Tile.Empty) {
			tiles[row-1][col-1].blank = !tiles[row-1][col-1].blank;
		}
	}
	
	public int undo() {
		int undone = 0;
		
		for (int i = 0; i < tiles.length; i++) {
			for (int j = 0; j < tiles.length; j++) {
				Tile t = tiles[i][j];
				
				if (t.fresh) {
					t.letter = Tile.Empty;
					t.fresh = false;
					t.blank = false;
					
					undone++;
				}
			}
		}

		return undone;
	}
}
	
	
