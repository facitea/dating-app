package com.dating.backend.admin;
import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.IOException;import java.util.*;
import org.springframework.web.filter.OncePerRequestFilter;import org.springframework.security.core.context.SecurityContextHolder;import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.core.authority.SimpleGrantedAuthority;
public class AdminFilter extends OncePerRequestFilter {
 private final AdminService admins;public AdminFilter(AdminService admins){this.admins=admins;}
 public static String token(HttpServletRequest r){if(r.getCookies()!=null)for(var c:r.getCookies())if(c.getName().equals("dating_admin"))return c.getValue();return null;}
 public static boolean allowed(String role,String method,String path){
  if(path.equals("/api/admin/session")||path.startsWith("/api/admin/session/"))return true;
  if(role.equals("OWNER"))return true;
  if(path.startsWith("/api/admin/accounts")||path.equals("/api/admin/audit"))return false;
  if(method.equals("GET"))return path.startsWith("/api/admin/console/")||path.startsWith("/api/admin/reviews")||path.startsWith("/api/admin/media/")||path.startsWith("/api/admin/support/")||path.startsWith("/api/admin/safety/")||path.startsWith("/api/admin/phone-auth/");
  if(role.equals("REVIEWER"))return path.matches("/api/admin/reviews/[0-9]+/(approve|reject|memo)")||path.matches("/api/admin/console/members/[0-9]+/review");
  if(role.equals("SUPPORT"))return path.startsWith("/api/admin/support/")||path.matches("/api/admin/safety/reports/[0-9]+/resolve");
  return false;
 }
 private void reject(HttpServletResponse r,int status,String text)throws IOException{r.setStatus(status);r.setContentType("application/json;charset=UTF-8");r.getWriter().write("{\"message\":\""+text+"\"}");}
 @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse out,FilterChain chain)throws ServletException,IOException{
  String path=r.getRequestURI();if(!path.startsWith("/api/admin/")||path.equals("/api/admin/session/login")){chain.doFilter(r,out);return;}
  out.setHeader("Cache-Control","no-store");var p=admins.authenticate(token(r));if(p==null){reject(out,401,"관리자 로그인이 필요합니다.");return;}
  boolean mutation=!Set.of("GET","HEAD","OPTIONS").contains(r.getMethod());
  if(mutation&&!java.security.MessageDigest.isEqual(p.csrf().getBytes(java.nio.charset.StandardCharsets.UTF_8),Optional.ofNullable(r.getHeader("X-Admin-CSRF")).orElse("").getBytes(java.nio.charset.StandardCharsets.UTF_8))){reject(out,403,"관리자 요청을 다시 확인해 주세요.");return;}
  if(!allowed(p.role(),r.getMethod(),path)){admins.audit(p.id(),p.username(),"DENIED "+r.getMethod()+" "+path,403);reject(out,403,"이 작업에 필요한 관리자 권한이 없습니다.");return;}
  r.setAttribute("admin",p);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
  try{chain.doFilter(r,out);}finally{if(mutation)admins.audit(p.id(),p.username(),r.getMethod()+" "+path,out.getStatus());}
 }
}
