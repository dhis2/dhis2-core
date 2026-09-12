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
package org.hisp.dhis.test.config;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * Test-only datasource proxy that makes selected statements artificially slow by wrapping them in a
 * {@code pg_sleep}. This exists so a test can trip a query timeout against real production SQL.
 *
 * <p>To use, add
 *
 * <pre>{@code
 * @ContextConfiguration(classes = {SlowQueryDataSourceProxy.class})
 * }</pre>
 *
 * to the test class, then arm it around the code under test:
 *
 * <pre>{@code
 * SlowQueryDataSourceProxy.sleepBefore("select te.trackedentityid, te.uid", Duration.ofSeconds(5));
 * // ... run the code under test ...
 * SlowQueryDataSourceProxy.disarm();
 * }</pre>
 *
 * <p>The matched statement is wrapped so the production SQL itself is unchanged and still yields
 * exactly one ResultSet with its original columns. The sleep runs on the same {@code Statement} as
 * the query, so a {@code setQueryTimeout} armed for that statement cancels mid-sleep, which is what
 * makes the timeout observable.
 *
 * <p>Matching is a case-insensitive substring test on the SQL text, and matched statements are
 * counted so a test can assert the injection actually happened rather than passing because the
 * sleep never fired.
 */
@Component
public class SlowQueryDataSourceProxy implements BeanPostProcessor {

  /** SQL pattern (lower case) to the sleep in seconds applied to statements matching it. */
  private static final Map<String, Double> SLEEPS = new ConcurrentHashMap<>();

  private static final AtomicInteger MATCHES = new AtomicInteger();

  /**
   * Arms the proxy: every subsequent statement whose SQL contains {@code sqlPattern}
   * (case-insensitive) is slowed down by {@code sleep}. May be called more than once to slow down
   * several different statements within one request.
   */
  public static void sleepBefore(String sqlPattern, Duration sleep) {
    SLEEPS.put(sqlPattern.toLowerCase(Locale.ROOT), sleep.toMillis() / 1000.0);
  }

  /**
   * Stops slowing down statements but keeps the match count. Call as soon as the code under test
   * returns, so no sleep bleeds into the assertions that follow.
   */
  public static void disarm() {
    SLEEPS.clear();
  }

  /** Disarms the proxy and clears the match count. Call in an {@code @AfterEach}. */
  public static void reset() {
    disarm();
    MATCHES.set(0);
  }

  /** How many statements have been slowed down since the last {@link #reset()}. */
  public static int matches() {
    return MATCHES.get();
  }

  /** The sleep armed for the first pattern this query matches, or null if none matches. */
  private static Double matchingSleep(String query) {
    if (SLEEPS.isEmpty()) {
      return null;
    }
    String lower = query.toLowerCase(Locale.ROOT);
    for (Map.Entry<String, Double> sleep : SLEEPS.entrySet()) {
      if (lower.contains(sleep.getKey())) {
        return sleep.getValue();
      }
    }
    return null;
  }

  @Override
  public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName)
      throws BeansException {
    if (bean instanceof DataSource originalDataSource && beanName.equals("actualDataSource")) {
      return ProxyDataSourceBuilder.create(originalDataSource)
          .name("slow-query-datasource-proxy")
          .queryTransformer(
              info -> {
                String query = info.getQuery();
                Double sleepSeconds = matchingSleep(query);
                if (sleepSeconds == null) {
                  return query;
                }
                MATCHES.incrementAndGet();
                // The sleep has to live inside the same single statement: a separate
                // "select pg_sleep(n);" prefix returns a second ResultSet, which JdbcTemplate
                // rejects with "Multiple ResultSets were returned by the query". Wrapping the
                // original query keeps exactly one ResultSet with the original columns.
                //
                // The sleep goes in a CTE that the query's WHERE cannot be pushed into, so it is
                // evaluated exactly once. Putting it in a joined subquery instead makes PostgreSQL
                // evaluate it per output row, which multiplies the delay by the row count.
                return "with __sleep as materialized (select pg_sleep("
                    + sleepSeconds
                    + ") as slept) select __slow.* from ("
                    + query
                    + ") __slow where (select count(*) from __sleep) >= 0";
              })
          .build();
    }
    return bean;
  }
}
