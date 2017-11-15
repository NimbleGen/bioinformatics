package com.roche.sequencing.bioinformatics.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrefixCompressionDictionary {

	private final static short DEFAULT_MIN_PREFIX_LENGTH = 3;

	private List<String> prefixes;
	private short minPrefixLength;
	private int maxSerializedStringLength;

	public PrefixCompressionDictionary() {
		this(DEFAULT_MIN_PREFIX_LENGTH);
	}

	public PrefixCompressionDictionary(short minPrefixLength) {
		super();
		this.prefixes = new ArrayList<>();
		this.minPrefixLength = minPrefixLength;
	}

	public int getMaxSerializedStringLength() {
		return maxSerializedStringLength;
	}

	/**
	 * Need to train on the complete library of text to be used in order for compress to work
	 * 
	 * @param text
	 */
	public void train(String text) {
		compress(text, true);
	}

	public PrefixCompressedString compress(String textToCompress) {
		return compress(textToCompress, false);
	}

	public boolean isTextCompressable(String text) {
		boolean isTextCompressable = true;
		try {
			compress(text, false);
		} catch (NewPrefixesDisabledException e) {
			isTextCompressable = false;
		}
		return isTextCompressable;
	}

	private PrefixCompressedString compress(String textToCompress, boolean allowPrefixesToBeAdded) {
		PrefixCompressedString prefixCompressedString = null;

		short prefixIndex = 0;
		short endIndexInPrefix = 0;
		String suffix = "";
		if (prefixes.size() == 0) {
			if (!allowPrefixesToBeAdded) {
				throw new NewPrefixesDisabledException("Unable to compress [" + textToCompress + "] with prefixes[" + ListUtil.toString(prefixes) + "].");
			}

			prefixes.add(textToCompress);
			prefixIndex = 0;
			endIndexInPrefix = (short) textToCompress.length();

			prefixCompressedString = new PrefixCompressedString(prefixIndex, endIndexInPrefix, suffix);

		} else {
			short bestPrefixIndex = 0;
			short bestPrefixLength = 0;
			for (prefixIndex = 0; prefixIndex < prefixes.size(); prefixIndex++) {
				String prefix = prefixes.get(prefixIndex);
				short index = 0;
				while (index < textToCompress.length() && index < prefix.length() && textToCompress.charAt(index) == prefix.charAt(index)) {
					index++;
				}

				if (index > bestPrefixLength) {
					bestPrefixIndex = prefixIndex;
					bestPrefixLength = index;
				}
			}

			if (bestPrefixLength > Math.min(minPrefixLength, textToCompress.length() - 1)) {
				prefixCompressedString = new PrefixCompressedString(bestPrefixIndex, bestPrefixLength, textToCompress.substring(bestPrefixLength, textToCompress.length()));
			} else {
				if (!allowPrefixesToBeAdded) {
					throw new NewPrefixesDisabledException("Unable to compress [" + textToCompress + "] with prefixes[" + ListUtil.toString(prefixes) + "].");
				}
				prefixes.add(textToCompress);
				prefixCompressedString = new PrefixCompressedString((short) (prefixes.size() - 1), (short) (textToCompress.length()), "");
			}
		}

		maxSerializedStringLength = Math.max(maxSerializedStringLength, prefixCompressedString.getSerializedString().length());

		return prefixCompressedString;
	}

	private static class NewPrefixesDisabledException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public NewPrefixesDisabledException(String message) {
			super(message);
		}
	}

	public String decompress(PrefixCompressedString compressedString) {
		String prefix = prefixes.get(compressedString.indexOfPrefix);
		String text = prefix.substring(0, compressedString.endIndexInPrefix) + compressedString.suffix;
		return text;
	}

	public List<String> getPrefixes() {
		return Collections.unmodifiableList(prefixes);
	}

	public static class PrefixCompressedString {
		private final short indexOfPrefix;
		private final short endIndexInPrefix;
		private final String suffix;

		public PrefixCompressedString(short indexOfPrefix, short endIndexInPrefix, String suffix) {
			super();
			this.indexOfPrefix = indexOfPrefix;
			this.endIndexInPrefix = endIndexInPrefix;
			this.suffix = suffix;
		}

		public PrefixCompressedString(String serializedString) {
			int firstDelimiterIndex = serializedString.indexOf('_');
			int secondDelimiterIndex = serializedString.indexOf('_', firstDelimiterIndex + 1);
			if (firstDelimiterIndex < 0 || secondDelimiterIndex < 0) {
				throw new IllegalStateException("The provided serialized string[" + serializedString + "] is not of the format expected for the PrefixCompressedString[number_number_anyString]");
			}
			this.indexOfPrefix = Short.valueOf(serializedString.substring(0, firstDelimiterIndex));
			this.endIndexInPrefix = Short.valueOf(serializedString.substring(firstDelimiterIndex + 1, secondDelimiterIndex));
			this.suffix = serializedString.substring(secondDelimiterIndex + 1, serializedString.length());
		}

		public String getSerializedString() {
			return indexOfPrefix + "_" + endIndexInPrefix + "_" + suffix;
		}

		public short getIndexOfPrefix() {
			return indexOfPrefix;
		}

		public short getEndIndexInPrefix() {
			return endIndexInPrefix;
		}

		public String getSuffix() {
			return suffix;
		}

		@Override
		public String toString() {
			return suffix + " (" + indexOfPrefix + ")";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + endIndexInPrefix;
			result = prime * result + indexOfPrefix;
			result = prime * result + ((suffix == null) ? 0 : suffix.hashCode());
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
			PrefixCompressedString other = (PrefixCompressedString) obj;
			if (endIndexInPrefix != other.endIndexInPrefix)
				return false;
			if (indexOfPrefix != other.indexOfPrefix)
				return false;
			if (suffix == null) {
				if (other.suffix != null)
					return false;
			} else if (!suffix.equals(other.suffix))
				return false;
			return true;
		}
	}

	public static void main(String[] args) {
		PrefixCompressionDictionary dict = new PrefixCompressionDictionary();
		String[] words = new String[] { "pizza847589749570987457", "tree", "trees", "pie", "pizza roll", "try", "pizza tree" };
		for (String word : words) {
			dict.train(word);
		}

		System.out.println("prefixes:" + ListUtil.toString(dict.getPrefixes()));

		for (String word : words) {
			PrefixCompressedString compressedString = dict.compress(word);
			String serializedString = compressedString.getSerializedString();
			PrefixCompressedString newCompressedstring = new PrefixCompressedString(serializedString);
			if (!compressedString.equals(newCompressedstring)) {
				throw new IllegalStateException("String serialization not working.");
			}
			String uncompressedWord = dict.decompress(compressedString);
			if (!word.equals(uncompressedWord)) {
				System.out.println("failed on word [" + word + "] which decompressed to [" + uncompressedWord + "].");
			}
		}

	}

}
