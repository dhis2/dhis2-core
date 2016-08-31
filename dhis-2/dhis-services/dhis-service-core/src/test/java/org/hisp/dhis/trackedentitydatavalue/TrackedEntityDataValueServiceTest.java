package org.hisp.dhis.trackedentitydatavalue;

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
public class TrackedEntityDataValueServiceTest
    extends DhisSpringTest
{
    @Autowired
    private TrackedEntityDataValueService dataValueService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementService dataElementService;

    private ProgramInstance programInstance;

    private ProgramStageInstance stageInstanceA;

    private ProgramStageInstance stageInstanceB;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private TrackedEntityDataValue dataValueA;

    private TrackedEntityDataValue dataValueB;

    private TrackedEntityDataValue dataValueC;

    private TrackedEntityDataValue dataValueD;

    private Date yesterday;

    private Date tomorrow;

    private TrackedEntityInstance entityInstance;

    @Override
    public void setUpTest()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );

        entityInstance = createTrackedEntityInstance( 'A', organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstance );

        Program program = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( program );

        ProgramStage stageA = createProgramStage( 'A', 0 );
        stageA.setProgram( program );
        programStageService.saveProgramStage( stageA );

        ProgramStage stageB = createProgramStage( 'B', 0 );
        stageB.setProgram( program );
        programStageService.saveProgramStage( stageB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        program.setProgramStages( programStages );
        programService.updateProgram( program );

        DateTime yesterDate = DateTime.now();
        yesterDate.withTimeAtStartOfDay();
        yesterDate.minusDays( 1 );
        yesterday = yesterDate.toDate();

        DateTime tomorrowDate = DateTime.now();
        tomorrowDate.withTimeAtStartOfDay();
        tomorrowDate.plusDays( 1 );
        tomorrow = tomorrowDate.toDate();

        programInstance = programInstanceService.enrollTrackedEntityInstance( entityInstance, program, yesterday,
            yesterday, organisationUnit );

        stageInstanceA = programStageInstanceService.createProgramStageInstance( programInstance, stageA, yesterday,
            yesterday, organisationUnit );
        stageInstanceB = programStageInstanceService.createProgramStageInstance( programInstance, stageB, yesterday,
            yesterday, organisationUnit );

        dataValueA = new TrackedEntityDataValue( stageInstanceA, dataElementA, "1" );
        dataValueB = new TrackedEntityDataValue( stageInstanceA, dataElementB, "2" );
        dataValueC = new TrackedEntityDataValue( stageInstanceB, dataElementA, "3" );
        dataValueD = new TrackedEntityDataValue( stageInstanceB, dataElementB, "4" );
    }

    @Test
    public void testSaveTrackedEntityDataValue()
    {
        dataValueService.saveTrackedEntityDataValue( dataValueA );
        dataValueService.saveTrackedEntityDataValue( dataValueB );

        assertNotNull( dataValueService.getTrackedEntityDataValue( stageInstanceA, dataElementA ) );
        assertNotNull( dataValueService.getTrackedEntityDataValue( stageInstanceA, dataElementB ) );
    }

    @Test
    public void testDeleteTrackedEntityDataValue()
    {
        dataValueService.saveTrackedEntityDataValue( dataValueA );
        dataValueService.saveTrackedEntityDataValue( dataValueB );

        assertNotNull( dataValueService.getTrackedEntityDataValue( stageInstanceA, dataElementA ) );
        assertNotNull( dataValueService.getTrackedEntityDataValue( stageInstanceA, dataElementB ) );

        dataValueService.deleteTrackedEntityDataValue( dataValueA );

        assertNull( dataValueService.getTrackedEntityDataValue( stageInstanceA, dataElementA ) );
        assertNotNull( dataValueService.getTrackedEntityDataValue( stageInstanceA, dataElementB ) );

        dataValueService.deleteTrackedEntityDataValue( dataValueB );

        assertNull( dataValueService.getTrackedEntityDataValue( stageInstanceA, dataElementA ) );
        assertNull( dataValueService.getTrackedEntityDataValue( stageInstanceA, dataElementB ) );
    }

    @Test
    public void testUpdateTrackedEntityDataValue()
    {
        dataValueService.saveTrackedEntityDataValue( dataValueA );

        assertNotNull( dataValueService.getTrackedEntityDataValue( stageInstanceA, dataElementA ) );

        dataValueA.setValue( "2" );
        dataValueService.updateTrackedEntityDataValue( dataValueA );

        assertEquals( "2", dataValueService.getTrackedEntityDataValue( stageInstanceA, dataElementA ).getValue() );
    }

    @Test
    public void testGetTrackedEntityDataValuesByStageInstance()
    {
        dataValueService.saveTrackedEntityDataValue( dataValueA );
        dataValueService.saveTrackedEntityDataValue( dataValueB );
        dataValueService.saveTrackedEntityDataValue( dataValueC );
        dataValueService.saveTrackedEntityDataValue( dataValueD );

        List<TrackedEntityDataValue> dataValues = dataValueService.getTrackedEntityDataValues( stageInstanceA );
        assertEquals( 2, dataValues.size() );
        assertTrue( dataValues.contains( dataValueA ) );
        assertTrue( dataValues.contains( dataValueB ) );

        dataValues = dataValueService.getTrackedEntityDataValues( stageInstanceB );
        assertEquals( 2, dataValues.size() );
        assertTrue( dataValues.contains( dataValueC ) );
        assertTrue( dataValues.contains( dataValueD ) );
    }

    @Test
    public void testGetTrackedEntityDataValuesByStageElement()
    {
        dataValueService.saveTrackedEntityDataValue( dataValueA );
        dataValueService.saveTrackedEntityDataValue( dataValueB );
        dataValueService.saveTrackedEntityDataValue( dataValueC );
        dataValueService.saveTrackedEntityDataValue( dataValueD );

        List<DataElement> dataElements = new ArrayList<>();
        dataElements.add( dataElementA );
        dataElements.add( dataElementB );

        List<TrackedEntityDataValue> dataValues = dataValueService.getTrackedEntityDataValues( stageInstanceA,
            dataElements );
        assertEquals( 2, dataValues.size() );
        assertTrue( dataValues.contains( dataValueA ) );
        assertTrue( dataValues.contains( dataValueB ) );
    }

    @Test
    public void testGetTrackedEntityDataValues()
    {
        dataValueService.saveTrackedEntityDataValue( dataValueA );
        dataValueService.saveTrackedEntityDataValue( dataValueB );
        dataValueService.saveTrackedEntityDataValue( dataValueC );
        dataValueService.saveTrackedEntityDataValue( dataValueD );

        List<ProgramStageInstance> programStageInstances = new ArrayList<>();
        programStageInstances.add( stageInstanceA );
        programStageInstances.add( stageInstanceB );

        List<TrackedEntityDataValue> dataValues = dataValueService
            .getTrackedEntityDataValues( programStageInstances );
        assertEquals( 4, dataValues.size() );
        assertTrue( dataValues.contains( dataValueA ) );
        assertTrue( dataValues.contains( dataValueB ) );
        assertTrue( dataValues.contains( dataValueC ) );
        assertTrue( dataValues.contains( dataValueD ) );
    }

    @Test
    public void testGetTrackedEntityDataValuesByDataElement()
    {
        dataValueService.saveTrackedEntityDataValue( dataValueA );
        dataValueService.saveTrackedEntityDataValue( dataValueB );
        dataValueService.saveTrackedEntityDataValue( dataValueC );
        dataValueService.saveTrackedEntityDataValue( dataValueD );

        List<TrackedEntityDataValue> dataValues = dataValueService.getTrackedEntityDataValues( dataElementA );
        assertEquals( 2, dataValues.size() );
        assertTrue( dataValues.contains( dataValueA ) );
        assertTrue( dataValues.contains( dataValueC ) );

        dataValues = dataValueService.getTrackedEntityDataValues( dataElementB );
        assertEquals( 2, dataValues.size() );
        assertTrue( dataValues.contains( dataValueB ) );
        assertTrue( dataValues.contains( dataValueD ) );
    }

    @Test
    public void testGetDataValuesByDataElement()
    {
        dataValueService.saveTrackedEntityDataValue( dataValueA );
        dataValueService.saveTrackedEntityDataValue( dataValueB );
        dataValueService.saveTrackedEntityDataValue( dataValueC );
        dataValueService.saveTrackedEntityDataValue( dataValueD );

        List<DataElement> dataElements = new ArrayList<>();
        dataElements.add( dataElementA );
        dataElements.add( dataElementB );

        List<TrackedEntityDataValue> dataValues = dataValueService.getTrackedEntityDataValues( entityInstance,
            dataElements, yesterday, tomorrow );

        dataValues = dataValueService.getTrackedEntityDataValues( dataElementB );
        assertEquals( 2, dataValues.size() );
        assertTrue( dataValues.contains( dataValueB ) );
        assertTrue( dataValues.contains( dataValueD ) );
    }

    @Test
    public void testGetDataValue()
    {
        dataValueService.saveTrackedEntityDataValue( dataValueA );
        dataValueService.saveTrackedEntityDataValue( dataValueB );

        TrackedEntityDataValue dataValue = dataValueService.getTrackedEntityDataValue( stageInstanceA, dataElementA );
        assertEquals( dataValueA, dataValue );

        dataValue = dataValueService.getTrackedEntityDataValue( stageInstanceA, dataElementB );
        assertEquals( dataValueB, dataValue );
    }

}
