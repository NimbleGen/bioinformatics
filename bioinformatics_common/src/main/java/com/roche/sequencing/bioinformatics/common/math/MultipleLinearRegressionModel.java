package com.roche.sequencing.bioinformatics.common.math;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.la4j.Matrix;
import org.la4j.Vector;
import org.la4j.inversion.GaussJordanInverter;
import org.la4j.inversion.MatrixInverter;
import org.la4j.matrix.DenseMatrix;

import com.roche.sequencing.bioinformatics.common.utils.ArraysUtil;
import com.roche.sequencing.bioinformatics.common.utils.DelimitedFileParserUtil;
import com.roche.sequencing.bioinformatics.common.utils.StatisticsUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;
import com.roche.sequencing.bioinformatics.common.utils.UnableToFindHeaderException;

public class MultipleLinearRegressionModel {

	private final static DecimalFormat formatter = new DecimalFormat("###.##");

	private double[] coefficients;

	private Set<Predictors> predictorValues;
	private double[] yValues;

	private double[] residualErrors;

	private double meanSquareError;
	private double rSquared;
	private double adjustedRSquared;

	private double regressionSumOfSquares;
	private double errorSumOfSquares;
	private double totalSumOfSquares;

	public MultipleLinearRegressionModel(double[] coefficients, Set<Predictors> predictorValues, double[] yValues, double[] residualErrors, double meanSquareError, double rSquared,
			double adjustedRSquared, double regressionSumOfSquares, double errorSumOfSquares, double totalSumOfSquares) {
		super();
		this.coefficients = coefficients;
		this.predictorValues = predictorValues;
		this.yValues = yValues;
		this.residualErrors = residualErrors;
		this.meanSquareError = meanSquareError;
		this.rSquared = rSquared;
		this.adjustedRSquared = adjustedRSquared;
		this.regressionSumOfSquares = regressionSumOfSquares;
		this.errorSumOfSquares = errorSumOfSquares;
		this.totalSumOfSquares = totalSumOfSquares;
	}

	// @Override
	// public String toString() {
	// return "MultipleLinearRegressionModel [b0=" + b0 + ", b1=" + b1 + ", b2=" + b2 + ", x1Mean=" + x1Mean + ", x2Mean=" + x2Mean + ", yMean=" + yMean;
	// }
	//
	// public String getMinitabOutput() {
	// StringBuilder minitabOutput = new StringBuilder();
	// minitabOutput.append("The regression equation is" + StringUtil.NEWLINE);
	// minitabOutput.append("y = " + formatter.format(b0) + " + " + formatter.format(b1) + " x1 + " + formatter.format(b2) + " x2" + StringUtil.NEWLINE);
	// minitabOutput.append(StringUtil.NEWLINE);
	// minitabOutput.append("S = " + formatter.format(Math.sqrt(meanSquareError)) + " R-Sq = " + formatter.format(rSquared * 100) + "% R-sq(adj) = " + formatter.format(adjustedRSquared * 100) + "%"
	// + StringUtil.NEWLINE);
	// minitabOutput.append(StringUtil.NEWLINE);
	// minitabOutput.append("Analysis of Variance" + StringUtil.NEWLINE);
	// minitabOutput.append(StringUtil.NEWLINE);
	// minitabOutput.append("Source" + StringUtil.TAB + "DF" + StringUtil.TAB + "SS" + StringUtil.TAB + "MS" + StringUtil.TAB + "F" + StringUtil.TAB + "P" + StringUtil.NEWLINE);
	// minitabOutput.append("Regression" + StringUtil.TAB + "1" + StringUtil.TAB + formatter.format(regressionSumOfSquares) + StringUtil.TAB + formatter.format(regressionSumOfSquares)
	// + StringUtil.TAB + "f-stat" + StringUtil.TAB + "P-value" + StringUtil.NEWLINE);
	// minitabOutput.append("Error" + StringUtil.TAB + (yValues.length - 2) + StringUtil.TAB + formatter.format(errorSumOfSquares) + StringUtil.TAB + formatter.format(meanSquareError)
	// + StringUtil.NEWLINE);
	// minitabOutput.append("Total" + StringUtil.TAB + (yValues.length - 1) + StringUtil.TAB + formatter.format(totalSumOfSquares) + StringUtil.NEWLINE);
	//
	// return minitabOutput.toString();
	// }

	public double[] getResidualErrors() {
		return residualErrors;
	}

	// http://faculty.cas.usf.edu/mbrannick/regression/Reg2IV.html
	// https://onlinecourses.science.psu.edu/stat501/node/285
	public static MultipleLinearRegressionModel calculateModel(double[] yValues, double[]... xValues) {
		double yMean = StatisticsUtil.arithmeticMean(yValues);

		// b = ((X'X)^-1)(X'Y)

		double[][] xValuesWithFirstColumnAsOnes = new double[xValues.length + 1][yValues.length];
		double[] firstColumn = ArraysUtil.createDoubleArray(yValues.length, 1);
		xValuesWithFirstColumnAsOnes[0] = firstColumn;
		for (int i = 0; i < xValues.length; i++) {
			xValuesWithFirstColumnAsOnes[i + 1] = xValues[i];
		}

		// X'
		Matrix xMatrixTransposed = DenseMatrix.from2DArray(xValuesWithFirstColumnAsOnes);
		// System.out.println("X':" + StringUtil.NEWLINE + xMatrixTransposed.toCSV());

		// X
		Matrix xMatrix = xMatrixTransposed.transpose();
		// System.out.println("X:" + StringUtil.NEWLINE + xMatrix.toCSV());

		// (X'X)
		Matrix xTimesXTransposed = xMatrixTransposed.multiply(xMatrix);
		// System.out.println("X'X:" + StringUtil.NEWLINE + xTimesXTransposed.toCSV());

		// (X'X)^-1
		MatrixInverter inverter = new GaussJordanInverter(xTimesXTransposed);
		Matrix inverseOfXTimesXTransposed = inverter.inverse();
		// System.out.println("(X'X)^-1:" + StringUtil.NEWLINE + inverseOfXTimesXTransposed.toCSV());
		// Y
		Matrix yMatrix = DenseMatrix.from1DArray(yValues.length, 1, yValues);
		// System.out.println("Y:" + StringUtil.NEWLINE + yMatrix.toCSV());

		// X'Y
		Matrix xPrimeY = xMatrixTransposed.multiply(yMatrix);
		// System.out.println("X'Y:" + StringUtil.NEWLINE + xPrimeY.toCSV());

		// ((X'X)^-1)(X'Y)
		Matrix b = inverseOfXTimesXTransposed.multiply(xPrimeY);
		// System.out.println("b:" + StringUtil.NEWLINE + b.toCSV());

		Vector coefficientsAsVector = b.getColumn(0);
		double[] coefficients = new double[coefficientsAsVector.length()];
		for (int i = 0; i < coefficientsAsVector.length(); i++) {
			coefficients[i] = coefficientsAsVector.get(i);
		}

		double regressionSumOfSquares = 0;
		double errorSumOfSquares = 0;
		double totalSumOfSquares = 0;
		Set<Predictors> predictorValues = new LinkedHashSet<MultipleLinearRegressionModel.Predictors>();

		double[] residualErrors = new double[yValues.length];
		for (int i = 0; i < yValues.length; i++) {
			double[] xPredictors = new double[xValues.length];
			// first column is 1s which were not passed in
			for (int j = 0; j < xPredictors.length; j++) {
				xPredictors[j] = xValues[j][i];
			}
			Predictors predictors = new Predictors(xPredictors);
			predictorValues.add(predictors);

			double yPredicted = predict(coefficients, xPredictors);
			double yActual = yValues[i];
			double residualError = yActual - yPredicted;
			residualErrors[i] = residualError;

			errorSumOfSquares += Math.pow(residualError, 2);

			double regressionError = yPredicted - yMean;
			regressionSumOfSquares += Math.pow(regressionError, 2);

			double squaresError = yPredicted - yMean;
			totalSumOfSquares += Math.pow(squaresError, 2);
		}

		int n = yValues.length;

		// MSE
		double meanSquareError = errorSumOfSquares / (n - 2);

		double rSquared = regressionSumOfSquares / totalSumOfSquares;
		// adjR2 = 1 - ((1-R2)*(n - 1)/(n - p))
		int numberOfParameters = 3;
		double adjustedRSquared = 1 - ((1 - rSquared) * (n - 1) / (n - numberOfParameters));

		return new MultipleLinearRegressionModel(coefficients, predictorValues, yValues, residualErrors, meanSquareError, rSquared, adjustedRSquared, regressionSumOfSquares, errorSumOfSquares,
				totalSumOfSquares);
	}

	private static double predict(double[] coefficients, double[] xValues) {
		double prediction = coefficients[0];
		for (int i = 0; i < xValues.length; i++) {
			prediction += xValues[i] * coefficients[i + 1];
		}
		return prediction;
	}

	public double getPredictedValue(double[] xValues) {
		double predictedValue = predict(coefficients, xValues);
		return predictedValue;
	}

	public boolean isUsedForCreatingRegressionModel(double... xValues) {
		Predictors predictors = new Predictors(xValues);
		boolean isUsedforCreatingRegressionModel = predictorValues.contains(predictors);
		return isUsedforCreatingRegressionModel;
	}

	private static class Predictors {
		private final double[] xValues;

		public Predictors(double... xValues) {
			super();
			this.xValues = xValues;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(xValues);
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
			if (!Arrays.equals(xValues, other.xValues))
				return false;
			return true;
		}

	}

	public String getFormula() {
		StringBuilder formula = new StringBuilder();
		formula.append("y = " + formatter.format(coefficients[0]));
		for (int i = 1; i < coefficients.length; i++) {
			formula.append(" + " + formatter.format(coefficients[i]) + "X" + i);
		}
		return formula.toString();
	}

	public double[] getCoefficients() {
		return coefficients;
	}

	public static void main(String[] args) throws UnableToFindHeaderException, IOException {
		example3();
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
		MultipleLinearRegressionModel model = calculateModel(yValues, x1Values, x2Values);
		// System.out.println(model.getMinitabOutput());
	}

	public static void example2() {
		double[] x2Values = new double[] { 25, 20, 30, 30, 28, 30, 34, 36, 32, 34, 38, 28, 30, 36, 34, 38, 42, 38, 34, 38 };
		double[] x1Values = new double[] { 40, 45, 38, 50, 48, 55, 53, 55, 58, 40, 55, 48, 45, 55, 60, 60, 60, 65, 50, 58 };
		double[] yValues = new double[] { 1, 2, 1, 3, 2, 3, 3, 4, 4, 3, 5, 3, 3, 2, 4, 5, 5, 5, 4, 3 };

		MultipleLinearRegressionModel model = calculateModel(yValues, x1Values, x2Values);
		// System.out.println(model.getMinitabOutput());
	}

	public static void example3() {
		double[] xValues = new double[] { 4, 4.5, 5, 5.5, 6, 6.5, 7 };
		double[] x2Values = new double[] { 4.5, 5, 5.5, 6, 6.5, 7, 12 };
		double[] x3Values = new double[] { 5, 5.5, 6, 6.5, 7, 9, 13 };
		double[] yValues = new double[] { 33, 42, 45, 51, 53, 61, 62 };

		MultipleLinearRegressionModel model = calculateModel(yValues, xValues, x2Values, x3Values);
		if (!model.isUsedForCreatingRegressionModel(4, 4.5, 5)) {
			System.out.println("error");
		}
		System.out.println(model.getFormula());
	}

}
