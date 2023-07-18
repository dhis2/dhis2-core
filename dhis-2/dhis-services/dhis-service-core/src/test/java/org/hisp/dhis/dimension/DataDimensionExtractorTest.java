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
package org.hisp.dhis.dimension;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT_OPERAND;
import static org.hisp.dhis.common.DimensionItemType.INDICATOR;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_INDICATOR;
import static org.hisp.dhis.common.DimensionItemType.REPORTING_RATE;

import java.util.Set;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DataDimensionExtractor}.
 *
 * @author maikel arabori
 */
class DataDimensionExtractorTest {
  @Test
  void testGetAtomicIdsForDataElement() {
    // Given
    final DimensionalItemId dataElementItem = new DimensionalItemId(DATA_ELEMENT, "id0");
    final Set<DimensionalItemId> someItemIds = newHashSet(dataElementItem);

    // When
    final SetMap<Class<? extends IdentifiableObject>, String> result =
        new DataDimensionExtractor(null).getAtomicIds(someItemIds);

    // Then
    assertThat(result.size(), is(equalTo(1)));
    assertThat(result.get(DataElement.class), containsInAnyOrder(dataElementItem.getId0()));
  }

  @Test
  void testGetAtomicIdsForDataElementOperand() {
    // Given
    final DimensionalItemId dataElementOperandItem =
        new DimensionalItemId(DATA_ELEMENT_OPERAND, "id0", "id1", "id2");

    final Set<DimensionalItemId> someItemIds = newHashSet(dataElementOperandItem);

    // When
    final SetMap<Class<? extends IdentifiableObject>, String> result =
        new DataDimensionExtractor(null).getAtomicIds(someItemIds);

    // Then
    assertThat(result.size(), is(equalTo(2)));
    assertThat(result.get(DataElement.class), containsInAnyOrder(dataElementOperandItem.getId0()));
    assertThat(
        result.get(CategoryOptionCombo.class),
        containsInAnyOrder(dataElementOperandItem.getId1(), dataElementOperandItem.getId2()));
  }

  @Test
  void testGetAtomicIdsForIndicator() {
    // Given
    final DimensionalItemId indicatorItem = new DimensionalItemId(INDICATOR, "id0");
    final Set<DimensionalItemId> someItemIds = newHashSet(indicatorItem);

    // When
    final SetMap<Class<? extends IdentifiableObject>, String> result =
        new DataDimensionExtractor(null).getAtomicIds(someItemIds);

    // Then
    assertThat(result.size(), is(equalTo(1)));
    assertThat(result.get(Indicator.class), containsInAnyOrder(indicatorItem.getId0()));
  }

  @Test
  void testGetAtomicIdsForReportingRate() {
    // Given
    final DimensionalItemId reportingRateItem =
        new DimensionalItemId(REPORTING_RATE, "id0", "REPORTING_RATE");
    final Set<DimensionalItemId> someItemIds = newHashSet(reportingRateItem);

    // When
    final SetMap<Class<? extends IdentifiableObject>, String> result =
        new DataDimensionExtractor(null).getAtomicIds(someItemIds);

    // Then
    assertThat(result.size(), is(equalTo(1)));
    assertThat(result.get(DataSet.class), containsInAnyOrder(reportingRateItem.getId0()));
  }

  @Test
  void testGetAtomicIdsForProgramDataElement() {
    // Given
    final DimensionalItemId programDataElementItem =
        new DimensionalItemId(PROGRAM_DATA_ELEMENT, "id0", "id1");

    final Set<DimensionalItemId> someItemIds = newHashSet(programDataElementItem);

    // When
    final SetMap<Class<? extends IdentifiableObject>, String> result =
        new DataDimensionExtractor(null).getAtomicIds(someItemIds);

    // Then
    assertThat(result.size(), is(equalTo(2)));
    assertThat(result.get(Program.class), containsInAnyOrder(programDataElementItem.getId0()));
    assertThat(result.get(DataElement.class), containsInAnyOrder(programDataElementItem.getId1()));
  }

  @Test
  void testGetAtomicIdsForProgramAttribute() {
    // Given
    final DimensionalItemId programAttributeItem =
        new DimensionalItemId(PROGRAM_ATTRIBUTE, "id0", "id1");
    final Set<DimensionalItemId> someItemIds = newHashSet(programAttributeItem);

    // When
    final SetMap<Class<? extends IdentifiableObject>, String> result =
        new DataDimensionExtractor(null).getAtomicIds(someItemIds);

    // Then
    assertThat(result.size(), is(equalTo(2)));
    assertThat(result.get(Program.class), containsInAnyOrder(programAttributeItem.getId0()));
    assertThat(
        result.get(TrackedEntityAttribute.class),
        containsInAnyOrder(programAttributeItem.getId1()));
  }

  @Test
  void testGetAtomicIdsForProgramIndicator() {
    // Given
    final DimensionalItemId programIndicatorItem = new DimensionalItemId(PROGRAM_INDICATOR, "id0");
    final Set<DimensionalItemId> someItemIds = newHashSet(programIndicatorItem);

    // When
    final SetMap<Class<? extends IdentifiableObject>, String> result =
        new DataDimensionExtractor(null).getAtomicIds(someItemIds);

    // Then
    assertThat(result.size(), is(equalTo(1)));
    assertThat(
        result.get(ProgramIndicator.class), containsInAnyOrder(programIndicatorItem.getId0()));
  }
}
