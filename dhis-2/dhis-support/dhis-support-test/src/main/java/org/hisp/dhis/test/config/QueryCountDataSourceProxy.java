/*
 * Copyright (c) 2004-2024, University of Oslo
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

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * Test class that enhances the originalDatasource with the ability to count queries, and returns it
 * as a proxy. This should only be used in tests. To use, simply add:
 *
 * <pre>{@code
 * @ContextConfiguration(classes = {QueryCountDataSourceProxy.class})
 * }</pre>
 *
 * on your test class and then use:
 *
 * <pre>{@code
 * SQLStatementCountValidator.reset();
 * }</pre>
 *
 * before the condition to test and then use asserts afterward e.g.
 *
 * <pre>{@code
 * assertDeleteCount(1);
 * }</pre>
 */
@Component
public class QueryCountDataSourceProxy implements BeanPostProcessor {

  @Override
  public Object postProcessBeforeInitialization(@Nonnull Object bean, @Nonnull String beanName) {
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName)
      throws BeansException {
    if (bean instanceof DataSource originalDataSource && beanName.equals("actualDataSource")) {
      ChainListener listener = new ChainListener();
      listener.addListener(new DataSourceQueryCountListener());
      return ProxyDataSourceBuilder.create(originalDataSource)
          .name("query-count-datasource-proxy")
          .listener(listener)
          .build();
    }
    return bean;
  }
}
