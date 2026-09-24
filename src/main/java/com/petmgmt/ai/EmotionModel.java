package com.petmgmt.ai;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

/**
 * Heuristic multi-feature pet emotion detection from images.
 * Analyzes HSV color features, edge density, contrast, and texture
 * to predict emotional state. Pure Java translation of Python implementation.
 */
public class EmotionModel {

    public static final String[] EMOTIONS = {"happy", "relaxed", "alert", "anxious", "scared", "tired", "sad"};

    public static final Map<String, String> EMOTION_LABELS_ZH = Map.of(
        "happy", "开心", "relaxed", "放松", "alert", "警惕",
        "anxious", "焦虑", "scared", "害怕", "tired", "疲惫", "sad", "伤心"
    );

    public static final Map<String, String> COMFORT_ADVICE = Map.of(
        "happy", "您的宠物心情很好！建议继续保持目前的环境和互动方式，可以适当进行一些游戏和奖励。多带狗狗外出散步，或与猫咪进行逗猫棒游戏。",
        "relaxed", "宠物状态非常放松舒适。建议维持安静舒适的环境，不要打扰宠物休息。可以轻轻抚摸，用温柔的声音交流。",
        "alert", "宠物似乎注意到了什么，处于警觉状态。建议检查周围环境是否有异常声响或陌生气味。可以轻声安抚。",
        "anxious", "宠物可能感到不安和焦虑。建议用温柔的声音安抚，可以尝试轻轻按摩宠物颈部和背部。如果持续焦虑，建议咨询兽医。",
        "scared", "宠物处于受惊状态，请立即移除可能的压力源。给宠物提供一个安全的躲藏空间，不要强行抱它。用平静的语气说话。",
        "tired", "宠物看起来有些疲惫。请确保它有安静舒适的休息空间。如果持续精神不振，建议带去宠物医院做体检。",
        "sad", "宠物看起来情绪低落。建议多陪伴宠物，用温柔的声音和轻柔的抚摸给予安慰。尝试用喜欢的玩具或零食调动情绪。"
    );

    public Map<String, Object> predict(String imagePath) {
        try {
            BufferedImage img = ImageUtils.loadImage(imagePath, 256, 256);
            Map<String, Object> hsvFeats = ImageUtils.extractHsvFeatures(img);
            Map<String, Object> edgeFeats = ImageUtils.extractEdgeFeatures(img);

            double sat = (Double) hsvFeats.get("mean_saturation");
            double bri = (Double) hsvFeats.get("mean_brightness");
            double con = (Double) edgeFeats.get("contrast");
            double edg = (Double) edgeFeats.get("edge_density");
            double[] hueHist = (double[]) hsvFeats.get("hue_histogram");

            double warm = hueHist[0] + hueHist[1];
            double cool = hueHist[3] + hueHist[4] + hueHist[5];
            double neutralHue = hueHist[2];
            double hueVariance = 0;
            for (int i = 0; i < 6; i++) hueVariance += Math.abs(hueHist[i] - 1.0 / 6);
            double isVivid = Math.min(hueVariance / 0.4, 1.0);

            // Emotion scoring
            Map<String, Double> raw = new LinkedHashMap<>();
            raw.put("happy",
                safeMin(sat, 0.45) * 0.25 + safeMin(warm, 0.35) * 0.25 +
                safeMin(bri, 0.55) * 0.20 + gaussianLike(edg, 0.30, 0.25) * 0.15 + isVivid * 0.15);

            raw.put("relaxed",
                gaussianLike(sat, 0.30, 0.25) * 0.25 + gaussianLike(bri, 0.50, 0.30) * 0.25 +
                (1.0 - safeMin(edg, 0.35)) * 0.30 + gaussianLike(warm, 0.35, 0.25) * 0.20);

            raw.put("alert",
                safeMin(con, 0.40) * 0.30 + safeMin(edg, 0.35) * 0.30 +
                safeMin(bri, 0.45) * 0.25 + isVivid * 0.15);

            raw.put("anxious",
                safeMin(cool, 0.30) * 0.30 + safeMin(edg, 0.30) * 0.25 +
                safeMin(con, 0.35) * 0.25 + (1.0 - safeMin(bri, 0.65)) * 0.20);

            raw.put("scared",
                (1.0 - safeMin(bri, 0.40)) * 0.30 + safeMin(con, 0.50) * 0.25 +
                safeMin(edg, 0.40) * 0.25 + safeMin(cool, 0.25) * 0.20);

            raw.put("tired",
                (1.0 - safeMin(sat, 0.30)) * 0.30 + (1.0 - safeMin(con, 0.35)) * 0.25 +
                (1.0 - safeMin(edg, 0.30)) * 0.25 + gaussianLike(bri, 0.42, 0.30) * 0.20);

            raw.put("sad",
                (1.0 - safeMin(sat, 0.35)) * 0.25 + (1.0 - safeMin(edg, 0.28)) * 0.25 +
                gaussianLike(bri, 0.38, 0.28) * 0.20 + safeMin(cool + neutralHue, 0.40) * 0.15 +
                (1.0 - isVivid) * 0.15);

            // Softmax with temperature
            double temperature = 0.35;
            double[] rawArr = new double[EMOTIONS.length];
            for (int i = 0; i < EMOTIONS.length; i++) {
                rawArr[i] = raw.getOrDefault(EMOTIONS[i], 0.0);
            }
            double[] probs = softmax(rawArr, temperature);

            Map<String, Double> scores = new LinkedHashMap<>();
            int bestIdx = 0;
            for (int i = 0; i < EMOTIONS.length; i++) {
                scores.put(EMOTION_LABELS_ZH.get(EMOTIONS[i]), Math.round(probs[i] * 1000.0) / 10.0);
                if (probs[i] > probs[bestIdx]) bestIdx = i;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("emotion", EMOTIONS[bestIdx]);
            result.put("emotion_zh", EMOTION_LABELS_ZH.get(EMOTIONS[bestIdx]));
            result.put("confidence", Math.round(probs[bestIdx] * 1000.0) / 10.0);
            result.put("comfort_advice", COMFORT_ADVICE.get(EMOTIONS[bestIdx]));
            result.put("scores", scores);
            return result;
        } catch (Exception e) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("emotion", "relaxed");
            fallback.put("emotion_zh", "放松");
            fallback.put("confidence", 50.0);
            fallback.put("comfort_advice", COMFORT_ADVICE.get("relaxed"));
            return fallback;
        }
    }

    private static double safeMin(double value, double threshold) {
        if (threshold <= 0) return 1.0;
        return Math.max(0.0, Math.min(1.0, value / threshold));
    }

    private static double gaussianLike(double value, double center, double width) {
        if (width <= 0) width = 0.1;
        double dist = Math.abs(value - center) / width;
        return Math.max(0.0, 1.0 - dist);
    }

    private static double[] softmax(double[] scores, double temperature) {
        double[] exp = new double[scores.length];
        double sum = 0;
        for (int i = 0; i < scores.length; i++) {
            exp[i] = Math.exp(scores[i] / temperature);
            sum += exp[i];
        }
        for (int i = 0; i < exp.length; i++) exp[i] /= sum;
        return exp;
    }
}
