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

import static org.apache.commons.collections4.CollectionUtils.isEqualCollection;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParamType.DIMENSIONS;
import static org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset.emptyElementWithOffset;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlParameterManager;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.Test;

class OrganisationUnitConditionTest {
  @Test
  void testTeiOuMultipleOusProduceCorrectSql() {
    List<String> ous = List.of("ou1", "ou2");
    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        stubDimensionIdentifier(ous, null, null);
    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setOuMode(OrganisationUnitSelectionMode.SELECTED);

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .commonRaw(requestParams)
            .build();

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(contextParams, sqlParameterManager);

    OrganisationUnitCondition organisationUnitCondition =
        OrganisationUnitCondition.of(dimensionIdentifier, queryContext);

    String render = organisationUnitCondition.render();

    assertEquals("t_1.\"ou\" in (:1)", render);
    assertEquals(ous, queryContext.getParametersPlaceHolder().get("1"));
  }

  @Test
  void testChildrenOuModeProduceCorrectSql() {
    List<String> ous = List.of("ou1", "ou2");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        stubDimensionIdentifier(
            ous,
            null,
            null,
            ouId -> {
              OrganisationUnit organisationUnit = mock(OrganisationUnit.class);
              when(organisationUnit.getUid()).thenReturn(ouId);
              when(organisationUnit.getChildren())
                  .thenReturn(
                      Set.of(
                          newOrganisationalUnit(ouId + "_children1"),
                          newOrganisationalUnit(ouId + "_children2")));
              return organisationUnit;
            });

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setOuMode(OrganisationUnitSelectionMode.CHILDREN);

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .commonRaw(requestParams)
            .build();

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(contextParams, sqlParameterManager);

    OrganisationUnitCondition organisationUnitCondition =
        OrganisationUnitCondition.of(dimensionIdentifier, queryContext);

    String render = organisationUnitCondition.render();

    List<String> expected =
        ous.stream().flatMap(ouId -> Stream.of(ouId + "_children1", ouId + "_children2")).toList();

    assertEquals("t_1.\"ou\" in (:1)", render);
    assertTrue(
        isEqualCollection(
            expected, (Collection<?>) queryContext.getParametersPlaceHolder().get("1")));
  }

  @Test
  void testDescendantOuModeProduceCorrectSql() {
    List<String> ous = List.of("ou1", "ou2");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        stubDimensionIdentifier(
            ous,
            null,
            null,
            ouId -> {
              OrganisationUnit organisationUnit = mock(OrganisationUnit.class);
              when(organisationUnit.getUid()).thenReturn(ouId);
              when(organisationUnit.getLevel()).thenReturn(1);
              return organisationUnit;
            });

    CommonRequestParams requestParams = new CommonRequestParams();

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .commonRaw(requestParams)
            .build();

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(contextParams, sqlParameterManager);

    OrganisationUnitCondition organisationUnitCondition =
        OrganisationUnitCondition.of(dimensionIdentifier, queryContext);

    String render = organisationUnitCondition.render();

    assertEquals("(t_1.\"uidlevel1\" = :1 or t_1.\"uidlevel1\" = :2)", render);
  }

  @Test
  void testEmptyOuProduceFalse() {
    List<String> ous = List.of();
    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        stubDimensionIdentifier(ous, null, null);

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setOuMode(OrganisationUnitSelectionMode.SELECTED);

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .commonRaw(requestParams)
            .build();

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(contextParams, sqlParameterManager);

    OrganisationUnitCondition organisationUnitCondition =
        OrganisationUnitCondition.of(dimensionIdentifier, queryContext);

    String render = organisationUnitCondition.render();

    assertEquals("false", render);
    assertTrue(queryContext.getParametersPlaceHolder().isEmpty());
  }

  @Test
  void testTeiOuSingleOusProduceCorrectSql() {
    List<String> ous = List.of("ou1");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        stubDimensionIdentifier(ous, null, null);

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setOuMode(OrganisationUnitSelectionMode.SELECTED);

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .commonRaw(requestParams)
            .build();

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(contextParams, sqlParameterManager);

    OrganisationUnitCondition organisationUnitCondition =
        OrganisationUnitCondition.of(dimensionIdentifier, queryContext);

    String render = organisationUnitCondition.render();

    assertEquals("t_1.\"ou\" = :1", render);
    assertEquals(ous.get(0), queryContext.getParametersPlaceHolder().get("1"));
  }

  @Test
  void testEventOuSingleOusProduceCorrectSql() {
    List<String> ous = List.of("ou1");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        stubDimensionIdentifier(ous, "Z8z5uu61HAb", "tO8L1aBitDm");

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setOuMode(OrganisationUnitSelectionMode.SELECTED);

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .commonRaw(requestParams)
            .build();

    SqlParameterManager sqlParameterManager = new SqlParameterManager();

    QueryContext queryContext = QueryContext.of(contextParams, sqlParameterManager);
    OrganisationUnitCondition organisationUnitCondition =
        OrganisationUnitCondition.of(dimensionIdentifier, queryContext);

    String statement = organisationUnitCondition.render();

    assertEquals("\"Z8z5uu61HAb.tO8L1aBitDm\".\"ou\" = :1", statement);
    assertEquals(ous.get(0), queryContext.getParametersPlaceHolder().get("1"));
  }

  @Test
  void testEventOuMultipleOusProduceCorrectSql() {
    List<String> ous = List.of("ou1", "ou2");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        stubDimensionIdentifier(ous, "Z8z5uu61HAb", "tO8L1aBitDm");

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setOuMode(OrganisationUnitSelectionMode.SELECTED);

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .commonRaw(requestParams)
            .typedParsed(
                TrackedEntityQueryParams.builder()
                    .trackedEntityType(stubTrackedEntityType("T2d3uj69RAb"))
                    .build())
            .build();

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(contextParams, sqlParameterManager);

    OrganisationUnitCondition organisationUnitCondition =
        OrganisationUnitCondition.of(dimensionIdentifier, queryContext);

    String statement = organisationUnitCondition.render();

    assertEquals("\"Z8z5uu61HAb.tO8L1aBitDm\".\"ou\" in (:1)", statement);
    assertEquals(ous, queryContext.getParametersPlaceHolder().get("1"));
  }

  @Test
  void testEnrollmentOuSingleOusProduceCorrectSql() {
    List<String> ous = List.of("ou1");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        stubDimensionIdentifier(ous, "Z8z5uu61HAb", null);

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setOuMode(OrganisationUnitSelectionMode.SELECTED);

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .commonRaw(requestParams)
            .typedParsed(
                TrackedEntityQueryParams.builder()
                    .trackedEntityType(stubTrackedEntityType("T2d3uj69RAb"))
                    .build())
            .build();

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(contextParams, sqlParameterManager);

    OrganisationUnitCondition organisationUnitCondition =
        OrganisationUnitCondition.of(dimensionIdentifier, queryContext);

    String statement = organisationUnitCondition.render();

    assertEquals("\"Z8z5uu61HAb\".\"ou\" = :1", statement);
    assertEquals(ous.get(0), queryContext.getParametersPlaceHolder().get("1"));
  }

  @Test
  void testEnrollmentOuMultipleOusProduceCorrectSql() {
    List<String> ous = List.of("ou1", "ou2");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        stubDimensionIdentifier(ous, "Z8z5uu61HAb", null);

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setOuMode(OrganisationUnitSelectionMode.SELECTED);

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .commonRaw(requestParams)
            .typedParsed(
                TrackedEntityQueryParams.builder()
                    .trackedEntityType(stubTrackedEntityType("T2d3uj69RAb"))
                    .build())
            .build();

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(contextParams, sqlParameterManager);
    OrganisationUnitCondition organisationUnitCondition =
        OrganisationUnitCondition.of(dimensionIdentifier, queryContext);

    String statement = organisationUnitCondition.render();

    assertEquals("\"Z8z5uu61HAb\".\"ou\" in (:1)", statement);
    assertEquals(ous, queryContext.getParametersPlaceHolder().get("1"));
  }

  private TrackedEntityType stubTrackedEntityType(String uid) {
    TrackedEntityType tet = new TrackedEntityType();
    tet.setUid(uid);

    return tet;
  }

  private DimensionIdentifier<DimensionParam> stubDimensionIdentifier(
      List<String> ous, String programUid, String programStageUid) {
    return stubDimensionIdentifier(ous, programUid, programStageUid, this::newOrganisationalUnit);
  }

  private DimensionIdentifier<DimensionParam> stubDimensionIdentifier(
      List<String> ous,
      String programUid,
      String programStageUid,
      Function<String, OrganisationUnit> orgUnitCreator) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            new BaseDimensionalObject(
                "ou", ORGANISATION_UNIT, ous.stream().map(orgUnitCreator).toList()),
            DIMENSIONS,
            UID,
            ous);

    ElementWithOffset<Program> program = emptyElementWithOffset();
    ElementWithOffset<ProgramStage> programStage = emptyElementWithOffset();

    if (isNotBlank(programUid)) {
      Program p = new Program();
      p.setUid(programUid);
      program = ElementWithOffset.of(p, null);
    }

    if (isNotBlank(programStageUid)) {
      ProgramStage ps = new ProgramStage();
      ps.setUid(programStageUid);
      programStage = ElementWithOffset.of(ps, null);
    }

    return DimensionIdentifier.of(program, programStage, dimensionParam);
  }

  private OrganisationUnit newOrganisationalUnit(String ous) {
    OrganisationUnit organisationUnit = new OrganisationUnit();
    organisationUnit.setUid(ous);
    return organisationUnit;
  }
}
