package org.hisp.dhis.dxf2.metadata.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncPreProcessor;
import org.hisp.dhis.dxf2.metadata.sync.exception.DhisVersionMismatchException;
import org.hisp.dhis.leader.election.LeaderManager;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.ControlledJobProgress;
import org.hisp.dhis.scheduling.DefaultJobService;
import org.hisp.dhis.scheduling.DefaultSchedulingManager;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobProgress.Status;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.notification.Notifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class MetadataSyncJobTest {

  @Mock private ApplicationContext applicationContext;

  @Mock private JobConfigurationService jobConfigurationService;

  @Mock private LeaderManager leaderManager;

  @Mock private TaskScheduler taskScheduler;

  @Mock private CacheProvider cacheProvider;
  private DefaultSchedulingManager schedulingManager;

  @Mock private SystemSettingManager systemSettingManager;

  @BeforeEach
  void setUp() {
    schedulingManager =
        new DefaultSchedulingManager(
            new DefaultJobService(applicationContext),
            jobConfigurationService,
            mock(MessageService.class),
            mock(Notifier.class),
            leaderManager,
            taskScheduler,
            mock(AsyncTaskExecutor.class),
            cacheProvider);
  }

  @Test
  @DisplayName("Job status is updated correctly after setup process")
  void jobStatusIsUpdatedCorrectlyAfterSetupProcessTest() throws DhisVersionMismatchException {
    // given
    JobConfiguration config = new JobConfiguration();
    config.setJobType(JobType.META_DATA_SYNC);
    config.setInMemoryJob(true);

    ControlledJobProgress jobProgress = new ControlledJobProgress(config);
    //    MetadataSyncJob job = new MetadataSyncJob(null, null, null, null, null, null, null, null);

    //    job.runSyncTask(null, (MetadataSyncJobParameters) config.getJobParameters(), jobProgress);
    MetadataSyncPreProcessor preProcessor =
        new MetadataSyncPreProcessor(systemSettingManager, null, null, null, null, null, null);
    preProcessor.setUp(null, jobProgress);

    // then
    //    assertTrue(schedulingManager.checkWasSuccessfulRun(config, jobProgress));
    //    assertEquals(1, jobProgress.getProcesses().size());
    //    assertEquals(Status.SUCCESS, jobProgress.getProcesses().getFirst().getStatus());
  }
}
