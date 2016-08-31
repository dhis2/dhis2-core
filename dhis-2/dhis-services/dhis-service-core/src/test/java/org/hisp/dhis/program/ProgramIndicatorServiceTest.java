package org.hisp.dhis.program;

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

import static org.hisp.dhis.program.ProgramIndicator.KEY_ATTRIBUTE;
import static org.hisp.dhis.program.ProgramIndicator.KEY_DATAELEMENT;
import static org.hisp.dhis.program.ProgramIndicator.KEY_PROGRAM_VARIABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
public class ProgramIndicatorServiceTest
    extends DhisSpringTest
{
    private static final String COL_QUOTE = "\"";

    @Autowired
    private ProgramIndicatorService programIndicatorService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private TrackedEntityDataValueService dataValueService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private TrackedEntityAttributeValueService attributeValueService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private ConstantService constantService;

    private Date incidentDate;

    private Date enrollmentDate;

    private ProgramStage psA;

    private ProgramStage psB;

    private Program programA;

    private Program programB;

    private ProgramInstance programInstance;

    private DataElement deA;

    private DataElement deB;

    private TrackedEntityAttribute atA;

    private TrackedEntityAttribute atB;

    private ProgramIndicator indicatorA;

    private ProgramIndicator indicatorB;

    private ProgramIndicator indicatorC;

    private ProgramIndicator indicatorD;

    private ProgramIndicator indicatorE;

    @Override
    public void setUpTest()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        // ---------------------------------------------------------------------
        // Program
        // ---------------------------------------------------------------------

        programA = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( programA );

        psA = new ProgramStage( "StageA", programA );
        psA.setSortOrder( 1 );
        programStageService.saveProgramStage( psA );

        psB = new ProgramStage( "StageB", programA );
        psB.setSortOrder( 2 );
        programStageService.saveProgramStage( psB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( psA );
        programStages.add( psB );
        programA.setProgramStages( programStages );
        programService.updateProgram( programA );

        programB = createProgram( 'B', new HashSet<>(), organisationUnit );
        programService.addProgram( programB );

        // ---------------------------------------------------------------------
        // Program Stage DE
        // ---------------------------------------------------------------------

        deA = createDataElement( 'A' );
        deA.setDomainType( DataElementDomain.TRACKER );

        deB = createDataElement( 'B' );
        deB.setDomainType( DataElementDomain.TRACKER );

        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );

        ProgramStageDataElement stageDataElementA = new ProgramStageDataElement( psA, deA, false, 1 );
        ProgramStageDataElement stageDataElementB = new ProgramStageDataElement( psA, deB, false, 2 );
        ProgramStageDataElement stageDataElementC = new ProgramStageDataElement( psB, deA, false, 1 );
        ProgramStageDataElement stageDataElementD = new ProgramStageDataElement( psB, deB, false, 2 );

        programStageDataElementService.addProgramStageDataElement( stageDataElementA );
        programStageDataElementService.addProgramStageDataElement( stageDataElementB );
        programStageDataElementService.addProgramStageDataElement( stageDataElementC );
        programStageDataElementService.addProgramStageDataElement( stageDataElementD );

        // ---------------------------------------------------------------------
        // TrackedEntityInstance & Enrollment
        // ---------------------------------------------------------------------

        TrackedEntityInstance entityInstance = createTrackedEntityInstance( 'A', organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstance );

        incidentDate = DateUtils.getMediumDate( "2014-10-22" );
        enrollmentDate = DateUtils.getMediumDate( "2014-12-31" );

        programInstance = programInstanceService.enrollTrackedEntityInstance( entityInstance, programA, enrollmentDate,
            incidentDate, organisationUnit );

        incidentDate = DateUtils.getMediumDate( "2014-10-22" );
        enrollmentDate = DateUtils.getMediumDate( "2014-12-31" );

        programInstance = programInstanceService.enrollTrackedEntityInstance( entityInstance, programA, enrollmentDate,
            incidentDate, organisationUnit );

        // TODO enroll twice?

        // ---------------------------------------------------------------------
        // TrackedEntityAttribute
        // ---------------------------------------------------------------------

        atA = createTrackedEntityAttribute( 'A', ValueType.NUMBER );
        atB = createTrackedEntityAttribute( 'B', ValueType.NUMBER );

        attributeService.addTrackedEntityAttribute( atA );
        attributeService.addTrackedEntityAttribute( atB );

        TrackedEntityAttributeValue attributeValueA = new TrackedEntityAttributeValue( atA, entityInstance, "1" );
        TrackedEntityAttributeValue attributeValueB = new TrackedEntityAttributeValue( atB, entityInstance, "2" );

        attributeValueService.addTrackedEntityAttributeValue( attributeValueA );
        attributeValueService.addTrackedEntityAttributeValue( attributeValueB );

        // ---------------------------------------------------------------------
        // TrackedEntityDataValue
        // ---------------------------------------------------------------------

        ProgramStageInstance stageInstanceA = programStageInstanceService.createProgramStageInstance( programInstance,
            psA, enrollmentDate, incidentDate, organisationUnit );
        ProgramStageInstance stageInstanceB = programStageInstanceService.createProgramStageInstance( programInstance,
            psB, enrollmentDate, incidentDate, organisationUnit );

        Set<ProgramStageInstance> programStageInstances = new HashSet<>();
        programStageInstances.add( stageInstanceA );
        programStageInstances.add( stageInstanceB );
        programInstance.setProgramStageInstances( programStageInstances );
        programInstance.setProgram( programA );

        TrackedEntityDataValue dataValueA = new TrackedEntityDataValue( stageInstanceA, deA, "3" );
        TrackedEntityDataValue dataValueB = new TrackedEntityDataValue( stageInstanceA, deB, "1" );
        TrackedEntityDataValue dataValueC = new TrackedEntityDataValue( stageInstanceB, deA, "5" );
        TrackedEntityDataValue dataValueD = new TrackedEntityDataValue( stageInstanceB, deB, "7" );

        dataValueService.saveTrackedEntityDataValue( dataValueA );
        dataValueService.saveTrackedEntityDataValue( dataValueB );
        dataValueService.saveTrackedEntityDataValue( dataValueC );
        dataValueService.saveTrackedEntityDataValue( dataValueD );

        // ---------------------------------------------------------------------
        // Constant
        // ---------------------------------------------------------------------

        Constant constantA = createConstant( 'A', 7.0 );
        constantService.saveConstant( constantA );

        // ---------------------------------------------------------------------
        // ProgramIndicator
        // ---------------------------------------------------------------------

        String expressionA = "( " + KEY_PROGRAM_VARIABLE + "{" + ProgramIndicator.VAR_ENROLLMENT_DATE + "} - " + KEY_PROGRAM_VARIABLE + "{"
            + ProgramIndicator.VAR_INCIDENT_DATE + "} )  / " + ProgramIndicator.KEY_CONSTANT + "{" + constantA.getUid() + "}";
        indicatorA = createProgramIndicator( 'A', programA, expressionA, null );
        programA.getProgramIndicators().add( indicatorA );

        indicatorB = createProgramIndicator( 'B', programA, "70", null );
        programA.getProgramIndicators().add( indicatorB );

        indicatorC = createProgramIndicator( 'C', programA, "0", null );
        programA.getProgramIndicators().add( indicatorC );

        String expressionD = "0 + A + 4 + " + ProgramIndicator.KEY_PROGRAM_VARIABLE + "{" + ProgramIndicator.VAR_INCIDENT_DATE + "}";
        indicatorD = createProgramIndicator( 'D', programB, expressionD, null );

        String expressionE = KEY_DATAELEMENT + "{" + psA.getUid() + "." + deA.getUid() + "} + " + KEY_DATAELEMENT + "{"
            + psB.getUid() + "." + deA.getUid() + "} - " + KEY_ATTRIBUTE + "{" + atA.getUid() + "} + " + KEY_ATTRIBUTE
            + "{" + atB.getUid() + "}";
        String filterE = KEY_DATAELEMENT + "{" + psA.getUid() + "." + deA.getUid() + "} + " + KEY_ATTRIBUTE + "{" + atA.getUid() + "} > 10";
        indicatorE = createProgramIndicator( 'E', programB, expressionE, filterE );
    }

    // -------------------------------------------------------------------------
    // CRUD tests
    // -------------------------------------------------------------------------

    @Test
    public void testAddProgramIndicator()
    {
        int idA = programIndicatorService.addProgramIndicator( indicatorA );
        int idB = programIndicatorService.addProgramIndicator( indicatorB );
        int idC = programIndicatorService.addProgramIndicator( indicatorC );

        assertNotNull( programIndicatorService.getProgramIndicator( idA ) );
        assertNotNull( programIndicatorService.getProgramIndicator( idB ) );
        assertNotNull( programIndicatorService.getProgramIndicator( idC ) );
    }

    @Test
    public void testDeleteProgramIndicator()
    {
        int idA = programIndicatorService.addProgramIndicator( indicatorB );
        int idB = programIndicatorService.addProgramIndicator( indicatorA );

        assertNotNull( programIndicatorService.getProgramIndicator( idA ) );
        assertNotNull( programIndicatorService.getProgramIndicator( idB ) );

        programIndicatorService.deleteProgramIndicator( indicatorB );

        assertNull( programIndicatorService.getProgramIndicator( idA ) );
        assertNotNull( programIndicatorService.getProgramIndicator( idB ) );

        programIndicatorService.deleteProgramIndicator( indicatorA );

        assertNull( programIndicatorService.getProgramIndicator( idA ) );
        assertNull( programIndicatorService.getProgramIndicator( idB ) );
    }

    @Test
    public void testUpdateProgramIndicator()
    {
        int idA = programIndicatorService.addProgramIndicator( indicatorB );

        assertNotNull( programIndicatorService.getProgramIndicator( idA ) );

        indicatorB.setName( "B" );
        programIndicatorService.updateProgramIndicator( indicatorB );

        assertEquals( "B", programIndicatorService.getProgramIndicator( idA ).getName() );
    }

    @Test
    public void testGetProgramIndicatorById()
    {
        int idA = programIndicatorService.addProgramIndicator( indicatorB );
        int idB = programIndicatorService.addProgramIndicator( indicatorA );

        assertEquals( indicatorB, programIndicatorService.getProgramIndicator( idA ) );
        assertEquals( indicatorA, programIndicatorService.getProgramIndicator( idB ) );
    }

    @Test
    public void testGetProgramIndicatorByName()
    {
        programIndicatorService.addProgramIndicator( indicatorB );
        programIndicatorService.addProgramIndicator( indicatorA );

        assertEquals( "IndicatorA", programIndicatorService.getProgramIndicator( "IndicatorA" ).getName() );
        assertEquals( "IndicatorB", programIndicatorService.getProgramIndicator( "IndicatorB" ).getName() );
    }

    @Test
    public void testGetAllProgramIndicators()
    {
        programIndicatorService.addProgramIndicator( indicatorB );
        programIndicatorService.addProgramIndicator( indicatorA );

        assertTrue( equals( programIndicatorService.getAllProgramIndicators(), indicatorB, indicatorA ) );
    }

    // -------------------------------------------------------------------------
    // Logic tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetExpressionDescription()
    {
        programIndicatorService.addProgramIndicator( indicatorB );
        programIndicatorService.addProgramIndicator( indicatorA );

        String description = programIndicatorService.getExpressionDescription( indicatorB.getExpression() );
        assertEquals( "70", description );

        description = programIndicatorService.getExpressionDescription( indicatorA.getExpression() );
        assertEquals( "( Enrollment date - Incident date )  / ConstantA", description );
    }

    @Test
    public void testGetAnyValueExistsFilterAnalyticsSQl()
    {
        String expected = "\"GCyeKSqlpdk\" is not null or \"gAyeKSqlpdk\" is not null";
        String expression = "#{OXXcwl6aPCQ.GCyeKSqlpdk} - A{gAyeKSqlpdk}";

        assertEquals( expected, programIndicatorService.getAnyValueExistsClauseAnalyticsSql( expression ) );
    }
    
    @Test
    public void testGetAnalyticsSQl()
    {
        String expected = "coalesce(\"" + deA.getUid() + "\",0) + coalesce(\"" + atA.getUid() + "\",0) > 10";

        assertEquals( expected, programIndicatorService.getAnalyticsSQl( indicatorE.getFilter() ) );
    }

    @Test
    public void testGetAnalyticsSQlRespectMissingValues()
    {
        String expected = "\"" + deA.getUid() + "\" + \"" + atA.getUid() + "\" > 10";

        assertEquals( expected, programIndicatorService.getAnalyticsSQl( indicatorE.getFilter(), false ) );
    }
    
    @Test
    public void testGetAnalyticsWithVariables()
    {
        String expected = 
            "coalesce(case when \"EZq9VbPWgML\" < 0 then 0 else \"EZq9VbPWgML\" end, 0) + " +
            "coalesce(\"GCyeKSqlpdk\",0) + " +
            "nullif(cast((case when \"EZq9VbPWgML\" >= 0 then 1 else 0 end + case when \"GCyeKSqlpdk\" >= 0 then 1 else 0 end) as double),0)";
        
        String expression = 
            "d2:zing(#{OXXcwl6aPCQ.EZq9VbPWgML}) + " +
            "#{OXXcwl6aPCQ.GCyeKSqlpdk} + " +
            "V{zero_pos_value_count}";
        
        assertEquals( expected, programIndicatorService.getAnalyticsSQl( expression ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsZingA()
    {
        String col = COL_QUOTE + deA.getUid() + COL_QUOTE;
        String expected = "coalesce(case when " + col + " < 0 then 0 else " + col + " end, 0)";
        String expression = "d2:zing(" + col + ")";

        assertEquals( expected, programIndicatorService.getAnalyticsSQl( expression ) );
    }
    
    @Test
    public void testGetAnalyticsSqlWithFunctionsZingB()
    {
        String expected = 
            "coalesce(case when \"EZq9VbPWgML\" < 0 then 0 else \"EZq9VbPWgML\" end, 0) + " +
            "coalesce(case when \"GCyeKSqlpdk\" < 0 then 0 else \"GCyeKSqlpdk\" end, 0) + " +
            "coalesce(case when \"hsCmEqBcU23\" < 0 then 0 else \"hsCmEqBcU23\" end, 0)";        
            
        String expression = 
            "d2:zing(#{OXXcwl6aPCQ.EZq9VbPWgML}) + " +
            "d2:zing(#{OXXcwl6aPCQ.GCyeKSqlpdk}) + " +
            "d2:zing(#{OXXcwl6aPCQ.hsCmEqBcU23})";
        
        assertEquals( expected, programIndicatorService.getAnalyticsSQl( expression ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsOizp()
    {
        String col = COL_QUOTE + deA.getUid() + COL_QUOTE;
        String expected = "coalesce(case when " + col + " >= 0 then 1 else 0 end, 0)";
        String expression = "d2:oizp(" + col + ")";

        assertEquals( expected, programIndicatorService.getAnalyticsSQl( expression ) );
    }
    
    @Test
    public void testGetAnalyticsSqlWithFunctionsZpvc()
    {
        String expected = 
            "nullif(cast((" +
            "case when \"EZq9VbPWgML\" >= 0 then 1 else 0 end + " +
            "case when \"GCyeKSqlpdk\" >= 0 then 1 else 0 end" +
            ") as double precision),0)";
        
        String expression = "d2:zpvc(#{OXXcwl6aPCQ.EZq9VbPWgML},#{OXXcwl6aPCQ.GCyeKSqlpdk})";
        
        assertEquals( expected, programIndicatorService.getAnalyticsSQl( expression ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsDaysBetween()
    {
        String col1 = COL_QUOTE + deA.getUid() + COL_QUOTE;
        String col2 = COL_QUOTE + deB.getUid() + COL_QUOTE;
        String expected = "(cast(" + col2 + " as date) - cast(" + col1 + " as date))";
        String expression = "d2:daysBetween(" + col1 + "," + col2 + ")";

        assertEquals( expected, programIndicatorService.getAnalyticsSQl( expression ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsCondition()
    {
        String col1 = COL_QUOTE + deA.getUid() + COL_QUOTE;
        String expected = "case when (" + col1 + " > 3) then 10 else 5 end";
        String expression = "d2:condition('" + col1 + " > 3',10,5)";

        assertEquals( expected, programIndicatorService.getAnalyticsSQl( expression ) );
    }
    
    @Test
    public void testGetAnalyticsSqlWithFunctionsComposite()
    {
        String expected = 
            "coalesce(case when \"EZq9VbPWgML\" < 0 then 0 else \"EZq9VbPWgML\" end, 0) + " +
            "(cast(\"kts5J79K9gA\" as date) - cast(\"GCyeKSqlpdk\" as date)) + " +
            "case when (\"GCyeKSqlpdk\" > 70) then 100 else 50 end + " +
            "case when (\"HihhUWBeg7I\" < 30) then 20 else 100 end";
        
        String expression = 
            "d2:zing(#{OXXcwl6aPCQ.EZq9VbPWgML}) + " +
            "d2:daysBetween(#{OXXcwl6aPCQ.GCyeKSqlpdk},#{OXXcwl6aPCQ.kts5J79K9gA}) + " +
            "d2:condition(\"#{OXXcwl6aPCQ.GCyeKSqlpdk} > 70\",100,50) + " +
            "d2:condition('#{OXXcwl6aPCQ.HihhUWBeg7I} < 30',20,100)";
        
        assertEquals( expected, programIndicatorService.getAnalyticsSQl( expression ) );
    }
    
    @Test( expected = IllegalStateException.class )
    public void testGetAnalyticsSqlWithFunctionsInvalid()
    {
        String col = COL_QUOTE + deA.getUid() + COL_QUOTE;
        String expected = "case when " + col + " >= 0 then 1 else " + col + " end";
        String expression = "d2:xyza(" + col + ")";

        assertEquals( expected, programIndicatorService.getAnalyticsSQl( expression ) );
    }

    @Test
    public void testGetAnalyticsSqlWithVariables()
    {
        String expected = "coalesce(\"EZq9VbPWgML\",0) + (executiondate - enrollmentdate)";
        String expression = "#{OXXcwl6aPCQ.EZq9VbPWgML} + (V{execution_date} - V{enrollment_date})";

        assertEquals( expected, programIndicatorService.getAnalyticsSQl( expression ) );
    }
    
    @Test
    public void testExpressionIsValid()
    {
        programIndicatorService.addProgramIndicator( indicatorB );
        programIndicatorService.addProgramIndicator( indicatorA );
        programIndicatorService.addProgramIndicator( indicatorD );

        assertEquals( ProgramIndicator.VALID, programIndicatorService.expressionIsValid( indicatorB.getExpression() ) );
        assertEquals( ProgramIndicator.VALID, programIndicatorService.expressionIsValid( indicatorA.getExpression() ) );
        assertEquals( ProgramIndicator.EXPRESSION_NOT_VALID, programIndicatorService.expressionIsValid( indicatorD.getExpression() ) );
    }
    
    @Test
    public void testExpressionWithFunctionIsValid()
    {
        String exprA = "#{" + psA.getUid() + "." + deA.getUid() + "}";
        String exprB = "d2:zing(#{" + psA.getUid() + "." + deA.getUid() + "})";
        String exprC = "d2:condition('#{" + psA.getUid() + "." + deA.getUid() + "} > 10',2,1)";
        
        assertEquals( ProgramIndicator.VALID, programIndicatorService.expressionIsValid( exprA ) );
        assertEquals( ProgramIndicator.VALID, programIndicatorService.expressionIsValid( exprB ) );
        assertEquals( ProgramIndicator.VALID, programIndicatorService.expressionIsValid( exprC ) );
    }

    @Test
    public void testFilterIsValid()
    {
        String filterA = KEY_DATAELEMENT + "{" + psA.getUid() + "." + deA.getUid() + "}  - " + KEY_ATTRIBUTE + "{" + atA.getUid() + "} > 10";
        String filterB = KEY_ATTRIBUTE + "{" + atA.getUid() + "} == " + KEY_DATAELEMENT + "{" + psA.getUid() + "." + deA.getUid() + "} - 5";
        String filterC = KEY_ATTRIBUTE + "{invaliduid} == 100";
        String filterD = KEY_ATTRIBUTE + "{" + atA.getUid() + "} + 200";

        assertEquals( ProgramIndicator.VALID, programIndicatorService.filterIsValid( filterA ) );
        assertEquals( ProgramIndicator.VALID, programIndicatorService.filterIsValid( filterB ) );
        assertEquals( ProgramIndicator.INVALID_IDENTIFIERS_IN_EXPRESSION, programIndicatorService.filterIsValid( filterC ) );
        assertEquals( ProgramIndicator.FILTER_NOT_EVALUATING_TO_TRUE_OR_FALSE, programIndicatorService.filterIsValid( filterD ) );
    }
}
