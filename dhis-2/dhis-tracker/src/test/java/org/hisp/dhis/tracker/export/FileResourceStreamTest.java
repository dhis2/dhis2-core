/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.tracker.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.storage.BlobReadOptions;
import org.hisp.dhis.storage.BlobReadTimeoutException;
import org.hisp.dhis.tracker.export.timeout.Deadline;
import org.hisp.dhis.tracker.export.timeout.DeadlineExceededException;
import org.hisp.dhis.tracker.export.timeout.DeadlineHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * How the file suppliers bound their object store fetch by the export budget. The store is mocked,
 * so these assert what tracker hands it and when it is left alone; {@code S3BlobStoreServiceTest}
 * covers what the timeout does to the request, and the export controller tests cover how a fetch
 * failure is reported.
 */
class FileResourceStreamTest {

  private final FileResourceService fileResourceService = mock(FileResourceService.class);

  @AfterEach
  void clearDeadline() {
    DeadlineHolder.clear();
  }

  private static FileResource fileResource() {
    FileResource fileResource = new FileResource();
    fileResource.setUid("fileResourc1");
    fileResource.setName("a.pdf");
    fileResource.setContentType("application/pdf");
    fileResource.setContentLength(3L);
    return fileResource;
  }

  private BlobReadOptions capturedOptions() throws Exception {
    ArgumentCaptor<BlobReadOptions> captor = ArgumentCaptor.forClass(BlobReadOptions.class);
    verify(fileResourceService).openContentStream(any(FileResource.class), captor.capture());
    return captor.getValue();
  }

  @Test
  void shouldBoundTheFetchByWhatIsLeftOfTheBudgetRatherThanTheWholeOfIt() throws Exception {
    // a 10s budget with 4s already spent on the database lookup
    long[] clock = {0};
    DeadlineHolder.set(Deadline.in(Duration.ofSeconds(10), () -> clock[0]));
    clock[0] = Duration.ofSeconds(4).toNanos();
    when(fileResourceService.openContentStream(any(FileResource.class), any()))
        .thenReturn(new ByteArrayInputStream(new byte[3]));

    FileResourceStream.of(fileResourceService, fileResource()).contentSupplier().get();

    assertEquals(
        Duration.ofSeconds(6),
        capturedOptions().nextTimeout(),
        "the fetch must get what is left of the budget, not a fresh one");
  }

  @Test
  void shouldLeaveTheFetchUnboundedWhenTheTimeoutIsDisabled() throws Exception {
    // no deadline armed, as when tracker.export.timeout is off
    when(fileResourceService.openContentStream(any(FileResource.class), any()))
        .thenReturn(new ByteArrayInputStream(new byte[3]));

    FileResourceStream.of(fileResourceService, fileResource()).contentSupplier().get();

    assertNull(
        capturedOptions().nextTimeout(),
        "callers without a budget must keep the store configuration");
  }

  @Test
  void shouldFailFastWhenTheBudgetIsAlreadySpentBeforeReachingTheStore() {
    // a 5s budget armed 6s ago, so the store is never reached
    long[] clock = {0};
    DeadlineHolder.set(Deadline.in(Duration.ofSeconds(5), () -> clock[0]));
    clock[0] = Duration.ofSeconds(6).toNanos();

    FileResourceSupplier<FileResourceStream.Content> content =
        FileResourceStream.of(fileResourceService, fileResource()).contentSupplier();

    assertThrows(DeadlineExceededException.class, content::get);
    verifyNoInteractions(fileResourceService);
  }

  @Test
  void shouldReportAStoreTimeoutAsTheBudgetBeingExceeded() throws Exception {
    DeadlineHolder.set(Deadline.in(Duration.ofSeconds(5)));
    when(fileResourceService.openContentStream(any(FileResource.class), any()))
        .thenThrow(new BlobReadTimeoutException("timed out", new RuntimeException()));

    FileResourceSupplier<FileResourceStream.Content> content =
        FileResourceStream.of(fileResourceService, fileResource()).contentSupplier();

    DeadlineExceededException e = assertThrows(DeadlineExceededException.class, content::get);
    assertEquals(
        "Tracker export exceeded its time budget of 5s",
        e.getMessage(),
        "the error must name the budget, as a timed out query does");
  }

  @Test
  void shouldBoundTheBufferedImageFetchTooSinceItReadsTheWholeImage() throws Exception {
    DeadlineHolder.set(Deadline.in(Duration.ofSeconds(10)));
    ArgumentCaptor<BlobReadOptions> captor = ArgumentCaptor.forClass(BlobReadOptions.class);
    when(fileResourceService.copyImageContent(
            any(FileResource.class), eq(ImageFileDimension.MEDIUM), any()))
        .thenReturn(new byte[3]);

    FileResourceStream.ofImage(fileResourceService, fileResource(), ImageFileDimension.MEDIUM)
        .contentSupplier()
        .get();

    verify(fileResourceService)
        .copyImageContent(any(FileResource.class), eq(ImageFileDimension.MEDIUM), captor.capture());
    assertNotNull(
        captor.getValue().nextTimeout(), "the buffered image path must be bounded as well");
  }
}
