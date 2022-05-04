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
package org.hisp.dhis.predictor;

import static com.google.common.collect.Sets.newHashSet;
import static org.hisp.dhis.common.OrganisationUnitDescendants.SELECTED;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_DAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.MissingValueStrategy;
import org.hisp.dhis.jdbc.batchhandler.DataValueBatchHandler;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.CurrentUserServiceTarget;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 * @author Jim Grace
 */
class PredictionServiceTest extends IntegrationTestBase
{

    private final JobProgress progress = NoopJobProgress.INSTANCE;

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private PredictorService predictorService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private BatchHandlerFactory batchHandlerFactory;

    @Autowired
    private UserService _userService;

    private OrganisationUnitLevel orgUnitLevel1;

    private OrganisationUnitLevel orgUnitLevel2;

    private OrganisationUnitLevel orgUnitLevel3;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataElement dataElementC;

    private DataElement dataElementD;

    private DataElement dataElementE;

    private DataElement dataElementF;

    private DataElement dataElementG;

    private DataElement dataElementH;

    private DataElement dataElementI;

    private DataElement dataElementJ;

    private DataElement dataElementX;

    private DataElement dataElementY;

    private DataElement dataElementZ;

    private CategoryOptionCombo defaultCombo;

    private CategoryOptionCombo altCombo;

    private CategoryOption altCategoryOption;

    private Category altCategory;

    private CategoryCombo altCategoryCombo;

    private OrganisationUnit sourceA, sourceB, sourceC, sourceD, sourceE, sourceF, sourceG;

    private OrganisationUnitGroup ouGroupA, ouGroupB;

    private Set<CategoryOptionCombo> optionCombos;

    private Expression expressionA;

    private Expression expressionB;

    private Expression expressionC;

    private Expression expressionD;

    private Expression expressionE;

    private Expression expressionF;

    private Expression expressionG;

    private Expression expressionH;

    private PeriodType periodTypeMonthly;

    private DataSet dataSetMonthly;

    private BatchHandler<DataValue> dataValueBatchHandler;

    private PredictionSummary summary;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------
    @Override
    public void setUpTest()
        throws Exception
    {
        this.userService = _userService;

        PeriodType.invalidatePeriodCache();
        orgUnitLevel1 = new OrganisationUnitLevel( 1, "Level1" );
        orgUnitLevel2 = new OrganisationUnitLevel( 2, "Level2" );
        orgUnitLevel3 = new OrganisationUnitLevel( 3, "Level3" );
        organisationUnitService.addOrganisationUnitLevel( orgUnitLevel1 );
        organisationUnitService.addOrganisationUnitLevel( orgUnitLevel2 );
        organisationUnitService.addOrganisationUnitLevel( orgUnitLevel3 );
        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementC = createDataElement( 'C' );
        dataElementD = createDataElement( 'D' );
        dataElementE = createDataElement( 'E', ValueType.TEXT, AggregationType.NONE );
        dataElementF = createDataElement( 'F', ValueType.TEXT, AggregationType.NONE );
        dataElementG = createDataElement( 'G', ValueType.DATE, AggregationType.NONE );
        dataElementH = createDataElement( 'H', ValueType.DATE, AggregationType.NONE );
        dataElementI = createDataElement( 'I', ValueType.BOOLEAN, AggregationType.NONE );
        dataElementJ = createDataElement( 'J', ValueType.BOOLEAN, AggregationType.NONE );
        dataElementX = createDataElement( 'X', ValueType.NUMBER, AggregationType.NONE );
        dataElementY = createDataElement( 'Y', ValueType.INTEGER, AggregationType.NONE );
        dataElementZ = createDataElement( 'Z', ValueType.INTEGER, AggregationType.NONE );
        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );
        dataElementService.addDataElement( dataElementE );
        dataElementService.addDataElement( dataElementF );
        dataElementService.addDataElement( dataElementG );
        dataElementService.addDataElement( dataElementH );
        dataElementService.addDataElement( dataElementI );
        dataElementService.addDataElement( dataElementJ );
        dataElementService.addDataElement( dataElementX );
        dataElementService.addDataElement( dataElementY );
        dataElementService.addDataElement( dataElementZ );
        // Org unit hierarchy:
        //
        // Level 1: A, B, G
        // Level 2: C, D (children of B)
        // Level 3: E, F (children of D)
        sourceA = createOrganisationUnit( 'A' );
        sourceB = createOrganisationUnit( 'B' );
        sourceC = createOrganisationUnit( 'C', sourceB );
        sourceD = createOrganisationUnit( 'D', sourceB );
        sourceE = createOrganisationUnit( 'E', sourceD );
        sourceF = createOrganisationUnit( 'F', sourceD );
        sourceG = createOrganisationUnit( 'G' );
        organisationUnitService.addOrganisationUnit( sourceA );
        organisationUnitService.addOrganisationUnit( sourceB );
        organisationUnitService.addOrganisationUnit( sourceC );
        organisationUnitService.addOrganisationUnit( sourceD );
        organisationUnitService.addOrganisationUnit( sourceE );
        organisationUnitService.addOrganisationUnit( sourceF );
        organisationUnitService.addOrganisationUnit( sourceG );
        ouGroupA = createOrganisationUnitGroup( 'A' );
        ouGroupB = createOrganisationUnitGroup( 'B' );
        ouGroupA.addOrganisationUnit( sourceA );
        ouGroupA.addOrganisationUnit( sourceC );
        ouGroupB.addOrganisationUnit( sourceD );
        ouGroupB.addOrganisationUnit( sourceF );
        organisationUnitGroupService.addOrganisationUnitGroup( ouGroupA );
        organisationUnitGroupService.addOrganisationUnitGroup( ouGroupB );
        periodTypeMonthly = PeriodType.getPeriodTypeByName( "Monthly" );
        dataSetMonthly = createDataSet( 'M', periodTypeMonthly );
        dataSetMonthly.addDataSetElement( dataElementA );
        dataSetMonthly.addDataSetElement( dataElementB );
        dataSetMonthly.addDataSetElement( dataElementC );
        dataSetMonthly.addDataSetElement( dataElementD );
        dataSetMonthly.addDataSetElement( dataElementX );
        dataSetMonthly.addDataSetElement( dataElementY );
        dataSetMonthly.addOrganisationUnit( sourceA );
        dataSetMonthly.addOrganisationUnit( sourceB );
        dataSetMonthly.addOrganisationUnit( sourceC );
        dataSetMonthly.addOrganisationUnit( sourceD );
        dataSetMonthly.addOrganisationUnit( sourceE );
        dataSetMonthly.addOrganisationUnit( sourceG );
        dataSetService.addDataSet( dataSetMonthly );
        CategoryOptionCombo categoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
        defaultCombo = categoryService.getDefaultCategoryOptionCombo();
        altCategoryOption = new CategoryOption( "AltCategoryOption" );
        categoryService.addCategoryOption( altCategoryOption );
        altCategory = createCategory( 'A', altCategoryOption );
        categoryService.addCategory( altCategory );
        altCategoryCombo = createCategoryCombo( 'Y', altCategory );
        categoryService.addCategoryCombo( altCategoryCombo );
        altCombo = createCategoryOptionCombo( altCategoryCombo, altCategoryOption );
        optionCombos = new HashSet<>();
        optionCombos.add( categoryOptionCombo );
        optionCombos.add( altCombo );
        categoryService.addCategoryOptionCombo( altCombo );
        expressionA = new Expression(
            "avg(#{" + dataElementA.getUid() + "})+1.5*stddev(#{" + dataElementA.getUid() + "})", "descriptionA" );
        expressionH = new Expression(
            "avg(#{" + dataElementA.getUid() + "})+1.5*stddevPop(#{" + dataElementA.getUid() + "})", "descriptionH" );
        expressionB = new Expression( "avg(#{" + dataElementB.getUid() + "." + defaultCombo.getUid() + "})",
            "descriptionB" );
        expressionC = new Expression( "135.79", "descriptionC" );
        expressionD = new Expression( SYMBOL_DAYS, "descriptionD" );
        expressionE = new Expression( "sum(#{" + dataElementA.getUid() + "})+#{" + dataElementB.getUid() + "}",
            "descriptionE" );
        expressionF = new Expression( "#{" + dataElementB.getUid() + "}", "descriptionF" );
        expressionG = new Expression( "sum(#{" + dataElementA.getUid() + "}+#{" + dataElementB.getUid() + "})",
            "descriptionG" );
        summary = new PredictionSummary();
        dataValueBatchHandler = batchHandlerFactory.createBatchHandler( DataValueBatchHandler.class ).init();
        Set<OrganisationUnit> units = newHashSet( sourceA, sourceB, sourceG );

        User user = createAndAddUser( true, "mockUser", units, units );
        injectSecurityContext( user );
    }

    @Override
    public void tearDownTest()
    {
        setDependency( CurrentUserServiceTarget.class, CurrentUserServiceTarget::setCurrentUserService,
            currentUserService, predictionService );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------
    private Period makeMonth( int year, int month )
    {
        Date start = getDate( year, month, 1 );
        Period period = periodTypeMonthly.createPeriod( start );
        Date end = getDate( year, month, period.getDaysInPeriod() );
        return createPeriod( periodTypeMonthly, start, end );
    }

    private Date monthStart( int year, int month )
    {
        DateTime starting = new DateTime( year, month, 1, 0, 0 );
        return starting.toDate();
    }

    private void useDataValue( DataElement e, Period p, OrganisationUnit s, Object value )
    {
        useDataValue( e, p, s, defaultCombo, value );
    }

    private void useDataValue( DataElement e, Period p, OrganisationUnit s, CategoryOptionCombo attributeOptionCombo,
        Object value )
    {
        useDataValue( e, p, s, attributeOptionCombo, value, false );
    }

    private void useDataValue( DataElement e, Period p, OrganisationUnit s, CategoryOptionCombo attributeOptionCombo,
        Object value, boolean deleted )
    {
        dataValueBatchHandler.addObject( createDataValue( e, periodService.reloadPeriod( p ), s, defaultCombo,
            attributeOptionCombo, value == null ? null : value.toString(), deleted ) );
    }

    private String getDataValue( DataElement dataElement, CategoryOptionCombo combo, OrganisationUnit source,
        Period period )
    {
        return getDataValue( dataElement, combo, defaultCombo, source, period );
    }

    private String getDataValue( DataElement dataElement, CategoryOptionCombo combo,
        CategoryOptionCombo attributeOptionCombo, OrganisationUnit source, Period period )
    {
        DataExportParams params = new DataExportParams()
            .setDataElementOperands( Sets.newHashSet( new DataElementOperand( dataElement, combo ) ) )
            .setAttributeOptionCombos( Sets.newHashSet( attributeOptionCombo ) )
            .setOrganisationUnits( Sets.newHashSet( source ) )
            .setPeriods( Sets.newHashSet( periodService.reloadPeriod( period ) ) );
        List<DeflatedDataValue> values = dataValueService.getDeflatedDataValues( params );
        if ( values != null && values.size() > 0 )
        {
            return values.get( 0 ).getValue();
        }
        return null;
    }

    private String shortSummary( PredictionSummary summary )
    {
        return "Pred " + summary.getPredictors() + " Ins " + summary.getInserted() + " Upd " + summary.getUpdated()
            + " Del " + summary.getDeleted() + " Unch " + summary.getUnchanged();
    }

    private void setupTestData()
    {
        // dataElementA - 2001
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceA, 5 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceA, 3 );
        useDataValue( dataElementA, makeMonth( 2001, 8 ), sourceA, 8 );
        useDataValue( dataElementA, makeMonth( 2001, 9 ), sourceA, 4 );
        useDataValue( dataElementA, makeMonth( 2001, 10 ), sourceA, 7 );
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceC, 6 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceC, 4 );
        useDataValue( dataElementA, makeMonth( 2001, 8 ), sourceC, 7 );
        useDataValue( dataElementA, makeMonth( 2001, 9 ), sourceC, 4 );
        useDataValue( dataElementA, makeMonth( 2001, 10 ), sourceC, 7 );
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceE, 2 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceE, 1 );
        useDataValue( dataElementA, makeMonth( 2001, 8 ), sourceE, 3 );
        useDataValue( dataElementA, makeMonth( 2001, 9 ), sourceE, 2 );
        useDataValue( dataElementA, makeMonth( 2001, 10 ), sourceE, 1 );
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceF, 3 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceF, 2 );
        useDataValue( dataElementA, makeMonth( 2001, 8 ), sourceF, 4 );
        useDataValue( dataElementA, makeMonth( 2001, 9 ), sourceF, 3 );
        useDataValue( dataElementA, makeMonth( 2001, 10 ), sourceF, 2 );
        // dataElementA - 2002
        useDataValue( dataElementA, makeMonth( 2002, 6 ), sourceA, 8 );
        useDataValue( dataElementA, makeMonth( 2002, 7 ), sourceA, 4 );
        useDataValue( dataElementA, makeMonth( 2002, 8 ), sourceA, 10 );
        useDataValue( dataElementA, makeMonth( 2002, 9 ), sourceA, 5 );
        useDataValue( dataElementA, makeMonth( 2002, 10 ), sourceA, 7 );
        useDataValue( dataElementA, makeMonth( 2002, 6 ), sourceC, 7 );
        useDataValue( dataElementA, makeMonth( 2002, 7 ), sourceC, 4 );
        useDataValue( dataElementA, makeMonth( 2002, 8 ), sourceC, 11 );
        useDataValue( dataElementA, makeMonth( 2002, 9 ), sourceC, 5 );
        useDataValue( dataElementA, makeMonth( 2002, 10 ), sourceC, 6 );
        useDataValue( dataElementA, makeMonth( 2002, 6 ), sourceE, 3 );
        useDataValue( dataElementA, makeMonth( 2002, 7 ), sourceE, 2 );
        useDataValue( dataElementA, makeMonth( 2002, 8 ), sourceE, 1 );
        useDataValue( dataElementA, makeMonth( 2002, 9 ), sourceE, 2 );
        useDataValue( dataElementA, makeMonth( 2002, 10 ), sourceE, 2 );
        useDataValue( dataElementA, makeMonth( 2002, 6 ), sourceF, 4 );
        useDataValue( dataElementA, makeMonth( 2002, 7 ), sourceF, 3 );
        useDataValue( dataElementA, makeMonth( 2002, 8 ), sourceF, 2 );
        useDataValue( dataElementA, makeMonth( 2002, 9 ), sourceF, 3 );
        useDataValue( dataElementA, makeMonth( 2002, 10 ), sourceF, 3 );
        // dataElementA - 2003
        useDataValue( dataElementA, makeMonth( 2003, 5 ), sourceA, 9 );
        useDataValue( dataElementA, makeMonth( 2003, 6 ), sourceA, 11 );
        useDataValue( dataElementA, makeMonth( 2003, 7 ), sourceA, 6 );
        useDataValue( dataElementA, makeMonth( 2003, 8 ), sourceA, 7 );
        useDataValue( dataElementA, makeMonth( 2003, 9 ), sourceA, 9 );
        useDataValue( dataElementA, makeMonth( 2003, 10 ), sourceA, 10 );
        useDataValue( dataElementA, makeMonth( 2003, 5 ), sourceC, 10 );
        useDataValue( dataElementA, makeMonth( 2003, 6 ), sourceC, 10 );
        useDataValue( dataElementA, makeMonth( 2003, 7 ), sourceC, 7 );
        useDataValue( dataElementA, makeMonth( 2003, 8 ), sourceC, 7 );
        useDataValue( dataElementA, makeMonth( 2003, 9 ), sourceC, 8 );
        useDataValue( dataElementA, makeMonth( 2003, 10 ), sourceC, 9 );
        useDataValue( dataElementA, makeMonth( 2003, 5 ), sourceE, 4 );
        useDataValue( dataElementA, makeMonth( 2003, 6 ), sourceE, 4 );
        useDataValue( dataElementA, makeMonth( 2003, 7 ), sourceE, 3 );
        useDataValue( dataElementA, makeMonth( 2003, 8 ), sourceE, 2 );
        useDataValue( dataElementA, makeMonth( 2003, 9 ), sourceE, 2 );
        useDataValue( dataElementA, makeMonth( 2003, 10 ), sourceE, 1 );
        useDataValue( dataElementA, makeMonth( 2003, 5 ), sourceF, 5 );
        useDataValue( dataElementA, makeMonth( 2003, 6 ), sourceF, 5 );
        useDataValue( dataElementA, makeMonth( 2003, 7 ), sourceF, 4 );
        useDataValue( dataElementA, makeMonth( 2003, 8 ), sourceF, 3 );
        useDataValue( dataElementA, makeMonth( 2003, 9 ), sourceF, 3 );
        useDataValue( dataElementA, makeMonth( 2003, 10 ), sourceF, 2 );
        // dataElementA - 2004
        useDataValue( dataElementA, makeMonth( 2004, 5 ), sourceA, 4 );
        useDataValue( dataElementA, makeMonth( 2004, 6 ), sourceA, 8 );
        useDataValue( dataElementA, makeMonth( 2004, 7 ), sourceA, 4 );
        useDataValue( dataElementA, makeMonth( 2004, 8 ), sourceA, 7 );
        useDataValue( dataElementA, makeMonth( 2004, 9 ), sourceA, 5 );
        useDataValue( dataElementA, makeMonth( 2004, 10 ), sourceA, 6 );
        useDataValue( dataElementA, makeMonth( 2004, 5 ), sourceC, 5 );
        useDataValue( dataElementA, makeMonth( 2004, 6 ), sourceC, 9 );
        useDataValue( dataElementA, makeMonth( 2004, 7 ), sourceC, 6 );
        useDataValue( dataElementA, makeMonth( 2004, 8 ), sourceC, 7 );
        useDataValue( dataElementA, makeMonth( 2004, 9 ), sourceC, 6 );
        useDataValue( dataElementA, makeMonth( 2004, 10 ), sourceC, 5 );
        useDataValue( dataElementA, makeMonth( 2004, 5 ), sourceE, 5 );
        useDataValue( dataElementA, makeMonth( 2004, 6 ), sourceE, 7 );
        useDataValue( dataElementA, makeMonth( 2004, 7 ), sourceE, 5 );
        useDataValue( dataElementA, makeMonth( 2004, 8 ), sourceE, 4 );
        useDataValue( dataElementA, makeMonth( 2004, 9 ), sourceE, 4 );
        useDataValue( dataElementA, makeMonth( 2004, 10 ), sourceE, 3 );
        useDataValue( dataElementA, makeMonth( 2004, 5 ), sourceF, 6 );
        useDataValue( dataElementA, makeMonth( 2004, 6 ), sourceF, 8 );
        useDataValue( dataElementA, makeMonth( 2004, 7 ), sourceF, 6 );
        useDataValue( dataElementA, makeMonth( 2004, 8 ), sourceF, 5 );
        useDataValue( dataElementA, makeMonth( 2004, 9 ), sourceF, 5 );
        useDataValue( dataElementA, makeMonth( 2004, 10 ), sourceF, 4 );
        // dataElementB - 2003
        useDataValue( dataElementB, makeMonth( 2003, 6 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 7 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 8 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 9 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 10 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 5 ), sourceF, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 6 ), sourceF, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 7 ), sourceF, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 9 ), sourceF, 1 );
        useDataValue( dataElementB, makeMonth( 2003, 10 ), sourceF, 1 );
        dataValueBatchHandler.flush();
    }

    // -------------------------------------------------------------------------
    // Prediction tests
    // -------------------------------------------------------------------------
    @Test
    void testPredictWithCategoryOptionCombo()
    {
        useDataValue( dataElementB, makeMonth( 2001, 6 ), sourceA, 5 );
        dataValueBatchHandler.flush();
        Predictor p = createPredictor( dataElementX, defaultCombo, "A", expressionB, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 8 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "5.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
    }

    @Test
    void testPredictSequentialWithDescendants()
    {
        setupTestData();
        Predictor p = createPredictor( dataElementX, defaultCombo, "PredictSequential", expressionA, null,
            periodTypeMonthly, orgUnitLevel1, 3, 1, 0 );
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 12 ), summary );
        assertEquals( "Pred 1 Ins 8 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "5.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "6.121", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 9 ) ) );
        assertEquals( "10.8", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 10 ) ) );
        assertEquals( "10.24", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 11 ) ) );
        assertEquals( "11.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 8 ) ) );
        assertEquals( "13.24", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 9 ) ) );
        assertEquals( "17.92", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 10 ) ) );
        assertEquals( "16.8", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 11 ) ) );
        // Make sure we can do it again.
        summary = new PredictionSummary();
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 12 ), summary );
        assertEquals( "Pred 1 Ins 0 Upd 0 Del 0 Unch 8", shortSummary( summary ) );
    }

    @Test
    void testPredictSequentialWithoutDescendants()
    {
        setupTestData();
        Predictor p = createPredictor( dataElementX, defaultCombo, "PredictSequential", expressionA, null,
            periodTypeMonthly, orgUnitLevel1, 3, 1, 0 );
        p.setOrganisationUnitDescendants( SELECTED );
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 12 ), summary );
        assertEquals( "Pred 1 Ins 4 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "5.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "6.121", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 9 ) ) );
        assertEquals( "10.8", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 10 ) ) );
        assertEquals( "10.24", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 11 ) ) );
        // Make sure we can do it again.
        summary = new PredictionSummary();
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 12 ), summary );
        assertEquals( "Pred 1 Ins 0 Upd 0 Del 0 Unch 4", shortSummary( summary ) );
    }

    @Test
    void testPredictSequentialStddevPop()
    {
        setupTestData();
        Predictor p = createPredictor( dataElementX, defaultCombo, "PredictSequential", expressionH, null,
            periodTypeMonthly, orgUnitLevel1, 3, 1, 0 );
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 12 ), summary );
        assertEquals( "Pred 1 Ins 8 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "5.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "5.5", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 9 ) ) );
        assertEquals( "9.25", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 10 ) ) );
        assertEquals( "9.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 11 ) ) );
        assertEquals( "11.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 8 ) ) );
        assertEquals( "12.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 9 ) ) );
        assertEquals( "15.75", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 10 ) ) );
        assertEquals( "15.25", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 11 ) ) );
        // Make sure we can do it again.
        summary = new PredictionSummary();
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 12 ), summary );
        assertEquals( "Pred 1 Ins 0 Upd 0 Del 0 Unch 8", shortSummary( summary ) );
    }

    @Test
    void testPredictSequentialStddevPopWithLimitedUser()
    {
        setupTestData();
        Set<OrganisationUnit> units = newHashSet( sourceA );

        User user2 = createAndAddUser( true, "mockUser2", units, units );
        injectSecurityContext( user2 );

        Predictor p = createPredictor( dataElementX, defaultCombo, "PredictSequential", expressionH, null,
            periodTypeMonthly, orgUnitLevel1, 3, 1, 0 );
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 12 ), summary );
        assertEquals( "Pred 1 Ins 4 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "5.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "5.5", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 9 ) ) );
        assertEquals( "9.25", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 10 ) ) );
        assertEquals( "9.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 11 ) ) );
        // Make sure we can do it again.
        summary = new PredictionSummary();
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 12 ), summary );
        assertEquals( "Pred 1 Ins 0 Upd 0 Del 0 Unch 4", shortSummary( summary ) );
    }

    @Test
    void testPredictSeasonal()
    {
        setupTestData();
        Predictor p = createPredictor( dataElementX, altCombo, "GetPredictionsSeasonal", expressionA, null,
            periodTypeMonthly, orgUnitLevel1, 3, 1, 2 );
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2005, 12 ), summary );
        assertEquals( "Pred 1 Ins 100 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "5.0", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "6.121", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2001, 9 ) ) );
        assertEquals( "7.0", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2003, 1 ) ) );
        assertEquals( "9.682", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2003, 3 ) ) );
        assertEquals( "11.09", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2004, 7 ) ) );
        assertEquals( "10.98", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2004, 8 ) ) );
        // This value is derived from organisation units beneath the actual
        // *sourceB*.
        assertEquals( "18.35", getDataValue( dataElementX, altCombo, sourceB, makeMonth( 2004, 7 ) ) );
    }

    @Test
    void testGetPredictionsSeasonalWithOutbreak()
    {
        setupTestData();
        String auid = dataElementA.getUid();
        Predictor p = createPredictor( dataElementX, altCombo, "GetPredictionsSeasonalWithOutbreak",
            new Expression( "avg(#{" + auid + "})+1.5*stddevPop(#{" + auid + "})", "descriptionA" ),
            new Expression( "isNotNull(#{" + dataElementB.getUid() + "})", "outbreak" ), periodTypeMonthly,
            orgUnitLevel1, 3, 1, 2 );
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2005, 12 ), summary );
        assertEquals( "Pred 1 Ins 99 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "5.0", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "5.5", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2001, 9 ) ) );
        assertEquals( "7.0", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2003, 1 ) ) );
        assertEquals( "8.75", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2003, 3 ) ) );
        assertEquals( "10.09", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2004, 7 ) ) );
        assertEquals( "10.1", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2004, 8 ) ) );
        // This value is derived from organisation units beneath the actual
        // *sourceB*.
        assertEquals( "15.75", getDataValue( dataElementX, altCombo, sourceB, makeMonth( 2004, 7 ) ) );
    }

    @Test
    void testPredictMultiLevelsWithDataElementOperandExpression()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceE, 1 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceE, 2 );
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceF, 4 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceF, 8 );
        dataValueBatchHandler.flush();
        Set<OrganisationUnitLevel> orgUnitLevels = Sets.newHashSet( orgUnitLevel1, orgUnitLevel2, orgUnitLevel3 );
        Predictor p = createPredictor( dataElementX, defaultCombo, "GetPredictionsMultiLevels",
            new Expression( "sum(#{" + dataElementA.getUid() + "." + defaultCombo.getUid() + "})", "descriptionA" ),
            null, periodTypeMonthly, orgUnitLevels, 2, 0, 0 );
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 9 ), summary );
        assertEquals( "Pred 1 Ins 8 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "1.0", getDataValue( dataElementX, defaultCombo, sourceE, makeMonth( 2001, 7 ) ) );
        assertEquals( "4.0", getDataValue( dataElementX, defaultCombo, sourceF, makeMonth( 2001, 7 ) ) );
        assertEquals( "5.0", getDataValue( dataElementX, defaultCombo, sourceD, makeMonth( 2001, 7 ) ) );
        assertEquals( "5.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 7 ) ) );
        assertEquals( "3.0", getDataValue( dataElementX, defaultCombo, sourceE, makeMonth( 2001, 8 ) ) );
        assertEquals( "12.0", getDataValue( dataElementX, defaultCombo, sourceF, makeMonth( 2001, 8 ) ) );
        assertEquals( "15.0", getDataValue( dataElementX, defaultCombo, sourceD, makeMonth( 2001, 8 ) ) );
        assertEquals( "15.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 8 ) ) );
        summary = new PredictionSummary();
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 9 ), summary );
        assertEquals( "Pred 1 Ins 0 Upd 0 Del 0 Unch 8", shortSummary( summary ) );
    }

    @Test
    void testPredictConstant()
    {
        setupTestData();
        Predictor p = createPredictor( dataElementX, defaultCombo, "PredictConstant", expressionC, null,
            periodTypeMonthly, orgUnitLevel1, 0, 0, 0 );
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 9 ), summary );
        assertEquals( "Pred 1 Ins 6 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "135.8", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "135.8", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "135.8", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 7 ) ) );
        assertEquals( "135.8", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 8 ) ) );
        assertEquals( "135.8", getDataValue( dataElementX, defaultCombo, sourceG, makeMonth( 2001, 7 ) ) );
        assertEquals( "135.8", getDataValue( dataElementX, defaultCombo, sourceG, makeMonth( 2001, 8 ) ) );
    }

    @Test
    void testPredictInteger()
    {
        setupTestData();
        Predictor p = createPredictor( dataElementY, defaultCombo, "PredictInteger", expressionC, null,
            periodTypeMonthly, orgUnitLevel1, 0, 0, 0 );
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 8 ), summary );
        assertEquals( "Pred 1 Ins 3 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "136", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "136", getDataValue( dataElementY, defaultCombo, sourceB, makeMonth( 2001, 7 ) ) );
        assertEquals( "136", getDataValue( dataElementY, defaultCombo, sourceG, makeMonth( 2001, 7 ) ) );
    }

    @Test
    void testPredictDays()
    {
        setupTestData();
        Predictor p = createPredictor( dataElementX, altCombo, "PredictDays", expressionD, null, periodTypeMonthly,
            orgUnitLevel1, 0, 0, 0 );
        predictionService.predict( p, monthStart( 2001, 8 ), monthStart( 2001, 10 ), summary );
        assertEquals( "Pred 1 Ins 6 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "31.0", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "30.0", getDataValue( dataElementX, altCombo, sourceA, makeMonth( 2001, 9 ) ) );
        assertEquals( "31.0", getDataValue( dataElementX, altCombo, sourceB, makeMonth( 2001, 8 ) ) );
        assertEquals( "30.0", getDataValue( dataElementX, altCombo, sourceB, makeMonth( 2001, 9 ) ) );
        assertEquals( "31.0", getDataValue( dataElementX, altCombo, sourceG, makeMonth( 2001, 8 ) ) );
        assertEquals( "30.0", getDataValue( dataElementX, altCombo, sourceG, makeMonth( 2001, 9 ) ) );
    }

    @Test
    void testPredictNoPeriods()
    {
        setupTestData();
        Predictor p = createPredictor( dataElementX, altCombo, "PredictDays", expressionD, null, periodTypeMonthly,
            orgUnitLevel1, 3, 1, 2 );
        predictionService.predict( p, monthStart( 2001, 8 ), monthStart( 2001, 8 ), summary );
        assertEquals( "Pred 1 Ins 0 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
    }

    @Test
    void testPredictWithCurrentPeriodData()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceA, 10 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceA, 20 );
        useDataValue( dataElementA, makeMonth( 2001, 8 ), sourceA, 30 );
        useDataValue( dataElementA, makeMonth( 2001, 9 ), sourceA, 40 );
        useDataValue( dataElementB, makeMonth( 2001, 7 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2001, 8 ), sourceA, 2 );
        useDataValue( dataElementB, makeMonth( 2001, 9 ), sourceA, 3 );
        useDataValue( dataElementB, makeMonth( 2001, 10 ), sourceA, 4 );
        dataValueBatchHandler.flush();
        Predictor p = createPredictor( dataElementX, defaultCombo, "PredictWithCurrentPeriodData", expressionE, null,
            periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 11 ), summary );
        assertEquals( "Pred 1 Ins 4 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "11.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "22.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "33.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 9 ) ) );
        assertEquals( "44.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 10 ) ) );
    }

    @Test
    void testPredictWithOnlyCurrentPeriodData()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceA, 10 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceA, 20 );
        useDataValue( dataElementA, makeMonth( 2001, 8 ), sourceA, 30 );
        useDataValue( dataElementA, makeMonth( 2001, 9 ), sourceA, 40 );
        useDataValue( dataElementB, makeMonth( 2001, 7 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2001, 8 ), sourceA, 2 );
        useDataValue( dataElementB, makeMonth( 2001, 9 ), sourceA, 3 );
        useDataValue( dataElementB, makeMonth( 2001, 10 ), sourceA, 4 );
        dataValueBatchHandler.flush();
        Predictor p = createPredictor( dataElementX, defaultCombo, "PredictWithOnlyCurrentPeriodData", expressionF,
            null, periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 11 ), summary );
        assertEquals( "Pred 1 Ins 4 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "1.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "2.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "3.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 9 ) ) );
        assertEquals( "4.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 10 ) ) );
    }

    @Test
    void testPredictMultipleDataElements()
    {
        useDataValue( dataElementA, makeMonth( 2010, 6 ), sourceA, 3 );
        useDataValue( dataElementB, makeMonth( 2010, 6 ), sourceA, 5 );
        dataValueBatchHandler.flush();
        Predictor p = createPredictor( dataElementX, defaultCombo, "A", expressionG, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( p, monthStart( 2010, 7 ), monthStart( 2010, 8 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "8.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2010, 7 ) ) );
    }

    @Test
    void testPredictMultipleAttributeOptionCombos()
    {
        CategoryOption optionJ = new CategoryOption( "CategoryOptionJ" );
        CategoryOption optionK = new CategoryOption( "CategoryOptionK" );
        CategoryOption optionL = new CategoryOption( "CategoryOptionL" );
        categoryService.addCategoryOption( optionJ );
        categoryService.addCategoryOption( optionK );
        categoryService.addCategoryOption( optionL );
        Category categoryJ = createCategory( 'J', optionJ, optionK );
        Category categoryL = createCategory( 'L', optionL );
        categoryJ.setDataDimension( true );
        categoryL.setDataDimension( true );
        categoryService.addCategory( categoryJ );
        categoryService.addCategory( categoryL );
        CategoryCombo categoryComboJL = createCategoryCombo( 'A', categoryJ, categoryL );
        categoryService.addCategoryCombo( categoryComboJL );
        CategoryOptionCombo optionComboJL = createCategoryOptionCombo( categoryComboJL, optionJ, optionK );
        CategoryOptionCombo optionComboKL = createCategoryOptionCombo( categoryComboJL, optionK, optionL );
        categoryService.addCategoryOptionCombo( optionComboJL );
        categoryService.addCategoryOptionCombo( optionComboKL );
        useDataValue( dataElementA, makeMonth( 2011, 6 ), sourceA, optionComboJL, 1 );
        useDataValue( dataElementB, makeMonth( 2011, 6 ), sourceA, optionComboJL, 2 );
        useDataValue( dataElementA, makeMonth( 2011, 6 ), sourceA, optionComboKL, 3 );
        useDataValue( dataElementB, makeMonth( 2011, 6 ), sourceA, optionComboKL, 4 );
        dataValueBatchHandler.flush();
        Predictor p = createPredictor( dataElementX, defaultCombo, "A", expressionG, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( p, monthStart( 2011, 7 ), monthStart( 2011, 8 ), summary );
        assertEquals( "Pred 1 Ins 2 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "3.0", getDataValue( dataElementX, defaultCombo, optionComboJL, sourceA, makeMonth( 2011, 7 ) ) );
        assertEquals( "7.0", getDataValue( dataElementX, defaultCombo, optionComboKL, sourceA, makeMonth( 2011, 7 ) ) );
    }

    @Test
    void testPredictIf()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceA, 10 );
        useDataValue( dataElementB, makeMonth( 2001, 6 ), sourceA, 10 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceA, 20 );
        useDataValue( dataElementB, makeMonth( 2001, 7 ), sourceA, 40 );
        dataValueBatchHandler.flush();
        Predictor p = createPredictor( dataElementX, defaultCombo, "PredictIf_A",
            new Expression( "if(#{" + dataElementB.getUid() + "} == #{" + dataElementA.getUid() + "},1,2)",
                "ExpressionIf_A" ),
            null, periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( p, monthStart( 2001, 6 ), monthStart( 2001, 8 ), summary );
        assertEquals( "Pred 1 Ins 2 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "1.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 6 ) ) );
        assertEquals( "2.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        p = createPredictor( dataElementX, defaultCombo, "PredictIf_B",
            new Expression( "sum(if(#{" + dataElementB.getUid() + "} < 2 * #{" + dataElementA.getUid() + "},3,4))",
                "ExpressionIf_B" ),
            null, periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );
        summary = new PredictionSummary();
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 9 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 1 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "3.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "4.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
        p = createPredictor( dataElementX, defaultCombo, "PredictIf_C",
            new Expression(
                "if(sum(#{" + dataElementB.getUid() + "}) != sum(2 * #{" + dataElementA.getUid() + "}),5,6)",
                "ExpressionIf_C" ),
            null, periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );
        summary = new PredictionSummary();
        predictionService.predict( p, monthStart( 2001, 7 ), monthStart( 2001, 9 ), summary );
        assertEquals( "Pred 1 Ins 0 Upd 2 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "5.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "6.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
    }

    @Test
    void testPredictIsNull()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2001, 6 ), sourceA, 2 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceA, 3 );
        dataValueBatchHandler.flush();
        Predictor p = createPredictor( dataElementX, defaultCombo, "PredictIsNull",
            new Expression( "#{" + dataElementA.getUid() + "} + if(isNull(#{" + dataElementB.getUid() + "}),5,#{"
                + dataElementB.getUid() + "})", "ExpressionIsNull" ),
            null, periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( p, monthStart( 2001, 6 ), monthStart( 2001, 8 ), summary );
        assertEquals( "Pred 1 Ins 2 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "3.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 6 ) ) );
        assertEquals( "8.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
    }

    @Test
    void testPredictStrategyNeverSkip()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceA, 1 );
        useDataValue( dataElementB, makeMonth( 2001, 6 ), sourceA, 2 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceA, 4 );
        dataValueBatchHandler.flush();
        Expression expressionX = new Expression(
            "10 + #{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}", "descriptionX",
            MissingValueStrategy.NEVER_SKIP );
        Expression expressionY = new Expression(
            "10 + sum( #{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "} )", "descriptionY",
            MissingValueStrategy.NEVER_SKIP );
        Expression expressionZ = new Expression(
            "10 + sum( #{" + dataElementA.getUid() + "} + firstNonNull(#{" + dataElementB.getUid() + "},0) )",
            "descriptionZ", MissingValueStrategy.NEVER_SKIP );
        Predictor predictorX = createPredictor( dataElementX, defaultCombo, "PredictNeverSkipX", expressionX, null,
            periodTypeMonthly, orgUnitLevel1, 0, 0, 0 );
        Predictor predictorY = createPredictor( dataElementY, defaultCombo, "PredictNeverSkipY", expressionY, null,
            periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );
        Predictor predictorZ = createPredictor( dataElementZ, defaultCombo, "PredictNeverSkipZ", expressionZ, null,
            periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( predictorX, monthStart( 2001, 6 ), monthStart( 2001, 9 ), summary );
        assertEquals( "Pred 1 Ins 9 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "13.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 6 ) ) );
        assertEquals( "14.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 6 ) ) );
        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 7 ) ) );
        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2001, 8 ) ) );
        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceG, makeMonth( 2001, 6 ) ) );
        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceG, makeMonth( 2001, 7 ) ) );
        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceG, makeMonth( 2001, 8 ) ) );
        summary = new PredictionSummary();
        predictionService.predict( predictorY, monthStart( 2001, 6 ), monthStart( 2001, 9 ), summary );
        assertEquals( "Pred 1 Ins 9 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "10", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 6 ) ) );
        assertEquals( "13", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "14", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "10", getDataValue( dataElementY, defaultCombo, sourceB, makeMonth( 2001, 6 ) ) );
        assertEquals( "10", getDataValue( dataElementY, defaultCombo, sourceB, makeMonth( 2001, 7 ) ) );
        assertEquals( "10", getDataValue( dataElementY, defaultCombo, sourceB, makeMonth( 2001, 8 ) ) );
        assertEquals( "10", getDataValue( dataElementY, defaultCombo, sourceG, makeMonth( 2001, 6 ) ) );
        assertEquals( "10", getDataValue( dataElementY, defaultCombo, sourceG, makeMonth( 2001, 7 ) ) );
        assertEquals( "10", getDataValue( dataElementY, defaultCombo, sourceG, makeMonth( 2001, 8 ) ) );
        summary = new PredictionSummary();
        predictionService.predict( predictorZ, monthStart( 2001, 6 ), monthStart( 2001, 9 ), summary );
        assertEquals( "Pred 1 Ins 9 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "10", getDataValue( dataElementZ, defaultCombo, sourceA, makeMonth( 2001, 6 ) ) );
        assertEquals( "13", getDataValue( dataElementZ, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        assertEquals( "14", getDataValue( dataElementZ, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
        assertEquals( "10", getDataValue( dataElementZ, defaultCombo, sourceB, makeMonth( 2001, 6 ) ) );
        assertEquals( "10", getDataValue( dataElementZ, defaultCombo, sourceB, makeMonth( 2001, 7 ) ) );
        assertEquals( "10", getDataValue( dataElementZ, defaultCombo, sourceB, makeMonth( 2001, 8 ) ) );
        assertEquals( "10", getDataValue( dataElementZ, defaultCombo, sourceG, makeMonth( 2001, 6 ) ) );
        assertEquals( "10", getDataValue( dataElementZ, defaultCombo, sourceG, makeMonth( 2001, 7 ) ) );
        assertEquals( "10", getDataValue( dataElementZ, defaultCombo, sourceG, makeMonth( 2001, 8 ) ) );
    }

    @Test
    void testPredictStrategySkipIfAllValuesMissing()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceG, 1 );
        useDataValue( dataElementB, makeMonth( 2001, 6 ), sourceG, 2 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceG, 4 );
        dataValueBatchHandler.flush();
        Expression expressionX = new Expression(
            "10 + #{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}", "descriptionY",
            MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING );
        Expression expressionY = new Expression(
            "10 + sum( #{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "} )", "descriptionX",
            MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING );
        Expression expressionZ = new Expression(
            "10 + sum( #{" + dataElementA.getUid() + "} + firstNonNull(#{" + dataElementB.getUid() + "},0) )",
            "descriptionZ", MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING );
        Predictor predictorX = createPredictor( dataElementX, defaultCombo, "PredictNeverSkipX", expressionX, null,
            periodTypeMonthly, orgUnitLevel1, 0, 0, 0 );
        Predictor predictorY = createPredictor( dataElementY, defaultCombo, "PredictNeverSkipY", expressionY, null,
            periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );
        Predictor predictorZ = createPredictor( dataElementZ, defaultCombo, "PredictNeverSkipZ", expressionZ, null,
            periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( predictorX, monthStart( 2001, 6 ), monthStart( 2001, 9 ), summary );
        assertEquals( "Pred 1 Ins 2 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "13.0", getDataValue( dataElementX, defaultCombo, sourceG, makeMonth( 2001, 6 ) ) );
        assertEquals( "14.0", getDataValue( dataElementX, defaultCombo, sourceG, makeMonth( 2001, 7 ) ) );
        summary = new PredictionSummary();
        predictionService.predict( predictorY, monthStart( 2001, 6 ), monthStart( 2001, 9 ), summary );
        assertEquals( "Pred 1 Ins 2 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "13", getDataValue( dataElementY, defaultCombo, sourceG, makeMonth( 2001, 7 ) ) );
        assertEquals( "14", getDataValue( dataElementY, defaultCombo, sourceG, makeMonth( 2001, 8 ) ) );
        summary = new PredictionSummary();
        predictionService.predict( predictorZ, monthStart( 2001, 6 ), monthStart( 2001, 9 ), summary );
        assertEquals( "Pred 1 Ins 2 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "13", getDataValue( dataElementZ, defaultCombo, sourceG, makeMonth( 2001, 7 ) ) );
        assertEquals( "14", getDataValue( dataElementZ, defaultCombo, sourceG, makeMonth( 2001, 8 ) ) );
    }

    @Test
    void testPredictStrategySkipIfAnyValueMissing()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceG, 1 );
        useDataValue( dataElementB, makeMonth( 2001, 6 ), sourceG, 2 );
        useDataValue( dataElementA, makeMonth( 2001, 7 ), sourceG, 4 );
        dataValueBatchHandler.flush();
        Expression expressionX = new Expression(
            "10 + #{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}", "descriptionY",
            MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING );
        Expression expressionY = new Expression(
            "10 + sum( #{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "} )", "descriptionX",
            MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING );
        Expression expressionZ = new Expression(
            "10 + sum( #{" + dataElementA.getUid() + "} + firstNonNull(#{" + dataElementB.getUid() + "},0) )",
            "descriptionZ", MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING );
        Predictor predictorX = createPredictor( dataElementX, defaultCombo, "PredictSkipIfAnyValueMissingX",
            expressionX, null, periodTypeMonthly, orgUnitLevel1, 0, 0, 0 );
        Predictor predictorY = createPredictor( dataElementY, defaultCombo, "PredictSkipIfAnyValueMissingY",
            expressionY, null, periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );
        Predictor predictorZ = createPredictor( dataElementZ, defaultCombo, "PredictSkipIfAnyValueMissingZ",
            expressionZ, null, periodTypeMonthly, orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( predictorX, monthStart( 2001, 6 ), monthStart( 2001, 9 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "13.0", getDataValue( dataElementX, defaultCombo, sourceG, makeMonth( 2001, 6 ) ) );
        summary = new PredictionSummary();
        predictionService.predict( predictorY, monthStart( 2001, 6 ), monthStart( 2001, 9 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "13", getDataValue( dataElementY, defaultCombo, sourceG, makeMonth( 2001, 7 ) ) );
        summary = new PredictionSummary();
        predictionService.predict( predictorZ, monthStart( 2001, 6 ), monthStart( 2001, 9 ), summary );
        assertEquals( "Pred 1 Ins 2 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "13", getDataValue( dataElementZ, defaultCombo, sourceG, makeMonth( 2001, 7 ) ) );
        assertEquals( "14", getDataValue( dataElementZ, defaultCombo, sourceG, makeMonth( 2001, 8 ) ) );
    }

    @Test
    void testPredictTaskPredictors()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceA, 10 );
        useDataValue( dataElementB, makeMonth( 2001, 6 ), sourceA, 20 );
        dataValueBatchHandler.flush();
        Predictor predictorA = createPredictor( dataElementX, defaultCombo, "A", expressionA, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        Predictor predictorB = createPredictor( dataElementY, defaultCombo, "B", expressionB, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictorService.addPredictor( predictorA );
        predictorService.addPredictor( predictorB );

        List<String> predictors = Lists.newArrayList( predictorA.getUid() );
        summary = predictionService.predictTask( monthStart( 2001, 7 ), monthStart( 2001, 8 ), predictors, null,
            progress );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        predictors = Lists.newArrayList( predictorA.getUid(), predictorB.getUid() );
        summary = predictionService.predictTask( monthStart( 2001, 7 ), monthStart( 2001, 8 ), predictors, null,
            progress );
        assertEquals( "Pred 2 Ins 1 Upd 0 Del 0 Unch 1", shortSummary( summary ) );
        assertEquals( "20", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        summary = predictionService.predictTask( monthStart( 2001, 7 ), monthStart( 2001, 8 ), predictors, null,
            progress );
        assertEquals( "Pred 2 Ins 0 Upd 0 Del 0 Unch 2", shortSummary( summary ) );
    }

    @Test
    void testPredictTaskPredictorGroups()
    {
        useDataValue( dataElementA, makeMonth( 2001, 6 ), sourceA, 10 );
        useDataValue( dataElementB, makeMonth( 2001, 6 ), sourceA, 20 );
        dataValueBatchHandler.flush();
        Predictor predictorA = createPredictor( dataElementX, defaultCombo, "A", expressionA, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        Predictor predictorB = createPredictor( dataElementY, defaultCombo, "B", expressionB, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictorService.addPredictor( predictorA );
        predictorService.addPredictor( predictorB );
        PredictorGroup predictorGroupA = createPredictorGroup( 'A' );
        predictorGroupA.addPredictor( predictorA );
        predictorService.addPredictorGroup( predictorGroupA );
        List<String> predictorGroups = Lists.newArrayList( predictorGroupA.getUid() );
        summary = predictionService.predictTask( monthStart( 2001, 7 ), monthStart( 2001, 8 ), null, predictorGroups,
            progress );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "10.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        predictorGroupA.addPredictor( predictorB );
        predictorService.updatePredictorGroup( predictorGroupA );
        summary = predictionService.predictTask( monthStart( 2001, 7 ), monthStart( 2001, 8 ), null, predictorGroups,
            progress );
        assertEquals( "Pred 2 Ins 1 Upd 0 Del 0 Unch 1", shortSummary( summary ) );
        assertEquals( "20", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
    }

    @Test
    void testPredictMedian()
    {
        useDataValue( dataElementA, makeMonth( 2001, 1 ), sourceA, 50 );
        useDataValue( dataElementA, makeMonth( 2001, 2 ), sourceA, 10 );
        useDataValue( dataElementA, makeMonth( 2001, 3 ), sourceA, 40 );
        useDataValue( dataElementA, makeMonth( 2001, 4 ), sourceA, 30 );
        useDataValue( dataElementA, makeMonth( 2001, 5 ), sourceA, 20 );
        dataValueBatchHandler.flush();
        Expression expressionM = new Expression( "median(#{" + dataElementA.getUid() + "})", "median" );
        Predictor predictorM = createPredictor( dataElementY, defaultCombo, "M", expressionM, null, periodTypeMonthly,
            orgUnitLevel1, 5, 0, 0 );
        predictorService.addPredictor( predictorM );
        predictionService.predict( predictorM, monthStart( 2001, 6 ), monthStart( 2001, 11 ), summary );
        assertEquals( "Pred 1 Ins 5 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        // Values_10_20_30_40_50
        assertEquals( "30", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 6 ) ) );
        // Values_10_20_30_40
        assertEquals( "25", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 7 ) ) );
        // Values_20_30_40
        assertEquals( "30", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 8 ) ) );
        // Values_20_30
        assertEquals( "25", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 9 ) ) );
        // Value_20
        assertEquals( "20", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 10 ) ) );
    }

    @Test
    void testPredictPercentileCont()
    {
        useDataValue( dataElementA, makeMonth( 2001, 1 ), sourceA, 10 );
        useDataValue( dataElementA, makeMonth( 2001, 2 ), sourceA, 30 );
        useDataValue( dataElementA, makeMonth( 2001, 3 ), sourceA, 20 );
        dataValueBatchHandler.flush();
        Expression expressionP25 = new Expression( "percentileCont(.25, #{" + dataElementA.getUid() + "})",
            "percentileCont25" );
        Expression expressionP50 = new Expression( "percentileCont(.50, #{" + dataElementA.getUid() + "})",
            "percentileCont20" );
        Predictor predictorP25 = createPredictor( dataElementY, defaultCombo, "2", expressionP25, null,
            periodTypeMonthly, orgUnitLevel1, 3, 0, 0 );
        Predictor predictorP50 = createPredictor( dataElementZ, defaultCombo, "5", expressionP50, null,
            periodTypeMonthly, orgUnitLevel1, 3, 0, 0 );
        predictorService.addPredictor( predictorP25 );
        predictorService.addPredictor( predictorP50 );
        predictionService.predict( predictorP25, monthStart( 2001, 4 ), monthStart( 2001, 6 ), summary );
        assertEquals( "Pred 1 Ins 2 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        // 25th_percentile_of_10_20_30
        assertEquals( "15", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 4 ) ) );
        // 25th_percentile_of_20_30
        assertEquals( "23", getDataValue( dataElementY, defaultCombo, sourceA, makeMonth( 2001, 5 ) ) );
        predictionService.predict( predictorP50, monthStart( 2001, 4 ), monthStart( 2001, 6 ), summary );
        assertEquals( "Pred 2 Ins 4 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        // 50th_percentile_of_10_20_30
        assertEquals( "20", getDataValue( dataElementZ, defaultCombo, sourceA, makeMonth( 2001, 4 ) ) );
        // 50th_percentile_of_20_30
        assertEquals( "25", getDataValue( dataElementZ, defaultCombo, sourceA, makeMonth( 2001, 5 ) ) );
    }

    @Test
    void testPredictNullDataValue()
    {
        useDataValue( dataElementA, makeMonth( 2010, 6 ), sourceA, 42 );
        useDataValue( dataElementA, makeMonth( 2010, 7 ), sourceA, null );
        dataValueBatchHandler.flush();
        Expression expression = new Expression( "sum(#{" + dataElementA.getUid() + "})", "description" );
        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expression, null, periodTypeMonthly,
            orgUnitLevel1, 2, 0, 0 );
        predictionService.predict( predictor, monthStart( 2010, 8 ), monthStart( 2010, 9 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "42.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2010, 8 ) ) );
    }

    @Test
    void testMissingValuesMin()
    {
        useDataValue( dataElementA, makeMonth( 2010, 8 ), sourceA, 33 );
        dataValueBatchHandler.flush();
        Expression expression = new Expression(
            "min(#{" + dataElementA.getUid() + "}) + #{" + dataElementA.getUid() + "}", "description",
            MissingValueStrategy.NEVER_SKIP );
        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expression, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( predictor, monthStart( 2010, 8 ), monthStart( 2010, 9 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "33.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2010, 8 ) ) );
    }

    @Test
    void testMissingValuesMax()
    {
        useDataValue( dataElementA, makeMonth( 2010, 8 ), sourceA, 33 );
        dataValueBatchHandler.flush();
        Expression expression = new Expression(
            "max(#{" + dataElementA.getUid() + "}) + #{" + dataElementA.getUid() + "}", "description",
            MissingValueStrategy.NEVER_SKIP );
        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expression, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( predictor, monthStart( 2010, 8 ), monthStart( 2010, 9 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "33.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2010, 8 ) ) );
    }

    @Test
    void testMissingValuesMedian()
    {
        useDataValue( dataElementA, makeMonth( 2010, 8 ), sourceA, 33 );
        dataValueBatchHandler.flush();
        Expression expression = new Expression(
            "median(#{" + dataElementA.getUid() + "}) + #{" + dataElementA.getUid() + "}", "description",
            MissingValueStrategy.NEVER_SKIP );
        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expression, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( predictor, monthStart( 2010, 8 ), monthStart( 2010, 9 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "33.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2010, 8 ) ) );
    }

    @Test
    void testMissingValuesSum()
    {
        useDataValue( dataElementA, makeMonth( 2010, 8 ), sourceA, 33 );
        dataValueBatchHandler.flush();
        Expression expression = new Expression(
            "sum(#{" + dataElementA.getUid() + "}) + #{" + dataElementA.getUid() + "}", "description",
            MissingValueStrategy.NEVER_SKIP );
        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expression, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( predictor, monthStart( 2010, 8 ), monthStart( 2010, 9 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "33.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2010, 8 ) ) );
    }

    @Test
    void testMissingValuesCount()
    {
        useDataValue( dataElementA, makeMonth( 2010, 8 ), sourceA, 33 );
        dataValueBatchHandler.flush();
        Expression expression = new Expression(
            "count(#{" + dataElementA.getUid() + "}) + #{" + dataElementA.getUid() + "}", "description" );
        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expression, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( predictor, monthStart( 2010, 8 ), monthStart( 2010, 9 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "33.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2010, 8 ) ) );
    }

    @Test
    void testMissingValuesStddevSamp()
    {
        useDataValue( dataElementA, makeMonth( 2010, 8 ), sourceA, 33 );
        dataValueBatchHandler.flush();
        Expression expression = new Expression(
            "stddevSamp(#{" + dataElementA.getUid() + "}) + #{" + dataElementA.getUid() + "}", "description" );
        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expression, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( predictor, monthStart( 2010, 8 ), monthStart( 2010, 9 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "33.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2010, 8 ) ) );
    }

    @Test
    void testMissingValuesStddevPop()
    {
        useDataValue( dataElementA, makeMonth( 2010, 8 ), sourceA, 33 );
        dataValueBatchHandler.flush();
        Expression expression = new Expression(
            "stddevPop(#{" + dataElementA.getUid() + "}) + #{" + dataElementA.getUid() + "}", "description" );
        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expression, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( predictor, monthStart( 2010, 8 ), monthStart( 2010, 9 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "33.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2010, 8 ) ) );
    }

    @Test
    void testMissingValuesPercentileCont()
    {
        useDataValue( dataElementA, makeMonth( 2010, 8 ), sourceA, 33 );
        dataValueBatchHandler.flush();
        Expression expression = new Expression(
            "percentileCont(#{" + dataElementA.getUid() + "}) + #{" + dataElementA.getUid() + "}", "description" );
        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expression, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( predictor, monthStart( 2010, 8 ), monthStart( 2010, 9 ), summary );
        assertEquals( "Pred 1 Ins 0 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
    }

    @Test
    void testMissingValuesAvg()
    {
        useDataValue( dataElementA, makeMonth( 2010, 8 ), sourceA, 33 );
        dataValueBatchHandler.flush();
        Expression expression = new Expression(
            "avg(#{" + dataElementA.getUid() + "}) + #{" + dataElementA.getUid() + "}", "description",
            MissingValueStrategy.NEVER_SKIP );
        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expression, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( predictor, monthStart( 2010, 8 ), monthStart( 2010, 9 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "33.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2010, 8 ) ) );
    }

    @Test
    void testPredictOrgUnitAncestor()
    {
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceA, 1 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceB, 2 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceC, 4 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceD, 8 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceE, 16 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceF, 32 );
        dataValueBatchHandler.flush();
        Expression expression = new Expression( "if(orgUnit.ancestor( " + sourceC.getUid() + " , " + sourceD.getUid()
            + " ), #{" + dataElementA.getUid() + "}, 64)", "description",
            MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING );
        Set<OrganisationUnitLevel> allLevels = Sets.newHashSet( orgUnitLevel1, orgUnitLevel2, orgUnitLevel3 );
        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expression, null, periodTypeMonthly,
            allLevels, 1, 0, 0 );
        predictionService.predict( predictor, monthStart( 2021, 8 ), monthStart( 2021, 9 ), summary );
        assertEquals( "Pred 1 Ins 6 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "64.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2021, 8 ) ) );
        assertEquals( "64.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2021, 8 ) ) );
        assertEquals( "64.0", getDataValue( dataElementX, defaultCombo, sourceC, makeMonth( 2021, 8 ) ) );
        assertEquals( "64.0", getDataValue( dataElementX, defaultCombo, sourceD, makeMonth( 2021, 8 ) ) );
        assertEquals( "16.0", getDataValue( dataElementX, defaultCombo, sourceE, makeMonth( 2021, 8 ) ) );
        assertEquals( "32.0", getDataValue( dataElementX, defaultCombo, sourceF, makeMonth( 2021, 8 ) ) );
    }

    @Test
    void testPredictOrgUnitGroup()
    {
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceA, 1 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceB, 2 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceC, 4 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceD, 8 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceE, 16 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceF, 32 );
        dataValueBatchHandler.flush();
        Expression expression = new Expression( "if(orgUnit.group( " + ouGroupA.getUid() + " , " + ouGroupB.getUid()
            + " ), #{" + dataElementA.getUid() + "}, 64)", "description",
            MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING );
        Set<OrganisationUnitLevel> allLevels = Sets.newHashSet( orgUnitLevel1, orgUnitLevel2, orgUnitLevel3 );
        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expression, null, periodTypeMonthly,
            allLevels, 1, 0, 0 );
        predictionService.predict( predictor, monthStart( 2021, 8 ), monthStart( 2021, 9 ), summary );
        assertEquals( "Pred 1 Ins 6 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "1.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2021, 8 ) ) );
        assertEquals( "64.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2021, 8 ) ) );
        assertEquals( "4.0", getDataValue( dataElementX, defaultCombo, sourceC, makeMonth( 2021, 8 ) ) );
        assertEquals( "56.0", getDataValue( dataElementX, defaultCombo, sourceD, makeMonth( 2021, 8 ) ) );
        assertEquals( "64.0", getDataValue( dataElementX, defaultCombo, sourceE, makeMonth( 2021, 8 ) ) );
        assertEquals( "32.0", getDataValue( dataElementX, defaultCombo, sourceF, makeMonth( 2021, 8 ) ) );
    }

    @Test
    void testPredictOrgUnitDataSet()
    {
        DataSet dataSetA = createDataSet( 'A', periodTypeMonthly );
        DataSet dataSetB = createDataSet( 'B', periodTypeMonthly );
        dataSetA.addOrganisationUnit( sourceA );
        dataSetA.addOrganisationUnit( sourceC );
        dataSetB.addOrganisationUnit( sourceD );
        dataSetB.addOrganisationUnit( sourceF );
        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );

        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceA, 1 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceB, 2 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceC, 4 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceD, 8 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceE, 16 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceF, 32 );
        dataValueBatchHandler.flush();

        Expression expression = new Expression( "if(orgUnit.dataSet( " + dataSetA.getUid() + " , " + dataSetB.getUid()
            + " ), #{" + dataElementA.getUid() + "}, 64)", "description",
            MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING );
        Set<OrganisationUnitLevel> allLevels = Sets.newHashSet( orgUnitLevel1, orgUnitLevel2, orgUnitLevel3 );

        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expression, null, periodTypeMonthly,
            allLevels, 1, 0, 0 );
        predictionService.predict( predictor, monthStart( 2021, 8 ), monthStart( 2021, 9 ), summary );

        assertEquals( "Pred 1 Ins 6 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "1.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2021, 8 ) ) );
        assertEquals( "64.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2021, 8 ) ) );
        assertEquals( "4.0", getDataValue( dataElementX, defaultCombo, sourceC, makeMonth( 2021, 8 ) ) );
        assertEquals( "56.0", getDataValue( dataElementX, defaultCombo, sourceD, makeMonth( 2021, 8 ) ) );
        assertEquals( "64.0", getDataValue( dataElementX, defaultCombo, sourceE, makeMonth( 2021, 8 ) ) );
        assertEquals( "32.0", getDataValue( dataElementX, defaultCombo, sourceF, makeMonth( 2021, 8 ) ) );
    }

    @Test
    void testPredictOrgUnitProgram()
    {
        Program programA = createProgram( 'A' );
        Program programB = createProgram( 'B' );
        programA.addOrganisationUnit( sourceA );
        programA.addOrganisationUnit( sourceC );
        programB.addOrganisationUnit( sourceD );
        programB.addOrganisationUnit( sourceF );
        programService.addProgram( programA );
        programService.addProgram( programB );

        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceA, 1 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceB, 2 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceC, 4 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceD, 8 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceE, 16 );
        useDataValue( dataElementA, makeMonth( 2021, 8 ), sourceF, 32 );
        dataValueBatchHandler.flush();

        Expression expression = new Expression( "if(orgUnit.program( " + programA.getUid() + " , " + programB.getUid()
            + " ), #{" + dataElementA.getUid() + "}, 64)", "description",
            MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING );
        Set<OrganisationUnitLevel> allLevels = Sets.newHashSet( orgUnitLevel1, orgUnitLevel2, orgUnitLevel3 );

        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expression, null, periodTypeMonthly,
            allLevels, 1, 0, 0 );
        predictionService.predict( predictor, monthStart( 2021, 8 ), monthStart( 2021, 9 ), summary );

        assertEquals( "Pred 1 Ins 6 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "1.0", getDataValue( dataElementX, defaultCombo, sourceA, makeMonth( 2021, 8 ) ) );
        assertEquals( "64.0", getDataValue( dataElementX, defaultCombo, sourceB, makeMonth( 2021, 8 ) ) );
        assertEquals( "4.0", getDataValue( dataElementX, defaultCombo, sourceC, makeMonth( 2021, 8 ) ) );
        assertEquals( "56.0", getDataValue( dataElementX, defaultCombo, sourceD, makeMonth( 2021, 8 ) ) );
        assertEquals( "64.0", getDataValue( dataElementX, defaultCombo, sourceE, makeMonth( 2021, 8 ) ) );
        assertEquals( "32.0", getDataValue( dataElementX, defaultCombo, sourceF, makeMonth( 2021, 8 ) ) );
    }

    @Test
    void testPredictCarryingForwardPredictedDataElement()
    {
        useDataValue( dataElementA, makeMonth( 2010, 8 ), sourceA, 1 );
        dataValueBatchHandler.flush();
        Expression expression = new Expression( "2 * sum(#{" + dataElementA.getUid() + "})", "description",
            MissingValueStrategy.NEVER_SKIP );
        Predictor predictor = createPredictor( dataElementA, defaultCombo, "A", expression, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( predictor, monthStart( 2010, 9 ), monthStart( 2010, 12 ), summary );
        assertEquals( "Pred 1 Ins 3 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "1", getDataValue( dataElementA, defaultCombo, sourceA, makeMonth( 2010, 8 ) ) );
        assertEquals( "2", getDataValue( dataElementA, defaultCombo, sourceA, makeMonth( 2010, 9 ) ) );
        assertEquals( "4", getDataValue( dataElementA, defaultCombo, sourceA, makeMonth( 2010, 10 ) ) );
        assertEquals( "8", getDataValue( dataElementA, defaultCombo, sourceA, makeMonth( 2010, 11 ) ) );
    }

    @Test
    void testPredictCarryingForwardPredictedDataElementOperand()
    {
        useDataValue( dataElementA, makeMonth( 2010, 8 ), sourceA, 1 );
        dataValueBatchHandler.flush();
        Expression expression = new Expression(
            "3 * sum(#{" + dataElementA.getUid() + "." + defaultCombo.getUid() + "})", "description",
            MissingValueStrategy.NEVER_SKIP );
        Predictor predictor = createPredictor( dataElementA, defaultCombo, "A", expression, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( predictor, monthStart( 2010, 9 ), monthStart( 2010, 12 ), summary );
        assertEquals( "Pred 1 Ins 3 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "1", getDataValue( dataElementA, defaultCombo, sourceA, makeMonth( 2010, 8 ) ) );
        assertEquals( "3", getDataValue( dataElementA, defaultCombo, sourceA, makeMonth( 2010, 9 ) ) );
        assertEquals( "9", getDataValue( dataElementA, defaultCombo, sourceA, makeMonth( 2010, 10 ) ) );
        assertEquals( "27", getDataValue( dataElementA, defaultCombo, sourceA, makeMonth( 2010, 11 ) ) );
    }

    @Test
    void testPredictToReplaceDeletedValue()
    {
        useDataValue( dataElementA, makeMonth( 2010, 8 ), sourceA, defaultCombo, 22 );
        useDataValue( dataElementA, makeMonth( 2010, 9 ), sourceA, defaultCombo, 33 );
        useDataValue( dataElementZ, makeMonth( 2010, 8 ), sourceA, defaultCombo, 22, true );
        useDataValue( dataElementZ, makeMonth( 2010, 9 ), sourceA, defaultCombo, 1, true );
        dataValueBatchHandler.flush();
        Expression expression = new Expression( "#{" + dataElementA.getUid() + "." + defaultCombo.getUid() + "}",
            "description", MissingValueStrategy.NEVER_SKIP );
        Predictor predictor = createPredictor( dataElementZ, defaultCombo, "A", expression, null, periodTypeMonthly,
            orgUnitLevel1, 0, 0, 0 );
        predictionService.predict( predictor, monthStart( 2010, 8 ), monthStart( 2010, 10 ), summary );
        assertEquals( "Pred 1 Ins 0 Upd 2 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "22", getDataValue( dataElementZ, defaultCombo, sourceA, makeMonth( 2010, 8 ) ) );
        assertEquals( "33", getDataValue( dataElementZ, defaultCombo, sourceA, makeMonth( 2010, 9 ) ) );
    }

    @Test
    void testPredictString()
    {
        useDataValue( dataElementE, makeMonth( 2021, 8 ), sourceA, defaultCombo, "Hello" );
        dataValueBatchHandler.flush();
        String expr = "if( isNull(#{" + dataElementE.getUid() + "}), 'was null', #{" + dataElementE.getUid() + "} )";
        Expression expression = new Expression( expr, "description", MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING );
        Predictor p = createPredictor( dataElementF, defaultCombo, "PredictString", expression, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( p, monthStart( 2021, 8 ), monthStart( 2021, 9 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "Hello", getDataValue( dataElementF, defaultCombo, sourceA, makeMonth( 2021, 8 ) ) );
    }

    @Test
    void testPredictDate()
    {
        useDataValue( dataElementG, makeMonth( 2021, 8 ), sourceA, defaultCombo, "2021-09-10" );
        dataValueBatchHandler.flush();
        String expr = "if( isNull(#{" + dataElementG.getUid() + "}), '1999-01-01', #{" + dataElementG.getUid() + "} )";
        Expression expression = new Expression( expr, "description", MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING );
        Predictor p = createPredictor( dataElementH, defaultCombo, "PredictString", expression, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( p, monthStart( 2021, 8 ), monthStart( 2021, 9 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "2021-09-10", getDataValue( dataElementH, defaultCombo, sourceA, makeMonth( 2021, 8 ) ) );
    }

    @Test
    void testPredictBoolean()
    {
        useDataValue( dataElementI, makeMonth( 2021, 8 ), sourceA, defaultCombo, "true" );
        dataValueBatchHandler.flush();
        String expr = "if( isNull(#{" + dataElementI.getUid() + "}), 'false', #{" + dataElementI.getUid() + "} )";
        Expression expression = new Expression( expr, "description", MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING );
        Predictor p = createPredictor( dataElementJ, defaultCombo, "PredictString", expression, null, periodTypeMonthly,
            orgUnitLevel1, 1, 0, 0 );
        predictionService.predict( p, monthStart( 2021, 8 ), monthStart( 2021, 9 ), summary );
        assertEquals( "Pred 1 Ins 1 Upd 0 Del 0 Unch 0", shortSummary( summary ) );
        assertEquals( "true", getDataValue( dataElementJ, defaultCombo, sourceA, makeMonth( 2021, 8 ) ) );
    }

    @Test
    void testOrderWithinPredictorGroup()
    {
        useDataValue( dataElementA, makeMonth( 2021, 12 ), sourceA, defaultCombo, "0" );
        dataValueBatchHandler.flush();
        // 0 + 1 * 2 + 2 * 3 + 3 * 4 + 4 = 64, if operations are in order
        Expression e1 = new Expression( "#{" + dataElementA.getUid() + "} + 1", "e1" );
        Expression e2 = new Expression( "#{" + dataElementA.getUid() + "} * 2", "e2" );
        Expression e3 = new Expression( "#{" + dataElementA.getUid() + "} + 2", "e3" );
        Expression e4 = new Expression( "#{" + dataElementA.getUid() + "} * 3", "e4" );
        Expression e5 = new Expression( "#{" + dataElementA.getUid() + "} + 3", "e5" );
        Expression e6 = new Expression( "#{" + dataElementA.getUid() + "} * 4", "e6" );
        Expression e7 = new Expression( "#{" + dataElementA.getUid() + "} + 4", "e7" );
        Predictor p1 = createPredictor( dataElementA, defaultCombo, "p1", e1, null, periodTypeMonthly, orgUnitLevel1, 1,
            0, 0 );
        Predictor p2 = createPredictor( dataElementA, defaultCombo, "p2", e2, null, periodTypeMonthly, orgUnitLevel1, 1,
            0, 0 );
        Predictor p3 = createPredictor( dataElementA, defaultCombo, "p3", e3, null, periodTypeMonthly, orgUnitLevel1, 1,
            0, 0 );
        Predictor p4 = createPredictor( dataElementA, defaultCombo, "p4", e4, null, periodTypeMonthly, orgUnitLevel1, 1,
            0, 0 );
        Predictor p5 = createPredictor( dataElementA, defaultCombo, "p5", e5, null, periodTypeMonthly, orgUnitLevel1, 1,
            0, 0 );
        Predictor p6 = createPredictor( dataElementA, defaultCombo, "p6", e6, null, periodTypeMonthly, orgUnitLevel1, 1,
            0, 0 );
        Predictor p7 = createPredictor( dataElementA, defaultCombo, "p7", e7, null, periodTypeMonthly, orgUnitLevel1, 1,
            0, 0 );
        predictorService.addPredictor( p1 );
        predictorService.addPredictor( p2 );
        predictorService.addPredictor( p3 );
        predictorService.addPredictor( p4 );
        predictorService.addPredictor( p5 );
        predictorService.addPredictor( p6 );
        predictorService.addPredictor( p7 );
        PredictorGroup predictorGroup = createPredictorGroup( 'A', p1, p2, p3, p4, p5, p6, p7 );
        predictorService.addPredictorGroup( predictorGroup );
        predictionService.predictTask( monthStart( 2021, 12 ), monthStart( 2022, 1 ), null,
            Lists.newArrayList( "predictorgA" ), progress );
        assertEquals( "64", getDataValue( dataElementA, defaultCombo, sourceA, makeMonth( 2021, 12 ) ) );
    }
}
