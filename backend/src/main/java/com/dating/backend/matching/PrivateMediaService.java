package com.dating.backend.matching;
import com.dating.backend.entity.*;import com.dating.backend.config.UploadProperties;
import jakarta.persistence.*;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;import java.nio.file.*;import java.util.*;
@Service @RequiredArgsConstructor public class PrivateMediaService {
 private final EntityManager em;private final SocialService social;private final UploadProperties config;
 public Path path(String relative){try{if(relative==null||!relative.matches("(?:profile|demo)/[a-zA-Z0-9_.-]+\\.(?:jpg|jpeg|png|webp)"))throw denied();var root=Path.of(config.dir()==null?"uploads":config.dir()).toRealPath();var file=root.resolve(relative).normalize();if(!file.startsWith(root)||!Files.isRegularFile(file)||!file.toRealPath().startsWith(root))throw denied();return file;}catch(java.io.IOException e){throw denied();}}
 private ResponseStatusException denied(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"사진을 찾을 수 없습니다.");}
 @Transactional(readOnly=true) public Path read(String email,String relative){
  var file=path(relative);String url="/uploads/"+relative;
  var me=em.createQuery("select u from User u where u.email=:e",User.class).setParameter("e",email).getResultStream().findFirst().orElseThrow(this::denied);
  if(Set.of("DELETED","SUSPENDED").contains(me.getStatus()))throw denied();
  var owners=new HashSet<Long>(em.createQuery("select i.userId from UserProfileImage i where i.imageUrl=:url",Long.class).setParameter("url",url).getResultList());
  owners.addAll(em.createQuery("select p.userId from ApprovedProfile p join p.images i where i=:url",Long.class).setParameter("url",url).getResultList());
  if(owners.contains(me.getId()))return file;
  for(Long owner:owners)try{if(social.profile(email,owner).images().contains(url))return file;}catch(ResponseStatusException denied){}
  throw denied();
 }
 @Transactional(readOnly=true) public void validateOwnedUpload(Long id,String url){
  if(url==null||!url.startsWith("/uploads/"))throw denied();
  boolean owned=em.createQuery("select count(i) from UserProfileImage i where i.userId=:id and i.imageUrl=:url",Long.class).setParameter("id",id).setParameter("url",url).getSingleResult()>0;
  if(!owned&&!url.matches("/uploads/profile/user-"+id+"-[a-fA-F0-9-]{36}\\.(jpg|jpeg|png|webp)"))throw denied();
  path(url.substring("/uploads/".length()));
 }
}
