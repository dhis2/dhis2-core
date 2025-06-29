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
package org.hisp.dhis.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.stream.Collectors;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class BaseDimensionalObjectTest {

  private static final EasyRandom rnd = new EasyRandom();

  @Test
  void verifyInstanceCloneObject() {
    BaseDimensionalObject target = new BaseDimensionalObject("test-dimension");
    target.setUid("uid-999999");
    target.setDimensionType(DimensionType.DATA_X);
    target.setDimensionName("test-dimension-name");
    target.setItems(Lists.newArrayList(buildDimensionalItemObject(), buildDimensionalItemObject()));
    target.setFilter("test-filter");
    target.setLegendSet(
        new LegendSet(
            "legend-name", "symbolizer-test", Sets.newHashSet(buildLegend(), buildLegend())));
    target.setAggregationType(AggregationType.AVERAGE);
    target.setDataDimension(true);
    target.setFixed(true);
    target.setDimensionalKeywords(
        new DimensionItemKeywords(
            rnd.objects(BaseIdentifiableObject.class, 5).collect(Collectors.toList())));
    DimensionalObject cloned = DimensionalObjectUtils.copyOf(target);
    assertThat(cloned.getName(), is(target.getName()));
    assertThat(cloned.getUid(), is(target.getUid()));
    assertThat(cloned.getDimensionType(), is(target.getDimensionType()));
    assertThat(cloned.getDimensionName(), is(target.getDimensionName()));
    assertThat(cloned.getItems(), hasSize(2));
    assertThat(
        cloned.getItems(),
        IsIterableContainingInAnyOrder.containsInAnyOrder(
            allOf(
                hasProperty("name", is(target.getItems().get(0).getName())),
                hasProperty("uid", is(target.getItems().get(0).getUid())),
                hasProperty("code", is(target.getItems().get(0).getCode()))),
            allOf(
                hasProperty("name", is(target.getItems().get(1).getName())),
                hasProperty("uid", is(target.getItems().get(1).getUid())),
                hasProperty("code", is(target.getItems().get(1).getCode())))));
    assertThat(cloned.getFilter(), is(target.getFilter()));
    assertThat(cloned.getLegendSet().getName(), is("legend-name"));
    assertThat(cloned.getLegendSet().getSymbolizer(), is("symbolizer-test"));
    assertThat(cloned.getLegendSet().getLegends(), hasSize(2));
    assertThat(cloned.getAggregationType(), is(target.getAggregationType()));
    assertThat(cloned.isDataDimension(), is(target.isDataDimension()));
    assertThat(cloned.isFixed(), is(target.isFixed()));
    assertThat(
        cloned.getDimensionItemKeywords().getKeywords(),
        hasSize(target.getDimensionItemKeywords().getKeywords().size()));
  }

  @Test
  void testConstructorWithFullQualifiedDimension() {
    // When
    DimensionalObject baseDimensionalObject =
        new BaseDimensionalObject("programUid.programStageUid.dimensionUid");

    // Then
    assertTrue(baseDimensionalObject.hasProgram());
    assertTrue(baseDimensionalObject.hasProgramStage());
    assertEquals("programUid", baseDimensionalObject.getProgram().getUid());
    assertEquals("programStageUid", baseDimensionalObject.getProgramStage().getUid());
    assertEquals("dimensionUid", baseDimensionalObject.getUid());
  }

  @Test
  void testConstructorQualifiedDimensionNoStage() {
    // When
    DimensionalObject baseDimensionalObject = new BaseDimensionalObject("programUid.dimensionUid");

    // Then
    assertTrue(baseDimensionalObject.hasProgram());
    assertFalse(baseDimensionalObject.hasProgramStage());
    assertEquals("programUid", baseDimensionalObject.getProgram().getUid());
    assertNull(baseDimensionalObject.getProgramStage());
    assertEquals("dimensionUid", baseDimensionalObject.getUid());
  }

  @Test
  void testConstructorQualifiedDimensionOnlyDimensionItem() {
    // When
    DimensionalObject baseDimensionalObject = new BaseDimensionalObject("dimensionUid");

    // Then
    assertFalse(baseDimensionalObject.hasProgram());
    assertFalse(baseDimensionalObject.hasProgramStage());
    assertNull(baseDimensionalObject.getProgram());
    assertNull(baseDimensionalObject.getProgramStage());
    assertEquals("dimensionUid", baseDimensionalObject.getUid());
  }

  private DimensionalItemObject buildDimensionalItemObject() {
    return rnd.nextObject(BaseDimensionalItemObject.class);
  }

  private Legend buildLegend() {
    return rnd.nextObject(Legend.class);
  }
}
