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

import static java.lang.String.format;
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
  public static final String TABLE_NAME = "analytics_rs_orgunitstructure";
  public static final int ROOT_LEVEL = 1;

  private final Logged logged;

  private final int organisationUnitLevels;

  /** A to do is removing this service and finding a way to retrieve with SQL. */
  private final OrganisationUnitService organisationUnitService;

  @Override
  public Table getTable() {
    return new Table(toStaging(TABLE_NAME), getColumns(), getPrimaryKey(), logged);
  }

  @Override
  public Table getMainTable() {
    return new Table(TABLE_NAME, getColumns(), getPrimaryKey(), logged);
  }

  private List<Column> getColumns() {
    List<Column> columns =
        Lists.newArrayList(
            new Column("organisationunitid", DataType.BIGINT, Nullable.NOT_NULL),
            new Column("organisationunituid", DataType.CHARACTER_11, Nullable.NOT_NULL),
            new Column("code", DataType.VARCHAR_50, Nullable.NULL),
            new Column("name", DataType.VARCHAR_255, Nullable.NOT_NULL),
            new Column("openingdate", DataType.DATE, Nullable.NULL),
            new Column("closeddate", DataType.DATE, Nullable.NULL),
            new Column("level", DataType.INTEGER, Nullable.NOT_NULL),
            new Column("path", DataType.VARCHAR_255, Nullable.NULL));

    for (int level = ROOT_LEVEL; level <= organisationUnitLevels; level++) {
      columns.addAll(
          List.of(
              new Column(("idlevel" + level), DataType.BIGINT),
              new Column(("uidlevel" + level), DataType.CHARACTER_11),
              new Column(("namelevel" + level), DataType.TEXT)));
    }

    return columns;
  }

  private List<String> getPrimaryKey() {
    return List.of("organisationunitid");
  }

  @Override
  public List<Index> getIndexes() {
    return List.of(
        Index.builder()
            .name(appendRandom("in_orgunitstructure_organisationunituid"))
            .tableName(toStaging(TABLE_NAME))
            .unique(Unique.UNIQUE)
            .columns(List.of("organisationunituid"))
            .build());
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
    List<Object[]> batchObjects = new ArrayList<>();

    for (int level = ROOT_LEVEL; level <= organisationUnitLevels; level++) {
      List<OrganisationUnit> units = organisationUnitService.getOrganisationUnitsAtLevel(level);

      batchObjects.addAll(createBatchObjects(units, level));
    }

    return Optional.of(batchObjects);
  }

  /**
   * Creates the list of batch of objects to be added to the org. unit resource table.
   *
   * @param units the list of {@link OrganisationUnit}.
   * @param level the level of the given list of {@link OrganisationUnit}.
   * @return the list of batch objects.
   */
  List<Object[]> createBatchObjects(List<OrganisationUnit> units, int level) {
    List<Object[]> batchObjects = new ArrayList<>();

    for (OrganisationUnit unit : units) {
      List<Object> values = new ArrayList<>();

      values.add(unit.getId());
      values.add(unit.getUid());
      values.add(unit.getCode());
      values.add(unit.getName());
      values.add(unit.getOpeningDate());
      values.add(unit.getClosedDate());
      values.add(level);
      values.add(unit.getStoredPath());

      Map<Integer, Long> identifiers = new HashMap<>();
      Map<Integer, String> uids = new HashMap<>();
      Map<Integer, String> names = new HashMap<>();

      for (int j = level; j > 0; j--) {
        identifiers.put(j, unit.getId());
        uids.put(j, unit.getUid());
        names.put(j, unit.getName());

        if (isOrgUnitLevelValid(unit, j)) {
          unit = unit.getParent();
        } else {
          throw new IllegalStateException(
              format(
                  "Invalid hierarchy level or missing parent for organisation unit %s.",
                  unit.getUid()));
        }
      }

      for (int k = ROOT_LEVEL; k <= organisationUnitLevels; k++) {
        values.add(identifiers.get(k) != null ? identifiers.get(k) : null);
        values.add(uids.get(k));
        values.add(names.get(k));
      }

      batchObjects.add(values.toArray());
    }

    return batchObjects;
  }

  /**
   * Verifies if the given {@link OrganisationUnit} matches the given value, as expected.
   *
   * @param unit the {@link OrganisationUnit} to check.
   * @param level the level to be used in the validation.
   * @return true if the expectation is matched, false otherwise.
   */
  private static boolean isOrgUnitLevelValid(OrganisationUnit unit, int level) {
    boolean isLevelCorrect = unit.getLevel() == level && unit.getHierarchyLevel() == level;
    boolean hasParent = unit.getParent() != null;
    return isLevelCorrect
        && ((hasParent && level > ROOT_LEVEL) || (level == ROOT_LEVEL && !hasParent));
  }
}
