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
import java.util.HashMap;
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
import org.hisp.dhis.db.model.constraint.Unique;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
public class OrganisationUnitStructureResourceTable implements ResourceTable {
  private static final String TABLE_NAME = "_orgunitstructure";

  private final OrganisationUnitService organisationUnitService;

  private final int organisationUnitLevels;

  private final Logged logged;

  @Override
  public Table getTable() {
    return new Table(toStaging(TABLE_NAME), getColumns(), getPrimaryKey(), logged);
  }

  private List<Column> getColumns() {
    List<Column> columns =
        Lists.newArrayList(
            new Column("organisationunitid", DataType.BIGINT, Nullable.NOT_NULL),
            new Column("organisationunituid", DataType.CHARACTER_11, Nullable.NOT_NULL),
            new Column("level", DataType.INTEGER, Nullable.NOT_NULL));

    for (int k = 1; k <= organisationUnitLevels; k++) {
      columns.addAll(
          List.of(
              new Column(("idlevel" + k), DataType.BIGINT),
              new Column(("uidlevel" + k), DataType.CHARACTER_11),
              new Column(("namelevel" + k), DataType.TEXT)));
    }

    return columns;
  }

  private List<String> getPrimaryKey() {
    return List.of("organisationunitid");
  }

  @Override
  public List<Index> getIndexes() {
    return List.of(
        new Index(
            appendRandom("in_orgunitstructure_organisationunituid"),
            toStaging(TABLE_NAME),
            Unique.UNIQUE,
            List.of("organisationunituid")));
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.ORG_UNIT_STRUCTURE;
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    return Optional.empty();
  }

  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    List<Object[]> batchArgs = new ArrayList<>();

    for (int i = 0; i < organisationUnitLevels; i++) {
      int level = i + 1;

      List<OrganisationUnit> units = organisationUnitService.getOrganisationUnitsAtLevel(level);

      for (OrganisationUnit unit : units) {
        List<Object> values = new ArrayList<>();

        values.add(unit.getId());
        values.add(unit.getUid());
        values.add(level);

        Map<Integer, Long> identifiers = new HashMap<>();
        Map<Integer, String> uids = new HashMap<>();
        Map<Integer, String> names = new HashMap<>();

        for (int j = level; j > 0; j--) {
          identifiers.put(j, unit.getId());
          uids.put(j, unit.getUid());
          names.put(j, unit.getName());

          unit = unit.getParent();
        }

        for (int k = 1; k <= organisationUnitLevels; k++) {
          values.add(identifiers.get(k) != null ? identifiers.get(k) : null);
          values.add(uids.get(k));
          values.add(names.get(k));
        }

        batchArgs.add(values.toArray());
      }
    }

    return Optional.of(batchArgs);
  }
}
