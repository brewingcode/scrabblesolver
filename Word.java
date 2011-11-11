package com.alex.scrabblesolver;

import java.util.ArrayList;

public class Word implements Comparable<Word> {
	public String word;
	public int score;
	public Tile first;
	public String orientation;
	public ArrayList<String> bonuses;
	
	// Comparable
	public int compareTo(Word o) {
		if (this.score < o.score) {
			return -1;
		}
		else if (this.score > o.score) {
			return 1;
		}
		else {
			return 0;
		}
	}
}
