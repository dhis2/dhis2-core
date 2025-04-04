/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hisp.dhis.common.hibernate.HibernateAnalyticalObjectStore;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expressiondimensionitem.ExpressionDimensionItem;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.predictor.PredictorGroup;
import org.hisp.dhis.program.ProgramExpression;
import org.hisp.dhis.program.ProgramIndicatorGroup;
import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.visualization.Visualization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Luciano Fiandesio
 */
@Configuration("coreStoreConfig")
public class StoreConfig {

  @PersistenceContext private EntityManager entityManager;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ApplicationEventPublisher publisher;

  @Autowired private AclService aclService;

  @Bean("org.hisp.dhis.indicator.IndicatorTypeStore")
  public HibernateIdentifiableObjectStore<IndicatorType> indicatorTypeStore() {
    return new HibernateIdentifiableObjectStore<>(
        entityManager, jdbcTemplate, publisher, IndicatorType.class, aclService, true);
  }

  @Bean("org.hisp.dhis.indicator.IndicatorGroupStore")
  public HibernateIdentifiableObjectStore<IndicatorGroup> indicatorGroupStore() {
    return new HibernateIdentifiableObjectStore<>(
        entityManager, jdbcTemplate, publisher, IndicatorGroup.class, aclService, true);
  }

  @Bean("org.hisp.dhis.indicator.IndicatorGroupSetStore")
  public HibernateIdentifiableObjectStore<IndicatorGroupSet> indicatorGroupSetStore() {
    return new HibernateIdentifiableObjectStore<>(
        entityManager, jdbcTemplate, publisher, IndicatorGroupSet.class, aclService, true);
  }

  @Bean("org.hisp.dhis.predictor.PredictorGroupStore")
  public HibernateIdentifiableObjectStore<PredictorGroup> predictorGroupStore() {
    return new HibernateIdentifiableObjectStore<>(
        entityManager, jdbcTemplate, publisher, PredictorGroup.class, aclService, true);
  }

  @Bean("org.hisp.dhis.expression.ExpressionStore")
  public HibernateGenericStore<Expression> expressionStore() {
    return new HibernateGenericStore<>(
        entityManager, jdbcTemplate, publisher, Expression.class, true);
  }

  @Bean("org.hisp.dhis.expression.ExpressionDimensionItemStore")
  public HibernateGenericStore<ExpressionDimensionItem> expressionDimensionItemStore() {
    return new HibernateGenericStore<>(
        entityManager, jdbcTemplate, publisher, ExpressionDimensionItem.class, true);
  }

  @Bean("org.hisp.dhis.configuration.ConfigurationStore")
  public HibernateGenericStore<org.hisp.dhis.configuration.Configuration> configurationStore() {
    return new HibernateGenericStore<>(
        entityManager,
        jdbcTemplate,
        publisher,
        org.hisp.dhis.configuration.Configuration.class,
        true);
  }

  @Bean("org.hisp.dhis.constant.ConstantStore")
  public HibernateIdentifiableObjectStore<Constant> constantStore() {
    return new HibernateIdentifiableObjectStore<>(
        entityManager, jdbcTemplate, publisher, Constant.class, aclService, true);
  }

  @Bean("org.hisp.dhis.option.OptionSetStore")
  public HibernateIdentifiableObjectStore<OptionSet> optionSetStore() {
    return new HibernateIdentifiableObjectStore<>(
        entityManager, jdbcTemplate, publisher, OptionSet.class, aclService, true);
  }

  @Bean("org.hisp.dhis.legend.LegendSetStore")
  public HibernateIdentifiableObjectStore<LegendSet> legendSetStore() {
    return new HibernateIdentifiableObjectStore<>(
        entityManager, jdbcTemplate, publisher, LegendSet.class, aclService, true);
  }

  @Bean("org.hisp.dhis.program.ProgramIndicatorGroupStore")
  public HibernateIdentifiableObjectStore<ProgramIndicatorGroup> programIndicatorGroupStore() {
    return new HibernateIdentifiableObjectStore<>(
        entityManager, jdbcTemplate, publisher, ProgramIndicatorGroup.class, aclService, true);
  }

  @Bean("org.hisp.dhis.report.ReportStore")
  public HibernateIdentifiableObjectStore<Report> reportStore() {
    return new HibernateIdentifiableObjectStore<>(
        entityManager, jdbcTemplate, publisher, Report.class, aclService, true);
  }

  @Bean("org.hisp.dhis.visualization.generic.VisualizationStore")
  public HibernateAnalyticalObjectStore<Visualization> visuzliationStore() {
    return new HibernateAnalyticalObjectStore<>(
        entityManager, jdbcTemplate, publisher, Visualization.class, aclService, true);
  }

  @Bean("org.hisp.dhis.dashboard.DashboardStore")
  public HibernateIdentifiableObjectStore<Dashboard> dashboardStore() {
    return new HibernateIdentifiableObjectStore<>(
        entityManager, jdbcTemplate, publisher, Dashboard.class, aclService, true);
  }

  @Bean("org.hisp.dhis.program.ProgramExpressionStore")
  public HibernateGenericStore<ProgramExpression> programExpressionStore() {
    return new HibernateGenericStore<>(
        entityManager, jdbcTemplate, publisher, ProgramExpression.class, true);
  }

  /**
   * @deprecated THIS IS BEING DEPRECATED IN FAVOUR OF THE EventVisualization.
   */
  @Deprecated
  @Bean("org.hisp.dhis.eventreport.EventReportStore")
  public HibernateAnalyticalObjectStore<EventReport> eventReportStore() {
    return new HibernateAnalyticalObjectStore<>(
        entityManager, jdbcTemplate, publisher, EventReport.class, aclService, true);
  }

  /**
   * @deprecated THIS IS BEING DEPRECATED IN FAVOUR OF THE EventVisualization.
   */
  @Deprecated
  @Bean("org.hisp.dhis.eventchart.EventChartStore")
  public HibernateAnalyticalObjectStore<EventChart> eventChartStore() {
    return new HibernateAnalyticalObjectStore<>(
        entityManager, jdbcTemplate, publisher, EventChart.class, aclService, true);
  }

  @Bean("org.hisp.dhis.program.notification.ProgramNotificationStore")
  public HibernateIdentifiableObjectStore<ProgramNotificationTemplate> programNotificationStore() {
    return new HibernateIdentifiableObjectStore<>(
        entityManager,
        jdbcTemplate,
        publisher,
        ProgramNotificationTemplate.class,
        aclService,
        true);
  }

  @Bean("org.hisp.dhis.program.notification.ProgramNotificationInstanceStore")
  public HibernateIdentifiableObjectStore<ProgramNotificationInstance>
      programNotificationInstanceStore() {
    return new HibernateIdentifiableObjectStore<>(
        entityManager,
        jdbcTemplate,
        publisher,
        ProgramNotificationInstance.class,
        aclService,
        true);
  }
}
