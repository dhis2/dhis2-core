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
package org.hisp.dhis.datavalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nonnull;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataValueUtilTest {

  @Test
  @DisplayName("Creates a new DataValue with the expected new category option combo")
  void newDataValueWithCocTest() {
    // given
    DataValue originalDataValue = getDataValue();

    // new category option combo to use
    CategoryOptionCombo newCoc = new CategoryOptionCombo();
    newCoc.setName("New COC");

    // when
    DataValue newDataValue =
        DataValueUtil.dataValueWithNewCatOptionCombo.apply(originalDataValue, newCoc);

    // then
    assertWithNewValue(newDataValue, "New COC", "COC");
  }

  @Test
  @DisplayName("Creates a new DataValue with the expected new attribute option combo")
  void newDataValueWithAocTest() {
    // given
    DataValue originalDataValue = getDataValue();

    // new attribute option combo to use
    CategoryOptionCombo newAoc = new CategoryOptionCombo();
    newAoc.setName("New AOC");

    // when
    DataValue newDataValue =
        DataValueUtil.dataValueWithNewAttrOptionCombo.apply(originalDataValue, newAoc);

    // then
    assertWithNewValue(newDataValue, "New AOC", "AOC");
  }

  @Test
  @DisplayName("Creates a new DataValue with the expected new data element")
  void newDataValueWithDeTest() {
    // given
    DataValue originalDataValue = getDataValue();

    // new attribute option combo to use
    DataElement newDe = new DataElement("New DE");

    // when
    DataValue newDataValue =
        DataValueUtil.dataValueWithNewDataElement.apply(originalDataValue, newDe);

    // then
    assertWithNewValue(newDataValue, "New DE", "DE");
  }

  private void assertWithNewValue(
      @Nonnull DataValue dv, @Nonnull String newValue, @Nonnull String property) {
    if (property.equals("DE")) {
      assertEquals(newValue, dv.getDataElement().getName());
    } else assertEquals("test DE", dv.getDataElement().getName());
    assertEquals("test Period", dv.getPeriod().getName());
    assertEquals("test Org Unit", dv.getSource().getName());
    if (property.equals("COC")) {
      assertEquals(newValue, dv.getCategoryOptionCombo().getName());
    } else assertEquals("test COC", dv.getCategoryOptionCombo().getName());
    if (property.equals("AOC")) {
      assertEquals(newValue, dv.getAttributeOptionCombo().getName());
    } else assertEquals("test AOC", dv.getAttributeOptionCombo().getName());
    assertEquals("test value", dv.getValue());
    assertEquals("test user", dv.getStoredBy());
    assertEquals(DateUtils.toMediumDate("2024-11-15"), dv.getLastUpdated());
    assertEquals("test comment", dv.getComment());
    assertTrue(dv.isFollowup());
    assertTrue(dv.isDeleted());
    assertEquals(DateUtils.toMediumDate("2024-07-25"), dv.getCreated());
  }

  private DataValue getDataValue() {
    DataElement dataElement = new DataElement("test DE");
    Period period = new Period();
    period.setName("test Period");
    OrganisationUnit orgUnit = new OrganisationUnit("test Org Unit");
    CategoryOptionCombo coc = new CategoryOptionCombo();
    coc.setName("test COC");
    CategoryOptionCombo aoc = new CategoryOptionCombo();
    aoc.setName("test AOC");

    DataValue dv =
        DataValue.builder()
            .dataElement(dataElement)
            .period(period)
            .source(orgUnit)
            .categoryOptionCombo(coc)
            .attributeOptionCombo(aoc)
            .value("test value")
            .storedBy("test user")
            .lastUpdated(DateUtils.toMediumDate("2024-11-15"))
            .comment("test comment")
            .followup(true)
            .deleted(true)
            .build();
    dv.setCreated(DateUtils.toMediumDate("2024-07-25"));
    return dv;
  }
}
