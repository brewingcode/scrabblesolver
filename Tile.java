package com.alex.scrabblesolver;

import java.io.Serializable;

public class Tile implements Serializable {
	
	public static char Empty = '-';
	
	public Tile(int row, int col) {
		this.row = row;
		this.col = col;
		dir = 0;
		letter = Empty;
		letterBonus = 1;
		wordBonus = 1;
		fresh = false;
		pending = false;
	}
	
	// "row" and "col" are 0-index
	// "dir" is direction if this tile is the start of a word: 0:none, 1:up,
	// 2:right, 3:down, 4:left
	public int row, col, dir;
	
	// the letter that is currently on the tile
	public char letter;
	
	// multipliers
	public int letterBonus, wordBonus;
	
	// used for attempting word placement
	public boolean pending;
	
	// used for undoing 
	public boolean fresh;
}
