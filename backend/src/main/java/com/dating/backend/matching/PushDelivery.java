package com.dating.backend.matching;
import jakarta.annotation.PostConstruct;import org.springframework.stereotype.Component;import org.springframework.beans.factory.annotation.Value;
import nl.martijndwars.webpush.*;import org.bouncycastle.jce.provider.BouncyCastleProvider;import java.security.Security;import java.nio.charset.StandardCharsets;import java.util.concurrent.TimeUnit;
@Component public class PushDelivery {
 @Value("${app.push.public-key:}") private String publicKey;
 @Value("${app.push.private-key:}") private String privateKey;
 @Value("${app.push.subject:mailto:admin@example.com}") private String subject;
 private nl.martijndwars.webpush.PushService service;
 @PostConstruct void init()throws Exception{if(!publicKey.isBlank()&&!privateKey.isBlank()){Security.addProvider(new BouncyCastleProvider());service=new nl.martijndwars.webpush.PushService(publicKey,privateKey,subject);}}
 public String key(){return publicKey;}
 public boolean ready(){return service!=null;}
 public int send(PushDevice d)throws Exception{
  // Keep message and report details off lock screens; the authenticated inbox contains the content.
  var n=new Notification(d.getEndpoint(),d.getP256dh(),d.getAuth(),"{\"title\":\"새 알림\",\"body\":\"앱에서 새로운 소식을 확인해 주세요.\",\"url\":\"/notifications\"}".getBytes(StandardCharsets.UTF_8));
  var future=service.sendAsync(n);try{return future.get(10,TimeUnit.SECONDS).getStatusLine().getStatusCode();}finally{if(!future.isDone())future.cancel(true);}
 }
}
