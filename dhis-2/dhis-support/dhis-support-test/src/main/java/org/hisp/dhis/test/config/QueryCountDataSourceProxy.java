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
