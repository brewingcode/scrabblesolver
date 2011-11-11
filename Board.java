package com.alex.scrabblesolver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


public class Board implements Serializable {
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
	
	public void print(String which) {
		System.out.print(toString(which));
	}
	
	public String toString() {
		return toString("letters");
	}
	
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
					b += tiles[row][col].fresh ? Character.toUpperCase(c) : c; 
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
			System.err.println("unknown 'which': " + which + " (try 'bonuses' or 'letters')");
		}
		
		return b;
	}

	// plays the given word at the row and col specified, returns the score for the play, or
	// 0 if the play is invalid
	// WARNING: row and col are 1-based indexes NOT 0-based!!!
	public int play(int row, int col, String word, String dir) {
		// accumulate our score here
		int score = 0;
		
		if (fits(row, col, word, dir)) {
			// to commit this turn, we need to:
			// 1. clear the old "fresh" tiles from the previous turn
			// 2. increment the "turn" counter by 1
			// 3. find all the "pending" tiles to score them AND also make them the new "fresh" tiles
			
			// if the bingo threshold is hit with pending letters, they get the bingo bonus
			int pendingCount = 0; 

			// any double/triple word bonuses
			int wordBonus = 1;
			
			for (int i = 0; i < tiles.length; i++) {
				for (int j = 0; j < tiles.length; j++) {
					Tile t = tiles[i][j];
					
					t.fresh = false;
					if (t.pending) {
						pendingCount++;
						t.fresh = true;
						t.pending = false;
						int s = t.letterBonus * letterValues[t.letter - 'a'];
						score += s;
						wordBonus *= t.wordBonus;

						System.out.print(t.letter + ": " + s);
						if (t.letterBonus > 1) {
							System.out.print(" (" + t.letterBonus + "x letter)");
						}
						if (t.wordBonus > 1) {
							System.out.print(" (" + t.wordBonus + "x word)");
						}
						System.out.print("\n");
					}
				}
			}
			
			turn++;
			
			score *= wordBonus;
			
			if (pendingCount == bingoCount) {
				System.out.println("bingo!");
				score += bingoBonus;
			}
		}
		
		return score;
	}

	// WARNING: startRow and startCol are 1-based indexes not 0-based!!!
	public boolean fits(int startRow, int startCol, String word, String dir) {
		// break the word into its characters
		ArrayList<Character> letters = new ArrayList<Character>();
		for (char c : word.toCharArray()) {
			letters.add(c);
		}
		
		// as we're placing letters, we'll also check that we touch an existing letter
		boolean touchesExisting = false;
		if (turn == 0) {
			// the very first word is always counted as touching
			// TODO: this allows the first word to be played anywhere, ie it's not guaranteed 
			// to be the center tile
			touchesExisting = true;
		}
		
		for (int i = 0; i < word.length(); i++) {
			int row = startRow - 1 + (dir.equals("down") ? i : 0);
			int col = startCol - 1 + (dir.equals("right") ? i : 0);

			if (row < 0 || row >= tiles.length) {
				System.err.println("row is out of bounds: " + (row + 1));
				break;
			}
			if (col < 0 || col >= tiles.length) {
				System.err.println("column is out of bounds: " + (col + 1));
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
				System.err.printf("couldn't play on (%d,%d), contains %c\n", row+1, col+1, tiles[row][col].letter);
				break;
			}
			
			if (!touchesExisting) {
				if (row - 1 >= 0 && tiles[row-1][col].letter != Tile.Empty && tiles[row-1][col].pending == false) {
					touchesExisting = true;
				}
				if (row + 1 < tiles.length && tiles[row+1][col].letter != Tile.Empty && tiles[row+1][col].pending == false) {
					touchesExisting = true;
				}
				if (col - 1 >= 0 && tiles[row][col-1].letter != Tile.Empty && tiles[row][col-1].pending == false) {
					touchesExisting = true;
				}
				if (col + 1 < tiles.length && tiles[row][col+1].letter != Tile.Empty && tiles[row][col+1].pending == false) {
					touchesExisting = true;
				}
			}
		}

		if (!letters.isEmpty()) {
			System.err.println("couldn't use all letters: " + letters.toString());
			clearPending();
			return false;
		}
		
		if (!touchesExisting) {
			System.err.println("word doesn't touch an existing letter");
			clearPending();
			return false;
		}
		
		if (!checkWords()) {
			System.err.println("one or more invalid words introduced");
			clearPending();
			return false;
		}
		
		// at this point, the word is a valid play
		return true;
	}
	
	private void clearPending() {
		for (int i = 0; i < tiles.length; i++) {
			for (int j = 0; j < tiles.length; j++) {
				if (tiles[i][j].pending){
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
					if (!checkWord(i, col+1, "right")) {
						return false;
					}
					
					// keep going up until we run out of letters, and check that word
					int row = i;
					while (row >= 0 && tiles[row][j].letter != Tile.Empty) {
						row--;
					}
					if (!checkWord(row+1, j, "down")) {
						return false;
					}
				}
			}
		}
		
		return true;
	}

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
			
			if (dir.equals("right")) {
				col++;
			}
			else if (dir.equals("down")) {
				row++;
			}
			else {
				System.err.println("bad dir: " + dir);
				return false;
			}
		}

		// builder has the original word
		String word = builder.toString();
		if (word.length() <= 1) {
			return true;
		}
		
		System.out.println("checkWord: " + word);
		
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
		
		return false;
		
	}
	// finds the best place to play a given word, and prints the top <limit> 
	// candidates to System.out
	public void findbest(String word, int limit) {
		
	}
	
	public transient HashMap<String, ArrayList<String>> dictionary;
	
	private int bingoCount;
	private int bingoBonus;
	
	// the tiles on the board (ie, the state of the game)
	private Tile[][] tiles;
	
	// point values of the letters (changes for different rule sets)
	private int[] letterValues;
	
	// what turn we are on
	private int turn;
}
