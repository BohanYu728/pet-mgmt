package com.petmgmt.ai;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Breed recognition using color analysis and pattern heuristics.
 * Simplified from PyTorch EfficientNet/ResNet — uses rule-based color/edge analysis.
 */
public class BreedModel {

    /** Analyze cat coat color and pattern from image. */
    public static Map<String, Object> analyzeCatColor(String imagePath) {
        try {
            BufferedImage img = ImageUtils.loadImage(imagePath, 224, 224);
            Map<String, Object> hsv = ImageUtils.extractHsvFeatures(img);
            Map<String, Object> edge = ImageUtils.extractEdgeFeatures(img);

            double[] hueHist = (double[]) hsv.get("hue_histogram");
            double sat = (Double) hsv.get("mean_saturation") * 100;
            double bri = (Double) hsv.get("mean_brightness") * 100;
            double edgeDensity = (Double) edge.get("edge_density");

            // Calculate color variance for pattern detection
            double colorVariance = calculateColorVariance(img);

            boolean isTabby = edgeDensity > 0.35 && colorVariance > 40;

            // Center vs border for pointed pattern
            double borderMean = calculateBorderDifference(img);
            boolean isPointed = Math.abs(borderMean) > 15;
            boolean isMulticolor = colorVariance > 60;

            int dominantHue = 0;
            for (int i = 1; i < 6; i++) {
                if (hueHist[i] > hueHist[dominantHue]) dominantHue = i;
            }

            String baseColor, colorCn;
            if (hueHist[0] > 0.18 && sat > 35) {
                baseColor = "orange"; colorCn = "橘色";
            } else if (hueHist[1] > 0.12 && sat > 30 && bri > 35) {
                baseColor = "brown"; colorCn = "棕色";
            } else if (bri < 65 && sat < 40) {
                baseColor = "black"; colorCn = "黑色";
            } else if (sat < 35 && hueHist[4] > 0.05) {
                baseColor = "blue-gray"; colorCn = "蓝灰色";
            } else if (sat < 25) {
                baseColor = bri > 155 ? "white" : "blue-gray";
                colorCn = bri > 155 ? "白色" : "蓝灰色";
            } else {
                baseColor = "mixed"; colorCn = "混合色";
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("base_color", baseColor);
            result.put("color_cn", colorCn);
            result.put("saturation", sat);
            result.put("brightness", bri);
            result.put("is_dark", bri < 40);
            result.put("is_light", bri > 160);
            result.put("is_gray", "blue-gray".equals(baseColor));
            result.put("is_orange", "orange".equals(baseColor));
            result.put("is_tabby", isTabby);
            result.put("is_pointed", isPointed);
            result.put("is_multicolor", isMulticolor);
            result.put("edge_density", edgeDensity);
            result.put("color_variance", colorVariance);
            return result;
        } catch (Exception e) {
            return defaultColorInfo();
        }
    }

    private static Map<String, Object> defaultColorInfo() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("base_color", "unknown"); map.put("color_cn", "未知");
        map.put("saturation", 0.0); map.put("brightness", 100.0);
        map.put("is_dark", false); map.put("is_light", false);
        map.put("is_gray", false); map.put("is_orange", false);
        map.put("is_tabby", false); map.put("is_pointed", false);
        map.put("is_multicolor", false);
        map.put("edge_density", 0.0); map.put("color_variance", 0.0);
        return map;
    }

    /** Generate cat breed result from color/pattern analysis. */
    public static String[] catFromColorOnly(Map<String, Object> ci) {
        boolean isOrange = (Boolean) ci.get("is_orange");
        boolean isTabby = (Boolean) ci.get("is_tabby");
        boolean isPointed = (Boolean) ci.get("is_pointed");
        boolean isLight = (Boolean) ci.get("is_light");
        boolean isGray = (Boolean) ci.get("is_gray");
        boolean isDark = (Boolean) ci.get("is_dark");
        boolean isMulticolor = (Boolean) ci.get("is_multicolor");
        double saturation = ((Number) ci.get("saturation")).doubleValue();

        if (isOrange && isTabby) {
            return new String[]{"橘猫/黄虎斑（推测）",
                "橘色虎斑纹，中华田园猫最经典毛色。性格活泼粘人食量大。",
                "1. 极易发胖需控制食量\n2. 定期梳毛减少毛球\n3. 性格好适合家庭饲养"};
        }
        if (isOrange) {
            return new String[]{"橘猫/奶油色猫（推测）",
                "橘色或奶油色被毛，性格温柔粘人。",
                "1. 注意控制饮食防肥胖\n2. 短毛梳理方便\n3. 性格温顺适合陪伴"};
        }
        if (isPointed && isLight) {
            return new String[]{"布偶猫/重点色猫（推测）",
                "重点色花纹，蓝眼长毛体型大。性格温顺粘人。",
                "1. 长毛需每日梳理\n2. 室内猫不适合外出\n3. 性格粘人需多陪伴"};
        }
        if (isGray && !isTabby) {
            return new String[]{"英国短毛猫/蓝猫（推测）",
                "灰蓝色短毛，圆脸铜色大眼，体态敦实。性格沉稳安静。",
                "1. 短毛易打理\n2. 易患心肌病需定期体检\n3. 容易发胖需控制饮食"};
        }
        if (isGray) {
            return new String[]{"美国短毛猫/银虎斑（推测）",
                "银色虎斑纹短毛，体型匀称肌肉发达。性格活泼友善。",
                "1. 短毛易打理\n2. 需适量运动\n3. 口腔健康需关注"};
        }
        if (isTabby) {
            return new String[]{"虎斑猫/狸花猫（推测）",
                "条纹或斑纹被毛，聪明机警善于捕猎。身体素质好。",
                "1. 性格独立活动力强\n2. 捕猎本能强\n3. 体质好较少生病"};
        }
        if (isDark) {
            return new String[]{"黑猫/孟买猫风格（推测）",
                "全黑被毛神秘优雅。性格独立聪明。",
                "1. 暗处注意安全\n2. 被毛短易打理\n3. 免疫系统通常较强"};
        }
        if (isLight && saturation < 25) {
            return new String[]{"白猫（推测）",
                "纯白被毛优雅高贵。蓝眼白猫约60-80%先天性耳聋。",
                "1. 蓝眼需做耳聋筛查\n2. 防晒避免皮肤癌\n3. 白毛需勤清洁"};
        }
        if (isMulticolor) {
            return new String[]{"三花猫/玳瑁猫（推测）",
                "多色混合，几乎全是母猫。性格多变独特。",
                "1. 被毛漂亮需梳理\n2. 性格独特需耐心\n3. 通常健康长寿"};
        }
        return new String[]{"中华田园猫/混血猫（推测）",
            "非特定纯种猫，混血猫通常更健康长寿。",
            "1. 根据毛长短定期梳理\n2. 体质通常较好\n3. 定期体检疫苗不可少"};
    }

    private static double calculateColorVariance(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        double sumR = 0, sumG = 0, sumB = 0, sumR2 = 0, sumG2 = 0, sumB2 = 0;
        int total = w * h;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = new Color(img.getRGB(x, y));
                sumR += c.getRed(); sumG += c.getGreen(); sumB += c.getBlue();
                sumR2 += c.getRed() * c.getRed();
                sumG2 += c.getGreen() * c.getGreen();
                sumB2 += c.getBlue() * c.getBlue();
            }
        }
        double varR = sumR2 / total - (sumR / total) * (sumR / total);
        double varG = sumG2 / total - (sumG / total) * (sumG / total);
        double varB = sumB2 / total - (sumB / total) * (sumB / total);
        return Math.sqrt((varR + varG + varB) / 3.0);
    }

    private static double calculateBorderDifference(BufferedImage img) {
        int w = img.getWidth() / 2, h = img.getHeight() / 2;
        int cw = w / 2, ch = h / 2;
        double centerMean = 0, borderMean = 0;
        int cc = 0, bc = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int gray = new Color(img.getRGB(x, y)).getRed();
                if (x > cw / 2 && x < w - cw / 2 && y > ch / 2 && y < h - ch / 2) {
                    centerMean += gray; cc++;
                } else {
                    borderMean += gray; bc++;
                }
            }
        }
        if (cc == 0 || bc == 0) return 0;
        return borderMean / bc - centerMean / cc;
    }
}
