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
package org.hisp.dhis.dxf2.deprecated.tracker;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.Lists;
import java.util.*;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.Relationship;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.RelationshipItem;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.*;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Enrico Colasante
 */
class HandleRelationshipsTrackedEntityServiceTest extends SingleSetupIntegrationTestBase {

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private TrackedEntityInstanceService trackedEntityInstanceService;

  @Autowired private RelationshipTypeService relationshipTypeService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private EventService eventService;

  @Autowired private IdentifiableObjectManager manager;

  private TrackedEntity trackedEntityA;

  private TrackedEntity trackedEntityB;

  private OrganisationUnit organisationUnitA;

  private Program programA;

  private ProgramStage programStageA1;

  private ProgramStage programStageA2;

  private TrackedEntityType trackedEntityType;

  private Event eventA;

  @Override
  protected void setUpTest() throws Exception {
    organisationUnitA = createOrganisationUnit('A');
    trackedEntityType = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    trackedEntityA = createTrackedEntity(organisationUnitA);
    trackedEntityA.setTrackedEntityType(trackedEntityType);
    trackedEntityB = createTrackedEntity(organisationUnitA);
    trackedEntityB.setTrackedEntityType(trackedEntityType);
    programA = createProgram('A', new HashSet<>(), organisationUnitA);
    programA.setProgramType(ProgramType.WITH_REGISTRATION);
    manager.save(organisationUnitA);
    manager.save(trackedEntityA);
    manager.save(trackedEntityB);
    manager.save(programA);
    programStageA1 = createProgramStage('1', programA);
    programStageA2 = createProgramStage('2', programA);
    manager.save(programStageA1);
    manager.save(programStageA2);
    programA.getProgramStages().addAll(Set.of(programStageA1, programStageA2));
    manager.update(programA);
    Enrollment enrollmentA =
        enrollmentService.enrollTrackedEntity(
            trackedEntityA, programA, null, null, organisationUnitA);
    eventA = new Event(enrollmentA, programStageA1);
    eventA.setDueDate(null);
    eventA.setUid("UID-A");
    CategoryCombo categoryComboA = createCategoryCombo('A');
    CategoryOptionCombo categoryOptionComboA = createCategoryOptionCombo('A');
    categoryOptionComboA.setCategoryCombo(categoryComboA);
    manager.save(categoryComboA);
    manager.save(categoryOptionComboA);
    eventA.setAttributeOptionCombo(categoryOptionComboA);
    eventService.addEvent(eventA);
  }

  @Test
  void testUpdateTeiWithUniDirectionalRelationshipTeiToTei() {
    TrackedEntityInstance trackedEntityInstanceFrom =
        trackedEntityInstanceService.getTrackedEntityInstance(this.trackedEntityA.getUid());
    TrackedEntityInstance trackedEntityInstanceTo =
        trackedEntityInstanceService.getTrackedEntityInstance(this.trackedEntityB.getUid());
    RelationshipType personToPersonRelationshipType =
        createPersonToPersonRelationshipType('A', programA, trackedEntityType, false);
    relationshipTypeService.addRelationshipType(personToPersonRelationshipType);
    Relationship relationship =
        createTeiToTeiRelationship(
            'A',
            personToPersonRelationshipType,
            trackedEntityInstanceFrom,
            trackedEntityInstanceTo);
    trackedEntityInstanceFrom.setRelationships(Lists.newArrayList(relationship));
    ImportSummary importSummaryFrom =
        trackedEntityInstanceService.updateTrackedEntityInstance(
            trackedEntityInstanceFrom, null, null, true);
    assertEquals(ImportStatus.SUCCESS, importSummaryFrom.getStatus());
    assertEquals(ImportStatus.SUCCESS, importSummaryFrom.getRelationships().getStatus());
    trackedEntityInstanceTo.setRelationships(Lists.newArrayList(relationship));
    ImportSummary importSummaryTo =
        trackedEntityInstanceService.updateTrackedEntityInstance(
            trackedEntityInstanceTo, null, null, true);
    assertEquals(ImportStatus.SUCCESS, importSummaryTo.getStatus());
    assertEquals(ImportStatus.ERROR, importSummaryTo.getRelationships().getStatus());
    assertEquals(
        "Can't update relationship '"
            + relationship.getRelationship()
            + "': TrackedEntity '"
            + trackedEntityInstanceTo.getTrackedEntityInstance()
            + "' is not the owner of the relationship",
        importSummaryTo.getRelationships().getImportSummaries().get(0).getDescription());
  }

  @Test
  void testUpdateTeiWithBidirectionalRelationshipTeiToTei() {
    TrackedEntityInstance trackedEntityInstanceFrom =
        trackedEntityInstanceService.getTrackedEntityInstance(this.trackedEntityA.getUid());
    TrackedEntityInstance trackedEntityInstanceTo =
        trackedEntityInstanceService.getTrackedEntityInstance(this.trackedEntityB.getUid());
    RelationshipType personToPersonRelationshipType =
        createPersonToPersonRelationshipType('A', programA, trackedEntityType, true);
    relationshipTypeService.addRelationshipType(personToPersonRelationshipType);
    Relationship relationship =
        createTeiToTeiRelationship(
            'A',
            personToPersonRelationshipType,
            trackedEntityInstanceFrom,
            trackedEntityInstanceTo);
    trackedEntityInstanceFrom.setRelationships(Lists.newArrayList(relationship));
    ImportSummary importSummaryFrom =
        trackedEntityInstanceService.updateTrackedEntityInstance(
            trackedEntityInstanceFrom, null, null, true);
    assertEquals(ImportStatus.SUCCESS, importSummaryFrom.getStatus());
    assertEquals(ImportStatus.SUCCESS, importSummaryFrom.getRelationships().getStatus());
    trackedEntityInstanceTo.setRelationships(Lists.newArrayList(relationship));
    ImportSummary importSummaryTo =
        trackedEntityInstanceService.updateTrackedEntityInstance(
            trackedEntityInstanceTo, null, null, true);
    assertEquals(ImportStatus.SUCCESS, importSummaryTo.getStatus());
    assertEquals(ImportStatus.SUCCESS, importSummaryTo.getRelationships().getStatus());
  }

  @Test
  void testUpdateTeiWithUniDirectionalRelationshipEventToTei() {
    TrackedEntityInstance trackedEntityInstance =
        trackedEntityInstanceService.getTrackedEntityInstance(this.trackedEntityA.getUid());
    RelationshipType malariaCaseLinkedToPersonRelationshipType =
        createMalariaCaseLinkedToPersonRelationshipType('A', programA, trackedEntityType);
    malariaCaseLinkedToPersonRelationshipType.setBidirectional(false);
    relationshipTypeService.addRelationshipType(malariaCaseLinkedToPersonRelationshipType);
    Relationship relationship =
        createEventToTeiRelationship(
            'A', malariaCaseLinkedToPersonRelationshipType, trackedEntityInstance, eventA);
    trackedEntityInstance.setRelationships(Lists.newArrayList(relationship));
    ImportSummary importSummary =
        trackedEntityInstanceService.updateTrackedEntityInstance(
            trackedEntityInstance, null, null, true);
    assertEquals(ImportStatus.SUCCESS, importSummary.getStatus());
    assertEquals(ImportStatus.ERROR, importSummary.getRelationships().getStatus());
    assertEquals(
        "Can't update relationship '"
            + relationship.getRelationship()
            + "': TrackedEntity '"
            + trackedEntityInstance.getTrackedEntityInstance()
            + "' is not the owner of the relationship",
        importSummary.getRelationships().getImportSummaries().get(0).getDescription());
  }

  @Test
  void testUpdateTeiWithBidirectionalRelationshipEventToTei() {
    TrackedEntityInstance trackedEntityInstance =
        trackedEntityInstanceService.getTrackedEntityInstance(this.trackedEntityA.getUid());
    RelationshipType relationshipType =
        createMalariaCaseLinkedToPersonRelationshipType('A', programA, trackedEntityType);
    relationshipTypeService.addRelationshipType(relationshipType);
    Relationship relationship =
        createEventToTeiRelationship('A', relationshipType, trackedEntityInstance, eventA);
    trackedEntityInstance.setRelationships(Lists.newArrayList(relationship));
    ImportSummary importSummary =
        trackedEntityInstanceService.updateTrackedEntityInstance(
            trackedEntityInstance, null, null, true);
    assertEquals(ImportStatus.SUCCESS, importSummary.getStatus());
    assertEquals(ImportStatus.SUCCESS, importSummary.getRelationships().getStatus());
  }

  private Relationship createEventToTeiRelationship(
      char uniqueCharacter,
      RelationshipType relationshipType,
      TrackedEntityInstance trackedEntityInstance,
      Event programStageInstance) {
    RelationshipItem relationshipItemEvent = new RelationshipItem();
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
        new org.hisp.dhis.dxf2.deprecated.tracker.event.Event();
    event.setEvent(programStageInstance.getUid());
    relationshipItemEvent.setEvent(event);
    RelationshipItem relationshipItemTei = new RelationshipItem();
    relationshipItemTei.setTrackedEntityInstance(trackedEntityInstance);
    Relationship relationship = new Relationship();
    relationship.setFrom(relationshipItemEvent);
    relationship.setTo(relationshipItemTei);
    relationship.setRelationshipType(relationshipType.getUid());
    relationship.setRelationship("UID_" + uniqueCharacter);
    relationship.setRelationshipName("Malaria case linked to person");
    relationship.setBidirectional(relationshipType.isBidirectional());
    return relationship;
  }

  private Relationship createTeiToTeiRelationship(
      char key,
      RelationshipType relationshipType,
      TrackedEntityInstance trackedEntityInstanceA,
      TrackedEntityInstance trackedEntityInstanceB) {
    RelationshipItem relationshipItemTeiA = new RelationshipItem();
    relationshipItemTeiA.setTrackedEntityInstance(trackedEntityInstanceA);
    RelationshipItem relationshipItemTeiB = new RelationshipItem();
    relationshipItemTeiB.setTrackedEntityInstance(trackedEntityInstanceB);
    Relationship relationship = new Relationship();
    relationship.setFrom(relationshipItemTeiA);
    relationship.setTo(relationshipItemTeiB);
    relationship.setRelationshipType(relationshipType.getUid());
    relationship.setRelationship("UID_" + key);
    relationship.setRelationshipName("Person to person");
    relationship.setBidirectional(relationshipType.isBidirectional());
    return relationship;
  }
}
