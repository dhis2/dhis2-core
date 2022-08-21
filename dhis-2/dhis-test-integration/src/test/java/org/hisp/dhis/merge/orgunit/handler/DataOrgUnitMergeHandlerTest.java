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
package org.hisp.dhis.merge.orgunit.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.DataApproval;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.merge.orgunit.DataMergeStrategy;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeRequest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.integration.NonTransactionalIntegrationTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
class DataOrgUnitMergeHandlerTest extends NonTransactionalIntegrationTest
{

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private DataApprovalService dataApprovalService;

    @Autowired
    private UserService userService;

    @Autowired
    private DataOrgUnitMergeHandler handler;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private DataElement deA;

    private DataElement deB;

    private CategoryOptionCombo cocA;

    private Period peA;

    private Period peB;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    private User usA;

    private DataApprovalLevel dlA;

    private DataApprovalWorkflow dwA;

    @Override
    public void setUpTest()
    {
        cocA = categoryService.getDefaultCategoryOptionCombo();
        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        idObjectManager.save( deA );
        idObjectManager.save( deB );
        PeriodType monthly = periodService.getPeriodTypeByClass( MonthlyPeriodType.class );
        peA = monthly.createPeriod( "202101" );
        peB = monthly.createPeriod( "202102" );
        periodService.addPeriod( peA );
        periodService.addPeriod( peB );
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        ouC = createOrganisationUnit( 'C' );
        idObjectManager.save( ouA );
        idObjectManager.save( ouB );
        idObjectManager.save( ouC );
        usA = makeUser( "A" );
        userService.addUser( usA );
        dlA = new DataApprovalLevel( "DataApprovalLevelA", 1 );
        idObjectManager.save( dlA );
        dwA = new DataApprovalWorkflow( "DataApprovalWorkflowA", monthly, Sets.newHashSet( dlA ) );
        idObjectManager.save( dwA );
    }

    @Test
    void testMergeDataValues()
    {
        addDataValues( createDataValue( deA, peA, ouA, cocA, cocA, "10", date( 2021, 1, 1 ), date( 2021, 1, 1 ) ),
            createDataValue( deA, peA, ouB, cocA, cocA, "11", date( 2021, 2, 1 ), date( 2021, 2, 1 ) ),
            createDataValue( deB, peA, ouA, cocA, cocA, "12", date( 2021, 3, 1 ), date( 2021, 3, 1 ) ),
            createDataValue( deB, peA, ouB, cocA, cocA, "13", date( 2021, 4, 1 ), date( 2021, 4, 1 ) ) );
        assertEquals( 2, getDataValueCount( ouA ) );
        assertEquals( 2, getDataValueCount( ouB ) );
        assertEquals( 0, getDataValueCount( ouC ) );
        OrgUnitMergeRequest request = new OrgUnitMergeRequest.Builder().addSource( ouA ).addSource( ouB )
            .withTarget( ouC ).withDataValueMergeStrategy( DataMergeStrategy.LAST_UPDATED ).build();
        handler.mergeDataValues( request );
        assertEquals( 0, getDataValueCount( ouA ) );
        assertEquals( 0, getDataValueCount( ouB ) );
        assertEquals( 2, getDataValueCount( ouC ) );
    }

    @Test
    void testMergeDataApprovals()
    {
        addDataApprovals( new DataApproval( dlA, dwA, peA, ouA, cocA, false, date( 2021, 1, 1 ), usA ),
            new DataApproval( dlA, dwA, peA, ouB, cocA, false, date( 2021, 2, 1 ), usA ),
            new DataApproval( dlA, dwA, peB, ouA, cocA, false, date( 2021, 3, 1 ), usA ),
            new DataApproval( dlA, dwA, peB, ouB, cocA, false, date( 2021, 4, 1 ), usA ) );
        assertEquals( 2, getDataApprovalCount( ouA ) );
        assertEquals( 2, getDataApprovalCount( ouB ) );
        assertEquals( 0, getDataApprovalCount( ouC ) );
        OrgUnitMergeRequest request = new OrgUnitMergeRequest.Builder().addSource( ouA ).addSource( ouB )
            .withTarget( ouC ).withDataApprovalMergeStrategy( DataMergeStrategy.LAST_UPDATED ).build();
        handler.mergeDataApprovals( request );
        assertEquals( 0, getDataApprovalCount( ouA ) );
        assertEquals( 0, getDataApprovalCount( ouB ) );
        assertEquals( 2, getDataApprovalCount( ouC ) );
    }

    private long getDataValueCount( OrganisationUnit target )
    {
        final String sql = "select count(*) from datavalue dv where dv.sourceid = :target_id";
        return jdbcTemplate.queryForObject( sql, new MapSqlParameterSource().addValue( "target_id", target.getId() ),
            Long.class );
    }

    private long getDataApprovalCount( OrganisationUnit target )
    {
        final String sql = "select count(*) from dataapproval da where da.organisationunitid = :target_id";
        return jdbcTemplate.queryForObject( sql, new MapSqlParameterSource().addValue( "target_id", target.getId() ),
            Long.class );
    }

    private void addDataValues( DataValue... dataValues )
    {
        Stream.of( dataValues ).forEach( dataValueService::addDataValue );
    }

    private void addDataApprovals( DataApproval... dataApprovals )
    {
        Stream.of( dataApprovals ).forEach( dataApprovalService::addDataApproval );
    }
}
