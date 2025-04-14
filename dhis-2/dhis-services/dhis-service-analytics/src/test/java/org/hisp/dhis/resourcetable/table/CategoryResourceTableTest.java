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
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryResourceTableTest extends BaseResourceTableTest {

  private CategoryResourceTable categoryResourceTable;
  private final List<Category> categories =
      List.of(createCategory('A'), createCategory('B'), createCategory('C'));

  private final List<CategoryOptionGroupSet> groupSets =
      List.of(
          withId(createCategoryOptionGroupSet('E'), 1000),
          withId(createCategoryOptionGroupSet('F'), 2000),
          withId(createCategoryOptionGroupSet('G'), 3000));

  @BeforeEach
  void setUp() {
    categoryResourceTable = new CategoryResourceTable(Logged.UNLOGGED, categories, groupSets);
  }

  @Test
  void testGetTable() {
    Table table = categoryResourceTable.getTable();
    assertNotNull(table);
    assertEquals("analytics_rs_categorystructure_temp", table.getName());
    assertEquals(15, table.getColumns().size());
    assertEquals("categoryoptioncomboid", table.getColumns().get(0).getName());
    assertEquals("categoryoptioncombouid", table.getColumns().get(1).getName());
    assertEquals("categoryoptioncomboname", table.getColumns().get(2).getName());
    assertEquals(categories.get(0).getName(), table.getColumns().get(3).getName());
    assertEquals(categories.get(0).getUid(), table.getColumns().get(4).getName());
    assertEquals(categories.get(1).getName(), table.getColumns().get(5).getName());
    assertEquals(categories.get(1).getUid(), table.getColumns().get(6).getName());
    assertEquals(categories.get(2).getName(), table.getColumns().get(7).getName());
    assertEquals(categories.get(2).getUid(), table.getColumns().get(8).getName());
    assertEquals(groupSets.get(0).getName(), table.getColumns().get(9).getName());
    assertEquals(groupSets.get(0).getUid(), table.getColumns().get(10).getName());
    assertEquals(groupSets.get(1).getName(), table.getColumns().get(11).getName());
    assertEquals(groupSets.get(1).getUid(), table.getColumns().get(12).getName());
    assertEquals(groupSets.get(2).getName(), table.getColumns().get(13).getName());
    assertEquals(groupSets.get(2).getUid(), table.getColumns().get(14).getName());
  }

  @Test
  void testPopulateTableStatement() throws JSQLParserException {
    Optional<String> sql = categoryResourceTable.getPopulateTempTableStatement();
    assertTrue(sql.isPresent());

    Statement statement = CCJSqlParserUtil.parse(sql.get());
    assertInstanceOf(Insert.class, statement, "Statement should be an INSERT statement");

    Insert insertStatement = (Insert) statement;
    assertEquals(
        "analytics_rs_categorystructure_temp",
        insertStatement.getTable().getName(),
        "Insert target table should be analytics_rs_categorystructure_temp");

    // 2. Validate the Select statement
    assertNotNull(insertStatement.getSelect(), "Insert should contain a SELECT statement");
    PlainSelect selectStatement = (PlainSelect) insertStatement.getSelect().getSelectBody();
    // 3. Check FROM table
    assertEquals(
        "categoryoptioncombo",
        selectStatement.getFromItem().toString().split(" ")[0],
        "FROM clause should reference categoryoptioncombo table");
    assertEquals(
        "coc",
        selectStatement.getFromItem().getAlias().getName(),
        "FROM clause should use alias 'coc'");

    List<ExpessionAlias> expectedColumnAliases =
        List.of(
            new ExpessionAlias("cocid", "coc.categoryoptioncomboid"),
            new ExpessionAlias("cocuid", "coc.uid"),
            new ExpessionAlias("cocname", "coc.name"),
            new ExpessionAlias(
                categories.get(0).getName(),
                "(select co.name from categoryoptioncombos_categoryoptions cocco inner join categoryoption co on cocco.categoryoptionid = co.categoryoptionid inner join categories_categoryoptions cco on co.categoryoptionid = cco.categoryoptionid where coc.categoryoptioncomboid = cocco.categoryoptioncomboid and cco.categoryid = 0 limit 1)"),
            new ExpessionAlias(
                categories.get(0).getUid(),
                "(select co.uid from categoryoptioncombos_categoryoptions cocco inner join categoryoption co on cocco.categoryoptionid = co.categoryoptionid inner join categories_categoryoptions cco on co.categoryoptionid = cco.categoryoptionid where coc.categoryoptioncomboid = cocco.categoryoptioncomboid and cco.categoryid = 0 limit 1)"),
            new ExpessionAlias(
                categories.get(1).getName(),
                "(select co.name from categoryoptioncombos_categoryoptions cocco inner join categoryoption co on cocco.categoryoptionid = co.categoryoptionid inner join categories_categoryoptions cco on co.categoryoptionid = cco.categoryoptionid where coc.categoryoptioncomboid = cocco.categoryoptioncomboid and cco.categoryid = 0 limit 1)"),
            new ExpessionAlias(
                categories.get(1).getUid(),
                "(select co.uid from categoryoptioncombos_categoryoptions cocco inner join categoryoption co on cocco.categoryoptionid = co.categoryoptionid inner join categories_categoryoptions cco on co.categoryoptionid = cco.categoryoptionid where coc.categoryoptioncomboid = cocco.categoryoptioncomboid and cco.categoryid = 0 limit 1)"),
            new ExpessionAlias(
                categories.get(2).getName(),
                "(select co.name from categoryoptioncombos_categoryoptions cocco inner join categoryoption co on cocco.categoryoptionid = co.categoryoptionid inner join categories_categoryoptions cco on co.categoryoptionid = cco.categoryoptionid where coc.categoryoptioncomboid = cocco.categoryoptioncomboid and cco.categoryid = 0 limit 1)"),
            new ExpessionAlias(
                categories.get(2).getUid(),
                "(select co.uid from categoryoptioncombos_categoryoptions cocco inner join categoryoption co on cocco.categoryoptionid = co.categoryoptionid inner join categories_categoryoptions cco on co.categoryoptionid = cco.categoryoptionid where coc.categoryoptioncomboid = cocco.categoryoptioncomboid and cco.categoryid = 0 limit 1)"),
            new ExpessionAlias(
                groupSets.get(0).getName(),
                "(select cog.name from categoryoptioncombos_categoryoptions cocco inner join categoryoptiongroupmembers cogm on cocco.categoryoptionid = cogm.categoryoptionid inner join categoryoptiongroup cog on cogm.categoryoptiongroupid = cog.categoryoptiongroupid inner join categoryoptiongroupsetmembers cogsm on cogm.categoryoptiongroupid = cogsm.categoryoptiongroupid where coc.categoryoptioncomboid = cocco.categoryoptioncomboid and cogsm.categoryoptiongroupsetid = 1000 limit 1)"),
            new ExpessionAlias(
                groupSets.get(0).getUid(),
                "(select cog.uid from categoryoptioncombos_categoryoptions cocco inner join categoryoptiongroupmembers cogm on cocco.categoryoptionid = cogm.categoryoptionid inner join categoryoptiongroup cog on cogm.categoryoptiongroupid = cog.categoryoptiongroupid inner join categoryoptiongroupsetmembers cogsm on cogm.categoryoptiongroupid = cogsm.categoryoptiongroupid where coc.categoryoptioncomboid = cocco.categoryoptioncomboid and cogsm.categoryoptiongroupsetid = 1000 limit 1)"),
            new ExpessionAlias(
                groupSets.get(1).getName(),
                "(select cog.name from categoryoptioncombos_categoryoptions cocco inner join categoryoptiongroupmembers cogm on cocco.categoryoptionid = cogm.categoryoptionid inner join categoryoptiongroup cog on cogm.categoryoptiongroupid = cog.categoryoptiongroupid inner join categoryoptiongroupsetmembers cogsm on cogm.categoryoptiongroupid = cogsm.categoryoptiongroupid where coc.categoryoptioncomboid = cocco.categoryoptioncomboid and cogsm.categoryoptiongroupsetid = 2000 limit 1)"),
            new ExpessionAlias(
                groupSets.get(1).getUid(),
                "(select cog.uid from categoryoptioncombos_categoryoptions cocco inner join categoryoptiongroupmembers cogm on cocco.categoryoptionid = cogm.categoryoptionid inner join categoryoptiongroup cog on cogm.categoryoptiongroupid = cog.categoryoptiongroupid inner join categoryoptiongroupsetmembers cogsm on cogm.categoryoptiongroupid = cogsm.categoryoptiongroupid where coc.categoryoptioncomboid = cocco.categoryoptioncomboid and cogsm.categoryoptiongroupsetid = 2000 limit 1)"),
            new ExpessionAlias(
                groupSets.get(2).getName(),
                "(select cog.name from categoryoptioncombos_categoryoptions cocco inner join categoryoptiongroupmembers cogm on cocco.categoryoptionid = cogm.categoryoptionid inner join categoryoptiongroup cog on cogm.categoryoptiongroupid = cog.categoryoptiongroupid inner join categoryoptiongroupsetmembers cogsm on cogm.categoryoptiongroupid = cogsm.categoryoptiongroupid where coc.categoryoptioncomboid = cocco.categoryoptioncomboid and cogsm.categoryoptiongroupsetid = 3000 limit 1)"),
            new ExpessionAlias(
                groupSets.get(2).getUid(),
                "(select cog.uid from categoryoptioncombos_categoryoptions cocco inner join categoryoptiongroupmembers cogm on cocco.categoryoptionid = cogm.categoryoptionid inner join categoryoptiongroup cog on cogm.categoryoptiongroupid = cog.categoryoptiongroupid inner join categoryoptiongroupsetmembers cogsm on cogm.categoryoptiongroupid = cogsm.categoryoptiongroupid where coc.categoryoptioncomboid = cocco.categoryoptioncomboid and cogsm.categoryoptiongroupsetid = 3000 limit 1)"));

    verifyPopulateStatement(selectStatement.getSelectItems(), expectedColumnAliases);
  }

  private CategoryOptionGroupSet withId(CategoryOptionGroupSet groupSet, int id) {
    groupSet.setId(id);
    return groupSet;
  }
}
