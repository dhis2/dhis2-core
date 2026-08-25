/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.sync;

import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.formatFailedUids;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.FailedItem;
import org.junit.jupiter.api.Test;

class TrackerSyncReportUtilsTest {

  @Test
  void shouldReturnEmptyStringWhenNoFailures() {
    assertEquals("", formatFailedUids(List.of()));
  }

  @Test
  void shouldListAllUidsWhenFailuresAreAtOrBelowTheCap() {
    List<FailedItem> failed = List.of(item("Uid00000001", "E1032"), item("Uid00000002", "E1032"));

    assertEquals(" (failed: E1032 x2 [Uid00000001, Uid00000002])", formatFailedUids(failed));
  }

  @Test
  void shouldCapUidsPerReasonAndReportHowManyMoreThereAre() {
    List<FailedItem> failed =
        List.of(
            item("Uid00000001", "E1032"),
            item("Uid00000002", "E1032"),
            item("Uid00000003", "E1032"),
            item("Uid00000004", "E1032"),
            item("Uid00000005", "E1032"),
            item("Uid00000006", "E1032"),
            item("Uid00000007", "E1032"));

    assertEquals(
        " (failed: E1032 x7 [Uid00000001, Uid00000002, Uid00000003, Uid00000004, Uid00000005,"
            + " +2 more])",
        formatFailedUids(failed));
  }

  @Test
  void shouldGroupByErrorCodeAndOrderReasonsByDescendingFailureCount() {
    List<FailedItem> failed =
        List.of(
            item("Uid00000001", "E1082"),
            item("Uid00000002", "E1032"),
            item("Uid00000003", "E1032"),
            item("Uid00000004", "E1032"));

    assertEquals(
        " (failed: E1032 x3 [Uid00000002, Uid00000003, Uid00000004], E1082 x1 [Uid00000001])",
        formatFailedUids(failed));
  }

  @Test
  void shouldCountAFailedEntityOnceEvenWhenItHasMultipleErrorsUnderTheSameCode() {
    List<FailedItem> failed = List.of(item("Uid00000001", "E5000"), item("Uid00000001", "E5000"));

    assertEquals(" (failed: E5000 x1 [Uid00000001])", formatFailedUids(failed));
  }

  private static FailedItem item(String uid, String errorCode) {
    return new FailedItem(UID.of(uid), errorCode);
  }
}
