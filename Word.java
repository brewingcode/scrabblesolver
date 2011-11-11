package com.alex.scrabblesolver;

import java.util.ArrayList;

public class Word implements Comparable<Word> {
	public String word;
	public int score;
	public String where;
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
	
	public String toString() {
		String me = String.format("%03d %s %s", score, where, word.toUpperCase());
		if (bonuses.size() > 0) {
			me += bonuses.toString();
		}
		return me;
	}
}
