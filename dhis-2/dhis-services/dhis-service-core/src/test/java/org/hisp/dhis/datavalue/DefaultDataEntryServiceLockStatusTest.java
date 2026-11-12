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
package org.hisp.dhis.datavalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataset.LockStatus;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.period.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests {@link DefaultDataEntryService#getEntryStatus(UID, DataValueKey)} for a data set that
 * defines an explicit {@code DataInputPeriod} open/close window for the queried period.
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class DefaultDataEntryServiceLockStatusTest {

  private static final UID DATA_SET = UID.of("ds123456789");
  private static final UID DATA_ELEMENT = UID.of("de123456789");
  private static final UID ORG_UNIT = UID.of("ou123456789");
  private static final Period PERIOD = Period.of("2025");

  @Mock private DataEntryStore store;
  @Mock private org.hisp.dhis.common.IdCoder idCoder;
  @Mock private DataEntryAuditService audit;

  private DefaultDataEntryService service;

  @BeforeEach
  void setUp() {
    service = new DefaultDataEntryService(store, idCoder, audit);
    when(store.getDataSetAocInApproval(DATA_SET)).thenReturn(List.of());
    when(store.getEntrySpanByAoc(any())).thenReturn(Map.of());
  }

  private static DataValueKey key() {
    return new DataValueKey(DATA_ELEMENT, ORG_UNIT, null, null, PERIOD);
  }

  private static DateRange rangeAround(Date now, long startOffsetDays, long endOffsetDays) {
    return new DateRange(
        new Date(now.getTime() + TimeUnit.DAYS.toMillis(startOffsetDays)),
        new Date(now.getTime() + TimeUnit.DAYS.toMillis(endOffsetDays)));
  }

  @Test
  void getEntryStatus_isOpen_whenNowIsWithinConfiguredInputPeriodWindow() throws ConflictException {
    Date now = new Date();
    when(store.getEntrySpansByIsoPeriod(DATA_SET))
        .thenReturn(Map.of(PERIOD.getIsoDate(), List.of(rangeAround(now, -1, 1))));

    assertEquals(LockStatus.OPEN, service.getEntryStatus(DATA_SET, key()));
  }

  @Test
  void getEntryStatus_isLocked_whenNowIsOutsideConfiguredInputPeriodWindow()
      throws ConflictException {
    Date now = new Date();
    when(store.getEntrySpansByIsoPeriod(DATA_SET))
        .thenReturn(Map.of(PERIOD.getIsoDate(), List.of(rangeAround(now, -10, -5))));

    assertEquals(LockStatus.LOCKED, service.getEntryStatus(DATA_SET, key()));
  }
}
