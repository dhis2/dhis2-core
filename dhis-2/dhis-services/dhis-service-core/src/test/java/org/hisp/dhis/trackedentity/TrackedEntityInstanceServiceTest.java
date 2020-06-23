/*
 * Copyright (c) 2004-2020, University of Oslo
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
package org.hisp.dhis.trackedentity;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
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
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Chau Thu Tran
 */
public class TrackedEntityInstanceServiceTest
        extends
        DhisSpringTest
{
    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    private ProgramStageInstance programStageInstanceA;

    private ProgramInstance programInstanceA;

    private TrackedEntityInstance entityInstanceA1;

    private TrackedEntityInstance entityInstanceB1;

    @Override
    public void setUpTest()
    {
        final OrganisationUnit organisationUnit = createOrganisationUnit('A');
        organisationUnitService.addOrganisationUnit(organisationUnit);

        OrganisationUnit organisationUnitB = createOrganisationUnit( 'B' );
        organisationUnitService.addOrganisationUnit( organisationUnitB );

        final TrackedEntityAttribute entityInstanceAttribute = createTrackedEntityAttribute('A');
        attributeService.addTrackedEntityAttribute(entityInstanceAttribute);

        entityInstanceA1 = createTrackedEntityInstance(organisationUnit);
        entityInstanceB1 = createTrackedEntityInstance(organisationUnit);
        entityInstanceB1.setUid( "UID-B1" );

        final Program programA = createProgram('A', new HashSet<>(), organisationUnit);

        programService.addProgram(programA);

        ProgramStage stageA = createProgramStage( 'A', programA);
        stageA.setSortOrder( 1 );
        programStageService.saveProgramStage( stageA );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programA.setProgramStages( programStages );
        programService.updateProgram(programA);

        DateTime enrollmentDate = DateTime.now();
        enrollmentDate.withTimeAtStartOfDay();
        enrollmentDate = enrollmentDate.minusDays( 70 );

        DateTime incidentDate = DateTime.now();
        incidentDate.withTimeAtStartOfDay();

        programInstanceA = new ProgramInstance( enrollmentDate.toDate(), incidentDate.toDate(), entityInstanceA1,
                programA);
        programInstanceA.setUid( "UID-A" );
        programInstanceA.setOrganisationUnit(organisationUnit);

        programStageInstanceA = new ProgramStageInstance( programInstanceA, stageA );
        programInstanceA.setUid( "UID-PSI-A" );
        programInstanceA.setOrganisationUnit(organisationUnit);
    }

    @Test
    public void testSaveTrackedEntityInstance()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( idB ) );
    }

    @Test
    public void testDeleteTrackedEntityInstance()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

        TrackedEntityInstance teiA = entityInstanceService.getTrackedEntityInstance( idA );
        TrackedEntityInstance teiB = entityInstanceService.getTrackedEntityInstance( idB );

        assertNotNull( teiA );
        assertNotNull( teiB );

        entityInstanceService.deleteTrackedEntityInstance( entityInstanceA1 );

        assertNull( entityInstanceService.getTrackedEntityInstance( teiA.getUid() ) );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( teiB.getUid() ) );

        entityInstanceService.deleteTrackedEntityInstance( entityInstanceB1 );

        assertNull( entityInstanceService.getTrackedEntityInstance( teiA.getUid() ) );
        assertNull( entityInstanceService.getTrackedEntityInstance( teiB.getUid() ) );
    }

    @Test
    public void testDeleteTrackedEntityInstanceAndLinkedEnrollmentsAndEvents()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long psIdA = programInstanceService.addProgramInstance( programInstanceA );
        long psiIdA = programStageInstanceService.addProgramStageInstance( programStageInstanceA );

        programInstanceA.setProgramStageInstances( Sets.newHashSet( programStageInstanceA ) );
        entityInstanceA1.setProgramInstances( Sets.newHashSet( programInstanceA ) );

        programInstanceService.updateProgramInstance( programInstanceA );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceA1 );

        TrackedEntityInstance teiA = entityInstanceService.getTrackedEntityInstance( idA );
        ProgramInstance psA = programInstanceService.getProgramInstance( psIdA );
        ProgramStageInstance psiA = programStageInstanceService.getProgramStageInstance( psiIdA );

        assertNotNull( teiA );
        assertNotNull( psA );
        assertNotNull( psiA );

        entityInstanceService.deleteTrackedEntityInstance( entityInstanceA1 );

        assertNull( entityInstanceService.getTrackedEntityInstance( teiA.getUid() ) );
        assertNull( programInstanceService.getProgramInstance( psIdA ) );
        assertNull( programStageInstanceService.getProgramStageInstance( psiIdA ) );

    }

    @Test
    public void testUpdateTrackedEntityInstance()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );

        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );

        entityInstanceA1.setName( "B" );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceA1 );

        assertEquals( "B", entityInstanceService.getTrackedEntityInstance( idA ).getName() );
    }

    @Test
    public void testGetTrackedEntityInstanceById()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

        assertEquals( entityInstanceA1, entityInstanceService.getTrackedEntityInstance( idA ) );
        assertEquals( entityInstanceB1, entityInstanceService.getTrackedEntityInstance( idB ) );
    }

    @Test
    public void testGetTrackedEntityInstanceByUid()
    {
        entityInstanceA1.setUid( "A1" );
        entityInstanceB1.setUid( "B1" );

        entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

        assertEquals( entityInstanceA1, entityInstanceService.getTrackedEntityInstance( "A1" ) );
        assertEquals( entityInstanceB1, entityInstanceService.getTrackedEntityInstance( "B1" ) );
    }

    @Test
    public void testStoredByColumnForTrackedEntityInstance()
    {
        entityInstanceA1.setStoredBy( "test" );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );

        TrackedEntityInstance tei = entityInstanceService.getTrackedEntityInstance( entityInstanceA1.getUid() );
        assertEquals( "test", tei.getStoredBy() );
    }
}
