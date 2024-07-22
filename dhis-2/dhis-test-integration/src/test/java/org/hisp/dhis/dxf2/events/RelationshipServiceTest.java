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

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.util.RelationshipUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.relationship.RelationshipService;
import org.hisp.dhis.dxf2.events.trackedentity.Attribute;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.events.trackedentity.RelationshipItem;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.eventdatavalue.EventDataValue;
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
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackerdataview.TrackerDataView;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RelationshipServiceTest extends TransactionalIntegrationTest {
  @Autowired private ProgramInstanceService programInstanceService;

  @Autowired private RelationshipService relationshipService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private UserService _userService;

  private TrackedEntityAttribute teaA;

  private TrackedEntityAttribute teaB;

  private DataElement dataElementA;

  private DataElement dataElementB;

  private org.hisp.dhis.trackedentity.TrackedEntityInstance teiA;

  private org.hisp.dhis.trackedentity.TrackedEntityInstance teiB;

  private org.hisp.dhis.trackedentity.TrackedEntityInstance teiC;

  private ProgramInstance programInstanceA;

  private ProgramInstance programInstanceB;

  private ProgramStageInstance programStageInstanceA;

  private ProgramStageInstance programStageInstanceB;

  private final RelationshipType relationshipTypeTeiToTei = createRelationshipType('A');

  private final RelationshipType relationshipTypeTeiToPi = createRelationshipType('B');

  private final RelationshipType relationshipTypeTeiToPsi = createRelationshipType('C');

  @Override
  protected void setUpTest() throws Exception {
    userService = _userService;

    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    manager.save(trackedEntityType);

    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    manager.save(organisationUnit);

    teiA = createTrackedEntityInstance(organisationUnit);
    teiB = createTrackedEntityInstance(organisationUnit);
    teiC = createTrackedEntityInstance(organisationUnit);

    teiA.setTrackedEntityType(trackedEntityType);
    teiB.setTrackedEntityType(trackedEntityType);
    teiC.setTrackedEntityType(trackedEntityType);

    manager.save(teiA);
    manager.save(teiB);
    manager.save(teiC);

    teaA = createTrackedEntityAttribute('A');
    manager.save(teaA, false);

    teaB = createTrackedEntityAttribute('B');
    manager.save(teaB, false);

    TrackedEntityTypeAttribute trackedEntityTypeAttribute = new TrackedEntityTypeAttribute();
    trackedEntityTypeAttribute.setTrackedEntityAttribute(teaA);
    trackedEntityTypeAttribute.setTrackedEntityType(trackedEntityType);

    trackedEntityType.setTrackedEntityTypeAttributes(List.of(trackedEntityTypeAttribute));
    manager.save(trackedEntityType);

    Program program = createProgram('A', new HashSet<>(), organisationUnit);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    ProgramStage programStage = createProgramStage('1', program);
    program.setProgramStages(
        Stream.of(programStage).collect(Collectors.toCollection(HashSet::new)));
    program.setProgramAttributes(List.of(createProgramTrackedEntityAttribute(program, teaA)));

    manager.save(program);
    manager.save(programStage);

    programInstanceA =
        programInstanceService.enrollTrackedEntityInstance(
            teiA, program, new Date(), new Date(), organisationUnit);

    programInstanceB =
        programInstanceService.enrollTrackedEntityInstance(
            teiB, program, new Date(), new Date(), organisationUnit);

    programStageInstanceA = new ProgramStageInstance();
    programStageInstanceA.setProgramInstance(programInstanceA);
    programStageInstanceA.setProgramStage(programStage);
    programStageInstanceA.setOrganisationUnit(organisationUnit);
    manager.save(programStageInstanceA);

    programStageInstanceB = new ProgramStageInstance();
    programStageInstanceB.setProgramInstance(programInstanceB);
    programStageInstanceB.setProgramStage(programStage);
    programStageInstanceB.setOrganisationUnit(organisationUnit);
    manager.save(programStageInstanceB);

    dataElementA = createDataElement('a');
    manager.save(dataElementA);

    dataElementB = createDataElement('b');
    manager.save(dataElementB);

    relationshipTypeTeiToTei
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    relationshipTypeTeiToTei.getFromConstraint().setTrackedEntityType(trackedEntityType);
    relationshipTypeTeiToTei
        .getToConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    relationshipTypeTeiToTei.getToConstraint().setTrackedEntityType(trackedEntityType);

    relationshipTypeTeiToPi
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    relationshipTypeTeiToPi.getFromConstraint().setTrackedEntityType(trackedEntityType);
    relationshipTypeTeiToPi
        .getToConstraint()
        .setRelationshipEntity(RelationshipEntity.PROGRAM_INSTANCE);
    relationshipTypeTeiToPi.getToConstraint().setProgram(program);

    relationshipTypeTeiToPsi
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    relationshipTypeTeiToPsi.getFromConstraint().setTrackedEntityType(trackedEntityType);
    relationshipTypeTeiToPsi
        .getToConstraint()
        .setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    relationshipTypeTeiToPsi.getToConstraint().setProgramStage(programStage);

    manager.save(relationshipTypeTeiToTei);
    manager.save(relationshipTypeTeiToPi);
    manager.save(relationshipTypeTeiToPsi);
    createUserAndInjectSecurityContext(true);
  }

  @Test
  void shouldAddTeiToTeiRelationship() {
    teiA.setTrackedEntityAttributeValues(
        Set.of(
            new TrackedEntityAttributeValue(teaA, teiA, "100"),
            new TrackedEntityAttributeValue(teaB, teiA, "100")));
    teiB.setTrackedEntityAttributeValues(Set.of(new TrackedEntityAttributeValue(teaA, teiB, "10")));

    manager.update(teiA);
    manager.update(teiB);

    TrackerDataView trackerDataView = new TrackerDataView();
    trackerDataView.setAttributes(new LinkedHashSet<>(Set.of(teaA.getUid())));

    relationshipTypeTeiToTei.getFromConstraint().setTrackerDataView(trackerDataView);

    relationshipTypeTeiToTei.getToConstraint().setTrackerDataView(trackerDataView);

    manager.update(relationshipTypeTeiToPi);

    Relationship relationshipPayload = new Relationship();
    relationshipPayload.setRelationshipType(relationshipTypeTeiToTei.getUid());

    RelationshipItem from = teiFrom();

    RelationshipItem to = new RelationshipItem();
    TrackedEntityInstance trackedEntityInstanceTo = new TrackedEntityInstance();
    trackedEntityInstanceTo.setTrackedEntityInstance(teiB.getUid());
    to.setTrackedEntityInstance(trackedEntityInstanceTo);

    relationshipPayload.setFrom(from);
    relationshipPayload.setTo(to);

    ImportSummary importSummary =
        relationshipService.addRelationship(relationshipPayload, new ImportOptions());

    Optional<Relationship> relationshipDb =
        relationshipService.findRelationshipByUid(importSummary.getReference());

    assertAll(
        () -> assertEquals(ImportStatus.SUCCESS, importSummary.getStatus()),
        () -> assertEquals(1, importSummary.getImportCount().getImported()),
        () ->
            assertAll(
                () -> {
                  assertTrue(relationshipDb.isPresent());
                  Relationship r = relationshipDb.get();
                  assertEquals(r.getFrom(), from);
                  assertEquals(r.getTo(), to);
                  assertContainsOnly(
                      List.of(attributeFromTea(teaA, "100")),
                      r.getFrom().getTrackedEntityInstance().getAttributes());
                  assertContainsOnly(
                      List.of(attributeFromTea(teaA, "10")),
                      r.getTo().getTrackedEntityInstance().getAttributes());
                }));
  }

  @Test
  void shouldUpdateTeiToTeiRelationship() {
    org.hisp.dhis.relationship.Relationship relationship = relationship(teiA, teiB, null, null);

    Relationship relationshipPayload = new Relationship();
    relationshipPayload.setRelationship(relationship.getUid());
    relationshipPayload.setRelationshipType(relationship.getRelationshipType().getUid());

    RelationshipItem from = teiFrom();

    RelationshipItem to = new RelationshipItem();
    TrackedEntityInstance trackedEntityInstanceTo = new TrackedEntityInstance();
    trackedEntityInstanceTo.setTrackedEntityInstance(teiC.getUid());
    to.setTrackedEntityInstance(trackedEntityInstanceTo);

    relationshipPayload.setFrom(from);
    relationshipPayload.setTo(to);

    ImportSummary importSummary =
        relationshipService.updateRelationship(relationshipPayload, new ImportOptions());

    Optional<Relationship> relationshipDb =
        relationshipService.findRelationshipByUid(importSummary.getReference());

    assertAll(
        () -> assertEquals(ImportStatus.SUCCESS, importSummary.getStatus()),
        () -> assertEquals(1, importSummary.getImportCount().getUpdated()),
        () ->
            assertAll(
                () -> {
                  assertTrue(relationshipDb.isPresent());
                  Relationship r = relationshipDb.get();
                  assertEquals(relationship.getUid(), r.getRelationship());
                  assertEquals(from, r.getFrom());
                  assertEquals(to, r.getTo());
                }));
  }

  @Test
  void shouldAddTeiToPiRelationship() {
    teiA.setTrackedEntityAttributeValues(
        Set.of(new TrackedEntityAttributeValue(teaA, teiA, "100")));
    teiB.setTrackedEntityAttributeValues(
        Set.of(
            new TrackedEntityAttributeValue(teaA, teiB, "10"),
            new TrackedEntityAttributeValue(teaB, teiB, "100")));

    manager.update(teiA);
    manager.update(teiB);

    TrackerDataView trackerDataView = new TrackerDataView();
    trackerDataView.setAttributes(new LinkedHashSet<>(Set.of(teaA.getUid())));

    relationshipTypeTeiToPi.getFromConstraint().setTrackerDataView(trackerDataView);

    relationshipTypeTeiToPi.getToConstraint().setTrackerDataView(trackerDataView);

    manager.update(relationshipTypeTeiToPi);

    Relationship relationship = new Relationship();
    relationship.setRelationshipType(relationshipTypeTeiToPi.getUid());

    RelationshipItem from = teiFrom();

    RelationshipItem to = new RelationshipItem();
    Enrollment enrollment = new Enrollment();
    enrollment.setEnrollment(programInstanceB.getUid());
    to.setEnrollment(enrollment);

    relationship.setFrom(from);
    relationship.setTo(to);

    ImportSummary importSummary =
        relationshipService.addRelationship(relationship, new ImportOptions());

    Optional<Relationship> relationshipDb =
        relationshipService.findRelationshipByUid(importSummary.getReference());

    assertAll(
        () -> assertEquals(ImportStatus.SUCCESS, importSummary.getStatus()),
        () -> assertEquals(1, importSummary.getImportCount().getImported()),
        () ->
            assertAll(
                () -> {
                  assertTrue(relationshipDb.isPresent());
                  Relationship r = relationshipDb.get();
                  assertEquals(from, r.getFrom());
                  assertEquals(to, r.getTo());
                  assertContainsOnly(
                      List.of(attributeFromTea(teaA, "100")),
                      r.getFrom().getTrackedEntityInstance().getAttributes());
                  assertContainsOnly(
                      List.of(attributeFromTea(teaA, "10")),
                      r.getTo().getEnrollment().getAttributes());
                }));
  }

  @Test
  void shouldUpdateTeiToPiRelationship() {
    teiA.setTrackedEntityAttributeValues(
        Set.of(new TrackedEntityAttributeValue(teaA, teiA, "100")));
    teiB.setTrackedEntityAttributeValues(
        Set.of(
            new TrackedEntityAttributeValue(teaA, teiB, "10"),
            new TrackedEntityAttributeValue(teaB, teiB, "100")));

    manager.update(teiA);
    manager.update(teiB);

    TrackerDataView trackerDataView = new TrackerDataView();
    trackerDataView.setAttributes(new LinkedHashSet<>(Set.of(teaA.getUid())));

    relationshipTypeTeiToPi.getFromConstraint().setTrackerDataView(trackerDataView);

    relationshipTypeTeiToPi.getToConstraint().setTrackerDataView(trackerDataView);

    manager.update(relationshipTypeTeiToPi);

    org.hisp.dhis.relationship.Relationship relationship =
        relationship(teiA, null, programInstanceA, null);

    Relationship relationshipPayload = new Relationship();
    relationshipPayload.setRelationship(relationship.getUid());
    relationshipPayload.setRelationshipType(relationship.getRelationshipType().getUid());

    RelationshipItem from = teiFrom();

    RelationshipItem to = new RelationshipItem();
    Enrollment enrollment = new Enrollment();
    enrollment.setEnrollment(programInstanceB.getUid());
    to.setEnrollment(enrollment);

    relationshipPayload.setFrom(from);
    relationshipPayload.setTo(to);

    ImportSummary importSummary =
        relationshipService.updateRelationship(relationshipPayload, new ImportOptions());

    Optional<Relationship> relationshipDb =
        relationshipService.findRelationshipByUid(importSummary.getReference());

    assertAll(
        () -> assertEquals(ImportStatus.SUCCESS, importSummary.getStatus()),
        () -> assertEquals(1, importSummary.getImportCount().getUpdated()),
        () ->
            assertAll(
                () -> {
                  assertTrue(relationshipDb.isPresent());
                  Relationship r = relationshipDb.get();
                  assertEquals(relationship.getUid(), r.getRelationship());
                  assertEquals(from, r.getFrom());
                  assertEquals(to, r.getTo());
                  assertContainsOnly(
                      List.of(attributeFromTea(teaA, "100")),
                      r.getFrom().getTrackedEntityInstance().getAttributes());
                  assertContainsOnly(
                      List.of(attributeFromTea(teaA, "10")),
                      r.getTo().getEnrollment().getAttributes());
                }));
  }

  @Test
  void shouldAddTeiToPsiRelationship() {
    teiA.setTrackedEntityAttributeValues(
        Set.of(new TrackedEntityAttributeValue(teaA, teiA, "100")));

    manager.update(teiA);

    EventDataValue dataValueA = new EventDataValue();
    dataValueA.setValue("10");
    dataValueA.setDataElement(dataElementA.getUid());

    EventDataValue dataValueB = new EventDataValue();
    dataValueB.setValue("100");
    dataValueB.setDataElement(dataElementB.getUid());

    programStageInstanceA.setEventDataValues(Set.of(dataValueA, dataValueB));

    manager.update(programStageInstanceA);

    TrackerDataView trackerDataViewFrom = new TrackerDataView();
    trackerDataViewFrom.setAttributes(new LinkedHashSet<>(Set.of(teaA.getUid())));

    relationshipTypeTeiToPsi.getFromConstraint().setTrackerDataView(trackerDataViewFrom);

    TrackerDataView trackerDataViewTo = new TrackerDataView();
    trackerDataViewTo.setDataElements(new LinkedHashSet<>(Set.of(dataElementA.getUid())));

    relationshipTypeTeiToPsi.getToConstraint().setTrackerDataView(trackerDataViewTo);

    manager.update(relationshipTypeTeiToPsi);

    Relationship relationshipPayload = new Relationship();
    relationshipPayload.setRelationshipType(relationshipTypeTeiToPsi.getUid());

    RelationshipItem from = teiFrom();

    RelationshipItem to = new RelationshipItem();
    Event event = new Event();
    event.setEvent(programStageInstanceA.getUid());
    to.setEvent(event);

    relationshipPayload.setFrom(from);
    relationshipPayload.setTo(to);

    ImportSummary importSummary =
        relationshipService.addRelationship(relationshipPayload, new ImportOptions());

    Optional<Relationship> relationshipDb =
        relationshipService.findRelationshipByUid(importSummary.getReference());

    assertAll(
        () -> assertEquals(ImportStatus.SUCCESS, importSummary.getStatus()),
        () -> assertEquals(1, importSummary.getImportCount().getImported()),
        () ->
            assertAll(
                () -> {
                  assertTrue(relationshipDb.isPresent());
                  Relationship r = relationshipDb.get();
                  assertEquals(from, r.getFrom());
                  assertEquals(to, r.getTo());
                }));
  }

  @Test
  void shouldUpdateTeiToPsiRelationship() {
    org.hisp.dhis.relationship.Relationship relationship =
        relationship(teiA, null, null, programStageInstanceA);

    Relationship relationshipPayload = new Relationship();
    relationshipPayload.setRelationship(relationship.getUid());
    relationshipPayload.setRelationshipType(relationship.getRelationshipType().getUid());

    RelationshipItem from = teiFrom();

    RelationshipItem to = new RelationshipItem();
    Event event = new Event();
    event.setEvent(programStageInstanceA.getUid());
    to.setEvent(event);

    relationshipPayload.setFrom(from);
    relationshipPayload.setTo(to);

    ImportSummary importSummary =
        relationshipService.updateRelationship(relationshipPayload, new ImportOptions());

    Optional<Relationship> relationshipDb =
        relationshipService.findRelationshipByUid(importSummary.getReference());

    assertAll(
        () -> assertEquals(ImportStatus.SUCCESS, importSummary.getStatus()),
        () -> assertEquals(1, importSummary.getImportCount().getUpdated()),
        () ->
            assertAll(
                () -> {
                  assertTrue(relationshipDb.isPresent());
                  Relationship r = relationshipDb.get();
                  assertEquals(relationship.getUid(), r.getRelationship());
                  assertEquals(from, r.getFrom());
                  assertEquals(to, r.getTo());
                }));
  }

  private Attribute attributeFromTea(TrackedEntityAttribute tea, String value) {
    Attribute attribute = new Attribute(tea.getUid(), tea.getValueType(), value);
    attribute.setCode(tea.getCode());
    attribute.setDisplayName(tea.getDisplayName());
    return attribute;
  }

  private RelationshipItem teiFrom() {
    RelationshipItem from = new RelationshipItem();
    TrackedEntityInstance trackedEntityInstanceFrom = new TrackedEntityInstance();
    trackedEntityInstanceFrom.setTrackedEntityInstance(teiA.getUid());
    from.setTrackedEntityInstance(trackedEntityInstanceFrom);
    return from;
  }

  private org.hisp.dhis.relationship.Relationship relationship(
      org.hisp.dhis.trackedentity.TrackedEntityInstance teiFrom,
      org.hisp.dhis.trackedentity.TrackedEntityInstance teiTo,
      ProgramInstance piTo,
      ProgramStageInstance psiTo) {
    org.hisp.dhis.relationship.Relationship relationship =
        new org.hisp.dhis.relationship.Relationship();

    org.hisp.dhis.relationship.RelationshipItem from =
        new org.hisp.dhis.relationship.RelationshipItem();
    from.setTrackedEntityInstance(teiFrom);

    org.hisp.dhis.relationship.RelationshipItem to =
        new org.hisp.dhis.relationship.RelationshipItem();

    if (null != teiTo) {
      to.setTrackedEntityInstance(teiTo);
      relationship.setRelationshipType(relationshipTypeTeiToTei);
    } else if (null != piTo) {
      to.setProgramInstance(piTo);
      relationship.setRelationshipType(relationshipTypeTeiToPi);
    } else {
      to.setProgramStageInstance(psiTo);
      relationship.setRelationshipType(relationshipTypeTeiToPsi);
    }

    relationship.setFrom(from);
    relationship.setTo(to);

    relationship.setKey(RelationshipUtils.generateRelationshipKey(relationship));
    relationship.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationship));

    manager.save(relationship);

    return relationship;
  }
}
