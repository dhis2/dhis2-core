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

import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryItemFilterBuilderTest {

  @Mock private OrganisationUnitResolver organisationUnitResolver;

  private QueryItemFilterBuilder target;

  @BeforeEach
  void setUp() {
    target =
        new QueryItemFilterBuilder(organisationUnitResolver, new PostgreSqlAnalyticsSqlBuilder());
  }

  @Test
  void extractFiltersAsSqlKeepsExplicitOrgUnitsForOrgUnitDataElementFilters() {
    // Given
    OrganisationUnit userOrgUnit = createOrganisationUnit('A');
    DataElement orgUnitDataElement = createDataElement('B');
    orgUnitDataElement.setValueType(ValueType.ORGANISATION_UNIT);

    QueryItem item =
        new QueryItem(
            orgUnitDataElement,
            null,
            orgUnitDataElement.getValueType(),
            orgUnitDataElement.getAggregationType(),
            null);
    QueryFilter filter = new QueryFilter(QueryOperator.IN, "OU_GROUP-tDZVQ1WtwpA;ImspTQPwCqd");
    item.addFilter(filter);

    EventQueryParams params =
        new EventQueryParams.Builder().withUserOrgUnits(List.of(userOrgUnit)).build();

    // Union semantics: group members plus the explicit org unit
    when(organisationUnitResolver.resolveOrgUnits(same(filter), anyList(), same(item)))
        .thenReturn("ImspTQPwCqd;O6uvpzGd5pu;fdc6uOvgoji");

    // When
    String sql = target.extractFiltersAsSql(item, "event_ou", params);

    // Then
    assertEquals("event_ou in ('ImspTQPwCqd','O6uvpzGd5pu','fdc6uOvgoji')", sql);
    verify(organisationUnitResolver).resolveOrgUnits(filter, List.of(userOrgUnit), item);
  }
}
