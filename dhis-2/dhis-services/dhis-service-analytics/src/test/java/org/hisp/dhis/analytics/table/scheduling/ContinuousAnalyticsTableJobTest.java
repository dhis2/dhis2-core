/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.table.scheduling;

import static org.hisp.dhis.util.DateUtils.getDate;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Date;
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.common.TableInfoReader;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContinuousAnalyticsTableJobTest {
  @Mock private AnalyticsTableGenerator analyticsTableGenerator;

  @Mock private SystemSettingsService settingsService;

  @Mock private SystemSettings settings;

  @Mock private TableInfoReader tableInfoReader;

  @InjectMocks private ContinuousAnalyticsTableJob job;

  private final Date dateA = getDate(2024, 1, 4, 23, 0);
  private final Date dateB = getDate(2024, 1, 5, 2, 0);
  private final Date dateC = getDate(2024, 1, 5, 8, 0);

  @BeforeEach
  public void beforeEach() {
    when(settingsService.getCurrentSettings()).thenReturn(settings);
  }

  @Test
  void testRunFullUpdate() {
    when(settings.getNextAnalyticsTableUpdate()).thenReturn(dateB);

    assertFalse(job.runFullUpdate(dateA));
    assertTrue(job.runFullUpdate(dateC));
  }

  @Test
  void testRunFullUpdateNullNextUpdate() {
    when(settings.getNextAnalyticsTableUpdate()).thenReturn(new Date(0L));

    assertTrue(job.runFullUpdate(dateA));
    assertTrue(job.runFullUpdate(dateC));
  }
}
