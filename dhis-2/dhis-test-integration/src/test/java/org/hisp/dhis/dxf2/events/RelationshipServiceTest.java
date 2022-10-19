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
package org.hisp.dhis.dxf2.events;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.util.RelationshipUtils;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.relationship.RelationshipService;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.events.trackedentity.RelationshipItem;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RelationshipServiceTest extends TransactionalIntegrationTest
{
    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private IdentifiableObjectManager manager;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance teiA;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance teiB;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance teiC;

    private ProgramInstance programInstanceA;

    private ProgramInstance programInstanceB;

    private ProgramStageInstance programStageInstanceA;

    private ProgramStageInstance programStageInstanceB;

    private final RelationshipType relationshipTypeTeiToTei = createRelationshipType( 'A' );

    private final RelationshipType relationshipTypeTeiToPi = createRelationshipType( 'B' );

    private final RelationshipType relationshipTypeTeiToPsi = createRelationshipType( 'C' );

    @Override
    protected void setUpTest()
        throws Exception
    {
        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        manager.save( trackedEntityType );

        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        manager.save( organisationUnit );

        teiA = createTrackedEntityInstance( organisationUnit );
        teiB = createTrackedEntityInstance( organisationUnit );
        teiC = createTrackedEntityInstance( organisationUnit );

        teiA.setTrackedEntityType( trackedEntityType );
        teiB.setTrackedEntityType( trackedEntityType );
        teiC.setTrackedEntityType( trackedEntityType );

        manager.save( teiA );
        manager.save( teiB );
        manager.save( teiC );

        Program program = createProgram( 'A', new HashSet<>(), organisationUnit );
        program.setProgramType( ProgramType.WITH_REGISTRATION );
        ProgramStage programStage = createProgramStage( '1', program );
        program.setProgramStages(
            Stream.of( programStage ).collect( Collectors.toCollection( HashSet::new ) ) );

        manager.save( program );
        manager.save( programStage );

        programInstanceA = programInstanceService.enrollTrackedEntityInstance( teiA, program, new Date(), new Date(),
            organisationUnit );

        programInstanceB = programInstanceService.enrollTrackedEntityInstance( teiB, program, new Date(), new Date(),
            organisationUnit );

        programStageInstanceA = new ProgramStageInstance();
        programStageInstanceA.setProgramInstance( programInstanceA );
        programStageInstanceA.setProgramStage( programStage );
        programStageInstanceA.setOrganisationUnit( organisationUnit );
        manager.save( programStageInstanceA );

        programStageInstanceB = new ProgramStageInstance();
        programStageInstanceB.setProgramInstance( programInstanceB );
        programStageInstanceB.setProgramStage( programStage );
        programStageInstanceB.setOrganisationUnit( organisationUnit );
        manager.save( programStageInstanceB );

        relationshipTypeTeiToTei.getFromConstraint()
            .setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipTypeTeiToTei.getFromConstraint().setTrackedEntityType( trackedEntityType );
        relationshipTypeTeiToTei.getToConstraint()
            .setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipTypeTeiToTei.getToConstraint().setTrackedEntityType( trackedEntityType );

        relationshipTypeTeiToPi.getFromConstraint()
            .setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipTypeTeiToPi.getFromConstraint().setTrackedEntityType( trackedEntityType );
        relationshipTypeTeiToPi.getToConstraint()
            .setRelationshipEntity( RelationshipEntity.PROGRAM_INSTANCE );
        relationshipTypeTeiToPi.getToConstraint().setProgram( program );

        relationshipTypeTeiToPsi.getFromConstraint()
            .setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipTypeTeiToPsi.getFromConstraint().setTrackedEntityType( trackedEntityType );
        relationshipTypeTeiToPsi.getToConstraint()
            .setRelationshipEntity( RelationshipEntity.PROGRAM_STAGE_INSTANCE );
        relationshipTypeTeiToPsi.getToConstraint().setProgramStage( programStage );

        manager.save( relationshipTypeTeiToTei );
        manager.save( relationshipTypeTeiToPi );
        manager.save( relationshipTypeTeiToPsi );
    }

    @Test
    void shouldAddTeiToTeiRelationship()
    {
        Relationship relationshipPayload = new Relationship();
        relationshipPayload.setRelationshipType( relationshipTypeTeiToTei.getUid() );

        RelationshipItem from = teiFrom();

        RelationshipItem to = new RelationshipItem();
        TrackedEntityInstance trackedEntityInstanceTo = new TrackedEntityInstance();
        trackedEntityInstanceTo.setTrackedEntityInstance( teiB.getUid() );
        to.setTrackedEntityInstance( trackedEntityInstanceTo );

        relationshipPayload.setFrom( from );
        relationshipPayload.setTo( to );

        ImportSummary importSummary = relationshipService.addRelationship( relationshipPayload, new ImportOptions() );

        Relationship relationshipDb = relationshipService.getRelationshipByUid( importSummary.getReference() );

        assertAll( () -> assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() ),
            () -> assertEquals( 1, importSummary.getImportCount().getImported() ),
            () -> assertEquals( relationshipDb.getFrom(), from ),
            () -> assertEquals( relationshipDb.getTo(), to ) );
    }

    @Test
    void shouldUpdateTeiToTeiRelationship()
    {
        org.hisp.dhis.relationship.Relationship relationship = relationship( teiA, teiB, null, null );

        Relationship relationshipPayload = new Relationship();
        relationshipPayload.setRelationship( relationship.getUid() );
        relationshipPayload.setRelationshipType( relationship.getRelationshipType().getUid() );

        RelationshipItem from = teiFrom();

        RelationshipItem to = new RelationshipItem();
        TrackedEntityInstance trackedEntityInstanceTo = new TrackedEntityInstance();
        trackedEntityInstanceTo.setTrackedEntityInstance( teiC.getUid() );
        to.setTrackedEntityInstance( trackedEntityInstanceTo );

        relationshipPayload.setFrom( from );
        relationshipPayload.setTo( to );

        ImportSummary importSummary = relationshipService.updateRelationship( relationshipPayload,
            new ImportOptions() );

        Relationship relationshipDb = relationshipService.getRelationshipByUid( importSummary.getReference() );

        assertAll( () -> assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() ),
            () -> assertEquals( 1, importSummary.getImportCount().getUpdated() ),
            () -> assertEquals( relationship.getUid(), relationshipDb.getRelationship() ),
            () -> assertEquals( relationshipDb.getFrom(), from ),
            () -> assertEquals( relationshipDb.getTo(), to ) );
    }

    @Test
    void shouldAddTeiToPiRelationship()
    {
        Relationship relationship = new Relationship();
        relationship.setRelationshipType( relationshipTypeTeiToPi.getUid() );

        RelationshipItem from = teiFrom();

        RelationshipItem to = new RelationshipItem();
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( programInstanceA.getUid() );
        to.setEnrollment( enrollment );

        relationship.setFrom( from );
        relationship.setTo( to );

        ImportSummary importSummary = relationshipService.addRelationship( relationship, new ImportOptions() );

        Relationship relationshipDb = relationshipService.getRelationshipByUid( importSummary.getReference() );

        assertAll( () -> assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() ),
            () -> assertEquals( 1, importSummary.getImportCount().getImported() ),
            () -> assertEquals( relationshipDb.getFrom(), from ),
            () -> assertEquals( relationshipDb.getTo(), to ) );
    }

    @Test
    void shouldUpdateTeiToPiRelationship()
    {
        org.hisp.dhis.relationship.Relationship relationship = relationship( teiA, null, programInstanceA, null );

        Relationship relationshipPayload = new Relationship();
        relationshipPayload.setRelationship( relationship.getUid() );
        relationshipPayload.setRelationshipType( relationship.getRelationshipType().getUid() );

        RelationshipItem from = teiFrom();

        RelationshipItem to = new RelationshipItem();
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( programInstanceB.getUid() );
        to.setEnrollment( enrollment );

        relationshipPayload.setFrom( from );
        relationshipPayload.setTo( to );

        ImportSummary importSummary = relationshipService.updateRelationship( relationshipPayload,
            new ImportOptions() );

        Relationship relationshipDb = relationshipService.getRelationshipByUid( importSummary.getReference() );

        assertAll( () -> assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() ),
            () -> assertEquals( 1, importSummary.getImportCount().getUpdated() ),
            () -> assertEquals( relationship.getUid(), relationshipDb.getRelationship() ),
            () -> assertEquals( relationshipDb.getFrom(), from ),
            () -> assertEquals( relationshipDb.getTo(), to ) );
    }

    @Test
    void shouldAddTeiToPsiRelationship()
    {
        Relationship relationshipPayload = new Relationship();
        relationshipPayload.setRelationshipType( relationshipTypeTeiToPsi.getUid() );

        RelationshipItem from = teiFrom();

        RelationshipItem to = new RelationshipItem();
        Event event = new Event();
        event.setEvent( programStageInstanceA.getUid() );
        to.setEvent( event );

        relationshipPayload.setFrom( from );
        relationshipPayload.setTo( to );

        ImportSummary importSummary = relationshipService.addRelationship( relationshipPayload, new ImportOptions() );

        Relationship relationshipDb = relationshipService.getRelationshipByUid( importSummary.getReference() );

        assertAll( () -> assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() ),
            () -> assertEquals( 1, importSummary.getImportCount().getImported() ),
            () -> assertEquals( relationshipDb.getFrom(), from ),
            () -> assertEquals( relationshipDb.getTo(), to ) );
    }

    @Test
    void shouldUpdateTeiToPsiRelationship()
    {
        org.hisp.dhis.relationship.Relationship relationship = relationship( teiA, null, null, programStageInstanceA );

        Relationship relationshipPayload = new Relationship();
        relationshipPayload.setRelationship( relationship.getUid() );
        relationshipPayload.setRelationshipType( relationship.getRelationshipType().getUid() );

        RelationshipItem from = teiFrom();

        RelationshipItem to = new RelationshipItem();
        Event event = new Event();
        event.setEvent( programStageInstanceB.getUid() );
        to.setEvent( event );

        relationshipPayload.setFrom( from );
        relationshipPayload.setTo( to );

        ImportSummary importSummary = relationshipService.updateRelationship( relationshipPayload,
            new ImportOptions() );

        Relationship relationshipDb = relationshipService.getRelationshipByUid( importSummary.getReference() );

        assertAll( () -> assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() ),
            () -> assertEquals( 1, importSummary.getImportCount().getUpdated() ),
            () -> assertEquals( relationship.getUid(), relationshipDb.getRelationship() ),
            () -> assertEquals( relationshipDb.getFrom(), from ),
            () -> assertEquals( relationshipDb.getTo(), to ) );
    }

    private RelationshipItem teiFrom()
    {
        RelationshipItem from = new RelationshipItem();
        TrackedEntityInstance trackedEntityInstanceFrom = new TrackedEntityInstance();
        trackedEntityInstanceFrom.setTrackedEntityInstance( teiA.getUid() );
        from.setTrackedEntityInstance( trackedEntityInstanceFrom );
        return from;
    }

    private org.hisp.dhis.relationship.Relationship relationship(
        org.hisp.dhis.trackedentity.TrackedEntityInstance teiFrom,
        org.hisp.dhis.trackedentity.TrackedEntityInstance teiTo, ProgramInstance piTo, ProgramStageInstance psiTo )
    {
        org.hisp.dhis.relationship.Relationship relationship = new org.hisp.dhis.relationship.Relationship();

        org.hisp.dhis.relationship.RelationshipItem from = new org.hisp.dhis.relationship.RelationshipItem();
        from.setTrackedEntityInstance( teiFrom );

        org.hisp.dhis.relationship.RelationshipItem to = new org.hisp.dhis.relationship.RelationshipItem();

        if ( null != teiTo )
        {
            to.setTrackedEntityInstance( teiTo );
            relationship.setRelationshipType( relationshipTypeTeiToTei );
        }
        else if ( null != piTo )
        {
            to.setProgramInstance( piTo );
            relationship.setRelationshipType( relationshipTypeTeiToPi );
        }
        else
        {
            to.setProgramStageInstance( psiTo );
            relationship.setRelationshipType( relationshipTypeTeiToPsi );
        }

        relationship.setFrom( from );
        relationship.setTo( to );

        relationship.setKey( RelationshipUtils.generateRelationshipKey( relationship ) );
        relationship.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( relationship ) );

        manager.save( relationship );

        return relationship;
    }
}
