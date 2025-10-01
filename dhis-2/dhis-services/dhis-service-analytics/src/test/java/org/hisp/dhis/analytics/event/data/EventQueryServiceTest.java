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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createPeriod;
import static org.hisp.dhis.test.TestBase.createPeriodDimensions;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo.Data;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo.Settings;
import org.hisp.dhis.analytics.data.handler.SchemeIdResponseMapper;
import org.hisp.dhis.analytics.event.EventAnalyticsManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.analytics.table.model.Partitions;
import org.hisp.dhis.analytics.tracker.MetadataItemsHandler;
import org.hisp.dhis.analytics.tracker.SchemeIdHandler;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.user.SystemUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for DefaultEventAnalyticsService.
 *
 * @author maikel arabori
 */
@ExtendWith(MockitoExtension.class)
class EventQueryServiceTest {
  @Mock private EventQueryValidator queryValidator;

  @Mock private MetadataItemsHandler metadataHandler;

  @Mock private SchemeIdHandler schemeIdHandler;

  @Mock private AnalyticsSecurityManager securityManager;

  @Mock private EventQueryValidator eventQueryValidator;

  @Mock private EventAnalyticsManager eventAnalyticsManager;

  @Mock private EventQueryPlanner queryPlanner;

  @Mock private SqlBuilder sqlBuilder;

  @Mock private SchemeIdResponseMapper schemeIdResponseMapper;

  @InjectMocks private EventQueryService eventQueryService;

  @BeforeAll
  static void setup() {
    injectSecurityContextNoSettings(new SystemUser());
  }

  @Test
  void testOutputSchemeWhenSchemeIsSet() {
    IdScheme codeScheme = IdScheme.CODE;
    OrganisationUnit mockOrgUnit = createOrganisationUnit('A');
    Program mockProgram = createProgram('A', null, null, Sets.newHashSet(mockOrgUnit), null);
    EventQueryParams mockParams = mockEventQueryParams(mockOrgUnit, mockProgram, codeScheme);
    SchemeInfo mockSchemeInfo =
        new SchemeInfo(mockSchemeSettings(mockParams), mockDataSettings(mockParams));

    doNothing().when(securityManager).decideAccessEventQuery(mockParams);
    when(securityManager.withUserConstraints(mockParams)).thenReturn(mockParams);
    when(queryPlanner.planEventQuery(any(EventQueryParams.class))).thenReturn(mockParams);

    eventQueryService.getEvents(mockParams);

    verify(schemeIdResponseMapper, atMost(1)).getSchemeIdResponseMap(mockSchemeInfo);
  }

  @Test
  void testOutputSchemeWhenNoSchemeIsSet() {
    IdScheme noScheme = null;
    OrganisationUnit mockOrgUnit = createOrganisationUnit('A');
    Program mockProgram = createProgram('A', null, null, Sets.newHashSet(mockOrgUnit), null);
    EventQueryParams mockParams = mockEventQueryParams(mockOrgUnit, mockProgram, noScheme);
    SchemeInfo mockSchemeInfo =
        new SchemeInfo(mockSchemeSettings(mockParams), mockDataSettings(mockParams));

    doNothing().when(securityManager).decideAccessEventQuery(mockParams);
    when(securityManager.withUserConstraints(mockParams)).thenReturn(mockParams);
    when(queryPlanner.planEventQuery(any(EventQueryParams.class))).thenReturn(mockParams);

    eventQueryService.getEvents(mockParams);

    verify(schemeIdResponseMapper, never()).getSchemeIdResponseMap(mockSchemeInfo);
  }

  private EventQueryParams mockEventQueryParams(
      OrganisationUnit mockOrgUnit, Program mockProgram, IdScheme scheme) {
    return new EventQueryParams.Builder()
        .withPeriods(createPeriodDimensions("2000Q1"), "monthly")
        .withPartitions(new Partitions())
        .withOrganisationUnits(getList(mockOrgUnit))
        .withProgram(mockProgram)
        .withOutputIdScheme(scheme)
        .build();
  }

  private Data mockDataSettings(EventQueryParams eventQueryParams) {
    return Data.builder()
        .program(eventQueryParams.getProgram())
        .organizationUnits(eventQueryParams.getOrganisationUnits())
        .build();
  }

  private Settings mockSchemeSettings(EventQueryParams eventQueryParams) {
    return Settings.builder().outputIdScheme(eventQueryParams.getOutputIdScheme()).build();
  }
}
