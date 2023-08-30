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
package org.hisp.dhis.trackedentityattributevalue;

import static java.lang.String.format;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM_OUTLIER;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.commons.collection.CollectionUtils;
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

    log.info(
        "Starting Trigram Indexing Job. Attributes Provided to Index: {}",
        parameters.getAttributes());
    progress.startingProcess("Starting Trigram indexing process");

    // Fetch all indexable attributes only if needed
    if (!CollectionUtils.isEmpty(parameters.getAttributes()) || !parameters.isSkipIndexDeletion()) {
      log.debug("Fetching all indexable attributes from db");
      Set<TrackedEntityAttribute> allIndexableAttributes =
          trackedEntityAttributeService.getAllTrigramIndexableTrackedEntityAttributes();

      // Trigram index only need if requested in job parameters
      if (!CollectionUtils.isEmpty(parameters.getAttributes())) {
        createTrigramIndexesOnIndexableAttributes(progress, parameters, allIndexableAttributes);
      }

      // Obsolete index deletion
      if (!parameters.isSkipIndexDeletion()) {
        removeObsoleteTrigramIndexes(progress, allIndexableAttributes);
      }
    }
    progress.completedProcess("Job completed");
    log.info("Trigram Indexing job completed");
  }

  private void createTrigramIndexesOnIndexableAttributes(
      JobProgress progress,
      TrackerTrigramIndexJobParameters parameters,
      Set<TrackedEntityAttribute> allIndexableAttributes) {

    log.debug("Found total {} indexable attributes", allIndexableAttributes.size());

    // Remove indexableAttributes not requested to be indexed
    // Remove attributes requested to be indexed but not indexable
    Set<TrackedEntityAttribute> indexableAttributes =
        allIndexableAttributes.stream()
            .map(
                itea -> {
                  if (!parameters.getAttributes().contains(itea.getUid())) {
                    log.debug(
                        "Filtering out attribute uid : {} as it is not indexable", itea.getUid());
                  }
                  return itea;
                })
            .filter(itea -> parameters.getAttributes().contains(itea.getUid()))
            .collect(Collectors.toSet());

    log.debug(
        "Number of Attributes provided in job parameters that are indexable: {}",
        indexableAttributes.size());

    // Create indexes for indexable attributes specified in job parameters
    if (!indexableAttributes.isEmpty()) {
      createTrigramIndexes(progress, indexableAttributes);
    } else {
      log.warn(
          "No indexable attributes provided in job parameters. Skipping trigram index creation step");
    }
  }

  private void removeObsoleteTrigramIndexes(
      JobProgress progress, Set<TrackedEntityAttribute> allIndexableAttributes) {

    log.debug("Checking existence obsolete trigram indexes");

    /*
     * Fetch primary key ids of attributes based on existing trigram index
     * in db. Trigram index will be present with the naming convention
     * in_gin_teavalue_XXXXX where the last XXXXX denotes the attribute id
     * which will be matched and extracted in the query.
     */
    List<Long> teaIdList = trackedEntityAttributeTableManager.getAttributeIdsWithTrigramIndex();

    if (teaIdList.isEmpty()) {
      log.debug("No obsolete trigram indexes to drop");
      return;
    }

    Set<Long> teaIds = new HashSet<>(teaIdList);

    // Collect tea ids of all indexable attributets
    Set<Long> allIndexableAttributeIds =
        allIndexableAttributes.stream().map(IdentifiableObject::getId).collect(Collectors.toSet());

    log.debug("Found total {} trigram indexes in db", teaIds.size());

    /*
     * Remove all tea ids that are indexable. What remains in this set will
     * be attribute ids that have indexes present in the db but the
     * corresponding tea is not indexable anymore, hence an obsolete index.
     */
    teaIds.removeAll(allIndexableAttributeIds);

    log.debug("Found total {} obsolete trigram indexes in db", teaIds.size());

    progress.startingStage("Deleting obsolete trigram indexes", teaIds.size(), SKIP_ITEM_OUTLIER);
    progress.runStage(
        teaIds.stream(),
        Object::toString,
        trackedEntityAttributeTableManager::dropTrigramIndex,
        TrackerTrigramIndexingJob::computeTrigramIndexingDropSummary);
  }

  private void createTrigramIndexes(
      JobProgress progress, Set<TrackedEntityAttribute> indexableAttributes) {
    log.debug("Creating {} trigram indexes", indexableAttributes.size());
    progress.startingStage("Creating trigram indexes for attributes", indexableAttributes.size());
    progress.runStage(
        indexableAttributes.stream(),
        TrackedEntityAttribute::getName,
        trackedEntityAttributeTableManager::createTrigramIndex,
        TrackerTrigramIndexingJob::computeTrigramIndexingCreationSummary);
    progress.completedStage("Trigram indexes created");
    log.debug("Created {} trigram indexes", indexableAttributes.size());
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
