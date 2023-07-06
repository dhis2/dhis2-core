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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.tracker.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Geometry;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventMapperTest {

  @Mock private Geometry geometry;

  private EventMapper mapper = Mappers.getMapper(EventMapper.class);

  @Test
  void testMappingEvent() {
    Event event = new Event();
    event.setEvent("eventUid");
    event.setStatus(EventStatus.ACTIVE);
    event.setProgram("program");
    event.setProgramStage("programStage");
    event.setEnrollment("enrollment");
    event.setTrackedEntityInstance("trackedEntityInstance");
    event.setOrgUnit("orgUnit");
    event.setOrgUnitName("orgUnitName");
    event.setRelationships(Set.of());
    event.setEventDate("2020-01-01T00:00:00Z");
    event.setDueDate("2023-01-01T00:00:00Z");
    event.setStoredBy("storedBy");
    event.setFollowup(true);
    event.setDeleted(false);
    event.setCreated("2018-01-01T00:00:00Z");
    event.setCreatedAtClient("2018-01-02T00:00:00Z");
    event.setLastUpdated("2022-01-01T00:00:00Z");
    event.setLastUpdatedAtClient("2022-01-02T00:00:00Z");
    event.setAttributeOptionCombo("attributeOptionCombo");
    event.setAttributeCategoryOptions("attributeCategoryOptions");
    event.setCompletedBy("completedBy");
    event.setCompletedDate("2021-01-01T00:00:00Z");
    event.setGeometry(geometry);
    event.setAssignedUser("user");
    event.setAssignedUserUsername("username");
    event.setAssignedUserFirstName("firstname");
    event.setAssignedUserSurname("surname");
    event.setCreatedByUserInfo(createdUserInfoSnapshot());
    event.setLastUpdatedByUserInfo(updatedUserInfoSnapshot());
    event.setDataValues(Set.of());
    event.setNotes(List.of());

    org.hisp.dhis.tracker.domain.Event mappedEvent = mapper.from(event);

    assertEquals("eventUid", mappedEvent.getEvent());
    assertEquals(EventStatus.ACTIVE, mappedEvent.getStatus());
    assertEquals("program", mappedEvent.getProgram());
    assertEquals("programStage", mappedEvent.getProgramStage());
    assertEquals("enrollment", mappedEvent.getEnrollment());
    assertEquals("trackedEntityInstance", mappedEvent.getTrackedEntity());
    assertEquals("orgUnit", mappedEvent.getOrgUnit());
    assertEquals("orgUnitName", mappedEvent.getOrgUnitName());
    assertThat(mappedEvent.getRelationships(), empty());
    assertEquals("2020-01-01T00:00:00Z", mappedEvent.getOccurredAt().toString());
    assertEquals("2023-01-01T00:00:00Z", mappedEvent.getScheduledAt().toString());
    assertEquals("storedBy", mappedEvent.getStoredBy());
    assertEquals(true, mappedEvent.isFollowup());
    assertEquals(false, mappedEvent.isDeleted());
    assertEquals("2018-01-01T00:00:00Z", mappedEvent.getCreatedAt().toString());
    assertEquals("2018-01-02T00:00:00Z", mappedEvent.getCreatedAtClient().toString());
    assertEquals("2022-01-01T00:00:00Z", mappedEvent.getUpdatedAt().toString());
    assertEquals("2022-01-02T00:00:00Z", mappedEvent.getUpdatedAtClient().toString());
    assertEquals("attributeOptionCombo", mappedEvent.getAttributeOptionCombo());
    assertEquals("attributeCategoryOptions", mappedEvent.getAttributeCategoryOptions());
    assertEquals("completedBy", mappedEvent.getCompletedBy());
    assertEquals("2021-01-01T00:00:00Z", mappedEvent.getCompletedAt().toString());
    assertEquals(geometry, mappedEvent.getGeometry());
    assertEquals("user", mappedEvent.getAssignedUser().getUid());
    assertEquals("username", mappedEvent.getAssignedUser().getUsername());
    assertEquals("firstname", mappedEvent.getAssignedUser().getFirstName());
    assertEquals("surname", mappedEvent.getAssignedUser().getSurname());
    assertEquals(createdUser(), mappedEvent.getCreatedBy());
    assertEquals(updatedUser(), mappedEvent.getUpdatedBy());
    assertThat(mappedEvent.getDataValues(), empty());
    assertThat(mappedEvent.getNotes(), empty());
  }

  private User createdUser() {
    return User.builder()
        .uid("createdUserUid")
        .username("createdUserUsername")
        .firstName("createdUserFirstName")
        .surname("createdUserSurname")
        .build();
  }

  private User updatedUser() {
    return User.builder()
        .uid("updatedUserUid")
        .username("updatedUserUsername")
        .firstName("updatedUserFirstName")
        .surname("updatedUserSurname")
        .build();
  }

  private UserInfoSnapshot createdUserInfoSnapshot() {
    return UserInfoSnapshot.of(
        0,
        "code",
        "createdUserUid",
        "createdUserUsername",
        "createdUserFirstName",
        "createdUserSurname");
  }

  private UserInfoSnapshot updatedUserInfoSnapshot() {
    return UserInfoSnapshot.of(
        0,
        "code",
        "updatedUserUid",
        "updatedUserUsername",
        "updatedUserFirstName",
        "updatedUserSurname");
  }
}
