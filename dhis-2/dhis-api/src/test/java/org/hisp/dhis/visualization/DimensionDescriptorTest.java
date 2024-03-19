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
package org.hisp.dhis.visualization;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.visualization.DimensionDescriptor.getDimensionIdentifierFor;

import java.util.List;
import org.junit.jupiter.api.Test;

class DimensionDescriptorTest {

  @Test
  void testHasDimensionWithSuccess() {
    // Given
    final String anyDimensionValue = "dx";
    final DimensionDescriptor aDimensionDescriptor =
        new DimensionDescriptor(anyDimensionValue, DATA_X);
    final String dimensionAbbreviation = "dx";
    // When
    final boolean actualResult = aDimensionDescriptor.hasDimension(dimensionAbbreviation);
    // Then
    assertThat(actualResult, is(true));
  }

  @Test
  void testHasDimensionFails() {
    // Given
    final String anyDimensionValue = "dx";
    final DimensionDescriptor aDimensionDescriptor =
        new DimensionDescriptor(anyDimensionValue, DATA_X);
    final String nonExistingDimensionAbbreviation = "ou";
    // When
    final boolean actualResult =
        aDimensionDescriptor.hasDimension(nonExistingDimensionAbbreviation);
    // Then
    assertThat(actualResult, is(false));
  }

  @Test
  void testRetrieveDescriptiveValue() {
    // Given
    final String anyDimensionDynamicValue = "ZyxerTyuP";
    final List<DimensionDescriptor> someDimensionDescriptors =
        mockDimensionDescriptors(anyDimensionDynamicValue);
    final String theDimensionToRetrieve = "dx";
    // When
    final String actualResult =
        getDimensionIdentifierFor(theDimensionToRetrieve, someDimensionDescriptors);
    // Then
    assertThat(actualResult, is(DATA_X_DIM_ID));
  }

  @Test
  void testRetrieveDescriptiveValueDynamicValue() {
    // Given
    final String theDynamicDimensionToRetrieve = "ZyxerTyuP";
    final List<DimensionDescriptor> someDimensionDescriptors =
        mockDimensionDescriptors(theDynamicDimensionToRetrieve);
    // When
    final String actualResult =
        getDimensionIdentifierFor(theDynamicDimensionToRetrieve, someDimensionDescriptors);
    // Then
    assertThat(actualResult, is(ORGUNIT_DIM_ID));
  }

  private List<DimensionDescriptor> mockDimensionDescriptors(final String orgUnitDynamicDimension) {
    final DimensionDescriptor dx = new DimensionDescriptor("dx", DATA_X);
    final DimensionDescriptor dynamicOu =
        new DimensionDescriptor(orgUnitDynamicDimension, ORGANISATION_UNIT);
    return asList(dx, dynamicOu);
  }
}
