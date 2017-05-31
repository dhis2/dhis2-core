package org.hisp.dhis.dataapproval;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisTest;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Sets.newHashSet;
import static org.hisp.dhis.dataapproval.DataApprovalAction.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Jim Grace
 */
public class DataApprovalAuditStoreTest
    extends DhisTest
{
    @Autowired
    private DataApprovalAuditStore dataApprovalAuditStore;

    @Autowired
    private DataApprovalLevelService dataApprovalLevelService;

    @Autowired
    private DataApprovalService dataApprovalService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    // -------------------------------------------------------------------------
    // Supporting data
    // -------------------------------------------------------------------------

    private DataApprovalLevel level1;
    private DataApprovalLevel level2;

    private DataApprovalWorkflow workflowA;
    private DataApprovalWorkflow workflowB;

    private Period periodA;
    private Period periodB;

    private OrganisationUnit sourceA;
    private OrganisationUnit sourceB;

    private User userA;

    private DataElementCategoryOption optionA;
    private DataElementCategoryOption optionB;

    private DataElementCategory categoryA;

    private DataElementCategoryCombo categoryComboA;

    private DataElementCategoryOptionCombo optionComboA;
    private DataElementCategoryOptionCombo optionComboB;

    private Date dateA;
    private Date dateB;

    private DataApprovalAudit auditA;
    private DataApprovalAudit auditB;

    // -------------------------------------------------------------------------
    // Set up/tear down
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest() throws Exception
    {
        // ---------------------------------------------------------------------
        // Add supporting data
        // ---------------------------------------------------------------------

        level1 = new DataApprovalLevel( "01", 1, null );
        level2 = new DataApprovalLevel( "02", 2, null );
        dataApprovalLevelService.addDataApprovalLevel( level1 );
        dataApprovalLevelService.addDataApprovalLevel( level2 );

        PeriodType periodType = PeriodType.getPeriodTypeByName( "Monthly" );

        workflowA = new DataApprovalWorkflow( "workflowA", periodType, newHashSet( level1 ) );
        workflowB = new DataApprovalWorkflow( "workflowB", periodType, newHashSet( level1, level2 ) );
        dataApprovalService.addWorkflow( workflowA );
        dataApprovalService.addWorkflow( workflowB );

        periodA = createPeriod( new MonthlyPeriodType(), getDate( 2017, 1, 1 ), getDate( 2017, 1, 31 ) );
        periodB = createPeriod( new MonthlyPeriodType(), getDate( 2018, 1, 1 ), getDate( 2018, 1, 31 ) );
        periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );

        sourceA = createOrganisationUnit( 'A' );
        sourceB = createOrganisationUnit( 'B', sourceA );
        organisationUnitService.addOrganisationUnit( sourceA );
        organisationUnitService.addOrganisationUnit( sourceB );

        userA = createUser( 'A' );
        userService.addUser( userA );

        optionA = new DataElementCategoryOption( "CategoryOptionA" );
        optionB = new DataElementCategoryOption( "CategoryOptionB" );
        categoryService.addDataElementCategoryOption( optionA );
        categoryService.addDataElementCategoryOption( optionB );

        categoryA = createDataElementCategory( 'A', optionA, optionB );
        categoryService.addDataElementCategory( categoryA );

        categoryComboA = createCategoryCombo( 'A', categoryA );
        categoryService.addDataElementCategoryCombo( categoryComboA );

        optionComboA = createCategoryOptionCombo( 'A', categoryComboA, optionA );
        optionComboB = createCategoryOptionCombo( 'B', categoryComboA, optionA, optionB );
        categoryService.addDataElementCategoryOptionCombo( optionComboA );
        categoryService.addDataElementCategoryOptionCombo( optionComboB );

        dateA = getDate( 2017, 1, 1 );
        dateB = getDate( 2018, 1, 1 );

        DataApproval approvalA = new DataApproval( level1, workflowA, periodA, sourceA, optionComboA, false, dateA, userA );
        DataApproval approvalB = new DataApproval( level2, workflowB, periodB, sourceB, optionComboB, false, dateB, userA );
        auditA = new DataApprovalAudit(approvalA, APPROVE );
        auditB = new DataApprovalAudit(approvalB, UNAPPROVE );
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    public void tearDownTest()
    {
        setDependency( dataApprovalAuditStore, "currentUserService", currentUserService, CurrentUserService.class );
    }

    // -------------------------------------------------------------------------
    // DataApprovalAudit
    // -------------------------------------------------------------------------

    @Test
    public void testSave() throws Exception
    {
        dataApprovalAuditStore.save( auditA );
        dataApprovalAuditStore.save( auditB );

        List<DataApprovalAudit> audits = dataApprovalAuditStore.getDataApprovalAudits( new DataApprovalAuditQueryParams() );
        assertEquals( 2, audits.size() );

        assertEquals( auditA, audits.get( 0 ) );
        assertEquals( auditB, audits.get( 1 ) );
    }

    @Test
    public void testDelete() throws Exception
    {
        dataApprovalAuditStore.save( auditA );
        dataApprovalAuditStore.save( auditB );

        dataApprovalAuditStore.delete( auditA );

        List<DataApprovalAudit> audits = dataApprovalAuditStore.getDataApprovalAudits( new DataApprovalAuditQueryParams() );
        assertEquals( 1, audits.size() );

        assertEquals( auditB, audits.get( 0 ) );
    }

    @Test
    public void testDeleteDataApprovalAudits() throws Exception
    {
        dataApprovalAuditStore.save( auditA );
        dataApprovalAuditStore.save( auditB );

        dataApprovalAuditStore.deleteDataApprovalAudits( sourceB );

        List<DataApprovalAudit> audits = dataApprovalAuditStore.getDataApprovalAudits( new DataApprovalAuditQueryParams() );
        assertEquals( 1, audits.size() );

        assertEquals( auditA, audits.get( 0 ) );
    }

    @Test
    public void TestGetDataApprovalAudits() throws Exception
    {
        dataApprovalAuditStore.save( auditA );
        dataApprovalAuditStore.save( auditB );

        DataApprovalAuditQueryParams params;
        List<DataApprovalAudit> audits;

        params = new DataApprovalAuditQueryParams();
        params.setWorkflows( Sets.newHashSet( workflowA ) );
        audits = dataApprovalAuditStore.getDataApprovalAudits( params );
        assertEquals( 1, audits.size() );
        assertEquals( auditA, audits.get( 0 ) );

        params = new DataApprovalAuditQueryParams();
        params.setLevels( Sets.newHashSet( level1 ) );
        audits = dataApprovalAuditStore.getDataApprovalAudits( params );
        assertEquals( 1, audits.size() );
        assertEquals( auditA, audits.get( 0 ) );

        params = new DataApprovalAuditQueryParams();
        params.setOrganisationUnits( Sets.newHashSet( sourceA ) );
        audits = dataApprovalAuditStore.getDataApprovalAudits( params );
        assertEquals( 1, audits.size() );
        assertEquals( auditA, audits.get( 0 ) );

        params = new DataApprovalAuditQueryParams();
        params.setAttributeOptionCombos( Sets.newHashSet( optionComboA ) );
        audits = dataApprovalAuditStore.getDataApprovalAudits( params );
        assertEquals( 1, audits.size() );
        assertEquals( auditA, audits.get( 0 ) );

        params = new DataApprovalAuditQueryParams();
        params.setStartDate( dateB );
        audits = dataApprovalAuditStore.getDataApprovalAudits( params );
        assertEquals( 1, audits.size() );
        assertEquals( auditB, audits.get( 0 ) );

        params = new DataApprovalAuditQueryParams();
        params.setEndDate( dateB );
        audits = dataApprovalAuditStore.getDataApprovalAudits( params );
        assertEquals( 1, audits.size() );
        assertEquals( auditA, audits.get( 0 ) );

        CurrentUserService mockUserService = new MockCurrentUserService( Sets.newHashSet( sourceB ), null );
        setDependency( dataApprovalAuditStore, "currentUserService", mockUserService, CurrentUserService.class );

        params = new DataApprovalAuditQueryParams();
        audits = dataApprovalAuditStore.getDataApprovalAudits( params );
        assertEquals( 1, audits.size() );
        assertEquals( auditB, audits.get( 0 ) );
    }
}
