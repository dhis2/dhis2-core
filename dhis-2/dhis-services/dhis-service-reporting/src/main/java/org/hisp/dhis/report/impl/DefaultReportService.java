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
package org.hisp.dhis.report.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.commons.util.Encoder;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.report.ReportService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.JRExportUtils;
import org.hisp.dhis.system.velocity.VelocityManager;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationGridService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service( "org.hisp.dhis.report.ReportService" )
public class DefaultReportService
    implements ReportService
{
    private static final String ORGUNIT_LEVEL_COLUMN_PREFIX = "idlevel";

    private static final String ORGUNIT_UID_LEVEL_COLUMN_PREFIX = "uidlevel";

    private static final Encoder ENCODER = new Encoder();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final IdentifiableObjectStore<Report> reportStore;

    private final VisualizationGridService visualizationService;

    private final ConstantService constantService;

    private final OrganisationUnitService organisationUnitService;

    private final PeriodService periodService;

    private final I18nManager i18nManager;

    private final DataSource dataSource;

    private final SystemSettingManager systemSettingManager;

    public DefaultReportService(
        @Qualifier( "org.hisp.dhis.report.ReportStore" ) IdentifiableObjectStore<Report> reportStore,
        VisualizationGridService visualizationService,
        ConstantService constantService, OrganisationUnitService organisationUnitService, PeriodService periodService,
        I18nManager i18nManager, DataSource dataSource, SystemSettingManager systemSettingManager )
    {
        checkNotNull( reportStore );
        checkNotNull( visualizationService );
        checkNotNull( constantService );
        checkNotNull( organisationUnitService );
        checkNotNull( periodService );
        checkNotNull( i18nManager );
        checkNotNull( dataSource );
        checkNotNull( systemSettingManager );

        this.reportStore = reportStore;
        this.visualizationService = visualizationService;
        this.constantService = constantService;
        this.organisationUnitService = organisationUnitService;
        this.periodService = periodService;
        this.i18nManager = i18nManager;
        this.dataSource = dataSource;
        this.systemSettingManager = systemSettingManager;
    }

    // -------------------------------------------------------------------------
    // ReportService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional( readOnly = true )
    public JasperPrint renderReport( OutputStream out, String reportUid, Period period,
        String organisationUnitUid, String type )
    {
        I18nFormat format = i18nManager.getI18nFormat();

        Report report = getReport( reportUid );

        Map<String, Object> params = new HashMap<>();

        params.putAll( constantService.getConstantParameterMap() );

        Date reportDate = new Date();

        if ( period != null )
        {
            params.put( PARAM_PERIOD_NAME, format.formatPeriod( period ) );

            reportDate = period.getStartDate();
        }

        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( organisationUnitUid );

        if ( orgUnit != null )
        {
            int level = orgUnit.getLevel();

            params.put( PARAM_ORGANISATIONUNIT_COLUMN_NAME, orgUnit.getName() );
            params.put( PARAM_ORGANISATIONUNIT_LEVEL, level );
            params.put( PARAM_ORGANISATIONUNIT_LEVEL_COLUMN, ORGUNIT_LEVEL_COLUMN_PREFIX + level );
            params.put( PARAM_ORGANISATIONUNIT_UID_LEVEL_COLUMN, ORGUNIT_UID_LEVEL_COLUMN_PREFIX + level );
        }

        JasperPrint print;

        log.debug( String.format( "Get report for report date: '%s', org unit: '%s'",
            DateUtils.getMediumDateString( reportDate ), organisationUnitUid ) );

        try
        {
            JasperReport jasperReport = JasperCompileManager
                .compileReport( IOUtils.toInputStream( report.getDesignContent(), StandardCharsets.UTF_8 ) );

            if ( report.hasVisualization() ) // Use JR data source
            {
                Visualization visualization = report.getVisualization();

                Grid grid = visualizationService.getVisualizationGrid( visualization.getUid(), reportDate,
                    organisationUnitUid );

                print = JasperFillManager.fillReport( jasperReport, params, grid );
            }
            else // Use JDBC data source
            {
                if ( report.hasRelativePeriods() )
                {
                    AnalyticsFinancialYearStartKey financialYearStart = systemSettingManager
                        .getSystemSetting( SettingKey.ANALYTICS_FINANCIAL_YEAR_START,
                            AnalyticsFinancialYearStartKey.class );

                    List<Period> relativePeriods = report.getRelatives().getRelativePeriods( reportDate, null, false,
                        financialYearStart );

                    String periodString = getCommaDelimitedString(
                        getIdentifiers( periodService.reloadPeriods( relativePeriods ) ) );
                    String isoPeriodString = getCommaDelimitedString(
                        IdentifiableObjectUtils.getUids( relativePeriods ) );

                    params.put( PARAM_RELATIVE_PERIODS, periodString );
                    params.put( PARAM_RELATIVE_ISO_PERIODS, isoPeriodString );
                }

                if ( report.hasReportParams() && report.getReportParams().isOrganisationUnit() && orgUnit != null )
                {
                    params.put( PARAM_ORG_UNITS, String.valueOf( orgUnit.getId() ) );
                    params.put( PARAM_ORG_UNITS_UID, String.valueOf( orgUnit.getUid() ) );
                }

                Connection connection = DataSourceUtils.getConnection( dataSource );

                try
                {
                    print = JasperFillManager.fillReport( jasperReport, params, connection );
                }
                finally
                {
                    DataSourceUtils.releaseConnection( connection, dataSource );
                }
            }

            if ( print != null )
            {
                JRExportUtils.export( type, out, print );
            }
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Failed to render report", ex );
        }

        return print;
    }

    @Override
    @Transactional( readOnly = true )
    public void renderHtmlReport( Writer writer, String uid, Date date, String ou )
    {
        Report report = getReport( uid );
        OrganisationUnit organisationUnit = null;
        List<OrganisationUnit> organisationUnitHierarchy = new ArrayList<>();
        List<OrganisationUnit> organisationUnitChildren = new ArrayList<>();
        List<String> periods = new ArrayList<>();

        I18nFormat format = i18nManager.getI18nFormat();

        if ( ou != null )
        {
            organisationUnit = organisationUnitService.getOrganisationUnit( ou );

            if ( organisationUnit != null )
            {
                organisationUnitHierarchy.add( organisationUnit );

                OrganisationUnit parent = organisationUnit;

                while ( parent.getParent() != null )
                {
                    parent = parent.getParent();
                    organisationUnitHierarchy.add( parent );
                }

                organisationUnitChildren.addAll( organisationUnit.getChildren() );
            }
        }

        Calendar calendar = PeriodType.getCalendar();

        if ( report != null && report.hasRelativePeriods() )
        {
            AnalyticsFinancialYearStartKey financialYearStart = systemSettingManager
                .getSystemSetting( SettingKey.ANALYTICS_FINANCIAL_YEAR_START, AnalyticsFinancialYearStartKey.class );

            if ( calendar.isIso8601() )
            {
                for ( Period period : report.getRelatives().getRelativePeriods( date, format, true,
                    financialYearStart ) )
                {
                    periods.add( period.getIsoDate() );
                }
            }
            else
            {
                periods = IdentifiableObjectUtils
                    .getLocalPeriodIdentifiers( report.getRelatives().getRelativePeriods( date, format, true,
                        financialYearStart ), calendar );
            }
        }

        String dateString = DateUtils.getMediumDateString( date );

        if ( date != null && !calendar.isIso8601() )
        {
            dateString = calendar.formattedDate( calendar.fromIso( date ) );
        }

        final VelocityContext context = new VelocityContext();
        context.put( "report", report );
        context.put( "organisationUnit", organisationUnit );
        context.put( "organisationUnitHierarchy", organisationUnitHierarchy );
        context.put( "organisationUnitChildren", organisationUnitChildren );
        context.put( "date", dateString );
        context.put( "periods", periods );
        context.put( "format", format );
        context.put( "encoder", ENCODER );

        new VelocityManager().getEngine().getTemplate( "html-report.vm" ).merge( context, writer );
    }

    @Override
    @Transactional
    public long saveReport( Report report )
    {
        reportStore.save( report );

        return report.getId();
    }

    @Override
    @Transactional
    public void deleteReport( Report report )
    {
        reportStore.delete( report );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Report> getAllReports()
    {
        return reportStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public Report getReport( long id )
    {
        return reportStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public Report getReport( String uid )
    {
        return reportStore.getByUid( uid );
    }
}
