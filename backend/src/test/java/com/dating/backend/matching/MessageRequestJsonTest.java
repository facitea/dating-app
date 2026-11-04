package com.dating.backend.matching;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
class MessageRequestJsonTest {
 @Test void optionalAcknowledgementDefaultsToFalseForBothRequests() {
  var mapper=JsonMapper.builder().build();
  for(String suffix:new String[]{"",",\"safetyAcknowledged\":null",",\"safetyAcknowledged\":false",",\"safetyAcknowledged\":true"}) {
   String json="{\"text\":\"hello\",\"clientId\":\"test\""+suffix+"}";
   boolean expected=suffix.endsWith("true");
   assertEquals(expected,mapper.readValue(json,SocialService.SendRequest.class).safetyAcknowledged());
   assertEquals(expected,mapper.readValue(json,SocialService.DirectRequest.class).safetyAcknowledged());
  }
 }
}
