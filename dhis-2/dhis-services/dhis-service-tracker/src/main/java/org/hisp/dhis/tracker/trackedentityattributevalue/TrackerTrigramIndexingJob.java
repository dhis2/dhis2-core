/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.tracker.trackedentityattributevalue;

import static java.lang.String.format;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM_OUTLIER;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.TrackerTrigramIndexJobParameters;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.stereotype.Component;

/**
 * @author Ameen
 */
@Slf4j
@RequiredArgsConstructor
@Component("trackerTrigramIndexingJob")
public class TrackerTrigramIndexingJob implements Job {
  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final TrackedEntityAttributeTableManager trackedEntityAttributeTableManager;

  // -------------------------------------------------------------------------
  // Implementation
  // -------------------------------------------------------------------------

  @Override
  public JobType getJobType() {
    return JobType.TRACKER_SEARCH_OPTIMIZATION;
  }

  @Override
  public void execute(JobConfiguration jobConfiguration, JobProgress progress) {
    TrackerTrigramIndexJobParameters parameters =
        (TrackerTrigramIndexJobParameters) jobConfiguration.getJobParameters();

    log.info("Starting Trigram Indexing Job.");
    progress.startingProcess("Starting Trigram indexing process");

    log.debug("Fetching all trigram indexed attributes from db");
    List<Long> indexedAttributes =
        trackedEntityAttributeTableManager.getAttributesWithTrigramIndex();

    log.debug("Fetching all indexable attributes from db");
    Set<TrackedEntityAttribute> indexableAttributes =
        trackedEntityAttributeService.getAllTrigramIndexableAttributes();

    boolean indexesWereCreated =
        createTrigramIndexes(progress, indexedAttributes, indexableAttributes);
    boolean indexesWereRemoved =
        removeObsoleteTrigramIndexes(progress, indexedAttributes, indexableAttributes);

    if ((indexesWereCreated || indexesWereRemoved)
        && parameters != null
        && parameters.isRunAnalyze()) {
      log.debug("Running `ANALYZE` on tracked entity attribute values");
      trackedEntityAttributeTableManager.runAnalyzeOnTrackedEntityAttributeValue();
    }

    progress.completedProcess("Job completed");
    log.info("Trigram Indexing job completed");
  }

  private boolean createTrigramIndexes(
      JobProgress progress,
      List<Long> indexedAttributes,
      Set<TrackedEntityAttribute> indexableAttributes) {
    log.debug("Found total {} indexable attributes", indexableAttributes.size());

    Set<TrackedEntityAttribute> missingIndexableAttributes =
        indexableAttributes.stream()
            .filter(attr -> !indexedAttributes.contains(attr.getId()))
            .collect(Collectors.toSet());

    if (missingIndexableAttributes.isEmpty()) {
      log.debug("All indexable attributes are already indexed, skipping trigram index creation.");
      return false;
    } else {
      log.debug("Creating {} trigram indexes", missingIndexableAttributes.size());

      progress.startingStage(
          "Creating trigram indexes for attributes", missingIndexableAttributes.size());
      progress.runStage(
          missingIndexableAttributes.stream(),
          TrackedEntityAttribute::getName,
          trackedEntityAttributeTableManager::createTrigramIndex,
          TrackerTrigramIndexingJob::computeTrigramIndexingCreationSummary);
      progress.completedStage("Trigram indexes created");

      log.debug("Created {} trigram indexes", missingIndexableAttributes.size());
    }
    return true;
  }

  private boolean removeObsoleteTrigramIndexes(
      JobProgress progress,
      List<Long> indexedAttributes,
      Set<TrackedEntityAttribute> indexableAttributes) {
    log.debug("Checking existence obsolete trigram indexes");

    Set<Long> obsoleteIndexedAttributes = new HashSet<>(indexedAttributes);
    obsoleteIndexedAttributes.removeAll(
        indexableAttributes.stream()
            .map(BaseIdentifiableObject::getId)
            .collect(Collectors.toSet()));

    if (obsoleteIndexedAttributes.isEmpty()) {
      log.debug("No obsolete trigram indexes to drop");
      return false;
    }

    log.debug("Found total {} obsolete trigram indexes in db", obsoleteIndexedAttributes.size());

    progress.startingStage(
        "Deleting obsolete trigram indexes", obsoleteIndexedAttributes.size(), SKIP_ITEM_OUTLIER);
    progress.runStage(
        obsoleteIndexedAttributes.stream(),
        Object::toString,
        trackedEntityAttributeTableManager::dropTrigramIndex,
        TrackerTrigramIndexingJob::computeTrigramIndexingDropSummary);
    return true;
  }

  private static String computeTrigramIndexingCreationSummary(int successful, int failed) {
    String summary =
        format(
            "Number of trigram index created: %d. Number of trigram index creation failed: %d",
            successful, failed);
    log.debug(summary);
    return summary;
  }

  private static String computeTrigramIndexingDropSummary(int successful, int failed) {
    String summary =
        format(
            "Number of obsolete trigram index dropped: %d. Number of obsoletet trigram index drop failed: %d",
            successful, failed);
    log.debug(summary);
    return summary;
  }
}
