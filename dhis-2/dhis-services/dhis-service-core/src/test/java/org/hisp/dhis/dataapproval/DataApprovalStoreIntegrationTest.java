package org.hisp.dhis.dataapproval;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hibernate.SessionFactory;
import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataapproval.hibernate.HibernateDataApprovalStore;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.jdbc.statementbuilder.PostgreSQLStatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.*;

/**
 * DataApprovalStore tests that no longer work in the H2 database but must
 * be done in the PostgreSQL database.
 *
 * @author Jim Grace
 */
@Category( IntegrationTest.class )
public class DataApprovalStoreIntegrationTest
    extends IntegrationTestBase
{
    private HibernateDataApprovalStore dataApprovalStore;

    @Autowired
    private DataApprovalLevelService dataApprovalLevelService;

    @Autowired
    private DataApprovalService dataApprovalService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private CacheProvider cacheProvider;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private Environment environment;

    @Mock
    private CurrentUserService currentUserService;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    // -------------------------------------------------------------------------
    // Supporting data
    // -------------------------------------------------------------------------

    private DataApprovalLevel level1;

    private DataApprovalWorkflow workflowA;

    private OrganisationUnit sourceA;

    private DataSet dataSetA;

    private User userA;

    private CategoryOption categoryOptionA;
    private CategoryOption categoryOptionB;

    private org.hisp.dhis.category.Category categoryA;

    private CategoryCombo categoryComboA;

    private CategoryOptionCombo categoryOptionCombo;

    // -------------------------------------------------------------------------
    // Set up/tear down
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest() throws Exception
    {
        dataApprovalStore = new HibernateDataApprovalStore( sessionFactory, jdbcTemplate,
            publisher, cacheProvider, periodService, currentUserService, categoryService,
            systemSettingManager, new PostgreSQLStatementBuilder(), environment );

        dataApprovalStore.init();

        // ---------------------------------------------------------------------
        // Add supporting data
        // ---------------------------------------------------------------------

        level1 = new DataApprovalLevel( "01", 1, null );

        dataApprovalLevelService.addDataApprovalLevel( level1 );

        PeriodType periodType = PeriodType.getPeriodTypeByName( "Monthly" );

        workflowA = new DataApprovalWorkflow( "workflowA1", periodType, newHashSet( level1 ) );

        dataApprovalService.addWorkflow( workflowA );

        sourceA = createOrganisationUnit( 'A' );

        sourceA.setHierarchyLevel( 1 );

        organisationUnitService.addOrganisationUnit( sourceA );

        dataSetA = createDataSet( 'A', new MonthlyPeriodType(), categoryComboA );
        dataSetA.assignWorkflow( workflowA );
        dataSetA.addOrganisationUnit( sourceA );

        dataSetService.addDataSet( dataSetA );

        userA = createUser( 'A' );

        userA.addOrganisationUnit( sourceA );

        userService.addUser( userA );

        categoryOptionA = createCategoryOption( 'A' );
        categoryOptionB = createCategoryOption( 'B' );

        categoryOptionA.setPublicAccess( "r-r-----" );
        categoryOptionB.setPublicAccess( "r-r-----" );

        categoryService.addCategoryOption( categoryOptionA );
        categoryService.addCategoryOption( categoryOptionB );

        categoryA = createCategory( 'A', categoryOptionA, categoryOptionB );

        categoryService.addCategory( categoryA );

        categoryComboA = createCategoryCombo( 'A', categoryA );

        categoryService.addCategoryCombo( categoryComboA );

        categoryOptionCombo = createCategoryOptionCombo( categoryComboA, categoryOptionA, categoryOptionB );
        categoryOptionCombo.setName( "categoryOptionCombo" );
        categoryOptionCombo.setUid( "optComboUid" );

        categoryService.addCategoryOptionCombo( categoryOptionCombo );

        categoryComboA.getOptionCombos().add( categoryOptionCombo );

        categoryService.updateCategoryCombo( categoryComboA );
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Test
    public void testGetDataApprovalStatusesWithOpenPeriodsAfterCoEndDate()
    {
        dataApprovalLevelService.addDataApprovalLevel( level1 );

        Period periodJan = createPeriod( "202001" );
        Period periodFeb = createPeriod( "202002" );
        Period periodMay = createPeriod( "202005" );
        Period periodJun = createPeriod( "202006" );

        periodService.addPeriod( periodJan );
        periodService.addPeriod( periodFeb );
        periodService.addPeriod( periodMay );
        periodService.addPeriod( periodJun );

        List<DataApprovalLevel> userApprovalLevels = ListUtils.newList( level1 );

        Mockito.when( currentUserService.getCurrentUser() ).thenReturn( userA );

        assertEquals( 1, dataApprovalStore.getDataApprovalStatuses( workflowA, periodJan, null, 1, categoryComboA, null, userApprovalLevels, null ).size() );
        assertEquals( 1, dataApprovalStore.getDataApprovalStatuses( workflowA, periodFeb, null, 1, categoryComboA, null, userApprovalLevels, null ).size() );
        assertEquals( 1, dataApprovalStore.getDataApprovalStatuses( workflowA, periodMay, null, 1, categoryComboA, null, userApprovalLevels, null ).size() );
        assertEquals( 1, dataApprovalStore.getDataApprovalStatuses( workflowA, periodJun, null, 1, categoryComboA, null, userApprovalLevels, null ).size() );

        categoryOptionA.setStartDate( new DateTime( 2020, 1, 1, 0, 0 ).toDate() );
        categoryOptionA.setEndDate( new DateTime( 2020, 5, 30, 0, 0 ).toDate() );

        categoryOptionB.setStartDate( new DateTime( 2020, 2, 1, 0, 0 ).toDate() );
        categoryOptionB.setEndDate( new DateTime( 2020, 6, 30, 0, 0 ).toDate() );

        categoryService.updateCategoryOption( categoryOptionA );
        categoryService.updateCategoryOption( categoryOptionB );

        assertEquals( 0, dataApprovalStore.getDataApprovalStatuses( workflowA, periodJan, null, 1, categoryComboA, null, userApprovalLevels, null ).size() );
        assertEquals( 1, dataApprovalStore.getDataApprovalStatuses( workflowA, periodFeb, null, 1, categoryComboA, null, userApprovalLevels, null ).size() );
        assertEquals( 1, dataApprovalStore.getDataApprovalStatuses( workflowA, periodMay, null, 1, categoryComboA, null, userApprovalLevels, null ).size() );
        assertEquals( 0, dataApprovalStore.getDataApprovalStatuses( workflowA, periodJun, null, 1, categoryComboA, null, userApprovalLevels, null ).size() );

        dataSetA.setOpenPeriodsAfterCoEndDate( 1 );

        dataSetService.updateDataSet( dataSetA );

        assertEquals( 0, dataApprovalStore.getDataApprovalStatuses( workflowA, periodJan, null, 1, categoryComboA, null, userApprovalLevels, null ).size() );
        assertEquals( 1, dataApprovalStore.getDataApprovalStatuses( workflowA, periodFeb, null, 1, categoryComboA, null, userApprovalLevels, null ).size() );
        assertEquals( 1, dataApprovalStore.getDataApprovalStatuses( workflowA, periodMay, null, 1, categoryComboA, null, userApprovalLevels, null ).size() );
        assertEquals( 1, dataApprovalStore.getDataApprovalStatuses( workflowA, periodJun, null, 1, categoryComboA, null, userApprovalLevels, null ).size() );
    }
}
