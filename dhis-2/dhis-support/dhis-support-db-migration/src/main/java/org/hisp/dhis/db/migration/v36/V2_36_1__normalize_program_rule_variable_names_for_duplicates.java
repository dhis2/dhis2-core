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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
@Slf4j
public class V2_36_1__normalize_program_rule_variable_names_for_duplicates
    extends BaseJavaMigration {

  public static final String UPDATE_PROGRAMRULEVARIABLE =
      "UPDATE programrulevariable SET name= ? WHERE uid=?";

  @Override
  public void migrate(Context context) throws Exception {
    Collection<String> affectedRules =
        getCandidates(context.getConnection()).stream()
            .map(candidate -> renameOccurrencesWithSuffix(candidate, context.getConnection()))
            .collect(Collectors.toSet())
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    if (!affectedRules.isEmpty()) {
      log.warn(
          "The following rules have variables whose names were formerly duplicated by some other variables. "
              + "Some of the following rules might not work as expected after this migration, please review them: "
              + affectedRules);
    }
  }

  /**
   * Returns a list of rule variable to be renamed, as pairs of (uid, name)
   *
   * @param connection
   * @return
   * @throws SQLException
   */
  private List<Pair<Long, String>> getCandidates(Connection connection) throws SQLException {

    final String candidateDetectionSql =
        "SELECT programid, name"
            + " FROM programrulevariable "
            + " group by programid, name "
            + " having count(*) > 1";

    List<Pair<Long, String>> candidates = new ArrayList<>();

    try (final Statement stmt = connection.createStatement();
        final ResultSet rs = stmt.executeQuery(candidateDetectionSql)) {
      while (rs.next()) {
        long programId = rs.getLong("programid");
        String programRuleVariableName = rs.getString("name");

        candidates.add(Pair.of(programId, programRuleVariableName));
      }
    }
    return candidates;
  }

  /**
   * Given a rule variable name, renames it
   *
   * @param candidate
   * @param connection
   * @return variable names that have actually been renamed
   */
  @SneakyThrows
  private Collection<String> renameOccurrencesWithSuffix(
      Pair<Long, String> candidate, Connection connection) {
    Long programId = candidate.getLeft();
    String variableName = candidate.getRight();

    final String programRulesVariableToRenameSql =
        "SELECT uid, name"
            + " FROM programrulevariable where programid = "
            + programId
            + " AND name like '"
            + variableName
            + "%'";

    Map<String, String> uidWithNewNames = new HashMap<>();

    try (final Statement stmt = connection.createStatement();
        final ResultSet rs = stmt.executeQuery(programRulesVariableToRenameSql)) {
      while (rs.next()) {
        uidWithNewNames.put(rs.getString("uid"), rs.getString("name"));
      }
    }

    Collection<String> renamedVariableNames =
        ProgramRuleMigrationUtils.renameAll(
            variableName,
            uidWithNewNames,
            connection,
            UPDATE_PROGRAMRULEVARIABLE,
            this::nameUidSupplier);

    return getAffectedRules(renamedVariableNames, connection);
  }

  /**
   * Detects which Program Rules have been affected by variable renaming
   *
   * @param renamedVariableNames
   * @param connection
   * @return
   */
  @SneakyThrows
  private Collection<String> getAffectedRules(
      Collection<String> renamedVariableNames, Connection connection) {
    if (!renamedVariableNames.isEmpty()) {
      String affectedRulesSql =
          "SELECT name FROM programrule WHERE "
              + renamedVariableNames.stream()
                  .map(variableName -> "rulecondition LIKE '%{" + variableName + "}%'")
                  .collect(Collectors.joining(" OR "));

      try (final Statement stmt = connection.createStatement();
          ResultSet resultSet = stmt.executeQuery(affectedRulesSql)) {
        Collection<String> rules = new HashSet<>();

        while (resultSet.next()) {
          String ruleName = resultSet.getString("name");
          rules.add(ruleName);
        }
        return rules;
      }
    }
    return Collections.emptySet();
  }

  private Pair<String, String> nameUidSupplier(
      Map.Entry<String, String> uidNameEntry, Set<String> existingNames) {
    return Pair.of(
        findAvailableName(uidNameEntry.getValue(), existingNames), uidNameEntry.getKey());
  }

  /**
   * Utility class which shares commons method with Program Name flyway migration (i.e.
   * V2_36_2__normalize_program_rule_names_for_duplicates)
   */
  static class ProgramRuleMigrationUtils {

    /**
     * Try to append a numeric suffix to variable name
     *
     * @param originalName
     * @param existingNames
     * @return
     */
    static String findAvailableName(String originalName, Set<String> existingNames) {
      int i = 2;
      while (i < 99) {
        String proposedName = originalName + "-" + i;
        if (!existingNames.contains(proposedName)) {
          existingNames.add(proposedName);
          return proposedName;
        }
        i++;
      }
      throw new IllegalStateException(
          "Unable to detect a unique name for rule variable " + originalName);
    }

    @SneakyThrows
    static void executeUpdate(
        String updateQuery, Connection connection, Pair<String, String> params) {
      try (final PreparedStatement preparedStatement =
          prepareUpdateStatement(updateQuery, connection, params)) {
        preparedStatement.executeUpdate();
      }
    }

    @SneakyThrows
    private static PreparedStatement prepareUpdateStatement(
        String updateQuery, Connection connection, Pair<String, String> params) {
      PreparedStatement preparedStatement = connection.prepareStatement(updateQuery);
      preparedStatement.setString(1, params.getLeft());
      preparedStatement.setString(2, params.getRight());
      return preparedStatement;
    }

    static Collection<String> renameAll(
        String variableName,
        Map<String, String> uidWithNewNames,
        Connection connection,
        String updateSql,
        BiFunction<Map.Entry<String, String>, Set<String>, Pair<String, String>>
            nameUidUpdateSupplier) {
      Collection<String> renamedVariableNames = new HashSet<>();

      Set<String> existingNames = new HashSet<>(uidWithNewNames.values());

      uidWithNewNames.entrySet().stream()
          .filter(entry -> entry.getValue().equals(variableName))
          .skip(1)
          .peek(entry -> renamedVariableNames.add(entry.getValue()))
          .map(entry -> nameUidUpdateSupplier.apply(entry, existingNames))
          .forEach(params -> executeUpdate(updateSql, connection, params));

      return renamedVariableNames;
    }
  }
}
