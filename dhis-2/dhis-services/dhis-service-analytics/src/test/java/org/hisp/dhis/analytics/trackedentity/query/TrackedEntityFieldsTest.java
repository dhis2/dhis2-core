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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.AggregateQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlParameterManager;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.jupiter.api.Test;

class TrackedEntityFieldsTest {

  @Test
  void getAggregateGridHeadersOmitsStaticFieldsAndKeepsOuDimension() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        aggregateContextParamsWithOuDimension();

    Set<GridHeader> headers = TrackedEntityFields.getAggregateGridHeaders(contextParams);

    assertEquals(1, headers.size());
    assertEquals("ou", headers.iterator().next().getName());
    // No per-TEI static header leaked in:
    assertTrue(headers.stream().map(GridHeader::getName).noneMatch("ouname"::equals));
  }

  /**
   * Grounded test: the aggregate {@code ou} group-by column is produced at runtime by the real
   * {@link AggregateQueryBuilder} (via {@code Field.ofDimensionIdentifier}), which yields a select
   * field carrying neither a field alias nor a dimension identifier key. This test reproduces that
   * runtime representation - a select field that cannot be matched back to its dimension via {@code
   * Field.getDimensionIdentifier()} - and asserts that {@link
   * TrackedEntityFields#getAggregateGridHeaders} still emits an {@code ou} header. The header must
   * be named {@code ou} to line up with the SQL result column consumed by the aggregate grid.
   */
  @Test
  void getAggregateGridHeadersKeepsOuDimensionFromRealSelectFields() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        aggregateContextParamsWithOuDimension();

    // Exercise the real aggregate select-field construction so the ou field matches runtime.
    QueryContext queryContext = QueryContext.of(contextParams, new SqlParameterManager());
    RenderableSqlQuery renderableSqlQuery =
        new AggregateQueryBuilder()
            .buildSqlQuery(
                queryContext,
                List.of(),
                contextParams.getCommonParsed().getDimensionIdentifiers(),
                List.of());
    List<Field> selectFields = renderableSqlQuery.getSelectFields();
    // Guard: the ou select field really is alias-free (this is what hid the bug).
    assertTrue(
        selectFields.stream().anyMatch(f -> isBlank(f.getDimensionIdentifier())),
        "expected an alias-free aggregate select field from the real AggregateQueryBuilder");

    Set<GridHeader> headers = TrackedEntityFields.getAggregateGridHeaders(contextParams);

    assertTrue(
        headers.stream().map(GridHeader::getName).anyMatch("ou"::equals),
        "aggregate headers should include the requested ou dimension, but were: "
            + headers.stream().map(GridHeader::getName).toList());
  }

  private ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams>
      aggregateContextParamsWithOuDimension() {
    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder().aggregate(true).build();

    return ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
        .typedParsed(trackedEntityQueryParams)
        .commonRaw(new CommonRequestParams().withDimension(Set.of("ou")))
        .commonParsed(
            CommonParsedParams.builder()
                .dimensionIdentifiers(List.of(stubOuDimension("ou1")))
                .build())
        .build();
  }

  private DimensionIdentifier<DimensionParam> stubOuDimension(String ou) {
    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid(ou);
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of(orgUnit)),
            DimensionParamType.DIMENSIONS,
            UID,
            List.of(ou));
    return DimensionIdentifier.of(
            ElementWithOffset.emptyElementWithOffset(),
            ElementWithOffset.emptyElementWithOffset(),
            dimensionParam)
        .withDefaultGroupId();
  }
}
