/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static java.lang.String.valueOf;
import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.db.model.Table.toStaging;
import static org.hisp.dhis.resourcetable.util.ColumnNameUtils.toValidColumnName;
import static org.hisp.dhis.system.util.SqlUtils.quote;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;
import org.hisp.dhis.resourcetable.util.UniqueNameContext;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
public class IndicatorGroupSetResourceTable implements ResourceTable {
  public static final String TABLE_NAME = "analytics_rs_indicatorgroupsetstructure";

  private final Logged logged;

  private final List<IndicatorGroupSet> groupSets;

  @Override
  public Table getTable() {
    return new Table(toStaging(TABLE_NAME), getColumns(), getPrimaryKey(), logged);
  }

  @Override
  public Table getMainTable() {
    return new Table(TABLE_NAME, getColumns(), getPrimaryKey(), logged);
  }

  private List<Column> getColumns() {
    List<Column> columns =
        Lists.newArrayList(
            new Column("indicatorid", DataType.BIGINT, Nullable.NOT_NULL),
            new Column("indicatorname", DataType.VARCHAR_255, Nullable.NOT_NULL));

    UniqueNameContext nameContext = new UniqueNameContext();

    for (IndicatorGroupSet groupSet : groupSets) {
      columns.addAll(
          List.of(
              new Column(
                  nameContext.uniqueName(toValidColumnName(groupSet.getShortName())),
                  DataType.VARCHAR_255),
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
        replace(
            """
            insert into ${table_name} \
            select i.indicatorid as indicatorid, i.name as indicatorname, \
            """,
            "table_name",
            toStaging(TABLE_NAME));

    for (IndicatorGroupSet groupSet : groupSets) {
      sql +=
          replace(
              """
              (
              select ig.name from indicatorgroup ig \
              inner join indicatorgroupmembers igm on igm.indicatorgroupid = ig.indicatorgroupid \
              inner join indicatorgroupsetmembers igsm on igsm.indicatorgroupid = igm.indicatorgroupid \
              and igsm.indicatorgroupsetid = ${groupSetId} \
              where igm.indicatorid = i.indicatorid limit 1) as ${groupSetName}, \
              (
              select ig.uid from indicatorgroup ig \
              inner join indicatorgroupmembers igm on igm.indicatorgroupid = ig.indicatorgroupid \
              inner join indicatorgroupsetmembers igsm on igsm.indicatorgroupid = igm.indicatorgroupid \
              and igsm.indicatorgroupsetid = ${groupSetId} \
              where igm.indicatorid = i.indicatorid limit 1) as ${groupSetUid}, \
              """,
              Map.of(
                  "groupSetId", valueOf(groupSet.getId()),
                  "groupSetName", quote(toValidColumnName(groupSet.getName())),
                  "groupSetUid", quote(groupSet.getUid())));
    }

    sql = TextUtils.removeLastComma(sql) + " ";
    sql += "from indicator i;";

    return Optional.of(sql);
  }

  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    return Optional.empty();
  }
}
