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
package org.hisp.dhis.db.migration.v36;

import static org.hisp.dhis.db.migration.v36.V2_36_1__normalize_program_rule_variable_names_for_duplicates.ProgramRuleMigrationUtils.findAvailableName;
import static org.hisp.dhis.db.migration.v36.V2_36_1__normalize_program_rule_variable_names_for_duplicates.ProgramRuleMigrationUtils.renameAll;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
public class V2_36_2__normalize_program_rule_names_for_duplicates extends BaseJavaMigration {

  public static final String CANDIDATE_DETECTION_SQL =
      "SELECT programid, name"
          + " FROM programrule "
          + " group by programid, name "
          + " having count(*) > 1";

  public static final String PROGRAMID_AND_NAME_LIKE =
      "SELECT uid, name FROM programrule where programid = ?  AND name like ?";

  public static final String UPDATE_PROGRAMRULE = "UPDATE programrule SET name=? WHERE uid=?";

  @Override
  public void migrate(Context context) throws Exception {
    getCandidates(context.getConnection())
        .forEach(candidate -> renameOccurrencesWithSuffix(candidate, context.getConnection()));
  }

  /**
   * Returns a list of rules to be renamed, as pairs of (uid, name)
   *
   * @param connection a sql connection
   * @return a list of candidates
   * @throws SQLException in case of errors
   */
  private List<Pair<Long, String>> getCandidates(Connection connection) throws SQLException {

    List<Pair<Long, String>> candidates = new ArrayList<>();

    try (final Statement stmt = connection.createStatement();
        final ResultSet rs = stmt.executeQuery(CANDIDATE_DETECTION_SQL)) {
      while (rs.next()) {
        candidates.add(Pair.of(rs.getLong("programid"), rs.getString("name")));
      }
    }
    return candidates;
  }

  /**
   * Given a rule name, renames it
   *
   * @param candidate a candidate
   * @param connection a sql connection
   */
  @SneakyThrows
  private void renameOccurrencesWithSuffix(Pair<Long, String> candidate, Connection connection) {
    Long programId = candidate.getLeft();
    String ruleName = candidate.getRight();

    Map<String, String> uidWithNewNames = new HashMap<>();

    try (final PreparedStatement preparedStatement =
            prepareStatement(connection, programId, ruleName);
        final ResultSet resultSet = preparedStatement.executeQuery()) {
      while (resultSet.next()) {
        uidWithNewNames.put(resultSet.getString("uid"), resultSet.getString("name"));
      }
    }
    renameAll(
        ruleName, uidWithNewNames, connection, UPDATE_PROGRAMRULE, this::nameUidUpdateSupplier);
  }

  @SneakyThrows
  private PreparedStatement prepareStatement(
      Connection connection, Long programId, String ruleName) {
    PreparedStatement preparedStatement = connection.prepareStatement(PROGRAMID_AND_NAME_LIKE);
    preparedStatement.setLong(1, programId);
    preparedStatement.setString(2, ruleName + "%");
    return preparedStatement;
  }

  private Pair<String, String> nameUidUpdateSupplier(
      Map.Entry<String, String> uidNameEntry, Set<String> existingNames) {
    return Pair.of(
        findAvailableName(uidNameEntry.getValue(), existingNames), uidNameEntry.getKey());
  }
}
