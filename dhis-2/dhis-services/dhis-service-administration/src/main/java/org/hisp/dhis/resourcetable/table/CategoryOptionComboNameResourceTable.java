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

import static org.hisp.dhis.dataapproval.DataApprovalLevelService.APPROVAL_LEVEL_HIGHEST;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;

/**
 * @author Lars Helge Overland
 */
@Slf4j
public class CategoryOptionComboNameResourceTable extends ResourceTable<CategoryCombo> {
  private final String tableType;

  public CategoryOptionComboNameResourceTable(List<CategoryCombo> objects, String tableType) {
    super(objects);
    this.tableType = tableType;
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.CATEGORY_OPTION_COMBO_NAME;
  }

  @Override
  public String getCreateTempTableStatement() {
    return "create "
        + tableType
        + " table "
        + getTempTableName()
        + " (categoryoptioncomboid bigint not null primary key, "
        + "categoryoptioncomboname varchar(255), approvallevel integer, "
        + "startdate date, enddate date)";
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    return Optional.empty();
  }

  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    List<Object[]> batchArgs = new ArrayList<>();

    for (CategoryCombo combo : objects) {
      if (!combo.isValid()) {
        log.warn("Ignoring category combo, not valid: " + combo);
        continue;
      }

      for (CategoryOptionCombo coc : combo.getOptionCombos()) {
        List<Object> values = new ArrayList<>();

        values.add(coc.getId());
        values.add(coc.getName());
        values.add(coc.isIgnoreApproval() ? APPROVAL_LEVEL_HIGHEST : null);
        values.add(coc.getLatestStartDate());
        values.add(coc.getEarliestEndDate());

        batchArgs.add(values.toArray());
      }
    }

    return Optional.of(batchArgs);
  }

  @Override
  public List<String> getCreateIndexStatements() {
    return Lists.newArrayList();
  }
}
