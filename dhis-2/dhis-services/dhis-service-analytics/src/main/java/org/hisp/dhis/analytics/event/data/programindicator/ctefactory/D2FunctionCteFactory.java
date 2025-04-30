/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.event.data.programindicator.ctefactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.event.data.programindicator.BoundarySqlBuilder;
import org.hisp.dhis.analytics.event.data.programindicator.ctefactory.placeholder.PlaceholderParser;
import org.hisp.dhis.antlr.AntlrParserUtils;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.ProgramIndicator;

@Slf4j
public class D2FunctionCteFactory implements CteSqlFactory {

  private static final Pattern PATTERN = PlaceholderParser.d2FuncPattern();
  // Pattern to parse simple conditions like '< 10', '!= "Completed"' etc.
  private static final Pattern SIMPLE_CONDITION_PATTERN =
      Pattern.compile("\\s*([<>!=]+)\\s*(.*)"); // Group 1: Operator, Group 2: Value

  @Override
  public boolean supports(String rawSql) {
    return rawSql != null && rawSql.contains("__D2FUNC__(");
  }

  @Override
  public String process(
      String rawSql,
      ProgramIndicator programIndicator,
      Date earliestStart,
      Date latestEnd,
      CteContext cteContext,
      Map<String, String> aliasMap,
      SqlBuilder sqlBuilder) {

    StringBuilder out = new StringBuilder();
    Matcher matcher = PATTERN.matcher(rawSql);
    while (matcher.find()) {
      String placeholderString = matcher.group(0);
      Optional<PlaceholderParser.D2FuncFields> opt =
          PlaceholderParser.parseD2Func(matcher.group(0));
      // malformed or bad Base64
      if (opt.isEmpty()) {
        matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
        continue;
      }
      String argType = opt.get().argType();
      String decodedArgSql = null;
      if ("val64".equals(argType) || "condLit64".equals(argType)) {
        if (!StringUtils.isEmpty(opt.get().valueSql())) {
          try {
            byte[] decodedBytes = Base64.getDecoder().decode(opt.get().valueSql());
            decodedArgSql = new String(decodedBytes, StandardCharsets.UTF_8);
          } catch (IllegalArgumentException e) {
            log.error(
                "Failed to decode Base64 argument for placeholder: {}. Skipping CTE generation.",
                matcher.group(0),
                e);
            // Append the original placeholder and skip to next match
            matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
            continue;
          }
        } else {
          log.warn(
              "Placeholder specified argType '{}' but arg64 was empty: {}",
              argType,
              matcher.group(0));
          decodedArgSql = "";
        }
      }

      String argumentHash = (decodedArgSql != null) ? generateSqlHash(decodedArgSql) : "noarg";

      // Construct the deterministic CTE key
      String cteKey =
          String.format(
              "d2%s_%s_%s_%s_%s_%s",
              opt.get().func().toLowerCase(),
              opt.get().psUid(),
              opt.get().deUid(),
              argumentHash,
              opt.get().boundaryHash(),
              opt.get().piUid());

      // Check if CTE already exists
      if (!cteContext.containsCte(cteKey)) {

        // Get Program UID and Event Table Name
        if (programIndicator.getProgram() == null) {
          log.error(
              "ProgramIndicator {} (from context) has no associated Program. Cannot determine event table name for key {}.",
              programIndicator.getUid(),
              cteKey);
          matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
          continue;
        }
        String programUid = programIndicator.getProgram().getUid();
        String eventTableName = "analytics_event_" + programUid;

        // Get quoted DE UID and boundary conditions
        String quotedDeUid = sqlBuilder.quote(opt.get().deUid());
        String boundaryConditionsSql =
            BoundarySqlBuilder.buildSql(
                programIndicator.getAnalyticsPeriodBoundaries(),
                AnalyticsPeriodBoundary.DB_EVENT_DATE,
                programIndicator,
                earliestStart,
                latestEnd,
                sqlBuilder);

        // Generate CTE Body SQL based on function type and argument
        String cteBodySql;
        String conditionSql;
        String isNotNullCondition = String.format("%s is not null", quotedDeUid);

        if ("countIfValue".equals(opt.get().func())) {
          if (decodedArgSql == null) { // Should have decoded valueSql
            log.error("Missing decoded value SQL for countIfValue, key: {}", cteKey);
            matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
            continue;
          }
          conditionSql = String.format("%s = %s", quotedDeUid, decodedArgSql); // Use DE = valueSql

        } else if ("countIfCondition".equals(opt.get().func())) {
          if (decodedArgSql == null) { // Should have decoded condition literal
            log.error("Missing decoded condition literal for countIfCondition, key: {}", cteKey);
            matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
            continue;
          }
          // Apply simple parsing strategy for condition literal
          String rawConditionLiteral = decodedArgSql; // This includes quotes
          String conditionLiteral =
              AntlrParserUtils.trimQuotes(rawConditionLiteral); // Remove quotes
          Matcher conditionMatcher = SIMPLE_CONDITION_PATTERN.matcher(conditionLiteral);
          if (conditionMatcher.matches()) {
            String operator = conditionMatcher.group(1);
            String value = conditionMatcher.group(2).trim();
            // Basic casting assumption (numeric) - enhance if needed
            conditionSql =
                String.format(
                    "%s %s %s",
                    sqlBuilder.cast(quotedDeUid, DataType.NUMERIC),
                    operator,
                    sqlBuilder.cast(value, DataType.NUMERIC));
          } else {
            log.error(
                "Could not parse condition literal '{}' for countIfCondition, key: {}",
                conditionLiteral,
                cteKey);
            matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
            continue;
          }

        } else if ("count".equals(opt.get().func())) {
          // No specific value/condition argument, just count non-nulls
          conditionSql = isNotNullCondition;
          isNotNullCondition = "1=1";
        } else {
          log.warn(
              "Unsupported d2 function '{}' encountered for CTE generation. Key: {}",
              opt.get().func(),
              cteKey);
          matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
          continue;
        }

        // Combine base conditions (ps, is not null) with specific condition and boundaries
        String whereClause =
            String.format(
                "where ps = %s and %s and %s %s",
                sqlBuilder.singleQuote(opt.get().psUid()),
                isNotNullCondition, // Always check base column is not null (unless it's d2:count)
                conditionSql, // Specific condition from function type
                boundaryConditionsSql // Boundary conditions (includes leading ' and ' if present)
                );

        // Construct the final CTE body using the generated condition
        cteBodySql =
            String.format(
                "select enrollment, count(%1$s) as value " // Count the specific column
                    + "from %2$s "
                    + "%3$s " // Combined WHERE clause
                    + "group by enrollment",
                quotedDeUid, eventTableName, whereClause);

        // Add CTE definition to context (using D2_FUNCTION type)
        CteDefinition d2CteDef = CteDefinition.forD2Function(cteKey, cteBodySql, "enrollment");
        cteContext.addD2FunctionCte(cteKey, d2CteDef);

        log.debug("Generated D2 Function CTE '{}' for function '{}'", cteKey, opt.get().func());
      }

      CteDefinition cteDef = cteContext.getDefinitionByKey(cteKey);
      if (cteDef == null) {
        log.error(
            "CTE definition unexpectedly missing for key '{}' after generation attempt. Placeholder remains.",
            cteKey);
        matcher.appendReplacement(out, Matcher.quoteReplacement(placeholderString));
        continue; // Skip replacement if definition is missing
      }

      String alias = cteDef.getAlias();
      if (alias == null) {
        log.error("CTE definition for key '{}' has null alias. Placeholder remains.", cteKey);
        matcher.appendReplacement(out, Matcher.quoteReplacement(placeholderString));
        continue; // Skip replacement if alias is missing
      }
      // Populate the output map
      aliasMap.put(placeholderString, alias);

      // Determine replacement value
      String replacement = String.format("coalesce(%s.value, 0)", alias);

      // Perform replacement
      matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(out);
    return out.toString();
  }

  /** Generates a simple hash for SQL value strings to use in CTE keys. */
  private static String generateSqlHash(String sql) {
    return SqlHashUtil.sha1(sql);
  }
}
