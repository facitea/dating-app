package com.dating.backend.matching;
import com.dating.backend.entity.User;import com.dating.backend.config.UploadProperties;import com.dating.backend.service.PhotoCompression;import jakarta.persistence.*;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;import java.nio.file.*;import java.time.*;import java.util.*;import java.io.*;import javax.imageio.ImageIO;
@Service @RequiredArgsConstructor public class ProfilePhotoHistoryService {
 private final EntityManager em;private final UploadProperties config;private final PrivateMediaService media;
 public record View(Long id,String reason,Instant archivedAt,Instant expiresAt,long bytes){}
 private Path root()throws IOException{var p=Path.of(config.dir()==null?"uploads":config.dir()).toAbsolutePath().normalize().resolve("photo-history");Files.createDirectories(p);return p.toRealPath();}
 private Path file(String name)throws IOException{if(name==null||!name.matches("[a-f0-9-]{36}\\.jpg"))throw new IOException("Invalid archive path");return root().resolve(name);}
 private boolean deleted(Long id){var u=em.find(User.class,id);return u==null||"DELETED".equals(u.getStatus());}
 @Transactional public void archive(Long userId,String url,String reason){
  if(url==null||url.isBlank())return;
  em.find(User.class,userId,LockModeType.PESSIMISTIC_WRITE);
  if(em.createQuery("select count(h) from ProfilePhotoHistory h where h.userId=:u and h.sourceUrl=:s and h.purgedAt is null",Long.class).setParameter("u",userId).setParameter("s",url).getSingleResult()>0)return;
  String name=null;
  try{if(!url.startsWith("/uploads/"))throw new IOException("Invalid source");var image=ImageIO.read(media.path(url.substring(9)).toFile());if(image==null)throw new IOException("Unreadable source");var bytes=PhotoCompression.jpeg(image,960,.82f);name=UUID.randomUUID()+".jpg";Files.write(file(name),bytes,StandardOpenOption.CREATE_NEW);
   var row=new ProfilePhotoHistory();row.setUserId(userId);row.setSourceUrl(url);row.setFilename(name);row.setReason(reason);row.setBytes(bytes.length);row.setExpiresAt(Instant.now().plus(Duration.ofDays(90)));em.persist(row);
   final String saved=name;org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(file(saved));}catch(IOException ignored){}}});
  }catch(IOException e){if(name!=null)try{Files.deleteIfExists(file(name));}catch(IOException ignored){}throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"이전 사진 보관에 실패했습니다. 잠시 후 다시 시도해 주세요.");}
 }
 @Transactional(readOnly=true) public List<View> list(Long userId,int page){if(deleted(userId))return List.of();return em.createQuery("select h from ProfilePhotoHistory h where h.userId=:u and h.purgedAt is null and h.expiresAt>:now order by h.id desc",ProfilePhotoHistory.class).setParameter("u",userId).setParameter("now",Instant.now()).setFirstResult(Math.max(0,Math.min(page,10000))*20).setMaxResults(20).getResultList().stream().map(h->new View(h.getId(),h.getReason(),h.getArchivedAt(),h.getExpiresAt(),h.getBytes())).toList();}
 @Transactional(readOnly=true) public byte[] photo(Long id){var h=em.find(ProfilePhotoHistory.class,id);if(h==null||deleted(h.getUserId())||h.getPurgedAt()!=null||!h.getExpiresAt().isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.NOT_FOUND);try{return Files.readAllBytes(file(h.getFilename()));}catch(IOException e){throw new ResponseStatusException(HttpStatus.NOT_FOUND);}}
 @Transactional public void withdraw(Long userId){
  var rows=em.createQuery("select h from ProfilePhotoHistory h where h.userId=:u and h.purgedAt is null",ProfilePhotoHistory.class).setParameter("u",userId).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
  var names=new ArrayList<String>();for(var h:rows){h.setExpiresAt(Instant.now());names.add(h.getFilename());}
  org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){public void afterCommit(){for(String name:names)try{Files.deleteIfExists(file(name));}catch(IOException ignored){/* scheduled purge retries */}}});
 }
 @Scheduled(fixedDelay=3600000) @Transactional public void purge(){
  // Remove retired source files only after neither current nor approved profiles reference them.
  var orphaned=em.createQuery("select h from ProfilePhotoHistory h where h.sourceCleaned=false and not exists (select i.id from UserProfileImage i where i.imageUrl=h.sourceUrl) and not exists (select p.userId from ApprovedProfile p join p.images img where img=h.sourceUrl)",ProfilePhotoHistory.class).setMaxResults(100).getResultList();
  for(var h:orphaned){em.find(User.class,h.getUserId(),LockModeType.PESSIMISTIC_WRITE);long current=em.createQuery("select count(i) from UserProfileImage i where i.imageUrl=:url",Long.class).setParameter("url",h.getSourceUrl()).getSingleResult();long approved=em.createQuery("select count(p) from ApprovedProfile p join p.images img where img=:url",Long.class).setParameter("url",h.getSourceUrl()).getSingleResult();if(current>0||approved>0)continue;if(h.getSourceUrl()!=null&&h.getSourceUrl().startsWith("/uploads/profile/")){try{Files.deleteIfExists(media.path(h.getSourceUrl().substring(9)));}catch(ResponseStatusException missing){if(missing.getStatusCode().value()!=404)throw missing;}catch(IOException e){throw new IllegalStateException("Retired photo cleanup failed",e);}}h.setSourceCleaned(true);}
  var rows=em.createQuery("select h from ProfilePhotoHistory h where h.purgedAt is null and (h.expiresAt<=:now or h.userId in (select u.id from User u where u.status='DELETED'))",ProfilePhotoHistory.class).setParameter("now",Instant.now()).setMaxResults(100).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
  for(var h:rows)try{Files.deleteIfExists(file(h.getFilename()));h.setFilename(null);h.setPurgedAt(Instant.now());}catch(IOException e){throw new IllegalStateException("Photo history purge failed",e);}
 }
}
