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
package org.hisp.dhis.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.audit.Audit;
import org.hisp.dhis.audit.AuditQuery;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditService;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.commons.util.RelationshipUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAudit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Enrico Colasante
 */
class MaintenanceServiceTest extends IntegrationTestBase
{
    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramMessageService programMessageService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private TrackedEntityDataValueAuditService trackedEntityDataValueAuditService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private EventService eventService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private MaintenanceService maintenanceService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private AuditService auditService;

    private Date incidenDate;

    private Date enrollmentDate;

    private Program program;

    private ProgramStage stageA;

    private ProgramStage stageB;

    private OrganisationUnit organisationUnit;

    private ProgramInstance programInstance;

    private ProgramInstance programInstanceWithTeiAssociation;

    private Event event;

    private Event eventWithTeiAssociation;

    private TrackedEntityType trackedEntityType;

    private TrackedEntityInstance entityInstance;

    private TrackedEntityInstance entityInstanceB;

    private TrackedEntityInstance entityInstanceWithAssociations;

    private RelationshipType relationshipType;

    @Override
    public void setUpTest()
    {
        organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );
        program = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( program );
        stageA = createProgramStage( 'A', program );
        stageA.setSortOrder( 1 );
        programStageService.saveProgramStage( stageA );
        stageB = createProgramStage( 'B', program );
        stageB.setSortOrder( 2 );
        programStageService.saveProgramStage( stageB );
        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        program.setProgramStages( programStages );
        programService.updateProgram( program );
        trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityType );
        entityInstance = createTrackedEntityInstance( organisationUnit );
        entityInstance.setTrackedEntityType( trackedEntityType );
        entityInstanceService.addTrackedEntityInstance( entityInstance );
        entityInstanceB = createTrackedEntityInstance( organisationUnit );
        entityInstanceB.setTrackedEntityType( trackedEntityType );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB );
        entityInstanceWithAssociations = createTrackedEntityInstance( 'T', organisationUnit );
        DateTime testDate1 = DateTime.now();
        testDate1.withTimeAtStartOfDay();
        testDate1 = testDate1.minusDays( 70 );
        incidenDate = testDate1.toDate();
        DateTime testDate2 = DateTime.now();
        testDate2.withTimeAtStartOfDay();
        enrollmentDate = testDate2.toDate();
        programInstance = new ProgramInstance( enrollmentDate, incidenDate, entityInstance, program );
        programInstance.setUid( "UID-A" );
        programInstance.setOrganisationUnit( organisationUnit );
        programInstanceWithTeiAssociation = new ProgramInstance( enrollmentDate, incidenDate,
            entityInstanceWithAssociations, program );
        programInstanceWithTeiAssociation.setUid( "UID-B" );
        programInstanceWithTeiAssociation.setOrganisationUnit( organisationUnit );
        trackedEntityInstanceService.addTrackedEntityInstance( entityInstanceWithAssociations );
        programInstanceService.addProgramInstance( programInstanceWithTeiAssociation );
        programInstanceService.addProgramInstance( programInstance );
        event = new Event( programInstance, stageA );
        event.setUid( "PSUID-B" );
        event.setOrganisationUnit( organisationUnit );
        event.setProgramInstance( programInstance );
        event.setExecutionDate( new Date() );
        eventWithTeiAssociation = new Event( programInstanceWithTeiAssociation, stageA );
        eventWithTeiAssociation.setUid( "PSUID-C" );
        eventWithTeiAssociation.setOrganisationUnit( organisationUnit );
        eventWithTeiAssociation.setProgramInstance( programInstanceWithTeiAssociation );
        eventWithTeiAssociation.setExecutionDate( new Date() );
        eventService.addEvent( eventWithTeiAssociation );
        relationshipType = createPersonToPersonRelationshipType( 'A', program, trackedEntityType, false );
        relationshipTypeService.addRelationshipType( relationshipType );
    }

    @Test
    void testDeleteSoftDeletedTrackedEntityInstanceLinkedToARelationshipItem()
    {
        RelationshipType rType = createRelationshipType( 'A' );
        rType.getFromConstraint().setRelationshipEntity( RelationshipEntity.PROGRAM_INSTANCE );
        rType.getFromConstraint().setProgram( program );
        rType.getToConstraint().setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        rType.getFromConstraint().setTrackedEntityType( entityInstance.getTrackedEntityType() );
        relationshipTypeService.addRelationshipType( rType );
        Relationship r = new Relationship();
        RelationshipItem rItem1 = new RelationshipItem();
        rItem1.setProgramInstance( programInstance );
        RelationshipItem rItem2 = new RelationshipItem();
        rItem2.setTrackedEntityInstance( entityInstance );
        r.setFrom( rItem1 );
        r.setTo( rItem2 );
        r.setRelationshipType( rType );
        r.setKey( RelationshipUtils.generateRelationshipKey( r ) );
        r.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( r ) );
        relationshipService.addRelationship( r );
        assertNotNull( trackedEntityInstanceService.getTrackedEntityInstance( entityInstance.getId() ) );
        assertNotNull( relationshipService.getRelationship( r.getId() ) );
        trackedEntityInstanceService.deleteTrackedEntityInstance( entityInstance );
        assertNull( trackedEntityInstanceService.getTrackedEntityInstance( entityInstance.getId() ) );
        assertNull( relationshipService.getRelationship( r.getId() ) );
        assertTrue(
            trackedEntityInstanceService.trackedEntityInstanceExistsIncludingDeleted( entityInstance.getUid() ) );
        assertTrue( relationshipService.relationshipExistsIncludingDeleted( r.getUid() ) );
        maintenanceService.deleteSoftDeletedTrackedEntityInstances();
        assertFalse(
            trackedEntityInstanceService.trackedEntityInstanceExistsIncludingDeleted( entityInstance.getUid() ) );
        assertFalse( relationshipService.relationshipExistsIncludingDeleted( r.getUid() ) );
    }

    @Test
    void testDeleteSoftDeletedProgramInstanceWithAProgramMessage()
    {
        ProgramMessageRecipients programMessageRecipients = new ProgramMessageRecipients();
        programMessageRecipients.setEmailAddresses( Sets.newHashSet( "testemail" ) );
        programMessageRecipients.setPhoneNumbers( Sets.newHashSet( "testphone" ) );
        programMessageRecipients.setOrganisationUnit( organisationUnit );
        programMessageRecipients.setTrackedEntityInstance( entityInstance );
        ProgramMessage message = ProgramMessage.builder().subject( "subject" ).text( "text" )
            .recipients( programMessageRecipients ).deliveryChannels( Sets.newHashSet( DeliveryChannel.EMAIL ) )
            .programInstance( programInstance ).build();
        long idA = programInstanceService.addProgramInstance( programInstance );
        programMessageService.saveProgramMessage( message );
        assertNotNull( programInstanceService.getProgramInstance( idA ) );
        programInstanceService.deleteProgramInstance( programInstance );
        assertNull( programInstanceService.getProgramInstance( idA ) );
        assertTrue( programInstanceService.programInstanceExistsIncludingDeleted( programInstance.getUid() ) );
        maintenanceService.deleteSoftDeletedProgramInstances();
        assertFalse( programInstanceService.programInstanceExistsIncludingDeleted( programInstance.getUid() ) );
    }

    @Test
    void testDeleteSoftDeletedProgramStageInstanceWithAProgramMessage()
    {
        ProgramMessageRecipients programMessageRecipients = new ProgramMessageRecipients();
        programMessageRecipients.setEmailAddresses( Sets.newHashSet( "testemail" ) );
        programMessageRecipients.setPhoneNumbers( Sets.newHashSet( "testphone" ) );
        programMessageRecipients.setOrganisationUnit( organisationUnit );
        ProgramMessage message = ProgramMessage.builder().subject( "subject" ).text( "text" )
            .recipients( programMessageRecipients ).deliveryChannels( Sets.newHashSet( DeliveryChannel.EMAIL ) )
            .event( event ).build();
        long idA = eventService.addEvent( event );
        programMessageService.saveProgramMessage( message );
        assertNotNull( eventService.getEvent( idA ) );
        eventService.deleteEvent( event );
        assertNull( eventService.getEvent( idA ) );
        assertTrue(
            eventService.eventExistsIncludingDeleted( event.getUid() ) );
        maintenanceService.deleteSoftDeletedEvents();
        assertFalse(
            eventService.eventExistsIncludingDeleted( event.getUid() ) );
    }

    @Test
    void testDeleteSoftDeletedTrackedEntityInstanceAProgramMessage()
    {
        ProgramMessageRecipients programMessageRecipients = new ProgramMessageRecipients();
        programMessageRecipients.setEmailAddresses( Sets.newHashSet( "testemail" ) );
        programMessageRecipients.setPhoneNumbers( Sets.newHashSet( "testphone" ) );
        programMessageRecipients.setOrganisationUnit( organisationUnit );
        programMessageRecipients.setTrackedEntityInstance( entityInstanceB );
        ProgramMessage message = ProgramMessage.builder().subject( "subject" ).text( "text" )
            .recipients( programMessageRecipients ).deliveryChannels( Sets.newHashSet( DeliveryChannel.EMAIL ) )
            .build();
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceB );
        programMessageService.saveProgramMessage( message );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );
        entityInstanceService.deleteTrackedEntityInstance( entityInstanceB );
        assertNull( entityInstanceService.getTrackedEntityInstance( idA ) );
        assertTrue( entityInstanceService.trackedEntityInstanceExistsIncludingDeleted( entityInstanceB.getUid() ) );
        maintenanceService.deleteSoftDeletedTrackedEntityInstances();
        assertFalse( entityInstanceService.trackedEntityInstanceExistsIncludingDeleted( entityInstanceB.getUid() ) );
    }

    @Test
    void testDeleteSoftDeletedProgramInstanceLinkedToATrackedEntityDataValueAudit()
    {
        DataElement dataElement = createDataElement( 'A' );
        dataElementService.addDataElement( dataElement );
        Event eventA = new Event( programInstance,
            program.getProgramStageByStage( 1 ) );
        eventA.setDueDate( enrollmentDate );
        eventA.setUid( "UID-A" );
        eventService.addEvent( eventA );
        TrackedEntityDataValueAudit trackedEntityDataValueAudit = new TrackedEntityDataValueAudit( dataElement,
            eventA, "value", "modifiedBy", false, org.hisp.dhis.common.AuditType.UPDATE );
        trackedEntityDataValueAuditService.addTrackedEntityDataValueAudit( trackedEntityDataValueAudit );
        long idA = programInstanceService.addProgramInstance( programInstance );
        assertNotNull( programInstanceService.getProgramInstance( idA ) );
        programInstanceService.deleteProgramInstance( programInstance );
        assertNull( programInstanceService.getProgramInstance( idA ) );
        assertTrue( programInstanceService.programInstanceExistsIncludingDeleted( programInstance.getUid() ) );
        maintenanceService.deleteSoftDeletedProgramInstances();
        assertFalse( programInstanceService.programInstanceExistsIncludingDeleted( programInstance.getUid() ) );
    }

    @Test
    void testDeleteSoftDeletedProgramStageInstanceLinkedToARelationshipItem()
    {
        RelationshipType rType = createRelationshipType( 'A' );
        rType.getFromConstraint().setRelationshipEntity( RelationshipEntity.PROGRAM_STAGE_INSTANCE );
        rType.getFromConstraint().setProgram( program );
        rType.getFromConstraint().setProgramStage( program.getProgramStageByStage( 1 ) );
        rType.getToConstraint().setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        rType.getFromConstraint().setTrackedEntityType( entityInstance.getTrackedEntityType() );
        relationshipTypeService.addRelationshipType( rType );
        Event eventA = new Event( programInstance,
            program.getProgramStageByStage( 1 ) );
        eventA.setDueDate( enrollmentDate );
        eventA.setUid( "UID-A" );
        long idA = eventService.addEvent( eventA );
        Relationship r = new Relationship();
        RelationshipItem rItem1 = new RelationshipItem();
        rItem1.setEvent( eventA );
        RelationshipItem rItem2 = new RelationshipItem();
        rItem2.setTrackedEntityInstance( entityInstance );
        r.setFrom( rItem1 );
        r.setTo( rItem2 );
        r.setRelationshipType( rType );
        r.setKey( RelationshipUtils.generateRelationshipKey( r ) );
        r.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( r ) );
        relationshipService.addRelationship( r );
        assertNotNull( eventService.getEvent( idA ) );
        assertNotNull( relationshipService.getRelationship( r.getId() ) );
        eventService.deleteEvent( eventA );
        assertNull( eventService.getEvent( idA ) );
        assertNull( relationshipService.getRelationship( r.getId() ) );
        assertTrue(
            eventService.eventExistsIncludingDeleted( eventA.getUid() ) );
        assertTrue( relationshipService.relationshipExistsIncludingDeleted( r.getUid() ) );
        maintenanceService.deleteSoftDeletedEvents();
        assertFalse(
            eventService.eventExistsIncludingDeleted( eventA.getUid() ) );
        assertFalse( relationshipService.relationshipExistsIncludingDeleted( r.getUid() ) );
    }

    @Test
    void testDeleteSoftDeletedProgramInstanceLinkedToARelationshipItem()
    {
        RelationshipType rType = createRelationshipType( 'A' );
        rType.getFromConstraint().setRelationshipEntity( RelationshipEntity.PROGRAM_INSTANCE );
        rType.getFromConstraint().setProgram( program );
        rType.getToConstraint().setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        rType.getFromConstraint().setTrackedEntityType( entityInstance.getTrackedEntityType() );
        relationshipTypeService.addRelationshipType( rType );
        Relationship r = new Relationship();
        RelationshipItem rItem1 = new RelationshipItem();
        rItem1.setProgramInstance( programInstance );
        RelationshipItem rItem2 = new RelationshipItem();
        rItem2.setTrackedEntityInstance( entityInstance );
        r.setFrom( rItem1 );
        r.setTo( rItem2 );
        r.setRelationshipType( rType );
        r.setKey( RelationshipUtils.generateRelationshipKey( r ) );
        r.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( r ) );
        relationshipService.addRelationship( r );
        assertNotNull( programInstanceService.getProgramInstance( programInstance.getId() ) );
        assertNotNull( relationshipService.getRelationship( r.getId() ) );
        programInstanceService.deleteProgramInstance( programInstance );
        assertNull( programInstanceService.getProgramInstance( programInstance.getId() ) );
        assertNull( relationshipService.getRelationship( r.getId() ) );
        assertTrue( programInstanceService.programInstanceExistsIncludingDeleted( programInstance.getUid() ) );
        assertTrue( relationshipService.relationshipExistsIncludingDeleted( r.getUid() ) );
        maintenanceService.deleteSoftDeletedProgramInstances();
        assertFalse( programInstanceService.programInstanceExistsIncludingDeleted( programInstance.getUid() ) );
        assertFalse( relationshipService.relationshipExistsIncludingDeleted( r.getUid() ) );
    }

    @Test
    @Disabled( "until we can inject dhis.conf property overrides" )
    void testAuditEntryForDeletionOfSoftDeletedTrackedEntityInstance()
    {
        trackedEntityInstanceService.deleteTrackedEntityInstance( entityInstanceWithAssociations );
        assertNull( trackedEntityInstanceService.getTrackedEntityInstance( entityInstanceWithAssociations.getId() ) );
        assertTrue( trackedEntityInstanceService
            .trackedEntityInstanceExistsIncludingDeleted( entityInstanceWithAssociations.getUid() ) );
        maintenanceService.deleteSoftDeletedTrackedEntityInstances();
        List<Audit> audits = auditService
            .getAudits( AuditQuery.builder().auditType( Sets.newHashSet( AuditType.DELETE ) )
                .auditScope( Sets.newHashSet( AuditScope.TRACKER ) ).build() );
        assertFalse( audits.isEmpty() );
        assertEquals( 1,
            audits.stream().filter( a -> a.getKlass().equals( "org.hisp.dhis.program.ProgramInstance" ) ).count() );
        assertEquals( 1, audits.stream()
            .filter( a -> a.getKlass().equals( "org.hisp.dhis.program.Event" ) ).count() );
        assertEquals( 1, audits.stream()
            .filter( a -> a.getKlass().equals( "org.hisp.dhis.trackedentity.TrackedEntityInstance" ) ).count() );
        audits.forEach( a -> assertSame( a.getAuditType(), AuditType.DELETE ) );
    }

    @Test
    void testDeleteSoftDeletedRelationship()
    {
        Relationship relationship = createTeiToTeiRelationship( entityInstance, entityInstanceB, relationshipType );
        relationshipService.addRelationship( relationship );
        assertNotNull( relationshipService.getRelationship( relationship.getUid() ) );

        relationshipService.deleteRelationship( relationship );
        assertNull( relationshipService.getRelationship( relationship.getUid() ) );
        assertNotNull( relationshipService.getRelationshipIncludeDeleted( relationship.getUid() ) );

        maintenanceService.deleteSoftDeletedRelationships();
        assertNull( relationshipService.getRelationshipIncludeDeleted( relationship.getUid() ) );
    }
}
