package com.dating.backend.matching;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
@Component @Profile("demo-seed") @RequiredArgsConstructor
public class DemoSeedRunner implements ApplicationRunner {
 private final DemoSeedService service;
 @Override public void run(ApplicationArguments args) throws Exception {System.out.println("DEMO_SEED_COMPLETE created="+service.seed());}
}
