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
package org.hisp.dhis.webapi.dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.PrefixedDimension;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.webapi.dimension.mappers.BaseDimensionalItemObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DimensionMapperServiceTest {

  private DimensionMapperService dimensionMapperService;
  private BaseDimensionMapper baseDimensionMapper;

  @BeforeEach
  void setUp() {
    baseDimensionMapper = mock(BaseDimensionalItemObjectMapper.class);
    dimensionMapperService = new DimensionMapperService(Collections.singleton(baseDimensionMapper));
  }

  @Test
  void testReturnedDimensionsHaveNoDuplicates() {

    when(baseDimensionMapper.map(any(), any()))
        .thenAnswer(
            invocation -> {
              PrefixedDimension prefixedDimension = invocation.getArgument(0);
              return DimensionResponse.builder().uid(prefixedDimension.getItem().getUid()).build();
            });
    when(baseDimensionMapper.supports(any())).thenReturn(true);

    List<DimensionResponse> dimensionResponse =
        dimensionMapperService.toDimensionResponse(
            mockDimensions(), EnrollmentAnalyticsPrefixStrategy.INSTANCE, true);

    assertEquals(4, dimensionResponse.size());

    Collection<String> dimensionResponseUids =
        dimensionResponse.stream().map(DimensionResponse::getUid).toList();

    assertEquals(List.of("uid1", "uid2", "uid3", "repeated"), dimensionResponseUids);
  }

  private Collection<PrefixedDimension> mockDimensions() {
    return Stream.of("uid1", "uid2", "uid3", "repeated", "repeated")
        .map(this::asPrefixedDimension)
        .toList();
  }

  private PrefixedDimension asPrefixedDimension(String dimension) {
    return PrefixedDimension.builder()
        .item(buildItem(dimension))
        .dimensionType(DimensionType.PROGRAM_ATTRIBUTE.name())
        .build();
  }

  private BaseIdentifiableObject buildItem(String uid) {
    TrackedEntityAttribute item = new TrackedEntityAttribute();
    item.setUid(uid);
    return item;
  }
}
