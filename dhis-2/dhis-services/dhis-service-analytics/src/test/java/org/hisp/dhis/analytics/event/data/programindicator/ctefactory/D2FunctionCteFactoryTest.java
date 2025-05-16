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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.hisp.dhis.analytics.common.AnalyticsQueryType;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.event.data.programindicator.ctefactory.placeholder.PlaceholderParser;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class D2FunctionCteFactoryTest extends TestBase {

  private ProgramIndicator pi;
  private SqlBuilder qb;
  private CteContext ctx;
  private D2FunctionCteFactory factory;
  private Map<String, String> alias;
  private Date start, end;
  private Date from, to;
  private static final String DUMMY_EXPRESSION = "#{1234567}";

  @BeforeEach
  void init() {
    Program p = createProgram('A');
    pi = createProgramIndicator('A', p, DUMMY_EXPRESSION, "");
    qb = new PostgreSqlBuilder();
    ctx = new CteContext(AnalyticsQueryType.ENROLLMENT);
    factory = new D2FunctionCteFactory();
    alias = new HashMap<>();
    start = new Date(0);
    end = new Date();
    from = new Date(0);
    to = new Date();
  }

  /* helper */
  private static String b64(String s) {
    return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
  }

  // ---------------------------------------------------------------------
  // Happy-path tests
  // ---------------------------------------------------------------------

  @Nested
  class HappyPath {

    @Test
    void countIfValue() {
      String ph =
          "__D2FUNC__(func='countIfValue', ps='PS1', de='DE1', "
              + "argType='val64', arg64='"
              + b64("42")
              + "', hash='h', pi='"
              + pi.getUid()
              + "')__";
      String sql = "select " + ph;

      String out = factory.process(sql, pi, start, end, ctx, alias, qb);

      assertTrue(out.contains("coalesce("));
      assertEquals(1, ctx.getCteKeys().size());
      assertEquals(1, alias.size());
    }

    @Test
    void countIfCondition() {
      String cond = "'< 10'";
      String ph =
          "__D2FUNC__(func='countIfCondition', ps='PS1', de='DE1', "
              + "argType='condLit64', arg64='"
              + b64(cond)
              + "', hash='h', pi='"
              + pi.getUid()
              + "')__";
      String sql = "where " + ph;

      String out = factory.process(sql, pi, start, end, ctx, alias, qb);

      assertTrue(out.contains("coalesce("));
      assertEquals(1, ctx.getCteKeys().size());
    }

    @Test
    void count() {
      String ph =
          "__D2FUNC__(func='count', ps='PS1', de='DE1', "
              + "argType='none', arg64='', hash='h', pi='"
              + pi.getUid()
              + "')__";
      String sql = "select " + ph;

      String out = factory.process(sql, pi, start, end, ctx, alias, qb);

      assertTrue(out.contains("coalesce("));
      assertEquals(1, ctx.getCteKeys().size());
    }
  }

  @Nested
  class ErrorCases {

    @Test
    void malformedBase64_isIgnored() {
      String ph =
          "__D2FUNC__(func='countIfValue', ps='PS', de='DE', "
              + "argType='val64', arg64='**BAD**', hash='h', pi='"
              + pi.getUid()
              + "')__";
      String sql = "select " + ph;

      String out = factory.process(sql, pi, start, end, ctx, alias, qb);

      assertEquals(sql, out);
      assertTrue(ctx.getCteKeys().isEmpty());
      assertTrue(alias.isEmpty());
    }

    @Test
    void unsupportedFunction_isIgnored() {
      String ph =
          "__D2FUNC__(func='sum', ps='PS', de='DE', "
              + "argType='none', arg64='', hash='h', pi='"
              + pi.getUid()
              + "')__";
      String sql = "select " + ph;

      String out = factory.process(sql, pi, start, end, ctx, alias, qb);

      assertEquals(sql, out);
      assertTrue(ctx.getCteKeys().isEmpty());
    }

    @Test
    void badPlaceholderPattern_isIgnored() {
      String ph = "__D2FUNC__(broken)__";
      String sql = "select " + ph;

      String out = factory.process(sql, pi, start, end, ctx, alias, qb);

      assertEquals(sql, out);
      assertTrue(ctx.getCteKeys().isEmpty());
    }
  }

  @Test
  void regexFindsPlaceholders() {
    String ph1 =
        "__D2FUNC__(func='count', ps='P', de='D', argType='none', arg64='', hash='h', pi='X')__";
    String ph2 =
        "__D2FUNC__(func='count', ps='P', de='D', argType='none', arg64='', hash='h', pi='X')__";
    String sql = ph1 + " + " + ph2;

    int count = 0;
    java.util.regex.Matcher m = PlaceholderParser.d2FuncPattern().matcher(sql);
    while (m.find()) count++;

    assertEquals(2, count);
  }

  @Test
  void emptyArg64_stillGeneratesCte() {
    String ph =
        "__D2FUNC__(func='countIfValue', ps='PS', de='DE', "
            + "argType='val64', arg64='', hash='h', pi='"
            + pi.getUid()
            + "')__";
    String out = factory.process("select " + ph, pi, from, to, ctx, alias, qb);

    assertTrue(out.contains("coalesce("));
    assertEquals(1, ctx.getCteKeys().size());
    assertEquals(1, alias.size());
  }

  @Test
  void conditionWithStringLiteral_generatesSql() {
    String arg = "'>= \"ABC\"'";
    String ph =
        "__D2FUNC__(func='countIfCondition', ps='PS', de='DE', "
            + "argType='condLit64', arg64='"
            + b64(arg)
            + "', hash='h', pi='"
            + pi.getUid()
            + "')__";

    String out = factory.process("x " + ph, pi, from, to, ctx, alias, qb);

    assertTrue(out.contains("coalesce(")); // alias substitution happened
    assertEquals(1, ctx.getCteKeys().size()); // CTE was generated
  }

  // #3 countIfCondition unparsable literal -------------------------------

  @Test
  void unparsableCondition_isIgnored() {
    String arg = "'BETWEEN 1 AND 2'";
    String ph =
        "__D2FUNC__(func='countIfCondition', ps='P', de='D', "
            + "argType='condLit64', arg64='"
            + b64(arg)
            + "', hash='h', pi='"
            + pi.getUid()
            + "')__";
    String sqlIn = "select " + ph;

    String out = factory.process(sqlIn, pi, from, to, ctx, alias, qb);

    assertEquals(sqlIn, out);
    assertTrue(ctx.getCteKeys().isEmpty());
  }

  @Test
  void badBase64_skipsPlaceholder() {
    String ph =
        "__D2FUNC__(func='countIfValue', ps='P', de='D', "
            + "argType='val64', arg64='@@@', hash='h', pi='"
            + pi.getUid()
            + "')__";
    String in = "select " + ph;

    String out = factory.process(in, pi, from, to, ctx, alias, qb);

    assertEquals(in, out);
    assertTrue(ctx.getCteKeys().isEmpty());
  }

  @Test
  void duplicatePlaceholder_createsOneCte() {
    String ph =
        "__D2FUNC__(func='count', ps='P', de='D', argType='none', arg64='', hash='h', pi='"
            + pi.getUid()
            + "')__";
    String sql = ph + " + " + ph;

    String out = factory.process(sql, pi, from, to, ctx, alias, qb);

    assertEquals(1, ctx.getCteKeys().size());
    assertEquals(1, alias.size());
    String al = alias.values().iterator().next();
    assertEquals(2, out.split(al + "\\.value").length - 1); // alias used twice
  }

  @Test
  void piWithoutProgram_leavesPlaceholder() {
    pi.setProgram(null); // TestBase beans are mutable
    String ph =
        "__D2FUNC__(func='count', ps='P', de='D', argType='none', arg64='', hash='h', pi='X')__";
    String sql = "select " + ph;

    String out = factory.process(sql, pi, from, to, ctx, alias, qb);

    assertEquals(sql, out);
    assertTrue(ctx.getCteKeys().isEmpty());
  }

  @Test
  void existingCteAlias_isReused() {
    String placeholder =
        "__D2FUNC__(func='count', ps='P', de='D', argType='none', arg64='', hash='h', pi='"
            + pi.getUid()
            + "')__";

    /* key must match factory logic: sha1("noarg") */
    String hash = SqlHashUtil.sha1("noarg");
    String key = "d2count_P_D_" + hash + "_h_" + pi.getUid();

    CteDefinition seeded = CteDefinition.forD2Function(key, "dummy_sql", "enrollment");
    ctx.addD2FunctionCte(key, seeded);

    String seededAlias = ctx.getDefinitionByKey(key).getAlias(); // alias after registration

    String out = factory.process("select " + placeholder, pi, from, to, ctx, alias, qb);

    // alias reused
    assertTrue(out.contains(seededAlias + ".value"));
    // Still one cte
    assertEquals(1, ctx.getCteKeys().size());
    assertEquals(seededAlias, alias.get(placeholder));
  }

  @Test
  void count_withIgnoredArg64_stillWorks() {
    String ph =
        "__D2FUNC__(func='count', ps='P', de='D', argType='none', arg64='XYZ', hash='h', pi='"
            + pi.getUid()
            + "')__";
    String out = factory.process("select " + ph, pi, from, to, ctx, alias, qb);

    assertTrue(out.contains("coalesce("));
  }

  @Test
  void mixedPlaceholders_bothHandled() {
    String p1 =
        "__D2FUNC__(func='count', ps='P', de='D', argType='none', arg64='', hash='h1', pi='"
            + pi.getUid()
            + "')__";
    String p2 =
        "__D2FUNC__(func='countIfValue', ps='P', de='D', argType='val64', arg64='"
            + b64("1")
            + "', hash='h2', pi='"
            + pi.getUid()
            + "')__";
    String sql = p1 + " || " + p2;

    String out = factory.process(sql, pi, from, to, ctx, alias, qb);

    assertTrue(out.contains("coalesce("));
    assertEquals(2, ctx.getCteKeys().size());
    assertEquals(2, alias.size());
  }
}
