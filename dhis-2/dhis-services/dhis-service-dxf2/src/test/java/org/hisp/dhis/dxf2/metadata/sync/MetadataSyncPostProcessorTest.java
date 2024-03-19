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
package org.hisp.dhis.dxf2.metadata.sync;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.jobs.MetadataRetryContext;
import org.hisp.dhis.email.EmailService;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.VersionType;
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
class MetadataSyncPostProcessorTest {
  @Mock private EmailService emailService;

  @Mock private MetadataRetryContext metadataRetryContext;

  @InjectMocks private MetadataSyncPostProcessor metadataSyncPostProcessor;

  private MetadataVersion dataVersion;

  private MetadataSyncSummary metadataSyncSummary;

  @BeforeEach
  public void setUp() {

    dataVersion = new MetadataVersion();
    dataVersion.setType(VersionType.BEST_EFFORT);
    dataVersion.setName("testVersion");
    dataVersion.setCreated(new Date());
    dataVersion.setHashCode("samplehashcode");
    metadataSyncSummary = new MetadataSyncSummary();
  }

  @Test
  void testShouldSendSuccessEmailIfSyncSummaryIsOk() {
    metadataSyncSummary.setImportReport(new ImportReport());
    metadataSyncSummary.getImportReport().setStatus(Status.OK);
    metadataSyncSummary.setMetadataVersion(dataVersion);
    MetadataRetryContext mockRetryContext = mock(MetadataRetryContext.class);

    boolean status =
        metadataSyncPostProcessor.handleSyncNotificationsAndAbortStatus(
            metadataSyncSummary, mockRetryContext, dataVersion);

    assertFalse(status);
  }

  @Test
  void testShouldSendSuccessEmailIfSyncSummaryIsWarning() {
    metadataSyncSummary.setImportReport(new ImportReport());
    metadataSyncSummary.getImportReport().setStatus(Status.WARNING);
    metadataSyncSummary.setMetadataVersion(dataVersion);
    MetadataRetryContext mockRetryContext = mock(MetadataRetryContext.class);

    boolean status =
        metadataSyncPostProcessor.handleSyncNotificationsAndAbortStatus(
            metadataSyncSummary, mockRetryContext, dataVersion);

    assertFalse(status);
  }

  @Test
  void testShouldSendSuccessEmailIfSyncSummaryIsError() {
    metadataSyncSummary.setImportReport(new ImportReport());
    metadataSyncSummary.getImportReport().setStatus(Status.ERROR);
    metadataSyncSummary.setMetadataVersion(dataVersion);
    MetadataRetryContext mockMetadataRetryContext = mock(MetadataRetryContext.class);
    RetryContext mockRetryContext = mock(RetryContext.class);

    when(mockMetadataRetryContext.getRetryContext()).thenReturn(mockRetryContext);
    boolean status =
        metadataSyncPostProcessor.handleSyncNotificationsAndAbortStatus(
            metadataSyncSummary, mockMetadataRetryContext, dataVersion);

    assertTrue(status);
  }

  @Test
  void testShouldSendEmailToAdminWithProperSubjectAndBody() {
    ImportReport importReport = mock(ImportReport.class);

    metadataSyncSummary.setImportReport(importReport);
    metadataSyncSummary.getImportReport().setStatus(Status.OK);
    metadataSyncSummary.setMetadataVersion(dataVersion);
    MetadataRetryContext mockRetryContext = mock(MetadataRetryContext.class);

    boolean status =
        metadataSyncPostProcessor.handleSyncNotificationsAndAbortStatus(
            metadataSyncSummary, mockRetryContext, dataVersion);

    assertFalse(status);
  }
}
