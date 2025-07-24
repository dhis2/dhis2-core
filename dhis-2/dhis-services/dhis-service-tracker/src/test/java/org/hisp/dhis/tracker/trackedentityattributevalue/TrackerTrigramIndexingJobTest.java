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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.parameters.TrackerTrigramIndexJobParameters;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link TrackerTrigramIndexingJob} using mocks to only test the logic of the job.
 *
 * @author Ameen
 */
class TrackerTrigramIndexingJobTest {

  private final TrackedEntityAttributeService trackedEntityAttributeService =
      mock(TrackedEntityAttributeService.class);

  private final TrackedEntityAttributeTableManager trackedEntityAttributeTableManager =
      mock(TrackedEntityAttributeTableManager.class);

  private final TrackerTrigramIndexingJob job =
      new TrackerTrigramIndexingJob(
          trackedEntityAttributeService, trackedEntityAttributeTableManager);

  @BeforeEach
  void setUp() {
    when(trackedEntityAttributeService.getAllTrigramIndexableAttributes())
        .thenReturn(Collections.emptySet());
    when(trackedEntityAttributeTableManager.getAttributesWithTrigramIndex())
        .thenReturn(Collections.emptyList());
  }

  @Test
  void testRunJobWithoutAnyAttributesInJobParametersAndWithoutAnyObsolete() {
    JobConfiguration jobConfiguration = new JobConfiguration();
    TrackerTrigramIndexJobParameters jp = new TrackerTrigramIndexJobParameters(true);
    jobConfiguration.setJobParameters(jp);

    job.execute(jobConfiguration, JobProgress.noop());

    verify(trackedEntityAttributeTableManager, never()).createTrigramIndex(any());
    verify(trackedEntityAttributeTableManager, never()).dropTrigramIndex(any());
    verify(trackedEntityAttributeTableManager, never()).runAnalyzeOnTrackedEntityAttributeValue();
  }

  @Test
  void testRunJobWithoutAnyAttributesInJobParametersButWithObsoleteIndexes() {
    when(trackedEntityAttributeTableManager.getAttributesWithTrigramIndex())
        .thenReturn(Arrays.asList(12L, 13L));

    JobConfiguration jobConfiguration = new JobConfiguration();
    TrackerTrigramIndexJobParameters jp = new TrackerTrigramIndexJobParameters(true);
    jobConfiguration.setJobParameters(jp);

    job.execute(jobConfiguration, JobProgress.noop());

    verify(trackedEntityAttributeTableManager, never()).createTrigramIndex(any());
    verify(trackedEntityAttributeTableManager, times(2)).dropTrigramIndex(any());
    verify(trackedEntityAttributeTableManager, times(1)).runAnalyzeOnTrackedEntityAttributeValue();
  }

  @Test
  void testRunJobWithTwoIndexableAttributesInJobParameters() {
    Set<TrackedEntityAttribute> indexableAttributes = new HashSet<>();
    TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
    tea1.setUid("tea1");
    TrackedEntityAttribute tea2 = new TrackedEntityAttribute();
    tea2.setUid("tea2");
    TrackedEntityAttribute tea3 = new TrackedEntityAttribute();
    tea3.setUid("tea3");
    indexableAttributes.add(tea1);
    indexableAttributes.add(tea2);
    indexableAttributes.add(tea3);

    when(trackedEntityAttributeService.getAllTrigramIndexableAttributes())
        .thenReturn(indexableAttributes);
    doNothing().when(trackedEntityAttributeTableManager).createTrigramIndex(any());
    JobConfiguration jobConfiguration = new JobConfiguration();

    job.execute(jobConfiguration, JobProgress.noop());

    verify(trackedEntityAttributeTableManager, times(3)).createTrigramIndex(any());
    verify(trackedEntityAttributeTableManager, never()).runAnalyzeOnTrackedEntityAttributeValue();
  }
}
