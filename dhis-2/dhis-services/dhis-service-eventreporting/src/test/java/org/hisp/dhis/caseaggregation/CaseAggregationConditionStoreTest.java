package org.hisp.dhis.caseaggregation;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 * 
 * @version $ CaseAggregationConditionStoreTest.java Nov 29, 2013 10:01:48 AM $
 */
public class CaseAggregationConditionStoreTest
    extends DhisSpringTest
{
    @Autowired
    private CaseAggregationConditionStore aggConditionServiceStore;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private TrackedEntityDataValueService dataValueService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private PeriodService periodService;

    private TrackedEntityAttribute attribute;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataElement dataElementC;

    private DataElement dataElementD;

    private DataElementCategoryOptionCombo categoryOptionCombo;

    private Program program;

    private CaseAggregationCondition conditionA;

    private CaseAggregationCondition conditionB;

    private OrganisationUnit organisationUnit;

    private Period period;

    private int stageBId;

    @Override
    public void setUpTest()
    {
        organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        // ---------------------------------------------------------------------
        // Data elements && Category
        // ---------------------------------------------------------------------

        DataElementCategoryOption categoryOptionA = new DataElementCategoryOption( "Male" );
        DataElementCategoryOption categoryOptionB = new DataElementCategoryOption( "Female" );
        DataElementCategoryOption categoryOptionC = new DataElementCategoryOption( "0-20" );
        DataElementCategoryOption categoryOptionD = new DataElementCategoryOption( "20-100" );

        categoryService.addDataElementCategoryOption( categoryOptionA );
        categoryService.addDataElementCategoryOption( categoryOptionB );
        categoryService.addDataElementCategoryOption( categoryOptionC );
        categoryService.addDataElementCategoryOption( categoryOptionD );

        DataElementCategory categoryA = new DataElementCategory( "Gender" );
        DataElementCategory categoryB = new DataElementCategory( "Agegroup" );

        categoryA.getCategoryOptions().add( categoryOptionA );
        categoryA.getCategoryOptions().add( categoryOptionB );
        categoryB.getCategoryOptions().add( categoryOptionC );
        categoryB.getCategoryOptions().add( categoryOptionD );

        categoryService.addDataElementCategory( categoryA );
        categoryService.addDataElementCategory( categoryB );

        DataElementCategoryCombo categoryComboA = new DataElementCategoryCombo( "GenderAgegroup" );

        categoryComboA.getCategories().add( categoryA );
        categoryComboA.getCategories().add( categoryB );

        categoryService.addDataElementCategoryCombo( categoryComboA );

        categoryOptionCombo = new DataElementCategoryOptionCombo();
        Set<DataElementCategoryOption> categoryOptions = new HashSet<>();
        categoryOptions.add( categoryOptionA );
        categoryOptions.add( categoryOptionB );
        categoryOptionCombo.setCategoryCombo( categoryComboA );
        categoryOptionCombo.setCategoryOptions( categoryOptions );
        categoryService.addDataElementCategoryOptionCombo( categoryOptionCombo );

        dataElementA = createDataElement( 'A' );
        dataElementA.setValueType( ValueType.TEXT );
        dataElementA.setDomainType( DataElementDomain.TRACKER );

        dataElementB = createDataElement( 'B' );
        dataElementB.setValueType( ValueType.TEXT );
        dataElementB.setDomainType( DataElementDomain.TRACKER );

        dataElementC = createDataElement( 'C' );
        dataElementC.setDomainType( DataElementDomain.AGGREGATE );

        dataElementD = createDataElement( 'D' );
        dataElementD.setDomainType( DataElementDomain.AGGREGATE );

        int deAId = dataElementService.addDataElement( dataElementA );
        int deBId = dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );

        // ---------------------------------------------------------------------
        // TrackedEntityInstance
        // ---------------------------------------------------------------------

        TrackedEntityInstance entityInstance = createTrackedEntityInstance( 'A', organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstance );

        // ---------------------------------------------------------------------
        // Attribue value
        // ---------------------------------------------------------------------

        attribute = createTrackedEntityAttribute( 'A' );
        int attributeId = attributeService.addTrackedEntityAttribute( attribute );

        TrackedEntityAttributeValue attributeValue = createTrackedEntityAttributeValue( 'A', entityInstance, attribute );
        Set<TrackedEntityAttributeValue> attributeValues = new HashSet<>();
        attributeValues.add( attributeValue );

        // ---------------------------------------------------------------------
        // Program && Program stages
        // ---------------------------------------------------------------------

        program = createProgram( 'A', new HashSet<>(), organisationUnit );
        int programId = programService.addProgram( program );

        ProgramStage stageA = new ProgramStage( "Stage-A", program );
        int stageAId = programStageService.saveProgramStage( stageA );

        ProgramStage stageB = new ProgramStage( "Stage-B", program );
        stageBId = programStageService.saveProgramStage( stageB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        program.setProgramStages( programStages );
        programService.updateProgram( program );

        // ---------------------------------------------------------------------
        // Program Instance && data values
        // ---------------------------------------------------------------------

        DateTime today = DateTime.now();
        today.withTimeAtStartOfDay();
        ProgramInstance programInstance = programInstanceService.enrollTrackedEntityInstance( entityInstance, program,
            today.toDate(), today.toDate(), organisationUnit );

        ProgramStageInstance stageInstanceA = programStageInstanceService.createProgramStageInstance( programInstance,
            stageA, today.toDate(), today.toDate(), organisationUnit );
        ProgramStageInstance stageInstanceB = programStageInstanceService.createProgramStageInstance( programInstance,
            stageB, today.toDate(), today.toDate(), organisationUnit );

        TrackedEntityDataValue dataValueA = new TrackedEntityDataValue( stageInstanceA, dataElementA, "A" );
        TrackedEntityDataValue dataValueB = new TrackedEntityDataValue( stageInstanceA, dataElementB, "B" );
        TrackedEntityDataValue dataValueC = new TrackedEntityDataValue( stageInstanceB, dataElementA, "C" );
        TrackedEntityDataValue dataValueD = new TrackedEntityDataValue( stageInstanceB, dataElementB, "D" );

        dataValueService.saveTrackedEntityDataValue( dataValueA );
        dataValueService.saveTrackedEntityDataValue( dataValueB );
        dataValueService.saveTrackedEntityDataValue( dataValueC );
        dataValueService.saveTrackedEntityDataValue( dataValueD );

        // ---------------------------------------------------------------------
        // Period
        // ---------------------------------------------------------------------

        PeriodType periodType = periodService.getPeriodTypeByName( DailyPeriodType.NAME );
        period = new Period();
        period.setPeriodType( periodType );
        period.setStartDate( today.toDate() );
        period.setEndDate( today.toDate() );
        periodService.addPeriod( period );

        // ---------------------------------------------------------------------
        // CaseAggregationCondition
        // ---------------------------------------------------------------------

        String expression = "[" + CaseAggregationCondition.OBJECT_PROGRAM_STAGE_DATAELEMENT
            + CaseAggregationCondition.SEPARATOR_OBJECT + programId + "." + stageAId + "." + deAId + "] is not null";
        expression += " AND [" + CaseAggregationCondition.OBJECT_PROGRAM_STAGE_DATAELEMENT
            + CaseAggregationCondition.SEPARATOR_OBJECT + programId + "." + stageAId + "." + deBId + "] is not null";
        expression += " AND [" + CaseAggregationCondition.OBJECT_TRACKED_ENTITY_ATTRIBUTE
            + CaseAggregationCondition.SEPARATOR_OBJECT + attributeId + "] is not null";
        conditionA = new CaseAggregationCondition( "A", CaseAggregationCondition.AGGRERATION_COUNT, expression,
            dataElementC, categoryOptionCombo );

        expression = "[" + CaseAggregationCondition.OBJECT_PROGRAM_STAGE_DATAELEMENT
            + CaseAggregationCondition.SEPARATOR_OBJECT + programId + "." + stageBId + "." + deAId + "] is not null";
        conditionB = new CaseAggregationCondition( "B", CaseAggregationCondition.AGGRERATION_COUNT, expression,
            dataElementD, categoryOptionCombo );
    }

    @Test
    public void testGetCaseAggregationConditionByDe()
    {
        aggConditionServiceStore.save( conditionA );
        aggConditionServiceStore.save( conditionB );

        assertTrue( equals( aggConditionServiceStore.get( dataElementC ), conditionA ) );
        assertTrue( equals( aggConditionServiceStore.get( dataElementD ), conditionB ) );
    }

    @Test
    public void testGetCaseAggregationConditionByDeOptionCombo()
    {
        aggConditionServiceStore.save( conditionA );
        aggConditionServiceStore.save( conditionB );

        CaseAggregationCondition condition = aggConditionServiceStore.get( dataElementC, categoryOptionCombo );
        assertEquals( conditionA, condition );

        condition = aggConditionServiceStore.get( dataElementD, categoryOptionCombo );
        assertEquals( conditionB, condition );
    }

    @Test
    public void testGetCaseAggregationCondition()
    {
        aggConditionServiceStore.save( conditionA );
        aggConditionServiceStore.save( conditionB );

        Collection<DataElement> dataElements = new HashSet<>();
        dataElements.add( dataElementC );
        dataElements.add( dataElementD );

        assertTrue( equals( aggConditionServiceStore.get( dataElements, null, null, null ), conditionA, conditionB ) );
    }

}
