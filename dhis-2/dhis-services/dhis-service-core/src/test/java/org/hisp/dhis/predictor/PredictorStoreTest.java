package org.hisp.dhis.predictor;

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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
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
public class PredictorStoreTest
    extends DhisSpringTest
{
    @Autowired
    private PredictorStore predictorStore;

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

    private Set<DataElementCategoryOptionCombo> optionCombos;

    private DataElementCategoryOptionCombo defaultCombo;

    private Expression expressionA;

    private Expression expressionB;

    private PeriodType periodType;
    
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
        dataElementX = createDataElement( 'X' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );
        dataElementService.addDataElement( dataElementX );

        DataElementCategoryOptionCombo categoryOptionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();
        defaultCombo = categoryOptionCombo;

        optionCombos = new HashSet<>();
        optionCombos.add( categoryOptionCombo );

        expressionA = new Expression( "expressionA", "descriptionA" );
        expressionB = new Expression( "expressionB", "descriptionB" );

        expressionService.addExpression( expressionB );
        expressionService.addExpression( expressionA );

        periodType = PeriodType.getAvailablePeriodTypes().iterator().next();
    }

    // -------------------------------------------------------------------------
    // predictor
    // -------------------------------------------------------------------------

    @Test
    public void testSavePredictor()
    {
        Predictor predictorA = createPredictor( dataElementX, defaultCombo, "A", expressionA, expressionB, periodType, orgUnitLevel1, 6, 1, 0 );
        Predictor predictorB = createPredictor( dataElementX, defaultCombo, "B", expressionA, expressionB, periodType, orgUnitLevel1, 6, 1, 0 );

        predictorStore.save( predictorA );
        int idA = predictorA.getId();
        Set<OrganisationUnitLevel> levelsA = predictorA.getOrganisationUnitLevels();
        Set<OrganisationUnitLevel> expectedLevelsA = new HashSet<OrganisationUnitLevel>();
        expectedLevelsA.add( orgUnitLevel1 );

        predictorA = predictorStore.get( idA );

        levelsA = predictorA.getOrganisationUnitLevels();

        assertEquals( predictorA.getName(), "PredictorA" );
        assertEquals( predictorA.getDescription(), "DescriptionA" );
        assertNotNull( predictorA.getGenerator().getExpression() );
        // TODO Need a good skipTest test
        assertEquals( predictorA.getPeriodType(), periodType );
        assertEquals( predictorA.getOutput(), dataElementX );
        assertEquals( predictorA.getAnnualSampleCount(), new Integer( 0 ) );
        assertEquals( predictorA.getSequentialSampleCount(), new Integer( 6 ) );
        assertEquals( predictorA.getSequentialSkipCount(), new Integer( 1 ) );
        assertEquals( levelsA.size(), 1 );
        assertEquals( levelsA, expectedLevelsA );

        predictorStore.save( predictorB );
        int idB = predictorB.getId();
        Set<OrganisationUnitLevel> levelsB = predictorB.getOrganisationUnitLevels();
        Set<OrganisationUnitLevel> expectedLevelsB = new HashSet<OrganisationUnitLevel>();
        expectedLevelsB.add( orgUnitLevel1 );

        predictorB = predictorStore.get( idB );

        levelsB = predictorB.getOrganisationUnitLevels();

        assertEquals( predictorA.getName(), "PredictorA" );
        assertEquals( predictorA.getDescription(), "DescriptionA" );
        assertNotNull( predictorA.getGenerator().getExpression() );
        // TODO Need a good skipTest test
        assertEquals( predictorA.getPeriodType(), periodType );
        assertEquals( predictorA.getOutput(), dataElementX );
        assertEquals( predictorA.getAnnualSampleCount(), new Integer( 0 ) );
        assertEquals( predictorA.getSequentialSampleCount(), new Integer( 6 ) );
        assertEquals( predictorA.getSequentialSkipCount(), new Integer( 1 ) );
        assertEquals( levelsB.size(), 1 );
        assertEquals( levelsB, expectedLevelsB );
    }

    @Test
    public void testUpdatePredictor()
    {
        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expressionA, expressionB, periodType, orgUnitLevel1,
            6, 1, 0 );

        predictorStore.save( predictor );
        int id = predictor.getId();

        predictor = predictorStore.get( id );

        assertEquals( predictor.getName(), "PredictorA" );
        assertEquals( predictor.getDescription(), "DescriptionA" );
        assertNotNull( predictor.getGenerator().getExpression() );
        assertEquals( predictor.getPeriodType(), periodType );

        predictor.setName( "PredictorB" );
        predictor.setDescription( "DescriptionB" );
        predictor.setSequentialSkipCount( 2 );

        predictorStore.update( predictor );

        predictor = predictorStore.get( id );

        assertEquals( predictor.getName(), "PredictorB" );
        assertEquals( predictor.getDescription(), "DescriptionB" );
        assertEquals( predictor.getSequentialSkipCount(), new Integer( 2 ) );
    }

    @Test
    public void testDeletePredictor()
    {
        Predictor predictorA = createPredictor( dataElementX, defaultCombo, "A", expressionA, expressionB, periodType, orgUnitLevel1, 6, 1, 0 );
        Predictor predictorB = createPredictor( dataElementX, defaultCombo, "B", expressionA, expressionB, periodType, orgUnitLevel1, 6, 1, 0 );

        predictorStore.save( predictorA );
        int idA = predictorA.getId();
        predictorStore.save( predictorB );
        int idB = predictorB.getId();

        assertNotNull( predictorStore.get( idA ) );
        assertNotNull( predictorStore.get( idB ) );

        predictorA.clearExpressions();

        predictorStore.delete( predictorA );

        assertNull( predictorStore.get( idA ) );
        assertNotNull( predictorStore.get( idB ) );

        predictorB.clearExpressions();

        predictorStore.delete( predictorB );

        assertNull( predictorStore.get( idA ) );
        assertNull( predictorStore.get( idB ) );
    }

    @Test
    public void testGetAllPredictors()
    {
        Predictor predictorA = createPredictor( dataElementX, defaultCombo, "A", expressionA, expressionB, periodType, orgUnitLevel1, 6, 1, 0 );
        Predictor predictorB = createPredictor( dataElementX, defaultCombo, "B", expressionA, expressionB, periodType, orgUnitLevel1, 6, 1, 0 );

        predictorStore.save( predictorA );
        predictorStore.save( predictorB );

        List<Predictor> rules = predictorStore.getAll();

        assertTrue( rules.size() == 2 );
        assertTrue( rules.contains( predictorA ) );
        assertTrue( rules.contains( predictorB ) );
    }

    @Test
    public void testGetPredictorByName()
    {
        Predictor predictorA = createPredictor( dataElementX, defaultCombo, "A", expressionA, expressionB, periodType, orgUnitLevel1, 6, 1, 0 );
        Predictor predictorB = createPredictor( dataElementX, defaultCombo, "B", expressionA, expressionB, periodType, orgUnitLevel1, 6, 1, 0 );

        predictorStore.save( predictorA );
        int id = predictorA.getId();
        predictorStore.save( predictorB );

        Predictor rule = predictorStore.getByName( "PredictorA" );

        assertEquals( rule.getId(), id );
        assertEquals( rule.getName(), "PredictorA" );
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

        Predictor predictorA = createPredictor( dataElementX, defaultCombo, "A", expressionA, expressionB, periodType, orgUnitLevel1, 6, 1, 0 );
        Predictor predictorB = createPredictor( dataElementX, defaultCombo, "B", expressionA, expressionB, periodType, orgUnitLevel1, 6, 1, 0 );
        Predictor predictorC = createPredictor( dataElementX, defaultCombo, "C", expressionA, expressionB, periodType, orgUnitLevel1, 6, 1, 0 );

        predictorStore.save( predictorA );
        predictorStore.save( predictorB );
        predictorStore.save( predictorC );

        assertNotNull( predictorStore.getCount() );
        assertEquals( 3, predictorStore.getCount() );
    }
}
