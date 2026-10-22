package com.dating.backend.matching;
import com.dating.backend.entity.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import javax.sql.DataSource;
import java.time.*;
import java.util.*;

@Service @Profile("demo-seed") @RequiredArgsConstructor
public class DemoSeedService {
 private final EntityManager em; private final PasswordEncoder encoder; private final DataSource source;
 @Transactional public int seed() throws Exception {
  try(var connection=source.getConnection()){if(!"dating_test".equals(connection.getCatalog()))throw new IllegalStateException("Demo seed is restricted to dating_test database.");}
  String hash=encoder.encode("Demo1234!");int created=0;
  String[] regions={"서울 강남구","서울 강남구","서울 마포구","서울 송파구","경기 성남시","부산 해운대구"};
  String[] mbtis={"ESFJ","ENFP","INTJ","ISFP","INFJ","ENTP","ISTJ","INFP","ESTJ","ESFP","ISFJ","INTP","ENFJ","ISTP","ESTP","ENTJ"};
  String[] jobs={"디자이너","개발자","회사원","교사","연구원","기획자"};
  String[] hobbies={"주말에는 산책과 카페 탐방을 즐겨요.","음악과 영화를 좋아해요.","여행하며 새로운 풍경을 보는 걸 좋아해요.","운동을 하고 맛있는 음식을 함께 먹고 싶어요."};
  for(String gender:List.of("MALE","FEMALE"))for(int i=1;i<=24;i++){
   String tag=(gender.equals("MALE")?"m":"f")+String.format("%02d",i), email="demo."+tag+"@dating.test";
   if(em.createQuery("select count(u) from User u where u.email=:email",Long.class).setParameter("email",email).getSingleResult()>0)continue;
   var u=User.builder().email(email).nickname("테스트_"+(gender.equals("MALE")?"남":"여")+String.format("%02d",i)).provider("LOCAL").password(hash).status("ACTIVE").profileCompletedAt(LocalDateTime.now()).adminMemo("Local demo account; no real member").build();em.persist(u);
   em.persist(UserProfile.builder().userId(u.getId()).region(regions[(i-1)%regions.length]).job(jobs[(i-1)%jobs.length]).mbti(mbtis[(i-1)%mbtis.length]).personality("편안하게 대화하는 편이에요.").idealType("서로 존중하며 취향을 나눌 수 있는 사람").introduction("테스트용 프로필입니다. "+hobbies[(i-1)%hobbies.length]).smokingStatus(new String[]{"NON_SMOKER","NON_SMOKER","SMOKER","OCCASIONAL"}[(i-1)%4]).drinkingStatus(new String[]{"NONE","SOMETIMES","OFTEN"}[(i-1)%3]).religion(new String[]{"NONE","CHRISTIAN","CATHOLIC","BUDDHIST","OTHER"}[(i-1)%5]).build());
   em.persist(UserVerification.builder().userId(u.getId()).gender(gender).birthDate(LocalDate.now().minusYears(24+(i-1)%17).minusDays(i)).isVerified(true).verifiedAt(LocalDateTime.now()).build());
   em.persist(UserProfileImage.builder().userId(u.getId()).imageOrder(0).isMain(true).imageUrl("/uploads/demo/"+tag+".jpg").build());
   var wallet=new CreditWallet();wallet.setUserId(u.getId());wallet.setBalance(100);em.persist(wallet);
   var entry=new CreditEntry();entry.setUserId(u.getId());entry.setAmount(100);entry.setReferenceKey("DEMO_GRANT");em.persist(entry);created++;
  }
  return created;
 }
}
