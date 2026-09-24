package com.petmgmt.ai;

import java.util.*;

/**
 * Cosine similarity based product recommender.
 * Translated from Python numpy-based implementation.
 */
public class ProductRecommender {

    private final String[] featureNames = {
        "is_food", "is_toy", "target_dog", "target_cat",
        "target_both", "price_norm", "is_premium", "is_budget"
    };

    public ProductRecommender() {}

    private double[][] buildProductMatrix(List<Map<String, Object>> products) {
        int n = products.size();
        if (n == 0) return new double[0][0];
        double[][] X = new double[n][featureNames.length];
        double maxPrice = products.stream()
                .mapToDouble(p -> ((Number) p.get("price")).doubleValue())
                .max().orElse(1.0);
        double avgPrice = products.stream()
                .mapToDouble(p -> ((Number) p.get("price")).doubleValue())
                .average().orElse(0);

        for (int i = 0; i < n; i++) {
            Map<String, Object> p = products.get(i);
            double price = ((Number) p.get("price")).doubleValue();
            String cat = (String) p.get("category");
            String target = (String) p.getOrDefault("target_species", "both");
            X[i][0] = "food".equals(cat) ? 1.0 : 0.0;
            X[i][1] = "toy".equals(cat) ? 1.0 : 0.0;
            X[i][2] = "dog".equals(target) ? 1.0 : 0.0;
            X[i][3] = "cat".equals(target) ? 1.0 : 0.0;
            X[i][4] = "both".equals(target) ? 1.0 : 0.0;
            X[i][5] = price / maxPrice;
            X[i][6] = price > avgPrice ? 1.0 : 0.0;
            X[i][7] = price < avgPrice / 2 ? 1.0 : 0.0;
        }
        return X;
    }

    public List<Map<String, Object>> recommend(String petSpecies, double petAge, double petWeight,
                                                List<Map<String, Object>> products, int topK) {
        if (products.isEmpty()) return Collections.emptyList();

        double[][] X = buildProductMatrix(products);
        if (X.length == 0) return Collections.emptyList();

        // Build pet preference vector
        double isDog = "dog".equals(petSpecies) ? 1.0 : 0.0;
        double isCat = "cat".equals(petSpecies) ? 1.0 : 0.0;
        double ageFactor = Math.min(1.0, (petAge > 0 ? petAge : 1) / 10.0);
        double weightFactor = Math.min(1.0, (petWeight > 0 ? petWeight : 5) / 40.0);

        double foodPref, toyPref;
        double ageVal = petAge > 0 ? petAge : 3;
        if (ageVal < 1) { foodPref = 0.8; toyPref = 0.6; }
        else if (ageVal < 7) { foodPref = 0.7; toyPref = 0.7; }
        else { foodPref = 0.5; toyPref = 0.3; }

        double pricePref = (petWeight > 0 ? petWeight : 10) > 25 ? 0.7 : 0.3;

        double[] userVec = {foodPref, toyPref, isDog, isCat, 0.3, pricePref, pricePref * 0.5, (1 - pricePref) * 0.5};

        // Cosine similarity scoring
        List<double[]> scores = new ArrayList<>();
        for (int i = 0; i < products.size(); i++) {
            Map<String, Object> p = products.get(i);
            double dot = 0, normU = 0, normP = 0;
            for (int j = 0; j < userVec.length; j++) {
                dot += userVec[j] * X[i][j];
                normU += userVec[j] * userVec[j];
                normP += X[i][j] * X[i][j];
            }
            normU = Math.sqrt(normU);
            normP = Math.sqrt(normP);
            double sim = (normU > 0 && normP > 0) ? dot / (normU * normP) : 0.0;

            // Species mismatch penalty
            String target = (String) p.getOrDefault("target_species", "both");
            if (!target.equals(petSpecies) && !"both".equals(target)) {
                sim *= 0.3;
            }
            // Stock bonus
            int stock = ((Number) p.getOrDefault("stock", 0)).intValue();
            double stockBonus = Math.min(0.1, stock / 500.0);
            scores.add(new double[]{sim + stockBonus, (double) i});
        }

        scores.sort((a, b) -> Double.compare(b[0], a[0]));

        List<Map<String, Object>> results = new ArrayList<>();
        for (int k = 0; k < Math.min(topK, scores.size()); k++) {
            if (scores.get(k)[0] <= 0.1) break;
            Map<String, Object> result = new LinkedHashMap<>(products.get((int) scores.get(k)[1]));
            result.put("similarity", Math.round(scores.get(k)[0] * 1000.0) / 1000.0);
            results.add(result);
        }
        return results;
    }
}
