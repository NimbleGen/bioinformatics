package com.roche.sequencing.bioinformatics.common.utils;

import java.nio.ByteOrder;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class PosixNumericalFilePermissions {

	private final static String PERMISSIONS_REGEX = "^[0-7]{3}$";

	private Set<PosixFilePermission> permissions;
	private int permissionsAsInt;

	public PosixNumericalFilePermissions(String numericalPosixPermissions) {
		if (!Pattern.matches(PERMISSIONS_REGEX, numericalPosixPermissions)) {
			throw new IllegalArgumentException("The provided argument[" + numericalPosixPermissions
					+ "] for numericalPosixPermissions does not conform to the expected format which consists of 3 numbers valued 1-7.must");
		}

		permissionsAsInt = Integer.parseInt(numericalPosixPermissions);
		this.permissions = new HashSet<PosixFilePermission>();

		int ownerPermissionsVaue = Integer.parseInt("" + numericalPosixPermissions.charAt(0));
		byte ownerByte = ByteUtil.convertIntToBytes(ownerPermissionsVaue, 1, ByteOrder.BIG_ENDIAN, false)[0];
		if (ByteUtil.isBitOn(ownerByte, 0)) {
			permissions.add(PosixFilePermission.OWNER_EXECUTE);
		}
		if (ByteUtil.isBitOn(ownerByte, 1)) {
			permissions.add(PosixFilePermission.OWNER_WRITE);
		}
		if (ByteUtil.isBitOn(ownerByte, 2)) {
			permissions.add(PosixFilePermission.OWNER_READ);
		}

		int groupPermissionsVaue = Integer.parseInt("" + numericalPosixPermissions.charAt(1));
		byte groupByte = ByteUtil.convertIntToBytes(groupPermissionsVaue, 1, ByteOrder.BIG_ENDIAN, false)[0];
		if (ByteUtil.isBitOn(groupByte, 0)) {
			permissions.add(PosixFilePermission.GROUP_EXECUTE);
		}
		if (ByteUtil.isBitOn(groupByte, 1)) {
			permissions.add(PosixFilePermission.GROUP_WRITE);
		}
		if (ByteUtil.isBitOn(groupByte, 2)) {
			permissions.add(PosixFilePermission.GROUP_READ);
		}

		int otherPermissionsVaue = Integer.parseInt("" + numericalPosixPermissions.charAt(2));
		byte otherByte = ByteUtil.convertIntToBytes(otherPermissionsVaue, 1, ByteOrder.BIG_ENDIAN, false)[0];
		if (ByteUtil.isBitOn(otherByte, 0)) {
			permissions.add(PosixFilePermission.OTHERS_EXECUTE);
		}
		if (ByteUtil.isBitOn(otherByte, 1)) {
			permissions.add(PosixFilePermission.OTHERS_WRITE);
		}
		if (ByteUtil.isBitOn(otherByte, 2)) {
			permissions.add(PosixFilePermission.OTHERS_READ);
		}
	}

	public PosixNumericalFilePermissions(int numericalPosixPermissions) {
		this("" + numericalPosixPermissions);
	}

	public PosixNumericalFilePermissions(Set<PosixFilePermission> filePermissions) {
		this.permissions = Collections.unmodifiableSet(filePermissions);
		int value = 0;

		if (filePermissions.contains(PosixFilePermission.OWNER_EXECUTE)) {
			value += 100;
		}
		if (filePermissions.contains(PosixFilePermission.OWNER_WRITE)) {
			value += 200;
		}
		if (filePermissions.contains(PosixFilePermission.OWNER_READ)) {
			value += 400;
		}

		if (filePermissions.contains(PosixFilePermission.GROUP_EXECUTE)) {
			value += 10;
		}
		if (filePermissions.contains(PosixFilePermission.GROUP_WRITE)) {
			value += 20;
		}
		if (filePermissions.contains(PosixFilePermission.GROUP_READ)) {
			value += 40;
		}

		if (filePermissions.contains(PosixFilePermission.OTHERS_EXECUTE)) {
			value += 1;
		}
		if (filePermissions.contains(PosixFilePermission.OTHERS_WRITE)) {
			value += 2;
		}
		if (filePermissions.contains(PosixFilePermission.OTHERS_READ)) {
			value += 4;
		}
		this.permissionsAsInt = value;
	}

	public Set<PosixFilePermission> getPosixFilePermissions() {
		return permissions;
	}

	public int getPermissions() {
		return permissionsAsInt;
	}

}
