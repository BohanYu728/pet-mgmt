package com.petmgmt.ai;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Image processing utilities — HSV feature extraction, edge detection, validation.
 * Translated from Python PIL + numpy implementation.
 */
public class ImageUtils {

    public static BufferedImage loadImage(String imagePath, int targetWidth, int targetHeight) throws IOException {
        BufferedImage img = ImageIO.read(new File(imagePath));
        if (img == null) throw new IOException("Cannot read image: " + imagePath);

        BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.createGraphics().drawImage(img, 0, 0, null);

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(rgb, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resized;
    }

    public static Map<String, Object> extractHsvFeatures(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        double[] hueHist = new double[6];
        double sumSat = 0, sumBri = 0;
        int totalPixels = w * h;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                float[] hsv = rgbToHsv(rgb);
                int bin = Math.min(5, (int) (hsv[0] * 6));
                hueHist[bin]++;
                sumSat += hsv[1];
                sumBri += hsv[2];
            }
        }

        for (int i = 0; i < 6; i++) hueHist[i] /= totalPixels;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mean_saturation", sumSat / totalPixels);
        result.put("mean_brightness", sumBri / totalPixels);
        result.put("hue_histogram", hueHist);
        return result;
    }

    public static Map<String, Object> extractEdgeFeatures(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        double sum = 0, sumSq = 0;
        int total = w * h;
        int edgeCount = 0;
        double sumLaplacian = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int gray = new Color(img.getRGB(x, y)).getRed();
                sum += gray;
                sumSq += gray * gray;
                if (x < w - 1 && y < h - 1) {
                    int gx = Math.abs(gray - new Color(img.getRGB(x + 1, y)).getRed());
                    int gy = Math.abs(gray - new Color(img.getRGB(x, y + 1)).getRed());
                    double grad = Math.sqrt(gx * gx + gy * gy);
                    if (grad > 30) edgeCount++;
                    // Laplacian approximation
                    int g00 = gray, g10 = new Color(img.getRGB(Math.min(x + 1, w - 1), y)).getRed();
                    int g01 = new Color(img.getRGB(x, Math.min(y + 1, h - 1))).getRed();
                    sumLaplacian += Math.abs(4 * g00 - g10 - g01 - new Color(img.getRGB(Math.max(x - 1, 0), y)).getRed()
                            - new Color(img.getRGB(x, Math.max(y - 1, 0))).getRed());
                }
            }
        }

        double mean = sum / total;
        double variance = sumSq / total - mean * mean;
        double contrast = Math.sqrt(variance) / 255.0;
        double edgeDensity = (double) edgeCount / total;
        double blurScore = sumLaplacian / total;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contrast", contrast);
        result.put("edge_density", edgeDensity);
        result.put("blur_score", blurScore);
        return result;
    }

    public static Map<String, Object> validateImage(String imagePath) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            BufferedImage img = ImageIO.read(new File(imagePath));
            if (img == null) {
                result.put("valid", false);
                result.put("error", "无法读取图片");
                return result;
            }
            int w = img.getWidth(), h = img.getHeight();
            if (w < 64 || h < 64) {
                result.put("valid", false);
                result.put("error", "图片尺寸过小，至少需要 64x64");
                return result;
            }
            if (w > 4096 || h > 4096) {
                result.put("valid", false);
                result.put("error", "图片尺寸过大，不超过 4096x4096");
                return result;
            }
            result.put("valid", true);
            result.put("error", null);
        } catch (IOException e) {
            result.put("valid", false);
            result.put("error", "图片读取失败: " + e.getMessage());
        }
        return result;
    }

    private static float[] rgbToHsv(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        float[] hsv = new float[3];
        Color.RGBtoHSB(r, g, b, hsv);
        return hsv;
    }
}
