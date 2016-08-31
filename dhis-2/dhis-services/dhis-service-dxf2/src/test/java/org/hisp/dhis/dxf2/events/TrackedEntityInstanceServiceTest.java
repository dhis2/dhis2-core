package org.hisp.dhis.dxf2.events;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackedEntityInstanceServiceTest
    extends DhisSpringTest
{
    @Autowired
    private TrackedEntityService trackedEntityService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private IdentifiableObjectManager manager;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance maleA;
    private org.hisp.dhis.trackedentity.TrackedEntityInstance maleB;
    private org.hisp.dhis.trackedentity.TrackedEntityInstance femaleA;
    private org.hisp.dhis.trackedentity.TrackedEntityInstance femaleB;

    private OrganisationUnit organisationUnitA;
    private OrganisationUnit organisationUnitB;

    private Program programA;

    @Override
    protected void setUpTest() throws Exception
    {
        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitB = createOrganisationUnit( 'B' );

        organisationUnitB.setParent( organisationUnitA );

        TrackedEntity trackedEntity = createTrackedEntity( 'A' );
        trackedEntityService.addTrackedEntity( trackedEntity );

        maleA = createTrackedEntityInstance( 'A', organisationUnitA );
        maleB = createTrackedEntityInstance( 'B', organisationUnitB );
        femaleA = createTrackedEntityInstance( 'C', organisationUnitA );
        femaleB = createTrackedEntityInstance( 'D', organisationUnitB );

        maleA.setTrackedEntity( trackedEntity );
        maleB.setTrackedEntity( trackedEntity );
        femaleA.setTrackedEntity( trackedEntity );
        femaleB.setTrackedEntity( trackedEntity );

        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        manager.save( organisationUnitA );
        manager.save( organisationUnitB );
        manager.save( maleA );
        manager.save( maleB );
        manager.save( femaleA );
        manager.save( femaleB );
        manager.save( programA );

        programInstanceService.enrollTrackedEntityInstance( maleA, programA, null, null, organisationUnitA );
        programInstanceService.enrollTrackedEntityInstance( femaleA, programA, null, null, organisationUnitA );
    }

    @Test
    public void getPersonByUid()
    {
        assertEquals( maleA.getUid(), trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() ).getTrackedEntityInstance() );
        assertEquals( femaleB.getUid(), trackedEntityInstanceService.getTrackedEntityInstance( femaleB.getUid() ).getTrackedEntityInstance() );
        assertNotEquals( femaleA.getUid(), trackedEntityInstanceService.getTrackedEntityInstance( femaleB.getUid() ).getTrackedEntityInstance() );
        assertNotEquals( maleA.getUid(), trackedEntityInstanceService.getTrackedEntityInstance( maleB.getUid() ).getTrackedEntityInstance() );
    }

    @Test
    public void getPersonByPatient()
    {
        assertEquals( maleA.getUid(), trackedEntityInstanceService.getTrackedEntityInstance( maleA ).getTrackedEntityInstance() );
        assertEquals( femaleB.getUid(), trackedEntityInstanceService.getTrackedEntityInstance( femaleB ).getTrackedEntityInstance() );
        assertNotEquals( femaleA.getUid(), trackedEntityInstanceService.getTrackedEntityInstance( femaleB ).getTrackedEntityInstance() );
        assertNotEquals( maleA.getUid(), trackedEntityInstanceService.getTrackedEntityInstance( maleB ).getTrackedEntityInstance() );
    }

    @Test
    @Ignore
    public void testUpdatePerson()
    {
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        // person.setName( "UPDATED_NAME" );

        ImportSummary importSummary = trackedEntityInstanceService.updateTrackedEntityInstance( trackedEntityInstance, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        // assertEquals( "UPDATED_NAME", personService.getTrackedEntityInstance( maleA.getUid() ).getName() );
    }

    @Test
    @Ignore
    public void testSavePerson()
    {
        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        // person.setName( "NAME" );
        trackedEntityInstance.setOrgUnit( organisationUnitA.getUid() );

        ImportSummary importSummary = trackedEntityInstanceService.addTrackedEntityInstance( trackedEntityInstance, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        // assertEquals( "NAME", personService.getTrackedEntityInstance( importSummary.getReference() ).getName() );
    }

    @Test
    public void testDeletePerson()
    {
        trackedEntityInstanceService.deleteTrackedEntityInstance( maleA.getUid() );

        assertNull( trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() ) );
        assertNotNull( trackedEntityInstanceService.getTrackedEntityInstance( maleB.getUid() ) );
    }

    @Test
    public void testDeleteTrackedEntityInstances()
    {
        List<String> uids = Lists.newArrayList( maleA.getUid(), maleB.getUid() );
        trackedEntityInstanceService.deleteTrackedEntityInstances( uids );

        assertNull( trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() ) );
        assertNull( trackedEntityInstanceService.getTrackedEntityInstance( maleB.getUid() ) );
    }
}
