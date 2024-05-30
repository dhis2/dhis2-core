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

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
public class CategoryOptionComboResourceTable implements ResourceTable {
  public static final String TABLE_NAME = "analytics_rs_dataelementcategoryoptioncombo";

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
        new Column("categoryoptioncomboid", DataType.BIGINT, Nullable.NOT_NULL),
        new Column("dataelementuid", DataType.CHARACTER_11, Nullable.NOT_NULL),
        new Column("categoryoptioncombouid", DataType.CHARACTER_11, Nullable.NOT_NULL));
  }

  private List<String> getPrimaryKey() {
    return List.of("dataelementid", "categoryoptioncomboid");
  }

  @Override
  public List<Index> getIndexes() {
    return List.of(
        Index.builder()
            .build()
            .withName(appendRandom("in_dataelementcategoryoptioncombo"))
            .withTableName(toStaging(TABLE_NAME))
            .withColumns(List.of("dataelementuid", "categoryoptioncombouid")));
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
            (dataelementid, categoryoptioncomboid, dataelementuid, categoryoptioncombouid) \
            select de.dataelementid as dataelementid, coc.categoryoptioncomboid as categoryoptioncomboid, \
            de.uid as dataelementuid, coc.uid as categoryoptioncombouid \
            from dataelement de \
            inner join categorycombos_optioncombos cc on de.categorycomboid = cc.categorycomboid \
            inner join categoryoptioncombo coc on cc.categoryoptioncomboid = coc.categoryoptioncomboid;""",
            "tableName",
            toStaging(TABLE_NAME));

    return Optional.of(sql);
  }

  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    return Optional.empty();
  }
}
