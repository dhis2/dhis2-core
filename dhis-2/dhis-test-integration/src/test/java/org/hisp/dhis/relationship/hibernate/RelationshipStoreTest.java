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
package org.hisp.dhis.relationship.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.RelationshipUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RelationshipStoreTest extends TransactionalIntegrationTest
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

    @Autowired
    private AttributeService attributeService;

    private TrackedEntityInstance trackedEntityInstanceA;

    private TrackedEntityInstance trackedEntityInstanceB;

    private RelationshipType relationshipType;

    private OrganisationUnit organisationUnit;

    @Override
    public void setUpTest()
    {
        relationshipType = createRelationshipType( 'A' );
        relationshipTypeService.addRelationshipType( relationshipType );
        organisationUnit = createOrganisationUnit( "testOU" );
        organisationUnitService.addOrganisationUnit( organisationUnit );
    }

    @Test
    void testGetByTrackedEntityInstance()
    {
        Relationship teiRelationship = addTeiToTeiRelationship();

        List<Relationship> relationshipList = relationshipService
            .getRelationshipsByTrackedEntityInstance( trackedEntityInstanceA, true );

        assertEquals( 1, relationshipList.size() );
        assertTrue( relationshipList.contains( teiRelationship ) );
    }

    @Test
    void testGetByProgramStageInstance()
    {
        Program programA = addProgram();

        ProgramInstance programInstance = addProgramInstance( programA );

        ProgramStage programStageA = addProgramStage( programA );

        Event event = addProgramStageInstance( programInstance, programStageA );

        trackedEntityInstanceA = createTrackedEntityInstance( organisationUnit );
        trackedEntityInstanceService.addTrackedEntityInstance( trackedEntityInstanceA );

        Relationship relationshipA = addTeiToProgramStageInstanceRelationship( trackedEntityInstanceA,
            event );

        List<Relationship> relationshipList = relationshipService
            .getRelationshipsByProgramStageInstance( event, true );

        assertEquals( 1, relationshipList.size() );
        assertTrue( relationshipList.contains( relationshipA ) );

        assertTrue( relationshipService.getRelationshipByRelationship( relationshipA ).isPresent() );
    }

    @Test
    void testGetByProgramInstance()
    {
        trackedEntityInstanceA = createTrackedEntityInstance( organisationUnit );
        trackedEntityInstanceService.addTrackedEntityInstance( trackedEntityInstanceA );

        Program programA = addProgram();

        ProgramInstance programInstance = addProgramInstance( programA );

        Relationship relationshipA = addTeiToProgramInstanceRelationship( trackedEntityInstanceA,
            programInstance );

        List<Relationship> relationshipList = relationshipService
            .getRelationshipsByProgramInstance( programInstance, true );

        assertEquals( 1, relationshipList.size() );
        assertTrue( relationshipList.contains( relationshipA ) );

        assertTrue( relationshipService.getRelationshipByRelationship( relationshipA ).isPresent() );
    }

    @Test
    void testGetByRelationshipType()
    {
        Relationship teiRelationship = addTeiToTeiRelationship();

        List<Relationship> relationshipList = relationshipService
            .getRelationshipsByRelationshipType( relationshipType );

        assertEquals( 1, relationshipList.size() );
        assertTrue( relationshipList.contains( teiRelationship ) );
    }

    @Test
    void testGetByRelationship()
    {
        Relationship teiRelationship = addTeiToTeiRelationship();

        Optional<Relationship> existing = relationshipService.getRelationshipByRelationship( teiRelationship );

        assertTrue( existing.isPresent() );
    }

    @Test
    void testAddRelationshipTypeWithAttribute()
    {
        Attribute attribute = createAttribute( 'A' );
        attribute.setRelationshipTypeAttribute( true );
        attribute.setValueType( ValueType.TEXT );
        attributeService.addAttribute( attribute );

        relationshipType = createRelationshipType( 'A' );
        relationshipType.getAttributeValues().add( new AttributeValue( attribute, "test" ) );
        relationshipTypeService.addRelationshipType( relationshipType );

        RelationshipType saved = relationshipTypeService.getRelationshipType( relationshipType.getId() );
        assertEquals( "test", saved.getAttributeValue( attribute ).getValue() );
    }

    private Relationship addTeiToTeiRelationship()
    {
        trackedEntityInstanceA = createTrackedEntityInstance( organisationUnit );
        trackedEntityInstanceB = createTrackedEntityInstance( organisationUnit );

        trackedEntityInstanceService.addTrackedEntityInstance( trackedEntityInstanceA );
        trackedEntityInstanceService.addTrackedEntityInstance( trackedEntityInstanceB );

        Relationship teiRelationship = new Relationship();

        RelationshipItem relationshipItemFrom = new RelationshipItem();
        RelationshipItem relationshipItemTo = new RelationshipItem();
        relationshipItemFrom.setTrackedEntityInstance( trackedEntityInstanceA );
        relationshipItemTo.setTrackedEntityInstance( trackedEntityInstanceB );

        teiRelationship.setRelationshipType( relationshipType );
        teiRelationship.setFrom( relationshipItemFrom );
        teiRelationship.setTo( relationshipItemTo );
        teiRelationship.setKey( RelationshipUtils.generateRelationshipKey( teiRelationship ) );
        teiRelationship.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( teiRelationship ) );
        relationshipService.addRelationship( teiRelationship );
        return teiRelationship;
    }

    private Relationship addTeiToProgramStageInstanceRelationship( TrackedEntityInstance entityInstance,
        Event event )
    {
        RelationshipItem relationshipItemFrom = new RelationshipItem();
        relationshipItemFrom.setTrackedEntityInstance( entityInstance );
        RelationshipItem relationshipItemTo = new RelationshipItem();
        relationshipItemTo.setEvent( event );

        Relationship relationshipA = new Relationship();
        relationshipA.setRelationshipType( relationshipType );
        relationshipA.setFrom( relationshipItemFrom );
        relationshipA.setTo( relationshipItemTo );
        relationshipA.setKey( RelationshipUtils.generateRelationshipKey( relationshipA ) );
        relationshipA.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( relationshipA ) );

        relationshipService.addRelationship( relationshipA );
        return relationshipA;
    }

    private Relationship addTeiToProgramInstanceRelationship( TrackedEntityInstance entityInstance,
        ProgramInstance programInstance )
    {
        RelationshipItem relationshipItemFrom = new RelationshipItem();
        relationshipItemFrom.setTrackedEntityInstance( entityInstance );
        RelationshipItem relationshipItemTo = new RelationshipItem();
        relationshipItemTo.setProgramInstance( programInstance );

        Relationship relationshipA = new Relationship();
        relationshipA.setRelationshipType( relationshipType );
        relationshipA.setFrom( relationshipItemFrom );
        relationshipA.setTo( relationshipItemTo );
        relationshipA.setKey( RelationshipUtils.generateRelationshipKey( relationshipA ) );
        relationshipA.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( relationshipA ) );

        relationshipService.addRelationship( relationshipA );
        return relationshipA;
    }

    private Event addProgramStageInstance( ProgramInstance programInstance, ProgramStage programStageA )
    {
        Event event = new Event();
        event.setOrganisationUnit( organisationUnit );
        event.setProgramStage( programStageA );
        event.setProgramInstance( programInstance );
        event.setAutoFields();

        programStageInstanceService.addProgramStageInstance( event );
        return event;
    }

    private ProgramStage addProgramStage( Program programA )
    {
        ProgramStage programStageA = createProgramStage( 'S', programA );
        programStageA.setProgram( programA );
        programStageService.saveProgramStage( programStageA );
        programA.getProgramStages().add( programStageA );
        return programStageA;
    }

    private ProgramInstance addProgramInstance( Program programA )
    {
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setProgram( programA );
        programInstance.setAutoFields();
        programInstance.setEnrollmentDate( new Date() );
        programInstance.setIncidentDate( new Date() );
        programInstance.setStatus( ProgramStatus.ACTIVE );
        programInstanceService.addProgramInstance( programInstance );
        return programInstance;
    }

    private Program addProgram()
    {
        Program programA = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( programA );
        return programA;
    }
}