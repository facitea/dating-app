package com.dating.backend.service;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.mock.web.MockMultipartFile;import org.springframework.web.server.ResponseStatusException;import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest class FileStorageSecurityTest {
 @Autowired FileStorageService files;
 @Test void disguisedHtmlIsRejected(){var fake=new MockMultipartFile("file","photo.jpg","image/jpeg","<html><script>alert(1)</script></html>".getBytes());assertThrows(ResponseStatusException.class,()->files.storeProfileImage(999L,fake));}
 @Test void realPhotoIsReencoded()throws Exception{var image=new java.awt.image.BufferedImage(4,4,java.awt.image.BufferedImage.TYPE_INT_RGB);var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);String url=files.storeProfileImage(999L,new MockMultipartFile("file","photo.png","image/png",bytes.toByteArray()));var path=java.nio.file.Path.of("build/test-uploads",url.substring("/uploads/".length()));try{assertTrue(url.endsWith(".png"));assertEquals(4,javax.imageio.ImageIO.read(path.toFile()).getWidth());}finally{java.nio.file.Files.deleteIfExists(path);}}
}
