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

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.Lists;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.events.trackedentity.RelationshipItem;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.*;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Enrico Colasante
 */
class HandleRelationshipsTrackedEntityInstanceServiceTest extends DhisSpringTest {

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private TrackedEntityInstanceService trackedEntityInstanceService;

  @Autowired private RelationshipTypeService relationshipTypeService;

  @Autowired private ProgramInstanceService programInstanceService;

  @Autowired private ProgramStageInstanceService programStageInstanceService;

  @Autowired private IdentifiableObjectManager manager;

  private org.hisp.dhis.trackedentity.TrackedEntityInstance trackedEntityInstanceA;

  private org.hisp.dhis.trackedentity.TrackedEntityInstance trackedEntityInstanceB;

  private OrganisationUnit organisationUnitA;

  private Program programA;

  private ProgramStage programStageA1;

  private ProgramStage programStageA2;

  private TrackedEntityType trackedEntityType;

  private ProgramStageInstance programStageInstanceA;

  @Override
  protected void setUpTest() throws Exception {
    organisationUnitA = createOrganisationUnit('A');
    trackedEntityType = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    trackedEntityInstanceA = createTrackedEntityInstance(organisationUnitA);
    trackedEntityInstanceA.setTrackedEntityType(trackedEntityType);
    trackedEntityInstanceB = createTrackedEntityInstance(organisationUnitA);
    trackedEntityInstanceB.setTrackedEntityType(trackedEntityType);
    programA = createProgram('A', new HashSet<>(), organisationUnitA);
    programA.setProgramType(ProgramType.WITH_REGISTRATION);
    programStageA1 = createProgramStage('1', programA);
    programStageA2 = createProgramStage('2', programA);
    programA.setProgramStages(
        Stream.of(programStageA1, programStageA2).collect(Collectors.toCollection(HashSet::new)));
    manager.save(organisationUnitA);
    manager.save(trackedEntityInstanceA);
    manager.save(trackedEntityInstanceB);
    manager.save(programA);
    manager.save(programStageA1);
    manager.save(programStageA2);
    ProgramInstance programInstanceA =
        programInstanceService.enrollTrackedEntityInstance(
            trackedEntityInstanceA, programA, null, null, organisationUnitA);
    programStageInstanceA = new ProgramStageInstance(programInstanceA, programStageA1);
    programStageInstanceA.setDueDate(null);
    programStageInstanceA.setUid("UID-A");
    CategoryCombo categoryComboA = createCategoryCombo('A');
    CategoryOptionCombo categoryOptionComboA = createCategoryOptionCombo('A');
    categoryOptionComboA.setCategoryCombo(categoryComboA);
    manager.save(categoryComboA);
    manager.save(categoryOptionComboA);
    programStageInstanceA.setAttributeOptionCombo(categoryOptionComboA);
    programStageInstanceService.addProgramStageInstance(programStageInstanceA);
  }

  @Test
  void testUpdateTeiWithUniDirectionalRelationshipTeiToTei() {
    TrackedEntityInstance trackedEntityInstanceFrom =
        trackedEntityInstanceService.getTrackedEntityInstance(this.trackedEntityInstanceA.getUid());
    TrackedEntityInstance trackedEntityInstanceTo =
        trackedEntityInstanceService.getTrackedEntityInstance(this.trackedEntityInstanceB.getUid());
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
            + "': TrackedEntityInstance '"
            + trackedEntityInstanceTo.getTrackedEntityInstance()
            + "' is not the owner of the relationship",
        importSummaryTo.getRelationships().getImportSummaries().get(0).getDescription());
  }

  @Test
  void testUpdateTeiWithBidirectionalRelationshipTeiToTei() {
    TrackedEntityInstance trackedEntityInstanceFrom =
        trackedEntityInstanceService.getTrackedEntityInstance(this.trackedEntityInstanceA.getUid());
    TrackedEntityInstance trackedEntityInstanceTo =
        trackedEntityInstanceService.getTrackedEntityInstance(this.trackedEntityInstanceB.getUid());
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
        trackedEntityInstanceService.getTrackedEntityInstance(this.trackedEntityInstanceA.getUid());
    RelationshipType malariaCaseLinkedToPersonRelationshipType =
        createMalariaCaseLinkedToPersonRelationshipType('A', programA, trackedEntityType);
    malariaCaseLinkedToPersonRelationshipType.setBidirectional(false);
    relationshipTypeService.addRelationshipType(malariaCaseLinkedToPersonRelationshipType);
    Relationship relationship =
        createEventToTeiRelationship(
            'A',
            malariaCaseLinkedToPersonRelationshipType,
            trackedEntityInstance,
            programStageInstanceA);
    trackedEntityInstance.setRelationships(Lists.newArrayList(relationship));
    ImportSummary importSummary =
        trackedEntityInstanceService.updateTrackedEntityInstance(
            trackedEntityInstance, null, null, true);
    assertEquals(ImportStatus.SUCCESS, importSummary.getStatus());
    assertEquals(ImportStatus.ERROR, importSummary.getRelationships().getStatus());
    assertEquals(
        "Can't update relationship '"
            + relationship.getRelationship()
            + "': TrackedEntityInstance '"
            + trackedEntityInstance.getTrackedEntityInstance()
            + "' is not the owner of the relationship",
        importSummary.getRelationships().getImportSummaries().get(0).getDescription());
  }

  @Test
  void testUpdateTeiWithBidirectionalRelationshipEventToTei() {
    TrackedEntityInstance trackedEntityInstance =
        trackedEntityInstanceService.getTrackedEntityInstance(this.trackedEntityInstanceA.getUid());
    RelationshipType relationshipType =
        createMalariaCaseLinkedToPersonRelationshipType('A', programA, trackedEntityType);
    relationshipTypeService.addRelationshipType(relationshipType);
    Relationship relationship =
        createEventToTeiRelationship(
            'A', relationshipType, trackedEntityInstance, programStageInstanceA);
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
      ProgramStageInstance programStageInstance) {
    RelationshipItem relationshipItemEvent = new RelationshipItem();
    Event event = new Event();
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
