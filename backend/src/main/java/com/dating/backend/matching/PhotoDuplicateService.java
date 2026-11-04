package com.dating.backend.matching;
import com.dating.backend.entity.*;import jakarta.persistence.*;import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;
import java.util.*;import java.time.Instant;import java.nio.file.*;import java.security.*;import java.awt.image.BufferedImage;import javax.imageio.ImageIO;
@Service @RequiredArgsConstructor public class PhotoDuplicateService {
 private final EntityManager em;private final PrivateMediaService media;
 // Current photos only. Deletions/withdrawals immediately disappear from results.
 private static final String LIVE="exists (select i.id from UserProfileImage i, User u where i.id=f.photoId and i.imageUrl=f.url and i.userId=u.id and u.status<>'DELETED')";
 public record Member(Long id,String nickname,String status,String photo){}
 public record View(Long id,Member left,Member right,boolean exact,int distance,boolean reviewed,Instant detectedAt){}
 public static long visualHash(BufferedImage image){
  var small=new BufferedImage(9,8,BufferedImage.TYPE_BYTE_GRAY);var g=small.createGraphics();g.drawImage(image,0,0,9,8,null);g.dispose();long bits=0;
  for(int y=0;y<8;y++)for(int x=0;x<8;x++)if(small.getRaster().getSample(x,y,0)>small.getRaster().getSample(x+1,y,0))bits|=1L<<(y*8+x);
  return bits;
 }
 public static boolean similar(long a,long b){return Long.bitCount(a)>=12&&Long.bitCount(a)<=52&&Long.bitCount(b)>=12&&Long.bitCount(b)<=52&&Long.bitCount(a^b)<=3;}
 @Scheduled(initialDelay=45000,fixedDelay=60000) @Transactional public void scan(){
  var stale=em.createQuery("select f from PhotoFingerprint f where not ("+LIVE+")",PhotoFingerprint.class).setMaxResults(100).getResultList();
  for(var f:stale){em.createQuery("delete from PhotoDuplicate p where p.leftId=:id or p.rightId=:id").setParameter("id",f.getId()).executeUpdate();em.remove(f);}em.flush();
  var photos=em.createQuery("select i from UserProfileImage i, User u where i.userId=u.id and u.status<>'DELETED' and not exists (select f.id from PhotoFingerprint f where f.photoId=i.id) order by i.id",UserProfileImage.class).setMaxResults(10).getResultList();
  for(var p:photos){var f=new PhotoFingerprint();f.setPhotoId(p.getId());f.setUserId(p.getUserId());f.setUrl(p.getImageUrl());
   try{
    if(!p.getImageUrl().startsWith("/uploads/"))throw new IllegalArgumentException();var path=media.path(p.getImageUrl().substring(9));
    if(Files.size(path)>10*1024*1024)throw new IllegalArgumentException();
    var digest=MessageDigest.getInstance("SHA-256");try(var in=Files.newInputStream(path)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)digest.update(buffer,0,n);}f.setSha(HexFormat.of().formatHex(digest.digest()));
    // Decoder subsampling keeps even legacy images bounded in memory.
    try(var input=ImageIO.createImageInputStream(path.toFile())){var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IllegalArgumentException();var reader=readers.next();try{reader.setInput(input);int w=reader.getWidth(0),h=reader.getHeight(0);if((long)w*h>20_000_000)throw new IllegalArgumentException();var param=reader.getDefaultReadParam();int sub=Math.max(1,Math.max(w,h)/128);param.setSourceSubsampling(sub,sub,0,0);f.setVisual(visualHash(reader.read(0,param)));}finally{reader.dispose();}}
    f.setInformative(Long.bitCount(f.getVisual())>=12&&Long.bitCount(f.getVisual())<=52);f.setBand0((int)(f.getVisual()&65535));f.setBand1((int)((f.getVisual()>>>16)&65535));f.setBand2((int)((f.getVisual()>>>32)&65535));f.setBand3((int)((f.getVisual()>>>48)&65535));
   }catch(Exception ignored){f.setSha(null);f.setInformative(false);}
   em.persist(f);em.flush();if(f.getSha()==null)continue;
   var candidates=new LinkedHashMap<Long,PhotoFingerprint>();
   add(candidates,em.createQuery("select f from PhotoFingerprint f where f.userId<>:u and f.sha=:sha and "+LIVE+" order by f.id",PhotoFingerprint.class).setParameter("u",f.getUserId()).setParameter("sha",f.getSha()).setMaxResults(200).getResultList());
   if(f.isInformative())for(int band=0;band<4;band++)add(candidates,em.createQuery("select f from PhotoFingerprint f where f.userId<>:u and f.informative=true and f.band"+band+"=:b and "+LIVE+" order by f.id",PhotoFingerprint.class).setParameter("u",f.getUserId()).setParameter("b",(int)((f.getVisual()>>>(band*16))&65535)).setMaxResults(200).getResultList());
   for(var other:candidates.values()){boolean exact=f.getSha().equals(other.getSha());if(!exact&&!similar(f.getVisual(),other.getVisual()))continue;var pair=new PhotoDuplicate();pair.setLeftId(other.getId());pair.setRightId(f.getId());pair.setExact(exact);pair.setDistance(Long.bitCount(f.getVisual()^other.getVisual()));em.persist(pair);}
  }
 }
 private void add(Map<Long,PhotoFingerprint> map,List<PhotoFingerprint> rows){for(var f:rows)map.put(f.getId(),f);}
 private PhotoFingerprint live(Long id){return em.createQuery("select f from PhotoFingerprint f where f.id=:id and "+LIVE,PhotoFingerprint.class).setParameter("id",id).getResultStream().findFirst().orElse(null);}
 private Member member(PhotoFingerprint f){var u=em.find(User.class,f.getUserId());return new Member(u.getId(),u.getNickname(),u.getStatus(),f.getUrl());}
 @Transactional(readOnly=true) public List<View> list(boolean reviewed,int page){
  var pairs=em.createQuery("select p from PhotoDuplicate p where p.reviewed=:r and exists (select f.id from PhotoFingerprint f where f.id=p.leftId and "+LIVE+") and exists (select f.id from PhotoFingerprint f where f.id=p.rightId and "+LIVE+") order by p.id desc",PhotoDuplicate.class).setParameter("r",reviewed).setFirstResult(Math.min(10000,Math.max(0,page))*20).setMaxResults(20).getResultList();
  var result=new ArrayList<View>();for(var p:pairs){var a=live(p.getLeftId());var b=live(p.getRightId());if(a!=null&&b!=null)result.add(new View(p.getId(),member(a),member(b),p.isExact(),p.getDistance(),p.isReviewed(),p.getDetectedAt()));}return result;
 }
 @Transactional public void review(Long id,boolean reviewed){var p=em.find(PhotoDuplicate.class,id);if(p==null||live(p.getLeftId())==null||live(p.getRightId())==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);p.setReviewed(reviewed);p.setReviewedAt(reviewed?Instant.now():null);}
}
