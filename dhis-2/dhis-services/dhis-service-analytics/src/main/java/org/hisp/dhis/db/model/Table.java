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
package org.hisp.dhis.db.model;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.util.ObjectUtils.notNull;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.Validate;

/**
 * Represents a database table.
 *
 * @author Lars Helge Overland
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Table {
  public static final String STAGING_TABLE_SUFFIX = "_temp";

  private static final String STAGING_TABLE_SUFFIX_RGX = "\\_temp$";

  /** Table name. Required. */
  @EqualsAndHashCode.Include private final String name;

  /** Table columns. At least one column required, unless a parent table is specified. */
  private final List<Column> columns;

  /** Table primary key column name(s). Optional. */
  private final List<String> primaryKey;

  /** Table checks. PostgreSQL-only feature. Optional. */
  private final List<String> checks;

  /** Whether table is logged or unlogged. PostgreSQL-only feature. */
  private final Logged logged;

  /**
   * The parent table. This table will inherit from the parent table, if specified. Optional, may be
   * null.
   */
  private final Table parent;

  /**
   * Constructor.
   *
   * @param name the table name.
   * @param columns the list of {@link Column}.
   * @param primaryKey the primary key.
   */
  public Table(String name, List<Column> columns, List<String> primaryKey) {
    this.name = name;
    this.columns = columns;
    this.primaryKey = primaryKey;
    this.checks = List.of();
    this.logged = Logged.LOGGED;
    this.parent = null;
    this.validate();
  }

  /**
   * Constructor.
   *
   * @param name the table name.
   * @param columns the list of {@link Column}.
   * @param primaryKey the primary key.
   * @param logged the {@link Logged} parameter.
   */
  public Table(String name, List<Column> columns, List<String> primaryKey, Logged logged) {
    this.name = name;
    this.columns = columns;
    this.primaryKey = primaryKey;
    this.checks = List.of();
    this.logged = logged;
    this.parent = null;
    this.validate();
  }

  /**
   * Constructor.
   *
   * @param name the table name.
   * @param columns the list of {@link Column}.
   * @param primaryKey the primary key.
   * @param checks the list of checks.
   * @param logged the {@link Logged} parameter.
   */
  public Table(
      String name,
      List<Column> columns,
      List<String> primaryKey,
      List<String> checks,
      Logged logged) {
    this.name = name;
    this.columns = columns;
    this.primaryKey = primaryKey;
    this.checks = checks;
    this.logged = logged;
    this.parent = null;
    this.validate();
  }

  /**
   * Constructor.
   *
   * @param name the table name.
   * @param columns the list of {@link Column}.
   * @param primaryKey the primary key.
   * @param checks the list of checks.
   * @param logged the {@link Logged} parameter.
   * @param parent the parent {@link Table}.
   */
  public Table(
      String name,
      List<Column> columns,
      List<String> primaryKey,
      List<String> checks,
      Logged logged,
      Table parent) {
    this.name = name;
    this.columns = columns;
    this.primaryKey = primaryKey;
    this.checks = checks;
    this.logged = logged;
    this.parent = parent;
    this.validate();
  }

  /** Validates this object. */
  public void validate() {
    Validate.notBlank(name);
    Validate.isTrue(hasColumns() || hasParent());
  }

  /**
   * Indicates whether the table has at least one column.
   *
   * @return true if the table has at least one column.
   */
  public boolean hasColumns() {
    return isNotEmpty(columns);
  }

  /**
   * Indicates whether the table has a primary key.
   *
   * @return true if the table has a primary key.
   */
  public boolean hasPrimaryKey() {
    return isNotEmpty(primaryKey);
  }

  /**
   * Indicates whether the table has at least one check.
   *
   * @return true if the table has at least one check.
   */
  public boolean hasChecks() {
    return isNotEmpty(checks);
  }

  /**
   * Indicates whether the table is unlogged.
   *
   * @return true if the table is unlogged.
   */
  public boolean isUnlogged() {
    return Logged.UNLOGGED == logged;
  }

  /**
   * Indicates whether the table has a parent table.
   *
   * @return true if table has a parent table.
   */
  public boolean hasParent() {
    return notNull(parent);
  }

  /**
   * Converts the given table name to a staging table name.
   *
   * @param tableName the table name.
   * @return the staging table name.
   */
  public static String toStaging(String tableName) {
    return tableName + STAGING_TABLE_SUFFIX;
  }

  /**
   * Converts the given staging table name to a main table name.
   *
   * @param tableName the staging table name.
   * @return a main table name.
   */
  public static String fromStaging(String tableName) {
    return RegExUtils.removePattern(tableName, STAGING_TABLE_SUFFIX_RGX);
  }
}
