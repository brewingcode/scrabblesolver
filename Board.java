package com.alex.scrabblesolver;


public class Board {
	// constructor
	public Board(int size) {
		tiles = new Tile[size][size];	
		for (int row = 0; row < size; row++) {
			for (int col = 0; col < size; col++) {
				tiles[row][col] = new Tile(row,col);
			}
		}
		
		wordsWithFriends();
	}

	// setup board with WWF rules
	private void wordsWithFriends() {
		letterValues = new int[] { 1, 4, 4, 2, 1, 4, 3, 3, 1, 10, 5, 2, 4, 2, 1, 4, 10, 1, 1, 1, 2, 5, 4, 8, 3, 10 };
		
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
	
	public String toString() {
		String b = "";
		String select = "bonuses";
		int size = tiles.length;
		
		if (select == "bonuses") {
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
		else if (select == "letters") {
			for (int row = 0; row < size; row++) {
				for (int col = 0; col < size; col++) {
					b += tiles[row][col].letter;
					if (col == size - 1) {
						b += "\n";
					}
					else {
						b += " ";
					}
				}
			}
		}
		
		return b;
	}
	
	
	// the tiles on the board (ie, the state of the game)
	private Tile[][] tiles;
	
	// point values of the letters (changes for different rule sets)
	private int[] letterValues;
}
