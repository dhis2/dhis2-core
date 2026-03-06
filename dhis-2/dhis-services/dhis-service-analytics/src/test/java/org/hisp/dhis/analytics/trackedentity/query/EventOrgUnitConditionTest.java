/*
 * Copyright (c) 2004-2025, University of Oslo
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
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParamType.DIMENSIONS;
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
import org.junit.jupiter.api.Test;

class EventOrgUnitConditionTest {

  @Test
  void testEventOuSelectedModeProducesCorrectSql() {
    List<String> ous = List.of("ou1", "ou2");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        stubDimensionIdentifier(ous, "programUid", "programStageUid");

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setOuMode(OrganisationUnitSelectionMode.SELECTED);

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .commonRaw(requestParams)
            .build();

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(contextParams, sqlParameterManager);

    EventOrgUnitCondition eventOrgUnitCondition =
        EventOrgUnitCondition.of(dimensionIdentifier, queryContext);

    String render = eventOrgUnitCondition.render();

    assertEquals("\"programUid.programStageUid\".\"ou\" in (:1)", render);
    assertEquals(ous, queryContext.getParametersPlaceHolder().get("1"));
  }

  @Test
  void testEventOuSingleOuSelectedModeProducesCorrectSql() {
    List<String> ous = List.of("ou1");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        stubDimensionIdentifier(ous, "programUid", "programStageUid");

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setOuMode(OrganisationUnitSelectionMode.SELECTED);

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .commonRaw(requestParams)
            .build();

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(contextParams, sqlParameterManager);

    EventOrgUnitCondition eventOrgUnitCondition =
        EventOrgUnitCondition.of(dimensionIdentifier, queryContext);

    String render = eventOrgUnitCondition.render();

    assertEquals("\"programUid.programStageUid\".\"ou\" = :1", render);
    assertEquals("ou1", queryContext.getParametersPlaceHolder().get("1"));
  }

  @Test
  void testEventOuChildrenModeProducesCorrectSql() {
    List<String> ous = List.of("ou1", "ou2");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        stubDimensionIdentifier(
            ous,
            "programUid",
            "programStageUid",
            ouId -> {
              OrganisationUnit organisationUnit = mock(OrganisationUnit.class);
              when(organisationUnit.getUid()).thenReturn(ouId);
              when(organisationUnit.getChildren())
                  .thenReturn(
                      Set.of(
                          newOrganisationUnit(ouId + "_child1"),
                          newOrganisationUnit(ouId + "_child2")));
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

    EventOrgUnitCondition eventOrgUnitCondition =
        EventOrgUnitCondition.of(dimensionIdentifier, queryContext);

    String render = eventOrgUnitCondition.render();

    // CHILDREN mode includes only the immediate children
    List<String> expected =
        ous.stream().flatMap(ouId -> Stream.of(ouId + "_child1", ouId + "_child2")).toList();

    assertEquals("\"programUid.programStageUid\".\"ou\" in (:1)", render);
    assertTrue(
        isEqualCollection(
            expected, (Collection<?>) queryContext.getParametersPlaceHolder().get("1")));
  }

  @Test
  void testEventOuDescendantsModeProducesCorrectSql() {
    List<String> ous = List.of("ou1");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        stubDimensionIdentifier(
            ous,
            "programUid",
            "programStageUid",
            ouId -> {
              OrganisationUnit organisationUnit = mock(OrganisationUnit.class);
              when(organisationUnit.getUid()).thenReturn(ouId);
              when(organisationUnit.getLevel()).thenReturn(2);
              return organisationUnit;
            });

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setOuMode(OrganisationUnitSelectionMode.DESCENDANTS);

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .commonRaw(requestParams)
            .build();

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(contextParams, sqlParameterManager);

    EventOrgUnitCondition eventOrgUnitCondition =
        EventOrgUnitCondition.of(dimensionIdentifier, queryContext);

    String render = eventOrgUnitCondition.render();

    assertEquals("\"programUid.programStageUid\".\"uidlevel2\" = :1", render);
    assertEquals("ou1", queryContext.getParametersPlaceHolder().get("1"));
  }

  @Test
  void testEmptyOuProducesFalseCondition() {
    List<String> ous = List.of();

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        stubDimensionIdentifier(ous, "programUid", "programStageUid");

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setOuMode(OrganisationUnitSelectionMode.SELECTED);

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .commonRaw(requestParams)
            .build();

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(contextParams, sqlParameterManager);

    EventOrgUnitCondition eventOrgUnitCondition =
        EventOrgUnitCondition.of(dimensionIdentifier, queryContext);

    String render = eventOrgUnitCondition.render();

    assertEquals("false", render);
  }

  private DimensionIdentifier<DimensionParam> stubDimensionIdentifier(
      List<String> ous, String programUid, String programStageUid) {
    return stubDimensionIdentifier(ous, programUid, programStageUid, this::newOrganisationUnit);
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

    Program program = new Program();
    program.setUid(programUid);

    ProgramStage programStage = new ProgramStage();
    programStage.setUid(programStageUid);

    return DimensionIdentifier.of(
        ElementWithOffset.of(program, null),
        ElementWithOffset.of(programStage, null),
        dimensionParam);
  }

  private OrganisationUnit newOrganisationUnit(String uid) {
    OrganisationUnit ou = new OrganisationUnit();
    ou.setUid(uid);
    return ou;
  }
}
