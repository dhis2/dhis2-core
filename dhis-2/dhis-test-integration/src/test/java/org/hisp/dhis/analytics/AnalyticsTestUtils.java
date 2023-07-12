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
package org.hisp.dhis.analytics;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.Sets;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Map;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * @author Henning Haakonsen
 */
public class AnalyticsTestUtils {

  /**
   * Configure org unit hierarchy like so:
   *
   * <p>A / \ B C / \ D E
   *
   * @param ouA root
   * @param ouB leftRoot
   * @param ouC rightRoot
   * @param ouD leftB
   * @param ouE rightB
   */
  public static void configureHierarchy(
      OrganisationUnit ouA,
      OrganisationUnit ouB,
      OrganisationUnit ouC,
      OrganisationUnit ouD,
      OrganisationUnit ouE) {
    ouA.getChildren().addAll(Sets.newHashSet(ouB, ouC));
    ouB.setParent(ouA);
    ouC.setParent(ouA);
    ouB.getChildren().addAll(Sets.newHashSet(ouD, ouE));
    ouD.setParent(ouB);
    ouE.setParent(ouB);
  }

  /**
   * Test if values from keyValue corresponds with values in aggregatedResultMapping. Also test for
   * null values.
   *
   * @param keyValue expected results
   * @param aggregatedResultData aggregated results
   */
  public static void assertResultGrid(Map<String, Object> keyValue, Grid aggregatedResultData) {
    assertNotNull(aggregatedResultData);
    for (int i = 0; i < aggregatedResultData.getRows().size(); i++) {
      int numberOfDimensions = aggregatedResultData.getRows().get(0).size() - 1;
      StringBuilder key = new StringBuilder();
      for (int j = 0; j < numberOfDimensions; j++) {
        key.append(aggregatedResultData.getValue(i, j).toString());
        if (j != numberOfDimensions - 1) {
          key.append("-");
        }
      }
      Number expected = (Number) keyValue.get(key.toString());
      Number actual = (Number) aggregatedResultData.getValue(i, numberOfDimensions);
      assertNotNull(expected, "Did not find " + key + " in provided results");
      assertNotNull(aggregatedResultData.getRow(i));
      assertEquals(
          expected, actual, "Value for " + key + " was " + actual + ", not expected " + expected);
    }
  }

  /**
   * Test if values from keyValue corresponds with values in aggregatedDataValueSet. Also test for
   * null values.
   *
   * @param keyValue expected results
   * @param aggregatedResultSet aggregated values
   */
  public static void assertResultSet(
      Map<String, Object> keyValue, DataValueSet aggregatedResultSet) {
    for (org.hisp.dhis.dxf2.datavalue.DataValue dataValue : aggregatedResultSet.getDataValues()) {
      String key =
          dataValue.getDataElement() + "-" + dataValue.getOrgUnit() + "-" + dataValue.getPeriod();
      assertNotNull(keyValue.get(key));
      try {
        Number actual = NumberFormat.getInstance().parse(dataValue.getValue());
        Number expected = (Number) keyValue.get(key);
        assertEquals(
            expected,
            actual,
            "Value for key: '" + key + "' not matching expected value: '" + expected + "'");
      } catch (ParseException e) {
        fail(format("Failed to parse number: %s. Error: %s", dataValue.getValue(), e.getMessage()));
      }
    }
  }
}
