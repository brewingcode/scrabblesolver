package com.alex.scrabblesolver;

public class Tile {
	public Tile(int row, int col) {
		this.row = row;
		this.col = col;
		dir = 0;
		letter = '-';
		letterBonus = 1;
		wordBonus = 1;
	}
	
	// "row" and "col" are 0-index
	// "dir" is direction if this tile is the start of a word: 0:none, 1:up,
	// 2:right, 3:down, 4:left
	public int row, col, dir;
	
	// letter, empty string if letter is not set yet
	public char letter;
	
	// multipliers
	public int letterBonus, wordBonus;
}
