package com.lorentzos.flingswipe;

/*************************************************************************
 *  Compilation:  javac LinearRegression.java
 *  Execution:    java  LinearRegression
 *
 *  Compute least squares solution to y = beta * x + alpha.
 *  Simple linear regression.
 *
 *************************************************************************/

/**
 * The <tt>LinearRegression</tt> class performs a simple linear regression
 * on an set of <em>N</em> data points (<em>y<sub>i</sub></em>, <em>x<sub>i</sub></em>).
 * That is, it fits a straight line <em>y</em> = &alpha; + &beta; <em>x</em>,
 * (where <em>y</em> is the response variable, <em>x</em> is the predictor variable,
 * &alpha; is the <em>y-intercept</em>, and &beta; is the <em>slope</em>)
 * that minimizes the sum of squared residuals of the linear regression model.
 * It also computes associated statistics, including the coefficient of
 * determination <em>R</em><sup>2</sup> and the standard deviation of the
 * estimates for the slope and <em>y</em>-intercept.
 *
 * @author Robert Sedgewick
 * @author Kevin Wayne
 */
public class LinearRegression {
    private final double alpha, beta;
    private final double R2;

    /**
     * Performs a linear regression on the data points <tt>(y[i], x[i])</tt>.
     *
     * @param x the values of the predictor variable
     * @param y the corresponding values of the response variable
     * @throws java.lang.IllegalArgumentException if the lengths of the two arrays are not equal
     */
    public LinearRegression(float[] x, float[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("array lengths are not equal");
        }
        int n = x.length;

        // First pass.
        double sumX = 0.0, sumY = 0.0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
        }
        double xBar = sumX / n;
        double yBar = sumY / n;

        // Second pass: compute summary statistics.
        double xxBar = 0.0, yyBar = 0.0, xyBar = 0.0;
        for (int i = 0; i < n; i++) {
            xxBar += (x[i] - xBar) * (x[i] - xBar);
            yyBar += (y[i] - yBar) * (y[i] - yBar);
            xyBar += (x[i] - xBar) * (y[i] - yBar);
        }
        beta = xyBar / xxBar;
        alpha = yBar - beta * xBar;

        // More statistical analysis.
        double ssr = 0.0; // Regression sum of squares.
        for (float aX : x) {
            double fit = beta * aX + alpha;
            ssr += (fit - yBar) * (fit - yBar);
        }
        R2 = ssr / yyBar;
    }

    /**
     * Returns the <em>y</em>-intercept &alpha; of the best of the best-fit line <em>y</em> =
     * &alpha; + &beta; <em>x</em>.
     *
     * @return the <em>y</em>-intercept &alpha; of the best-fit line <em>y = &alpha; + &beta; x</em>
     */
    public double intercept() {
        return alpha;
    }

    /**
     * Returns the slope &beta; of the best of the best-fit line <em>y</em> = &alpha; + &beta;
     * <em>x</em>.
     *
     * @return the slope &beta; of the best-fit line <em>y</em> = &alpha; + &beta; <em>x</em>
     */
    public double slope() {
        return beta;
    }

    /**
     * Returns the coefficient of determination <em>R</em><sup>2</sup>.
     *
     * @return the coefficient of determination <em>R</em><sup>2</sup>, which is a real number
     * between 0 and 1
     */
    public double R2() {
        return R2;
    }

    /**
     * Returns a string representation of the simple linear regression model.
     *
     * @return a string representation of the simple linear regression model,
     * including the best-fit line and the coefficient of determination <em>R</em><sup>2</sup>
     */
    public String toString() {
        String s = "";
        s += String.format("%.2f N + %.2f", slope(), intercept());
        return s + "  (R^2 = " + String.format("%.3f", R2()) + ")";
    }
}
