package com.dating.backend.service;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.*;import javax.imageio.*;
public final class PhotoCompression {
 private PhotoCompression(){}
 public static byte[] jpeg(BufferedImage source,int edge,float quality)throws IOException{
  double ratio=Math.min(1,(double)edge/Math.max(source.getWidth(),source.getHeight()));
  int w=Math.max(1,(int)Math.round(source.getWidth()*ratio)),h=Math.max(1,(int)Math.round(source.getHeight()*ratio));
  var scaled=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);var g=scaled.createGraphics();try{g.setColor(Color.WHITE);g.fillRect(0,0,w,h);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(source,0,0,w,h,null);}finally{g.dispose();}
  var bytes=new ByteArrayOutputStream();var writer=ImageIO.getImageWritersByFormatName("jpeg").next();
  try(var out=ImageIO.createImageOutputStream(bytes)){writer.setOutput(out);var params=writer.getDefaultWriteParam();params.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);params.setCompressionQuality(quality);writer.write(null,new IIOImage(scaled,null,null),params);}finally{writer.dispose();}
  return bytes.toByteArray();
 }
}
