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

<<<<<<< HEAD
import com.google.common.collect.Sets;
||||||| merged common ancestors
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

=======
import static org.hisp.dhis.program.ProgramIndicator.KEY_ATTRIBUTE;
import static org.hisp.dhis.program.ProgramIndicator.KEY_DATAELEMENT;
import static org.hisp.dhis.program.ProgramIndicator.KEY_PROGRAM_VARIABLE;
import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;

import java.util.*;

>>>>>>> [wip] move parse tree traversal into expr & PI services
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Chau Thu Tran
 */
public class ProgramInstanceServiceTest
        extends DhisSpringTest
{
    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    private Date incidenDate;

    private Date enrollmentDate;

    private Program programA;

    private Program programB;

    private Program programC;

<<<<<<< HEAD
    private OrganisationUnit organisationUnitA;
||||||| merged common ancestors
    private TrackedEntityAttribute atA;
=======
    private DataElement deC;

    private DataElement deD;

    private DataElement deE;

    private TrackedEntityAttribute atA;
>>>>>>> [wip] move parse tree traversal into expr & PI services

    private OrganisationUnit organisationUnitB;

    private ProgramInstance programInstanceA;

    private ProgramInstance programInstanceB;

    private ProgramInstance programInstanceC;

    private ProgramInstance programInstanceD;

    private TrackedEntityInstance entityInstanceA;

    private Collection<Integer> orgunitIds;

    @Override
    public void setUpTest()
    {
        organisationUnitA = createOrganisationUnit( 'A' );
        long idA = organisationUnitService.addOrganisationUnit( organisationUnitA );

        organisationUnitB = createOrganisationUnit( 'B' );
        long idB = organisationUnitService.addOrganisationUnit( organisationUnitB );

        orgunitIds = new HashSet<>();
        orgunitIds.add( idA );
        orgunitIds.add( idB );

        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );

<<<<<<< HEAD
||||||| merged common ancestors
        programA = createProgram( 'A', new HashSet<>(), organisationUnit );
=======
        programA = createProgram( 'A', new HashSet<>(), organisationUnit );
        programA.setUid( "Program000A" );
>>>>>>> [wip] move parse tree traversal into expr & PI services
        programService.addProgram( programA );

<<<<<<< HEAD
        ProgramStage stageA = createProgramStage( 'A', programA );
        stageA.setSortOrder( 1 );
        programStageService.saveProgramStage( stageA );
||||||| merged common ancestors
        psA = new ProgramStage( "StageA", programA );
        psA.setSortOrder( 1 );
        programStageService.saveProgramStage( psA );
=======
        psA = new ProgramStage( "StageA", programA );
        psA.setSortOrder( 1 );
        psA.setUid( "ProgrmStagA" );
        programStageService.saveProgramStage( psA );
>>>>>>> [wip] move parse tree traversal into expr & PI services

<<<<<<< HEAD
        ProgramStage stageB = createProgramStage( 'B', programA );
        stageB.setSortOrder( 2 );
        programStageService.saveProgramStage( stageB );
||||||| merged common ancestors
        psB = new ProgramStage( "StageB", programA );
        psB.setSortOrder( 2 );
        programStageService.saveProgramStage( psB );
=======
        psB = new ProgramStage( "StageB", programA );
        psB.setSortOrder( 2 );
        psB.setUid( "ProgrmStagB" );
        programStageService.saveProgramStage( psB );
>>>>>>> [wip] move parse tree traversal into expr & PI services

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        programA.setProgramStages( programStages );
        programService.updateProgram( programA );

<<<<<<< HEAD
        programB = createProgram( 'B', new HashSet<>(), organisationUnitA );
||||||| merged common ancestors
        programB = createProgram( 'B', new HashSet<>(), organisationUnit );
=======
        programB = createProgram( 'B', new HashSet<>(), organisationUnit );
        programB.setUid( "Program000B" );
>>>>>>> [wip] move parse tree traversal into expr & PI services
        programService.addProgram( programB );

<<<<<<< HEAD
        programC = createProgram( 'C', new HashSet<>(), organisationUnitA );
        programService.addProgram( programC );
||||||| merged common ancestors
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
=======
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

        TrackedEntityInstance entityInstance = createTrackedEntityInstance( 'A', organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstance );

        incidentDate = DateUtils.getMediumDate( "2014-10-22" );
        enrollmentDate = DateUtils.getMediumDate( "2014-12-31" );
>>>>>>> [wip] move parse tree traversal into expr & PI services

        entityInstanceA = createTrackedEntityInstance( 'A', organisationUnitA );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA );

        TrackedEntityInstance entityInstanceB = createTrackedEntityInstance( 'B', organisationUnitB );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB );

        DateTime testDate1 = DateTime.now();
        testDate1.withTimeAtStartOfDay();
        testDate1 = testDate1.minusDays( 70 );
        incidenDate = testDate1.toDate();

        DateTime testDate2 = DateTime.now();
        testDate2.withTimeAtStartOfDay();
        enrollmentDate = testDate2.toDate();

        programInstanceA = new ProgramInstance( enrollmentDate, incidenDate, entityInstanceA, programA );
        programInstanceA.setUid( "UID-A" );
        programInstanceA.setOrganisationUnit( organisationUnitA );

        programInstanceB = new ProgramInstance( enrollmentDate, incidenDate, entityInstanceA, programB );
        programInstanceB.setUid( "UID-B" );
        programInstanceB.setStatus( ProgramStatus.CANCELLED );
        programInstanceB.setOrganisationUnit( organisationUnitB );

<<<<<<< HEAD
        programInstanceC = new ProgramInstance( enrollmentDate, incidenDate, entityInstanceA, programC );
        programInstanceC.setUid( "UID-C" );
        programInstanceC.setStatus( ProgramStatus.COMPLETED );
        programInstanceC.setOrganisationUnit( organisationUnitA );
||||||| merged common ancestors
        attributeService.addTrackedEntityAttribute( atA );
        attributeService.addTrackedEntityAttribute( atB );
=======
        atA.setUid( "Attribute0A" );
        atB.setUid( "Attribute0B" );

        attributeService.addTrackedEntityAttribute( atA );
        attributeService.addTrackedEntityAttribute( atB );
>>>>>>> [wip] move parse tree traversal into expr & PI services

        programInstanceD = new ProgramInstance( enrollmentDate, incidenDate, entityInstanceB, programA );
        programInstanceD.setUid( "UID-D" );
        programInstanceD.setOrganisationUnit( organisationUnitB );
    }

    @Test
    public void testAddProgramInstance()
    {
        long idA = programInstanceService.addProgramInstance( programInstanceA );
        long idB = programInstanceService.addProgramInstance( programInstanceB );

        assertNotNull( programInstanceService.getProgramInstance( idA ) );
        assertNotNull( programInstanceService.getProgramInstance( idB ) );
    }

    @Test
    public void testDeleteProgramInstance()
    {
        long idA = programInstanceService.addProgramInstance( programInstanceA );
        long idB = programInstanceService.addProgramInstance( programInstanceB );

        assertNotNull( programInstanceService.getProgramInstance( idA ) );
        assertNotNull( programInstanceService.getProgramInstance( idB ) );

        programInstanceService.deleteProgramInstance( programInstanceA );

        assertNull( programInstanceService.getProgramInstance( idA ) );
        assertNotNull( programInstanceService.getProgramInstance( idB ) );

        programInstanceService.deleteProgramInstance( programInstanceB );

        assertNull( programInstanceService.getProgramInstance( idA ) );
        assertNull( programInstanceService.getProgramInstance( idB ) );
    }

    @Test
    public void testUpdateProgramInstance()
    {
        long idA = programInstanceService.addProgramInstance( programInstanceA );

        assertNotNull( programInstanceService.getProgramInstance( idA ) );

        programInstanceA.setIncidentDate( enrollmentDate );
        programInstanceService.updateProgramInstance( programInstanceA );

        assertEquals( enrollmentDate, programInstanceService.getProgramInstance( idA ).getIncidentDate() );
    }

    @Test
    public void testGetProgramInstanceById()
    {
        long idA = programInstanceService.addProgramInstance( programInstanceA );
        long idB = programInstanceService.addProgramInstance( programInstanceB );

        assertEquals( programInstanceA, programInstanceService.getProgramInstance( idA ) );
        assertEquals( programInstanceB, programInstanceService.getProgramInstance( idB ) );
    }

    @Test
    public void testGetProgramInstanceByUid()
    {
        programInstanceService.addProgramInstance( programInstanceA );
        programInstanceService.addProgramInstance( programInstanceB );

        assertEquals( "UID-A", programInstanceService.getProgramInstance( "UID-A" ).getUid() );
        assertEquals( "UID-B", programInstanceService.getProgramInstance( "UID-B" ).getUid() );
    }

    @Test
    public void testGetProgramInstancesByProgram()
    {
        programInstanceService.addProgramInstance( programInstanceA );
        programInstanceService.addProgramInstance( programInstanceB );
        programInstanceService.addProgramInstance( programInstanceD );

        List<ProgramInstance> programInstances = programInstanceService.getProgramInstances( programA );
        assertEquals( 2, programInstances.size() );
        assertTrue( programInstances.contains( programInstanceA ) );
        assertTrue( programInstances.contains( programInstanceD ) );

        programInstances = programInstanceService.getProgramInstances( programB );
        assertEquals( 1, programInstances.size() );
        assertTrue( programInstances.contains( programInstanceB ) );
    }

    @Test
    public void testGetProgramInstancesByEntityInstanceProgramStatus()
    {
        programInstanceService.addProgramInstance( programInstanceA );

        ProgramInstance programInstance1 = programInstanceService.enrollTrackedEntityInstance( entityInstanceA, programA, enrollmentDate,
                incidenDate, organisationUnitA );
        programInstance1.setStatus( ProgramStatus.COMPLETED );
        programInstanceService.updateProgramInstance( programInstance1 );

        ProgramInstance programInstance2 = programInstanceService.enrollTrackedEntityInstance( entityInstanceA, programA, enrollmentDate,
                incidenDate, organisationUnitA );
        programInstance2.setStatus( ProgramStatus.COMPLETED );
        programInstanceService.updateProgramInstance( programInstance2 );

        List<ProgramInstance> programInstances = programInstanceService.getProgramInstances( entityInstanceA, programA, ProgramStatus.COMPLETED );
        assertEquals( 2, programInstances.size() );
        assertTrue( programInstances.contains( programInstance1 ) );
        assertTrue( programInstances.contains( programInstance2 ) );

        programInstances = programInstanceService.getProgramInstances( entityInstanceA, programA,
                ProgramStatus.ACTIVE );
        assertEquals( 1, programInstances.size() );
        assertTrue( programInstances.contains( programInstanceA ) );
    }

    @Test
    public void testGetProgramInstancesByOuProgram()
    {
        programInstanceService.addProgramInstance( programInstanceA );
        programInstanceService.addProgramInstance( programInstanceC );
        programInstanceService.addProgramInstance( programInstanceD );

        List<ProgramInstance> programInstances = programInstanceService.getProgramInstances( new ProgramInstanceQueryParams()
                .setProgram( programA )
                .setOrganisationUnits( Sets.newHashSet( organisationUnitA ) )
                .setOrganisationUnitMode( OrganisationUnitSelectionMode.SELECTED ) );
        assertEquals( 1, programInstances.size() );
        assertTrue( programInstances.contains( programInstanceA ) );
    }

    @Test
    public void testEnrollTrackedEntityInstance()
    {
        ProgramInstance programInstance = programInstanceService.enrollTrackedEntityInstance( entityInstanceA, programB, enrollmentDate,
                incidenDate, organisationUnitA );

        assertNotNull( programInstanceService.getProgramInstance( programInstance.getId() ) );
    }

    @Test
    @Ignore
    public void testCanAutoCompleteProgramInstanceStatus()
    {
        programInstanceService.addProgramInstance( programInstanceA );
        programInstanceService.addProgramInstance( programInstanceD );

        assertTrue( programInstanceService.canAutoCompleteProgramInstanceStatus( programInstanceA ) );
        assertTrue( programInstanceService.canAutoCompleteProgramInstanceStatus( programInstanceD ) );
    }

    @Test
    public void testCompleteProgramInstanceStatus()
    {
        int idA = programInstanceService.addProgramInstance( programInstanceA );
        int idD = programInstanceService.addProgramInstance( programInstanceD );

        programInstanceService.completeProgramInstanceStatus( programInstanceA );
        programInstanceService.completeProgramInstanceStatus( programInstanceD );

        assertEquals( ProgramStatus.COMPLETED, programInstanceService.getProgramInstance( idA ).getStatus() );
        assertEquals( ProgramStatus.COMPLETED, programInstanceService.getProgramInstance( idD ).getStatus() );
    }

    @Test
    public void testIncompleteProgramInstanceStatus()
    {
        programInstanceA.setStatus( ProgramStatus.COMPLETED );
        programInstanceD.setStatus( ProgramStatus.COMPLETED );

        long idA = programInstanceService.addProgramInstance( programInstanceA );
        long idD = programInstanceService.addProgramInstance( programInstanceD );

        programInstanceService.incompleteProgramInstanceStatus( programInstanceA );
        programInstanceService.incompleteProgramInstanceStatus( programInstanceD );

        assertEquals( ProgramStatus.ACTIVE, programInstanceService.getProgramInstance( idA ).getStatus() );
        assertEquals( ProgramStatus.ACTIVE, programInstanceService.getProgramInstance( idD ).getStatus() );
    }

    @Test
    public void testCancelProgramInstanceStatus()
    {
        long idA = programInstanceService.addProgramInstance( programInstanceA );
        long idD = programInstanceService.addProgramInstance( programInstanceD );

        programInstanceService.cancelProgramInstanceStatus( programInstanceA );
        programInstanceService.cancelProgramInstanceStatus( programInstanceD );

        assertEquals( ProgramStatus.CANCELLED, programInstanceService.getProgramInstance( idA ).getStatus() );
        assertEquals( ProgramStatus.CANCELLED, programInstanceService.getProgramInstance( idD ).getStatus() );
    }
<<<<<<< HEAD
}
||||||| merged common ancestors
}
=======

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
>>>>>>> [wip] move parse tree traversal into expr & PI services
