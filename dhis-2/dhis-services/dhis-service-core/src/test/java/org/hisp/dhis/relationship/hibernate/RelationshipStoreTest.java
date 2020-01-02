package org.hisp.dhis.relationship.hibernate;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipStore;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.relationship.RelationshipTypeStore;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.*;
import org.hisp.dhis.program.*;
import org.hisp.dhis.relationship.*;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

@Category( IntegrationTest.class )
public class RelationshipStoreTest
    extends IntegrationTestBase
{

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageService programStageService;

    private TrackedEntityInstance trackedEntityInstanceA;

    private TrackedEntityInstance trackedEntityInstanceB;

    private RelationshipType relationshipType;

    private Relationship relationship;

    private OrganisationUnit organisationUnit;

    @Override
    public void setUpTest()
    {

        relationshipType = createRelationshipType( 'A' );
        relationshipTypeService.addRelationshipType( relationshipType );

        organisationUnit = createOrganisationUnit( "testOU" );

        organisationUnitService.addOrganisationUnit( organisationUnit );

        trackedEntityInstanceA = createTrackedEntityInstance( organisationUnit );
        trackedEntityInstanceB = createTrackedEntityInstance( organisationUnit );

        trackedEntityInstanceService.addTrackedEntityInstance( trackedEntityInstanceA );
        trackedEntityInstanceService.addTrackedEntityInstance( trackedEntityInstanceB );

        relationship = new Relationship();
        RelationshipItem relationshipItemFrom = new RelationshipItem();
        RelationshipItem relationshipItemTo = new RelationshipItem();
        relationshipItemFrom.setTrackedEntityInstance( trackedEntityInstanceA );
        relationshipItemTo.setTrackedEntityInstance( trackedEntityInstanceB );

        relationship.setRelationshipType( relationshipType );
        relationship.setFrom( relationshipItemFrom );
        relationship.setTo( relationshipItemTo );

        relationshipService.addRelationship( relationship );
    }

    @Test
    public void getByTrackedEntityInstance()
    {
        List<Relationship> relationshipList = relationshipService.getRelationshipsByTrackedEntityInstance( trackedEntityInstanceA, true );

        assertEquals( 1, relationshipList.size() );
        assertTrue( relationshipList.contains( relationship ) );
    }

    @Test
    public void getByProgramStageInstance()
    {
        Program programA = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( programA );
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setProgram( programA );
        programInstance.setAutoFields();
        programInstance.setEnrollmentDate( new Date() );
        programInstance.setIncidentDate( new Date() );
        programInstance.setStatus( ProgramStatus.ACTIVE );
        programInstanceService.addProgramInstance( programInstance );

        ProgramStage programStageA = createProgramStage( 'S', programA );
        programStageA.setProgram( programA );
        programStageService.saveProgramStage( programStageA );
        programA.getProgramStages().add( programStageA );
        programService.updateProgram( programA );

        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setOrganisationUnit( organisationUnit );
        programStageInstance.setProgramStage( programStageA );
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setAutoFields();

        programStageInstanceService.addProgramStageInstance( programStageInstance );

        RelationshipItem relationshipItemFrom = new RelationshipItem();
        relationshipItemFrom.setTrackedEntityInstance( trackedEntityInstanceA );
        RelationshipItem relationshipItemTo = new RelationshipItem();
        relationshipItemTo.setProgramStageInstance( programStageInstance );

        Relationship relationshipA = new Relationship();
        relationshipA.setRelationshipType( relationshipType );
        relationshipA.setFrom( relationshipItemFrom );
        relationshipA.setTo( relationshipItemTo );

        relationshipService.addRelationship( relationshipA );

        List<Relationship> relationshipList = relationshipService
            .getRelationshipsByProgramStageInstance( programStageInstance, true );

        assertEquals( 1, relationshipList.size() );
        assertTrue( relationshipList.contains( relationshipA ) );
    }

    @Test
    public void getByRelationshipType()
    {
        List<Relationship> relationshipList = relationshipService.getRelationshipsByRelationshipType( relationshipType );

        assertEquals( 1, relationshipList.size() );
        assertTrue( relationshipList.contains( relationship ) );
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }
}