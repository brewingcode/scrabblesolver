package com.alex.scrabblesolver;

import java.io.Serializable;

public class Tile implements Serializable {
	
	private static final long serialVersionUID = 4556286527432162412L;
	
	public static char Empty = '-';
	public static char Blank = '_';
	
	public Tile(int row, int col) {
		this.row = row;
		this.col = col;
		letter = Empty;
		letterBonus = 1;
		wordBonus = 1;
		pending = false;
		fresh = false;
		blank = false;
	}
	
	// "row" and "col" are 0-index
	public int row, col;
	
	// the letter that is currently on the tile
	public char letter;
	
	// multipliers
	public int letterBonus, wordBonus;
	
	// used for attempting word placement
	public boolean pending;
	
	// used for undoing and indicating previous move
	public boolean fresh;
	
	// for blank letters
	public boolean blank;
	
	public String toString() {
		return String.format("[%s]", letter);
	}
}
