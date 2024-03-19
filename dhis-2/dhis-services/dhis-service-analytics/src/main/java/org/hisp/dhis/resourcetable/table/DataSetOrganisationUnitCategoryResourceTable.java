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
import static org.hisp.dhis.system.util.SqlUtils.appendRandom;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.db.model.constraint.Unique;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.resourcetable.ResourceTableType;
import org.hisp.dhis.util.DateUtils;

/**
 * @author Lars Helge Overland
 */
public class DataSetOrganisationUnitCategoryResourceTable extends AbstractResourceTable {
  public static final String TABLE_NAME = "analytics_rs_datasetorganisationunitcategory";

  private final List<DataSet> dataSets;

  private final CategoryOptionCombo defaultOptionCombo;

  public DataSetOrganisationUnitCategoryResourceTable(
      SqlBuilder sqlBuilder,
      Logged logged,
      List<DataSet> dataSets,
      CategoryOptionCombo defaultOptionCombo) {
    super(sqlBuilder, logged);
    this.dataSets = dataSets;
    this.defaultOptionCombo = defaultOptionCombo;
  }

  @Override
  public Table getTable() {
    return new Table(toStaging(TABLE_NAME), getColumns(), List.of(), logged);
  }

  private List<Column> getColumns() {
    return List.of(
        new Column("datasetid", DataType.BIGINT, Nullable.NOT_NULL),
        new Column("organisationunitid", DataType.BIGINT, Nullable.NOT_NULL),
        new Column("attributeoptioncomboid", DataType.BIGINT, Nullable.NOT_NULL),
        new Column("costartdate", DataType.DATE),
        new Column("coenddate", DataType.DATE));
  }

  @Override
  public List<Index> getIndexes() {
    return List.of(
        new Index(
            appendRandom("in_datasetorganisationunitcategory"),
            toStaging(TABLE_NAME),
            Unique.UNIQUE,
            List.of("datasetid", "organisationunitid", "attributeoptioncomboid")));
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.DATA_SET_ORG_UNIT_CATEGORY;
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    return Optional.empty();
  }

  /**
   * Iterate over data sets and associated organisation units. If data set has a category
   * combination and the organisation unit has category options, find the intersection of the
   * category option combinations linked to the organisation unit through its category options, and
   * the category option combinations linked to the data set through its category combination. If
   * not, use the default category option combo.
   */
  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    List<Object[]> batchArgs = new ArrayList<>();

    for (DataSet dataSet : dataSets) {
      CategoryCombo categoryCombo = dataSet.getCategoryCombo();

      for (OrganisationUnit orgUnit : dataSet.getSources()) {
        if (!categoryCombo.isDefault()) {
          if (orgUnit.hasCategoryOptions()) {
            Set<CategoryOption> orgUnitOptions = orgUnit.getCategoryOptions();

            for (CategoryOptionCombo optionCombo : categoryCombo.getOptionCombos()) {
              Set<CategoryOption> optionComboOptions = optionCombo.getCategoryOptions();

              if (orgUnitOptions.containsAll(optionComboOptions)) {
                Date startDate =
                    DateUtils.min(
                        optionComboOptions.stream()
                            .map(co -> co.getStartDate())
                            .collect(Collectors.toSet()));
                Date endDate =
                    DateUtils.max(
                        optionComboOptions.stream()
                            .map(co -> co.getAdjustedEndDate(dataSet))
                            .collect(Collectors.toSet()));

                List<Object> values =
                    Lists.newArrayList(
                        dataSet.getId(), orgUnit.getId(), optionCombo.getId(), startDate, endDate);

                batchArgs.add(values.toArray());
              }
            }
          }
        } else {
          List<Object> values =
              Lists.newArrayList(
                  dataSet.getId(), orgUnit.getId(), defaultOptionCombo.getId(), null, null);

          batchArgs.add(values.toArray());
        }
      }
    }

    return Optional.of(batchArgs);
  }
}
