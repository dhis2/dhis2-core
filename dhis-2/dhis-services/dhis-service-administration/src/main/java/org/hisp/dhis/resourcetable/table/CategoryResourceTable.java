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
package org.hisp.dhis.resourcetable.table;

import static org.hisp.dhis.system.util.SqlUtils.quote;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;

/**
 * @author Lars Helge Overland
 */
public class CategoryResourceTable extends ResourceTable<Category> {
  private final List<CategoryOptionGroupSet> groupSets;

  private final String tableType;

  public CategoryResourceTable(
      List<Category> objects, List<CategoryOptionGroupSet> groupSets, String tableType) {
    super(objects);
    this.groupSets = groupSets;
    this.tableType = tableType;
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.CATEGORY_STRUCTURE;
  }

  @Override
  public String getCreateTempTableStatement() {
    String statement =
        "create "
            + tableType
            + " table "
            + getTempTableName()
            + " ("
            + "categoryoptioncomboid bigint not null, "
            + "categoryoptioncomboname varchar(255), ";

    UniqueNameContext nameContext = new UniqueNameContext();

    for (Category category : objects) {
      statement += quote(nameContext.uniqueName(category.getShortName())) + " varchar(230), ";
      statement += quote(category.getUid()) + " character(11), ";
    }

    for (CategoryOptionGroupSet groupSet : groupSets) {
      statement += quote(nameContext.uniqueName(groupSet.getShortName())) + " varchar(230), ";
      statement += quote(groupSet.getUid()) + " character(11), ";
    }

    statement += "primary key (categoryoptioncomboid))";

    return statement;
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    String sql =
        "insert into "
            + getTempTableName()
            + " "
            + "select coc.categoryoptioncomboid as cocid, coc.name as cocname, ";

    for (Category category : objects) {
      sql +=
          "("
              + "select co.name from categoryoptioncombos_categoryoptions cocco "
              + "inner join dataelementcategoryoption co on cocco.categoryoptionid = co.categoryoptionid "
              + "inner join categories_categoryoptions cco on co.categoryoptionid = cco.categoryoptionid "
              + "where coc.categoryoptioncomboid = cocco.categoryoptioncomboid "
              + "and cco.categoryid = "
              + category.getId()
              + " "
              + "limit 1) as "
              + quote(category.getName())
              + ", ";

      sql +=
          "("
              + "select co.uid from categoryoptioncombos_categoryoptions cocco "
              + "inner join dataelementcategoryoption co on cocco.categoryoptionid = co.categoryoptionid "
              + "inner join categories_categoryoptions cco on co.categoryoptionid = cco.categoryoptionid "
              + "where coc.categoryoptioncomboid = cocco.categoryoptioncomboid "
              + "and cco.categoryid = "
              + category.getId()
              + " "
              + "limit 1) as "
              + quote(category.getUid())
              + ", ";
    }

    for (CategoryOptionGroupSet groupSet : groupSets) {
      sql +=
          "("
              + "select cog.name from categoryoptioncombos_categoryoptions cocco "
              + "inner join categoryoptiongroupmembers cogm on cocco.categoryoptionid = cogm.categoryoptionid "
              + "inner join categoryoptiongroup cog on cogm.categoryoptiongroupid = cog.categoryoptiongroupid "
              + "inner join categoryoptiongroupsetmembers cogsm on "
              + "cogm.categoryoptiongroupid = cogsm.categoryoptiongroupid "
              + "where coc.categoryoptioncomboid = cocco.categoryoptioncomboid "
              + "and cogsm.categoryoptiongroupsetid = "
              + groupSet.getId()
              + " "
              + "limit 1) as "
              + quote(groupSet.getName())
              + ", ";

      sql +=
          "("
              + "select cog.uid from categoryoptioncombos_categoryoptions cocco "
              + "inner join categoryoptiongroupmembers cogm on cocco.categoryoptionid = cogm.categoryoptionid "
              + "inner join categoryoptiongroup cog on cogm.categoryoptiongroupid = cog.categoryoptiongroupid "
              + "inner join categoryoptiongroupsetmembers cogsm on "
              + "cogm.categoryoptiongroupid = cogsm.categoryoptiongroupid "
              + "where coc.categoryoptioncomboid = cocco.categoryoptioncomboid "
              + "and cogsm.categoryoptiongroupsetid = "
              + groupSet.getId()
              + " "
              + "limit 1) as "
              + quote(groupSet.getUid())
              + ", ";
    }

    sql = TextUtils.removeLastComma(sql) + " ";
    sql += "from categoryoptioncombo coc ";

    return Optional.of(sql);
  }

  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    return Optional.empty();
  }

  @Override
  public List<String> getCreateIndexStatements() {
    return Lists.newArrayList();
  }
}
