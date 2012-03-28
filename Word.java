import java.util.ArrayList;

public class Word implements Comparable<Word> {
	public String word;
	public int score;
	public String where;
	
	private ArrayList<String> bonuses;
	private ArrayList<Word> attached;
	private ArrayList<Integer> blankLetters;
	
	// 
	public Word(String w) {
		word = w;
		where = "";
	}
	
	// 0-based indexes for 'row' and 'col'
	public Word(String w, int row, int col, String orientation) {
		word = w;
		where = "";
		bonuses = new ArrayList<String>();
		attached = new ArrayList<Word>();
		setLocation(row, col, orientation);
	}
	
	// 0-indexes
	public void setLocation(int row, int col, String orientation) {
		where = String.format("%s %d,%d", orientation, row+1, col+1);
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
		String me = String.format("%3d  %s  ", score, where);
		for (int i = 0; i < word.length(); i++) {
			me += word.charAt(i);
			if (blankLetters().contains(i)) {
				me += "*";
			}
			
		}
		if (bonuses != null && bonuses.size() > 0) {
			me += "  " + bonuses.toString();
		}
		return me;
	}

	public int hashCode() {
		return toString().hashCode();
	}
	
	public boolean equals(Object w) {
		return toString().equals(w.toString());
	}
	
	public void addBonuses(int wordBonus, int letterBonus) {
		if (bonuses == null) {
			bonuses = new ArrayList<String>();
		}
		
		if (wordBonus == 2) bonuses.add("DW");
		if (wordBonus == 3) bonuses.add("TW");
		if (letterBonus == 2) bonuses.add("DL");
		if (letterBonus == 3) bonuses.add("TL");
	}
	
	public void attach(Word w) {
		if (attached == null) {
			attached = new ArrayList<Word>();
		}
		attached.add(w);
	}
	
	public ArrayList<Integer> blankLetters() {
		if (blankLetters == null) blankLetters = new ArrayList<Integer>();
		return blankLetters;
	}
	
	@SuppressWarnings("unchecked")
	public Word clone() {
		Word w = new Word(word);
		w.score = score;
		if (bonuses != null ) w.bonuses = (ArrayList<String>) bonuses.clone();
		if (attached != null) w.attached = (ArrayList<Word>) attached.clone();
		if (blankLetters != null) w.blankLetters = (ArrayList<Integer>) blankLetters.clone();
		w.where = where;
		
		return w;
	}
	
	public void bingo() {
		if (bonuses == null) bonuses = new ArrayList<String>();
		bonuses.add("B");
	}
}
