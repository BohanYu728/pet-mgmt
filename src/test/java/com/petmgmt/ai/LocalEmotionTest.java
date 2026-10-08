package com.petmgmt.ai;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
class LocalEmotionTest {
 @Test void huskyWithPersonStillProducesVisualCandidates() {
  var result=new EmotionModel().predict("src/test/resources/ai/husky-with-person.jpg");
  System.out.println("Husky inference: "+result);
  assertEquals("dog",result.get("species"));
  assertEquals(6,((List<?>)result.get("candidates")).size());
 }
 @Test void catAndDogImagesProduceLocalVisualCandidatesInsteadOfFixedMessage() {
  for(String file:List.of("cat-lifestyle.jpg","border-collie-sample.jpg")) {
   var result=new EmotionModel().predict("frontend/public/images/"+file);
   assertEquals("local_clip_zero_shot",result.get("analysis_mode"));
   assertEquals(6,((List<?>)result.get("candidates")).size());
   assertNull(result.get("confidence"));
   assertTrue(result.get("comfort_advice").toString().contains("推测"));
  }
 }
}
