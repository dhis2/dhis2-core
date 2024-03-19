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
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.PrefixedDimension;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
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
  void testReturnedDimensionsHaveNoDuplicatesWhenProgramAttributes() {

    when(baseDimensionMapper.map(any(), any()))
        .thenAnswer(
            invocation -> {
              PrefixedDimension prefixedDimension = invocation.getArgument(0);
              return DimensionResponse.builder()
                  .dimensionType(prefixedDimension.getDimensionType())
                  .id(prefixedDimension.getPrefix() + "." + prefixedDimension.getItem().getUid())
                  .uid(prefixedDimension.getItem().getUid())
                  .build();
            });
    when(baseDimensionMapper.supports(any())).thenReturn(true);

    List<DimensionResponse> dimensionResponse =
        dimensionMapperService.toDimensionResponse(
            mockDimensions(DimType.PROGRAM_ATTRIBUTE), TeiAnalyticsPrefixStrategy.INSTANCE, true);

    assertEquals(4, dimensionResponse.size());

    Collection<String> dimensionResponseUids =
        dimensionResponse.stream().map(DimensionResponse::getUid).toList();

    assertEquals(List.of("uid1", "uid2", "uid3", "repeated"), dimensionResponseUids);
  }

  @Test
  void testReturnedDimensionsHaveDuplicatesWhenDataElements() {

    when(baseDimensionMapper.map(any(), any()))
        .thenAnswer(
            invocation -> {
              PrefixedDimension prefixedDimension = invocation.getArgument(0);
              return DimensionResponse.builder()
                  .dimensionType(prefixedDimension.getDimensionType())
                  .id(prefixedDimension.getPrefix() + "." + prefixedDimension.getItem().getUid())
                  .uid(prefixedDimension.getItem().getUid())
                  .build();
            });
    when(baseDimensionMapper.supports(any())).thenReturn(true);

    List<DimensionResponse> dimensionResponse =
        dimensionMapperService.toDimensionResponse(
            mockDimensions(DimType.DATA_ELEMENT), TeiAnalyticsPrefixStrategy.INSTANCE, true);

    assertEquals(5, dimensionResponse.size());

    Collection<String> dimensionResponseIds =
        dimensionResponse.stream().map(DimensionResponse::getId).toList();

    assertEquals(
        List.of("p1.s1.uid1", "p1.s1.uid2", "p1.s1.uid3", "p1.s1.repeated", "p1.s2.repeated"),
        dimensionResponseIds);
  }

  @Getter
  @RequiredArgsConstructor
  private enum DimType {
    PROGRAM_ATTRIBUTE(TrackedEntityAttribute::new),
    DATA_ELEMENT(ProgramStageDataElement::new);
    private final Supplier<BaseIdentifiableObject> instanceSupplier;
  }

  private Collection<PrefixedDimension> mockDimensions(DimType dimType) {
    return Stream.of("p1.s1.uid1", "p1.s1.uid2", "p1.s1.uid3", "p1.s1.repeated", "p1.s2.repeated")
        .map(uid -> asItem(uid, dimType))
        .toList();
  }

  private PrefixedDimension asItem(String uid, DimType dimType) {
    String[] split = uid.split("\\.");
    return PrefixedDimension.builder()
        .program((Program) buildItem(split[0], Program::new))
        .programStage((ProgramStage) buildItem(split[1], ProgramStage::new))
        .item(buildItem(split[2], dimType.getInstanceSupplier()))
        .dimensionType(dimType.name())
        .build();
  }

  private BaseIdentifiableObject buildItem(String uid, Supplier<BaseIdentifiableObject> supplier) {
    BaseIdentifiableObject item = supplier.get();
    item.setUid(uid);
    return item;
  }
}
