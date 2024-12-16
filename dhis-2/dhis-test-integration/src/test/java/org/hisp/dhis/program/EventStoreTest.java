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
package org.hisp.dhis.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class EventStoreTest extends PostgresIntegrationTestBase {

  @Autowired private EventStore eventStore;
  @Autowired private IdentifiableObjectManager manager;

  @Test
  @DisplayName("Retrieving Events by category option combo returns expected entries")
  void getByCocTest() {
    // given
    CategoryOptionCombo coc1 = createCategoryOptionCombo('1');
    coc1.setCategoryCombo(categoryService.getDefaultCategoryCombo());

    CategoryOptionCombo coc2 = createCategoryOptionCombo('2');
    coc2.setCategoryCombo(categoryService.getDefaultCategoryCombo());

    CategoryOptionCombo coc3 = createCategoryOptionCombo('3');
    coc3.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    manager.save(List.of(coc1, coc2, coc3));

    Program p = createProgram('p');
    manager.save(p);

    ProgramStage ps1 = createProgramStage('1', p);
    ProgramStage ps2 = createProgramStage('2', p);
    ProgramStage ps3 = createProgramStage('3', p);
    manager.save(List.of(ps1, ps2, ps3));

    OrganisationUnit org = createOrganisationUnit("org test");
    manager.save(org);

    TrackedEntity te1 = createTrackedEntity(org);
    TrackedEntity te2 = createTrackedEntity(org);
    TrackedEntity te3 = createTrackedEntity(org);
    manager.save(List.of(te1, te2, te3));

    Enrollment enrollment1 = createEnrollment(p, te1, org);
    Enrollment enrollment2 = createEnrollment(p, te2, org);
    Enrollment enrollment3 = createEnrollment(p, te3, org);
    manager.save(List.of(enrollment1, enrollment2, enrollment3));

    Event e1 = new Event(enrollment1, ps1, org);
    e1.setAttributeOptionCombo(coc1);
    Event e2 = new Event(enrollment2, ps2, org);
    e2.setAttributeOptionCombo(coc2);
    Event e3 = new Event(enrollment3, ps3, org);
    e3.setAttributeOptionCombo(coc3);
    manager.save(List.of(e1, e2, e3));

    // when
    List<Event> allByAttributeOptionCombo =
        eventStore.getAllByAttributeOptionCombo(UID.of(coc1.getUid(), coc2.getUid()));

    // then
    assertEquals(2, allByAttributeOptionCombo.size());
    assertTrue(
        allByAttributeOptionCombo.containsAll(List.of(e1, e2)),
        "Retrieved result set should contain both Events");
  }

  @Test
  @DisplayName("Deleting Events by category option combo deletes correct Events")
  void deleteByCocTest() {
    // given
    CategoryOptionCombo coc1 = createCategoryOptionCombo('1');
    coc1.setCategoryCombo(categoryService.getDefaultCategoryCombo());

    CategoryOptionCombo coc2 = createCategoryOptionCombo('2');
    coc2.setCategoryCombo(categoryService.getDefaultCategoryCombo());

    CategoryOptionCombo coc3 = createCategoryOptionCombo('3');
    coc3.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    manager.save(List.of(coc1, coc2, coc3));

    Program p = createProgram('p');
    manager.save(p);

    ProgramStage ps1 = createProgramStage('1', p);
    ProgramStage ps2 = createProgramStage('2', p);
    ProgramStage ps3 = createProgramStage('3', p);
    manager.save(List.of(ps1, ps2, ps3));

    OrganisationUnit org = createOrganisationUnit("org test");
    manager.save(org);

    TrackedEntity te1 = createTrackedEntity(org);
    TrackedEntity te2 = createTrackedEntity(org);
    TrackedEntity te3 = createTrackedEntity(org);
    manager.save(List.of(te1, te2, te3));

    Enrollment enrollment1 = createEnrollment(p, te1, org);
    Enrollment enrollment2 = createEnrollment(p, te2, org);
    Enrollment enrollment3 = createEnrollment(p, te3, org);
    manager.save(List.of(enrollment1, enrollment2, enrollment3));

    Event e1 = new Event(enrollment1, ps1, org);
    e1.setAttributeOptionCombo(coc1);
    Event e2 = new Event(enrollment2, ps2, org);
    e2.setAttributeOptionCombo(coc2);
    Event e3 = new Event(enrollment3, ps3, org);
    e3.setAttributeOptionCombo(coc3);
    manager.save(List.of(e1, e2, e3));

    // state before delete
    List<Event> eventsBeforeDelete =
        eventStore.getAllByAttributeOptionCombo(UID.of(coc1.getUid(), coc2.getUid()));
    assertEquals(2, eventsBeforeDelete.size());
    assertTrue(
        eventsBeforeDelete.containsAll(List.of(e1, e2)),
        "Retrieved result set should contain both Events");

    // when
    eventStore.deleteAllByAttributeOptionCombo(UID.of(coc1.getUid(), coc2.getUid()));
    List<Event> eventsAfterDelete =
        eventStore.getAllByAttributeOptionCombo(UID.of(coc1.getUid(), coc2.getUid()));
    List<Event> coc3AfterDelete = eventStore.getAllByAttributeOptionCombo(List.of(UID.of(coc3)));

    // then
    assertTrue(
        eventsAfterDelete.isEmpty(), "There should be 0 events referencing source COC1 or COC2");
    assertEquals(1, coc3AfterDelete.size(), "COC3 should have 1 Event");
  }
}
