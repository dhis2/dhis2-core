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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSSpecialCharacter;
import org.hisp.dhis.sms.command.hibernate.SMSCommandStore;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guards the JPA store mapping of {@link SMSSpecialCharacter} (columns, id generation and the
 * cascade from {@link SMSCommand}), formerly mapped in HBM.
 */
@Transactional
class SMSSpecialCharacterStoreTest extends PostgresIntegrationTestBase {

  @Autowired private SMSCommandStore commandStore;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("special characters are saved by cascade with a generated id and reload intact")
  void testSaveByCascadeAndReload() {
    SMSSpecialCharacter plus = new SMSSpecialCharacter("plus", "+");
    SMSSpecialCharacter hash = new SMSSpecialCharacter("hash", "#");
    long commandId = saveCommand("CmdSave", plus, hash);

    assertTrue(plus.getId() > 0);
    assertTrue(hash.getId() > 0);
    assertNotEquals(plus.getId(), hash.getId());

    clearSession();

    SMSCommand reloaded = commandStore.get(commandId);
    assertEquals(Map.of("plus", "+", "hash", "#"), namesToValues(reloaded.getSpecialCharacters()));
    assertEquals(Set.of(plus.getId(), hash.getId()), joinRowIds(commandId));
  }

  @Test
  @DisplayName("name and value are text columns: values longer than 255 characters round-trip")
  void testTextColumns() {
    String longName = "n".repeat(1000);
    String longValue = "v".repeat(1000);
    SMSSpecialCharacter character = new SMSSpecialCharacter(longName, longValue);
    saveCommand("CmdText", character);

    clearSession();

    SMSSpecialCharacter reloaded = entityManager.find(SMSSpecialCharacter.class, character.getId());
    assertEquals(longName, reloaded.getName());
    assertEquals(longValue, reloaded.getValue());
  }

  @Test
  @DisplayName("name and value are nullable")
  void testNullableColumns() {
    SMSSpecialCharacter character = new SMSSpecialCharacter(null, null);
    saveCommand("CmdNull", character);

    clearSession();

    SMSSpecialCharacter reloaded = entityManager.find(SMSSpecialCharacter.class, character.getId());
    assertNull(reloaded.getName());
    assertNull(reloaded.getValue());
  }

  @Test
  @DisplayName("updating name and value of a loaded special character is persisted")
  void testUpdate() {
    SMSSpecialCharacter character = new SMSSpecialCharacter("old", "o");
    long commandId = saveCommand("CmdUpdate", character);

    clearSession();

    SMSCommand loaded = commandStore.get(commandId);
    SMSSpecialCharacter loadedCharacter = loaded.getSpecialCharacters().iterator().next();
    loadedCharacter.setName("new");
    loadedCharacter.setValue("n");
    commandStore.update(loaded);

    clearSession();

    SMSSpecialCharacter reloaded = entityManager.find(SMSSpecialCharacter.class, character.getId());
    assertEquals("new", reloaded.getName());
    assertEquals("n", reloaded.getValue());
  }

  @Test
  @DisplayName("equality is by identity: two characters with equal name and value are both kept")
  void testEqualContentCharactersAreDistinct() {
    SMSSpecialCharacter first = new SMSSpecialCharacter("dup", "d");
    SMSSpecialCharacter second = new SMSSpecialCharacter("dup", "d");
    long commandId = saveCommand("CmdDup", first, second);

    clearSession();

    assertEquals(2, commandStore.get(commandId).getSpecialCharacters().size());
    assertEquals(Set.of(first.getId(), second.getId()), joinRowIds(commandId));
  }

  @Test
  @DisplayName(
      "removing a character from the command deletes its join row but keeps the orphaned"
          + " smsspecialcharacter row")
  void testRemoveFromCommand() {
    SMSSpecialCharacter keep = new SMSSpecialCharacter("keep", "k");
    SMSSpecialCharacter remove = new SMSSpecialCharacter("remove", "r");
    long commandId = saveCommand("CmdRemove", keep, remove);

    clearSession();

    SMSCommand loaded = commandStore.get(commandId);
    loaded.getSpecialCharacters().removeIf(c -> "remove".equals(c.getName()));
    commandStore.update(loaded);

    clearSession();

    assertEquals(Set.of(keep.getId()), joinRowIds(commandId));
    // intentional parity with the HBM mapping: the many-to-many has no orphan removal
    assertEquals(1, countCharacterRows(remove.getId()));
  }

  @Test
  @DisplayName("deleting the command cascades to its special characters")
  void testDeleteCommandCascades() {
    SMSSpecialCharacter character = new SMSSpecialCharacter("gone", "g");
    long commandId = saveCommand("CmdDelete", character);

    clearSession();

    commandStore.delete(commandStore.get(commandId));
    entityManager.flush();

    assertEquals(Set.of(), joinRowIds(commandId));
    assertEquals(0, countCharacterRows(character.getId()));
  }

  private long saveCommand(String name, SMSSpecialCharacter... characters) {
    SMSCommand command = new SMSCommand();
    command.setName(name);
    command.setParserType(ParserType.KEY_VALUE_PARSER);
    command.setSpecialCharacters(new HashSet<>(List.of(characters)));
    commandStore.save(command);
    entityManager.flush();
    return command.getId();
  }

  private static Map<String, String> namesToValues(Set<SMSSpecialCharacter> characters) {
    Map<String, String> result = new HashMap<>();
    characters.forEach(c -> result.put(c.getName(), c.getValue()));
    return result;
  }

  private Set<Integer> joinRowIds(long commandId) {
    return new HashSet<>(
        jdbcTemplate.queryForList(
            "select specialcharacterid from smscommandspecialcharacters where smscommandid = ?",
            Integer.class,
            commandId));
  }

  private int countCharacterRows(int id) {
    return jdbcTemplate.queryForObject(
        "select count(*) from smsspecialcharacter where specialcharacterid = ?", Integer.class, id);
  }
}
