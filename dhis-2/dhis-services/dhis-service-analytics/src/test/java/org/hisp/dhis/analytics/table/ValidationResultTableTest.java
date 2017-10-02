package org.hisp.dhis.analytics.table;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.utils.AnalyticsTestUtils;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.*;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.organisationunit.*;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.UserGroupAccessService;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationResultStore;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleStore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.util.*;

import static org.hisp.dhis.expression.Operator.equal_to;
import static org.junit.Assert.assertEquals;

/**
 * @author Henning Håkonsen
 */
public class ValidationResultTableTest
    extends DhisSpringTest
{
    @Autowired
    private AnalyticsTableGenerator analyticsTableGenerator;

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private ValidationRuleStore validationRuleStore;

    @Autowired
    private ValidationResultStore validationResultStore;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    protected UserGroupAccessService userGroupAccessService;

    @Autowired
    protected UserGroupService userGroupService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    protected IdentifiableObjectManager identifiableObjectManager;

    @Resource( name = "readOnlyJdbcTemplate" )
    private JdbcTemplate jdbcTemplate;

    // -------------------------------------------------------------------------
    // Supporting data
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest() throws Exception
    {
        // ---------------------------------------------------------------------
        // Add supporting data
        // ---------------------------------------------------------------------
        PeriodType periodType = PeriodType.getPeriodTypeByName( "Monthly" );

        Period periodA = createPeriod( new MonthlyPeriodType(), getDate( 2017, 1, 1 ), getDate( 2017, 1, 31 ) );
        periodService.addPeriod( periodA );

        OrganisationUnit ouA = createOrganisationUnit( 'A' );

        OrganisationUnit ouB = createOrganisationUnit( 'B' );

        OrganisationUnit ouC = createOrganisationUnit( 'C' );
        ouC.setOpeningDate( getDate( 2016, 4, 10 ) );
        ouC.setClosedDate( null );

        OrganisationUnit ouD = createOrganisationUnit( 'D' );
        ouD.setOpeningDate( getDate( 2016, 12, 10 ) );
        ouD.setClosedDate( null );

        OrganisationUnit ouE = createOrganisationUnit( 'E' );
        AnalyticsTestUtils.configureHierarchy( ouA, ouB, ouC, ouD, ouE );

        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
        organisationUnitService.addOrganisationUnit( ouD );
        organisationUnitService.addOrganisationUnit( ouE );

        identifiableObjectManager.save( ouA );
        identifiableObjectManager.save( ouB );
        identifiableObjectManager.save( ouC );
        identifiableObjectManager.save( ouD );
        identifiableObjectManager.save( ouE );

        OrganisationUnitGroup organisationUnitGroupA = createOrganisationUnitGroup( 'A' );
        organisationUnitGroupA.setUid( "a2345groupA" );
        organisationUnitGroupA.addOrganisationUnit( ouA );
        organisationUnitGroupA.addOrganisationUnit( ouB );

        OrganisationUnitGroup organisationUnitGroupB = createOrganisationUnitGroup( 'B' );
        organisationUnitGroupB.setUid( "a2345groupB" );
        organisationUnitGroupB.addOrganisationUnit( ouC );
        organisationUnitGroupB.addOrganisationUnit( ouD );
        organisationUnitGroupB.addOrganisationUnit( ouE );

        OrganisationUnitGroup organisationUnitGroupC = createOrganisationUnitGroup( 'C' );
        organisationUnitGroupC.setUid( "a2345groupC" );
        organisationUnitGroupC.addOrganisationUnit( ouA );
        organisationUnitGroupC.addOrganisationUnit( ouB );
        organisationUnitGroupC.addOrganisationUnit( ouC );

        OrganisationUnitGroup organisationUnitGroupD = createOrganisationUnitGroup( 'D' );
        organisationUnitGroupD.setUid( "a2345groupD" );
        organisationUnitGroupD.addOrganisationUnit( ouD );
        organisationUnitGroupD.addOrganisationUnit( ouE );

        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroupA );
        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroupB );
        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroupC );
        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroupD );

        OrganisationUnitGroupSet organisationUnitGroupSetA = createOrganisationUnitGroupSet( 'A' );
        organisationUnitGroupSetA.setUid( "a234567setA" );
        OrganisationUnitGroupSet organisationUnitGroupSetB = createOrganisationUnitGroupSet( 'B' );
        organisationUnitGroupSetB.setUid( "a234567setB" );

        organisationUnitGroupSetA.getOrganisationUnitGroups().add( organisationUnitGroupA );
        organisationUnitGroupSetA.getOrganisationUnitGroups().add( organisationUnitGroupB );

        organisationUnitGroupSetB.getOrganisationUnitGroups().add( organisationUnitGroupC );
        organisationUnitGroupSetB.getOrganisationUnitGroups().add( organisationUnitGroupD );

        organisationUnitGroupService.addOrganisationUnitGroupSet( organisationUnitGroupSetA );
        organisationUnitGroupService.addOrganisationUnitGroupSet( organisationUnitGroupSetB );

        DataElementCategoryOption optionA = new DataElementCategoryOption( "CategoryOptionA" );
        DataElementCategoryOption optionB = new DataElementCategoryOption( "CategoryOptionB" );
        categoryService.addDataElementCategoryOption( optionA );
        categoryService.addDataElementCategoryOption( optionB );

        DataElementCategory categoryA = createDataElementCategory( 'A', optionA, optionB );
        categoryA.setDataDimensionType( DataDimensionType.ATTRIBUTE );
        categoryService.addDataElementCategory( categoryA );

        DataElementCategoryCombo categoryComboA = createCategoryCombo( 'A', categoryA );
        categoryService.addDataElementCategoryCombo( categoryComboA );

        DataElementCategoryOptionCombo optionComboA = createCategoryOptionCombo( 'A', categoryComboA, optionA );
        DataElementCategoryOptionCombo optionComboB = createCategoryOptionCombo( 'B', categoryComboA, optionB );
        DataElementCategoryOptionCombo optionComboC = createCategoryOptionCombo( 'C', categoryComboA, optionA, optionB );

        categoryService.addDataElementCategoryOptionCombo( optionComboA );
        categoryService.addDataElementCategoryOptionCombo( optionComboB );
        categoryService.addDataElementCategoryOptionCombo( optionComboC );

        CategoryOptionGroup optionGroupA = createCategoryOptionGroup( 'A', optionA );
        CategoryOptionGroup optionGroupB = createCategoryOptionGroup( 'B', optionB );
        categoryService.saveCategoryOptionGroup( optionGroupA );
        categoryService.saveCategoryOptionGroup( optionGroupB );

        CategoryOptionGroupSet optionGroupSetB = new CategoryOptionGroupSet( "OptionGroupSetB" );
        categoryService.saveCategoryOptionGroupSet( optionGroupSetB );

        optionGroupSetB.addCategoryOptionGroup( optionGroupA );
        optionGroupSetB.addCategoryOptionGroup( optionGroupB );

        optionGroupA.getGroupSets().add( optionGroupSetB );
        optionGroupB.getGroupSets().add( optionGroupSetB );

        Expression expressionA = new Expression( "expressionA", "descriptionA" );
        Expression expressionB = new Expression( "expressionB", "descriptionB" );
        expressionService.addExpression( expressionB );
        expressionService.addExpression( expressionA );

        ValidationRule validationRuleA = createValidationRule( 'A', equal_to, expressionA, expressionB, periodType );
        validationRuleA.setUid( "a234567vruB" );
        validationRuleStore.save( validationRuleA );

        ValidationResult validationResultBA = new ValidationResult( validationRuleA, periodA, ouB, optionComboA, 1.0,2.0, 3 );
        ValidationResult validationResultBB = new ValidationResult( validationRuleA, periodA, ouB, optionComboB, 1.0,2.0, 3 );
        ValidationResult validationResultAA = new ValidationResult( validationRuleA, periodA, ouA, optionComboA, 1.0,2.0, 3 );
        ValidationResult validationResultAB = new ValidationResult( validationRuleA, periodA, ouA, optionComboB, 1.0,2.0, 3 );

        validationResultStore.save( validationResultAA );
        validationResultStore.save( validationResultAB );
        validationResultStore.save( validationResultBB );
        validationResultStore.save( validationResultBA );
    }

    @Test
    public void testGetTableName()
    {

        Set<AnalyticsTableType> skipTableTypes = new HashSet<>(  );
        skipTableTypes.addAll( Arrays.asList(AnalyticsTableType.DATA_VALUE, AnalyticsTableType.COMPLETENESS, AnalyticsTableType.COMPLETENESS_TARGET, AnalyticsTableType.ORG_UNIT_TARGET, AnalyticsTableType.EVENT, AnalyticsTableType.ENROLLMENT) );

        analyticsTableGenerator.generateTables( 1, null, skipTableTypes, false);

        List<Map<String, Object>> resultMap = jdbcTemplate.queryForList( "select * from analytics_validation_result_2017;" );

        assertEquals(4, resultMap.size());
    }
}
