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
import static org.hisp.dhis.commons.util.TextUtils.removeLastComma;
import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.db.model.Table.toStaging;
import static org.hisp.dhis.system.util.SqlUtils.appendRandom;
import static org.hisp.dhis.system.util.SqlUtils.quote;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
public class OrganisationUnitGroupSetResourceTable implements ResourceTable {
  public static final String TABLE_NAME = "analytics_rs_organisationunitgroupsetstructure";

  private final Logged logged;

  private final List<OrganisationUnitGroupSet> groupSets;

  private final int organisationUnitLevels;

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
            new Column("organisationunitid", DataType.BIGINT, Nullable.NOT_NULL),
            new Column("organisationunitname", DataType.VARCHAR_255, Nullable.NOT_NULL),
            new Column("startdate", DataType.DATE));

    for (OrganisationUnitGroupSet groupSet : groupSets) {
      columns.addAll(
          List.of(
              new Column(groupSet.getShortName(), DataType.VARCHAR_255),
              new Column(groupSet.getUid(), DataType.CHARACTER_11)));
    }

    return columns;
  }

  private List<String> getPrimaryKey() {
    return List.of("organisationunitid");
  }

  @Override
  public List<Index> getIndexes() {
    return List.of(
        Index.builder()
            .name(appendRandom("in_orgunitgroupsetstructure_not_null"))
            .tableName(toStaging(TABLE_NAME))
            .columns(List.of("organisationunitid", "startdate"))
            .condition("startdate is not null")
            .build(),
        Index.builder()
            .name(appendRandom("in_orgunitgroupsetstructure_not_null"))
            .tableName(toStaging(TABLE_NAME))
            .columns(List.of("organisationunitid", "startdate"))
            .condition("startdate is null")
            .build());
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.ORG_UNIT_GROUP_SET_STRUCTURE;
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    String sql =
        replace(
            """
            insert into ${table_name} \
            select ou.organisationunitid as organisationunitid, ou.name as organisationunitname, null as startdate, \
            """,
            "table_name",
            toStaging(TABLE_NAME));

    for (OrganisationUnitGroupSet groupSet : groupSets) {
      if (!groupSet.isIncludeSubhierarchyInAnalytics()) {
        sql +=
            replace(
                """
                (
                select oug.name from orgunitgroup oug \
                inner join orgunitgroupmembers ougm on ougm.orgunitgroupid = oug.orgunitgroupid \
                inner join orgunitgroupsetmembers ougsm on ougsm.orgunitgroupid = ougm.orgunitgroupid \
                and ougsm.orgunitgroupsetid = ${groupSetId} \
                where ougm.organisationunitid = ou.organisationunitid limit 1) as ${groupSetName}, \
                (
                select oug.uid from orgunitgroup oug \
                inner join orgunitgroupmembers ougm on ougm.orgunitgroupid = oug.orgunitgroupid \
                inner join orgunitgroupsetmembers ougsm on ougsm.orgunitgroupid = ougm.orgunitgroupid \
                and ougsm.orgunitgroupsetid = ${groupSetId} \
                where ougm.organisationunitid = ou.organisationunitid limit 1) as ${groupSetUid}, \
                """,
                Map.of(
                    "groupSetId", valueOf(groupSet.getId()),
                    "groupSetName", quote(groupSet.getName()),
                    "groupSetUid", quote(groupSet.getUid())));
      } else {
        sql += "coalesce(";

        for (int i = organisationUnitLevels; i > 0; i--) {
          sql +=
              replace(
                  """
                  (
                  select oug.name from orgunitgroup oug \
                  inner join orgunitgroupmembers ougm on ougm.orgunitgroupid = oug.orgunitgroupid \
                  and ougm.organisationunitid = ous.idlevel${level} \
                  inner join orgunitgroupsetmembers ougsm on ougsm.orgunitgroupid = ougm.orgunitgroupid \
                  and ougsm.orgunitgroupsetid = ${groupSetId} limit 1), \
                  """,
                  Map.of(
                      "level", valueOf(i),
                      "groupSetId", valueOf(groupSet.getId())));
        }

        if (organisationUnitLevels == 0) {
          sql += "null";
        }

        sql = removeLastComma(sql) + ") as " + quote(groupSet.getName()) + ", ";

        sql += "coalesce(";

        for (int i = organisationUnitLevels; i > 0; i--) {
          sql +=
              replace(
                  """
                  (
                  select oug.uid from orgunitgroup oug \
                  inner join orgunitgroupmembers ougm on ougm.orgunitgroupid = oug.orgunitgroupid \
                  and ougm.organisationunitid = ous.idlevel${level} \
                  inner join orgunitgroupsetmembers ougsm on ougsm.orgunitgroupid = ougm.orgunitgroupid \
                  and ougsm.orgunitgroupsetid = ${groupSetId} limit 1), \
                  """,
                  Map.of(
                      "level", valueOf(i),
                      "groupSetId", valueOf(groupSet.getId())));
        }

        if (organisationUnitLevels == 0) {
          sql += "null";
        }

        sql = removeLastComma(sql) + ") as " + quote(groupSet.getUid()) + ", ";
      }
    }

    sql = removeLastComma(sql) + " ";
    sql +=
        """
        from organisationunit ou \
        inner join analytics_rs_orgunitstructure ous on ous.organisationunitid = ou.organisationunitid;\
        """;

    return Optional.of(sql);
  }

  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    return Optional.empty();
  }
}
