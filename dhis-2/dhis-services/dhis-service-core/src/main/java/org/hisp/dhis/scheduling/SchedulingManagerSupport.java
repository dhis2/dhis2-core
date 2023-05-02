package org.hisp.dhis.scheduling;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.eventhook.EventHookPublisher;
import org.hisp.dhis.leader.election.LeaderManager;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
@Getter
@RequiredArgsConstructor
public class SchedulingManagerSupport
{
    private final UserService userService;

    private final JobService jobService;

    private final JobConfigurationService jobConfigurationService;

    private final MessageService messageService;

    private final LeaderManager leaderManager;

    private final Notifier notifier;

    private final EventHookPublisher eventHookPublisher;

    private final CacheProvider cacheProvider;

    private final AsyncTaskExecutor taskExecutor;

    @Qualifier( "taskScheduler" )
    private final TaskScheduler jobScheduler;
}
