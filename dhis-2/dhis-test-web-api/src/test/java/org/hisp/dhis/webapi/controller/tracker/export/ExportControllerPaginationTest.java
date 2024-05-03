/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.controller.tracker.JsonPage;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests how {@link org.hisp.dhis.webapi.controller.tracker.export} controllers serialize {@link
 * org.hisp.dhis.tracker.export.Page} to JSON. The tests use the {@link
 * org.hisp.dhis.webapi.controller.tracker.export.relationship} controller but hold true for any of
 * the export controllers.
 */
class ExportControllerPaginationTest extends DhisControllerConvenienceTest {

  @Autowired private IdentifiableObjectManager manager;

  private OrganisationUnit orgUnit;

  private Program program;

  private ProgramStage programStage;

  private User owner;

  private User user;

  private TrackedEntityType trackedEntityType;

  @BeforeEach
  void setUp() {
    owner = makeUser("o");
    manager.save(owner, false);

    orgUnit = createOrganisationUnit('A');
    orgUnit.getSharing().setOwner(owner);
    manager.save(orgUnit, false);

    OrganisationUnit anotherOrgUnit = createOrganisationUnit('B');
    anotherOrgUnit.getSharing().setOwner(owner);
    manager.save(anotherOrgUnit, false);

    user = createUserWithId("tester", CodeGenerator.generateUid());
    user.addOrganisationUnit(orgUnit);
    user.setTeiSearchOrganisationUnits(Set.of(orgUnit));
    this.userService.updateUser(user);

    program = createProgram('A');
    program.addOrganisationUnit(orgUnit);
    program.getSharing().setOwner(owner);
    program.getSharing().addUserAccess(userAccess());
    manager.save(program, false);

    programStage = createProgramStage('A', program);
    programStage.getSharing().setOwner(owner);
    programStage.getSharing().addUserAccess(userAccess());
    manager.save(programStage, false);

    trackedEntityType = trackedEntityTypeAccessible();
  }

  @Test
  void shouldGetPaginatedItemsWithDefaults() {
    TrackedEntity to = trackedEntity();
    Event from1 = event(enrollment(to));
    Event from2 = event(enrollment(to));
    Relationship r1 = relationship(from1, to);
    Relationship r2 = relationship(from2, to);

    JsonPage page =
        GET("/tracker/relationships?trackedEntity={uid}", to.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of(r1.getUid(), r2.getUid()),
        page.getList("relationships", JsonRelationship.class)
            .toList(JsonRelationship::getRelationship));
    assertEquals(1, page.getPager().getPage());
    assertEquals(50, page.getPager().getPageSize());
    assertHasNoMember(page.getPager(), "total");
    assertHasNoMember(page.getPager(), "pageCount");

    // assert deprecated fields
    assertEquals(1, page.getPage());
    assertEquals(50, page.getPageSize());
    assertHasNoMember(page, "total");
    assertHasNoMember(page, "pageCount");
  }

  @Test
  void shouldGetPaginatedItemsWithPagingSetToTrue() {
    TrackedEntity to = trackedEntity();
    Event from1 = event(enrollment(to));
    Event from2 = event(enrollment(to));
    Relationship r1 = relationship(from1, to);
    Relationship r2 = relationship(from2, to);

    JsonPage page =
        GET("/tracker/relationships?trackedEntity={uid}&paging=true", to.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of(r1.getUid(), r2.getUid()),
        page.getList("relationships", JsonRelationship.class)
            .toList(JsonRelationship::getRelationship));
    assertEquals(1, page.getPager().getPage());
    assertEquals(50, page.getPager().getPageSize());
    assertHasNoMember(page.getPager(), "total");
    assertHasNoMember(page.getPager(), "pageCount");

    // assert deprecated fields
    assertEquals(1, page.getPage());
    assertEquals(50, page.getPageSize());
    assertHasNoMember(page, "total");
    assertHasNoMember(page, "pageCount");
  }

  @Test
  void shouldGetPaginatedItemsWithDefaultsAndTotals() {
    TrackedEntity to = trackedEntity();
    Event from1 = event(enrollment(to));
    Event from2 = event(enrollment(to));
    Relationship r1 = relationship(from1, to);
    Relationship r2 = relationship(from2, to);

    JsonPage page =
        GET("/tracker/relationships?trackedEntity={uid}&totalPages=true", to.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of(r1.getUid(), r2.getUid()),
        page.getList("relationships", JsonRelationship.class)
            .toList(JsonRelationship::getRelationship));
    assertEquals(1, page.getPager().getPage());
    assertEquals(50, page.getPager().getPageSize());
    assertEquals(2, page.getPager().getTotal());
    assertEquals(1, page.getPager().getPageCount());

    // assert deprecated fields
    assertEquals(1, page.getPage());
    assertEquals(50, page.getPageSize());
    assertEquals(2, page.getTotal());
    assertEquals(1, page.getPageCount());
  }

  @Test
  void shouldGetPaginatedItemsWithNonDefaults() {
    TrackedEntity to = trackedEntity();
    Event from1 = event(enrollment(to));
    Event from2 = event(enrollment(to));
    relationship(from1, to);
    relationship(from2, to);

    JsonPage page =
        GET("/tracker/relationships?trackedEntity={uid}&page=2&pageSize=1", to.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonList<JsonRelationship> relationships =
        page.getList("relationships", JsonRelationship.class);
    assertEquals(
        1,
        relationships.size(),
        () ->
            String.format("mismatch in number of expected relationship(s), got %s", relationships));
    assertEquals(2, page.getPager().getPage());
    assertEquals(1, page.getPager().getPageSize());
    assertHasNoMember(page.getPager(), "total");
    assertHasNoMember(page.getPager(), "pageCount");

    // assert deprecated fields
    assertEquals(2, page.getPage());
    assertEquals(1, page.getPageSize());
    assertHasNoMember(page.getPager(), "total");
    assertHasNoMember(page.getPager(), "pageCount");
  }

  @Test
  void shouldGetPaginatedItemsWithNonDefaultsAndTotals() {
    TrackedEntity to = trackedEntity();
    Event from1 = event(enrollment(to));
    Event from2 = event(enrollment(to));
    relationship(from1, to);
    relationship(from2, to);

    JsonPage page =
        GET(
                "/tracker/relationships?trackedEntity={uid}&page=2&pageSize=1&totalPages=true",
                to.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonList<JsonRelationship> relationships =
        page.getList("relationships", JsonRelationship.class);
    assertEquals(
        1,
        relationships.size(),
        () ->
            String.format("mismatch in number of expected relationship(s), got %s", relationships));
    assertEquals(2, page.getPager().getPage());
    assertEquals(1, page.getPager().getPageSize());
    assertEquals(2, page.getPager().getTotal());
    assertEquals(2, page.getPager().getPageCount());

    // assert deprecated fields
    assertEquals(2, page.getPage());
    assertEquals(1, page.getPageSize());
    assertEquals(2, page.getTotal());
    assertEquals(2, page.getPageCount());
  }

  @Test
  void shouldGetNonPaginatedItemsWithSkipPaging() {
    TrackedEntity to = trackedEntity();
    Event from1 = event(enrollment(to));
    Event from2 = event(enrollment(to));
    Relationship r1 = relationship(from1, to);
    Relationship r2 = relationship(from2, to);

    JsonPage page =
        GET("/tracker/relationships?trackedEntity={uid}&skipPaging=true", to.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of(r1.getUid(), r2.getUid()),
        page.getList("relationships", JsonRelationship.class)
            .toList(JsonRelationship::getRelationship));
    assertHasNoMember(page, "pager");

    // assert deprecated fields
    assertHasNoMember(page, "page");
    assertHasNoMember(page, "pageSize");
    assertHasNoMember(page, "total");
    assertHasNoMember(page, "pageCount");
  }

  @Test
  void shouldGetNonPaginatedItemsWithPagingSetToFalse() {
    TrackedEntity to = trackedEntity();
    Event from1 = event(enrollment(to));
    Event from2 = event(enrollment(to));
    Relationship r1 = relationship(from1, to);
    Relationship r2 = relationship(from2, to);

    JsonPage page =
        GET("/tracker/relationships?trackedEntity={uid}&paging=false", to.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    assertContainsOnly(
        List.of(r1.getUid(), r2.getUid()),
        page.getList("relationships", JsonRelationship.class)
            .toList(JsonRelationship::getRelationship));
    assertHasNoMember(page, "pager");

    // assert deprecated fields
    assertHasNoMember(page, "page");
    assertHasNoMember(page, "pageSize");
    assertHasNoMember(page, "total");
    assertHasNoMember(page, "pageCount");
  }

  private TrackedEntityType trackedEntityTypeAccessible() {
    TrackedEntityType type = trackedEntityType();
    type.getSharing().addUserAccess(userAccess());
    manager.save(type, false);
    return type;
  }

  private TrackedEntityType trackedEntityType() {
    TrackedEntityType type = createTrackedEntityType('A');
    type.getSharing().setOwner(owner);
    type.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    return type;
  }

  private TrackedEntity trackedEntity() {
    TrackedEntity te = trackedEntity(orgUnit);
    manager.save(te, false);
    return te;
  }

  private TrackedEntity trackedEntity(OrganisationUnit orgUnit) {
    TrackedEntity te = trackedEntity(orgUnit, trackedEntityType);
    manager.save(te, false);
    return te;
  }

  private TrackedEntity trackedEntity(
      OrganisationUnit orgUnit, TrackedEntityType trackedEntityType) {
    TrackedEntity te = createTrackedEntity(orgUnit);
    te.setTrackedEntityType(trackedEntityType);
    te.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    te.getSharing().setOwner(owner);
    return te;
  }

  private Enrollment enrollment(TrackedEntity te) {
    Enrollment enrollment = new Enrollment(program, te, orgUnit);
    enrollment.setAutoFields();
    enrollment.setEnrollmentDate(new Date());
    enrollment.setOccurredDate(new Date());
    enrollment.setStatus(ProgramStatus.COMPLETED);
    manager.save(enrollment, false);
    te.setEnrollments(Set.of(enrollment));
    manager.save(te, false);
    return enrollment;
  }

  private Event event(Enrollment enrollment) {
    Event event = new Event(enrollment, programStage, orgUnit);
    event.setAutoFields();
    manager.save(event, false);
    enrollment.setEvents(Set.of(event));
    manager.save(enrollment, false);
    return event;
  }

  private UserAccess userAccess() {
    UserAccess a = new UserAccess();
    a.setUser(user);
    a.setAccess(AccessStringHelper.FULL);
    return a;
  }

  private RelationshipType relationshipTypeAccessible() {
    RelationshipType type = relationshipType();
    type.getSharing().addUserAccess(userAccess());
    manager.save(type, false);
    return type;
  }

  private RelationshipType relationshipType() {
    RelationshipType type = createRelationshipType('A');
    type.getFromConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    type.getToConstraint().setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    type.getSharing().setOwner(owner);
    type.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(type, false);
    return type;
  }

  private Relationship relationship(Event from, TrackedEntity to) {
    Relationship r = new Relationship();

    RelationshipItem fromItem = new RelationshipItem();
    fromItem.setEvent(from);
    from.getRelationshipItems().add(fromItem);
    r.setFrom(fromItem);
    fromItem.setRelationship(r);

    RelationshipItem toItem = new RelationshipItem();
    toItem.setTrackedEntity(to);
    to.getRelationshipItems().add(toItem);
    r.setTo(toItem);
    toItem.setRelationship(r);

    RelationshipType type = relationshipTypeAccessible();
    r.setRelationshipType(type);
    r.setKey(type.getUid());
    r.setInvertedKey(type.getUid());

    r.setAutoFields();
    r.getSharing().setOwner(owner);
    r.setCreatedAtClient(new Date());
    manager.save(r, false);
    return r;
  }
}
