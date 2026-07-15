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
package org.hisp.dhis.dxf2.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Test data-source proxy that both feeds {@code QueryCountHolder} (aggregate select/insert/...
 * counts) and captures the executed SQL text, so a test can assert how many statements match a
 * given pattern (e.g. a specific join table) during a service call. Enable by adding
 * {@code @ContextConfiguration(classes = {QueryCaptureDataSourceProxy.class})} to the test.
 *
 * <p>Deliberately <b>not</b> a {@code @Component}: this class lives in a production-scanned package
 * ({@code org.hisp.dhis.dxf2.metadata}), so annotating it would register it in every integration
 * test's application context and double-wrap the data source alongside {@code
 * QueryCountDataSourceProxy}, doubling every query count. Registered only via {@code
 * @ContextConfiguration(classes = ...)} on the test that needs it.
 */
public class QueryCaptureDataSourceProxy implements BeanPostProcessor {

  private static final List<String> CAPTURED = Collections.synchronizedList(new ArrayList<>());

  /** Clears the captured SQL. Call before the code under test. */
  public static void reset() {
    CAPTURED.clear();
  }

  /** Number of executed statements whose SQL contains the given (case-insensitive) pattern. */
  public static long countMatching(String pattern) {
    String needle = pattern.toLowerCase(Locale.ROOT);
    synchronized (CAPTURED) {
      return CAPTURED.stream().filter(sql -> sql.toLowerCase(Locale.ROOT).contains(needle)).count();
    }
  }

  @Override
  public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) {
    if (bean instanceof DataSource originalDataSource && beanName.equals("actualDataSource")) {
      ChainListener listener = new ChainListener();
      listener.addListener(new DataSourceQueryCountListener());
      listener.addListener(
          new QueryExecutionListener() {
            // No-op: we only record SQL once it has actually executed (afterQuery). Capturing in
            // beforeQuery would count statements that error out or never run.
            @Override
            public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {}

            @Override
            public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
              queryInfoList.forEach(q -> CAPTURED.add(q.getQuery()));
            }
          });
      return ProxyDataSourceBuilder.create(originalDataSource)
          .name("query-capture-datasource-proxy")
          .listener(listener)
          .build();
    }
    return bean;
  }
}
