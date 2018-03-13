package org.hisp.dhis.predictor;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class PredictorServiceTest
    extends DhisSpringTest
{
    @Autowired
    private PredictorService predictorService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private ExpressionService expressionService;

    private OrganisationUnitLevel orgUnitLevel1;
    
    private DataElement dataElementA;
    private DataElement dataElementB;
    private DataElement dataElementC;
    private DataElement dataElementD;
    private DataElement dataElementX;

    private DataElementCategoryOptionCombo defaultCombo;

    private DataElementCategoryOptionCombo altCombo;

    DataElementCategoryOption altCategoryOption;
    DataElementCategory altDataElementCategory;
    DataElementCategoryCombo altDataElementCategoryCombo;

    private Set<DataElement> dataElements;

    private Set<DataElementCategoryOptionCombo> optionCombos;

    private Expression expressionA;
    private Expression expressionB;
    private Expression expressionC;

    private PeriodType periodTypeMonthly;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    {
        orgUnitLevel1 = new OrganisationUnitLevel( 1, "Level1" );

        organisationUnitService.addOrganisationUnitLevel( orgUnitLevel1 );
        
        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementC = createDataElement( 'C' );
        dataElementD = createDataElement( 'D' );
        dataElementX = createDataElement( 'X', ValueType.NUMBER, AggregationType.NONE );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );
        dataElementService.addDataElement( dataElementX );

        dataElements = new HashSet<>();

        dataElements.add( dataElementA );
        dataElements.add( dataElementB );
        dataElements.add( dataElementC );
        dataElements.add( dataElementD );

        periodTypeMonthly = PeriodType.getPeriodTypeByName( "Monthly" );

        DataElementCategoryOptionCombo categoryOptionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();

        defaultCombo = categoryService.getDefaultDataElementCategoryOptionCombo();

        altCategoryOption = new DataElementCategoryOption( "AltCategoryOption" );
        categoryService.addDataElementCategoryOption( altCategoryOption );
        altDataElementCategory = createDataElementCategory( 'A', altCategoryOption );
        categoryService.addDataElementCategory( altDataElementCategory );

        altDataElementCategoryCombo = createCategoryCombo( 'Y', altDataElementCategory );
        categoryService.addDataElementCategoryCombo( altDataElementCategoryCombo );

        altCombo = createCategoryOptionCombo( 'Z', altDataElementCategoryCombo, altCategoryOption );

        optionCombos = new HashSet<>();
        optionCombos.add( categoryOptionCombo );
        optionCombos.add( altCombo );

        categoryService.addDataElementCategoryOptionCombo( altCombo );

        expressionA = new Expression(
            "AVG(#{" + dataElementA.getUid() + "})+1.5*STDDEV(#{" + dataElementA.getUid() + "})", "descriptionA" );
        expressionB = new Expression( "AVG(#{" + dataElementB.getUid() + "." + defaultCombo.getUid() + "})", "descriptionB" );
        expressionC = new Expression( "135.79", "descriptionC" );

        expressionService.addExpression( expressionA );
        expressionService.addExpression( expressionB );
        expressionService.addExpression( expressionC );
    }

    // -------------------------------------------------------------------------
    // CRUD tests
    // -------------------------------------------------------------------------

    @Test
    public void testSaveGetPredictor()
    {
        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expressionA, expressionB, periodTypeMonthly, orgUnitLevel1,
            6, 1, 0 );
        Set<OrganisationUnitLevel> levels = new HashSet<OrganisationUnitLevel>();
        levels.add( orgUnitLevel1 );

        int id = predictorService.addPredictor( predictor );

        predictor = predictorService.getPredictor( id );

        assertEquals( predictor.getName(), "PredictorA" );
        assertEquals( predictor.getDescription(), "DescriptionA" );
        assertEquals( predictor.getGenerator(), expressionA );
        assertEquals( predictor.getSampleSkipTest(), expressionB );
        assertEquals( predictor.getPeriodType(), periodTypeMonthly );
        assertEquals( predictor.getOutput(), dataElementX );
        assertEquals( predictor.getAnnualSampleCount(), new Integer( 0 ) );
        assertEquals( predictor.getSequentialSampleCount(), new Integer( 6 ) );
        assertEquals( predictor.getSequentialSkipCount(), new Integer( 1 ) );
        assertEquals( predictor.getOrganisationUnitLevels(), levels );
    }

    @Test
    public void testSaveGetPredictorAlt()
    {
        Predictor predictor = createPredictor( dataElementA, altCombo, "B", expressionC, null, periodTypeMonthly, orgUnitLevel1,
            6, 1, 0 );
        Set<OrganisationUnitLevel> levels = new HashSet<OrganisationUnitLevel>();
        levels.add( orgUnitLevel1 );

        int id = predictorService.addPredictor( predictor );

        predictor = predictorService.getPredictor( id );

        assertEquals( predictor.getName(), "PredictorB" );
        assertEquals( predictor.getDescription(), "DescriptionB" );
        assertEquals( predictor.getGenerator(), expressionC );
        assertNull( predictor.getSampleSkipTest() );
        assertEquals( predictor.getPeriodType(), periodTypeMonthly );
        assertEquals( predictor.getOutput(), dataElementA );
        assertEquals( predictor.getAnnualSampleCount(), new Integer( 0 ) );
        assertEquals( predictor.getSequentialSampleCount(), new Integer( 6 ) );
        assertEquals( predictor.getSequentialSkipCount(), new Integer( 1 ) );
        assertEquals( predictor.getOrganisationUnitLevels(), levels );
    }

    @Test
    public void testUpdatePredictor()
    {
        Predictor predictor = createPredictor( dataElementX, altCombo, "A", expressionA, expressionB, periodTypeMonthly, orgUnitLevel1, 6, 1, 0 );

        int id = predictorService.addPredictor( predictor );

        predictor = predictorService.getPredictor( id );

        assertEquals( predictor.getName(), "PredictorA" );
        assertEquals( predictor.getDescription(), "DescriptionA" );
        assertNotNull( predictor.getGenerator().getExpression() );
        assertEquals( predictor.getPeriodType(), periodTypeMonthly );

        predictor.setName( "PredictorB" );
        predictor.setDescription( "DescriptionB" );
        predictor.setSequentialSkipCount( 2 );

        predictorService.updatePredictor( predictor );

        predictor = predictorService.getPredictor( id );

        assertEquals( predictor.getName(), "PredictorB" );
        assertEquals( predictor.getDescription(), "DescriptionB" );
        assertEquals( predictor.getSequentialSkipCount(), new Integer( 2 ) );
    }

    @Test
    public void testDeletePredictor()
    {
        Predictor predictorA = createPredictor( dataElementX, defaultCombo, "A", expressionA, expressionB,
            periodTypeMonthly, orgUnitLevel1, 6, 1, 0 );
        Predictor predictorB = createPredictor( dataElementX, altCombo, "B", expressionA, expressionB,
            periodTypeMonthly, orgUnitLevel1, 6, 1, 0 );

        int idA = predictorService.addPredictor( predictorA );
        int idB = predictorService.addPredictor( predictorB );

        assertNotNull( predictorService.getPredictor( idA ) );
        assertNotNull( predictorService.getPredictor( idB ) );

        predictorService.deletePredictor( predictorA );

        //TODO: Resolve error with following "org.hibernate.ObjectDeletedException: deleted object would be re-saved by cascade (remove deleted object from associations)"
//        assertNull( predictorService.getPredictor( idA ) );
//        assertNotNull( predictorService.getPredictor( idB ) );

//        predictorService.deletePredictor( predictorB );

//        assertNull( predictorService.getPredictor( idA ) );
//        assertNull( predictorService.getPredictor( idB ) );
    }

    @Test
    public void testGetAllPredictors()
    {
        Predictor predictorA = createPredictor( dataElementX, defaultCombo, "A", expressionA, expressionB,
            periodTypeMonthly, orgUnitLevel1, 6, 1, 0 );
        Predictor predictorB = createPredictor( dataElementX, altCombo, "B", expressionA, expressionB,
            periodTypeMonthly, orgUnitLevel1, 6, 1, 0 );

        predictorService.addPredictor( predictorA );
        predictorService.addPredictor( predictorB );

        List<Predictor> predictors = predictorService.getAllPredictors();

        assertTrue( predictors.size() == 2 );
        assertTrue( predictors.contains( predictorA ) );
        assertTrue( predictors.contains( predictorB ) );
    }

    @Test
    public void testGetPredictorByName()
    {
        Predictor predictorA = createPredictor( dataElementX, defaultCombo, "A", expressionA, expressionB,
            periodTypeMonthly, orgUnitLevel1, 6, 1, 0 );
        Predictor predictorB = createPredictor( dataElementX, altCombo, "B", expressionA, expressionB,
            periodTypeMonthly, orgUnitLevel1, 6, 1, 0 );

        int id = predictorService.addPredictor( predictorA );
        predictorService.addPredictor( predictorB );

        List<Predictor> p = predictorService.getPredictorsByName( "PredictorA" );

        assertEquals( p.size(), 1 );
        assertEquals( p.get( 0 ).getId(), id );

        assertEquals( p.get( 0 ).getName(), "PredictorA" );
    }

    @Test
    public void testGetPredictorCount()
    {
        Set<DataElement> dataElementsA = new HashSet<>();
        dataElementsA.add( dataElementA );
        dataElementsA.add( dataElementB );

        Set<DataElement> dataElementsB = new HashSet<>();
        dataElementsB.add( dataElementC );
        dataElementsB.add( dataElementD );

        Set<DataElement> dataElementsD = new HashSet<>();
        dataElementsD.addAll( dataElementsA );
        dataElementsD.addAll( dataElementsB );

        Expression expression1 = new Expression( "Expression1", "Expression1" );
        Expression expression2 = new Expression( "Expression2", "Expression2" );
        Expression expression3 = new Expression( "Expression3", "Expression3" );

        expressionService.addExpression( expression1 );
        expressionService.addExpression( expression2 );
        expressionService.addExpression( expression3 );

        Predictor predictorA = createPredictor( dataElementX, altCombo, "A", expressionA, expressionB,
            periodTypeMonthly, orgUnitLevel1, 6, 1, 0 );
        Predictor predictorB = createPredictor( dataElementX, defaultCombo, "B", expressionA, expressionB,
            periodTypeMonthly, orgUnitLevel1, 6, 1, 0 );
        Predictor predictorC = createPredictor( dataElementX, altCombo, "C", expressionA, expressionB,
            periodTypeMonthly, orgUnitLevel1, 6, 1, 0 );

        predictorService.addPredictor( predictorA );
        predictorService.addPredictor( predictorB );
        predictorService.addPredictor( predictorC );

        assertNotNull( predictorService.getPredictorCount() );
        assertEquals( 3, predictorService.getPredictorCount() );
    }
}
