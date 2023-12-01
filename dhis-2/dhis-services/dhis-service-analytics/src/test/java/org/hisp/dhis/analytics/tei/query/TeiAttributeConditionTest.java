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
package org.hisp.dhis.analytics.tei.query;

import static org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset.emptyElementWithOffset;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlParameterManager;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.junit.jupiter.api.Test;

class TeiAttributeConditionTest {
  @Test
  void testProgramAttributeConditionProduceCorrectSql() {

    // SETUP
    String attr = "attr";
    List<String> values = List.of("eq:v1");
    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        getProgramAttributeDimensionIdentifier("attr", values);

    // CALL
    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(null, sqlParameterManager);

    TeiAttributeCondition teiAttributeCondition =
        TeiAttributeCondition.of(dimensionIdentifier, queryContext);

    String render = teiAttributeCondition.render();

    assertEquals("\"" + attr + "\" = :1", render);
    assertEquals("v1", queryContext.getParametersPlaceHolder().get("1"));
  }

  private DimensionIdentifier<DimensionParam> getProgramAttributeDimensionIdentifier(
      String attr, List<String> items) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            new BaseDimensionalObject(
                attr,
                DimensionType.PROGRAM_ATTRIBUTE,
                items.stream().map(BaseDimensionalItemObject::new).collect(Collectors.toList())),
            DimensionParamType.DIMENSIONS,
            items);

    return DimensionIdentifier.of(
        emptyElementWithOffset(), emptyElementWithOffset(), dimensionParam);
  }
}
