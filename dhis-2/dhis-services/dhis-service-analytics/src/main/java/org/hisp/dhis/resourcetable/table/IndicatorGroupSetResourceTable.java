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

import static org.hisp.dhis.db.model.Table.toStaging;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
public class IndicatorGroupSetResourceTable implements ResourceTable {
  private static final String TABLE_NAME = "_indicatorgroupsetstructure";

  private final SqlBuilder sqlBuilder;

  private final List<IndicatorGroupSet> groupSets;

  private final Logged logged;

  @Override
  public Table getTable() {
    return new Table(toStaging(TABLE_NAME), getColumns(), getPrimaryKey(), logged);
  }

  private List<Column> getColumns() {
    List<Column> columns =
        Lists.newArrayList(
            new Column("indicatorid", DataType.BIGINT, Nullable.NOT_NULL),
            new Column("indicatorname", DataType.VARCHAR_255, Nullable.NOT_NULL));

    for (IndicatorGroupSet groupSet : groupSets) {
      columns.addAll(
          List.of(
              new Column(groupSet.getShortName(), DataType.VARCHAR_255),
              new Column(groupSet.getUid(), DataType.CHARACTER_11)));
    }

    return columns;
  }

  private List<String> getPrimaryKey() {
    return List.of("indicatorid");
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.INDICATOR_GROUP_SET_STRUCTURE;
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    String sql =
        "insert into "
            + toStaging(TABLE_NAME)
            + " "
            + "select i.indicatorid as indicatorid, i.name as indicatorname, ";

    for (IndicatorGroupSet groupSet : groupSets) {
      sql +=
          "("
              + "select ig.name from indicatorgroup ig "
              + "inner join indicatorgroupmembers igm on igm.indicatorgroupid = ig.indicatorgroupid "
              + "inner join indicatorgroupsetmembers igsm on "
              + "igsm.indicatorgroupid = igm.indicatorgroupid and igsm.indicatorgroupsetid = "
              + groupSet.getId()
              + " "
              + "where igm.indicatorid = i.indicatorid "
              + "limit 1) as "
              + sqlBuilder.quote(groupSet.getName())
              + ", ";

      sql +=
          "("
              + "select ig.uid from indicatorgroup ig "
              + "inner join indicatorgroupmembers igm on "
              + "igm.indicatorgroupid = ig.indicatorgroupid "
              + "inner join indicatorgroupsetmembers igsm on "
              + "igsm.indicatorgroupid = igm.indicatorgroupid and igsm.indicatorgroupsetid = "
              + groupSet.getId()
              + " "
              + "where igm.indicatorid = i.indicatorid "
              + "limit 1) as "
              + sqlBuilder.quote(groupSet.getUid())
              + ", ";
    }

    sql = TextUtils.removeLastComma(sql) + " ";
    sql += "from indicator i";

    return Optional.of(sql);
  }

  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    return Optional.empty();
  }
}
