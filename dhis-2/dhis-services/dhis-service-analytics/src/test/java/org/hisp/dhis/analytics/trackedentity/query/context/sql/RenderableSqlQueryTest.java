/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.trackedentity.query.context.sql;

import java.util.stream.Stream;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.GroupableCondition;
import org.hisp.dhis.analytics.common.query.IndexedOrder;
import org.hisp.dhis.analytics.common.query.LimitOffset;
import org.hisp.dhis.analytics.common.query.Order;
import org.hisp.dhis.analytics.common.query.Table;
import org.hisp.dhis.common.SortDirection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for {@link RenderableSqlQuery}. */
class RenderableSqlQueryTest {

  @ParameterizedTest
  @MethodSource("provideTestCases")
  void testRenderQueryMatches(String expected, RenderableSqlQuery query) {
    Assertions.assertEquals(expected, query.render());
  }

  private static Stream<Arguments> provideTestCases() {
    return Stream.of(
        Arguments.of(
            """
            select t1."field1" as "alias1", t2."field2" as "alias2" from table1 t1 where condition order by orderField nulls last limit 10 offset 20""",
            RenderableSqlQuery.builder()
                .selectField(Field.of("t1", () -> "field1", "alias1"))
                .selectField(Field.of("t2", () -> "field2", "alias2"))
                .selectField(Field.of("t1", () -> "field1", "alias1"))
                .mainTable(Table.of(() -> "table1", () -> "t1"))
                .limitOffset(LimitOffset.of(10, 20, false))
                .orderClause(IndexedOrder.of(1, Order.of(() -> "orderField", SortDirection.ASC)))
                .groupableCondition(GroupableCondition.of("group", () -> "condition"))
                .build()),
        Arguments.of(
            """
            select t1."field1" as "alias1" from table1 t1 where condition1 order by orderField nulls last limit 10 offset 20""",
            RenderableSqlQuery.builder()
                .selectField(Field.of("t1", () -> "field1", "alias1"))
                .selectField(Field.of("t2", () -> "field2", "alias2").asVirtual())
                .selectField(Field.of("t1", () -> "field1", "alias1"))
                .mainTable(Table.of(() -> "table1", () -> "t1"))
                .limitOffset(LimitOffset.of(10, 20, false))
                .orderClause(IndexedOrder.of(1, Order.of(() -> "orderField", SortDirection.ASC)))
                .groupableCondition(GroupableCondition.of("group", () -> "condition1"))
                .build()));
  }
}
