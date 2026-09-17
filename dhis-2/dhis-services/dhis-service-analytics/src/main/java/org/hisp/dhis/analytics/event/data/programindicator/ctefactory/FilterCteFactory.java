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

import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.AMPERSAND_2;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.AND;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.PAREN;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.event.data.programindicator.BoundarySqlBuilder;
import org.hisp.dhis.analytics.event.data.programindicator.ctefactory.placeholder.PlaceholderParser;
import org.hisp.dhis.analytics.event.data.programindicator.ctefactory.placeholder.PlaceholderParser.FilterFields;
import org.hisp.dhis.antlr.Parser;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.db.util.AnalyticsTableNames;
import org.hisp.dhis.parser.expression.antlr.ExpressionBaseListener;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExpressionContext;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;

public class FilterCteFactory implements CteSqlFactory {

  private static final Pattern PATTERN = PlaceholderParser.filterPattern();
  private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_]");

  /** Literal left behind in place of a comparison that has been lifted into a filter CTE. */
  private static final String NEUTRAL_LITERAL = "true";

  private static final Pattern NEUTRAL_TOKENS =
      Pattern.compile("\\btrue\\b|&&|\\|\\||\\band\\b|\\bor\\b|[()\\s]");

  @Override
  public boolean supports(String rawSql) {
    return rawSql != null && rawSql.contains("V{");
  }

  @Override
  public String process(
      String rawSql,
      ProgramIndicator pi,
      Date start,
      Date end,
      CteContext ctx,
      Map<String, String> aliasMap,
      SqlBuilder qb) {

    if (StringUtils.isBlank(rawSql)) {
      return "";
    }

    // Validate the original expression before changing it or registering any CTEs.
    Map<Integer, Integer> requiredComparisons = findRequiredComparisons(rawSql);
    StringBuilder out = new StringBuilder();
    boolean simpleFound = false;
    Matcher m = PATTERN.matcher(rawSql);

    while (m.find()) {
      if (!Integer.valueOf(m.end()).equals(requiredComparisons.get(m.start()))) {
        m.appendReplacement(out, Matcher.quoteReplacement(m.group(0)));
        continue;
      }
      Optional<FilterFields> opt = parse(m);
      if (opt.isEmpty()) {
        m.appendReplacement(out, Matcher.quoteReplacement(m.group(0)));
        continue;
      }

      FilterFields p = opt.get();

      /* Resolve column + SQL operator */
      String column = mapVariable(p.variableName());
      String sqlOp = mapOperator(p.operator());

      if (column == null || sqlOp == null) {
        m.appendReplacement(out, Matcher.quoteReplacement(m.group(0)));
        continue;
      }

      /* Build / register CTE */
      String cteKey =
          String.format(
              "filtercte_%s_%s_%s_%s",
              column, safeOp(p.operator()), safeLiteral(p.literal()), pi.getUid());

      if (!ctx.containsCte(cteKey)) {
        ctx.addFilterCte(cteKey, buildFilterCteSql(pi, column, p.literal(), sqlOp, start, end, qb));
      }

      String alias = ctx.getDefinitionByKey(cteKey).getAlias();
      aliasMap.put(m.group(0), alias); // placeholder-to-alias

      // Only conjunctive requirements can be enforced by an INNER JOIN and replaced with true.
      m.appendReplacement(out, NEUTRAL_LITERAL);
      simpleFound = true;
    }
    m.appendTail(out);

    if (!simpleFound) {
      return rawSql;
    }

    String remaining = out.toString().trim();

    return carriesNoCondition(remaining) ? "" : remaining;
  }

  private Optional<FilterFields> parse(Matcher m) {
    String raw = m.group(0);
    return PlaceholderParser.parseFilter(raw);
  }

  /**
   * Finds complete comparisons required by the whole filter. Comparisons under OR, negation or
   * functions must remain in the expression, where the variable CTE's LEFT JOIN preserves their
   * boolean context. Matching parser nodes also avoids rewriting comparison-like string literals.
   */
  private static Map<Integer, Integer> findRequiredComparisons(String filter) {
    Map<Integer, Integer> comparisons = new HashMap<>();
    Parser.listen(
        filter,
        new ExpressionBaseListener() {
          @Override
          public void enterExpr(ExprContext ctx) {
            if (ctx.it == null || mapOperator(ctx.it.getText()) == null || !isRequired(ctx)) {
              return;
            }
            String text = ctx.getText();
            String comparison = text.trim();
            if (PATTERN.matcher(comparison).matches()) {
              // ANTLR indexes Unicode code points; Matcher indexes UTF-16 characters.
              int start =
                  filter.offsetByCodePoints(0, ctx.getStart().getStartIndex())
                      + text.indexOf(comparison);
              comparisons.put(start, start + comparison.length());
            }
          }
        });
    return comparisons;
  }

  private static boolean isRequired(ExprContext comparison) {
    ParseTree parent = comparison.getParent();
    while (parent instanceof ExprContext expr) {
      if (expr.it != null
          && expr.it.getType() != PAREN
          && expr.it.getType() != AND
          && expr.it.getType() != AMPERSAND_2) {
        return false;
      }
      parent = parent.getParent();
    }
    return parent instanceof ExpressionContext;
  }

  private static String buildFilterCteSql(
      ProgramIndicator pi,
      String column,
      String literal,
      String sqlOp,
      Date start,
      Date end,
      SqlBuilder qb) {

    String table = AnalyticsTableNames.eventTable(pi.getProgram());
    String quotedCol = qb.quote(column);

    String boundaries =
        BoundarySqlBuilder.buildSql(
            pi.getAnalyticsPeriodBoundaries(),
            "occurreddate",
            pi,
            start,
            end,
            qb,
            AnalyticsType.EVENT);

    return String.format(
        """
                select enrollment
                from (
                    select enrollment, %1$s,
                           row_number() over (partition by enrollment order by occurreddate desc) as rn
                    from %2$s
                    where %1$s is not null %3$s
                ) latest
                where rn = 1 and %1$s %4$s %5$s""",
        quotedCol, table, boundaries, sqlOp, qb.singleQuote(literal));
  }

  /* Maps V{variableName} → event analytics column */
  private static String mapVariable(String name) {
    return switch (name) {
      case "event_status" -> "eventstatus";
      case "scheduled_date", "due_date" -> "scheduleddate";
      case "creation_date" -> "created";
      case "event_date" -> "occurreddate";
      default -> null;
    };
  }

  private static String mapOperator(String sym) {
    return switch (sym) {
      case "==" -> "=";
      case "!=" -> "!=";
      case ">" -> ">";
      case "<" -> "<";
      case ">=" -> ">=";
      case "<=" -> "<=";
      default -> null;
    };
  }

  private static String safeOp(String op) {
    return op.replace("=", "eq").replace("!", "ne").replace("<", "lt").replace(">", "gt");
  }

  private static String safeLiteral(String lit) {
    String s = INVALID_CHARS.matcher(lit).replaceAll("_").toLowerCase();
    return s.substring(0, Math.min(20, s.length()));
  }

  /**
   * Tells whether an expression restricts nothing, which is the case once every comparison it held
   * has been lifted into a filter CTE and only neutral literals and logical operators are left.
   *
   * @param expression the remaining filter expression. Must not be null.
   * @return true if the expression carries no condition of its own.
   */
  private static boolean carriesNoCondition(String expression) {
    return NEUTRAL_TOKENS.matcher(expression).replaceAll("").isEmpty();
  }
}
