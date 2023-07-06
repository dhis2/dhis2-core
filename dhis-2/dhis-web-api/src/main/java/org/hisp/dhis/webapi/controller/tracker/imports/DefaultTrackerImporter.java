/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.webapi.controller.tracker.imports;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;

/**
 * @author Luca Cambi <luca@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class DefaultTrackerImporter implements TrackerImporter {
  private static final TrackedEntityMapper TRACKED_ENTITY_MAPPER =
      Mappers.getMapper(TrackedEntityMapper.class);

  private static final EnrollmentMapper ENROLLMENT_MAPPER =
      Mappers.getMapper(EnrollmentMapper.class);

  private static final EventMapper EVENT_MAPPER = Mappers.getMapper(EventMapper.class);

  private static final RelationshipMapper RELATIONSHIP_MAPPER =
      Mappers.getMapper(RelationshipMapper.class);

  @NonNull private final TrackerSyncImporter syncImporter;

  @NonNull private final TrackerAsyncImporter asyncImporter;

  @Override
  public TrackerImportReport importTracker(TrackerImportRequest request) {

    TrackerImportParams params = trackerImportParams(request);

    if (request.isAsync()) {
      return asyncImporter.importTracker(params, request.getAuthentication(), request.getUid());
    }

    return syncImporter.importTracker(params, request.getTrackerBundleReportMode());
  }

  private TrackerImportParams trackerImportParams(TrackerImportRequest request) {
    TrackerIdSchemeParams idSchemeParams =
        TrackerImportParamsBuilder.getTrackerIdentifiers(
            request.getContextService().getParameterValuesMap());

    TrackerImportParams.TrackerImportParamsBuilder paramsBuilder =
        TrackerImportParamsBuilder.builder(request.getContextService().getParameterValuesMap())
            .userId(request.getUserUid())
            .trackedEntities(
                TRACKED_ENTITY_MAPPER.fromCollection(
                    request.getTrackerBundleParams().getTrackedEntities(), idSchemeParams))
            .enrollments(
                ENROLLMENT_MAPPER.fromCollection(
                    request.getTrackerBundleParams().getEnrollments(), idSchemeParams))
            .events(
                EVENT_MAPPER.fromCollection(
                    request.getTrackerBundleParams().getEvents(), idSchemeParams))
            .relationships(
                RELATIONSHIP_MAPPER.fromCollection(
                    request.getTrackerBundleParams().getRelationships(), idSchemeParams));

    if (!request.isAsync()) {
      JobConfiguration jobConfiguration =
          new JobConfiguration(
              "", JobType.TRACKER_IMPORT_JOB, request.getUserUid(), request.isAsync());
      jobConfiguration.setUid(request.getUid());
      paramsBuilder.jobConfiguration(jobConfiguration);
    }

    return paramsBuilder.build();
  }
}
