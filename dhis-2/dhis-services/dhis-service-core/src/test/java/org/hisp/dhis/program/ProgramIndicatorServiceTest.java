package org.hisp.dhis.program;
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

import static org.hisp.dhis.program.ProgramIndicator.KEY_ATTRIBUTE;
import static org.hisp.dhis.program.ProgramIndicator.KEY_DATAELEMENT;
import static org.hisp.dhis.program.ProgramIndicator.KEY_PROGRAM_VARIABLE;
import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;

import java.util.*;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.util.DateUtils;
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

    private DataElement deC;

    private DataElement deD;

    private DataElement deE;

    private TrackedEntityAttribute atA;

    private TrackedEntityAttribute atB;

    private ProgramIndicator indicatorA;

    private ProgramIndicator indicatorB;

    private ProgramIndicator indicatorC;

    private ProgramIndicator indicatorD;

    private ProgramIndicator indicatorE;

    private ProgramIndicator indicatorF;

    @Override
    public void setUpTest()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        // ---------------------------------------------------------------------
        // Program
        // ---------------------------------------------------------------------

        programA = createProgram( 'A', new HashSet<>(), organisationUnit );
        programA.setUid( "Program000A" );
        programService.addProgram( programA );

        psA = new ProgramStage( "StageA", programA );
        psA.setSortOrder( 1 );
        psA.setUid( "ProgrmStagA" );
        programStageService.saveProgramStage( psA );

        psB = new ProgramStage( "StageB", programA );
        psB.setSortOrder( 2 );
        psB.setUid( "ProgrmStagB" );
        programStageService.saveProgramStage( psB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( psA );
        programStages.add( psB );
        programA.setProgramStages( programStages );
        programService.updateProgram( programA );

        programB = createProgram( 'B', new HashSet<>(), organisationUnit );
        programB.setUid( "Program000B" );
        programService.addProgram( programB );

        // ---------------------------------------------------------------------
        // Program Stage DE
        // ---------------------------------------------------------------------

        deA = createDataElement( 'A' );
        deA.setDomainType( DataElementDomain.TRACKER );
        deA.setUid( "DataElmentA" );

        deB = createDataElement( 'B' );
        deB.setDomainType( DataElementDomain.TRACKER );
        deB.setUid( "DataElmentB" );

        deC = createDataElement( 'C' );
        deC.setDomainType( DataElementDomain.TRACKER );
        deC.setUid( "DataElmentC" );

        deD = createDataElement( 'D' );
        deD.setDomainType( DataElementDomain.TRACKER );
        deD.setUid( "DataElmentD" );

        deE = createDataElement( 'E' );
        deE.setValueType( ValueType.TEXT );
        deE.setDomainType( DataElementDomain.TRACKER );
        deE.setUid( "DataElmentE" );

        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        dataElementService.addDataElement( deD );
        dataElementService.addDataElement( deE );

        ProgramStageDataElement stageDataElementA = new ProgramStageDataElement( psA, deA, false, 1 );
        ProgramStageDataElement stageDataElementB = new ProgramStageDataElement( psA, deB, false, 2 );
        ProgramStageDataElement stageDataElementC = new ProgramStageDataElement( psB, deA, false, 1 );
        ProgramStageDataElement stageDataElementD = new ProgramStageDataElement( psB, deB, false, 2 );
        ProgramStageDataElement stageDataElementE = new ProgramStageDataElement( psA, deC, false, 3 );
        ProgramStageDataElement stageDataElementF = new ProgramStageDataElement( psA, deD, false, 4 );
        ProgramStageDataElement stageDataElementG = new ProgramStageDataElement( psA, deE, false, 5 );

        programStageDataElementService.addProgramStageDataElement( stageDataElementA );
        programStageDataElementService.addProgramStageDataElement( stageDataElementB );
        programStageDataElementService.addProgramStageDataElement( stageDataElementC );
        programStageDataElementService.addProgramStageDataElement( stageDataElementD );
        programStageDataElementService.addProgramStageDataElement( stageDataElementE );
        programStageDataElementService.addProgramStageDataElement( stageDataElementF );
        programStageDataElementService.addProgramStageDataElement( stageDataElementG );

        // ---------------------------------------------------------------------
        // TrackedEntityInstance & Enrollment
        // ---------------------------------------------------------------------

        TrackedEntityInstance entityInstance = createTrackedEntityInstance( organisationUnit );
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

        atA.setUid( "Attribute0A" );
        atB.setUid( "Attribute0B" );

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

        // ---------------------------------------------------------------------
        // Constant
        // ---------------------------------------------------------------------

        Constant constantA = createConstant( 'A', 7.0 );
        constantService.saveConstant( constantA );

        // ---------------------------------------------------------------------
        // ProgramIndicator
        // ---------------------------------------------------------------------

        String expressionA = "( d2:daysBetween(" + KEY_PROGRAM_VARIABLE + "{" + ProgramIndicator.VAR_ENROLLMENT_DATE + "}, " + KEY_PROGRAM_VARIABLE + "{"
                + ProgramIndicator.VAR_INCIDENT_DATE + "}) )  / " + ProgramIndicator.KEY_CONSTANT + "{" + constantA.getUid() + "}";
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

        String expressionF = KEY_DATAELEMENT + "{" + psA.getUid() + "." + deA.getUid() + "}";
        String filterF = KEY_DATAELEMENT + "{" + psA.getUid() + "." + deA.getUid() + "} > " +
            KEY_ATTRIBUTE + "{" + atA.getUid() + "}";
        indicatorF = createProgramIndicator( 'F', AnalyticsType.ENROLLMENT, programB, expressionF, filterF );
        indicatorF.getAnalyticsPeriodBoundaries().add( new AnalyticsPeriodBoundary(AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD, PeriodType.getByNameIgnoreCase( "daily" ), 10) );
    }

    // -------------------------------------------------------------------------
    // CRUD tests
    // -------------------------------------------------------------------------

    @Test
    public void testAddProgramIndicator()
    {
        long idA = programIndicatorService.addProgramIndicator( indicatorA );
        long idB = programIndicatorService.addProgramIndicator( indicatorB );
        long idC = programIndicatorService.addProgramIndicator( indicatorC );

        assertNotNull( programIndicatorService.getProgramIndicator( idA ) );
        assertNotNull( programIndicatorService.getProgramIndicator( idB ) );
        assertNotNull( programIndicatorService.getProgramIndicator( idC ) );
    }

    @Test
    public void testDeleteProgramIndicator()
    {
        long idA = programIndicatorService.addProgramIndicator( indicatorB );
        long idB = programIndicatorService.addProgramIndicator( indicatorA );

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
        long idA = programIndicatorService.addProgramIndicator( indicatorB );

        assertNotNull( programIndicatorService.getProgramIndicator( idA ) );

        indicatorB.setName( "B" );
        programIndicatorService.updateProgramIndicator( indicatorB );

        assertEquals( "B", programIndicatorService.getProgramIndicator( idA ).getName() );
    }

    @Test
    public void testGetProgramIndicatorById()
    {
        long idA = programIndicatorService.addProgramIndicator( indicatorB );
        long idB = programIndicatorService.addProgramIndicator( indicatorA );

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
        assertEquals( "( d2:daysBetween(Enrollment date, Incident date) )  / ConstantA", description );
    }

    @Test
    public void testGetAnyValueExistsFilterEventAnalyticsSQl()
    {
        String expected = "\"DataElmentA\" is not null or \"Attribute0A\" is not null";
        String expression = "#{ProgrmStagA.DataElmentA} - A{Attribute0A}";

        assertEquals( expected, programIndicatorService.getAnyValueExistsClauseAnalyticsSql( expression, AnalyticsType.EVENT ) );
    }

    @Test
    public void testGetAnyValueExistsFilterEnrollmentAnalyticsSQl()
    {
        String expected = "\"Attribute0A\" is not null or \"ProgrmStagA_DataElmentA\" is not null";
        String expression = "#{ProgrmStagA.DataElmentA} - A{Attribute0A}";

        assertEquals( expected, programIndicatorService.getAnyValueExistsClauseAnalyticsSql( expression, AnalyticsType.ENROLLMENT ) );
    }

    @Test
    public void testGetAnalyticsSQl()
    {
        String expected = "coalesce(\"" + deA.getUid() + "\"::numeric,0) + coalesce(\"" + atA.getUid() + "\"::numeric,0) > 10";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( indicatorE.getFilter(), indicatorE, new Date(), new Date(), true ) );
    }

    @Test
    public void testGetAnalyticsSQlRespectMissingValues()
    {
        String expected = "\"" + deA.getUid() + "\" + \"" + atA.getUid() + "\" > 10";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( indicatorE.getFilter(), indicatorE, new Date(), new Date(), false ) );
    }

    @Test
    public void testGetAnalyticsWithVariables()
    {
        String expected =
            "coalesce(case when \"DataElmentA\" < 0 then 0 else \"DataElmentA\" end, 0) + " +
            "coalesce(\"DataElmentB\"::numeric,0) + " +
            "nullif(cast((case when \"DataElmentA\" >= 0 then 1 else 0 end + case when \"DataElmentB\" >= 0 then 1 else 0 end) as double),0)";

        String expression =
            "d2:zing(#{ProgrmStagA.DataElmentA}) + " +
            "#{ProgrmStagB.DataElmentB} + " +
            "V{zero_pos_value_count}";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( expression, createProgramIndicator( 'X', programA, expression, null ), new Date(), new Date(), true ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsZingA()
    {
        String expected = "coalesce(case when \"DataElmentA\" < 0 then 0 else \"DataElmentA\" end, 0)";
        String expression = "d2:zing(#{ProgrmStagA.DataElmentA})";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( expression, createProgramIndicator( 'X', programA, expression, null ), new Date(), new Date(), false ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsZingB()
    {
        String expected =
            "coalesce(case when \"DataElmentA\" < 0 then 0 else \"DataElmentA\" end, 0) + " +
            "coalesce(case when \"DataElmentB\" < 0 then 0 else \"DataElmentB\" end, 0) + " +
            "coalesce(case when \"DataElmentC\" < 0 then 0 else \"DataElmentC\" end, 0)";

        String expression =
            "d2:zing(#{ProgrmStagA.DataElmentA}) + " +
            "d2:zing(#{ProgrmStagA.DataElmentB}) + " +
            "d2:zing(#{ProgrmStagA.DataElmentC})";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( expression, createProgramIndicator( 'X', programA, expression, null ), new Date(), new Date(), false ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsOizp()
    {
        String col = COL_QUOTE + deA.getUid() + COL_QUOTE;
        String expressionElement = "#{" + psA.getUid() + "." + deA.getUid() + "}";

        String expected = "coalesce(case when " + col + " >= 0 then 1 else 0 end, 0)";
        String expression = "d2:oizp(" + expressionElement + ")";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( expression, createProgramIndicator( 'X', programA, expression, null ), new Date(), new Date(), false ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsZpvc()
    {
        String expected =
            "nullif(cast((" +
            "case when \"DataElmentA\" >= 0 then 1 else 0 end + " +
            "case when \"DataElmentB\" >= 0 then 1 else 0 end" +
            ") as double precision),0)";

        String expression = "d2:zpvc(#{ProgrmStagA.DataElmentA},#{ProgrmStagA.DataElmentB})";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( expression, createProgramIndicator( 'X', programA, expression, null ), new Date(), new Date(), false ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsDaysBetween()
    {
        String expected = "(cast(\"DataElmentB\" as date) - cast(\"DataElmentA\" as date))";
        String expression = "d2:daysBetween(#{ProgrmStagA.DataElmentA},#{ProgrmStagB.DataElmentB})";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( expression, createProgramIndicator( 'X', programA, expression, null ), new Date(), new Date(), false ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsCondition()
    {
        String expected = "case when (\"DataElmentA\" > 3) then 10 else 5 end";
        String expression = "d2:condition('#{ProgrmStagA.DataElmentA} > 3',10,5)";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( expression, createProgramIndicator( 'X', programA, expression, null ), new Date(), new Date(), false ) );
    }

    @Test
    public void testGetAnalyticsSqlWithFunctionsComposite()
    {
        String expected =
            "coalesce(case when \"DataElmentA\" < 0 then 0 else \"DataElmentA\" end, 0) + " +
            "(cast(\"DataElmentC\" as date) - cast(\"DataElmentB\" as date)) + " +
            "case when (\"DataElmentD\" > 70) then 100 else 50 end + " +
            "case when (\"DataElmentE\" < 30) then 20 else 100 end";

        String expression =
            "d2:zing(#{ProgrmStagA.DataElmentA}) + " +
            "d2:daysBetween(#{ProgrmStagA.DataElmentB},#{ProgrmStagA.DataElmentC}) + " +
            "d2:condition(\"#{ProgrmStagA.DataElmentD} > 70\",100,50) + " +
            "d2:condition('#{ProgrmStagA.DataElmentE} < 30',20,100)";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( expression, createProgramIndicator( 'X', programA, expression, null ), new Date(), new Date(), false ) );
    }

    @Test( expected = IllegalStateException.class )
    public void testGetAnalyticsSqlWithFunctionsInvalid()
    {
        String expected = "case when \"DataElmentA\" >= 0 then 1 else \"DataElmentA\" end";
        String expression = "d2:xyza(#{ProgrmStagA.DataElmentA})";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( expression, createProgramIndicator( 'X', programA, expression, null ), new Date(), new Date(), true ) );
    }

    @Test
    public void testGetAnalyticsSqlWithVariables()
    {
        String expected = "coalesce(\"DataElmentA\"::numeric,0) + (executiondate - enrollmentdate)";
        String expression = "#{ProgrmStagA.DataElmentA} + (V{execution_date} - V{enrollment_date})";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( expression, createProgramIndicator( 'X', programA, expression, null ), new Date(), new Date(), true ) );
    }

    @Test
    public void testIsEmptyFilter()
    {
        String expected = "coalesce(\"DataElmentE\",'') == ''";
        String filter = "#{ProgrmStagA.DataElmentE} == ''";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( filter, createProgramIndicator( 'X', programA, null, filter ), new Date(), new Date(), true ) );
    }

    @Test
    public void testIsZeroFilter()
    {
        String expected = "coalesce(\"DataElmentA\"::numeric,0) == 0";
        String filter = "#{ProgrmStagA.DataElmentA} == 0";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( filter, createProgramIndicator( 'X', AnalyticsType.EVENT, programA, null, filter ), new Date(), new Date(), true ) );
    }

    @Test
    public void testIsZeroOrEmptyFilter()
    {
        String expected = "coalesce(\"DataElmentA\"::numeric,0) == 1 or " +
            "(coalesce(\"DataElmentE\",'') == '' and " +
            "coalesce(\"Attribute0A\"::numeric,0) == 0)";

        String filter = "#{ProgrmStagA.DataElmentA} == 1 or " +
            "(#{ProgrmStagA.DataElmentE}  == ''   and A{Attribute0A}== 0)";
        String actual = programIndicatorService.getAnalyticsSql( filter, createProgramIndicator( 'X', AnalyticsType.EVENT, programA, null, filter ), new Date(), new Date(), true );
        assertEquals( expected, actual );
    }

    @Test
    public void testEnrollmentIndicatorWithEventBoundaryExpression()
    {
        String expected = "coalesce((select \"DataElmentA\" from analytics_event_Program000B " +
            "where analytics_event_" + indicatorF.getProgram().getUid() +
            ".pi = ax.pi and \"DataElmentA\" is not null " +
            "and executiondate < cast( '2018-03-11' as date ) and "+
            "ps = 'ProgrmStagA' order by executiondate desc limit 1 )::numeric,0)";
        Date reportingStartDate = new GregorianCalendar(2018, Calendar.FEBRUARY, 1).getTime();
        Date reportingEndDate = new GregorianCalendar(2018, Calendar.FEBRUARY, 28).getTime();
        String actual = programIndicatorService.getAnalyticsSql( indicatorF.getExpression(), indicatorF, reportingStartDate, reportingEndDate, true );
        assertEquals( expected, actual );
    }

    @Test
    public void testEnrollmentIndicatorWithEventBoundaryFilter()
    {
        String expected = "(select \"DataElmentA\" from analytics_event_Program000B " +
            "where analytics_event_" + indicatorF.getProgram().getUid() + ".pi " +
            "= ax.pi and \"DataElmentA\" is not null and executiondate < cast( '2018-03-11' as date ) and " +
            "ps = 'ProgrmStagA' order by executiondate desc limit 1 ) > \"" + atA.getUid() + "\"";
        Date reportingStartDate = new GregorianCalendar(2018, Calendar.FEBRUARY, 1).getTime();
        Date reportingEndDate = new GregorianCalendar(2018, Calendar.FEBRUARY, 28).getTime();
        String actual = programIndicatorService.getAnalyticsSql( indicatorF.getFilter(), indicatorF, reportingStartDate, reportingEndDate, false );
        assertEquals( expected, actual );
    }

    @Test
    public void testDateFunctions()
    {
        String expected = "(date_part('year',age(cast('2016-01-01' as date), cast(enrollmentdate as date)))) < 1 " +
            "and (date_part('year',age(cast('2016-12-31' as date), cast(enrollmentdate as date)))) >= 1";

        String filter = "d2:yearsBetween(V{enrollment_date}, V{analytics_period_start}) < 1 " +
            "and d2:yearsBetween(V{enrollment_date}, V{analytics_period_end}) >= 1";
        String actual = programIndicatorService.getAnalyticsSql( filter, createProgramIndicator( 'X', programA, null, filter ), DateUtils.parseDate( "2016-01-01" ) , DateUtils.parseDate( "2016-12-31" ), true );
        assertEquals( expected, actual );
    }

    @Test
    public void testDateFunctionsWithProgramStageDateArguments()
    {
        String expected = "(date_part('year',age(cast((select executiondate from analytics_event_Program000A " +
            "where analytics_event_Program000A.pi = ax.pi and executiondate is not null and ps = 'ProgrmStagA' " +
            "order by executiondate desc limit 1 ) as date), cast(enrollmentdate as date)))) < 1 and " +
            "(date_part('month',age(cast((select executiondate from analytics_event_Program000A where " +
            "analytics_event_Program000A.pi = ax.pi and executiondate is not null and ps = 'ProgrmStagB' order " +
            "by executiondate desc limit 1 ) as date), cast((select executiondate from analytics_event_Program000A " +
            "where analytics_event_Program000A.pi = ax.pi and executiondate is not null and ps = 'ProgrmStagA' order " +
            "by executiondate desc limit 1 ) as date)))) > 10";

        String filter = "d2:yearsBetween(V{enrollment_date}, PS_EVENTDATE:ProgrmStagA) < 1 " +
                "and d2:monthsBetween(PS_EVENTDATE:ProgrmStagA, PS_EVENTDATE:ProgrmStagB) > 10";
        String actual = programIndicatorService.getAnalyticsSql( filter, createProgramIndicator( 'X', AnalyticsType.ENROLLMENT, programA, null, filter ), DateUtils.parseDate( "2016-01-01" ) , DateUtils.parseDate( "2016-12-31" ), true );
        assertEquals( expected, actual );
    }
    @Test
    public void testDateFunctionsWithprogramStageDateArgumentsAndBoundaries()
    {
        String expected = "(date_part('year',age(cast((select executiondate from analytics_event_" + programA.getUid() + " where analytics_event_" +
            programA.getUid() + ".pi = ax.pi and executiondate is not null and executiondate < cast( '2017-01-01' as date ) and executiondate >= " +
            "cast( '2016-01-01' as date ) and ps = '" + psA.getUid() + "' order by executiondate desc limit 1 ) as date), cast(enrollmentdate as date)))) < 1";

        String filter = "d2:yearsBetween(V{enrollment_date}, PS_EVENTDATE:" + psA.getUid() + ") < 1";
        ProgramIndicator programIndicator = createProgramIndicator( 'X', AnalyticsType.ENROLLMENT, programA, filter, null );
        Set<AnalyticsPeriodBoundary> boundaries = new HashSet<AnalyticsPeriodBoundary>();
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE, AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD, null, 0 ) );
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.EVENT_DATE, AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD, null, 0 ) );
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.ENROLLMENT_DATE, AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD, null, 0 ) );
        boundaries.add( new AnalyticsPeriodBoundary( AnalyticsPeriodBoundary.ENROLLMENT_DATE, AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD, null, 0 ) );
        programIndicator.setAnalyticsPeriodBoundaries( boundaries );

        String actual = programIndicatorService.getAnalyticsSql( filter, programIndicator, DateUtils.parseDate( "2016-01-01" ) , DateUtils.parseDate( "2016-12-31" ), true );
        assertEquals( expected, actual );
    }

    @Test
    public void testExpressionIsValid()
    {
        programIndicatorService.addProgramIndicator( indicatorB );
        programIndicatorService.addProgramIndicator( indicatorA );
        programIndicatorService.addProgramIndicator( indicatorD );

        assertTrue( programIndicatorService.expressionIsValid( indicatorB.getExpression() ) );
        assertTrue( programIndicatorService.expressionIsValid( indicatorA.getExpression() ) );
        assertFalse( programIndicatorService.expressionIsValid( indicatorD.getExpression() ) );
    }

    @Test
    public void testExpressionWithFunctionIsValid()
    {
        String exprA = "#{" + psA.getUid() + "." + deA.getUid() + "}";
        String exprB = "d2:zing(#{" + psA.getUid() + "." + deA.getUid() + "})";
        String exprC = "d2:condition('#{" + psA.getUid() + "." + deA.getUid() + "} > 10',2,1)";

        assertTrue( programIndicatorService.expressionIsValid( exprA ) );
        assertTrue( programIndicatorService.expressionIsValid( exprB ) );
        assertTrue( programIndicatorService.expressionIsValid( exprC ) );
    }

    @Test
    public void testFilterIsValid()
    {
        String filterA = KEY_DATAELEMENT + "{" + psA.getUid() + "." + deA.getUid() + "}  - " + KEY_ATTRIBUTE + "{" + atA.getUid() + "} > 10";
        String filterB = KEY_ATTRIBUTE + "{" + atA.getUid() + "} == " + KEY_DATAELEMENT + "{" + psA.getUid() + "." + deA.getUid() + "} - 5";
        String filterC = KEY_ATTRIBUTE + "{invaliduid} == 100";
        String filterD = KEY_ATTRIBUTE + "{" + atA.getUid() + "} + 200";

        assertTrue( programIndicatorService.filterIsValid( filterA ) );
        assertTrue( programIndicatorService.filterIsValid( filterB ) );
        assertFalse( programIndicatorService.filterIsValid( filterC ) );
        assertFalse( programIndicatorService.filterIsValid( filterD ) );
    }

    @Test
    public void testd2relationshipCountFilter()
    {
        String expected = "(select count(*) from relationship r join relationshipitem rifrom on rifrom.relationshipid = r.relationshipid join " +
            "trackedentityinstance tei on rifrom.trackedentityinstanceid = tei.trackedentityinstanceid and tei.uid = ax.tei)";

        String filter = "d2:relationshipCount()";
        String actual = programIndicatorService.getAnalyticsSql( filter, createProgramIndicator( 'X', programA, filter, null ), DateUtils.parseDate( "2016-01-01" ) , DateUtils.parseDate( "2016-12-31" ), true );
        assertEquals( expected, actual );
    }

    @Test
    public void testd2relationshipCountForOneRelationshipTypeFilter()
    {
        String expected = "(select count(*) from relationship r join relationshiptype rt on r.relationshiptypeid = rt.relationshiptypeid and " +
            "rt.uid = 'Zx7OEwPBUwD' join relationshipitem rifrom on rifrom.relationshipid = r.relationshipid " +
            "join trackedentityinstance tei on rifrom.trackedentityinstanceid = tei.trackedentityinstanceid and tei.uid = ax.tei)";

        String filter = "d2:relationshipCount('Zx7OEwPBUwD')";
        String actual = programIndicatorService.getAnalyticsSql( filter, createProgramIndicator( 'X', programA, filter, null ), DateUtils.parseDate( "2016-01-01" ) , DateUtils.parseDate( "2016-12-31" ), true );
        assertEquals( expected, actual );
    }
}
