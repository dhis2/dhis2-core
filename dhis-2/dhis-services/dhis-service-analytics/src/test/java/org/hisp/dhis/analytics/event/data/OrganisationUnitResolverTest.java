/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramStage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.data.DimensionalObjectProvider;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link OrganisationUnitResolver}. */
@ExtendWith(MockitoExtension.class)
class OrganisationUnitResolverTest {

  @Mock private DimensionalObjectProvider dimensionalObjectProducer;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private IdentifiableObjectManager idObjectManager;

  @Mock private AnalyticsSqlBuilder sqlBuilder;

  @InjectMocks private OrganisationUnitResolver resolver;

  @Test
  void buildStageOuCteContextUsesResolvedLevelOrgUnitsAndExcludesBoundary() {
    // Given
    OrganisationUnit resolvedDistrict = new OrganisationUnit("Bo");
    resolvedDistrict.setUid("O6uvpzGd5pu");
    resolvedDistrict.setPath("/ImspTQPwCqd/O6uvpzGd5pu");

    Program program = createProgram('A');
    ProgramStage programStage = createProgramStage('S', program);
    QueryItem stageOuItem =
        new QueryItem(
            new BaseDimensionalItemObject("ou"),
            program,
            null,
            ValueType.ORGANISATION_UNIT,
            AggregationType.NONE,
            null);
    stageOuItem.setProgramStage(programStage);
    stageOuItem.addFilter(new QueryFilter(QueryOperator.IN, "LEVEL-wjP19dkFeIk;ImspTQPwCqd"));

    OrganisationUnitResolver resolverWithSqlBuilder =
        new OrganisationUnitResolver(
            dimensionalObjectProducer,
            organisationUnitService,
            idObjectManager,
            new PostgreSqlAnalyticsSqlBuilder());

    EventQueryParams params = new EventQueryParams.Builder().withUserOrgUnits(List.of()).build();

    when(dimensionalObjectProducer.getOrgUnitDimensionUid(
            List.of("LEVEL-wjP19dkFeIk", "ImspTQPwCqd"), List.of(), true))
        .thenReturn(List.of(resolvedDistrict.getUid()));
    when(organisationUnitService.getOrganisationUnitsByUid(List.of(resolvedDistrict.getUid())))
        .thenReturn(List.of(resolvedDistrict));

    // When
    OrganisationUnitResolver.StageOuCteContext context =
        resolverWithSqlBuilder.buildStageOuCteContext(stageOuItem, params);

    // Then
    assertEquals("\"uidlevel2\"", context.valueColumn());
    assertEquals("\"uidlevel2\" in ('O6uvpzGd5pu')", context.filterCondition());
    assertFalse(context.filterCondition().contains("ImspTQPwCqd"));
    assertFalse(context.filterCondition().contains("uidlevel1"));
    assertTrue(context.additionalSelectColumns().contains("\"ouname\" as ev_ouname"));
  }

  @Test
  void loadOrgUnitDimensionalItemResolvesNumericLevel() {
    // Given
    OrganisationUnitLevel level = new OrganisationUnitLevel(3, "Chiefdom");
    level.setUid("tTUf91fCytl");
    when(organisationUnitService.getOrganisationUnitLevelByLevel(3)).thenReturn(level);

    // When
    DimensionalItemObject result = resolver.loadOrgUnitDimensionalItem("LEVEL-3", IdScheme.UID);

    // Then
    assertNotNull(result);
    assertEquals("tTUf91fCytl", result.getUid());
    assertEquals("Chiefdom", result.getName());
  }

  @Test
  void loadOrgUnitDimensionalItemResolvesLevelByUid() {
    // Given
    OrganisationUnitLevel level = new OrganisationUnitLevel(3, "Chiefdom");
    level.setUid("tTUf91fCytl");
    when(idObjectManager.getObject(OrganisationUnitLevel.class, IdScheme.UID, "tTUf91fCytl"))
        .thenReturn(level);

    // When
    DimensionalItemObject result =
        resolver.loadOrgUnitDimensionalItem("LEVEL-tTUf91fCytl", IdScheme.UID);

    // Then
    assertNotNull(result);
    assertEquals("tTUf91fCytl", result.getUid());
    assertEquals("Chiefdom", result.getName());
  }

  @Test
  void loadOrgUnitDimensionalItemReturnsNullForUnknownLevel() {
    // Given
    when(organisationUnitService.getOrganisationUnitLevelByLevel(9)).thenReturn(null);

    // When
    DimensionalItemObject result = resolver.loadOrgUnitDimensionalItem("LEVEL-9", IdScheme.UID);

    // Then
    assertNull(result);
  }
}
