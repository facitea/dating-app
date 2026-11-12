package com.dating.backend.matching;

import com.dating.backend.config.UploadProperties;
import com.dating.backend.service.PhotoCompression;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.file.*;
import java.io.*;
import java.util.UUID;
import javax.imageio.ImageIO;

@Service @RequiredArgsConstructor public class CommunityPhotos {
 private final UploadProperties properties;
 private Path root(){return Path.of(properties.dir()==null?"uploads":properties.dir()).toAbsolutePath().normalize().resolve("community");}
 private Path path(String key){if(key==null||!key.matches("[a-f0-9-]{36}\\.jpg"))throw new ResponseStatusException(HttpStatus.NOT_FOUND);return root().resolve(key);}
 public String store(MultipartFile file){
  if(file.isEmpty()||file.getSize()>2*1024*1024)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"사진을 압축한 뒤 다시 올려 주세요.");
  try(var in=ImageIO.createImageInputStream(file.getInputStream())){
   var readers=ImageIO.getImageReaders(in);if(!readers.hasNext())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"사진 파일을 확인해 주세요.");
   var reader=readers.next();byte[] bytes;
   try{reader.setInput(in);long pixels=(long)reader.getWidth(0)*reader.getHeight(0);if(pixels<=0||pixels>4_000_000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"사진을 압축한 뒤 다시 올려 주세요.");bytes=PhotoCompression.jpeg(reader.read(0),1200,.8f);}finally{reader.dispose();}
   Files.createDirectories(root());var key=UUID.randomUUID()+".jpg";Files.write(path(key),bytes,StandardOpenOption.CREATE_NEW);return key;
  }catch(IOException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"사진을 저장하지 못했어요.");}
 }
 public byte[] read(String key){try{return Files.readAllBytes(path(key));}catch(IOException e){throw new ResponseStatusException(HttpStatus.NOT_FOUND);}}
 public void remove(String key){if(key!=null)try{Files.deleteIfExists(path(key));}catch(IOException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Community photo cleanup failed");}}
}
