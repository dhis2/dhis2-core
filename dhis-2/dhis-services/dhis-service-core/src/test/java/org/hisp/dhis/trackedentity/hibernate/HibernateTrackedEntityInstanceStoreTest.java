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
package org.hisp.dhis.trackedentity.hibernate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

@ExtendWith( MockitoExtension.class )
class HibernateTrackedEntityInstanceStoreTest
{

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AclService aclService;

    @Mock
    private StatementBuilder statementBuilder;

    @Mock
    private OrganisationUnitStore organisationUnitStore;

    @Mock
    private SystemSettingManager systemSettingManager;

    @InjectMocks
    private HibernateTrackedEntityInstanceStore store;

    @Captor
    ArgumentCaptor<String> sqlQueryCaptor;

    private TrackedEntityInstanceQueryParams params;

    @BeforeEach
    void setUp()
    {
        params = new TrackedEntityInstanceQueryParams();
        params.setOrganisationUnits( Set.of( new OrganisationUnit( "orgUnitUid" ) ) );
        params.setProgram( new Program( "programUid" ) );
        params.setIncludeDeleted( false );
        params.setOrganisationUnitMode( OrganisationUnitSelectionMode.SELECTED );
        params.setTrackedEntityTypes( List.of( new TrackedEntityType( "ARV commodity", "ARV commodity" ) ) );

        SqlRowSet sqlRowSet = Mockito.mock( SqlRowSet.class );
        Mockito.when( jdbcTemplate.queryForRowSet( Mockito.anyString() ) ).thenReturn( sqlRowSet );
    }

    @Test
    void whenOrderingByInactiveThenQueryAndSubqueryContainInactiveField()
    {
        //given
        params.setOrders( List.of( new OrderParam( "inactive", OrderParam.SortDirection.DESC ) ) );

        //when
        store.getTrackedEntityInstanceIds( params );

        //then
        Mockito.verify( jdbcTemplate, Mockito.atLeast( 1 ) ).queryForRowSet( sqlQueryCaptor.capture() );
        String sqlQuery = sqlQueryCaptor.getValue();
        assertAll( () -> assertEquals( 2, countMatches( sqlQuery, ", TEI.inactive" ) ),
            () -> assertEquals( 2, countMatches( sqlQuery, "ORDER BY tei.inactive DESC" ) ) );
    }

    @Test
    void whenOrderingByUpdatedAtThenQueryAndSubqueryContainUpdatedAtField()
    {
        //given
        params.setOrders( List.of( new OrderParam( "updatedAt", OrderParam.SortDirection.ASC ) ) );

        //when
        store.getTrackedEntityInstanceIds( params );

        //then
        Mockito.verify( jdbcTemplate, Mockito.atLeast( 1 ) ).queryForRowSet( sqlQueryCaptor.capture() );
        String sqlQuery = sqlQueryCaptor.getValue();
        assertAll( () -> assertEquals( 2, countMatches( sqlQuery, ", TEI.lastupdated" ) ),
            () -> assertEquals( 2, countMatches( sqlQuery, "ORDER BY tei.lastUpdated ASC" ) ) );
    }

    @Test
    void whenOrderingByUpdatedAtClientThenQueryAndSubqueryContainUpdatedAtClient()
    {
        //given
        params.setOrders( List.of( new OrderParam( "updatedAtClient", OrderParam.SortDirection.ASC ) ) );

        //when
        store.getTrackedEntityInstanceIds( params );

        //then
        Mockito.verify( jdbcTemplate, Mockito.atLeast( 1 ) ).queryForRowSet( sqlQueryCaptor.capture() );
        String sqlQuery = sqlQueryCaptor.getValue();
        assertAll( () -> assertEquals( 2, countMatches( sqlQuery, ", tei.lastUpdatedAtClient" ) ),
            () -> assertEquals( 2, countMatches( sqlQuery, "ORDER BY tei.lastUpdatedAtClient ASC" ) ) );
    }

    @Test
    void whenOrderingByEnrolledAtThenQueryAndSubqueryContainEnrolledAtField()
    {
        //given
        params.setOrders( List.of( new OrderParam( "enrolledAt", OrderParam.SortDirection.DESC ) ) );

        //when
        store.getTrackedEntityInstanceIds( params );

        //then
        Mockito.verify( jdbcTemplate, Mockito.atLeast( 1 ) ).queryForRowSet( sqlQueryCaptor.capture() );
        String sqlQuery = sqlQueryCaptor.getValue();

        assertAll( () -> assertEquals( 1, countMatches( sqlQuery, ", pi.enrollmentDate" ) ),
            () -> assertEquals( 1, countMatches( sqlQuery, ", tei.enrollmentDate" ) ),
            () -> assertEquals( 1, countMatches( sqlQuery, "ORDER BY pi.enrollmentDate DESC" ) ),
            () -> assertEquals( 1, countMatches( sqlQuery, "ORDER BY tei.enrollmentDate DESC" ) ) );
    }

    //@Test
    void whenOrderingByNonStaticFieldThenQueryAndSubqueryContainNonStaticField()
    {
        //given
        params.setOrders( List.of( new OrderParam( "enrolledAt", OrderParam.SortDirection.DESC ),
            new OrderParam( "updatedAtClient", OrderParam.SortDirection.ASC ) ) );

        //when
        store.getTrackedEntityInstanceIds( params );

        //then
        Mockito.verify( jdbcTemplate, Mockito.atLeast( 1 ) ).queryForRowSet( sqlQueryCaptor.capture() );
        String sqlQuery = sqlQueryCaptor.getValue();

        fail();
    }

    @Test
    void whenOrderingByMultipleFieldsThenQueryAndSubqueryContainAllFields()
    {
        //given
        params.setOrders( List.of( new OrderParam( "enrolledAt", OrderParam.SortDirection.DESC ),
            new OrderParam( "updatedAtClient", OrderParam.SortDirection.ASC ) ) );

        //when
        store.getTrackedEntityInstanceIds( params );

        //then
        Mockito.verify( jdbcTemplate, Mockito.atLeast( 1 ) ).queryForRowSet( sqlQueryCaptor.capture() );
        String sqlQuery = sqlQueryCaptor.getValue();
        assertAll(
            () -> assertEquals( 1,
                countMatches( sqlQuery, ", pi.enrollmentDate, tei.lastUpdatedAtClient" ) ),
            () -> assertEquals( 1,
                countMatches( sqlQuery, ", tei.enrollmentDate, tei.lastUpdatedAtClient" ) ),
            () -> assertEquals( 1,
                countMatches( sqlQuery, "ORDER BY pi.enrollmentDate DESC,tei.lastUpdatedAtClient ASC" ) ),
            () -> assertEquals( 1, countMatches( sqlQuery,
                "ORDER BY tei.enrollmentDate DESC,tei.lastUpdatedAtClient ASC" ) ) );
    }

    @Test
    void whenNoOrderParamsProvidedThenQueryAndSubqueryContainDefaultOrderByClause()
    {
        //given
        params.setOrders( Collections.emptyList() );

        //when
        store.getTrackedEntityInstanceIds( params );

        //then
        Mockito.verify( jdbcTemplate, Mockito.atLeast( 1 ) ).queryForRowSet( sqlQueryCaptor.capture() );
        String sqlQuery = sqlQueryCaptor.getValue();

        assertEquals( 2, countMatches( sqlQuery, "ORDER BY TEI.trackedentityinstanceid ASC" ) );
    }

    private int countMatches( String string, String substring )
    {
        Matcher matcher = Pattern.compile( substring ).matcher( string );

        int count = 0;
        while ( matcher.find() )
        {
            count++;
        }

        return count;
    }
}