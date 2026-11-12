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
package org.hisp.dhis.dxf2.metadata.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncSummary;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author aamerm
 */
class MetadataRetryContextTest {
  private MetadataRetryContext metadataRetryContext;

  private MetadataVersion mockVersion;

  private final String testKey = "testKey";

  private final String testMessage = "testMessage";

  @BeforeEach
  void setUp() {
    metadataRetryContext = new MetadataRetryContext();
    mockVersion = mock(MetadataVersion.class);
  }

  @Test
  void testBeginAttemptIncrementsCount() {
    metadataRetryContext.beginAttempt();
    metadataRetryContext.beginAttempt();

    assertEquals(2, metadataRetryContext.getAttempt());
  }

  @Test
  void testResetClearsState() {
    metadataRetryContext.beginAttempt();
    metadataRetryContext.updateRetryContext(testKey, testMessage, mockVersion);
    metadataRetryContext.setLastThrowable(new RuntimeException("boom"));

    metadataRetryContext.reset();

    assertEquals(0, metadataRetryContext.getAttempt());
    assertNull(metadataRetryContext.getAttribute(testKey));
    assertNull(metadataRetryContext.getLastThrowable());
  }

  @Test
  void testIfVersionIsNull() {
    metadataRetryContext.updateRetryContext(testKey, testMessage, null);

    assertEquals(testMessage, metadataRetryContext.getAttribute(testKey));
    assertNull(metadataRetryContext.getAttribute(MetadataSyncJob.VERSION_KEY));
  }

  @Test
  void testIfVersionIsNotNull() {
    metadataRetryContext.updateRetryContext(testKey, testMessage, mockVersion);

    assertEquals(testMessage, metadataRetryContext.getAttribute(testKey));
    assertEquals(mockVersion, metadataRetryContext.getAttribute(MetadataSyncJob.VERSION_KEY));
  }

  @Test
  void testIfSummaryIsNull() {
    MetadataSyncSummary metadataSyncSummary = mock(MetadataSyncSummary.class);

    metadataRetryContext.updateRetryContext(testKey, testMessage, mockVersion, null);

    assertEquals(testMessage, metadataRetryContext.getAttribute(testKey));
    verify(metadataSyncSummary, never()).getImportReport();
  }

  @Test
  void testIfSummaryIsNotNull() {
    MetadataSyncSummary testSummary = new MetadataSyncSummary();
    ImportReport importReport = new ImportReport();
    importReport.setStatus(Status.ERROR);
    testSummary.setImportReport(importReport);

    metadataRetryContext.updateRetryContext(testKey, testMessage, mockVersion, testSummary);

    assertEquals(testMessage, metadataRetryContext.getAttribute(testKey));
  }
}
