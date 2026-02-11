/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.notification;

import static org.hisp.dhis.tracker.test.TrackerTestBase.createSingleEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Sets;
import java.util.Date;
import java.util.HashSet;
import java.util.function.Consumer;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateStore;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.tracker.program.notification.SingleEventNotificationMessageRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair Asghar
 */
@Transactional
class SingleEventNotificationMessageRendererTest extends PostgresIntegrationTestBase {

  private static final String DATA_ELEMENT_UID = CodeGenerator.generateUid();
  private static final String DE_NOT_IN_STAGE_UID = CodeGenerator.generateUid();
  private static final String DE_WITH_OPTION_SET_UID = CodeGenerator.generateUid();
  private static final String PROGRAM_UID = CodeGenerator.generateUid();
  private static final String PROGRAM_STAGE_UID = CodeGenerator.generateUid();
  private static final String ORG_UNIT_UID = CodeGenerator.generateUid();
  private static final String EVENT_UID = "PSI-UID";
  private static final String TEMPLATE_UID = "PNT-1";

  private static final String DATA_ELEMENT_VALUE = "dataElementA-Text";
  private static final String OPTION_CODE = "OptionCodeA";
  private static final String OPTION_NAME = "OptionA";
  private static final String DE_NOT_IN_PROGRAM_STAGE = "[DataElement not in Program Stage]";

  private DataElement dataElementA;
  private DataElement dataElementWithOptionSet;

  private Program program;
  private ProgramStage programStage;
  private OrganisationUnit organisationUnit;
  private SingleEvent event;
  private ProgramNotificationTemplate template;

  @Autowired private ProgramService programService;
  @Autowired private DataElementService dataElementService;
  @Autowired private ProgramStageService programStageService;
  @Autowired private ProgramStageDataElementService programStageDataElementService;
  @Autowired private ProgramNotificationTemplateStore templateStore;
  @Autowired private OrganisationUnitService organisationUnitService;
  @Autowired private SingleEventNotificationMessageRenderer renderer;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private OptionService optionService;

  @BeforeEach
  void setUp() throws ConflictException {
    createOptionSet();
    createDataElements();
    createOrganisationUnit();
    createProgram();
    createProgramStage();
    createEvent();
    createNotificationTemplate();
  }

  @Test
  void testRenderWithDataElement() {
    assertRenderedMessage(
        template -> {
          template.setMessageTemplate("message is #{" + DATA_ELEMENT_UID + "}");
          template.setSubjectTemplate("subject is #{" + DATA_ELEMENT_UID + "}");
        },
        "message is " + DATA_ELEMENT_VALUE,
        "subject is " + DATA_ELEMENT_VALUE);
  }

  @Test
  void testRenderWithMissingDataElement() {
    assertRenderedMessage(
        template -> {
          template.setMessageTemplate("message is #{" + DE_NOT_IN_STAGE_UID + "}");
          template.setSubjectTemplate("subject is #{" + DE_NOT_IN_STAGE_UID + "}");
        },
        "message is " + DE_NOT_IN_PROGRAM_STAGE,
        "subject is " + DE_NOT_IN_PROGRAM_STAGE);
  }

  @Test
  void testRenderWithDataElementAndOptionSet() {
    assertRenderedMessage(
        template -> {
          template.setMessageTemplate("message is #{" + DE_WITH_OPTION_SET_UID + "}");
          template.setSubjectTemplate("subject is #{" + DE_WITH_OPTION_SET_UID + "}");
        },
        "message is " + OPTION_NAME,
        "subject is " + OPTION_NAME);
  }

  @Test
  void testRenderWithVariableIds() {
    assertRenderedMessage(
        template -> {
          template.setMessageTemplate("message is V{program_id} and V{event_org_unit_id}");
          template.setSubjectTemplate("subject is V{program_stage_id}");
        },
        "message is " + program.getUid() + " and " + ORG_UNIT_UID,
        "subject is " + programStage.getUid());
  }

  private void createOptionSet() throws ConflictException {
    Option optionA = createOption('A');
    Option optionB = createOption('B');

    OptionSet optionSet = createOptionSet('O', optionA, optionB);
    optionSet.setValueType(ValueType.TEXT);
    optionService.saveOptionSet(optionSet);

    dataElementWithOptionSet.setOptionSet(optionSet);
    dataElementService.addDataElement(dataElementWithOptionSet);
  }

  private void createDataElements() {
    dataElementA = createAndSaveDataElement(DATA_ELEMENT_UID, 'A');
    dataElementWithOptionSet =
        createDataElement('Q', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER);
    dataElementWithOptionSet.setUid(DE_WITH_OPTION_SET_UID);
    createAndSaveDataElement(DE_NOT_IN_STAGE_UID, 'C');
  }

  private DataElement createAndSaveDataElement(String uid, char uniqueChar) {
    DataElement element =
        createDataElement(
            uniqueChar, ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER);
    element.setUid(uid);
    dataElementService.addDataElement(element);
    return element;
  }

  private void createOrganisationUnit() {
    organisationUnit = createOrganisationUnit('A');
    organisationUnit.setUid(ORG_UNIT_UID);
    organisationUnitService.addOrganisationUnit(organisationUnit);
  }

  private void createProgram() {
    program = createProgram('A', new HashSet<>(), organisationUnit);
    program.setUid(PROGRAM_UID);
    program.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    programService.addProgram(program);
  }

  private void createProgramStage() {
    programStage = createProgramStage('A', program);
    programStage.setUid(PROGRAM_STAGE_UID);
    programStageService.saveProgramStage(programStage);

    addProgramStageDataElement(programStage, dataElementA, 1);
    addProgramStageDataElement(programStage, dataElementWithOptionSet, 2);

    programStageService.updateProgramStage(programStage);

    program.setProgramStages(Sets.newHashSet(programStage));
    programService.updateProgram(program);
  }

  private void addProgramStageDataElement(ProgramStage stage, DataElement element, int sortOrder) {
    ProgramStageDataElement psde = createProgramStageDataElement(stage, element, sortOrder);
    programStageDataElementService.addProgramStageDataElement(psde);
    stage.getProgramStageDataElements().add(psde);
  }

  private void createEvent() {
    event = createSingleEvent(programStage, organisationUnit);
    event.setOccurredDate(new Date());
    event.setUid(EVENT_UID);

    event.setEventDataValues(
        Sets.newHashSet(
            createEventDataValue(DATA_ELEMENT_UID, DATA_ELEMENT_VALUE),
            createEventDataValue(DE_NOT_IN_STAGE_UID, "dataElementD-Text"),
            createEventDataValue(DE_WITH_OPTION_SET_UID, OPTION_CODE)));

    manager.save(event);
  }

  private EventDataValue createEventDataValue(String dataElementUid, String value) {
    EventDataValue eventDataValue = new EventDataValue();
    eventDataValue.setDataElement(dataElementUid);
    eventDataValue.setAutoFields();
    eventDataValue.setValue(value);
    return eventDataValue;
  }

  private void createNotificationTemplate() {
    template = new ProgramNotificationTemplate();
    template.setName("Test-PNT");
    template.setMessageTemplate("message_template");
    template.setDeliveryChannels(Sets.newHashSet(DeliveryChannel.SMS));
    template.setSubjectTemplate("subject_template");
    template.setNotificationTrigger(NotificationTrigger.PROGRAM_RULE);
    template.setAutoFields();
    template.setUid(TEMPLATE_UID);
    templateStore.save(template);
  }

  private void assertRenderedMessage(
      Consumer<ProgramNotificationTemplate> templateUpdater,
      String expectedMessage,
      String expectedSubject) {
    templateUpdater.accept(template);
    templateStore.update(template);

    NotificationMessage message = renderer.render(event, template);

    assertEquals(expectedMessage, message.getMessage());
    assertEquals(expectedSubject, message.getSubject());
  }
}
