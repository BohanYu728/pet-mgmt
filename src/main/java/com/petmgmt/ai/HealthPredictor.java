package com.petmgmt.ai;

import java.util.*;

/**
 * Linear regression model for predicting pet health scores (1-10).
 * Translated from Python numpy-based implementation using pure Java + Apache Commons Math.
 */
public class HealthPredictor {

    private double[] weights;
    private double bias;
    private final String[] featureNames = {
        "species_encoded", "age_normalized", "weight_normalized",
        "vaccine_count", "deworming_count", "checkup_count",
        "weight_trend", "has_breed_risk"
    };
    private static final Random RNG = new Random(42);

    public HealthPredictor() {
        train();
    }

    private double[][] generateTrainingData(int nSamples) {
        double[][] X = new double[nSamples][8];
        double[] y = new double[nSamples];

        for (int i = 0; i < nSamples; i++) {
            int species = RNG.nextInt(2); // 0=dog, 1=cat
            double age = 0.1 + RNG.nextDouble() * 14.9;
            double weight = species == 0 ? 1 + RNG.nextDouble() * 44 : 1 + RNG.nextDouble() * 11;
            int vaccineCount = RNG.nextInt(4);
            int dewormingCount = RNG.nextInt(3);
            int checkupCount = RNG.nextInt(3);
            int weightTrend = RNG.nextInt(3) - 1; // -1, 0, 1
            int breedRisk = RNG.nextInt(2);

            // Health score logic (mirrors Python implementation)
            double base = 6.0;
            base += (3 - Math.abs(2 - age / 5)) * 0.5;
            base += vaccineCount * 0.6;
            base += dewormingCount * 0.4;
            base += checkupCount * 0.5;
            base -= breedRisk * 0.5;
            if (weightTrend == -1) base += 0.3;
            else if (weightTrend == 1) base -= 0.2;
            double idealWeight = species == 0 ? 25 : 5;
            base -= Math.abs(weight - idealWeight) / idealWeight * 0.5;
            base += RNG.nextGaussian() * 0.3;
            double score = Math.max(1, Math.min(10, Math.round(base)));

            X[i][0] = species;
            X[i][1] = age / 15.0;
            X[i][2] = weight / 45.0;
            X[i][3] = vaccineCount / 3.0;
            X[i][4] = dewormingCount / 2.0;
            X[i][5] = checkupCount / 2.0;
            X[i][6] = weightTrend;
            X[i][7] = breedRisk;
            y[i] = score;
        }

        // Solve normal equation: theta = (X^T X)^-1 X^T y
        double[][] Xt = transpose(X);
        double[][] XtX = multiply(Xt, X);
        double[] Xty = multiplyVec(Xt, y);

        try {
            double[][] inv = invert(XtX);
            double[] theta = multiplyVec(inv, Xty);
            bias = theta[0];
            weights = Arrays.copyOfRange(theta, 1, theta.length);
        } catch (Exception e) {
            // Fallback weights
            bias = 6.0;
            weights = new double[]{-0.5, 1.0, -0.5, 1.2, 0.8, 1.0, 0.5, -0.8};
        }
        return X;
    }

    private void train() {
        generateTrainingData(500);
    }

    public double predict(int species, double age, double weight, int vaccineCount,
                          int dewormingCount, int checkupCount, int weightTrend, int breedRisk) {
        if (weights == null) return 7.0;
        double[] x = new double[]{
            species,
            age > 0 ? age / 15.0 : 0.5,
            weight > 0 ? weight / 45.0 : 0.5,
            vaccineCount / 3.0,
            dewormingCount / 2.0,
            checkupCount / 2.0,
            (double) weightTrend,
            (double) breedRisk
        };
        double score = bias;
        for (int i = 0; i < weights.length; i++) {
            score += weights[i] * x[i];
        }
        return Math.round(Math.max(1.0, Math.min(10.0, score)) * 10.0) / 10.0;
    }

    // --- Matrix helpers ---
    private static double[][] transpose(double[][] m) {
        int rows = m.length, cols = m[0].length;
        double[][] t = new double[cols][rows];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                t[j][i] = m[i][j];
        return t;
    }

    private static double[][] multiply(double[][] a, double[][] b) {
        int ra = a.length, ca = a[0].length, cb = b[0].length;
        double[][] r = new double[ra][cb];
        for (int i = 0; i < ra; i++)
            for (int k = 0; k < ca; k++)
                for (int j = 0; j < cb; j++)
                    r[i][j] += a[i][k] * b[k][j];
        return r;
    }

    private static double[] multiplyVec(double[][] a, double[] b) {
        double[][] bm = new double[b.length][1];
        for (int i = 0; i < b.length; i++) bm[i][0] = b[i];
        double[][] rm = multiply(a, bm);
        double[] r = new double[rm.length];
        for (int i = 0; i < rm.length; i++) r[i] = rm[i][0];
        return r;
    }

    private static double[][] invert(double[][] a) {
        int n = a.length;
        double[][] aug = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a[i], 0, aug[i], 0, n);
            aug[i][n + i] = 1;
        }
        for (int i = 0; i < n; i++) {
            double pivot = aug[i][i];
            if (Math.abs(pivot) < 1e-12) throw new ArithmeticException("Singular matrix");
            for (int j = 0; j < 2 * n; j++) aug[i][j] /= pivot;
            for (int k = 0; k < n; k++) {
                if (k == i) continue;
                double factor = aug[k][i];
                for (int j = 0; j < 2 * n; j++) aug[k][j] -= factor * aug[i][j];
            }
        }
        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++)
            System.arraycopy(aug[i], n, inv[i], 0, n);
        return inv;
    }
}
