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
package org.hisp.dhis.db.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test of the {@code trg_trim_name} trigger created by the {@code
 * V2_44_31__add_trim_name_trigger} migration, which trims the {@code name} of metadata on insert
 * and update.
 */
@Transactional
class TrimNameTriggerTest extends PostgresIntegrationTestBase {

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private DbmsManager dbmsManager;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void insertTrimsSurroundingWhitespaceFromNameOnly() {
    DataElement dataElement = createDataElement('A');
    dataElement.setName(" \t Trimmed Name \n ");
    dataElement.setShortName("  Short  ");
    dataElement.setCode("  CODE  ");

    save(dataElement);

    assertEquals("Trimmed Name", selectColumn("name", dataElement));
    assertEquals("  Short  ", selectColumn("shortname", dataElement));
    assertEquals("  CODE  ", selectColumn("code", dataElement));
  }

  @Test
  void insertTrimsVerticalTabButKeepsLetterV() {
    DataElement dataElement = createDataElement('E');
    dataElement.setName("\u000B version v\u000B");

    save(dataElement);

    assertEquals("version v", selectColumn("name", dataElement));
  }

  @Test
  void insertTrimsWhitespaceOnlyNameToEmpty() {
    DataElement dataElement = createDataElement('B');
    dataElement.setName("   ");

    save(dataElement);

    assertEquals("", selectColumn("name", dataElement));
  }

  @Test
  void updateTrimsSurroundingWhitespaceFromName() {
    DataElement dataElement = createDataElement('C');
    save(dataElement);

    jdbcTemplate.update(
        "update dataelement set name = ? where uid = ?",
        " \t Updated Name \n ",
        dataElement.getUid());

    assertEquals("Updated Name", selectColumn("name", dataElement));
  }

  @Test
  void entityUpdateTrimsExistingUntrimmedName() {
    DataElement dataElement = createDataElement('D');
    save(dataElement);
    setNameBypassingTrigger(dataElement, "  Untrimmed  ");

    DataElement persisted = manager.get(DataElement.class, dataElement.getUid());
    persisted.setDescription("changed");
    manager.update(persisted);
    dbmsManager.flushSession();

    assertEquals("Untrimmed", selectColumn("name", dataElement));
  }

  @Test
  void triggerIsAttachedToEveryTableWithUidCodeAndName() {
    List<String> tablesWithoutTrigger =
        jdbcTemplate.queryForList(
            """
            select c.table_name
            from information_schema.columns c
            join information_schema.tables t
              on t.table_schema = c.table_schema and t.table_name = c.table_name
            where c.table_schema = current_schema()
              and t.table_type = 'BASE TABLE'
              and c.column_name = 'name'
              and c.data_type in ('character varying', 'text')
              and exists (
                select 1 from information_schema.columns u
                where u.table_schema = c.table_schema
                  and u.table_name = c.table_name
                  and u.column_name = 'uid')
              and exists (
                select 1 from information_schema.columns k
                where k.table_schema = c.table_schema
                  and k.table_name = c.table_name
                  and k.column_name = 'code')
              and not exists (
                select 1 from pg_trigger tg
                where tg.tgrelid = to_regclass(quote_ident(c.table_name))
                  and tg.tgname = 'trg_trim_name')
            """,
            String.class);

    assertTrue(
        tablesWithoutTrigger.isEmpty(),
        "Tables missing trg_trim_name: "
            + tablesWithoutTrigger
            + ". Attach it in the migration creating the table: CREATE TRIGGER trg_trim_name"
            + " BEFORE INSERT OR UPDATE OF name ON <table> FOR EACH ROW EXECUTE FUNCTION trim_metadata_name();");
  }

  @Test
  void triggerIsAttachedToMetadataTablesOnly() {
    List<String> tablesWithTrigger =
        jdbcTemplate.queryForList(
            "select tgrelid::regclass::text from pg_trigger where tgname = 'trg_trim_name'",
            String.class);

    assertTrue(
        tablesWithTrigger.containsAll(
            List.of("dataelement", "organisationunit", "dataset", "program", "indicator")),
        "Metadata tables missing trg_trim_name: " + tablesWithTrigger);
    assertFalse(tablesWithTrigger.contains("datavalue"), "datavalue has no name column");
  }

  private void save(DataElement dataElement) {
    manager.save(dataElement);
    dbmsManager.flushSession();
    dbmsManager.clearSession();
  }

  private void setNameBypassingTrigger(DataElement dataElement, String name) {
    jdbcTemplate.execute("alter table dataelement disable trigger trg_trim_name");
    jdbcTemplate.update(
        "update dataelement set name = ? where uid = ?", name, dataElement.getUid());
    jdbcTemplate.execute("alter table dataelement enable trigger trg_trim_name");
  }

  private String selectColumn(String column, DataElement dataElement) {
    return jdbcTemplate.queryForObject(
        "select " + column + " from dataelement where uid = ?", String.class, dataElement.getUid());
  }
}
