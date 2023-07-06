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

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;

/**
 * @author Lars Helge Overland
 */
public class DataApprovalMinLevelResourceTable extends ResourceTable<OrganisationUnitLevel> {
  private final String tableType;

  public DataApprovalMinLevelResourceTable(List<OrganisationUnitLevel> objects, String tableType) {
    super(objects);
    this.tableType = tableType;
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.DATA_APPROVAL_MIN_LEVEL;
  }

  @Override
  public String getCreateTempTableStatement() {
    return "create "
        + tableType
        + " table "
        + getTempTableName()
        + "("
        + "workflowid bigint not null, "
        + "periodid bigint not null, "
        + "organisationunitid bigint not null, "
        + "attributeoptioncomboid bigint not null, "
        + "minlevel integer not null, "
        + "primary key (workflowid,periodid,attributeoptioncomboid,organisationunitid))";
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    String sql =
        "insert into "
            + getTempTableName()
            + " (workflowid,periodid,organisationunitid,attributeoptioncomboid,minlevel) "
            + "select da.workflowid, da.periodid, da.organisationunitid, da.attributeoptioncomboid, dal.level as minlevel "
            + "from dataapproval da "
            + "inner join _dataapprovalremaplevel dal on dal.workflowid=da.workflowid and dal.dataapprovallevelid=da.dataapprovallevelid "
            + "inner join _orgunitstructure ous on da.organisationunitid=ous.organisationunitid "
            + "where not exists ( "
            + "select 1 from dataapproval da2 "
            + "inner join _dataapprovalremaplevel dal2 on da2.workflowid = dal2.workflowid and da2.dataapprovallevelid=dal2.dataapprovallevelid "
            + "where da.workflowid=da2.workflowid "
            + "and da.periodid=da2.periodid "
            + "and da.attributeoptioncomboid=da2.attributeoptioncomboid "
            + "and dal.level > dal2.level "
            + "and ( ";

    for (OrganisationUnitLevel level : objects) {
      sql += "ous.idlevel" + level.getLevel() + " = da2.organisationunitid or ";
    }

    sql = TextUtils.removeLastOr(sql) + ") )";

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
