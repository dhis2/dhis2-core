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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class PredictorServiceTest extends IntegrationTestBase
{

    @Autowired
    private PredictorService predictorService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private CategoryService categoryService;

    private OrganisationUnitLevel orgUnitLevel1;

    private DataElement dataElementA;

    private DataElement dataElementX;

    private CategoryOptionCombo defaultCombo;

    private CategoryOptionCombo altCombo;

    private CategoryOption altCategoryOption;

    private Category altCategory;

    private CategoryCombo altCategoryCombo;

    private Set<DataElement> dataElements;

    private Set<CategoryOptionCombo> optionCombos;

    private Expression expressionA;

    private Expression expressionB;

    private Expression expressionC;

    private Expression expressionD;

    private PeriodType periodTypeMonthly;

    private Predictor predictorA;

    private Predictor predictorB;

    private PredictorGroup predictorGroupA;

    private PredictorGroup predictorGroupB;

    private long predictorGroupIdA;

    private long predictorGroupIdB;

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
        dataElementX = createDataElement( 'X', ValueType.NUMBER, AggregationType.NONE );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        DataElement dataElementD = createDataElement( 'D' );
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
            "AVG(#{" + dataElementA.getUid() + "})+1.5*STDDEV(#{" + dataElementA.getUid() + "})", "descriptionA" );
        expressionB = new Expression( "AVG(#{" + dataElementB.getUid() + "." + defaultCombo.getUid() + "})",
            "descriptionB" );
        expressionC = new Expression( "135.79", "descriptionC" );
        expressionD = new Expression( "34.98", "descriptionD" );
    }

    private void setUpPredictorGroups()
    {
        predictorA = createPredictor( dataElementX, defaultCombo, "A", expressionA, expressionB, periodTypeMonthly,
            orgUnitLevel1, 6, 1, 0 );
        predictorB = createPredictor( dataElementX, altCombo, "B", expressionB, expressionD, periodTypeMonthly,
            orgUnitLevel1, 6, 1, 0 );
        predictorService.addPredictor( predictorA );
        predictorService.addPredictor( predictorB );
        predictorGroupA = createPredictorGroup( 'A' );
        predictorGroupB = createPredictorGroup( 'B' );
        predictorGroupIdA = predictorService.addPredictorGroup( predictorGroupA );
        predictorGroupIdB = predictorService.addPredictorGroup( predictorGroupB );
        predictorGroupA.addPredictor( predictorA );
        predictorGroupA.addPredictor( predictorB );
        predictorGroupB.addPredictor( predictorA );
        predictorGroupB.addPredictor( predictorB );
        predictorService.updatePredictorGroup( predictorGroupA );
        predictorService.updatePredictorGroup( predictorGroupB );
        predictorService.updatePredictor( predictorA );
        predictorService.updatePredictor( predictorB );
    }

    // -------------------------------------------------------------------------
    // Predictor CRUD tests
    // -------------------------------------------------------------------------
    @Test
    void testSaveGetPredictor()
    {
        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expressionA, expressionB,
            periodTypeMonthly, orgUnitLevel1, 6, 1, 0 );
        Set<OrganisationUnitLevel> levels = new HashSet<>();
        levels.add( orgUnitLevel1 );
        long id = predictorService.addPredictor( predictor );
        predictor = predictorService.getPredictor( id );
        assertEquals( predictor.getName(), "PredictorA" );
        assertEquals( predictor.getDescription(), "DescriptionA" );
        assertEquals( predictor.getGenerator(), expressionA );
        assertEquals( predictor.getSampleSkipTest(), expressionB );
        assertEquals( predictor.getPeriodType(), periodTypeMonthly );
        assertEquals( predictor.getOutput(), dataElementX );
        assertEquals( predictor.getAnnualSampleCount(), Integer.valueOf( 0 ) );
        assertEquals( predictor.getSequentialSampleCount(), Integer.valueOf( 6 ) );
        assertEquals( predictor.getSequentialSkipCount(), Integer.valueOf( 1 ) );
        assertEquals( predictor.getOrganisationUnitLevels(), levels );
    }

    @Test
    void testSaveGetPredictorAlt()
    {
        Predictor predictor = createPredictor( dataElementA, altCombo, "B", expressionC, null, periodTypeMonthly,
            orgUnitLevel1, 6, 1, 0 );
        Set<OrganisationUnitLevel> levels = new HashSet<>();
        levels.add( orgUnitLevel1 );
        long id = predictorService.addPredictor( predictor );
        predictor = predictorService.getPredictor( id );
        assertEquals( predictor.getName(), "PredictorB" );
        assertEquals( predictor.getDescription(), "DescriptionB" );
        assertEquals( predictor.getGenerator(), expressionC );
        assertNull( predictor.getSampleSkipTest() );
        assertEquals( predictor.getPeriodType(), periodTypeMonthly );
        assertEquals( predictor.getOutput(), dataElementA );
        assertEquals( predictor.getAnnualSampleCount(), Integer.valueOf( 0 ) );
        assertEquals( predictor.getSequentialSampleCount(), Integer.valueOf( 6 ) );
        assertEquals( predictor.getSequentialSkipCount(), Integer.valueOf( 1 ) );
        assertEquals( predictor.getOrganisationUnitLevels(), levels );
    }

    @Test
    void testUpdatePredictor()
    {
        Predictor predictor = createPredictor( dataElementX, altCombo, "A", expressionA, expressionB, periodTypeMonthly,
            orgUnitLevel1, 6, 1, 0 );
        long id = predictorService.addPredictor( predictor );
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
        assertEquals( predictor.getSequentialSkipCount(), Integer.valueOf( 2 ) );
    }

    @Test
    void testDeletePredictor()
    {
        predictorA = createPredictor( dataElementX, defaultCombo, "A", expressionA, expressionB, periodTypeMonthly,
            orgUnitLevel1, 6, 1, 0 );
        predictorB = createPredictor( dataElementX, altCombo, "B", expressionC, expressionD, periodTypeMonthly,
            orgUnitLevel1, 6, 1, 0 );
        long idA = predictorService.addPredictor( predictorA );
        long idB = predictorService.addPredictor( predictorB );
        assertNotNull( predictorService.getPredictor( idA ) );
        assertNotNull( predictorService.getPredictor( idB ) );
        predictorService.deletePredictor( predictorA );
        assertNull( predictorService.getPredictor( idA ) );
        assertNotNull( predictorService.getPredictor( idB ) );
        predictorService.deletePredictor( predictorB );
        assertNull( predictorService.getPredictor( idA ) );
        assertNull( predictorService.getPredictor( idB ) );
    }

    @Test
    void testGetAllPredictors()
    {
        predictorA = createPredictor( dataElementX, defaultCombo, "A", expressionA, expressionB, periodTypeMonthly,
            orgUnitLevel1, 6, 1, 0 );
        predictorB = createPredictor( dataElementX, altCombo, "B", expressionC, expressionD, periodTypeMonthly,
            orgUnitLevel1, 6, 1, 0 );
        predictorService.addPredictor( predictorA );
        predictorService.addPredictor( predictorB );
        List<Predictor> predictors = predictorService.getAllPredictors();
        assertEquals( 2, predictors.size() );
        assertTrue( predictors.contains( predictorA ) );
        assertTrue( predictors.contains( predictorB ) );
    }

    // -------------------------------------------------------------------------
    // Predictor Group
    // -------------------------------------------------------------------------
    @Test
    void testAddPredictorGroup()
    {
        setUpPredictorGroups();
        assertEquals( predictorGroupA, predictorService.getPredictorGroup( predictorGroupIdA ) );
        assertEquals( predictorGroupB, predictorService.getPredictorGroup( predictorGroupIdB ) );
        Set<Predictor> predictors = predictorGroupA.getMembers();
        assertEquals( 2, predictors.size() );
        assertTrue( predictors.contains( predictorA ) );
        assertTrue( predictors.contains( predictorB ) );
    }

    @Test
    void testUpdatePredictorGroup()
    {
        setUpPredictorGroups();
        predictorGroupA.setName( "UpdatedPredictorGroupA" );
        predictorGroupB.setName( "UpdatedPredictorGroupB" );
        predictorService.updatePredictorGroup( predictorGroupA );
        predictorService.updatePredictorGroup( predictorGroupB );
        assertEquals( predictorGroupA, predictorService.getPredictorGroup( predictorGroupIdA ) );
        assertEquals( predictorGroupB, predictorService.getPredictorGroup( predictorGroupIdB ) );
    }

    @Test
    void testDeletePredictorGroup()
    {
        setUpPredictorGroups();
        assertNotNull( predictorService.getPredictorGroup( predictorGroupIdA ) );
        assertNotNull( predictorService.getPredictorGroup( predictorGroupIdB ) );
        assertEquals( 2, predictorA.getGroups().size() );
        predictorService.deletePredictorGroup( predictorGroupA );
        assertNull( predictorService.getPredictorGroup( predictorGroupIdA ) );
        assertNotNull( predictorService.getPredictorGroup( predictorGroupIdB ) );
        assertEquals( 1, predictorA.getGroups().size() );
        predictorService.deletePredictorGroup( predictorGroupB );
        assertNull( predictorService.getPredictorGroup( predictorGroupIdA ) );
        assertNull( predictorService.getPredictorGroup( predictorGroupIdB ) );
        assertEquals( 0, predictorA.getGroups().size() );
    }

    @Test
    void testDeletePredictorGroupMember()
    {
        setUpPredictorGroups();
        Set<Predictor> predictors = predictorGroupA.getMembers();
        assertEquals( 2, predictors.size() );
        assertTrue( predictors.contains( predictorA ) );
        assertTrue( predictors.contains( predictorB ) );
        predictorGroupA.getMembers().remove( predictorA );
        predictorService.updatePredictorGroup( predictorGroupA );
        predictors = predictorGroupA.getMembers();
        assertEquals( 1, predictors.size() );
        assertTrue( predictors.contains( predictorB ) );
        predictorGroupA.getMembers().remove( predictorB );
        predictorService.updatePredictorGroup( predictorGroupA );
        predictors = predictorGroupA.getMembers();
        assertEquals( 0, predictors.size() );
    }

    @Test
    void testGetAllPredictorGroup()
    {
        setUpPredictorGroups();
        Collection<PredictorGroup> groups = predictorService.getAllPredictorGroups();
        assertEquals( 2, groups.size() );
        assertTrue( groups.contains( predictorGroupA ) );
        assertTrue( groups.contains( predictorGroupB ) );
    }

    @Test
    void testCannotDeleteCategoryOptionComboUsedByPredictor()
    {
        setUpPredictorGroups();
        DeleteNotAllowedException ex = assertThrows( DeleteNotAllowedException.class,
            () -> categoryService.deleteCategoryOptionCombo( altCombo ) );
        assertEquals( "Object could not be deleted because it is associated with another object: Predictor",
            ex.getMessage() );
    }
}
