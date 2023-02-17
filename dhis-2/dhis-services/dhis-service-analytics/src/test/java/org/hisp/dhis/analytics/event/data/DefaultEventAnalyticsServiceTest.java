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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createPeriod;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.Partitions;
import org.hisp.dhis.analytics.cache.AnalyticsCache;
import org.hisp.dhis.analytics.data.handler.SchemaIdResponseMapper;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EventAnalyticsManager;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;

/**
 * Unit tests for DefaultEventAnalyticsService.
 *
 * @author maikel arabori
 */
@ExtendWith( MockitoExtension.class )
class DefaultEventAnalyticsServiceTest
{
    private DefaultEventAnalyticsService defaultEventAnalyticsService;

    @Mock
    private AnalyticsSecurityManager securityManager;

    @Mock
    private EventQueryValidator eventQueryValidator;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Mock
    private EventAnalyticsManager eventAnalyticsManager;

    @Mock
    private EnrollmentAnalyticsManager enrollmentAnalyticsManager;

    @Mock
    private EventDataQueryService eventDataQueryService;

    @Mock
    private EventQueryPlanner queryPlanner;

    @Mock
    private DatabaseInfo databaseInfo;

    @Mock
    private AnalyticsCache analyticsCache;

    @Mock
    private SchemaIdResponseMapper schemaIdResponseMapper;

    @BeforeEach
    public void setUp()
    {
        defaultEventAnalyticsService = new DefaultEventAnalyticsService( dataElementService,
            trackedEntityAttributeService, eventAnalyticsManager, eventDataQueryService, securityManager, queryPlanner,
            eventQueryValidator, databaseInfo, analyticsCache, enrollmentAnalyticsManager, schemaIdResponseMapper );
    }

    @Test
    void testOutputSchemeWhenSchemeIsSet()
    {
        IdScheme codeScheme = IdScheme.CODE;
        OrganisationUnit mockOrgUnit = createOrganisationUnit( 'A' );
        Program mockProgram = createProgram( 'A', null, null, Sets.newHashSet( mockOrgUnit ), null );
        EventQueryParams mockParams = mockEventQueryParams( mockOrgUnit, mockProgram, codeScheme );

        doNothing().when( securityManager ).decideAccessEventQuery( mockParams );
        when( securityManager.withUserConstraints( mockParams ) ).thenReturn( mockParams );
        doNothing().when( eventQueryValidator ).validate( mockParams );
        when( queryPlanner.planEventQuery( any( EventQueryParams.class ) ) ).thenReturn( mockParams );

        defaultEventAnalyticsService.getEvents( mockParams );

        verify( schemaIdResponseMapper, atMost( 1 ) ).getSchemeIdResponseMap( mockParams );
    }

    @Test
    void testOutputSchemeWhenNoSchemeIsSet()
    {
        IdScheme noScheme = null;
        OrganisationUnit mockOrgUnit = createOrganisationUnit( 'A' );
        Program mockProgram = createProgram( 'A', null, null, Sets.newHashSet( mockOrgUnit ), null );
        EventQueryParams mockParams = mockEventQueryParams( mockOrgUnit, mockProgram, noScheme );

        doNothing().when( securityManager ).decideAccessEventQuery( mockParams );
        when( securityManager.withUserConstraints( mockParams ) ).thenReturn( mockParams );
        doNothing().when( eventQueryValidator ).validate( mockParams );
        when( queryPlanner.planEventQuery( any( EventQueryParams.class ) ) ).thenReturn( mockParams );

        defaultEventAnalyticsService.getEvents( mockParams );

        verify( schemaIdResponseMapper, never() ).getSchemeIdResponseMap( mockParams );
    }

    private EventQueryParams mockEventQueryParams( OrganisationUnit mockOrgUnit, Program mockProgram,
        IdScheme scheme )
    {
        return new EventQueryParams.Builder()
            .withPeriods( getList( createPeriod( "2000Q1" ) ), "monthly" )
            .withPartitions( new Partitions() )
            .withOrganisationUnits( getList( mockOrgUnit ) )
            .withProgram( mockProgram )
            .withOutputIdScheme( scheme )
            .build();
    }
}
