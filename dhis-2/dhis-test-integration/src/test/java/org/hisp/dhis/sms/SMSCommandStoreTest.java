/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.sms;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.Hibernate;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.TextType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboStore;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.sms.command.CompletenessMethod;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSSpecialCharacter;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.command.hibernate.SMSCommandStore;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.UserGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author david mackessy
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class SMSCommandStoreTest extends PostgresIntegrationTestBase {

  @Autowired private DataElementService dataElementService;
  @Autowired private SMSCommandStore smsCommandStore;
  @Autowired private CategoryOptionComboStore categoryOptionComboStore;
  @Autowired private DataSetService dataSetService;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("retrieving SMS Codes by data element returns expected entries")
  void getSMSCodesByDataElementTest() {
    // given
    DataElement deW = createDataElementAndSave('W');
    DataElement deX = createDataElementAndSave('X');
    DataElement deY = createDataElementAndSave('Y');
    DataElement deZ = createDataElementAndSave('Z');

    createSMSCodeAndSave(deW, "code 1");
    createSMSCodeAndSave(deX, "code 2");
    createSMSCodeAndSave(deY, "code 3");
    createSMSCodeAndSave(deZ, "code 4");

    // when
    List<SMSCode> allByDataElement = smsCommandStore.getCodesByDataElement(List.of(deW, deX, deY));

    // then
    assertEquals(3, allByDataElement.size());
    assertTrue(
        allByDataElement.stream()
            .map(code -> code.getDataElement().getUid())
            .toList()
            .containsAll(List.of(deW.getUid(), deX.getUid(), deY.getUid())));
  }

  @Test
  @DisplayName("retrieving SMS Codes by cat option combos returns expected entries")
  void getByCocTest() {
    // given
    DataElement deW = createDataElementAndSave('W');
    DataElement deX = createDataElementAndSave('X');
    DataElement deY = createDataElementAndSave('Y');
    DataElement deZ = createDataElementAndSave('Z');

    SMSCode code1 = createSMSCodeAndSave(deW, "code 1");
    CategoryOptionCombo coc1 = createCategoryOptionCombo('A');
    coc1.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryOptionComboStore.save(coc1);
    code1.setOptionId(coc1);

    SMSCode code2 = createSMSCodeAndSave(deX, "code 2");
    CategoryOptionCombo coc2 = createCategoryOptionCombo('B');
    coc2.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryOptionComboStore.save(coc2);
    code2.setOptionId(coc2);

    SMSCode code3 = createSMSCodeAndSave(deY, "code 3");
    CategoryOptionCombo coc3 = createCategoryOptionCombo('C');
    coc3.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryOptionComboStore.save(coc3);
    code3.setOptionId(coc3);

    SMSCode code4 = createSMSCodeAndSave(deZ, "code 4");
    CategoryOptionCombo coc4 = createCategoryOptionCombo('D');
    coc4.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryOptionComboStore.save(coc4);
    code4.setOptionId(coc4);

    // when
    List<SMSCode> allByCoc = smsCommandStore.getCodesByCategoryOptionCombo(UID.of(coc1, coc2));

    // then
    assertEquals(2, allByCoc.size());
    assertTrue(
        allByCoc.stream()
            .map(code -> code.getOptionId().getUid())
            .toList()
            .containsAll(List.of(coc1.getUid(), coc2.getUid())),
        "Codes should contain correct COC UIDs");
  }

  @Test
  void shouldMapSmsCommandToItsTableColumnsAndSequence() {
    AbstractEntityPersister persister = persister();

    assertEquals("smscommands", persister.getTableName());
    assertArrayEquals(new String[] {"smscommandid"}, persister.getIdentifierColumnNames());
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister.getIdentifierGenerator());
    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
    assertEquals(1, generator.getDatabaseStructure().getIncrementSize());
    assertInstanceOf(NoopOptimizer.class, generator.getOptimizer());

    Map<String, String> columns =
        Map.ofEntries(
            Map.entry("uid", "uid"),
            Map.entry("name", "name"),
            Map.entry("created", "created"),
            Map.entry("lastUpdated", "lastupdated"),
            Map.entry("parserType", "parsertype"),
            Map.entry("separator", "separatorkey"),
            Map.entry("codeValueSeparator", "codeseparator"),
            Map.entry("defaultMessage", "defaultmessage"),
            Map.entry("receivedMessage", "receivedmessage"),
            Map.entry("wrongFormatMessage", "wrongformatmessage"),
            Map.entry("noUserMessage", "nousermessage"),
            Map.entry("moreThanOneOrgUnitMessage", "morethanoneorgunitmessage"),
            Map.entry("successMessage", "successmessage"),
            Map.entry("currentPeriodUsedForReporting", "currentperiodusedforreporting"),
            Map.entry("completenessMethod", "completenessmethod"),
            Map.entry("dataset", "datasetid"),
            Map.entry("userGroup", "usergroupid"),
            Map.entry("program", "programid"),
            Map.entry("programStage", "programstageid"));
    columns.forEach(
        (property, column) ->
            assertEquals(
                column,
                persister.getPropertyColumnNames(property)[0].toLowerCase(),
                property + " column"));
    assertNull(persister.getEntityMetamodel().getPropertyIndexOrNull("code"));
    assertNull(persister.getEntityMetamodel().getPropertyIndexOrNull("lastUpdatedBy"));
  }

  @Test
  void shouldMapSmsCommandNullabilityTypesLazinessAndCache() {
    AbstractEntityPersister persister = persister();

    for (String property : List.of("uid", "created", "lastUpdated")) {
      assertFalse(isNullable(property), property + " is mapped nullable");
    }
    for (String property :
        List.of("name", "parserType", "completenessMethod", "dataset", "receivedMessage")) {
      assertTrue(isNullable(property), property + " is mapped not-null");
    }
    for (String property :
        List.of(
            "name",
            "separator",
            "codeValueSeparator",
            "defaultMessage",
            "receivedMessage",
            "wrongFormatMessage",
            "noUserMessage",
            "moreThanOneOrgUnitMessage",
            "successMessage")) {
      assertInstanceOf(TextType.class, persister.getPropertyType(property), property);
    }
    for (String property : List.of("parserType", "completenessMethod")) {
      CustomType type = assertInstanceOf(CustomType.class, persister.getPropertyType(property));
      assertFalse(
          assertInstanceOf(EnumType.class, type.getUserType()).isOrdinal(),
          property + " is mapped ordinal");
    }
    for (String property : List.of("dataset", "userGroup", "program", "programStage")) {
      assertFalse(
          ((ManyToOneType) persister.getPropertyType(property)).isEager(null),
          property + " is eager");
    }
    assertTrue(
        persister
            .getPropertyUpdateability()[
            persister.getEntityMetamodel().getPropertyIndex("created")]);
    assertFalse(persister.canWriteToCache());
  }

  @Test
  void shouldMapCodesAndSpecialCharactersAsCascadedUniqueJoinTables() {
    AbstractEntityPersister persister = persister();
    Map<String, List<String>> joinTables =
        Map.of(
            "codes", List.of("smscommandcodes", "id", "codeid"),
            "specialCharacters",
                List.of("smscommandspecialcharacters", "smscommandid", "specialcharacterid"));

    joinTables.forEach(
        (property, table) -> {
          AbstractCollectionPersister collection = collectionPersister(property);
          assertEquals(table.get(0), collection.getTableName());
          assertArrayEquals(new String[] {table.get(1)}, collection.getKeyColumnNames());
          assertArrayEquals(new String[] {table.get(2)}, collection.getElementColumnNames());
          assertTrue(collection.isManyToMany(), property + " is not many-to-many");
          assertFalse(collection.isInverse(), property + " is inverse");
          assertTrue(collection.isLazy(), property + " is eager");
          CascadeStyle cascade =
              persister
                  .getPropertyCascadeStyles()[
                  persister.getEntityMetamodel().getPropertyIndex(property)];
          for (CascadingAction action :
              List.of(
                  CascadingActions.SAVE_UPDATE,
                  CascadingActions.PERSIST,
                  CascadingActions.MERGE,
                  CascadingActions.DELETE)) {
            assertTrue(cascade.doCascade(action), property + " does not cascade " + action);
          }
          assertFalse(cascade.hasOrphanDelete(), property + " deletes orphans");
        });
  }

  @Test
  void shouldWriteAndReadSmsCommandColumnsAndJoinTables() {
    DataSet dataSet = createDataSet('A');
    dataSetService.addDataSet(dataSet);
    UserGroup userGroup = createUserGroup('A', Set.of());
    manager.save(userGroup);
    Program program = createProgram('A');
    manager.save(program);
    ProgramStage programStage = createProgramStage('A', program);
    manager.save(programStage);
    SMSCode code = new SMSCode("a", createDataElementAndSave('A'), null);
    code.setFormula("+1");
    code.setCompulsory(true);
    SMSSpecialCharacter specialCharacter = new SMSSpecialCharacter("plus", "+");

    SMSCommand command =
        new SMSCommand(
            "Command A",
            ParserType.KEY_VALUE_PARSER,
            ".",
            dataSet,
            Set.of(code),
            "=",
            "default A",
            userGroup,
            "received A",
            Set.of(specialCharacter));
    command.setProgram(program);
    command.setProgramStage(programStage);
    command.setCurrentPeriodUsedForReporting(true);
    command.setCompletenessMethod(CompletenessMethod.ALL_DATAVALUE);
    command.setWrongFormatMessage("wrong format A");
    command.setNoUserMessage("no user A");
    command.setMoreThanOneOrgUnitMessage("more than one A");
    command.setSuccessMessage("success A");
    smsCommandStore.save(command);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select uid, name, created, lastupdated, parsertype, separatorkey, codeseparator,"
                + " defaultmessage, receivedmessage, wrongformatmessage, nousermessage,"
                + " morethanoneorgunitmessage, successmessage, currentperiodusedforreporting,"
                + " completenessmethod, datasetid, usergroupid, programid, programstageid"
                + " from smscommands where smscommandid = ?",
            command.getId());
    assertEquals(command.getUid(), row.get("uid"));
    assertEquals("Command A", row.get("name"));
    assertEquals(command.getCreated().getTime(), ((Timestamp) row.get("created")).getTime());
    assertEquals(
        command.getLastUpdated().getTime(), ((Timestamp) row.get("lastupdated")).getTime());
    assertEquals("KEY_VALUE_PARSER", row.get("parsertype"));
    assertEquals(".", row.get("separatorkey"));
    assertEquals("=", row.get("codeseparator"));
    assertEquals("default A", row.get("defaultmessage"));
    assertEquals("received A", row.get("receivedmessage"));
    assertEquals("wrong format A", row.get("wrongformatmessage"));
    assertEquals("no user A", row.get("nousermessage"));
    assertEquals("more than one A", row.get("morethanoneorgunitmessage"));
    assertEquals("success A", row.get("successmessage"));
    assertTrue((Boolean) row.get("currentperiodusedforreporting"));
    assertEquals("ALL_DATAVALUE", row.get("completenessmethod"));
    assertEquals(dataSet.getId(), ((Number) row.get("datasetid")).longValue());
    assertEquals(userGroup.getId(), ((Number) row.get("usergroupid")).longValue());
    assertEquals(program.getId(), ((Number) row.get("programid")).longValue());
    assertEquals(programStage.getId(), ((Number) row.get("programstageid")).longValue());
    assertEquals(
        List.of((long) code.getId()),
        jdbcTemplate.queryForList(
            "select codeid from smscommandcodes where id = ?", Long.class, command.getId()));
    assertEquals(
        List.of((long) specialCharacter.getId()),
        jdbcTemplate.queryForList(
            "select specialcharacterid from smscommandspecialcharacters where smscommandid = ?",
            Long.class,
            command.getId()));

    SMSCommand loaded = reload(command);
    assertEquals("Command A", loaded.getName());
    assertEquals(command.getCreated().getTime(), loaded.getCreated().getTime());
    assertEquals(command.getLastUpdated().getTime(), loaded.getLastUpdated().getTime());
    assertEquals(ParserType.KEY_VALUE_PARSER, loaded.getParserType());
    assertEquals(".", loaded.getSeparator());
    assertEquals("=", loaded.getCodeValueSeparator());
    assertEquals("default A", loaded.getDefaultMessage());
    assertEquals("received A", loaded.getReceivedMessage());
    assertEquals("wrong format A", loaded.getWrongFormatMessage());
    assertEquals("no user A", loaded.getNoUserMessage());
    assertEquals("more than one A", loaded.getMoreThanOneOrgUnitMessage());
    assertEquals("success A", loaded.getSuccessMessage());
    assertTrue(loaded.isCurrentPeriodUsedForReporting());
    assertEquals(CompletenessMethod.ALL_DATAVALUE, loaded.getCompletenessMethod());
    assertEquals(dataSet.getUid(), loaded.getDataset().getUid());
    assertEquals(userGroup.getUid(), loaded.getUserGroup().getUid());
    assertEquals(program.getUid(), loaded.getProgram().getUid());
    assertEquals(programStage.getUid(), loaded.getProgramStage().getUid());
    SMSCode loadedCode = loaded.getCodes().iterator().next();
    assertEquals(1, loaded.getCodes().size());
    assertEquals("a", loadedCode.getCode());
    assertEquals("+1", loadedCode.getFormula());
    assertTrue(loadedCode.isCompulsory());
    SMSSpecialCharacter loadedSpecialCharacter = loaded.getSpecialCharacters().iterator().next();
    assertEquals(1, loaded.getSpecialCharacters().size());
    assertEquals("plus", loadedSpecialCharacter.getName());
    assertEquals("+", loadedSpecialCharacter.getValue());
  }

  @Test
  void shouldWriteDefaultMessagesWhenNoneAreSet() {
    SMSCommand command = new SMSCommand();
    command.setName("Command B");
    smsCommandStore.save(command);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select defaultmessage, receivedmessage, wrongformatmessage, nousermessage,"
                + " morethanoneorgunitmessage, successmessage, parsertype, completenessmethod,"
                + " currentperiodusedforreporting from smscommands where smscommandid = ?",
            command.getId());
    assertNull(row.get("defaultmessage"));
    assertEquals(SMSCommand.SUCCESS_MESSAGE, row.get("receivedmessage"));
    assertEquals(SMSCommand.WRONG_FORMAT_MESSAGE, row.get("wrongformatmessage"));
    assertEquals(SMSCommand.NO_USER_MESSAGE, row.get("nousermessage"));
    assertEquals(SMSCommand.MORE_THAN_ONE_ORGUNIT_MESSAGE, row.get("morethanoneorgunitmessage"));
    assertEquals(SMSCommand.SUCCESS_MESSAGE, row.get("successmessage"));
    assertEquals("ALERT_PARSER", row.get("parsertype"));
    assertEquals("AT_LEAST_ONE_DATAVALUE", row.get("completenessmethod"));
    assertFalse((Boolean) row.get("currentperiodusedforreporting"));
  }

  @Test
  void shouldCascadeDeleteToCodesAndSpecialCharacters() {
    SMSCode code = new SMSCode("b", createDataElementAndSave('B'), null);
    SMSSpecialCharacter specialCharacter = new SMSSpecialCharacter("minus", "-");
    SMSCommand command = new SMSCommand();
    command.setName("Command C");
    command.setCodes(Set.of(code));
    command.setSpecialCharacters(Set.of(specialCharacter));
    smsCommandStore.save(command);
    entityManager.flush();
    assertEquals(1, countRows("smscodes where smscodeid = ?", code.getId()));
    assertEquals(
        1, countRows("smsspecialcharacter where specialcharacterid = ?", specialCharacter.getId()));

    smsCommandStore.delete(reload(command));
    entityManager.flush();

    assertEquals(0, countRows("smscommands where smscommandid = ?", command.getId()));
    assertEquals(0, countRows("smscommandcodes where id = ?", command.getId()));
    assertEquals(
        0, countRows("smscommandspecialcharacters where smscommandid = ?", command.getId()));
    assertEquals(0, countRows("smscodes where smscodeid = ?", code.getId()));
    assertEquals(
        0, countRows("smsspecialcharacter where specialcharacterid = ?", specialCharacter.getId()));
  }

  @Test
  void shouldFindCommandsByDataSetAndByNameAndParserType() {
    DataSet dataSet = createDataSet('B');
    dataSetService.addDataSet(dataSet);
    SMSCommand command = new SMSCommand();
    command.setName("Report Command D");
    command.setParserType(ParserType.KEY_VALUE_PARSER);
    command.setDataset(dataSet);
    smsCommandStore.save(command);
    entityManager.flush();
    clearSession();

    assertEquals(1, smsCommandStore.countDataSetSmsCommands(dataSet));
    assertEquals(
        command.getUid(),
        smsCommandStore.getSMSCommand("command d", ParserType.KEY_VALUE_PARSER).getUid());
    assertNull(smsCommandStore.getSMSCommand("command d", ParserType.ALERT_PARSER));
  }

  @Test
  void shouldEqualUninitializedProxyOfSameCommand() {
    SMSCommand command = new SMSCommand();
    command.setName("Command E");
    smsCommandStore.save(command);
    entityManager.flush();
    clearSession();

    SMSCommand proxy = entityManager.getReference(SMSCommand.class, command.getId());

    assertFalse(Hibernate.isInitialized(proxy));
    assertEquals(proxy, command);
    assertEquals(command, proxy);
    assertEquals(command.hashCode(), proxy.hashCode());
  }

  private SMSCommand reload(SMSCommand command) {
    clearSession();
    return smsCommandStore.getByUid(command.getUid());
  }

  private int countRows(String fromWhere, long id) {
    return jdbcTemplate.queryForObject("select count(*) from " + fromWhere, Integer.class, id);
  }

  private boolean isNullable(String property) {
    AbstractEntityPersister persister = persister();
    return persister
        .getPropertyNullability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }

  private AbstractEntityPersister persister() {
    return (AbstractEntityPersister)
        entityManagerFactory
            .unwrap(SessionFactoryImplementor.class)
            .getMetamodel()
            .entityPersister(SMSCommand.class);
  }

  private AbstractCollectionPersister collectionPersister(String property) {
    return (AbstractCollectionPersister)
        entityManagerFactory
            .unwrap(SessionFactoryImplementor.class)
            .getMetamodel()
            .collectionPersister(SMSCommand.class.getName() + "." + property);
  }

  private DataElement createDataElementAndSave(char c) {
    DataElement de = createDataElement(c);
    dataElementService.addDataElement(de);
    return de;
  }

  private SMSCode createSMSCodeAndSave(DataElement de, String code) {
    SMSCode smsCode = new SMSCode();
    smsCode.setCode("Code " + code);
    smsCode.setDataElement(de);

    SMSCommand smsCommand = new SMSCommand();
    smsCommand.setCodes(Set.of(smsCode));
    smsCommand.setName("CMD " + code);
    smsCommandStore.save(smsCommand);
    return smsCode;
  }
}
