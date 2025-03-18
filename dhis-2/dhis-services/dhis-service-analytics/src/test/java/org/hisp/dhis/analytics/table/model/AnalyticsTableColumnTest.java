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
package org.hisp.dhis.analytics.table.model;

import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.hisp.dhis.db.model.constraint.Nullable.NOT_NULL;
import static org.hisp.dhis.db.model.constraint.Nullable.NULL;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.db.model.Collation;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class AnalyticsTableColumnTest {
  @Test
  void testIsNotNull() {
    AnalyticsTableColumn colA =
        AnalyticsTableColumn.builder()
            .name("dx")
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("dx")
            .build();

    AnalyticsTableColumn colB =
        AnalyticsTableColumn.builder()
            .name("value")
            .dataType(DOUBLE)
            .nullable(NULL)
            .selectExpression("value")
            .build();

    assertTrue(colA.isNotNull());
    assertFalse(colB.isNotNull());
  }

  @Test
  void testHasCollation() {
    AnalyticsTableColumn colA =
        AnalyticsTableColumn.builder()
            .name("dx")
            .dataType(CHARACTER_11)
            .collation(Collation.DEFAULT)
            .selectExpression("dx")
            .build();

    AnalyticsTableColumn colB =
        AnalyticsTableColumn.builder()
            .name("ou")
            .dataType(CHARACTER_11)
            .collation(Collation.C)
            .selectExpression("ou")
            .build();

    AnalyticsTableColumn colC =
        AnalyticsTableColumn.builder()
            .name("value")
            .dataType(DOUBLE)
            .selectExpression("value")
            .build();

    assertFalse(colA.hasCollation());
    assertTrue(colB.hasCollation());
    assertFalse(colC.hasCollation());
  }

  @Test
  void testIsSkipIndex() {
    AnalyticsTableColumn colA =
        AnalyticsTableColumn.builder()
            .name("value")
            .dataType(DOUBLE)
            .selectExpression("value")
            .skipIndex(Skip.SKIP)
            .build();

    AnalyticsTableColumn colB =
        AnalyticsTableColumn.builder()
            .name("ou")
            .dataType(CHARACTER_11)
            .selectExpression("ou")
            .skipIndex(Skip.INCLUDE)
            .build();

    assertTrue(colA.isSkipIndex());
    assertFalse(colB.isSkipIndex());
  }
}
