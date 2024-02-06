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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.junit.jupiter.api.Test;

class TableTest {
  private final Column colA = new Column("dx", DataType.CHARACTER_11, Nullable.NOT_NULL);
  private final Column colB = new Column("value", DataType.DOUBLE, Nullable.NULL);

  @Test
  void testToStagingTable() {
    assertEquals("_categorystructure_temp", Table.toStaging("_categorystructure"));
    assertEquals("analytics_temp", Table.toStaging("analytics"));
  }

  @Test
  void testFromStagingTable() {
    assertEquals("_categorystructure", Table.fromStaging("_categorystructure_temp"));
    assertEquals("analytics", Table.fromStaging("analytics_temp"));
  }

  @Test
  void testIsUnlogged() {
    List<Column> columns = List.of(colA, colB);

    Table tableA = new Table("analytics", columns, List.of(), List.of(), Logged.UNLOGGED);
    Table tableB = new Table("analytics", columns, List.of(), List.of(), Logged.LOGGED);

    assertTrue(tableA.isUnlogged());
    assertFalse(tableB.isUnlogged());
  }

  @Test
  void testHasColumns() {
    Table table = new Table("analytics", List.of(colA, colB), List.of());

    assertTrue(table.hasColumns());
  }

  @Test
  void testSuccessfulValidation() {
    List<Column> columns = List.of(colA);
    List<String> primaryKey = List.of();

    assertDoesNotThrow(() -> new Table("analytics", columns, primaryKey));
  }

  @Test
  void testNameValidation() {
    List<Column> columns = List.of(colA);
    List<String> primaryKey = List.of();

    assertThrows(NullPointerException.class, () -> new Table(null, columns, primaryKey));
    assertThrows(IllegalArgumentException.class, () -> new Table("", columns, primaryKey));
  }

  @Test
  void testColumnsParentValidation() {
    List<Column> columns = List.of();
    List<String> primaryKey = List.of();
    List<String> checks = List.of();

    assertThrows(IllegalArgumentException.class, () -> new Table("analytics", columns, primaryKey));
    assertThrows(
        IllegalArgumentException.class,
        () -> new Table("analytics", columns, primaryKey, checks, Logged.UNLOGGED, null));
  }
}
