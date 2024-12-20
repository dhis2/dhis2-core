/*
 * Copyright (c) 2004-2024, University of Oslo
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
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;

@RequiredArgsConstructor
public class DataElementOptionResourceTable implements ResourceTable {
  public static final String TABLE_NAME = "analytics_rs_dataelementoption";

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
        new Column("dataelementid", DataType.BIGINT, Nullable.NOT_NULL),
        new Column("optionsetid", DataType.BIGINT, Nullable.NOT_NULL),
        new Column("optionvalueid", DataType.BIGINT, Nullable.NOT_NULL),
        new Column("dataelementuid", DataType.CHARACTER_11, Nullable.NOT_NULL),
        new Column("optionsetuid", DataType.CHARACTER_11, Nullable.NOT_NULL),
        new Column("optionvalueuid", DataType.CHARACTER_11, Nullable.NOT_NULL),
        new Column("optionvaluecode", DataType.VARCHAR_255, Nullable.NOT_NULL));
  }

  private List<String> getPrimaryKey() {
    return List.of("dataelementid", "optionsetid", "optionvalueid");
  }

  @Override
  public List<Index> getIndexes() {
    return List.of(
        Index.builder()
            .name(appendRandom("in_optionsetoptionvalue"))
            .tableName(toStaging(TABLE_NAME))
            .columns(List.of("dataelementuid", "optionsetuid", "optionvalueuid"))
            .build(),
        Index.builder()
            .name(appendRandom("in_dataelementoptioncode"))
            .tableName(toStaging(TABLE_NAME))
            .columns(List.of("dataelementuid", "optionvaluecode"))
            .build());
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.DATA_ELEMENT_CATEGORY_OPTION_COMBO;
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    String sql =
        replace(
            """
                        insert into ${tableName} \
                        (dataelementid, optionsetid, optionvalueid, dataelementuid, optionsetuid, optionvalueuid, optionvaluecode) \
                        select de.dataelementid, os.optionsetid as optionsetid, ov.optionvalueid as optionvalueid, \
                        de.uid as dataelementuid, os.uid as optionsetuid, ov.uid as optionvalueuid, ov.code as optionvaluecode from optionvalue ov \
                        inner join optionset os on ov.optionsetid = os.optionsetid \
                        inner join dataelement de on os.optionsetid = de.optionsetid;""",
            "tableName",
            toStaging(TABLE_NAME));

    return Optional.of(sql);
  }

  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    return Optional.empty();
  }
}
