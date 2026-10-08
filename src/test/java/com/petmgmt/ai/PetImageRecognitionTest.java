package com.petmgmt.ai;

import com.petmgmt.service.AiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PetImageRecognitionTest {
    @TempDir Path temp;

    @Test void dogPhotoMustNotBeClassifiedByCatColorRules() {
        var service = new AiService(null, null, null, null, null);
        var result = service.predictBreed("frontend/public/images/dog-lifestyle.jpg");
        assertEquals("dog", result.get("species"));
        assertFalse(result.get("name_zh").toString().contains("猫"));
        assertNotEquals(75.0, result.get("confidence"));
    }

    @Test void corruptImageMustNotProduceABreedOrAnEmotion() throws Exception {
        Path invalid = temp.resolve("invalid.jpg");
        Files.writeString(invalid, "not an image");
        var service = new AiService(null, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.predictBreed(invalid.toString()));
        assertThrows(IllegalArgumentException.class, () -> service.predictEmotion(invalid.toString()));
    }

    @Test void stillPhotoMustNotClaimCalibratedEmotionalConfidence() {
        var result = new EmotionModel().predict("frontend/public/images/cat-lifestyle.jpg");
        assertNull(result.get("confidence"));
        assertTrue(result.get("comfort_advice").toString().contains("观察"));
    }

    @Test void catPhotoStillRecognizesCat() {
        assertEquals("cat", new PetImageClassifier().predict("frontend/public/images/cat-lifestyle.jpg").get("species"));
    }

    @Test void borderColliePhotoIsADogWithBorderCollieCandidate() {
        var result = new PetImageClassifier().predict("frontend/public/images/border-collie-sample.jpg");
        assertEquals("dog", result.get("species"));
        assertTrue(result.get("name_zh").toString().contains("边境牧羊犬"));
    }

    @Test void blankImageDoesNotForceAPetClassification() throws Exception {
        var blank = new java.awt.image.BufferedImage(224,224,java.awt.image.BufferedImage.TYPE_INT_RGB);
        Path path = temp.resolve("blank.png");
        javax.imageio.ImageIO.write(blank,"png",path.toFile());
        var result = new PetImageClassifier().predict(path.toString());
        assertEquals("unknown",result.get("species"));
        assertNull(result.get("confidence"));
    }
}
