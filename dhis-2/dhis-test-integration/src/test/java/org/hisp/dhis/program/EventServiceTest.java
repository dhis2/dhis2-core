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
package org.hisp.dhis.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.TestCache;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.note.NoteService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
class EventServiceTest extends TransactionalIntegrationTest {

  @Autowired private EventService eventService;

  @Autowired private ProgramStageDataElementService programStageDataElementService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private DataElementService dataElementService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private TrackedEntityAttributeService attributeService;

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  @Autowired private NoteService noteService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private CategoryService categoryService;

  private OrganisationUnit organisationUnitA;

  private OrganisationUnit organisationUnitB;

  private ProgramStage stageA;

  private ProgramStage stageB;

  private ProgramStage stageC;

  private ProgramStage stageD;

  private DataElement dataElementA;

  private DataElement dataElementB;

  private DataElement dataElementC;

  private DataElement dataElementD;

  private ProgramStageDataElement stageDataElementA;

  private ProgramStageDataElement stageDataElementB;

  private ProgramStageDataElement stageDataElementC;

  private ProgramStageDataElement stageDataElementD;

  private Date incidenDate;

  private Date enrollmentDate;

  private Enrollment enrollmentA;

  private Enrollment enrollmentB;

  private Event eventA;

  private Event eventB;

  private Event eventC;

  private Event eventD1;

  private Event eventD2;

  private TrackedEntity trackedEntityA;

  private TrackedEntity trackedEntityB;

  private Program programA;

  private Cache<DataElement> dataElementMap = new TestCache<>();

  @Override
  public void setUpTest() {
    organisationUnitA = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnitA);
    organisationUnitB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(organisationUnitB);
    trackedEntityA = createTrackedEntity(organisationUnitA);
    trackedEntityService.addTrackedEntity(trackedEntityA);
    trackedEntityB = createTrackedEntity(organisationUnitB);
    trackedEntityService.addTrackedEntity(trackedEntityB);
    TrackedEntityAttribute attribute = createTrackedEntityAttribute('A');
    attribute.setValueType(ValueType.PHONE_NUMBER);
    attributeService.addTrackedEntityAttribute(attribute);
    TrackedEntityAttributeValue attributeValue =
        createTrackedEntityAttributeValue('A', trackedEntityA, attribute);
    attributeValue.setValue("123456789");
    attributeValueService.addTrackedEntityAttributeValue(attributeValue);
    trackedEntityA.getTrackedEntityAttributeValues().add(attributeValue);
    trackedEntityService.updateTrackedEntity(trackedEntityA);
    /** Program A */
    programA = createProgram('A', new HashSet<>(), organisationUnitA);
    programService.addProgram(programA);
    stageA = createProgramStage('A', 0);
    stageA.setProgram(programA);
    stageA.setSortOrder(1);
    programStageService.saveProgramStage(stageA);
    stageB = new ProgramStage("B", programA);
    stageB.setSortOrder(2);
    programStageService.saveProgramStage(stageB);
    Set<ProgramStage> programStages = new HashSet<>();
    programStages.add(stageA);
    programStages.add(stageB);
    programA.setProgramStages(programStages);
    programService.updateProgram(programA);
    dataElementA = createDataElement('A');
    dataElementB = createDataElement('B');
    dataElementC = createDataElement('C');
    dataElementD = createDataElement('D');
    dataElementService.addDataElement(dataElementA);
    dataElementService.addDataElement(dataElementB);
    dataElementService.addDataElement(dataElementC);
    dataElementService.addDataElement(dataElementD);
    stageDataElementA = new ProgramStageDataElement(stageA, dataElementA, false, 1);
    stageDataElementB = new ProgramStageDataElement(stageA, dataElementB, false, 2);
    stageDataElementC = new ProgramStageDataElement(stageB, dataElementA, false, 1);
    stageDataElementD = new ProgramStageDataElement(stageB, dataElementB, false, 2);
    programStageDataElementService.addProgramStageDataElement(stageDataElementA);
    programStageDataElementService.addProgramStageDataElement(stageDataElementB);
    programStageDataElementService.addProgramStageDataElement(stageDataElementC);
    programStageDataElementService.addProgramStageDataElement(stageDataElementD);
    /*
     * Program B
     */
    Program programB = createProgram('B', new HashSet<>(), organisationUnitB);
    programService.addProgram(programB);
    stageC = new ProgramStage("C", programB);
    stageC.setSortOrder(1);
    programStageService.saveProgramStage(stageC);
    stageD = new ProgramStage("D", programB);
    stageB.setSortOrder(2);
    stageC.setRepeatable(true);
    programStageService.saveProgramStage(stageD);
    programStages = new HashSet<>();
    programStages.add(stageC);
    programStages.add(stageD);
    programB.setProgramStages(programStages);
    programService.updateProgram(programB);
    /** Enrollment and event */
    DateTime testDate1 = DateTime.now();
    testDate1.withTimeAtStartOfDay();
    testDate1 = testDate1.minusDays(70);
    incidenDate = testDate1.toDate();
    DateTime testDate2 = DateTime.now();
    testDate2.withTimeAtStartOfDay();
    enrollmentDate = testDate2.toDate();
    enrollmentA = new Enrollment(enrollmentDate, incidenDate, trackedEntityA, programA);
    enrollmentA.setUid("UID-PIA");
    enrollmentService.addEnrollment(enrollmentA);
    enrollmentB = new Enrollment(enrollmentDate, incidenDate, trackedEntityB, programB);
    enrollmentService.addEnrollment(enrollmentB);
    eventA = createEvent(stageA, enrollmentA, organisationUnitA);
    eventA.setScheduledDate(enrollmentDate);
    eventA.setUid("UID-A");
    eventB = createEvent(stageB, enrollmentA, organisationUnitA);
    eventB.setScheduledDate(enrollmentDate);
    eventB.setUid("UID-B");
    eventC = createEvent(stageC, enrollmentB, organisationUnitB);
    eventC.setScheduledDate(enrollmentDate);
    eventC.setUid("UID-C");
    eventD1 = createEvent(stageD, enrollmentB, organisationUnitB);
    eventD1.setScheduledDate(enrollmentDate);
    eventD1.setUid("UID-D1");
    eventD2 = createEvent(stageD, enrollmentB, organisationUnitB);
    eventD2.setScheduledDate(enrollmentDate);
    eventD2.setUid("UID-D2");
    /*
     * Prepare data for EventDataValues manipulation tests
     */
    manager.save(eventA);
    // Check that there are no EventDataValues assigned to PSI
    Event tempPsiA = eventService.getEvent(eventA.getUid());
    assertEquals(0, tempPsiA.getEventDataValues().size());
    // Prepare EventDataValues to manipulate with
    dataElementMap.put(dataElementA.getUid(), dataElementA);
    dataElementMap.put(dataElementB.getUid(), dataElementB);
    dataElementMap.put(dataElementC.getUid(), dataElementC);
    dataElementMap.put(dataElementD.getUid(), dataElementD);
  }

  @Test
  void testAddEvent() {
    manager.save(eventA);
    long idA = eventA.getId();
    manager.save(eventB);
    long idB = eventB.getId();
    assertNotNull(getEvent(idA));
    assertNotNull(getEvent(idB));
  }

  @Test
  void testDeleteEvent() {
    manager.save(eventA);
    long idA = eventA.getId();
    manager.save(eventB);
    long idB = eventB.getId();
    assertNotNull(getEvent(idA));
    assertNotNull(getEvent(idB));
    eventService.deleteEvent(eventA);
    assertNull(getEvent(idA));
    assertNotNull(getEvent(idB));
    eventService.deleteEvent(eventB);
    assertNull(getEvent(idA));
    assertNull(getEvent(idB));
  }

  @Test
  void testGetEventById() {
    manager.save(eventA);
    long idA = eventA.getId();
    manager.save(eventB);
    long idB = eventB.getId();
    assertEquals(eventA, getEvent(idA));
    assertEquals(eventB, getEvent(idB));
  }

  @Test
  void testGetEventByUid() {
    manager.save(eventA);
    long idA = eventA.getId();
    manager.save(eventB);
    long idB = eventB.getId();
    assertEquals(eventA, getEvent(idA));
    assertEquals(eventB, getEvent(idB));
    assertEquals(eventA, eventService.getEvent("UID-A"));
    assertEquals(eventB, eventService.getEvent("UID-B"));
  }

  @Test
  void shouldNoteDeleteNoteWhenDeletingEvent() {
    Event event = createEvent(stageA, enrollmentA, organisationUnitA);
    event.setScheduledDate(enrollmentDate);
    event.setUid(CodeGenerator.generateUid());

    Note note = new Note();
    note.setCreator(CodeGenerator.generateUid());
    note.setNoteText("text");
    noteService.addNote(note);

    event.setNotes(List.of(note));

    manager.save(event);

    assertNotNull(eventService.getEvent(event.getUid()));

    eventService.deleteEvent(event);

    assertNull(eventService.getEvent(event.getUid()));
    assertTrue(noteService.noteExists(note.getUid()));
  }

  private Event getEvent(long id) {
    return manager.get(Event.class, id);
  }
}
