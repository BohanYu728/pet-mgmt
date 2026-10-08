package com.petmgmt.ai;
import java.util.Map;
/** Local image/text similarity inference for cat and dog state candidates. */
public class EmotionModel {
    public Map<String,Object> predict(String imagePath) {
        return new LocalEmotionClassifier().predict(imagePath);
    }
}
