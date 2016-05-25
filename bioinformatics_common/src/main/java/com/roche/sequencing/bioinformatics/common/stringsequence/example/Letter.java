package com.roche.sequencing.bioinformatics.common.stringsequence.example;

import java.util.ArrayList;
import java.util.List;

import com.roche.sequencing.bioinformatics.common.stringsequence.ILetter;
import com.roche.sequencing.bioinformatics.common.stringsequence.WordMergerUtil;

public class Letter implements ILetter {

	private final Character letter;
	private final int score;

	public Letter(Character letter, int score) {
		super();
		this.letter = letter;
		this.score = score;
	}

	@Override
	public int getScore() {
		return score;
	}

	@Override
	public String toString() {
		return "" + letter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((letter == null) ? 0 : letter.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Letter other = (Letter) obj;
		if (letter == null) {
			if (other.letter != null)
				return false;
		} else if (!letter.equals(other.letter))
			return false;
		return true;
	}

	public static void main(String[] args) {
		example2();
	}

	public static void example2() {
		ILetter a = new Letter('a', 1);
		ILetter b = new Letter('b', 1);

		List<ILetter[]> words = new ArrayList<ILetter[]>();
		words.add(new ILetter[] { a, a, b });
		words.add(new ILetter[] { a, b, a });
		words.add(new ILetter[] { b, a, b });
		words.add(new ILetter[] { b, b, a });
		words.add(new ILetter[] { a, b, b });
		words.add(new ILetter[] { b, a, a });
		words.add(new ILetter[] { a, a, a });
		words.add(new ILetter[] { b, b, b });

		ILetter[] result = WordMergerUtil.merge(words);

		System.out.println("final:");
		for (ILetter letter : result) {
			System.out.print(letter);
		}
	}

	public static void example1() {
		ILetter a = new Letter('a', 5);
		ILetter b = new Letter('b', 15);
		ILetter c = new Letter('c', 25);
		ILetter d = new Letter('d', 65);
		ILetter e = new Letter('e', 5);
		ILetter f = new Letter('f', 15);
		ILetter g = new Letter('g', 25);
		ILetter h = new Letter('h', 65);
		ILetter i = new Letter('i', 5);
		ILetter j = new Letter('j', 15);
		ILetter k = new Letter('k', 25);
		ILetter l = new Letter('l', 65);

		List<ILetter[]> words = new ArrayList<ILetter[]>();
		words.add(new ILetter[] { d, a, b, c, d, e, j });
		words.add(new ILetter[] { a, b, c, d, f, l });
		words.add(new ILetter[] { a, b, c, d, c, g, l });
		words.add(new ILetter[] { a, b, c, d, c, d, h, k });
		words.add(new ILetter[] { a, b, c, d, c, d, i });

		ILetter[] result = WordMergerUtil.merge(words);

		System.out.println("final:");
		for (ILetter letter : result) {
			System.out.print(letter);
		}

	}

	@Override
	public boolean matches(ILetter rhs) {
		return this.equals(rhs);
	}

}
