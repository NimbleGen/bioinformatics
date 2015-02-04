package com.roche.sequencing.bioinformatics.common.utils;

/**
 * This class has convenience methods related to logarithms. Methods which act on arrays modify the input data as designed instead of creating new data items.
 * 
 * @author vkalluri
 * 
 */
public class LogarithmUtil {

	public static final double LOG2 = Math.log(2.0);
	// private static final Logger LOGGER =
	// LoggerFactory.getLogger(LogarithmUtil.class);

	private static final double largestLog2Number = 1023;

	/**
	 * private constructor for static methods
	 */
	private LogarithmUtil() {
		throw new AssertionError();
	}

	/**
	 * Returns anti-log of the given value
	 * 
	 * @param value
	 *            value for which to return an anti-log
	 * @return an anti-log (will return the largest Double value possible for any value greater than 1023)
	 */
	public static double antiLog2(double value) {
		double result = 0.0;
		if (value >= largestLog2Number) {
			result = Double.MAX_VALUE;
		} else {
			result = Math.pow(2.0, value);
		}
		return result;
	}

	/**
	 * Sets the anti-log applied data to the given input array. This method updates the input array with the changed values.
	 * 
	 * @param values
	 *            array of values to be converted to anti-logs
	 */
	public static double[] antiLog2(double[] log2Values) {
		double[] values = new double[log2Values.length];
		for (int index = 0; index < log2Values.length; index++) {
			values[index] = antiLog2(log2Values[index]);
		}
		return values;
	}

	/**
	 * Safely calculate log2 from a data value. Returns negative infinity if the data value is zero.
	 * 
	 * @param value
	 *            data value
	 * @return log2 of value
	 */
	public static double log2(double value) {
		if (value > 0.0) {
			return Math.log(value) / LOG2;
		} else {
			return Double.NaN;
		}
	}

	/**
	 * transforms all values in the return array into the log2 form of the value at the corresponding index
	 * 
	 * @param array
	 *            of values to be converted to log2
	 * @return array of values converted to log2
	 */
	public static double[] log2(double[] values) {
		double[] log2Values = new double[values.length];
		for (int index = 0; index < values.length; index++) {
			log2Values[index] = log2(values[index]);
		}
		return log2Values;
	}

	/**
	 * transforms all values in the return array into the log2 form of the value at the corresponding index
	 * 
	 * @param array
	 *            of values to be converted to log2
	 * @return array of values converted to log2
	 */
	public static double[] log2(float[] values) {
		double[] log2Values = new double[values.length];
		for (int index = 0; index < values.length; index++) {
			log2Values[index] = new Double(log2(new Float(values[index]).doubleValue())).floatValue();
		}
		return log2Values;
	}

	/**
	 * Calculates the antilog data for the given input array data. Input array is updated with the results.
	 * 
	 * @param values
	 *            input data
	 */
	public static void antiLog(double[] values) {
		if (values.length > 0) {
			for (int index = 0; index < values.length; index++) {
				values[index] = Math.pow(10, values[index]);
			}
		}
	}
}
