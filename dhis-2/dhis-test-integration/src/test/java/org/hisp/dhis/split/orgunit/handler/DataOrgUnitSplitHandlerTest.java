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
package org.hisp.dhis.split.orgunit.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.hibernate.SessionFactory;
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
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.split.orgunit.OrgUnitSplitRequest;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
class DataOrgUnitSplitHandlerTest extends SingleSetupIntegrationTestBase
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
    private DataOrgUnitSplitHandler handler;

    @Autowired
    private SessionFactory sessionFactory;

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
            createDataValue( deA, peB, ouA, cocA, cocA, "11", date( 2021, 2, 1 ), date( 2021, 2, 1 ) ) );
        assertEquals( 2, getDataValueCount( ouA ) );
        assertEquals( 0, getDataValueCount( ouB ) );
        assertEquals( 0, getDataValueCount( ouC ) );
        OrgUnitSplitRequest request = new OrgUnitSplitRequest.Builder().withSource( ouA ).addTarget( ouB )
            .addTarget( ouC ).withPrimaryTarget( ouB ).build();
        handler.splitData( request );
        assertEquals( 0, getDataValueCount( ouA ) );
        assertEquals( 2, getDataValueCount( ouB ) );
        assertEquals( 0, getDataValueCount( ouC ) );
    }

    @Test
    void testMergeDataApprovals()
    {
        addDataApprovals( new DataApproval( dlA, dwA, peA, ouA, cocA, false, date( 2021, 1, 1 ), usA ),
            new DataApproval( dlA, dwA, peB, ouA, cocA, false, date( 2021, 3, 1 ), usA ) );
        assertEquals( 2, getDataApprovalCount( ouA ) );
        assertEquals( 0, getDataApprovalCount( ouB ) );
        assertEquals( 0, getDataApprovalCount( ouC ) );
        OrgUnitSplitRequest request = new OrgUnitSplitRequest.Builder().withSource( ouA ).addTarget( ouB )
            .addTarget( ouC ).withPrimaryTarget( ouB ).build();
        handler.splitData( request );
        assertEquals( 0, getDataApprovalCount( ouA ) );
        assertEquals( 2, getDataApprovalCount( ouB ) );
        assertEquals( 0, getDataApprovalCount( ouC ) );
    }

    private long getDataValueCount( OrganisationUnit target )
    {
        return (Long) sessionFactory.getCurrentSession()
            .createQuery( "select count(*) from DataValue dv where dv.source = :target" )
            .setParameter( "target", target ).uniqueResult();
    }

    private long getDataApprovalCount( OrganisationUnit target )
    {
        return (Long) sessionFactory.getCurrentSession()
            .createQuery( "select count(*) from DataApproval da where da.organisationUnit = :target" )
            .setParameter( "target", target ).uniqueResult();
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
