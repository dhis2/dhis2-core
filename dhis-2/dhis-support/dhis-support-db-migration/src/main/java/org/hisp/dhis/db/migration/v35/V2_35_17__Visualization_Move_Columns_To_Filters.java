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
package org.hisp.dhis.db.migration.v35;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2_35_17__Visualization_Move_Columns_To_Filters extends BaseJavaMigration {
  @Override
  public void migrate(final Context context) throws SQLException {
    // Select all Visualizations that matches "type" equals
    // 'YEAR_OVER_YEAR_COLUMN'
    // or 'YEAR_OVER_YEAR_LINE' and "dimension" equals 'dx'.
    final String sql =
        "SELECT visualizationid, dimension, sort_order "
            + "FROM visualization_columns WHERE visualizationid IN "
            + "(SELECT v.visualizationid FROM visualization v "
            + "WHERE UPPER(COALESCE(v.type, '')) = 'YEAR_OVER_YEAR_COLUMN' "
            + "OR UPPER(COALESCE(v.type, '')) = 'YEAR_OVER_YEAR_LINE') AND LOWER(COALESCE(dimension)) = 'dx'";

    try (final Statement stmt = context.getConnection().createStatement();
        final ResultSet rs = stmt.executeQuery(sql)) {
      while (rs.next()) {
        final long visualizationId = rs.getLong("visualizationid");
        final String dimension = rs.getString("dimension");
        final int sortOrder = rs.getInt("sort_order");

        // Get the greater sort_order, in filters table, for the current
        // Visualization
        // id.
        int greatVisualizationSortOrder =
            greaterSortOrderInFiltersTableFor(context, rs.getLong("visualizationid"));

        // Increment the sort order so it can be inserted in the correct
        // position.
        greatVisualizationSortOrder++;

        // Before inserting the current column into filters table, check
        // if this column
        // isn't already present in filters table.
        if (!filtersTableContains(context, visualizationId, dimension)) {
          // Insert the current column into filters table.
          insertIntoFilterTable(context, visualizationId, dimension, greatVisualizationSortOrder);

          // Once the columns is copied into filters, remove it from
          // columns table. The
          // "moving" process is concluded for this visualization
          // column.
          deleteFromColumnsTable(context, visualizationId, dimension, sortOrder);
        }
      }
    }
  }

  private int greaterSortOrderInFiltersTableFor(final Context context, final long visualizationId)
      throws SQLException {
    final String sql =
        "SELECT MAX(sort_order) AS greater_sort_order FROM visualization_filters WHERE visualizationid = ?";

    try (final PreparedStatement ps = context.getConnection().prepareStatement(sql)) {
      ps.setLong(1, visualizationId);

      try (final ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt("greater_sort_order");
      }
    }
  }

  private boolean filtersTableContains(
      final Context context, final long visualizationId, final String dimension)
      throws SQLException {
    final String sql =
        "SELECT count(visualizationid) AS vis_counter FROM visualization_filters "
            + "WHERE visualizationid = ? AND LOWER(dimension) = LOWER(?)";

    try (final PreparedStatement ps = context.getConnection().prepareStatement(sql)) {
      ps.setLong(1, visualizationId);
      ps.setString(2, dimension);

      try (final ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt("vis_counter") > 0;
      }
    }
  }

  private void insertIntoFilterTable(
      final Context context,
      final long visualizationId,
      final String dimension,
      final int sortOrder)
      throws SQLException {
    final String sql =
        "INSERT INTO visualization_filters (visualizationid, dimension, sort_order) "
            + "VALUES (?, LOWER(?), ? )";

    try (final PreparedStatement ps = context.getConnection().prepareStatement(sql)) {
      ps.setLong(1, visualizationId);
      ps.setString(2, dimension);
      ps.setInt(3, sortOrder);

      ps.executeUpdate();
    }
  }

  private void deleteFromColumnsTable(
      final Context context,
      final long visualizationId,
      final String dimension,
      final int sortOrder)
      throws SQLException {
    final String sql =
        "DELETE FROM visualization_columns WHERE visualizationid = ? "
            + "AND LOWER(COALESCE(dimension, '')) = LOWER(?) AND sort_order = ?";

    try (final PreparedStatement ps = context.getConnection().prepareStatement(sql)) {
      ps.setLong(1, visualizationId);
      ps.setString(2, dimension);
      ps.setInt(3, sortOrder);

      ps.executeUpdate();
    }
  }
}
