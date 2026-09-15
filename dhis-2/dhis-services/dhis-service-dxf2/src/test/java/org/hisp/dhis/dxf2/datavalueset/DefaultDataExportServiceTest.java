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
package org.hisp.dhis.dxf2.datavalueset;

import static org.hisp.dhis.test.TestBase.clearSecurityContext;
import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.common.IdCoder;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataExportStore;
import org.hisp.dhis.datavalue.DataExportValue;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.user.SystemUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultDataExportServiceTest {

  @Mock private DataExportStore store;

  @Mock private IdCoder idCoder;

  @BeforeEach
  void setUp() {
    injectSecurityContextNoSettings(new SystemUser());
  }

  @AfterEach
  void tearDown() {
    clearSecurityContext();
  }

  private DataExportParams.Input.InputBuilder validFiltersBuilder() {
    return DataExportParams.Input.builder()
        .dataSet(Set.of(UID.generate().getValue()))
        .orgUnit(Set.of(UID.generate().getValue()))
        .period(Set.of("202201"));
  }

  private DefaultDataExportService service() {
    return new DefaultDataExportService(store, idCoder);
  }

  @Test
  void testExportValues_InvalidLastUpdatedDurationThrowsE2005() {
    DataExportParams.Input params = validFiltersBuilder().lastUpdatedDuration("-5d").build();

    ConflictException ex =
        assertThrows(ConflictException.class, () -> service().exportValues(params));

    assertEquals(ErrorCode.E2005, ex.getCode());
  }

  @Test
  void testExportValues_MalformedLastUpdatedDurationThrowsE2005() {
    DataExportParams.Input params = validFiltersBuilder().lastUpdatedDuration("abc").build();

    ConflictException ex =
        assertThrows(ConflictException.class, () -> service().exportValues(params));

    assertEquals(ErrorCode.E2005, ex.getCode());
  }

  @Test
  void testExportValues_ZeroDurationIsValid() {
    // 0d parses successfully (Duration.ZERO); it's a degenerate "as of right now" filter,
    // not a parse failure, so it must not be rejected the same way "-5d"/"abc" are.
    DataExportParams.Input params = validFiltersBuilder().lastUpdatedDuration("0d").build();

    assertDoesNotThrow(() -> service().exportValues(params));
  }

  @Test
  void testExportValues_BlankLastUpdatedDurationIsIgnored() {
    // a blank (but non-null) value never reaches the parser, so it must not be
    // treated as unparseable and rejected like "-5d"/"abc" are.
    DataExportParams.Input params = validFiltersBuilder().lastUpdatedDuration(" ").build();

    assertDoesNotThrow(() -> service().exportValues(params));
  }

  @Test
  void testExportValues_DefaultCcDataSet_AppliesDefaultAocFilter() throws Exception {
    // Every requested data set uses the default attribute category combo, so the only valid
    // attribute option combo is the default one. It must be applied as an explicit AOC filter
    // so the export query can skip the per-row AOC data-sharing check.
    UID defaultAoc = UID.generate();
    when(store.getDefaultAttributeOptionComboForDataSets(any())).thenReturn(defaultAoc);
    when(store.exportValues(any())).thenReturn(Stream.<DataExportValue>empty());

    service().exportValues(validFiltersBuilder().build());

    ArgumentCaptor<DataExportParams> captor = ArgumentCaptor.forClass(DataExportParams.class);
    verify(store).exportValues(captor.capture());
    assertEquals(List.of(defaultAoc), captor.getValue().getAttributeOptionCombos());
  }

  @Test
  void testExportValues_NonDefaultCcDataSet_NoAocFilter() throws Exception {
    // A requested data set uses a non-default attribute category combo, so the default AOC
    // fast path must not apply and no AOC filter is added.
    when(store.getDefaultAttributeOptionComboForDataSets(any())).thenReturn(null);
    when(store.exportValues(any())).thenReturn(Stream.<DataExportValue>empty());

    service().exportValues(validFiltersBuilder().build());

    ArgumentCaptor<DataExportParams> captor = ArgumentCaptor.forClass(DataExportParams.class);
    verify(store).exportValues(captor.capture());
    assertEquals(List.of(), captor.getValue().getAttributeOptionCombos());
  }

  @Test
  void testExportValues_ExplicitAocGiven_DefaultCcFastPathSkipped() throws Exception {
    // An explicit attributeOptionCombo filter wins and the default-CC resolution must not run.
    UID explicitAoc = UID.generate();
    when(store.exportValues(any())).thenReturn(Stream.<DataExportValue>empty());

    service()
        .exportValues(
            validFiltersBuilder().attributeOptionCombo(Set.of(explicitAoc.getValue())).build());

    ArgumentCaptor<DataExportParams> captor = ArgumentCaptor.forClass(DataExportParams.class);
    verify(store).exportValues(captor.capture());
    assertEquals(List.of(explicitAoc), captor.getValue().getAttributeOptionCombos());
    verify(store, never()).getDefaultAttributeOptionComboForDataSets(any());
  }
}
