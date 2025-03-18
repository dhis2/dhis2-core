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
package org.hisp.dhis.analytics.trackedentity.query;

import static org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset.emptyElementWithOffset;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlParameterManager;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.junit.jupiter.api.Test;

class TrackedEntityAttributeConditionTest {
  @Test
  void testProgramAttributeConditionProduceCorrectSql() {

    String attr = "attr";
    List<String> values = List.of("eq:v1");
    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        getProgramAttributeDimensionIdentifier("attr", values);

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(null, sqlParameterManager);

    TrackedEntityAttributeCondition trackedEntityAttributeCondition =
        TrackedEntityAttributeCondition.of(dimensionIdentifier, queryContext);

    String render = trackedEntityAttributeCondition.render();

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
                items.stream().map(BaseDimensionalItemObject::new).toList()),
            DimensionParamType.DIMENSIONS,
            UID,
            items);

    return DimensionIdentifier.of(
        emptyElementWithOffset(), emptyElementWithOffset(), dimensionParam);
  }
}
