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
package org.hisp.dhis.analytics.data;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.subexpression.SubexpressionDimensionItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createPeriod;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jim Grace
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubexpressionPeriodOffsetUtilsTest {
  private DataElement de0 = createDataElement('A');

  private DataElement dem1 = createDataElementAWithPeriodOffset(-1);

  private DataElement dem2 = createDataElementAWithPeriodOffset(-2);

  private DataElement dep1 = createDataElementAWithPeriodOffset(1);

  private String deAUid = de0.getUid();

  private String expression =
      format(
          "subExpression( #{%s} + #{%s}.periodOffset(-1) + #{%s}.periodOffset(-2) + #{%s}.periodOffset(1) )",
          deAUid, deAUid, deAUid, deAUid);

  private List<DimensionalItemObject> items = List.of(de0, dem1, dem2, dep1);

  private SubexpressionDimensionItem subex =
      new SubexpressionDimensionItem(expression, items, null);

  private Period periodA = createPeriod("202309");

  private Period periodB = createPeriod("202310");

  private DataQueryParams params =
      DataQueryParams.newBuilder()
          .withPeriodType("monthly")
          .withPeriods(List.of(periodA, periodB))
          .addDimension(
              new BaseDimensionalObject(DATA_X_DIM_ID, DimensionType.DATA_X, getList(subex)))
          .build();

  @Test
  void testJoinPeriodOffsetValues() {
    String result = SubexpressionPeriodOffsetUtils.joinPeriodOffsetValues(params);
    String expected =
        " join (values"
            + "(-2,'202309','202307'),(-2,'202310','202308'),"
            + "(-1,'202309','202308'),(-1,'202310','202309'),"
            + "(0,'202309','202309'),(0,'202310','202310'),"
            + "(1,'202309','202310'),(1,'202310','202311'))"
            + " as shift (\"delta\", \"reportperiod\", \"dataperiod\") on \"dataperiod\" = \"monthly\"";
    assertEquals(expected, result);
  }

  @Test
  void testGetParamsWithOffsetPeriodsWithoutData() {
    DataQueryParams result =
        SubexpressionPeriodOffsetUtils.getParamsWithOffsetPeriodsWithoutData(params);

    List<DimensionalItemObject> expectedPeriods =
        getPeriodList("202307", "202308", "202309", "202310", "202311");
    assertContainsOnly(expectedPeriods, result.getPeriods());

    assertIsEmpty(result.getAllDataDimensionItems());
  }

  @Test
  void testGetParamsWithDataPeriods() {
    DataQueryParams result = SubexpressionPeriodOffsetUtils.getParamsWithOffsetPeriods(params);

    List<DimensionalItemObject> expectedPeriods =
        getPeriodList("202307", "202308", "202309", "202310", "202311");
    assertContainsOnly(expectedPeriods, result.getPeriods());

    List<DimensionalItemObject> expectedData = List.of(subex);
    assertContainsOnly(expectedData, result.getAllDataDimensionItems());
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private DataElement createDataElementAWithPeriodOffset(int periodOffset) {
    QueryModifiers mods = QueryModifiers.builder().periodOffset(periodOffset).build();
    DataElement de = createDataElement('A');
    de.setQueryMods(mods);
    return de;
  }

  private List<DimensionalItemObject> getPeriodList(String... isoPeriods) {
    return Arrays.stream(isoPeriods)
        .map(PeriodType::getPeriodFromIsoString)
        .map(DimensionalItemObject.class::cast)
        .toList();
  }
}
