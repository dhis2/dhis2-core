package org.hisp.dhis.system;


import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StartupEventPublisher implements ApplicationListener<ContextRefreshedEvent> {

  public static final CountDownLatch SERVER_STARTED_LATCH = new CountDownLatch(1);

  @Override public void onApplicationEvent(ContextRefreshedEvent event) {

    log.error("ContextRefreshedEvent received:" + event);

    SERVER_STARTED_LATCH.countDown();

  }
}
