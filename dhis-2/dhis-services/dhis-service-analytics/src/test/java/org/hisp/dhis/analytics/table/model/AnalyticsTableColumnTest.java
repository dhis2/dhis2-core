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
            .build()
            .withName("dx")
            .withDataType(CHARACTER_11)
            .withNullable(NOT_NULL)
            .withSelectExpression("dx");

    AnalyticsTableColumn colB =
        AnalyticsTableColumn.builder()
            .build()
            .withName("value")
            .withDataType(DOUBLE)
            .withNullable(NULL)
            .withSelectExpression("value");

    assertTrue(colA.isNotNull());
    assertFalse(colB.isNotNull());
  }

  @Test
  void testHasCollation() {
    AnalyticsTableColumn colA =
        AnalyticsTableColumn.builder()
            .build()
            .withName("dx")
            .withDataType(CHARACTER_11)
            .withCollation(Collation.DEFAULT)
            .withSelectExpression("dx");

    AnalyticsTableColumn colB =
        AnalyticsTableColumn.builder()
            .build()
            .withName("ou")
            .withDataType(CHARACTER_11)
            .withCollation(Collation.C)
            .withSelectExpression("ou");

    AnalyticsTableColumn colC =
        AnalyticsTableColumn.builder()
            .build()
            .withName("value")
            .withDataType(DOUBLE)
            .withSelectExpression("value");

    assertFalse(colA.hasCollation());
    assertTrue(colB.hasCollation());
    assertFalse(colC.hasCollation());
  }

  @Test
  void testIsSkipIndex() {
    AnalyticsTableColumn colA =
        AnalyticsTableColumn.builder()
            .build()
            .withName("value")
            .withDataType(DOUBLE)
            .withSelectExpression("value")
            .withSkipIndex(Skip.SKIP);

    AnalyticsTableColumn colB =
        AnalyticsTableColumn.builder()
            .build()
            .withName("ou")
            .withDataType(CHARACTER_11)
            .withSelectExpression("ou")
            .withSkipIndex(Skip.INCLUDE);

    assertTrue(colA.isSkipIndex());
    assertFalse(colB.isSkipIndex());
  }
}
