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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.WebClient.Accept;
import static org.hisp.dhis.webapi.WebClient.Body;
import static org.hisp.dhis.webapi.WebClient.ContentType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Set;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.event.RelationshipController} using (mocked)
 * REST requests.
 *
 * @author Jan Bernitt
 */
class RelationshipControllerTest extends DhisControllerConvenienceTest {

  private OrganisationUnit orgUnit;
  private TrackedEntityType trackedEntityType;

  private Program program;

  private ProgramStage programStage;

  @BeforeEach
  void setUp() {
    orgUnit = createOrganisationUnit('A');
    manager.save(orgUnit, false);

    trackedEntityType = createTrackedEntityType('B');
    manager.save(trackedEntityType, false);

    program = createProgram('A');
    program.addOrganisationUnit(orgUnit);
    manager.save(program, false);

    programStage = createProgramStage('A', program);
    manager.save(programStage, false);
  }

  @Test
  void testPostRelationshipJson() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Import was successful.",
        POST("/relationships/", "{'relationships':[]}").content(HttpStatus.OK));
  }

  @Test
  void testPostRelationshipXml() {
    HttpResponse response =
        POST(
            "/relationships/",
            Body("<relationships></relationships>"),
            ContentType(MediaType.APPLICATION_XML),
            Accept(MediaType.APPLICATION_XML));
    assertEquals(HttpStatus.OK, response.status());
    assertTrue(response.content(MediaType.APPLICATION_XML).startsWith("<webMessage"));
  }

  @Test
  void testUpdateRelationshipJson_NoSuchObject() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "No relationship with id 'xyz' was found.",
        PUT("/relationships/xyz", "{}").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testUpdateRelationshipXml_NoSuchObject() {
    HttpResponse response =
        PUT(
            "/relationships/xyz",
            Body("<relationship></relationship>"),
            ContentType(MediaType.APPLICATION_XML),
            Accept(MediaType.APPLICATION_XML));
    assertEquals(HttpStatus.NOT_FOUND, response.status());
  }

  @Test
  void testDeleteRelationship_NoSuchObject() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "No relationship with id 'xyz' was found.",
        DELETE("/relationships/xyz").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void shouldNotGetRelationshipsByTrackedEntityWhenRelationshipIsDeleted() {
    TrackedEntityInstance to = trackedEntity();
    ProgramInstance from = enrollment(to);
    Relationship r = relationship(from, to);

    assertRelationship(
        r, GET("/relationships?tei={te}", to.getUid()).content(HttpStatus.OK).as(JsonArray.class));

    manager.delete(r);

    assertNoRelationships(
        GET("/relationships?tei={te}", to.getUid()).content(HttpStatus.OK).as(JsonArray.class));
  }

  @Test
  void shouldNotGetRelationshipsByEnrollmentWhenRelationshipIsDeleted() {
    TrackedEntityInstance to = trackedEntity();
    ProgramInstance from = enrollment(to);
    Relationship r = relationship(from, to);

    assertRelationship(
        r,
        GET("/relationships?enrollment={en}", from.getUid())
            .content(HttpStatus.OK)
            .as(JsonArray.class));

    manager.delete(r);

    assertNoRelationships(
        GET("/relationships?enrollment={en}", from.getUid())
            .content(HttpStatus.OK)
            .as(JsonArray.class));
  }

  @Test
  void shouldNotGetRelationshipsByEventWhenRelationshipIsDeleted() {
    TrackedEntityInstance to = trackedEntity();
    ProgramStageInstance from = event(enrollment(to));
    Relationship r = relationship(from, to);

    assertRelationship(
        r,
        GET("/relationships?event={ev}", from.getUid()).content(HttpStatus.OK).as(JsonArray.class));

    manager.delete(r);

    assertNoRelationships(
        GET("/relationships?event={ev}", from.getUid()).content(HttpStatus.OK).as(JsonArray.class));
  }

  private TrackedEntityInstance trackedEntity() {
    TrackedEntityInstance te = trackedEntity(orgUnit);
    manager.save(te, false);
    return te;
  }

  private TrackedEntityInstance trackedEntity(OrganisationUnit orgUnit) {
    TrackedEntityInstance te = trackedEntity(orgUnit, trackedEntityType);
    manager.save(te, false);
    return te;
  }

  private TrackedEntityInstance trackedEntity(
      OrganisationUnit orgUnit, TrackedEntityType trackedEntityType) {
    TrackedEntityInstance te = createTrackedEntityInstance(orgUnit);
    te.setTrackedEntityType(trackedEntityType);
    te.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    return te;
  }

  private ProgramInstance enrollment(TrackedEntityInstance te) {
    ProgramInstance enrollment = new ProgramInstance(program, te, orgUnit);
    enrollment.setAutoFields();
    enrollment.setEnrollmentDate(new Date());
    enrollment.setIncidentDate(new Date());
    enrollment.setStatus(ProgramStatus.COMPLETED);
    manager.save(enrollment, false);
    te.setProgramInstances(Set.of(enrollment));
    manager.save(te, false);
    return enrollment;
  }

  private ProgramStageInstance event(ProgramInstance enrollment) {
    ProgramStageInstance event = new ProgramStageInstance(enrollment, programStage, orgUnit);
    event.setAutoFields();
    manager.save(event, false);
    enrollment.setProgramStageInstances(Set.of(event));
    manager.save(enrollment, false);
    return event;
  }

  private RelationshipType relationshipTypeAccessible(
      RelationshipEntity from, RelationshipEntity to) {
    RelationshipType type = relationshipType(from, to);
    manager.save(type, false);
    return type;
  }

  private RelationshipType relationshipType(RelationshipEntity from, RelationshipEntity to) {
    RelationshipType type = createRelationshipType('A');
    type.getFromConstraint().setRelationshipEntity(from);
    type.getToConstraint().setRelationshipEntity(to);
    type.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(type, false);
    return type;
  }

  private Relationship relationship(ProgramStageInstance from, TrackedEntityInstance to) {
    Relationship r = new Relationship();

    RelationshipItem fromItem = new RelationshipItem();
    fromItem.setProgramStageInstance(from);
    from.getRelationshipItems().add(fromItem);
    r.setFrom(fromItem);
    fromItem.setRelationship(r);

    RelationshipItem toItem = new RelationshipItem();
    toItem.setTrackedEntityInstance(to);
    to.getRelationshipItems().add(toItem);
    r.setTo(toItem);
    toItem.setRelationship(r);

    RelationshipType type =
        relationshipTypeAccessible(
            RelationshipEntity.PROGRAM_STAGE_INSTANCE, RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    r.setRelationshipType(type);
    r.setKey(type.getUid());
    r.setInvertedKey(type.getUid());

    r.setAutoFields();
    r.setCreated(new Date());
    manager.save(r, false);
    return r;
  }

  private Relationship relationship(ProgramInstance from, TrackedEntityInstance to) {
    manager.save(from, false);
    manager.save(to, false);

    Relationship r = new Relationship();

    RelationshipItem fromItem = new RelationshipItem();
    fromItem.setProgramInstance(from);
    from.getRelationshipItems().add(fromItem);
    r.setFrom(fromItem);
    fromItem.setRelationship(r);

    RelationshipItem toItem = new RelationshipItem();
    toItem.setTrackedEntityInstance(to);
    to.getRelationshipItems().add(toItem);
    r.setTo(toItem);
    toItem.setRelationship(r);

    RelationshipType type =
        relationshipTypeAccessible(
            RelationshipEntity.PROGRAM_INSTANCE, RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    r.setRelationshipType(type);
    r.setKey(type.getUid());
    r.setInvertedKey(type.getUid());

    r.setAutoFields();
    manager.save(r, false);
    return r;
  }

  public void assertNoRelationships(JsonArray json) {
    assertEquals(0, json.size(), "Response should have no relationship");
  }

  public static void assertRelationship(Relationship expected, JsonArray json) {
    assertEquals(
        1,
        json.size(),
        String.format("Relationship response should contain relationship %s", expected.getUid()));
    assertEquals(
        expected.getUid(),
        json.get(0).asObject().getString("relationship").string(),
        String.format(
            "Relationship response should contain relationship %s but got %s",
            expected.getUid(), json.get(0).asObject().getString("relationship").string()));
  }
}
