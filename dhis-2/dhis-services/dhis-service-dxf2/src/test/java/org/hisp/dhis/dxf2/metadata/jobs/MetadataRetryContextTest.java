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
package org.hisp.dhis.dxf2.metadata.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncSummary;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.RetryContext;

/**
 * @author aamerm
 */
@ExtendWith(MockitoExtension.class)
class MetadataRetryContextTest {
  @Mock RetryContext retryContext;

  @InjectMocks MetadataRetryContext metadataRetryContext;

  private MetadataVersion mockVersion;

  private String testKey = "testKey";

  private String testMessage = "testMessage";

  @BeforeEach
  public void setUp() {
    mockVersion = mock(MetadataVersion.class);
  }

  @Test
  void testShouldGetRetryContextCorrectly() {
    assertEquals(retryContext, metadataRetryContext.getRetryContext());
  }

  @Test
  void testShouldSetRetryContextCorrectly() {
    RetryContext newMock = mock(RetryContext.class);

    metadataRetryContext.setRetryContext(newMock);

    assertEquals(newMock, metadataRetryContext.getRetryContext());
  }

  @Test
  void testIfVersionIsNull() {
    metadataRetryContext.updateRetryContext(testKey, testMessage, null);

    verify(retryContext).setAttribute(testKey, testMessage);
    verify(retryContext, never()).setAttribute(MetadataSyncJob.VERSION_KEY, null);
  }

  @Test
  void testIfVersionIsNotNull() {
    metadataRetryContext.updateRetryContext(testKey, testMessage, mockVersion);

    verify(retryContext).setAttribute(testKey, testMessage);
    verify(retryContext).setAttribute(MetadataSyncJob.VERSION_KEY, mockVersion);
  }

  @Test
  void testIfSummaryIsNull() {
    MetadataSyncSummary metadataSyncSummary = mock(MetadataSyncSummary.class);

    metadataRetryContext.updateRetryContext(testKey, testMessage, mockVersion, null);

    verify(retryContext).setAttribute(testKey, testMessage);
    verify(metadataSyncSummary, never()).getImportReport();
  }

  @Test
  void testIfSummaryIsNotNull() {
    MetadataSyncSummary testSummary = new MetadataSyncSummary();
    ImportReport importReport = new ImportReport();
    importReport.setStatus(Status.ERROR);
    testSummary.setImportReport(importReport);

    metadataRetryContext.updateRetryContext(testKey, testMessage, mockVersion, testSummary);

    verify(retryContext).setAttribute(testKey, testMessage);
  }
}
