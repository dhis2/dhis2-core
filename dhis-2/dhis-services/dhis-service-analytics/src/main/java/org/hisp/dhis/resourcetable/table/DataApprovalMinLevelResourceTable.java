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

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
public class DataApprovalMinLevelResourceTable implements ResourceTable {
  private static final String TABLE_NAME = "_dataapprovalminlevel";

  private final List<OrganisationUnitLevel> levels;

  private final Logged logged;

  @Override
  public Table getTable() {
    return new Table(toStaging(TABLE_NAME), getColumns(), getPrimaryKey(), logged);
  }

  private List<Column> getColumns() {
    return List.of(
        new Column("workflowid", DataType.BIGINT, Nullable.NOT_NULL),
        new Column("periodid", DataType.BIGINT, Nullable.NOT_NULL),
        new Column("organisationunitid", DataType.BIGINT, Nullable.NOT_NULL),
        new Column("attributeoptioncomboid", DataType.BIGINT, Nullable.NOT_NULL),
        new Column("minlevel", DataType.INTEGER, Nullable.NOT_NULL));
  }

  private List<String> getPrimaryKey() {
    return List.of("workflowid", "periodid", "attributeoptioncomboid", "organisationunitid");
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.DATA_APPROVAL_MIN_LEVEL;
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    String sql =
        "insert into "
            + toStaging(TABLE_NAME)
            + " (workflowid,periodid,organisationunitid,attributeoptioncomboid,minlevel) "
            + "select da.workflowid, da.periodid, da.organisationunitid, "
            + "da.attributeoptioncomboid, dal.level as minlevel "
            + "from dataapproval da "
            + "inner join _dataapprovalremaplevel dal on "
            + "dal.workflowid=da.workflowid and dal.dataapprovallevelid=da.dataapprovallevelid "
            + "inner join _orgunitstructure ous on da.organisationunitid=ous.organisationunitid "
            + "where not exists ( "
            + "select 1 from dataapproval da2 "
            + "inner join _dataapprovalremaplevel dal2 on "
            + "da2.workflowid = dal2.workflowid and da2.dataapprovallevelid=dal2.dataapprovallevelid "
            + "where da.workflowid=da2.workflowid "
            + "and da.periodid=da2.periodid "
            + "and da.attributeoptioncomboid=da2.attributeoptioncomboid "
            + "and dal.level > dal2.level "
            + "and ( ";

    for (OrganisationUnitLevel level : levels) {
      sql += "ous.idlevel" + level.getLevel() + " = da2.organisationunitid or ";
    }

    sql = TextUtils.removeLastOr(sql) + ") )";

    return Optional.of(sql);
  }

  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    return Optional.empty();
  }
}
