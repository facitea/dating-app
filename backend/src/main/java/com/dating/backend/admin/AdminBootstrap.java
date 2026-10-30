package com.dating.backend.admin;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Component;import org.springframework.boot.ApplicationRunner;import org.springframework.boot.ApplicationArguments;import org.springframework.beans.factory.annotation.Value;
@Component @RequiredArgsConstructor public class AdminBootstrap implements ApplicationRunner {
 private final AdminService admins;
 @Value("${app.admin.bootstrap-username:}") private String name;
 @Value("${app.admin.bootstrap-password:}") private String password;
 public void run(ApplicationArguments args){admins.bootstrap(name,password);}
}
