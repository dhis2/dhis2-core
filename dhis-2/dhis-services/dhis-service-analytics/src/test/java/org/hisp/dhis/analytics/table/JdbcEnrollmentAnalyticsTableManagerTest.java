/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.analytics.table;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramTrackedEntityAttribute;
import static org.hisp.dhis.DhisConvenienceTest.createTrackedEntityAttribute;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
public class JdbcEnrollmentAnalyticsTableManagerTest {

    @Mock
    private IdentifiableObjectManager idObjectManager;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private SystemSettingManager systemSettingManager;

    @Mock
    private DataApprovalLevelService dataApprovalLevelService;

    @Mock
    private ResourceTableService resourceTableService;

    @Mock
    private AnalyticsTableHookService tableHookService;

    @Mock
    private StatementBuilder statementBuilder;

    @Mock
    private PartitionManager partitionManager;

    @Mock
    private DatabaseInfo databaseInfo;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private JdbcEnrollmentAnalyticsTableManager subject;

    @Before
    public void setUp()
    {
        ReflectionTestUtils.setField( subject, "statementBuilder", statementBuilder );
        when( jdbcTemplate.queryForList(
                "select distinct(extract(year from psi.executiondate)) from programstageinstance psi inner join programinstance pi on psi.programinstanceid = pi.programinstanceid where pi.programid = 0 and psi.executiondate is not null and psi.deleted is false and psi.executiondate >= '2018-01-01'",
                Integer.class ) ).thenReturn( Lists.newArrayList( 2018, 2019 ) );
    }

    @Test
    public void verifyTeiTypeOrgUnitFetchesOuNameWhenPopulatingEventAnalyticsTable()
    {
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass( String.class );
        when( databaseInfo.isSpatialSupport() ).thenReturn( true );
        Program p1 = createProgram( 'A' );

        TrackedEntityAttribute tea = createTrackedEntityAttribute( 'a', ValueType.ORGANISATION_UNIT );
        tea.setId( 9999 );

        ProgramTrackedEntityAttribute programTrackedEntityAttribute = createProgramTrackedEntityAttribute( p1, tea );

        p1.setProgramAttributes( Lists.newArrayList( programTrackedEntityAttribute ) );

        when( idObjectManager.getAllNoAcl( Program.class ) ).thenReturn( Lists.newArrayList( p1 ) );

        AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder().withLastYears( 2 ).build();

        subject.populateTable( params,
                PartitionUtils.getTablePartitions( subject.getAnalyticsTables( params ) ).get( 0 ) );

        verify( jdbcTemplate ).execute( sql.capture() );

        String ouQuery = "(select ou.name from organisationunit ou where ou.uid = "
                + "(select value from trackedentityattributevalue where trackedentityinstanceid=pi.trackedentityinstanceid and "
                + "trackedentityattributeid=9999)) as \"" + tea.getUid() + "\"";

        assertThat( sql.getValue(), containsString( ouQuery ) );
    }

}