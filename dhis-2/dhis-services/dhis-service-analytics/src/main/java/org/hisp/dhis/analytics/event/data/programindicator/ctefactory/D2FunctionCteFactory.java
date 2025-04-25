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

      Optional<PlaceholderParser.D2FuncFields> opt =
          PlaceholderParser.parseD2Func(matcher.group(0));
      // malformed or bad Base64
      if (opt.isEmpty()) {
        matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
        continue;
      }
      PlaceholderParser.D2FuncFields p = opt.get();

      // Build key & ensure CTE
      String cteKey = buildKey(p);
      ensureCte(cteKey, p, programIndicator, earliestStart, latestEnd, cteContext, sqlBuilder);

      // Substitute only if CTE exists
      String alias = aliasFor(cteKey, cteContext);
      if (alias == null) {
        matcher.appendReplacement(out, Matcher.quoteReplacement(p.raw()));
        continue;
      }

      aliasMap.put(p.raw(), alias);
      String replacement = "coalesce(" + alias + ".value, 0)";
      matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(out);
    return out.toString();
  }

  private String buildKey(PlaceholderParser.D2FuncFields p) {
    return String.format(
        "d2%s_%s_%s_%s_%s_%s",
        p.func().toLowerCase(),
        p.psUid(),
        p.deUid(),
        SqlHashUtil.sha1(p.valueSql()),
        p.boundaryHash(),
        p.piUid());
  }

  private void ensureCte(
      String key,
      PlaceholderParser.D2FuncFields p,
      ProgramIndicator pi,
      Date start,
      Date end,
      CteContext ctx,
      SqlBuilder qb) {

    if (ctx.containsCte(key)) {
      return; // already present
    }

    if (pi.getProgram() == null) {
      log.error("PI {} has no program; cannot build CTE {}", pi.getUid(), key);
      return;
    }

    String table = "analytics_event_" + pi.getProgram().getUid();
    String boundaries =
        BoundarySqlBuilder.buildSql(
            pi.getAnalyticsPeriodBoundaries(),
            AnalyticsPeriodBoundary.DB_EVENT_DATE,
            pi,
            start,
            end,
            qb);

    String bodySql;

    /* Only countIfValue implemented for now */
    if ("countIfValue".equalsIgnoreCase(p.func())) {
      bodySql =
          String.format(
              "select enrollment, count(%1$s) as value "
                  + "from %2$s "
                  + "where ps = %3$s "
                  + "and %1$s is not null "
                  + "and %1$s = %4$s "
                  + "%5$s "
                  + "group by enrollment",
              qb.quote(p.deUid()), table, qb.singleQuote(p.psUid()), p.valueSql(), boundaries);
    } else {
      log.warn("Unsupported d2 function '{}' in placeholder {}", p.func(), p.raw());
      return;
    }

    ctx.addD2FunctionCte(key, CteDefinition.forD2Function(key, bodySql, "enrollment"));
    log.debug("Generated D2 Function CTE '{}' for function '{}'", key, p.func());
  }

  private String aliasFor(String key, CteContext ctx) {
    CteDefinition def = ctx.getDefinitionByKey(key);
    return def != null ? def.getAlias() : null;
  }
}
