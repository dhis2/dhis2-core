/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.config;

import static org.hisp.dhis.external.conf.ConfigurationKey.USE_QUERY_CACHE;
import static org.hisp.dhis.external.conf.ConfigurationKey.USE_SECOND_LEVEL_CACHE;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.SessionFactory;
import org.hibernate.cache.jcache.ConfigSettings;
import org.hibernate.cache.jcache.MissingCacheStrategy;
import org.hibernate.cache.jcache.internal.JCacheRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.tool.schema.Action;
import org.hisp.dhis.cache.DefaultHibernateCacheManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dbms.HibernateDbmsManager;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Luciano Fiandesio
 * @author Morten Svanæs
 */
@Configuration
@EnableTransactionManagement
@Slf4j
public class HibernateConfig {

  @Bean
  public static PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
    return new PersistenceExceptionTranslationPostProcessor();
  }

  @Bean
  public JpaTransactionManager jpaTransactionManager(EntityManagerFactory entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory);
  }

  @Bean
  public TransactionTemplate transactionTemplate(JpaTransactionManager transactionManager) {
    return new TransactionTemplate(transactionManager);
  }

  @Bean
  public DefaultHibernateCacheManager cacheManager(EntityManagerFactory emf) {
    DefaultHibernateCacheManager cacheManager = new DefaultHibernateCacheManager();
    cacheManager.setSessionFactory(emf.unwrap(SessionFactory.class));

    return cacheManager;
  }

  @Bean
  public DbmsManager dbmsManager(
      JdbcTemplate jdbcTemplate,
      DefaultHibernateCacheManager cacheManager,
      EntityManager entityManager) {
    return new HibernateDbmsManager(jdbcTemplate, entityManager, cacheManager);
  }

  @Bean
  public EntityManager sharedEntityManager(EntityManagerFactory emf) {
    return SharedEntityManagerCreator.createSharedEntityManager(emf);
  }

  @Bean
  @DependsOn({"flyway"})
  public EntityManagerFactory entityManagerFactory(
      DhisConfigurationProvider dhisConfig, @Qualifier("actualDataSource") DataSource dataSource) {
    HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
    adapter.setDatabasePlatform(dhisConfig.getProperty(ConfigurationKey.CONNECTION_DIALECT));
    adapter.setGenerateDdl(shouldGenerateDDL(dhisConfig));
    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
    factory.setJpaVendorAdapter(adapter);
    factory.setPersistenceUnitName("dhis");
    factory.setPersistenceProviderClass(HibernatePersistenceProvider.class);
    factory.setDataSource(dataSource);
    factory.setPackagesToScan("org.hisp.dhis");
    factory.setMappingResources(loadResources());
    factory.setJpaProperties(getAdditionalProperties(dhisConfig));
    factory.afterPropertiesSet();
    return factory.getObject();
  }

  /**
   * If return true, hibernate will generate the DDL for the database. This is used by h2-test.
   *
   * @param dhisConfig {@link DhisConfigurationProvider}
   * @return TRUE if connection.schema is not set to none
   */
  private boolean shouldGenerateDDL(DhisConfigurationProvider dhisConfig) {
    return "update".equals(dhisConfig.getProperty(ConfigurationKey.CONNECTION_SCHEMA));
  }

  /**
   * Loads all the hibernate mapping files from the classpath
   *
   * @return Array of Strings representing the mapping files
   */
  private String[] loadResources() {
    try {
      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] resources = resolver.getResources("classpath*:org/hisp/dhis/**/*.hbm.xml");

      List<String> list = new ArrayList<>();
      for (Resource resource : resources) {
        String url = resource.getURL().toString();
        list.add(url);
      }
      return list.toArray(new String[0]);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  /**
   * Returns additional properties to be used by the {@link LocalContainerEntityManagerFactoryBean}
   */
  private Properties getAdditionalProperties(DhisConfigurationProvider dhisConfig) {
    Properties properties = new Properties();
    properties.put(
        "hibernate.current_session_context_class",
        "org.springframework.orm.hibernate5.SpringSessionContext");

    if (dhisConfig.getProperty(USE_SECOND_LEVEL_CACHE).equals("true")) {
      properties.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, "true");
      properties.put(AvailableSettings.CACHE_REGION_FACTORY, JCacheRegionFactory.class.getName());
      properties.put(AvailableSettings.USE_QUERY_CACHE, dhisConfig.getProperty(USE_QUERY_CACHE));
      properties.put(
          ConfigSettings.MISSING_CACHE_STRATEGY,
          MissingCacheStrategy.CREATE.getExternalRepresentation());
    }

    properties.put(
        AvailableSettings.HBM2DDL_AUTO,
        Action.valueOf(dhisConfig.getProperty(ConfigurationKey.CONNECTION_SCHEMA).toUpperCase()));

    properties.put(
        AvailableSettings.SHOW_SQL, dhisConfig.getProperty(ConfigurationKey.HIBERNATE_SHOW_SQL));
    properties.put(
        AvailableSettings.FORMAT_SQL, dhisConfig.getProperty(ConfigurationKey.HIBERNATE_SHOW_SQL));
    properties.put(
        AvailableSettings.HIGHLIGHT_SQL,
        dhisConfig.getProperty(ConfigurationKey.HIBERNATE_SHOW_SQL));

    // TODO: this is anti-pattern and should be turn off
    properties.put("hibernate.allow_update_outside_transaction", "true");

    return properties;
  }
}
