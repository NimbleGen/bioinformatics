package com.roche.sequencing.bioinformatics.common.math;

import java.text.DecimalFormat;
import java.util.Arrays;

import com.roche.sequencing.bioinformatics.common.utils.StatisticsUtil;
import com.roche.sequencing.bioinformatics.common.utils.StringUtil;

public class SimpleLinearRegressionModel {

	private final static DecimalFormat formatter = new DecimalFormat("###.##");

	private final double[] xValues;
	private final double[] yValues;

	private final double xMean;
	private final double yMean;

	private final double xStandardDeviation;
	private final double yStandardDeviation;

	private final double yIntercept;
	private final double slope;
	private final double coefficientOfDetermination;

	private final double[] residualErrors;

	private double meanSquareError;
	private double rSquared;
	private double adjustedRSquared;

	private double regressionSumOfSquares;
	private double errorSumOfSquares;
	private double totalSumOfSquares;

	private double pearsonCorrelationCoefficient;

	private double fStatistic;

	private SimpleLinearRegressionModel(double[] xValues, double[] yValues, double xMean, double yMean, double xStandardDeviation, double yStandardDeviation, double yIntercept, double slope,
			double coefficientOfDetermination, double[] residualErrors, double meanSquareError, double rSquared, double adjustedRSquared, double regressionSumOfSquares, double errorSumOfSquares,
			double totalSumOfSquares, double pearsonCorrelationCoefficient) {
		super();
		this.xValues = xValues;
		this.yValues = yValues;
		this.xMean = xMean;
		this.yMean = yMean;
		this.xStandardDeviation = xStandardDeviation;
		this.yStandardDeviation = yStandardDeviation;
		this.yIntercept = yIntercept;
		this.slope = slope;
		this.coefficientOfDetermination = coefficientOfDetermination;
		this.residualErrors = residualErrors;
		this.meanSquareError = meanSquareError;
		this.rSquared = rSquared;
		this.adjustedRSquared = adjustedRSquared;
		this.regressionSumOfSquares = regressionSumOfSquares;
		this.errorSumOfSquares = errorSumOfSquares;
		this.totalSumOfSquares = totalSumOfSquares;
		this.pearsonCorrelationCoefficient = pearsonCorrelationCoefficient;
		this.fStatistic = regressionSumOfSquares / meanSquareError;
	}

	public double[] getxValues() {
		return xValues;
	}

	public double[] getyValues() {
		return yValues;
	}

	public double getxMean() {
		return xMean;
	}

	public double getyMean() {
		return yMean;
	}

	public double getxStandardDeviation() {
		return xStandardDeviation;
	}

	public double getyStandardDeviation() {
		return yStandardDeviation;
	}

	public double getyIntercept() {
		return yIntercept;
	}

	public double getSlope() {
		return slope;
	}

	public double getCoefficientOfDetermination() {
		return coefficientOfDetermination;
	}

	public double getPearsonCorrelationCoefficient() {
		return pearsonCorrelationCoefficient;
	}

	public void setPearsonCorrelationCoefficient(double pearsonCorrelationCoefficient) {
		this.pearsonCorrelationCoefficient = pearsonCorrelationCoefficient;
	}

	@Override
	public String toString() {
		return "SimpleLinearRegressionModel [xValues=" + Arrays.toString(xValues) + ", yValues=" + Arrays.toString(yValues) + ", xMean=" + xMean + ", yMean=" + yMean + ", xStandardDeviation="
				+ xStandardDeviation + ", yStandardDeviation=" + yStandardDeviation + ", yIntercept=" + yIntercept + ", slope=" + slope + ", coefficientOfDetermination=" + coefficientOfDetermination
				+ "]";
	}

	public String getMinitabOutput() {
		StringBuilder minitabOutput = new StringBuilder();
		minitabOutput.append("The regression equation is" + StringUtil.NEWLINE);
		minitabOutput.append("y = " + formatter.format(yIntercept) + " + " + formatter.format(slope) + " x" + StringUtil.NEWLINE);
		minitabOutput.append(StringUtil.NEWLINE);
		minitabOutput.append("S = " + formatter.format(Math.sqrt(meanSquareError)) + " R-Sq = " + formatter.format(rSquared * 100) + "% R-sq(adj) = " + formatter.format(adjustedRSquared * 100) + "%"
				+ StringUtil.NEWLINE);
		minitabOutput.append(StringUtil.NEWLINE);
		minitabOutput.append("Analysis of Variance" + StringUtil.NEWLINE);
		minitabOutput.append(StringUtil.NEWLINE);
		minitabOutput.append("Source" + StringUtil.TAB + "DF" + StringUtil.TAB + "SS" + StringUtil.TAB + "MS" + StringUtil.TAB + "F" + StringUtil.TAB + "P" + StringUtil.NEWLINE);
		minitabOutput.append("Regression" + StringUtil.TAB + "1" + StringUtil.TAB + formatter.format(regressionSumOfSquares) + StringUtil.TAB + formatter.format(regressionSumOfSquares)
				+ StringUtil.TAB + formatter.format(fStatistic) + StringUtil.TAB + "P-value" + StringUtil.NEWLINE);
		minitabOutput.append("Error" + StringUtil.TAB + (xValues.length - 2) + StringUtil.TAB + formatter.format(errorSumOfSquares) + StringUtil.TAB + formatter.format(meanSquareError)
				+ StringUtil.NEWLINE);
		minitabOutput.append("Total" + StringUtil.TAB + (xValues.length - 1) + StringUtil.TAB + formatter.format(totalSumOfSquares) + StringUtil.NEWLINE);

		return minitabOutput.toString();
	}

	public static SimpleLinearRegressionModel calculate(double[] xValues, double[] yValues) {
		double xMean = StatisticsUtil.arithmeticMean(xValues);
		double yMean = StatisticsUtil.arithmeticMean(yValues);

		double xStandardDeviation = StatisticsUtil.standardDeviation(xValues);
		double yStandardDeviation = StatisticsUtil.standardDeviation(yValues);

		double sumOfDifferences = 0;
		for (int i = 0; i < xValues.length; i++) {
			double x = xValues[i];
			double y = yValues[i];
			sumOfDifferences += ((x - xMean) * (y - yMean));
		}

		double sumOfXDiffSquared = 0;
		for (int i = 0; i < xValues.length; i++) {
			double x = xValues[i];
			sumOfXDiffSquared += Math.pow((x - xMean), 2);
		}

		double slope = sumOfDifferences / sumOfXDiffSquared;

		double yIntercept = yMean - slope * xMean;

		double xySdProduct = xStandardDeviation * yStandardDeviation;
		double n = xValues.length;
		double coefficientOfDetermination = Math.pow(sumOfDifferences / xySdProduct / n, 2);

		double[] residualErrors = new double[xValues.length];

		double regressionSumOfSquares = 0;
		double errorSumOfSquares = 0;
		double totalSumOfSquares = 0;

		for (int i = 0; i < xValues.length; i++) {
			double x = xValues[i];
			double actualY = yValues[i];
			double predictedY = yIntercept + slope * x;

			double residualError = actualY - predictedY;
			residualErrors[i] = residualError;
			errorSumOfSquares += Math.pow(residualError, 2);

			double regressionError = predictedY - yMean;
			regressionSumOfSquares += Math.pow(regressionError, 2);

			double squaresError = actualY - yMean;
			totalSumOfSquares += Math.pow(squaresError, 2);
		}
		// MSE
		double meanSquareError = errorSumOfSquares / (n - 2);

		double rSquared = regressionSumOfSquares / totalSumOfSquares;
		// adjR2 = 1 - ((1-R2)*(n - 1)/(n - p))
		int numberOfParameters = 2;
		double adjustedRSquared = 1 - ((1 - rSquared) * (n - 1) / (n - numberOfParameters));

		// r
		double pearsonCorrelationCoefficient = Math.sqrt(rSquared);
		if (slope < 0) {
			pearsonCorrelationCoefficient = -pearsonCorrelationCoefficient;
		}

		return new SimpleLinearRegressionModel(xValues, yValues, xMean, yMean, xStandardDeviation, yStandardDeviation, yIntercept, slope, coefficientOfDetermination, residualErrors, meanSquareError,
				rSquared, adjustedRSquared, regressionSumOfSquares, errorSumOfSquares, totalSumOfSquares, pearsonCorrelationCoefficient);
	}

	public static void main(String[] args) {
		double[] xValues = new double[] { 63, 64, 66, 69, 69, 71, 71, 72, 73, 75 };
		double[] yValues = new double[] { 127, 121, 142, 157, 162, 156, 169, 165, 181, 208 };
		SimpleLinearRegressionModel model = calculate(xValues, yValues);
		System.out.println(model.getMinitabOutput());
	}

	public double[] getResidualErrors() {
		return residualErrors;
	}

	public double getRSquared() {
		return rSquared;
	}

	public double getMeanSquareError() {
		return meanSquareError;
	}

}
