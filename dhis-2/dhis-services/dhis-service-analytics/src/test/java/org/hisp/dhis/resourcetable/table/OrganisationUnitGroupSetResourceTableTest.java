/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.resourcetable.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganisationUnitGroupSetResourceTableTest extends BaseResourceTableTest {

  private OrganisationUnitGroupSetResourceTable organisationUnitGroupSetResourceTable;

  private final List<OrganisationUnitGroupSet> groupSets =
      List.of(
          withId(createOrganisationUnitGroupSet('A'), 1000),
          withId(createOrganisationUnitGroupSet('B'), 2000),
          withId(createOrganisationUnitGroupSet('C'), 3000));

  @BeforeEach
  void setUp() {
    organisationUnitGroupSetResourceTable =
        new OrganisationUnitGroupSetResourceTable(Logged.UNLOGGED, groupSets, 1);
  }

  @Test
  void testGetTable() {
    Table table = organisationUnitGroupSetResourceTable.getTable();
    assertNotNull(table);
    assertEquals("analytics_rs_organisationunitgroupsetstructure_temp", table.getName());
    assertEquals(9, table.getColumns().size());
    assertEquals("organisationunitid", table.getColumns().get(0).getName());
    assertEquals("organisationunitname", table.getColumns().get(1).getName());
    assertEquals("startdate", table.getColumns().get(2).getName());
    assertEquals(groupSets.get(0).getName(), table.getColumns().get(3).getName());
    assertEquals(groupSets.get(0).getUid(), table.getColumns().get(4).getName());
    assertEquals(groupSets.get(1).getName(), table.getColumns().get(5).getName());
    assertEquals(groupSets.get(1).getUid(), table.getColumns().get(6).getName());
    assertEquals(groupSets.get(2).getName(), table.getColumns().get(7).getName());
    assertEquals(groupSets.get(2).getUid(), table.getColumns().get(8).getName());
  }

  @Test
  void testPopulateTableStatement() throws JSQLParserException {
    Optional<String> sql = organisationUnitGroupSetResourceTable.getPopulateTempTableStatement();
    assertTrue(sql.isPresent());
    Statement statement = CCJSqlParserUtil.parse(sql.get());
    assertInstanceOf(Insert.class, statement, "Statement should be an INSERT statement");

    Insert insertStatement = (Insert) statement;
    assertEquals(
        "analytics_rs_organisationunitgroupsetstructure_temp",
        insertStatement.getTable().getName(),
        "Insert target table should be analytics_rs_organisationunitgroupsetstructure_temp");

    // 2. Validate the Select statement
    assertNotNull(insertStatement.getSelect(), "Insert should contain a SELECT statement");
    PlainSelect selectStatement = (PlainSelect) insertStatement.getSelect().getSelectBody();
    // 3. Check FROM table
    assertEquals(
        "organisationunit",
        selectStatement.getFromItem().toString().split(" ")[0],
        "FROM clause should reference organisationunit table");
    assertEquals(
        "ou",
        selectStatement.getFromItem().getAlias().getName(),
        "FROM clause should use alias 'ou'");

    String expectedNameExpression =
        "(select oug.name from orgunitgroup oug inner join orgunitgroupmembers ougm on ougm.orgunitgroupid = oug.orgunitgroupid inner join orgunitgroupsetmembers ougsm on ougsm.orgunitgroupid = ougm.orgunitgroupid and ougsm.orgunitgroupsetid = %s where ougm.organisationunitid = ou.organisationunitid limit 1)";

    String expectedExpression =
        "(select oug.uid from orgunitgroup oug inner join orgunitgroupmembers ougm on ougm.orgunitgroupid = oug.orgunitgroupid inner join orgunitgroupsetmembers ougsm on ougsm.orgunitgroupid = ougm.orgunitgroupid and ougsm.orgunitgroupsetid = %s where ougm.organisationunitid = ou.organisationunitid limit 1)";

    List<ExpessionAlias> expectedColumnAliases =
        List.of(
            new ExpessionAlias("organisationunitid", "ou.organisationunitid"),
            new ExpessionAlias("organisationunitname", "ou.name"),
            new ExpessionAlias("startdate", "null"),
            new ExpessionAlias(
                groupSets.get(0).getName(), expectedNameExpression.formatted("1000")),
            new ExpessionAlias(groupSets.get(0).getUid(), expectedExpression.formatted("1000")),
            new ExpessionAlias(
                groupSets.get(1).getName(), expectedNameExpression.formatted("2000")),
            new ExpessionAlias(groupSets.get(1).getUid(), expectedExpression.formatted("2000")),
            new ExpessionAlias(
                groupSets.get(2).getName(), expectedNameExpression.formatted("3000")),
            new ExpessionAlias(groupSets.get(2).getUid(), expectedExpression.formatted("3000")));
    verifyPopulateStatement(selectStatement.getSelectItems(), expectedColumnAliases);
  }

  private OrganisationUnitGroupSet withId(OrganisationUnitGroupSet groupSet, int id) {
    groupSet.setId(id);
    return groupSet;
  }
}
