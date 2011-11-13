package com.alex.scrabblesolver;

import java.io.Serializable;

public class Tile implements Serializable {
	
	public static char Empty = '-';
	
	public Tile(int row, int col) {
		this.row = row;
		this.col = col;
		letter = Empty;
		letterBonus = 1;
		wordBonus = 1;
		pending = false;
		fresh = false;
	}
	
	// "row" and "col" are 0-index
	public int row, col;
	
	// the letter that is currently on the tile
	public char letter;
	
	// multipliers
	public int letterBonus, wordBonus;
	
	// used for attempting word placement
	public boolean pending;
	
	// used for undoing and indicated previous move
	public boolean fresh;
}
