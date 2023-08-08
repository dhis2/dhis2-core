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
package org.hisp.dhis.trackedentityfilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.programstagefilter.DateFilterPeriod;
import org.hisp.dhis.programstagefilter.DatePeriodType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 */
class TrackedEntityFilterServiceTest extends SingleSetupIntegrationTestBase {

  @Autowired private ProgramService programService;

  @Autowired private TrackedEntityFilterService trackedEntityFilterService;

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  @Autowired private UserService _userService;

  private Program programA;

  private Program programB;

  @BeforeEach
  void init() {
    userService = _userService;
  }

  @Override
  public void setUpTest() {
    programA = createProgram('A', null, null);
    programB = createProgram('B', null, null);
    programService.addProgram(programA);
    programService.addProgram(programB);
  }

  @Test
  void testAddGet() {
    TrackedEntityFilter trackedEntityFilterA = createTrackedEntityFilter('A', programA);
    TrackedEntityFilter trackedEntityFilterB = createTrackedEntityFilter('B', programB);
    long idA = trackedEntityFilterService.add(trackedEntityFilterA);
    long idB = trackedEntityFilterService.add(trackedEntityFilterB);
    assertEquals(idA, trackedEntityFilterA.getId());
    assertEquals(idB, trackedEntityFilterB.getId());
    assertEquals(trackedEntityFilterA, trackedEntityFilterService.get(idA));
    assertEquals(trackedEntityFilterB, trackedEntityFilterService.get(idB));
  }

  @Test
  void testDefaultPrivateAccess() {
    long idA = trackedEntityFilterService.add(createTrackedEntityFilter('A', programA));
    TrackedEntityFilter trackedEntityFilterA = trackedEntityFilterService.get(idA);
    assertEquals(trackedEntityFilterA.getPublicAccess(), AccessStringHelper.DEFAULT);
  }

  @Test
  void testGetAll() {
    TrackedEntityFilter trackedEntityFilterA = createTrackedEntityFilter('A', programA);
    TrackedEntityFilter trackedEntityFilterB = createTrackedEntityFilter('B', programB);
    trackedEntityFilterService.add(trackedEntityFilterA);
    trackedEntityFilterService.add(trackedEntityFilterB);
    List<TrackedEntityFilter> trackedEntityFilters = trackedEntityFilterService.getAll();
    assertEquals(trackedEntityFilters.size(), 2);
    assertTrue(trackedEntityFilters.contains(trackedEntityFilterA));
    assertTrue(trackedEntityFilters.contains(trackedEntityFilterB));
  }

  @Test
  void testValidateProgramInTeiFilter() {
    TrackedEntityFilter trackedEntityFilterA = createTrackedEntityFilter('A', programA);
    assertEquals(0, trackedEntityFilterService.validate(trackedEntityFilterA).size());
    trackedEntityFilterA.setProgram(createProgram('z'));
    List<String> errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(1, errors.size());
    assertEquals(
        errors.get(0),
        "Program is specified but does not exist: " + trackedEntityFilterA.getProgram().getUid());

    trackedEntityFilterA.setProgram(programA);
    errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(0, errors.size());
  }

  @Test
  void testValidateAssignedUsersInTeiFilter() {
    TrackedEntityFilter trackedEntityFilterA = createTrackedEntityFilter('A', programA);
    trackedEntityFilterA
        .getEntityQueryCriteria()
        .setAssignedUserMode(AssignedUserSelectionMode.PROVIDED);
    List<String> errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(1, errors.size());
    assertEquals(errors.get(0), "Assigned Users cannot be empty with PROVIDED assigned user mode");

    trackedEntityFilterA
        .getEntityQueryCriteria()
        .setAssignedUsers(Collections.singleton("useruid"));
    errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(0, errors.size());
  }

  @Test
  void testValidateOrganisationUnitsSelectedModeInTeiFilter() {
    TrackedEntityFilter trackedEntityFilterA = createTrackedEntityFilter('A', programA);
    trackedEntityFilterA.getEntityQueryCriteria().setOuMode(OrganisationUnitSelectionMode.SELECTED);
    List<String> errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(1, errors.size());
    assertEquals(errors.get(0), "Organisation Unit cannot be empty with SELECTED org unit mode");

    trackedEntityFilterA.getEntityQueryCriteria().setOuMode(OrganisationUnitSelectionMode.CHILDREN);
    errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(1, errors.size());
    assertEquals(errors.get(0), "Organisation Unit cannot be empty with CHILDREN org unit mode");

    trackedEntityFilterA
        .getEntityQueryCriteria()
        .setOuMode(OrganisationUnitSelectionMode.DESCENDANTS);
    errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(1, errors.size());
    assertEquals(errors.get(0), "Organisation Unit cannot be empty with DESCENDANTS org unit mode");

    trackedEntityFilterA.getEntityQueryCriteria().setOuMode(OrganisationUnitSelectionMode.SELECTED);
    trackedEntityFilterA.getEntityQueryCriteria().setOrganisationUnit("organisationunituid");
    errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(0, errors.size());
  }

  @Test
  void testValidateOrderParamsInTeiFilter() {
    TrackedEntityFilter trackedEntityFilterA = createTrackedEntityFilter('A', programA);
    trackedEntityFilterA.getEntityQueryCriteria().setOrder("aaa:asc,created:desc");
    List<String> errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(1, errors.size());
    assertEquals(errors.get(0), "Invalid order property: aaa");
  }

  @Test
  void testValidateDateFilterPeriods() {
    TrackedEntityFilter trackedEntityFilterA = createTrackedEntityFilter('A', programA);
    DateFilterPeriod incorrectDateFilterPeriod = new DateFilterPeriod();
    incorrectDateFilterPeriod.setType(DatePeriodType.ABSOLUTE);

    DateFilterPeriod correctDateFilterPeriod = new DateFilterPeriod();
    correctDateFilterPeriod.setType(DatePeriodType.ABSOLUTE);
    correctDateFilterPeriod.setStartDate(new Date());
    TrackedEntityAttribute attributeA = createTrackedEntityAttribute('A');
    trackedEntityAttributeService.addTrackedEntityAttribute(attributeA);

    AttributeValueFilter avf1 = new AttributeValueFilter();
    avf1.setAttribute(attributeA.getUid());
    avf1.setDateFilter(incorrectDateFilterPeriod);
    trackedEntityFilterA.getEntityQueryCriteria().getAttributeValueFilters().add(avf1);
    List<String> errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(1, errors.size());
    assertEquals(
        errors.get(0),
        "Start date or end date not specified with ABSOLUTE date period type for "
            + attributeA.getUid());

    avf1.setDateFilter(correctDateFilterPeriod);
    errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(0, errors.size());

    trackedEntityFilterA.getEntityQueryCriteria().getAttributeValueFilters().clear();

    trackedEntityFilterA
        .getEntityQueryCriteria()
        .setEnrollmentCreatedDate(incorrectDateFilterPeriod);
    errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(1, errors.size());
    assertEquals(
        errors.get(0),
        "Start date or end date not specified with ABSOLUTE date period type for EnrollmentCreatedDate");
    trackedEntityFilterA.getEntityQueryCriteria().setEnrollmentCreatedDate(null);

    trackedEntityFilterA
        .getEntityQueryCriteria()
        .setEnrollmentIncidentDate(incorrectDateFilterPeriod);
    errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(1, errors.size());
    assertEquals(
        errors.get(0),
        "Start date or end date not specified with ABSOLUTE date period type for EnrollmentIncidentDate");
    trackedEntityFilterA.getEntityQueryCriteria().setEnrollmentIncidentDate(null);

    trackedEntityFilterA.getEntityQueryCriteria().setLastUpdatedDate(incorrectDateFilterPeriod);
    errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(1, errors.size());
    assertEquals(
        errors.get(0),
        "Start date or end date not specified with ABSOLUTE date period type for LastUpdatedDate");
    trackedEntityFilterA.getEntityQueryCriteria().setLastUpdatedDate(null);

    trackedEntityFilterA.getEntityQueryCriteria().setEventDate(incorrectDateFilterPeriod);
    errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(1, errors.size());
    assertEquals(
        errors.get(0),
        "Start date or end date not specified with ABSOLUTE date period type for EventDate");
    trackedEntityFilterA.getEntityQueryCriteria().setEventDate(null);
  }

  @Test
  void testValidateAttributeInTeiAttributeValueFilter() {
    TrackedEntityFilter trackedEntityFilterA = createTrackedEntityFilter('A', programA);
    TrackedEntityAttribute attributeA = createTrackedEntityAttribute('A');
    TrackedEntityAttribute attributeB = createTrackedEntityAttribute('B');
    trackedEntityAttributeService.addTrackedEntityAttribute(attributeA);

    AttributeValueFilter avf1 = new AttributeValueFilter();
    avf1.setAttribute(attributeA.getUid());
    avf1.setEq("abc");
    trackedEntityFilterA.getEntityQueryCriteria().getAttributeValueFilters().add(avf1);
    assertEquals(0, trackedEntityFilterService.validate(trackedEntityFilterA).size());

    AttributeValueFilter avf2 = new AttributeValueFilter();
    avf2.setAttribute(attributeB.getUid());
    avf2.setEq("abcef");
    trackedEntityFilterA.getEntityQueryCriteria().getAttributeValueFilters().add(avf2);

    List<String> errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(1, errors.size());
    assertEquals(
        errors.get(0), "No tracked entity attribute found for attribute:" + avf2.getAttribute());

    trackedEntityFilterA.getEntityQueryCriteria().getAttributeValueFilters().clear();
    avf2.setAttribute("");
    trackedEntityFilterA.getEntityQueryCriteria().getAttributeValueFilters().add(avf2);
    errors = trackedEntityFilterService.validate(trackedEntityFilterA);
    assertEquals(1, errors.size());
    assertEquals(errors.get(0), "Attribute Uid is missing in filter");
  }

  @Test
  void testGetByProgram() {
    TrackedEntityFilter trackedEntityFilterA = createTrackedEntityFilter('A', programA);
    TrackedEntityFilter trackedEntityFilterB = createTrackedEntityFilter('B', programB);
    TrackedEntityFilter trackedEntityFilterC = createTrackedEntityFilter('C', programA);
    trackedEntityFilterService.add(trackedEntityFilterA);
    trackedEntityFilterService.add(trackedEntityFilterB);
    trackedEntityFilterService.add(trackedEntityFilterC);
    List<TrackedEntityFilter> trackedEntityFilters = trackedEntityFilterService.get(programA);
    assertEquals(trackedEntityFilters.size(), 2);
    assertTrue(trackedEntityFilters.contains(trackedEntityFilterA));
    assertTrue(trackedEntityFilters.contains(trackedEntityFilterC));
    assertFalse(trackedEntityFilters.contains(trackedEntityFilterB));
  }

  @Test
  void testUpdate() {
    TrackedEntityFilter trackedEntityFilterA = createTrackedEntityFilter('A', programA);
    long idA = trackedEntityFilterService.add(trackedEntityFilterA);
    trackedEntityFilterA.setProgram(programB);
    trackedEntityFilterService.update(trackedEntityFilterA);
    assertEquals(trackedEntityFilterA, trackedEntityFilterService.get(idA));
    List<TrackedEntityFilter> trackedEntityFilters = trackedEntityFilterService.get(programB);
    assertEquals(trackedEntityFilters.size(), 1);
    assertTrue(trackedEntityFilters.contains(trackedEntityFilterA));
    trackedEntityFilters = trackedEntityFilterService.get(programA);
    assertEquals(trackedEntityFilters.size(), 0);
  }

  @Test
  void testDelete() {
    TrackedEntityFilter trackedEntityFilterA = createTrackedEntityFilter('A', programA);
    TrackedEntityFilter trackedEntityFilterB = createTrackedEntityFilter('B', programB);
    long idA = trackedEntityFilterService.add(trackedEntityFilterA);
    long idB = trackedEntityFilterService.add(trackedEntityFilterB);
    List<TrackedEntityFilter> trackedEntityFilters = trackedEntityFilterService.getAll();
    assertEquals(trackedEntityFilters.size(), 2);
    trackedEntityFilterService.delete(trackedEntityFilterService.get(idA));
    assertNull(trackedEntityFilterService.get(idA));
    assertNotNull(trackedEntityFilterService.get(idB));
  }

  @Test
  void testSaveWithoutAuthority() {
    createUserAndInjectSecurityContext(false);
    TrackedEntityFilter trackedEntityFilterA = createTrackedEntityFilter('A', programA);
    long idA = trackedEntityFilterService.add(trackedEntityFilterA);
    assertNotNull(trackedEntityFilterService.get(idA));
  }

  @Test
  void testSaveWithAuthority() {
    createUserAndInjectSecurityContext(false, "F_PROGRAMSTAGE_ADD");
    TrackedEntityFilter trackedEntityFilterA = createTrackedEntityFilter('A', programA);
    long idA = trackedEntityFilterService.add(trackedEntityFilterA);
    assertNotNull(trackedEntityFilterService.get(idA));
  }
}
