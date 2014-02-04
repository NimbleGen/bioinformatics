package com.roche.sequencing.bioinformatics.common.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

public class StringUtilTest {

	@Test(groups = { "unit" })
	public void testReverse() {
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String reverseAlphabet = "ZYXWVUTSRQPONMLKJIHGFEDCBA";
		Assert.assertEquals(StringUtil.reverse(alphabet), reverseAlphabet);
	}

	@Test(groups = { "unit" })
	public void testSpaceAsString() {
		String fourSpaces = "    ";
		Assert.assertEquals(StringUtil.getSpacesAsString(4), fourSpaces);
	}

	@Test(groups = { "unit" })
	public void testRepeat() {
		String fourRepeats = "ABCABCABCABC";
		Assert.assertEquals(StringUtil.repeatString("ABC", 4), fourRepeats);
	}

	@Test(groups = { "unit" })
	public void testPadLeft() {
		String word = "ABC";
		String leftPaddedWord = "       ABC";
		Assert.assertEquals(StringUtil.padLeft(word, 10), leftPaddedWord);
	}

	@Test(groups = { "unit" })
	public void testEqualsOne() {
		String wordOne = "ABC";
		String wordTwo = "ABC";
		String wordThree = "ABC";
		String wordFour = "ABC";
		Assert.assertTrue(StringUtil.equals(wordOne, wordTwo, wordThree, wordFour));
	}

	@Test(groups = { "unit" })
	public void testEqualsTwo() {
		String wordOne = "ABC";
		String wordTwo = "ABC";
		String wordThree = "ABC";
		String wordFour = "ABCD";
		Assert.assertFalse(StringUtil.equals(wordOne, wordTwo, wordThree, wordFour));
	}

	@Test(groups = { "unit" })
	public void testNthOccurence() {
		String string = "ABCDEFGHIDEFJKLDEFMNO";
		char letter = 'D';
		Assert.assertEquals(StringUtil.nthOccurrence(string, letter, 3), 15);
	}

	@Test(groups = { "unit" })
	public void testNthOccurenceTwo() {
		String string = "ABCDEFGHIDEFJKLDEFMNO";
		char letter = 'D';
		Assert.assertEquals(StringUtil.nthOccurrence(string, letter, 15), -1);
	}

	@Test(groups = { "unit" })
	public void testNthOccurenceThree() {
		String string = "ABCDEFGHIDEFJKLDEFMNO";
		char letter = 'D';
		Assert.assertEquals(StringUtil.nthOccurrence(string, letter, -10), -1);
	}

	@Test(groups = { "unit" })
	public void testIsNumeric() {
		String number = "123456.1";
		Assert.assertTrue(StringUtil.isNumeric(number));
	}

	@Test(groups = { "unit" })
	public void testIsNumericTwo() {
		String number = "123456.1.456";
		Assert.assertFalse(StringUtil.isNumeric(number));
	}

	@Test(groups = { "unit" })
	public void testInsertStringEveryNSpaces() {
		String baseString = "abcdefghijk";
		String expectedResult = "abzcdzefzghzijzk";
		String actualResult = StringUtil.insertStringEveryNSpaces(baseString, "z", 2);
		Assert.assertEquals(actualResult, expectedResult);
	}

	@Test(groups = { "unit" })
	public void testMatch() {
		Assert.assertEquals(StringUtil.contains("ABCDEFG", "ABC", 0), true);
		Assert.assertEquals(StringUtil.contains("ABCDEFG", "ACC", 0), false);
		Assert.assertEquals(StringUtil.contains("ABCDEFG", "ACC", 1), true);
		Assert.assertEquals(StringUtil.contains("ABCDEFG", "ABCCEF", 1), true);
		Assert.assertEquals(StringUtil.contains("CBCDEFG", "ABC", 1), true);
		Assert.assertEquals(StringUtil.contains("ABCDEFG", "DFF", 0), false);
		Assert.assertEquals(StringUtil.contains("ABCDEFG", "DFF", 1), true);
	}

}
