package com.dating.backend.matching;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="app.notices.jobs",havingValue="true",matchIfMissing=true)
public class ChatRequestJobs {
    private final ChatRequestService requests;
    @Scheduled(fixedDelay=30000,initialDelay=10000)
    public void expire(){for(var id:requests.expiredIds())requests.expireOne(id);}
}
