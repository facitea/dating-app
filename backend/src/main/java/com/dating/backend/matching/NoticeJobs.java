package com.dating.backend.matching;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Component;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import com.dating.backend.repository.UserRepository;
@Component @RequiredArgsConstructor @ConditionalOnProperty(name="app.notices.jobs",havingValue="true",matchIfMissing=true) public class NoticeJobs {
 private final InboxService inbox;private final PushService push;private final UserRepository users;private final DailyMatchSchedule schedule;
 @Scheduled(fixedDelay=15000,initialDelay=20000) public void push(){push.deliver();}
 @Scheduled(cron="0 0 8,20 * * *",zone="Asia/Seoul") public void round(){for(var u:users.findByStatusOrderByCreatedAtAsc("ACTIVE"))if(!u.isResting())inbox.add(u.getId(),NotificationService.todayKey(schedule.current()),"오늘의 인연이 도착했어요","새로운 추천 카드를 확인해 보세요.","/today");}
}
