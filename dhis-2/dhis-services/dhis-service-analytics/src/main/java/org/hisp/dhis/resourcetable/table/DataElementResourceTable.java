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
import static org.hisp.dhis.dataapproval.DataApprovalLevelService.APPROVAL_LEVEL_HIGHEST;
import static org.hisp.dhis.db.model.Table.toStaging;
import static org.hisp.dhis.system.util.SqlUtils.appendRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.db.model.constraint.Unique;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
public class DataElementResourceTable implements ResourceTable {
  public static final String TABLE_NAME = "analytics_rs_dataelementstructure";

  private final Logged logged;

  private final List<DataElement> dataElements;

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
        new Column("dataelementuid", DataType.CHARACTER_11, Nullable.NOT_NULL),
        new Column("dataelementname", DataType.VARCHAR_255, Nullable.NOT_NULL),
        new Column("aggregationtype", DataType.VARCHAR_50, Nullable.NOT_NULL),
        new Column("valuetype", DataType.VARCHAR_50, Nullable.NOT_NULL),
        new Column("domaintype", DataType.VARCHAR_50, Nullable.NOT_NULL),
        new Column("zeroissignificant", DataType.BOOLEAN, Nullable.NOT_NULL),
        new Column("datasetid", DataType.BIGINT),
        new Column("datasetuid", DataType.CHARACTER_11),
        new Column("datasetname", DataType.VARCHAR_255),
        new Column("datasetapprovallevel", DataType.INTEGER),
        new Column("workflowid", DataType.BIGINT),
        new Column("periodtypeid", DataType.BIGINT),
        new Column("periodtypename", DataType.VARCHAR_255));
  }

  private List<String> getPrimaryKey() {
    return List.of("dataelementid");
  }

  @Override
  public List<Index> getIndexes() {
    return List.of(
        Index.builder()
            .name(appendRandom("in_dataelementstructure_dataelementuid"))
            .tableName(toStaging(TABLE_NAME))
            .unique(Unique.UNIQUE)
            .columns(List.of("dataelementuid"))
            .build(),
        Index.builder()
            .name(appendRandom("in_dataelementstructure_datasetid"))
            .tableName(toStaging(TABLE_NAME))
            .columns(List.of("datasetid"))
            .build(),
        Index.builder()
            .name(appendRandom("in_dataelementstructure_datasetuid"))
            .tableName(toStaging(TABLE_NAME))
            .columns(List.of("datasetuid"))
            .build(),
        Index.builder()
            .name(appendRandom("in_dataelementstructure_periodtypeid"))
            .tableName(toStaging(TABLE_NAME))
            .columns(List.of("periodtypeid"))
            .build(),
        Index.builder()
            .name(appendRandom("in_dataelementstructure_workflowid"))
            .tableName(toStaging(TABLE_NAME))
            .columns(List.of("workflowid"))
            .build());
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.DATA_ELEMENT_STRUCTURE;
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    return Optional.empty();
  }

  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    List<Object[]> batchArgs = new ArrayList<>();

    for (DataElement dataElement : dataElements) {
      List<Object> values = new ArrayList<>();

      final DataSet dataSet = dataElement.getApprovalDataSet();
      final PeriodType periodType = dataElement.getPeriodType();

      // -----------------------------------------------------------------
      // Use highest approval level if data set does not require approval,
      // or null if approval is required.
      // -----------------------------------------------------------------

      values.add(dataElement.getId());
      values.add(dataElement.getUid());
      values.add(dataElement.getName());
      values.add(valueOf(dataElement.getAggregationType()));
      values.add(valueOf(dataElement.getValueType()));
      values.add(valueOf(dataElement.getDomainType()));
      values.add(dataElement.isZeroIsSignificant());
      values.add(dataSet != null ? dataSet.getId() : null);
      values.add(dataSet != null ? dataSet.getUid() : null);
      values.add(dataSet != null ? dataSet.getName() : null);
      values.add(dataSet != null && dataSet.isApproveData() ? null : APPROVAL_LEVEL_HIGHEST);
      values.add(dataSet != null && dataSet.isApproveData() ? dataSet.getWorkflow().getId() : null);
      values.add(periodType != null ? periodType.getId() : null);
      values.add(periodType != null ? periodType.getName() : null);

      batchArgs.add(values.toArray());
    }

    return Optional.of(batchArgs);
  }
}
