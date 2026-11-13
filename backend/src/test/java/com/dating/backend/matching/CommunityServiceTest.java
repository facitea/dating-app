package com.dating.backend.matching;
import com.dating.backend.entity.User;import jakarta.persistence.EntityManager;import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.server.ResponseStatusException;import java.time.Instant;import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @Transactional class CommunityServiceTest {
 @Autowired CommunityService service;@Autowired EntityManager em;@Autowired TraitService traits;User a,b;
 @BeforeEach void setup(){a=User.builder().email("community-a@test.invalid").nickname("상담A").provider("LOCAL").status("ACTIVE").build();b=User.builder().email("community-b@test.invalid").nickname("상담B").provider("LOCAL").status("ACTIVE").build();em.persist(a);em.persist(b);}
 CommunityService.View post(){return service.create(a.getEmail(),new CommunityService.Write("ADVICE","고민입니다","어떻게 말하면 좋을까요?",null));}
 @Test void threadReplyOwnershipAndHiddenContent(){var p=post();assertEquals(1,service.list(b.getEmail(),"ALL",0).size());var reply=service.create(b.getEmail(),new CommunityService.Write(null,null,"차분하게 대화해 보세요",p.id()));assertEquals(1,service.thread(a.getEmail(),p.id()).comments().size());assertThrows(ResponseStatusException.class,()->service.delete(b.getEmail(),p.id()));service.delete(b.getEmail(),reply.id());assertTrue(service.thread(a.getEmail(),p.id()).comments().isEmpty());em.find(CommunityPost.class,p.id()).setHidden(true);assertTrue(service.list(b.getEmail(),"ALL",0).isEmpty());assertThrows(ResponseStatusException.class,()->service.thread(b.getEmail(),p.id()));}
 @Test void blockedAuthorsNotVisibleAndReportsAreDeduplicated(){var p=post();service.report(b.getEmail(),p.id(),"광고 의심");service.report(b.getEmail(),p.id(),"광고 의심");assertEquals(1L,em.createQuery("select count(r) from CommunityReport r",Long.class).getSingleResult());var v=new MemberVisibility();v.setOwnerId(a.getId());v.setTargetId(b.getId());v.setBlocked(true);em.persist(v);assertTrue(service.list(b.getEmail(),"ALL",0).isEmpty());assertThrows(ResponseStatusException.class,()->service.thread(b.getEmail(),p.id()));assertThrows(ResponseStatusException.class,()->service.report(b.getEmail(),p.id(),"test"));}
 @Test void rateLimitsAndNonActiveMembers(){post();assertThrows(ResponseStatusException.class,()->post());b.setStatus("SUSPENDED");assertThrows(ResponseStatusException.class,()->service.list(b.getEmail(),"ALL",0));}
 @Test void customHobbiesAreBoundedAndPreserved(){var raw="{\"answers\":{\"hobbies\":[\"수영\",\"천체 사진 촬영\"]}}";assertTrue(traits.normalize(raw).contains("천체 사진 촬영"));assertThrows(ResponseStatusException.class,()->traits.normalize("{\"answers\":{\"hobbies\":[\"<script>\"]}}"));assertThrows(ResponseStatusException.class,()->traits.normalize("{\"answers\":{\"hobbies\":[\"수영\",\"수영\"]}}"));}

 @Autowired CommunityPhotos photos;
 @Test void photosFollowPostVisibilityAndCannotBeFetchedAfterDeletion() throws Exception {
  var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(40,30,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
  var file=new org.springframework.mock.web.MockMultipartFile("photo","test.png","image/png",bytes.toByteArray());
  var p=service.createWithPhoto(a.getEmail(),new CommunityService.Write("DAILY","사진","사진 테스트",null),file);
  var entity=em.find(CommunityPost.class,p.id());var key=entity.getImageKey();
  try{
   assertEquals("/community/"+p.id()+"/photo",p.imageUrl());assertTrue(service.photo(b.getEmail(),p.id()).length>0);
   entity.setHidden(true);assertThrows(ResponseStatusException.class,()->service.photo(b.getEmail(),p.id()));entity.setHidden(false);
   var block=new MemberVisibility();block.setOwnerId(b.getId());block.setTargetId(a.getId());block.setBlocked(true);em.persist(block);
   assertThrows(ResponseStatusException.class,()->service.photo(b.getEmail(),p.id()));block.setBlocked(false);
   b.setStatus("SUSPENDED");assertThrows(ResponseStatusException.class,()->service.photo(b.getEmail(),p.id()));b.setStatus("ACTIVE");
   service.delete(a.getEmail(),p.id());assertThrows(ResponseStatusException.class,()->service.photo(b.getEmail(),p.id()));
  }finally{photos.remove(key);}
 }
 @Test void invalidImagesAndPathTraversalRejected(){
  assertThrows(ResponseStatusException.class,()->photos.store(new org.springframework.mock.web.MockMultipartFile("photo","a.jpg","image/jpeg","not an image".getBytes())));
  assertThrows(ResponseStatusException.class,()->photos.read("../../secret"));
 }

 @Test void ownPostFilterIsScopedToLoggedInMember(){var first=post();var second=service.create(b.getEmail(),new CommunityService.Write("DAILY","나의 글","일상",null));assertEquals(first.id(),service.list(a.getEmail(),"ALL",0,true).get(0).id());assertEquals(second.id(),service.list(b.getEmail(),"ALL",0,true).get(0).id());assertTrue(service.list(a.getEmail(),"DAILY",0,true).isEmpty());assertTrue(service.list(a.getEmail(),"ALL",first.id(),true).isEmpty());}
 @Test void commentsNotifyOnlyPostAuthorWithDeepLink(){var post=post();var reply=service.create(b.getEmail(),new CommunityService.Write(null,null,"새 댓글",post.id()));var notices=inbox.list(a.getEmail(),0);assertEquals(1,notices.size());assertEquals("COMMUNITY_COMMENT:"+reply.id(),notices.get(0).getEventKey());assertEquals("/community?post="+post.id(),notices.get(0).getLink());assertTrue(inbox.list(b.getEmail(),0).isEmpty());em.find(CommunityPost.class,post.id()).setCreatedAt(Instant.now().minusSeconds(20));service.create(a.getEmail(),new CommunityService.Write(null,null,"직접 쓴 댓글",post.id()));assertEquals(1,inbox.list(a.getEmail(),0).size());}
 @Autowired InboxService inbox;
}
