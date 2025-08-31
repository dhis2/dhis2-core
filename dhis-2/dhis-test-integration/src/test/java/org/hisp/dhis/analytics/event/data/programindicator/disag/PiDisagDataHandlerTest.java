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
package org.hisp.dhis.analytics.event.data.programindicator.disag;

import static org.hisp.dhis.analytics.DataQueryParams.VALUE_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.common.DimensionConstants.DATA_COLLAPSED_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.program.ProgramCategoryMappingValidator;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jim Grace
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@ExtendWith(MockitoExtension.class)
class PiDisagDataHandlerTest extends AbstractPiDisagTest {

  @Autowired private ProgramCategoryMappingValidator mappingValidator;

  @Mock private SqlRowSet sqlRowSet;

  private PiDisagInfoInitializer infoInitializer;

  private Grid grid;

  @Override
  @BeforeAll
  protected void setUp() {
    super.setUp();
    infoInitializer = new PiDisagInfoInitializer(mappingValidator);

    grid = new ListGrid();
    grid.addHeader(new GridHeader(DATA_COLLAPSED_DIM_ID));
    grid.addHeader(new GridHeader(ORGUNIT_DIM_ID));
    grid.addHeader(new GridHeader(PERIOD_DIM_ID));
    grid.addHeader(new GridHeader(VALUE_ID, VALUE_HEADER_NAME, ValueType.NUMBER, false, false));
    grid.addRow();
    grid.addValue("de1");
    grid.addValue("ou2");
    grid.addValue("pe1");
    grid.addValue(3);
  }

  @Test
  void testAddCocAndAoc_ForDataValueSet() {

    // Given
    EventQueryParams params =
        infoInitializer.getParamsWithDisaggregationInfo(dataValueSetEventQueryParams);

    List<Object> row = new ArrayList<>();
    row.add("TshaiD9eise");
    row.add("IIpo1jem6na");
    row.add("hOaxuch8Eg3");
    row.add(42);

    String expectedDx =
        "TshaiD9eise"
            + COMPOSITE_DIM_OBJECT_PLAIN_SEP
            + cocAC.getUid()
            + COMPOSITE_DIM_OBJECT_PLAIN_SEP
            + cocEG.getUid();

    when(sqlRowSet.getString(category1.getUid())).thenReturn(optionA.getUid());
    when(sqlRowSet.getString(category2.getUid())).thenReturn(optionC.getUid());
    when(sqlRowSet.getString(category3.getUid())).thenReturn(optionE.getUid());
    when(sqlRowSet.getString(category4.getUid())).thenReturn(optionG.getUid());

    // When
    boolean result = PiDisagDataHandler.addCocAndAoc(params, grid, row, sqlRowSet);

    // Then
    assertTrue(result);
    assertEquals(expectedDx, row.get(0));
    assertEquals("IIpo1jem6na", row.get(1));
    assertEquals("hOaxuch8Eg3", row.get(2));
    assertEquals(42, row.get(3));
    assertEquals(4, row.size());
  }

  @Test
  void testAddCocAndAoc_WithoutDataValueSet() {

    // Given
    EventQueryParams params =
        infoInitializer.getParamsWithDisaggregationInfo(nonDataValueSetEventQueryParams);

    List<Object> row = new ArrayList<>();
    row.add("TshaiD9eise");
    row.add("IIpo1jem6na");
    row.add("hOaxuch8Eg3");

    String expectedDx = "TshaiD9eise";

    // When
    boolean result = PiDisagDataHandler.addCocAndAoc(params, grid, row, sqlRowSet);

    // Then
    assertTrue(result);
    assertEquals(expectedDx, row.get(0));
    assertEquals("IIpo1jem6na", row.get(1));
    assertEquals("hOaxuch8Eg3", row.get(2));
    assertEquals(3, row.size());
  }

  @Test
  void testAddCocAndAoc_WithMissingData() {

    // Given
    EventQueryParams params =
        infoInitializer.getParamsWithDisaggregationInfo(dataValueSetEventQueryParams);

    List<Object> row = new ArrayList<>();
    row.add("TshaiD9eise");
    row.add(null);
    row.add("hOaxuch8Eg3");

    String expectedDx = "TshaiD9eise";

    // When
    boolean result = PiDisagDataHandler.addCocAndAoc(params, grid, row, sqlRowSet);

    // Then
    assertFalse(result);
    assertEquals(expectedDx, row.get(0));
    assertNull(row.get(1));
    assertEquals("hOaxuch8Eg3", row.get(2));
    assertEquals(3, row.size());
  }
}
