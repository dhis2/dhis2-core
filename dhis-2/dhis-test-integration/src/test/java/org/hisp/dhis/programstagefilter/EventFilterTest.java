/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.programstagefilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class EventFilterTest extends PostgresIntegrationTestBase {

  @Autowired private EventFilterService eventFilterService;

  @Autowired private ProgramService programService;

  private Program programA;

  private Program programB;

  @BeforeAll
  void setUp() {
    programA = createProgram('A');
    programB = createProgram('B');
    programService.addProgram(programA);
    programService.addProgram(programB);
  }

  @Test
  void testValidatenvalidEventFilterWithMissingProgram() {
    EventFilter eventFilter = createProgramStageInstanceFilter('1', null, null);
    List<String> errors = eventFilterService.validate(eventFilter);
    assertNotNull(errors);
    assertEquals(1, errors.size());
    assertTrue(errors.get(0).contains("Program should be specified for event filters"));
  }

  @Test
  void testValidateInvalidEventFilterWithInvalidProgram() {
    EventFilter eventFilter = createProgramStageInstanceFilter('1', "ABCDEF12345", null);
    List<String> errors = eventFilterService.validate(eventFilter);
    assertNotNull(errors);
    assertEquals(1, errors.size());
    assertTrue(errors.get(0).contains("Program is specified but does not exist"));
  }

  @Test
  void testValidateInvalidEventFilterWithInvalidProgramStage() {
    EventFilter eventFilter =
        createProgramStageInstanceFilter('1', programA.getUid(), "ABCDEF12345");
    List<String> errors = eventFilterService.validate(eventFilter);
    assertNotNull(errors);
    assertEquals(1, errors.size());
    assertTrue(errors.get(0).contains("Program stage is specified but does not exist"));
  }

  @Test
  void testValidateInvalidEventFilterWithInvalidOrganisationUnit() {
    EventFilter eventFilter = createProgramStageInstanceFilter('1', programA.getUid(), null);
    EventQueryCriteria eqc = new EventQueryCriteria();
    eqc.setOrganisationUnit("ABCDEF12345");
    eventFilter.setEventQueryCriteria(eqc);
    List<String> errors = eventFilterService.validate(eventFilter);
    assertNotNull(errors);
    assertEquals(1, errors.size());
    assertTrue(errors.get(0).contains("Org unit is specified but does not exist"));
  }

  @Test
  void testValidateInvalidEventFilterWithDataFilterAndEventUids() {
    EventFilter eventFilter = createProgramStageInstanceFilter('1', programA.getUid(), null);
    EventQueryCriteria eqc = new EventQueryCriteria();
    eqc.setEvents(Collections.singleton("abcdefghijklm"));
    eqc.setDataFilters(Collections.singletonList(new EventDataFilter()));
    eventFilter.setEventQueryCriteria(eqc);
    List<String> errors = eventFilterService.validate(eventFilter);
    assertNotNull(errors);
    assertEquals(1, errors.size());
    assertTrue(
        errors.get(0).contains("Event UIDs and filters can not be specified at the same time"));
  }

  @Test
  void testValidateInvalidEventFilterWithIncorrectAssignedUserMode() {
    EventFilter eventFilter = createProgramStageInstanceFilter('1', programA.getUid(), null);
    EventQueryCriteria eqc = new EventQueryCriteria();
    eqc.setAssignedUserMode(AssignedUserSelectionMode.CURRENT);
    eqc.setAssignedUsers(Collections.singleton("abcdefghijklm"));
    eventFilter.setEventQueryCriteria(eqc);
    List<String> errors = eventFilterService.validate(eventFilter);
    assertNotNull(errors);
    assertEquals(1, errors.size());
    assertTrue(
        errors
            .get(0)
            .contains("Assigned User uid(s) cannot be specified if selectionMode is not PROVIDED"));
  }

  @Test
  void testValidateEventFilterSuccessfully() {
    EventFilter eventFilter = createProgramStageInstanceFilter('1', programA.getUid(), null);
    EventQueryCriteria eqc = new EventQueryCriteria();
    eqc.setAssignedUserMode(AssignedUserSelectionMode.CURRENT);
    eventFilter.setEventQueryCriteria(eqc);
    List<String> errors = eventFilterService.validate(eventFilter);
    assertNotNull(errors);
    assertEquals(0, errors.size());
  }

  private static EventFilter createProgramStageInstanceFilter(
      char uniqueCharacter, String program, String programStage) {
    EventFilter eventFilter = new EventFilter();
    eventFilter.setAutoFields();
    eventFilter.setName("eventFilterName" + uniqueCharacter);
    eventFilter.setCode("eventFilterCode" + uniqueCharacter);
    eventFilter.setDescription("eventFilterDescription" + uniqueCharacter);
    if (program != null) {
      eventFilter.setProgram(program);
    }
    if (programStage != null) {
      eventFilter.setProgramStage(programStage);
    }
    return eventFilter;
  }
}
