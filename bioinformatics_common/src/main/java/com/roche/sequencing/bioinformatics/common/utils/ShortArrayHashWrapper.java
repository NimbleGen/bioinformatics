package com.roche.sequencing.bioinformatics.common.utils;

import java.util.Arrays;

public class ShortArrayHashWrapper {
	private final short[] array;

	public ShortArrayHashWrapper(short[] array) {
		super();
		this.array = array;
	}

	public short[] getArray() {
		return array;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(array);
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
		ShortArrayHashWrapper other = (ShortArrayHashWrapper) obj;
		if (!Arrays.equals(array, other.array))
			return false;
		return true;
	}

}
