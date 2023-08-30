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
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;

/**
 * Remaps approval levels within a workflow for analytics tables approved data visibility. This
 * handles the case where a workflow does not include all data approval levels. Where approval
 * levels are skipped by the workflow, they are remapped upwords so that higher-level users can see
 * the approved data.
 *
 * <p>For example, if a workfow includes approval levels 1,2,4, and 5, the approved data will be
 * tagged at levels 1,2,3, and 5. This allows level 2 users to see data in this workflow that is
 * approved at level 4.
 *
 * <p>As another example, if a workflow includes levels 3,4,5, and 7, the approved data will be
 * tagged at levels 1,4,5, and 6. This allows level 1 users to see level 3 approved data, and level
 * 5 users to see level 7 approved data.
 *
 * @author Jim Grace
 */
public class DataApprovalRemapLevelResourceTable extends ResourceTable<DataApprovalWorkflow> {
  private final String tableType;

  public DataApprovalRemapLevelResourceTable(List<DataApprovalWorkflow> objects, String tableType) {
    super(objects);
    this.tableType = tableType;
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.DATA_APPROVAL_REMAP_LEVEL;
  }

  @Override
  public String getCreateTempTableStatement() {
    return "create "
        + tableType
        + " table "
        + getTempTableName()
        + "("
        + "workflowid bigint not null, "
        + "dataapprovallevelid bigint not null, "
        + "level integer not null, "
        + "primary key (workflowid,dataapprovallevelid))";
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    String sql =
        "insert into "
            + getTempTableName()
            + " (workflowid,dataapprovallevelid,level) "
            + "select w.workflowid, w.dataapprovallevelid, "
            + "1 + coalesce((select max(l2.level) "
            + "from dataapprovalworkflowlevels w2 "
            + "join dataapprovallevel l2 on l2.dataapprovallevelid = w2.dataapprovallevelid "
            + "where w2.workflowid = w.workflowid "
            + "and l2.level < l.level), 0) as level "
            + "from dataapprovalworkflowlevels w "
            + "join dataapprovallevel l on l.dataapprovallevelid = w.dataapprovallevelid";

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
