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

  @Override
  public boolean supports(String rawSql) {
    return rawSql != null && rawSql.contains("__D2FUNC__(");
  }

  @Override
  public String process(
      String sql,
      ProgramIndicator pi,
      Date start,
      Date end,
      CteContext ctx,
      Map<String, String> aliasMap,
      SqlBuilder qb) {

    StringBuilder out = new StringBuilder();
    Matcher m = PATTERN.matcher(sql);

    while (m.find()) {
      String raw = m.group(0);

      Optional<PlaceholderParser.D2FuncFields> opt = PlaceholderParser.parseD2Func(raw);
      if (opt.isEmpty()) {
        copyRaw(m, out);
        continue;
      }

      PlaceholderParser.D2FuncFields f = opt.get();

      D2FuncType kind = D2FuncType.from(f.func());
      if (kind == null) {
        copyRaw(m, out);
        continue;
      }

      String argDecoded = decodeArg(f);
      if (kind.needsArg() && argDecoded == null) {
        copyRaw(m, out);
        continue;
      }

      String cteKey = buildKey(f, kind, argDecoded);

      ensureCte(cteKey, kind, f, argDecoded, pi, start, end, ctx, qb);

      CteDefinition def = ctx.getDefinitionByKey(cteKey);
      if (def == null || def.getAlias() == null) {
        copyRaw(m, out);
        continue;
      }

      aliasMap.put(raw, def.getAlias());
      m.appendReplacement(
          out, Matcher.quoteReplacement("coalesce(" + def.getAlias() + ".value, 0)"));
    }
    m.appendTail(out);
    return out.toString();
  }

  private static void copyRaw(Matcher m, StringBuilder out) {
    m.appendReplacement(out, Matcher.quoteReplacement(m.group(0)));
  }

  /**
   * Decodes the Base64-encoded argument from the placeholder. If the placeholder has no argument,
   * returns null.
   *
   * @param f The parsed fields from the placeholder.
   * @return The decoded argument or null if not present.
   */
  private static String decodeArg(PlaceholderParser.D2FuncFields f) {
    if ("none".equals(f.argType())) return null;

    if (StringUtils.isBlank(f.valueSql())) {
      log.warn("Placeholder had argType '{}' but arg64 empty: {}", f.argType(), f.raw());
      return "";
    }
    try {
      byte[] bytes = Base64.getDecoder().decode(f.valueSql());
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      log.error("Bad Base64 in placeholder: {}", f.raw(), ex);
      return null;
    }
  }

  private static String buildKey(
      PlaceholderParser.D2FuncFields f, D2FuncType kind, String argDecoded) {

    String hash = SqlHashUtil.sha1(argDecoded == null ? "noarg" : argDecoded);
    return "d2%s_%s_%s_%s_%s_%s"
        .formatted(kind.id.toLowerCase(), f.psUid(), f.deUid(), hash, f.boundaryHash(), f.piUid());
  }

  private void ensureCte(
      String key,
      D2FuncType kind,
      PlaceholderParser.D2FuncFields f,
      String argDecoded,
      ProgramIndicator pi,
      Date start,
      Date end,
      CteContext ctx,
      SqlBuilder qb) {

    if (ctx.containsCte(key)) return;

    String eventTable = resolveEventTable(pi);
    if (eventTable == null) return;

    String quotedDe = qb.quote(f.deUid());

    String conditionSql = kind.buildCondition(qb, quotedDe, argDecoded);
    if (conditionSql == null) return; // logged inside

    String where =
        "where ps = %s and %s and %s%s"
            .formatted(
                qb.singleQuote(f.psUid()),
                kind.baseNotNull(qb, quotedDe),
                conditionSql,
                BoundarySqlBuilder.buildSql(
                    pi.getAnalyticsPeriodBoundaries(),
                    AnalyticsPeriodBoundary.DB_EVENT_DATE,
                    pi,
                    start,
                    end,
                    qb));

    String body =
        "select enrollment, count(%s) as value from %s %s group by enrollment"
            .formatted(quotedDe, eventTable, where);

    ctx.addD2FunctionCte(key, CteDefinition.forD2Function(key, body, "enrollment"));

    log.debug("Generated D2 Function CTE '{}' ({})", key, kind.id);
  }

  private static String resolveEventTable(ProgramIndicator pi) {
    if (pi.getProgram() == null) {
      log.error("ProgramIndicator {} lacks program – cannot build D2 CTE.", pi.getUid());
      return null;
    }
    return "analytics_event_" + pi.getProgram().getUid();
  }

  private enum D2FuncType {
    COUNT("count") {
      @Override
      boolean needsArg() {
        return false;
      }

      @Override
      String baseNotNull(SqlBuilder qb, String deQuoted) {
        return "1=1";
      }

      @Override
      String buildCondition(SqlBuilder qb, String deQuoted, String arg) {
        return "1=1";
      }
    },

    COUNT_IF_VALUE("countIfValue") {
      @Override
      String buildCondition(SqlBuilder qb, String deQuoted, String arg) {
        return deQuoted + " = " + arg;
      }
    },

    COUNT_IF_CONDITION("countIfCondition") {

      /**
       * Simple parser for condition-literals like "< 5", ">= 100", "!= 'Closed'".
       *
       * <p>^\s* – ignore leading whitespace ([<>!=]+) – **Group 1:** one-or-more comparison symbols
       * (<, >, =, !) \s* – optional whitespace (.*) – **Group 2:** the remainder (number, quoted
       * text, etc.)
       *
       * <p>The expression does not account for illegal operators like `<<<`; exact casting/quoting
       * is handled after the match.
       */
      private static final Pattern SIMPLE = Pattern.compile("\\s*([<>!=]+)\\s*(.*)");

      @Override
      String buildCondition(SqlBuilder qb, String deQuoted, String arg) {
        String trimmed = AntlrParserUtils.trimQuotes(arg);
        Matcher m = SIMPLE.matcher(trimmed);
        if (!m.matches()) {
          log.error("Unparsable condition literal '{}'", trimmed);
          return null;
        }
        String op = m.group(1);
        String val = m.group(2).trim();
        return qb.cast(deQuoted, DataType.NUMERIC)
            + " "
            + op
            + " "
            + qb.cast(val, DataType.NUMERIC);
      }
    };

    final String id;

    D2FuncType(String id) {
      this.id = id;
    }

    boolean needsArg() {
      return true;
    }

    String baseNotNull(SqlBuilder qb, String deQuoted) {
      return deQuoted + " is not null";
    }

    abstract String buildCondition(SqlBuilder qb, String deQuoted, String arg);

    /* Map raw func string to enum */
    static D2FuncType from(String func) {
      for (D2FuncType k : values()) if (k.id.equals(func)) return k;
      return null;
    }
  }
}
