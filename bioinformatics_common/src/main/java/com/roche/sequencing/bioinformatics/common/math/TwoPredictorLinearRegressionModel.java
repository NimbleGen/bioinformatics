package com.roche.sequencing.bioinformatics.common.math;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.roche.sequencing.bioinformatics.common.utils.DelimitedFileParserUtil;
import com.roche.sequencing.bioinformatics.common.utils.StatisticsUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.UnableToFindHeaderException;

public class TwoPredictorLinearRegressionModel {

	private final static DecimalFormat formatter = new DecimalFormat("###.##");

	private double b0;
	private double b1;
	private double b2;

	private double x1Mean;
	private double x2Mean;
	private double yMean;

	private List<Predictors> predictorValues;
	private double[] yValues;

	private double[] residualErrors;

	private double meanSquareError;
	private double rSquared;
	private double adjustedRSquared;

	private double regressionSumOfSquares;
	private double errorSumOfSquares;
	private double totalSumOfSquares;

	private TwoPredictorLinearRegressionModel(double b0, double b1, double b2, double x1Mean, double x2Mean, double yMean, double[] x1Values, double[] x2Values, double[] yValues,
			double[] residualErrors, double meanSquareError, double rSquared, double adjustedRSquared, double regressionSumOfSquares, double errorSumOfSquares, double totalSumOfSquares) {
		super();
		this.b0 = b0;
		this.b1 = b1;
		this.b2 = b2;
		this.x1Mean = x1Mean;
		this.x2Mean = x2Mean;
		this.yMean = yMean;
		this.predictorValues = new ArrayList<TwoPredictorLinearRegressionModel.Predictors>();
		for (int i = 0; i < x1Values.length; i++) {
			this.predictorValues.add(new Predictors(x1Values[i], x2Values[i]));
		}
		this.yValues = yValues;
		this.residualErrors = residualErrors;
		this.meanSquareError = meanSquareError;
		this.rSquared = rSquared;
		this.adjustedRSquared = adjustedRSquared;
		this.regressionSumOfSquares = regressionSumOfSquares;
		this.errorSumOfSquares = errorSumOfSquares;
		this.totalSumOfSquares = totalSumOfSquares;
	}

	@Override
	public String toString() {
		return "MultipleLinearRegressionModel [b0=" + b0 + ", b1=" + b1 + ", b2=" + b2 + ", x1Mean=" + x1Mean + ", x2Mean=" + x2Mean + ", yMean=" + yMean;
	}

	public String getMinitabOutput() {
		StringBuilder minitabOutput = new StringBuilder();
		minitabOutput.append("The regression equation is" + StringUtil.NEWLINE);
		minitabOutput.append("y = " + formatter.format(b0) + " + " + formatter.format(b1) + " x1 + " + formatter.format(b2) + " x2" + StringUtil.NEWLINE);
		minitabOutput.append(StringUtil.NEWLINE);
		minitabOutput.append("S = " + formatter.format(Math.sqrt(meanSquareError)) + " R-Sq = " + formatter.format(rSquared * 100) + "% R-sq(adj) = " + formatter.format(adjustedRSquared * 100) + "%"
				+ StringUtil.NEWLINE);
		minitabOutput.append(StringUtil.NEWLINE);
		minitabOutput.append("Analysis of Variance" + StringUtil.NEWLINE);
		minitabOutput.append(StringUtil.NEWLINE);
		minitabOutput.append("Source" + StringUtil.TAB + "DF" + StringUtil.TAB + "SS" + StringUtil.TAB + "MS" + StringUtil.TAB + "F" + StringUtil.TAB + "P" + StringUtil.NEWLINE);
		minitabOutput.append("Regression" + StringUtil.TAB + "1" + StringUtil.TAB + formatter.format(regressionSumOfSquares) + StringUtil.TAB + formatter.format(regressionSumOfSquares)
				+ StringUtil.TAB + "f-stat" + StringUtil.TAB + "P-value" + StringUtil.NEWLINE);
		minitabOutput.append("Error" + StringUtil.TAB + (yValues.length - 2) + StringUtil.TAB + formatter.format(errorSumOfSquares) + StringUtil.TAB + formatter.format(meanSquareError)
				+ StringUtil.NEWLINE);
		minitabOutput.append("Total" + StringUtil.TAB + (yValues.length - 1) + StringUtil.TAB + formatter.format(totalSumOfSquares) + StringUtil.NEWLINE);

		return minitabOutput.toString();
	}

	public double[] getResidualErrors() {
		return residualErrors;
	}

	// http://faculty.cas.usf.edu/mbrannick/regression/Reg2IV.html
	// https://onlinecourses.science.psu.edu/stat501/node/285
	public static TwoPredictorLinearRegressionModel calculateModel(double[] x1Values, double[] x2Values, double[] yValues) {
		double yMean = StatisticsUtil.arithmeticMean(yValues);
		double x1Mean = StatisticsUtil.arithmeticMean(x1Values);
		double x2Mean = StatisticsUtil.arithmeticMean(x2Values);

		double sumOfX1 = 0;
		double sumOfX1Squared = 0;
		for (double x1 : x1Values) {
			sumOfX1 += x1;
			sumOfX1Squared += (x1 * x1);
		}

		double sumOfX2 = 0;
		double sumOfX2Squared = 0;
		for (double x2 : x2Values) {
			sumOfX2 += x2;
			sumOfX2Squared += (x2 * x2);
		}

		double sumOfY = 0;
		double sumOfX1Y = 0;
		double sumOfX1X2 = 0;
		double sumOfX2Y = 0;
		for (int i = 0; i < yValues.length; i++) {
			double x1 = x1Values[i];
			double x2 = x2Values[i];
			double y = yValues[i];

			sumOfY += y;
			sumOfX1Y += (x1 * y);
			sumOfX1X2 += (x1 * x2);
			sumOfX2Y += (x2 * y);
		}

		double n = yValues.length;

		sumOfX1Y -= (sumOfX1 * sumOfY) / n;
		sumOfX2Y -= (sumOfX2 * sumOfY) / n;
		sumOfX1X2 -= (sumOfX1 * sumOfX2) / n;
		sumOfX1Squared -= (sumOfX1 * sumOfX1) / n;
		sumOfX2Squared -= (sumOfX2 * sumOfX2) / n;

		double b1Numerator = (sumOfX2Squared * sumOfX1Y) - (sumOfX1X2 * sumOfX2Y);
		double denominator = (sumOfX1Squared * sumOfX2Squared) - (sumOfX1X2 * sumOfX1X2);
		double b1 = b1Numerator / denominator;

		double b2Numerator = (sumOfX1Squared * sumOfX2Y) - (sumOfX1X2 * sumOfX1Y);
		double b2 = b2Numerator / denominator;

		double b0 = yMean - (b1 * x1Mean) - (b2 * x2Mean);

		double regressionSumOfSquares = 0;
		double errorSumOfSquares = 0;
		double totalSumOfSquares = 0;

		double[] residualErrors = new double[yValues.length];
		for (int i = 0; i < yValues.length; i++) {
			double x1 = x1Values[i];
			double x2 = x2Values[i];
			double yPredicted = b0 + (x1 * b1) + (x2 * b2);
			double yActual = yValues[i];
			double residualError = yActual - yPredicted;
			residualErrors[i] = residualError;

			errorSumOfSquares += Math.pow(residualError, 2);

			double regressionError = yPredicted - yMean;
			regressionSumOfSquares += Math.pow(regressionError, 2);

			double squaresError = yPredicted - yMean;
			totalSumOfSquares += Math.pow(squaresError, 2);
		}

		// MSE
		double meanSquareError = errorSumOfSquares / (n - 2);

		double rSquared = regressionSumOfSquares / totalSumOfSquares;
		// adjR2 = 1 - ((1-R2)*(n - 1)/(n - p))
		int numberOfParameters = 3;
		double adjustedRSquared = 1 - ((1 - rSquared) * (n - 1) / (n - numberOfParameters));

		return new TwoPredictorLinearRegressionModel(b0, b1, b2, x1Mean, x2Mean, yMean, x1Values, x2Values, yValues, residualErrors, meanSquareError, rSquared, adjustedRSquared,
				regressionSumOfSquares, errorSumOfSquares, totalSumOfSquares);
	}

	public double getPredictedValue(double x1, double x2) {
		double predictedValue = b0 + (x1 * b1) + (x2 * b2);
		return predictedValue;
	}

	public static void main(String[] args) throws UnableToFindHeaderException, IOException {
		example1();
	}

	public static void example1() throws UnableToFindHeaderException, IOException {
		double[] x1Values = new double[] {};
		double[] x2Values = new double[] {};
		double[] yValues = new double[] {};

		File file = new File("c:\\Users\\kurt\\Desktop\\data.txt");
		for (Entry<String, List<String>> entry : DelimitedFileParserUtil.getHeaderNameToValuesMapFromDelimitedFile(file, new String[] { "Vent", "O2", "CO2" }, StringUtil.TAB, false).entrySet()) {
			String columnHeader = entry.getKey();
			List<String> values = entry.getValue();

			double[] doubleArray = new double[values.size()];
			for (int i = 0; i < values.size(); i++) {
				doubleArray[i] = Double.parseDouble(values.get(i));
			}

			if (columnHeader.equals("Vent")) {
				yValues = doubleArray;
			} else if (columnHeader.equals("O2")) {
				x1Values = doubleArray;
			} else if (columnHeader.equals("CO2")) {
				x2Values = doubleArray;
			}
		}
		TwoPredictorLinearRegressionModel model = calculateModel(x1Values, x2Values, yValues);
		System.out.println(model.getMinitabOutput());
	}

	public static void example2() {
		double[] x2Values = new double[] { 25, 20, 30, 30, 28, 30, 34, 36, 32, 34, 38, 28, 30, 36, 34, 38, 42, 38, 34, 38 };
		double[] x1Values = new double[] { 40, 45, 38, 50, 48, 55, 53, 55, 58, 40, 55, 48, 45, 55, 60, 60, 60, 65, 50, 58 };
		double[] yValues = new double[] { 1, 2, 1, 3, 2, 3, 3, 4, 4, 3, 5, 3, 3, 2, 4, 5, 5, 5, 4, 3 };

		TwoPredictorLinearRegressionModel model = calculateModel(x1Values, x2Values, yValues);
		System.out.println(model.getMinitabOutput());
	}

	public boolean isUsedForCreatingRegressionModel(double x1, double x2) {
		boolean isUsedforCreatingRegressionModel = predictorValues.contains(new Predictors(x1, x2));
		return isUsedforCreatingRegressionModel;
	}

	private static class Predictors {
		private final double x1;
		private final double x2;

		public Predictors(double x1, double x2) {
			super();
			this.x1 = x1;
			this.x2 = x2;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(x1);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(x2);
			result = prime * result + (int) (temp ^ (temp >>> 32));
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
			Predictors other = (Predictors) obj;
			if (Double.doubleToLongBits(x1) != Double.doubleToLongBits(other.x1))
				return false;
			if (Double.doubleToLongBits(x2) != Double.doubleToLongBits(other.x2))
				return false;
			return true;
		}

	}

	public String getFormula() {
		return "y = " + formatter.format(b0) + " + " + formatter.format(b1) + " x1 + " + formatter.format(b2) + " x2";
	}

	public double getB0() {
		return b0;
	}

	public double getB1() {
		return b1;
	}

	public double getB2() {
		return b2;
	}
}
