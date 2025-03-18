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

import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.db.model.Table.toStaging;
import static org.hisp.dhis.system.util.SqlUtils.appendRandom;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.db.model.constraint.Unique;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;

@RequiredArgsConstructor
public class RelationshipCountResourceTable implements ResourceTable {

  public static final String TABLE_NAME = "analytics_rs_relationship";
  private final Logged logged;

  @Override
  public Table getTable() {
    return new Table(toStaging(TABLE_NAME), getColumns(), getPrimaryKey(), logged);
  }

  @Override
  public Table getMainTable() {
    return new Table(TABLE_NAME, getColumns(), getPrimaryKey(), logged);
  }

  private List<Column> getColumns() {
    return List.of(
        new Column("trackedentityid", DataType.CHARACTER_11, Nullable.NOT_NULL),
        new Column("relationshiptypeuid", DataType.CHARACTER_11, Nullable.NOT_NULL),
        new Column("relationship_count", DataType.BIGINT, Nullable.NOT_NULL));
  }

  private List<String> getPrimaryKey() {
    return List.of("trackedentityid", "relationshiptypeuid");
  }

  @Override
  public List<Index> getIndexes() {
    return List.of(
        Index.builder()
            .name(appendRandom("in_relationshipcount_teiid"))
            .tableName(toStaging(TABLE_NAME))
            .unique(Unique.UNIQUE)
            .columns(List.of("trackedentityid", "relationshiptypeuid"))
            .build());
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.TEI_RELATIONSHIP_COUNT;
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    String sql =
        replace(
            """
                        insert into ${tableName}
                        (trackedentityid, relationshiptypeuid, relationship_count)
                        select te.uid as trackedentity, rt.uid, count(*) as relationship_count
                        from relationship r
                        join relationshipitem rifrom on rifrom.relationshipid = r.relationshipid
                        join relationshiptype rt on r.relationshiptypeid = rt.relationshiptypeid
                        join trackedentity te on rifrom.trackedentityid = te.trackedentityid
                        where r.deleted is false
                        group by te.uid, rt.uid;
                        """,
            "tableName",
            toStaging(TABLE_NAME));

    return Optional.of(sql);
  }

  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    return Optional.empty();
  }
}
