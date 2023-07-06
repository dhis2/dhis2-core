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
package org.hisp.dhis.sms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.sms.command.CompletenessMethod;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.SMSSpecialCharacter;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Created by zubair@dhis2.org on 20.10.17. */
class SMSCommandServiceTest extends DhisSpringTest {

  public static final String WRONG_FORMAT_MESSAGE = "Wrong command format";

  public static final String MORE_THAN_ONE_ORGUNIT_MESSAGE =
      "Found more than one org unit for this number. Please specify one organisation unit";

  public static final String NO_USER_MESSAGE =
      "No user associated with this phone number. Please contact your supervisor.";

  public static final String SUCCESS_MESSAGE = "Command has been processed successfully";

  private final ImmutableMap<String, Function<SMSCommand, String>> DEFAULT_MESSAGE_MAPPER =
      new ImmutableMap.Builder<String, Function<SMSCommand, String>>()
          .put(WRONG_FORMAT_MESSAGE, comm -> comm.getWrongFormatMessage())
          .put(MORE_THAN_ONE_ORGUNIT_MESSAGE, comm -> comm.getMoreThanOneOrgUnitMessage())
          .put(NO_USER_MESSAGE, comm -> comm.getNoUserMessage())
          .put(SUCCESS_MESSAGE, comm -> comm.getSuccessMessage())
          .build();

  private List<String> keys =
      Arrays.asList(
          WRONG_FORMAT_MESSAGE, MORE_THAN_ONE_ORGUNIT_MESSAGE, NO_USER_MESSAGE, SUCCESS_MESSAGE);

  private SMSCommand keyValueCommandA;

  private SMSCommand keyValueCommandB;

  private SMSCommand alertParserCommand;

  private SMSCommand unregisteredParserCommand;

  private SMSCommand teiRegistrationCommand;

  private SMSCommand eventRegistrationCommand;

  private String keyValueCommandName = "keyValue";

  private String keyValueCommandNameB = "keyValueB";

  private String alertMessageCommandName = "alert";

  private String unregistrationCommandName = "unreg";

  private String eventRegistrationCommandName = "event";

  private String teiCommandName = "alert";

  private String success = "Command Processed Successfully";

  private String wrongFormat = "Command format incorrect";

  private String noUserMessage = "No user associated with this OU";

  private SMSCode smsCodeA1;

  private SMSCode smsCodeA2;

  private SMSCode smsCodeB1;

  private SMSCode smsCodeB2;

  private SMSCode smsCodeC;

  private SMSCode smsCodeTeiA;

  private SMSCode smsCodeTeiB;

  private SMSCode smsCodeEvent;

  private SMSSpecialCharacter characterA;

  private SMSSpecialCharacter characterB;

  private DataElement dataElementA;

  private DataElement dataElementB;

  private DataElement dataElementC;

  private DataElement dataElementD;

  private TrackedEntityAttribute trackedEntityAttributeA;

  private TrackedEntityAttribute trackedEntityAttributeB;

  private DataSet dataSetA;

  private User userA;

  private User userB;

  private UserGroup userGroupA;

  private Program programA;

  private Program programD;

  private ProgramStage programStageD;

  private Map<String, String> defaultMessagesA = new HashMap<>();

  private Map<String, String> defaultMessagesB = new HashMap<>();

  private Map<String, String> defaultMessagesC = new HashMap<>();

  private Map<String, String> defaultMessagesD = new HashMap<>();

  private Map<String, String> defaultMessagesE = new HashMap<>();

  @Autowired private SMSCommandService smsCommandService;

  @Autowired private DataSetService dataSetService;

  @Autowired private DataElementService dataElementService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  @Autowired private UserGroupService userGroupService;

  @Autowired private UserService userService;

  @Override
  protected void setUpTest() throws Exception {
    createKeyValueCommands();
    createAlertParserCommand();
    createTeiCommand();
    createUnregisteredParserCommand();
    createEventRegistrationParserCommand();
  }

  // -------------------------------------------------------------------------
  // SAVE
  // -------------------------------------------------------------------------
  @Test
  void testSaveKeyValueParser() {
    smsCommandService.save(keyValueCommandA);
    SMSCommand keyValueCmd = smsCommandService.getSMSCommand(keyValueCommandName);
    assertEquals(keyValueCommandName, keyValueCmd.getName());
    assertTrue(keyValueCmd.getCodes().contains(smsCodeA1));
    assertTrue(keyValueCmd.getCodes().contains(smsCodeA2));
    assertTrue(
        CompletenessMethod.AT_LEAST_ONE_DATAVALUE.equals(keyValueCmd.getCompletenessMethod()));
  }

  @Test
  void testSaveAlertParser() {
    smsCommandService.save(alertParserCommand);
    SMSCommand alertCommand = smsCommandService.getSMSCommand(alertMessageCommandName);
    assertEquals(alertMessageCommandName, alertCommand.getName());
    assertEquals(0, alertCommand.getCodes().size());
    assertTrue(
        CompletenessMethod.AT_LEAST_ONE_DATAVALUE.equals(alertCommand.getCompletenessMethod()));
    assertEquals(success, alertCommand.getSuccessMessage());
    testDefaults(alertCommand, defaultMessagesB);
  }

  @Test
  void testSaveTeiRegistrationParser() {
    smsCommandService.save(teiRegistrationCommand);
    SMSCommand teiCommand = smsCommandService.getSMSCommand(teiCommandName);
    assertEquals(teiCommandName, teiCommand.getName());
    assertEquals(ParserType.TRACKED_ENTITY_REGISTRATION_PARSER, teiCommand.getParserType());
    assertEquals(programA, teiCommand.getProgram());
    testDefaults(teiCommand, defaultMessagesC);
    Set<TrackedEntityAttribute> teiAttributes =
        teiCommand.getCodes().stream()
            .map(c -> c.getTrackedEntityAttribute())
            .collect(Collectors.toSet());
    assertTrue(teiAttributes.contains(trackedEntityAttributeA));
    assertTrue(teiAttributes.contains(trackedEntityAttributeB));
  }

  @Test
  void testSaveUnregistrationParser() {
    smsCommandService.save(unregisteredParserCommand);
    SMSCommand unregCommand = smsCommandService.getSMSCommand(unregistrationCommandName);
    assertNotNull(unregCommand);
    assertEquals(unregistrationCommandName, unregCommand.getName());
    assertEquals(userGroupA, unregCommand.getUserGroup());
    testDefaults(unregCommand, defaultMessagesD);
    Set<User> users = unregCommand.getUserGroup().getMembers();
    assertTrue(users.contains(userA));
    assertTrue(users.contains(userB));
  }

  @Test
  void testSaveEventRegistrationParser() {
    smsCommandService.save(eventRegistrationCommand);
    SMSCommand eventRegistrationCommand =
        smsCommandService.getSMSCommand(eventRegistrationCommandName);
    assertEquals(eventRegistrationCommandName, eventRegistrationCommand.getName());
    assertEquals(programD, eventRegistrationCommand.getProgram());
    assertEquals(programStageD, eventRegistrationCommand.getProgramStage());
    testDefaults(eventRegistrationCommand, defaultMessagesE);
    Set<DataElement> dataElements =
        eventRegistrationCommand.getCodes().stream()
            .map(c -> c.getDataElement())
            .collect(Collectors.toSet());
    assertTrue(dataElements.contains(dataElementD));
    assertFalse(dataElements.contains(dataElementC));
  }

  // -------------------------------------------------------------------------
  // UPDATE
  // -------------------------------------------------------------------------
  @Test
  void testAddSmsCodes() {
    smsCommandService.save(keyValueCommandA);
    SMSCommand createdCommand = smsCommandService.getSMSCommand(keyValueCommandName);
    assertEquals(2, createdCommand.getCodes().size());
    smsCommandService.addSmsCodes(Sets.newHashSet(smsCodeC), createdCommand.getId());
    SMSCommand updatedCommand = smsCommandService.getSMSCommand(keyValueCommandName);
    assertEquals(3, updatedCommand.getCodes().size());
  }

  @Test
  void testAddSpecialCharacters() {
    smsCommandService.save(keyValueCommandA);
    SMSCommand createdCommand = smsCommandService.getSMSCommand(keyValueCommandName);
    assertEquals(1, createdCommand.getSpecialCharacters().size());
    smsCommandService.addSpecialCharacterSet(Sets.newHashSet(characterB), createdCommand.getId());
    SMSCommand updatedCommand = smsCommandService.getSMSCommand(keyValueCommandName);
    assertEquals(2, updatedCommand.getSpecialCharacters().size());
  }

  // -------------------------------------------------------------------------
  // DELETE
  // -------------------------------------------------------------------------
  @Test
  void testDelete() {
    smsCommandService.save(keyValueCommandA);
    SMSCommand createdCommand = smsCommandService.getSMSCommand(keyValueCommandName);
    assertNotNull(createdCommand);
    assertEquals(keyValueCommandName, createdCommand.getName());
    smsCommandService.delete(createdCommand);
    SMSCommand deleteCommand = smsCommandService.getSMSCommand(keyValueCommandName);
    assertNull(deleteCommand);
  }

  @Test
  void testDeleteSmsCodes() {
    smsCommandService.save(keyValueCommandA);
    SMSCommand commandA = smsCommandService.getSMSCommand(keyValueCommandName);
    assertEquals(2, commandA.getCodes().size());
    smsCommandService.deleteCodeSet(Sets.newHashSet(smsCodeA1), commandA.getId());
    SMSCommand updatedCommand = smsCommandService.getSMSCommand(keyValueCommandName);
    assertEquals(1, updatedCommand.getCodes().size());
    assertEquals(smsCodeA2, updatedCommand.getCodes().iterator().next());
  }

  @Test
  void testDeleteSpecialCharacters() {
    smsCommandService.save(keyValueCommandA);
    SMSCommand createdCommand = smsCommandService.getSMSCommand(keyValueCommandName);
    assertEquals(1, createdCommand.getSpecialCharacters().size());
    smsCommandService.deleteSpecialCharacterSet(
        Sets.newHashSet(characterA), createdCommand.getId());
    SMSCommand updatedCommand = smsCommandService.getSMSCommand(keyValueCommandName);
    assertEquals(0, updatedCommand.getSpecialCharacters().size());
  }

  // -------------------------------------------------------------------------
  // GET
  // -------------------------------------------------------------------------
  @Test
  void testGetSMSCommandByNameAndParser() {
    smsCommandService.save(keyValueCommandA);
    SMSCommand keyValueCmd =
        smsCommandService.getSMSCommand(keyValueCommandName, ParserType.KEY_VALUE_PARSER);
    assertNotNull(keyValueCmd);
    SMSCommand noCommand =
        smsCommandService.getSMSCommand(keyValueCommandName, ParserType.ALERT_PARSER);
    assertNull(noCommand);
  }

  @Test
  void testGetCommandByName() {
    smsCommandService.save(keyValueCommandA);
    SMSCommand created = smsCommandService.getSMSCommand(keyValueCommandName);
    assertNotNull(created);
    assertEquals(keyValueCommandName, created.getName());
    testDefaults(created, defaultMessagesA);
  }

  @Test
  void testGetKeyValueParameters() {
    smsCommandService.save(keyValueCommandA);
    SMSCommand created = smsCommandService.getSMSCommand(keyValueCommandName);
    assertNotNull(created);
    assertEquals(keyValueCommandName, created.getName());
    assertEquals(dataSetA, created.getDataset());
    Set<DataElement> dataElements =
        created.getCodes().stream().map(c -> c.getDataElement()).collect(Collectors.toSet());
    assertTrue(dataElements.contains(dataElementA));
    assertFalse(dataElements.contains(dataElementC));
  }

  @Test
  void testGetAllCommands() {
    smsCommandService.save(keyValueCommandA);
    smsCommandService.save(keyValueCommandB);
    List<SMSCommand> commands = smsCommandService.getSMSCommands();
    assertNotNull(commands);
    assertEquals(2, commands.size());
    assertTrue(commands.contains(keyValueCommandA));
    assertTrue(commands.contains(keyValueCommandB));
  }

  @Test
  void testGetCommandById() {
    smsCommandService.save(keyValueCommandA);
    smsCommandService.save(keyValueCommandB);
    List<SMSCommand> commands = smsCommandService.getSMSCommands();
    long id = commands.iterator().next().getId();
    SMSCommand fetched = smsCommandService.getSMSCommand(id);
    assertNotNull(fetched);
  }

  // -------------------------------------------------------------------------
  // Supportive Methods
  // -------------------------------------------------------------------------
  private void createKeyValueCommands() {
    dataElementA = createDataElement('A');
    dataElementB = createDataElement('B');
    dataElementC = createDataElement('C');
    dataElementService.addDataElement(dataElementA);
    dataElementService.addDataElement(dataElementB);
    dataElementService.addDataElement(dataElementC);
    dataSetA = createDataSet('A');
    dataSetA.addDataSetElement(dataElementA);
    dataSetA.addDataSetElement(dataElementB);
    dataSetService.addDataSet(dataSetA);
    smsCodeA1 = new SMSCode();
    smsCodeA1.setCode("a1");
    smsCodeA1.setDataElement(dataElementA);
    smsCodeA2 = new SMSCode();
    smsCodeA2.setCode("a2");
    smsCodeA2.setDataElement(dataElementB);
    smsCodeB1 = new SMSCode();
    smsCodeB1.setCode("b1");
    smsCodeB1.setDataElement(dataElementA);
    smsCodeB2 = new SMSCode();
    smsCodeB2.setCode("b2");
    smsCodeB2.setDataElement(dataElementB);
    smsCodeC = new SMSCode();
    smsCodeC.setCode("c");
    smsCodeC.setDataElement(dataElementC);
    characterA = new SMSSpecialCharacter();
    characterA.setName("charA");
    characterA.setValue("vCharA");
    characterB = new SMSSpecialCharacter();
    characterB.setName("charB");
    characterB.setValue("vCharB");
    keyValueCommandA = new SMSCommand();
    keyValueCommandA.setName(keyValueCommandName);
    keyValueCommandA.setParserType(ParserType.KEY_VALUE_PARSER);
    keyValueCommandA.setDataset(dataSetA);
    keyValueCommandA.setCodes(Sets.newHashSet(smsCodeA1, smsCodeA2));
    keyValueCommandA.setSpecialCharacters(Sets.newHashSet(characterA));
    keyValueCommandA.setSuccessMessage(success);
    defaultMessagesA.put(SUCCESS_MESSAGE, success);
    keyValueCommandB = new SMSCommand();
    keyValueCommandB.setName(keyValueCommandNameB);
    keyValueCommandB.setParserType(ParserType.KEY_VALUE_PARSER);
    keyValueCommandB.setDataset(dataSetA);
    keyValueCommandB.setCodes(Sets.newHashSet(smsCodeB1, smsCodeB2));
  }

  private void createTeiCommand() {
    trackedEntityAttributeA = createTrackedEntityAttribute('A');
    trackedEntityAttributeB = createTrackedEntityAttribute('B');
    trackedEntityAttributeService.addTrackedEntityAttribute(trackedEntityAttributeA);
    trackedEntityAttributeService.addTrackedEntityAttribute(trackedEntityAttributeB);
    programA = createProgram('A');
    programService.addProgram(programA);
    smsCodeTeiA = new SMSCode();
    smsCodeTeiA.setCode("a");
    smsCodeTeiA.setTrackedEntityAttribute(trackedEntityAttributeA);
    smsCodeTeiB = new SMSCode();
    smsCodeTeiB.setCode("b");
    smsCodeTeiB.setTrackedEntityAttribute(trackedEntityAttributeB);
    teiRegistrationCommand = new SMSCommand();
    teiRegistrationCommand.setName(teiCommandName);
    teiRegistrationCommand.setProgram(programA);
    teiRegistrationCommand.setParserType(ParserType.TRACKED_ENTITY_REGISTRATION_PARSER);
    teiRegistrationCommand.setCodes(Sets.newHashSet(smsCodeTeiA, smsCodeTeiB));
    teiRegistrationCommand.setSuccessMessage(success);
    teiRegistrationCommand.setNoUserMessage(noUserMessage);
    defaultMessagesC.put(SUCCESS_MESSAGE, success);
    defaultMessagesC.put(NO_USER_MESSAGE, noUserMessage);
  }

  private void createAlertParserCommand() {
    alertParserCommand = new SMSCommand();
    alertParserCommand.setName(alertMessageCommandName);
    alertParserCommand.setParserType(ParserType.ALERT_PARSER);
    alertParserCommand.setSuccessMessage(success);
    alertParserCommand.setWrongFormatMessage(wrongFormat);
    defaultMessagesB.put(SUCCESS_MESSAGE, success);
    defaultMessagesB.put(WRONG_FORMAT_MESSAGE, wrongFormat);
  }

  private void createUnregisteredParserCommand() {
    userA = createUser('A');
    userB = createUser('B');
    userService.addUser(userA);
    userService.addUser(userB);
    userGroupA = createUserGroup('G', Sets.newHashSet(userA, userB));
    userGroupService.addUserGroup(userGroupA);
    unregisteredParserCommand = new SMSCommand();
    unregisteredParserCommand.setName(unregistrationCommandName);
    unregisteredParserCommand.setParserType(ParserType.UNREGISTERED_PARSER);
    unregisteredParserCommand.setUserGroup(userGroupA);
  }

  private void createEventRegistrationParserCommand() {
    dataElementD = createDataElement('D');
    dataElementService.addDataElement(dataElementD);
    programD = createProgram('D');
    programService.addProgram(programD);
    programStageD = createProgramStage('S', programD);
    programStageService.saveProgramStage(programStageD);
    eventRegistrationCommand = new SMSCommand();
    eventRegistrationCommand.setName(eventRegistrationCommandName);
    eventRegistrationCommand.setProgram(programD);
    eventRegistrationCommand.setProgramStage(programStageD);
    smsCodeEvent = new SMSCode();
    smsCodeEvent.setCode("E");
    smsCodeEvent.setDataElement(dataElementD);
    eventRegistrationCommand.setCodes(Sets.newHashSet(smsCodeEvent));
  }

  private void testDefaults(SMSCommand command, Map<String, String> defaultMessages) {
    for (String key : keys) {
      if (defaultMessages.containsKey(key)) {
        assertEquals(defaultMessages.get(key), DEFAULT_MESSAGE_MAPPER.get(key).apply(command));
      } else {
        assertEquals(key, DEFAULT_MESSAGE_MAPPER.get(key).apply(command));
      }
    }
  }
}
