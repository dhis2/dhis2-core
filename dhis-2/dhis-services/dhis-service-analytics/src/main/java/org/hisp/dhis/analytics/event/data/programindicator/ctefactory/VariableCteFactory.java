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

import static org.hisp.dhis.analytics.event.data.programindicator.ctefactory.placeholder.PlaceholderParser.variablePattern;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.event.data.programindicator.BoundarySqlBuilder;
import org.hisp.dhis.analytics.event.data.programindicator.ctefactory.placeholder.PlaceholderParser;
import org.hisp.dhis.analytics.event.data.programindicator.ctefactory.placeholder.PlaceholderParser.VariableFields;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.program.ProgramIndicator;

@Slf4j
public class VariableCteFactory implements CteSqlFactory {

  private static final Pattern PATTERN = variablePattern();

  @Override
  public boolean supports(String rawSql) {
    return rawSql != null && rawSql.contains("FUNC_CTE_VAR(");
  }

  @Override
  public String process(
      String rawSql,
      ProgramIndicator programIndicator,
      Date earliestStartDate,
      Date latestDate,
      CteContext cteContext,
      Map<String, String> aliasMap,
      SqlBuilder sqlBuilder) {

    StringBuilder out = new StringBuilder();
    Matcher matcher = PATTERN.matcher(rawSql);

    while (matcher.find()) {
      String rawPlaceholder = matcher.group(0);
      Optional<VariableFields> opt = PlaceholderParser.parseVariable(rawPlaceholder);
      if (opt.isEmpty()) {
        matcher.appendReplacement(out, Matcher.quoteReplacement(rawPlaceholder));
        continue;
      }
      PlaceholderParser.VariableFields v = opt.get();

      VariableCteKey cteKey = new VariableCteKey(v.column(), v.piUid(), v.offset());

      String alias = aliasMap.get(rawPlaceholder);

      if (alias == null) {
        if (!cteContext.containsCte(cteKey.toString())) {
          buildVariableCte(
              v, programIndicator, earliestStartDate, latestDate, cteContext, sqlBuilder);
        }
        CteDefinition def = cteContext.getDefinitionByKey(cteKey.toString());
        alias = def != null ? def.getAlias() : null;

        if (alias != null) {
          aliasMap.put(rawPlaceholder, alias);
          log.debug("Generated Variable CTE '{}' with alias '{}'", cteKey, alias);
        } else {
          log.error("Failed to resolve alias for key {}", cteKey);
        }
      }

      /* Determine replacement text (type-aware coalesce) */
      String replacement = buildReplacement(alias, rawPlaceholder, v.type());

      matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
    }

    matcher.appendTail(out);
    return out.toString();
  }

  private void buildVariableCte(
      PlaceholderParser.VariableFields v,
      ProgramIndicator pi,
      Date start,
      Date end,
      CteContext ctx,
      SqlBuilder qb) {

    String table = "analytics_event_" + pi.getProgram().getUid();
    String psCondition = v.psUid() != null ? "and ps = '" + v.psUid() + "' " : "";

    String boundaries =
        BoundarySqlBuilder.buildSql(
            pi.getAnalyticsPeriodBoundaries(),
            v.column(), // time column equals variable column
            pi,
            start,
            end,
            qb);

    String cteSql =
        String.format(
            "select enrollment, %1$s as value, "
                + "row_number() over (partition by enrollment order by occurreddate desc) as rn "
                + "from %2$s "
                + "where %1$s is not null %3$s %4$s",
            qb.quote(v.column()), table, psCondition, boundaries.trim());

    ctx.addVariableCte(
        new VariableCteKey(v.column(), v.piUid(), v.offset()).toString(), cteSql, "enrollment");
  }

  private String buildReplacement(String alias, String fallback, String typeToken) {

    if (alias == null) {
      return fallback;
    }

    if (isDateType(typeToken)) {
      return alias + ".value";
    } else if (isTextType(typeToken)) {
      return "coalesce(" + alias + ".value, '')";
    } else {
      return "coalesce(" + alias + ".value, 0)";
    }
  }

  private boolean isDateType(String t) {
    return t != null
        && switch (t) {
          case "vEventDate",
              "vCreationDate",
              "vCompletedDate",
              "vDueDate",
              "vScheduledDate",
              "vIncidentDate",
              "vEnrollmentDate" ->
              true;
          default -> false;
        };
  }

  private boolean isTextType(String t) {
    return t != null
        && switch (t) {
          case "vTextAttribute", "vOptionName", "vEventStatus", "vEnrollmentStatus" -> true;
          default -> false;
        };
  }

  record VariableCteKey(String column, String piUid, int offset) {
    @Override
    public String toString() {
      return String.format("varcte_%s_%s_%d", column, piUid, offset);
    }
  }
}
