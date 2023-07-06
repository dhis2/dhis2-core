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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class QueryItemTest {

  private Option opA;

  private Option opB;

  private Option opC;

  private OptionSet osA;

  private Legend leA;

  private Legend leB;

  private Legend leC;

  private LegendSet lsA;

  private Program prA;

  private Program prB;

  private DataElement deA;

  private DataElement deB;

  private RepeatableStageParams rspA;

  private RepeatableStageParams rspB;

  @BeforeEach
  void before() {
    opA = new Option("OptionA", "CODEA");
    opA.setUid("UIDA");
    opB = new Option("OptionB", "CODEB");
    opB.setUid("UIDB");
    opC = new Option("OptionC", "CODEC");
    opC.setUid("UIDC");
    osA = new OptionSet("OptionSetA", ValueType.TEXT, Lists.newArrayList(opA, opB, opC));
    leA = new Legend("LegendA", 0d, 1d, "", "");
    leA.setUid("UIDA");
    leB = new Legend("LegendB", 1d, 2d, "", "");
    leB.setUid("UIDB");
    leC = new Legend("LegendC", 3d, 4d, "", "");
    leC.setUid("UIDC");
    lsA = new LegendSet("LegendSetA", "", Sets.newHashSet(leA, leB, leC));
    prA = new Program("ProgramA");
    prA.setUid("PRUIDA");
    prB = new Program("ProgramB");
    prB.setUid("PRUIDB");
    deA = new DataElement("DataElementA");
    deA.setOptionSet(osA);
    deB = new DataElement("DataElementB");
    deB.setLegendSets(Lists.newArrayList(lsA));

    rspA = new RepeatableStageParams();
    rspA.setStartIndex(1);
    rspA.setCount(2);

    rspB = new RepeatableStageParams();
    rspB.setStartIndex(10);
    rspB.setCount(20);
  }

  @Test
  void testGetOptionSetQueryFilterItems() {
    QueryItem qiA = new QueryItem(deA, null, ValueType.TEXT, AggregationType.SUM, osA);
    qiA.addFilter(new QueryFilter(QueryOperator.IN, "CODEA;CODEB"));
    List<String> expected = Lists.newArrayList("UIDA", "UIDB");
    assertEquals(expected, qiA.getOptionSetFilterItemsOrAll());
    QueryItem qiB = new QueryItem(deA, null, ValueType.TEXT, AggregationType.SUM, osA);
    expected = Lists.newArrayList("UIDA", "UIDB", "UIDC");
    assertEquals(expected, qiB.getOptionSetFilterItemsOrAll());
  }

  @Test
  void testGet() {
    QueryItem qiA = new QueryItem(deB, lsA, ValueType.TEXT, AggregationType.SUM, null);
    qiA.addFilter(new QueryFilter(QueryOperator.IN, "UIDA;UIDB"));
    qiA.setRepeatableStageParams(rspA);
    List<String> expected = Lists.newArrayList("UIDA", "UIDB");
    assertEquals(expected, qiA.getLegendSetFilterItemsOrAll());
    QueryItem qiB = new QueryItem(deB, lsA, ValueType.TEXT, AggregationType.SUM, null);
    expected = Lists.newArrayList("UIDA", "UIDB", "UIDC");
    assertEquals(expected, qiB.getLegendSetFilterItemsOrAll());
    assertEquals(rspA, qiA.getRepeatableStageParams());
    assertEquals(rspA.toString(), qiA.getRepeatableStageParams().toString());
  }

  @Test
  void testEquality() {
    // Unique
    QueryItem qiA = new QueryItem(deA, prA, null, ValueType.TEXT, AggregationType.NONE, null);
    // Duplicate
    QueryItem qiB = new QueryItem(deA, prA, null, ValueType.TEXT, AggregationType.NONE, null);
    // of
    // 'qiA'
    // Unique
    QueryItem qiC = new QueryItem(deA, prB, null, ValueType.TEXT, AggregationType.NONE, null);
    // Unique
    QueryItem qiD = new QueryItem(deA);
    // Duplicate of 'qiD'
    QueryItem qiE = new QueryItem(deA);
    // Unique
    QueryItem qiF = new QueryItem(deB);
    Set<QueryItem> items = Sets.newHashSet(qiA, qiB, qiC, qiD, qiE, qiF);
    assertEquals(4, items.size());
  }
}
