/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.data.handler;

import static org.hisp.dhis.analytics.DataQueryParams.newBuilder;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.dataelement.DataElementOperand.TotalType.AOC_ONLY;
import static org.hisp.dhis.dataelement.DataElementOperand.TotalType.COC_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementOperand.TotalType;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DataHandler}. */
class DataHandlerTest {

  @Test
  void testOperandDataQueryParamsOnlyOperands() {
    // Given
    DataElement dataElement = new DataElement("NameA");
    dataElement.setUid("uid1234567A");
    dataElement.setCode("CodeA");

    DataElementOperand dataElementOperand = new DataElementOperand(dataElement, null);

    DataQueryParams stubParams =
        newBuilder()
            .addDimension(
                new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(dataElementOperand)))
            .build();

    TotalType anyTotalType = COC_ONLY;

    // When
    DataHandler dataHandler = withNullDependencies();
    DataQueryParams params =
        dataHandler.getOperandDataQueryParams(
            stubParams, List.of(dataElementOperand), anyTotalType);

    // Then
    assertEquals(1, params.getDimensions().size());
  }

  @Test
  void testOperandDataQueryParamsWithCatOptionCombo() {
    // Given
    DataElement dataElement = new DataElement("NameA");
    dataElement.setUid("uid1234567A");
    dataElement.setCode("CodeA");

    CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
    categoryOptionCombo.setName("NameC");
    categoryOptionCombo.setUid("uid1234567C");
    categoryOptionCombo.setCode("CodeC");

    DataElementOperand dataElementOperand =
        new DataElementOperand(dataElement, categoryOptionCombo);

    DataQueryParams stubParams =
        newBuilder()
            .addDimension(
                new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(dataElementOperand)))
            .build();

    // When
    DataHandler dataHandler = withNullDependencies();
    DataQueryParams params =
        dataHandler.getOperandDataQueryParams(stubParams, List.of(dataElementOperand), COC_ONLY);

    // Then
    assertEquals(2, params.getDimensions().size());
  }

  @Test
  void testOperandDataQueryParamsWithCatOptionComboInFilter() {
    // Given
    DataElement dataElement = new DataElement("NameA");
    dataElement.setUid("uid1234567A");
    dataElement.setCode("CodeA");

    CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
    categoryOptionCombo.setName("NameC");
    categoryOptionCombo.setUid("uid1234567C");
    categoryOptionCombo.setCode("CodeC");

    DataElementOperand dataElementOperand =
        new DataElementOperand(dataElement, categoryOptionCombo);

    DataQueryParams stubParams =
        newBuilder()
            .addFilter(
                new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(dataElementOperand)))
            .build();

    // When
    DataHandler dataHandler = withNullDependencies();
    DataQueryParams params =
        dataHandler.getOperandDataQueryParams(stubParams, List.of(dataElementOperand), COC_ONLY);

    // Then
    assertEquals(0, params.getDimensions().size());
    assertEquals(3, params.getFilters().size());
  }

  @Test
  void testOperandDataQueryParamsWithAttrOptionCombo() {
    // Given
    DataElement dataElement = new DataElement("NameA");
    dataElement.setUid("uid1234567A");
    dataElement.setCode("CodeA");

    CategoryOptionCombo attributeOptionCombo = new CategoryOptionCombo();
    attributeOptionCombo.setName("NameAt");
    attributeOptionCombo.setUid("uid1234567C");
    attributeOptionCombo.setCode("CodeAt");

    DataElementOperand dataElementOperand =
        new DataElementOperand(dataElement, null, attributeOptionCombo);

    DataQueryParams stubParams =
        newBuilder()
            .addDimension(
                new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(dataElementOperand)))
            .build();

    // When
    DataHandler dataHandler = withNullDependencies();
    DataQueryParams params =
        dataHandler.getOperandDataQueryParams(stubParams, List.of(dataElementOperand), AOC_ONLY);

    // Then
    assertEquals(2, params.getDimensions().size());
  }

  @Test
  void testOperandDataQueryParamsWithAttrOptionComboInFilter() {
    // Given
    DataElement dataElement = new DataElement("NameA");
    dataElement.setUid("uid1234567A");
    dataElement.setCode("CodeA");

    CategoryOptionCombo attributeOptionCombo = new CategoryOptionCombo();
    attributeOptionCombo.setName("NameAt");
    attributeOptionCombo.setUid("uid1234567C");
    attributeOptionCombo.setCode("CodeAt");

    DataElementOperand dataElementOperand =
        new DataElementOperand(dataElement, null, attributeOptionCombo);

    DataQueryParams stubParams =
        newBuilder()
            .addFilter(
                new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(dataElementOperand)))
            .build();

    // When
    DataHandler dataHandler = withNullDependencies();
    DataQueryParams params =
        dataHandler.getOperandDataQueryParams(stubParams, List.of(dataElementOperand), AOC_ONLY);

    // Then
    assertEquals(0, params.getDimensions().size());
    assertEquals(3, params.getFilters().size());
  }

  private DataHandler withNullDependencies() {
    return new DataHandler(null, null, null, null, null, null, null, null, null, null);
  }
}
