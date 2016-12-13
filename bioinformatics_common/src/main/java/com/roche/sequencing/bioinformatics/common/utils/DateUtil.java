/*
 *    Copyright 2016 Roche NimbleGen Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.roche.sequencing.bioinformatics.common.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 
 * Util for working with dates
 * 
 */
public class DateUtil {

	private DateUtil() {
		throw new AssertionError();
	}

	/**
	 * @return the current date in YYYY_MM_DD format
	 */
	public static String getCurrentDateINYYYY_MM_DD() {
		return convertTimeInMillisecondsToDateInYYY_MM_DD(System.currentTimeMillis());
	}

	/**
	 * @return the current date in YYYY_MM_DD_HH_MM_SS format
	 */
	public static String getCurrentDateINYYYY_MM_DD_HH_MM_SS() {
		return convertTimeInMillisecondsToDate(System.currentTimeMillis());
	}

	public static String getCurrentDateINYYYYMMDDHHMMSS() {
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		Date date = new Date(System.currentTimeMillis());
		return dateFormat.format(date);
	}

	public static String getCurrentDateINYYYYMMDDHHMMSSwithColons() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
		Date date = new Date(System.currentTimeMillis());
		return dateFormat.format(date);
	}

	/**
	 * 
	 * @param timeInMilliseconds
	 * @return the date provided by timeInMilliseconds in YYYY_MM_DD_HH_MM_SS format
	 */
	public static String convertTimeInMillisecondsToDate(long timeInMilliseconds) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date(timeInMilliseconds);
		return dateFormat.format(date);
	}

	/**
	 * 
	 * @param timeInMilliseconds
	 * @return the date provided by timeInMilliseconds in YYYY_MM_DD_HH_MM_SS format
	 */
	public static String convertTimeInMillisecondsToDateInYYY_MM_DD(long timeInMilliseconds) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		Date date = new Date(timeInMilliseconds);
		return dateFormat.format(date);
	}

	/**
	 * @return a milliscond duration in HH:MM:SS format
	 */
	public static String convertMillisecondsToHHMMSS(long milliseconds) {
		long millisecondsLeft = milliseconds;
		long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
		millisecondsLeft -= TimeUnit.HOURS.toMillis(hours);

		long minutes = TimeUnit.MILLISECONDS.toMinutes(millisecondsLeft);
		millisecondsLeft -= TimeUnit.MINUTES.toMillis(minutes);

		long seconds = TimeUnit.MILLISECONDS.toSeconds(millisecondsLeft);

		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}

	/**
	 * @return a milliscond duration in HH:MM:SS:MM format
	 */
	public static String convertMillisecondsToHHMMSSMMM(long milliseconds) {
		long millisecondsLeft = milliseconds;
		long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
		millisecondsLeft -= TimeUnit.HOURS.toMillis(hours);

		long minutes = TimeUnit.MILLISECONDS.toMinutes(millisecondsLeft);
		millisecondsLeft -= TimeUnit.MINUTES.toMillis(minutes);

		long seconds = TimeUnit.MILLISECONDS.toSeconds(millisecondsLeft);
		millisecondsLeft -= TimeUnit.SECONDS.toMillis(seconds);

		return String.format("%02d:%02d:%02d:%03d", hours, minutes, seconds, millisecondsLeft);
	}
}
