/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hibernate.SessionFactory;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.color.ColorSet;
import org.hisp.dhis.common.hibernate.HibernateAnalyticalObjectStore;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.deletedobject.DeletedObjectService;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.fileresource.FileResource;
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
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Luciano Fiandesio
 */
@Configuration( "coreStoreConfig" )
public class StoreConfig
{
    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private DeletedObjectService deletedObjectService;

    @Autowired
    private AclService aclService;

    @Bean( "org.hisp.dhis.indicator.IndicatorTypeStore" )
    public HibernateIdentifiableObjectStore indicatorTypeStore()
    {
        return new HibernateIdentifiableObjectStore<>( sessionFactory,
            jdbcTemplate, IndicatorType.class, currentUserService, deletedObjectService, aclService, true );
    }

    @Bean( "org.hisp.dhis.indicator.IndicatorGroupStore" )
    public HibernateIdentifiableObjectStore indicatorGroupStore()
    {
        return new HibernateIdentifiableObjectStore<>( sessionFactory,
            jdbcTemplate, IndicatorGroup.class, currentUserService, deletedObjectService, aclService, true );
    }

    @Bean( "org.hisp.dhis.indicator.IndicatorGroupSetStore" )
    public HibernateIdentifiableObjectStore indicatorGroupSetStore()
    {
        return new HibernateIdentifiableObjectStore<>(
            sessionFactory, jdbcTemplate, IndicatorGroupSet.class, currentUserService, deletedObjectService,
            aclService, true );
    }

    @Bean( "org.hisp.dhis.predictor.PredictorGroupStore" )
    public HibernateIdentifiableObjectStore predictorGroupStore()
    {
        return new HibernateIdentifiableObjectStore<>( sessionFactory,
            jdbcTemplate, PredictorGroup.class, currentUserService, deletedObjectService, aclService, true );
    }

    @Bean( "org.hisp.dhis.expression.ExpressionStore" )
    public HibernateGenericStore expressionStore()
    {
        return new HibernateGenericStore<>( sessionFactory, jdbcTemplate,
            Expression.class, true );
    }

    @Bean( "org.hisp.dhis.user.UserGroupStore" )
    public HibernateIdentifiableObjectStore userGroupStore()
    {
        return new HibernateIdentifiableObjectStore<>( sessionFactory,
            jdbcTemplate, UserGroup.class, currentUserService, deletedObjectService, aclService, true );
    }

    @Bean( "org.hisp.dhis.user.UserGroupAccessStore" )
    public HibernateGenericStore userGroupAccessStore()
    {
        return new HibernateGenericStore<>( sessionFactory, jdbcTemplate,
            UserGroupAccess.class, true );
    }

    @Bean( "org.hisp.dhis.user.UserAccessStore" )
    public HibernateGenericStore userAccessStore()
    {
        return new HibernateGenericStore<>( sessionFactory, jdbcTemplate,
            UserAccess.class, true );
    }

    @Bean( "org.hisp.dhis.configuration.ConfigurationStore" )
    public HibernateGenericStore configurationStore()
    {
        return new HibernateGenericStore<>(
            sessionFactory, jdbcTemplate, org.hisp.dhis.configuration.Configuration.class, true );
    }

    @Bean( "org.hisp.dhis.constant.ConstantStore" )
    public HibernateIdentifiableObjectStore constantStore()
    {
        return new HibernateIdentifiableObjectStore<>( sessionFactory,
            jdbcTemplate, Constant.class, currentUserService, deletedObjectService, aclService, true );
    }

    @Bean( "org.hisp.dhis.scheduling.JobConfigurationStore" )
    public HibernateIdentifiableObjectStore jobConfigurationStore()
    {
        return new HibernateIdentifiableObjectStore<>(
            sessionFactory, jdbcTemplate, JobConfiguration.class, currentUserService, deletedObjectService,
            aclService, true );
    }

    @Bean( "org.hisp.dhis.option.OptionSetStore" )
    public HibernateIdentifiableObjectStore optionSetStore()
    {
        return new HibernateIdentifiableObjectStore<>( sessionFactory,
            jdbcTemplate, OptionSet.class, currentUserService, deletedObjectService, aclService, true );
    }

    @Bean( "org.hisp.dhis.legend.LegendSetStore" )
    public HibernateIdentifiableObjectStore legendSetStore()
    {
        return new HibernateIdentifiableObjectStore<>( sessionFactory,
            jdbcTemplate, LegendSet.class, currentUserService, deletedObjectService, aclService, true );
    }

    @Bean( "org.hisp.dhis.program.ProgramIndicatorGroupStore" )
    public HibernateIdentifiableObjectStore programIndicatorGroupStore()
    {
        return new HibernateIdentifiableObjectStore<>(
            sessionFactory, jdbcTemplate, ProgramIndicatorGroup.class, currentUserService, deletedObjectService,
            aclService, true );
    }

    @Bean( "org.hisp.dhis.report.ReportStore" )
    public HibernateIdentifiableObjectStore reportStore()
    {
        return new HibernateIdentifiableObjectStore<>( sessionFactory,
            jdbcTemplate, Report.class, currentUserService, deletedObjectService, aclService, true );
    }

    @Bean( "org.hisp.dhis.chart.ChartStore" )
    public HibernateAnalyticalObjectStore chartStore()
    {
        return new HibernateAnalyticalObjectStore<>( sessionFactory,
            jdbcTemplate, Chart.class, currentUserService, deletedObjectService, aclService, true );
    }

    @Bean( "org.hisp.dhis.reporttable.ReportTableStore" )
    public HibernateAnalyticalObjectStore reportTableStore()
    {
        return new HibernateAnalyticalObjectStore<>( sessionFactory,
            jdbcTemplate, ReportTable.class, currentUserService, deletedObjectService, aclService, true );
    }

    @Bean( "org.hisp.dhis.dashboard.DashboardStore" )
    public HibernateIdentifiableObjectStore dashboardStore()
    {
        return new HibernateIdentifiableObjectStore<>( sessionFactory,
            jdbcTemplate, Dashboard.class, currentUserService, deletedObjectService, aclService, true );
    }

    @Bean( "org.hisp.dhis.program.ProgramExpressionStore" )
    public HibernateGenericStore programExpressionStore()
    {
        return new HibernateGenericStore<>( sessionFactory, jdbcTemplate,
            ProgramExpression.class, true );
    }

    @Bean( "org.hisp.dhis.eventreport.EventReportStore" )
    public HibernateAnalyticalObjectStore eventReportStore()
    {
        return new HibernateAnalyticalObjectStore<>( sessionFactory,
            jdbcTemplate, EventReport.class, currentUserService, deletedObjectService, aclService, true );
    }

    @Bean( "org.hisp.dhis.eventchart.EventChartStore" )
    public HibernateAnalyticalObjectStore eventChartStore()
    {
        return new HibernateAnalyticalObjectStore<>( sessionFactory,
            jdbcTemplate, EventChart.class, currentUserService, deletedObjectService, aclService, true );
    }

    @Bean( "org.hisp.dhis.color.ColorSetStore" )
    public HibernateIdentifiableObjectStore colorSetStore()
    {
        return new HibernateIdentifiableObjectStore<>( sessionFactory,
            jdbcTemplate, ColorSet.class, currentUserService, deletedObjectService, aclService, true );
    }

    @Bean( "org.hisp.dhis.program.notification.ProgramNotificationStore" )
    public HibernateIdentifiableObjectStore programNotificationStore()
    {
        return new HibernateIdentifiableObjectStore<>(
            sessionFactory, jdbcTemplate, ProgramNotificationTemplate.class, currentUserService, deletedObjectService,
            aclService, true );
    }

    @Bean( "org.hisp.dhis.program.notification.ProgramNotificationInstanceStore" )
    public HibernateIdentifiableObjectStore programNotificationInstanceStore()
    {
        return new HibernateIdentifiableObjectStore<>(
            sessionFactory, jdbcTemplate, ProgramNotificationInstance.class, currentUserService, deletedObjectService,
            aclService, true );
    }
}
