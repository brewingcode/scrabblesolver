package com.alex.scrabblesolver;

import java.util.ArrayList;

public class Word implements Comparable<Word> {
	public String word;
	public int score;
	public String where;
	public ArrayList<String> bonuses;
	
	public Word() {
		bonuses = new ArrayList<String>();
	}
	
	// Comparable
	public int compareTo(Word o) {
		if (this.score > o.score) {
			return -1;
		}
		else if (this.score < o.score) {
			return 1;
		}
		else {
			return 0;
		}
	}
	
	public String toString() {
		String me = String.format("%3d  %s  %s", score, where, word.toUpperCase());
		if (bonuses.size() > 0) {
			me += "  " + bonuses.toString();
		}
		return me;
	}
	
	public void addBonuses(int wordBonus, int letterBonus) {
		if (wordBonus == 2) bonuses.add("DW");
		if (wordBonus == 3) bonuses.add("TW");
		if (letterBonus == 2) bonuses.add("DL");
		if (letterBonus == 3) bonuses.add("TL");
	}
}
