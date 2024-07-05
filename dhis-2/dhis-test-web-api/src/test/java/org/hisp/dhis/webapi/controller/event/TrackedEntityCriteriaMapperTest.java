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
package org.hisp.dhis.webapi.controller.event;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.DhisWebSpringTest;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam.SortDirection;
import org.hisp.dhis.webapi.controller.event.mapper.TrackedEntityCriteriaMapper;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.controller.event.webrequest.TrackedEntityInstanceCriteria;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Luciano Fiandesio
 */
class TrackedEntityCriteriaMapperTest extends DhisWebSpringTest {

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private ProgramService programService;

  @Autowired private TrackedEntityAttributeService attributeService;

  @Autowired private UserService userService;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private TrackedEntityCriteriaMapper trackedEntityCriteriaMapper;

  private Program programA;

  private OrganisationUnit organisationUnit;

  private OrganisationUnit organisationUnitB;

  private TrackedEntityType trackedEntityTypeA = createTrackedEntityType('A');

  private TrackedEntityAttribute attrD = createTrackedEntityAttribute('D');

  private TrackedEntityAttribute attrE = createTrackedEntityAttribute('E');

  private TrackedEntityAttribute filtF = createTrackedEntityAttribute('F');

  private TrackedEntityAttribute filtG = createTrackedEntityAttribute('G');

  private String userId1;

  private String userId2;

  private String userId3;

  private String userIds;

  @Override
  public void setUpTest() {
    organisationUnit = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnit);
    organisationUnitB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(organisationUnitB);
    programA = createProgram('A', new HashSet<>(), organisationUnit);
    programService.addProgram(programA);
    trackedEntityTypeA.setPublicAccess(AccessStringHelper.FULL);
    trackedEntityTypeService.addTrackedEntityType(trackedEntityTypeA);
    attributeService.addTrackedEntityAttribute(attrD);
    attributeService.addTrackedEntityAttribute(attrE);
    attributeService.addTrackedEntityAttribute(filtF);
    attributeService.addTrackedEntityAttribute(filtG);
    userId1 = CodeGenerator.generateUid();
    userId2 = CodeGenerator.generateUid();
    userId3 = "user-3";
    userIds = userId1 + ";" + userId2 + ";" + userId3;
    // mock user
    super.userService = this.userService;
    User user = createUserWithAuth("testUser");
    user.setTeiSearchOrganisationUnits(Sets.newHashSet(organisationUnit));

    injectSecurityContext(user);
  }

  @Test
  void verifyCriteriaMapping() {
    TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
    criteria.setQuery("query-test");
    criteria.setAttribute(newHashSet(attrD.getUid(), attrE.getUid()));
    criteria.setFilter(newHashSet(filtF.getUid(), filtG.getUid()));
    criteria.setOu(organisationUnit.getUid());
    criteria.setOuMode(OrganisationUnitSelectionMode.DESCENDANTS);
    criteria.setProgram(programA.getUid());
    criteria.setProgramStatus(ProgramStatus.ACTIVE);
    criteria.setFollowUp(true);
    criteria.setLastUpdatedStartDate(StartDateTime.of("2019-01-01"));
    criteria.setLastUpdatedEndDate(EndDateTime.of("2020-01-01"));
    criteria.setLastUpdatedDuration("20");
    criteria.setProgramEnrollmentStartDate(StartDateTime.of("2019-08-05"));
    criteria.setProgramEnrollmentEndDate(EndDateTime.of("2020-08-05"));
    criteria.setProgramIncidentStartDate(StartDateTime.of("2019-05-05"));
    criteria.setProgramIncidentEndDate(EndDateTime.of("2020-05-05"));
    criteria.setTrackedEntityType(trackedEntityTypeA.getUid());
    criteria.setEventStatus(EventStatus.COMPLETED);
    criteria.setEventStartDate(StartDateTime.of("2019-07-07"));
    criteria.setEventEndDate(EndDateTime.of("2020-07-07"));
    criteria.setAssignedUserMode(AssignedUserSelectionMode.PROVIDED);
    criteria.setAssignedUser(userIds);
    criteria.setSkipMeta(true);
    criteria.setPage(1);
    criteria.setPageSize(50);
    criteria.setTotalPages(false);
    criteria.setSkipPaging(false);
    criteria.setIncludeDeleted(true);
    criteria.setIncludeAllAttributes(true);

    TrackedEntityInstanceQueryParams queryParams = trackedEntityCriteriaMapper.map(criteria);

    assertThat(queryParams.getQuery().getFilter(), is("query-test"));
    assertThat(queryParams.getQuery().getOperator(), is(QueryOperator.EQ));
    assertThat(queryParams.getProgram(), is(programA));
    assertThat(queryParams.getTrackedEntityType(), is(trackedEntityTypeA));
    assertThat(queryParams.getOrganisationUnits(), hasSize(1));
    assertThat(queryParams.getOrganisationUnits().iterator().next(), is(organisationUnit));
    assertThat(queryParams.getAttributes(), hasSize(2));
    assertTrue(
        queryParams.getAttributes().stream()
            .anyMatch(a -> a.getItem().getUid().equals(attrD.getUid())));
    assertTrue(
        queryParams.getAttributes().stream()
            .anyMatch(a -> a.getItem().getUid().equals(attrE.getUid())));
    assertThat(queryParams.getFilters(), hasSize(2));
    assertTrue(
        queryParams.getFilters().stream()
            .anyMatch(a -> a.getItem().getUid().equals(filtF.getUid())));
    assertTrue(
        queryParams.getFilters().stream()
            .anyMatch(a -> a.getItem().getUid().equals(filtG.getUid())));
    assertThat(queryParams.getPageSizeWithDefault(), is(50));
    assertThat(queryParams.getPageSize(), is(50));
    assertThat(queryParams.getPage(), is(1));
    assertThat(queryParams.isTotalPages(), is(false));
    assertThat(queryParams.getProgramStatus(), is(ProgramStatus.ACTIVE));
    assertThat(queryParams.getFollowUp(), is(true));
    assertThat(
        queryParams.getLastUpdatedStartDate(), is(criteria.getLastUpdatedStartDate().toDate()));
    assertThat(queryParams.getLastUpdatedEndDate(), is(criteria.getLastUpdatedEndDate().toDate()));
    assertThat(
        queryParams.getProgramEnrollmentStartDate(),
        is(criteria.getProgramEnrollmentStartDate().toDate()));
    assertThat(
        queryParams.getProgramEnrollmentEndDate(),
        is(criteria.getProgramEnrollmentEndDate().toDate()));
    assertThat(
        queryParams.getProgramIncidentStartDate(),
        is(criteria.getProgramIncidentStartDate().toDate()));
    assertThat(
        queryParams.getProgramIncidentEndDate(), is(criteria.getProgramIncidentEndDate().toDate()));
    assertThat(queryParams.getEventStatus(), is(EventStatus.COMPLETED));
    assertThat(queryParams.getEventStartDate(), is(criteria.getEventStartDate().toDate()));
    assertThat(queryParams.getEventEndDate(), is(criteria.getEventEndDate().toDate()));
    assertThat(queryParams.getAssignedUserSelectionMode(), is(AssignedUserSelectionMode.PROVIDED));
    assertTrue(queryParams.getAssignedUsers().stream().anyMatch(u -> u.equals(userId1)));
    assertTrue(queryParams.getAssignedUsers().stream().anyMatch(u -> u.equals(userId2)));
    assertThat(queryParams.getAssignedUsers().stream().anyMatch(u -> u.equals(userId3)), is(false));
    assertThat(queryParams.isIncludeDeleted(), is(true));
    assertThat(queryParams.isIncludeAllAttributes(), is(true));
  }

  @Test
  void mapOrderParam() {
    TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
    criteria.setOrder(
        List.of(
            OrderCriteria.of("inactive", SortDirection.ASC),
            OrderCriteria.of("createdAt", SortDirection.DESC)));

    TrackedEntityInstanceQueryParams queryParams = trackedEntityCriteriaMapper.map(criteria);

    assertEquals(
        List.of(
            new OrderParam("inactive", SortDirection.ASC),
            new OrderParam("createdAt", SortDirection.DESC)),
        queryParams.getOrders());
  }

  @Test
  void verifyCriteriaMappingFailOnMissingAttribute() {
    TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
    criteria.setAttribute(newHashSet(attrD.getUid(), attrE.getUid(), "missing"));
    IllegalQueryException e =
        assertThrows(IllegalQueryException.class, () -> trackedEntityCriteriaMapper.map(criteria));
    assertEquals("Attribute does not exist: missing", e.getMessage());
  }

  @Test
  void verifyCriteriaMappingFailOnMissingFilter() {
    TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
    criteria.setFilter(newHashSet(filtF.getUid(), filtG.getUid(), "missing"));
    IllegalQueryException e =
        assertThrows(IllegalQueryException.class, () -> trackedEntityCriteriaMapper.map(criteria));
    assertEquals("Attribute does not exist: missing", e.getMessage());
  }

  @Test
  void verifyCriteriaMappingFailOnMissingProgram() {
    TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
    criteria.setProgram(programA.getUid() + 'A');
    IllegalQueryException e =
        assertThrows(IllegalQueryException.class, () -> trackedEntityCriteriaMapper.map(criteria));
    assertEquals("Program does not exist: " + programA.getUid() + "A", e.getMessage());
  }

  @Test
  void verifyCriteriaMappingFailOnMissingTrackerEntityType() {
    TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
    criteria.setTrackedEntityType(trackedEntityTypeA.getUid() + "A");
    IllegalQueryException e =
        assertThrows(IllegalQueryException.class, () -> trackedEntityCriteriaMapper.map(criteria));
    assertEquals(
        "Tracked entity type does not exist: " + trackedEntityTypeA.getUid() + "A", e.getMessage());
  }

  @Test
  void verifyCriteriaMappingFailOnMissingOrgUnit() {
    TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
    criteria.setOu(organisationUnit.getUid() + "A");
    IllegalQueryException e =
        assertThrows(IllegalQueryException.class, () -> trackedEntityCriteriaMapper.map(criteria));
    assertEquals(
        "Organisation unit does not exist: " + organisationUnit.getUid() + "A", e.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenProgramIsProtectedSearchScopeIsEmptyAndUserNotInCaptureScope() {
    clearSecurityContext();
    User mockUser = createUserWithAuth("testUser2");
    mockUser.setOrganisationUnits(Set.of(organisationUnit));
    injectSecurityContext(mockUser);
    TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
    programA.setAccessLevel(AccessLevel.PROTECTED);
    criteria.setProgram(programA.getUid());
    criteria.setOu(organisationUnitB.getUid());

    IllegalQueryException e =
        assertThrows(IllegalQueryException.class, () -> trackedEntityCriteriaMapper.map(criteria));

    assertEquals(
        "Organisation unit is not part of the search scope: " + organisationUnitB.getUid(),
        e.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenProgramIsOpenAndUserNotInSearchScope() {
    clearSecurityContext();
    User mockUser = createUserWithAuth("testUser2");
    mockUser.setTeiSearchOrganisationUnits(Set.of(organisationUnit));
    injectSecurityContext(mockUser);
    TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
    programA.setAccessLevel(AccessLevel.OPEN);
    criteria.setProgram(programA.getUid());
    criteria.setOu(organisationUnitB.getUid());

    IllegalQueryException e =
        assertThrows(IllegalQueryException.class, () -> trackedEntityCriteriaMapper.map(criteria));

    assertEquals(
        "Organisation unit is not part of the search scope: " + organisationUnitB.getUid(),
        e.getMessage());
  }

  @Test
  void testGetFromUrlFailOnNonProvidedAndAssignedUsers() {
    TrackedEntityInstanceCriteria criteria = new TrackedEntityInstanceCriteria();
    criteria.setAssignedUser(userIds);
    criteria.setAssignedUserMode(AssignedUserSelectionMode.CURRENT);
    IllegalQueryException e =
        assertThrows(IllegalQueryException.class, () -> trackedEntityCriteriaMapper.map(criteria));
    assertEquals(
        "Assigned User uid(s) cannot be specified if selectionMode is not PROVIDED",
        e.getMessage());
  }
}