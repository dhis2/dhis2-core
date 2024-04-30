package org.hisp.dhis.dxf2.metadata.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncPreProcessor;
import org.hisp.dhis.dxf2.metadata.version.MetadataVersionDelegate;
import org.hisp.dhis.leader.election.LeaderManager;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.metadata.version.VersionType;
import org.hisp.dhis.scheduling.ControlledJobProgress;
import org.hisp.dhis.scheduling.DefaultJobService;
import org.hisp.dhis.scheduling.DefaultSchedulingManager;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobProgress.Status;
import org.hisp.dhis.scheduling.JobStatus;
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

  private DefaultSchedulingManager schedulingManager;
  @Mock private SystemSettingManager systemSettingManager;
  @Mock private MetadataVersionService metadataVersionService;
  @Mock private MetadataVersionDelegate metadataVersionDelegate;

  @BeforeEach
  void setUp() {
    schedulingManager =
        new DefaultSchedulingManager(
            new DefaultJobService(mock(ApplicationContext.class)),
            mock(JobConfigurationService.class),
            mock(MessageService.class),
            mock(Notifier.class),
            mock(LeaderManager.class),
            mock(TaskScheduler.class),
            mock(AsyncTaskExecutor.class),
            mock(CacheProvider.class));
  }

  @Test
  @DisplayName(
      "Last executed status should be COMPLETED after metadata sync pre-processor setup completes and progress process status is success")
  void preprocessSetupTest() {
    // given
    JobConfiguration config = new JobConfiguration();
    config.setJobType(JobType.META_DATA_SYNC);
    config.setInMemoryJob(true);
    config.setLastExecutedStatus(JobStatus.RUNNING);

    ControlledJobProgress jobProgress = new ControlledJobProgress(config);

    MetadataSyncPreProcessor preProcessor =
        new MetadataSyncPreProcessor(systemSettingManager, null, null, null, null, null, null);

    // when
    preProcessor.setUp(null, jobProgress);
    boolean wasSuccessfulRun = schedulingManager.checkWasSuccessfulRun(config, jobProgress);

    // then
    assertTrue(wasSuccessfulRun);
    assertEquals(JobStatus.COMPLETED, config.getLastExecutedStatus());
    assertEquals(1, jobProgress.getProcesses().size());
    assertEquals(Status.SUCCESS, jobProgress.getProcesses().getFirst().getStatus());
  }

  @Test
  @DisplayName(
      "Last executed status should be COMPLETED after metadata sync pre-processor handle metadata versions completes and progress process status is success")
  void handleMetadataVersionsTest() {
    // given
    JobConfiguration config = new JobConfiguration();
    config.setJobType(JobType.META_DATA_SYNC);
    config.setInMemoryJob(true);
    config.setLastExecutedStatus(JobStatus.RUNNING);

    ControlledJobProgress jobProgress = new ControlledJobProgress(config);

    MetadataSyncPreProcessor preProcessor =
        new MetadataSyncPreProcessor(
            systemSettingManager,
            metadataVersionService,
            metadataVersionDelegate,
            null,
            null,
            null,
            null);

    MetadataVersion mdVersion = new MetadataVersion("test", VersionType.BEST_EFFORT);
    when(metadataVersionService.getCurrentVersion()).thenReturn(mdVersion);
    when(metadataVersionDelegate.getMetaDataDifference(mdVersion)).thenReturn(List.of());

    // when
    preProcessor.handleMetadataVersions(null, jobProgress);
    boolean wasSuccessfulRun = schedulingManager.checkWasSuccessfulRun(config, jobProgress);

    // then
    assertTrue(wasSuccessfulRun);
    assertEquals(JobStatus.COMPLETED, config.getLastExecutedStatus());
    assertEquals(1, jobProgress.getProcesses().size());
    assertEquals(Status.SUCCESS, jobProgress.getProcesses().getFirst().getStatus());
  }
}
