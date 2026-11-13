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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.calendar.impl.NepaliCalendar;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.IdCoder;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataset.LockStatus;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests the category option date check of {@link DefaultDataEntryService} with a non-ISO system
 * calendar. "Open periods after category option end date" must not rewind dates by a negative
 * number of periods: the Nepali, Ethiopian and Persian calendars only count down, so a negative
 * count loops for seconds and then fails.
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class DefaultDataEntryServiceCalendarTest {

  private static final UID DATA_SET = UID.of("ds123456789");
  private static final UID DATA_ELEMENT = UID.of("de123456789");
  private static final UID ORG_UNIT = UID.of("ou123456789");
  private static final UID AOC = UID.of("aoc23456789");

  @Mock private DataEntryStore store;
  @Mock private IdCoder idCoder;
  @Mock private DataEntryAuditService audit;

  private DefaultDataEntryService service;

  @BeforeEach
  void setUp() {
    CalendarService calendarService = mock(CalendarService.class);
    when(calendarService.getSystemCalendar()).thenReturn(NepaliCalendar.getInstance());
    PeriodType.setCalendarService(calendarService);

    service = new DefaultDataEntryService(store, idCoder, audit);
    when(store.getEntrySpansByIsoPeriod(DATA_SET)).thenReturn(Map.of());
    when(store.getDataSetAocInApproval(DATA_SET)).thenReturn(List.of());
    // the AOC is valid from 1 January to 30 June 2025, plus 2 months of grace
    when(store.getEntrySpanByAoc(any()))
        .thenReturn(Map.of(AOC.getValue(), new DateRange(date(2025, 1, 1), date(2025, 6, 30))));
    when(store.getDataSetOpenPeriodsAfterCoEndDate(DATA_SET)).thenReturn(2);
  }

  @AfterEach
  void tearDown() {
    PeriodType.setCalendarService(null);
  }

  @Test
  void gracePeriodAfterOptionEndWorksWithNepaliCalendar() {
    // August is within the 2 months of grace; December is far beyond them
    assertTimeoutPreemptively(
        Duration.ofSeconds(2),
        () -> {
          assertEquals(LockStatus.OPEN, service.getEntryStatus(DATA_SET, key("202508")));
          assertEquals(LockStatus.LOCKED, service.getEntryStatus(DATA_SET, key("202512")));
        });
  }

  private static DataValueKey key(String isoPeriod) {
    return new DataValueKey(DATA_ELEMENT, ORG_UNIT, null, AOC, Period.of(isoPeriod));
  }

  private static Date date(int year, int month, int day) {
    return Date.from(
        LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant());
  }
}
