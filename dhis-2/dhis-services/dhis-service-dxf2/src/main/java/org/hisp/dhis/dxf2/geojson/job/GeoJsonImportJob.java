package org.hisp.dhis.dxf2.geojson.job;

import java.io.IOException;
import java.io.InputStream;
import lombok.AllArgsConstructor;
import org.hisp.dhis.dxf2.geojson.GeoJsonImportReport;
import org.hisp.dhis.dxf2.geojson.GeoJsonService;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.GeoJsonImportJobParams;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class GeoJsonImportJob implements Job {

  private final GeoJsonService geoJsonService;
  private final FileResourceService fileResourceService;
  private final Notifier notifier;

  @Override
  public JobType getJobType() {
    return JobType.GEOJSON_IMPORT;
  }

  @Override
  public void execute(JobConfiguration jobConfig, JobProgress progress) {
    progress.startingProcess("GeoJSON import started");
    GeoJsonImportJobParams jobParams = (GeoJsonImportJobParams) jobConfig.getJobParameters();

    progress.startingStage("Loading file resource");
    FileResource data =
        progress.runStage(() -> fileResourceService.getFileResource(jobConfig.getUid()));
    progress.startingStage("Loading file content");

    try (InputStream input =
        progress.runStage(() -> fileResourceService.getFileResourceContent(data))) {
      GeoJsonImportReport report = geoJsonService.importGeoData(jobParams, input);
      progress.completedProcess("GeoJSON import complete");
      notifier.addJobSummary(jobConfig, report, GeoJsonImportReport.class);
    } catch (IOException e) {
      progress.failedProcess(e);
    }
  }
}
