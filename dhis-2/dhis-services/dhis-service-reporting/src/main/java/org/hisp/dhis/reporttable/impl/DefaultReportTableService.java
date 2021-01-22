/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.reporttable.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.common.*;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.reporttable.ReportTableService;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.reporttable.ReportTableService" )
public class DefaultReportTableService
    extends GenericAnalyticalObjectService<ReportTable>
    implements ReportTableService
{
    // ---------------------------------------------------------------------
    // Dependencies
    // ---------------------------------------------------------------------

    private final AnalyticsService analyticsService;

    private final AnalyticalObjectStore<ReportTable> reportTableStore;

    private OrganisationUnitService organisationUnitService;

    private final CurrentUserService currentUserService;

    private final I18nManager i18nManager;

    public DefaultReportTableService( AnalyticsService analyticsService,
        @Qualifier( "org.hisp.dhis.reporttable.ReportTableStore" ) AnalyticalObjectStore<ReportTable> reportTableStore,
        OrganisationUnitService organisationUnitService,
        CurrentUserService currentUserService, I18nManager i18nManager )
    {
        checkNotNull( analyticsService );
        checkNotNull( reportTableStore );
        checkNotNull( organisationUnitService );
        checkNotNull( currentUserService );
        checkNotNull( i18nManager );

        this.analyticsService = analyticsService;
        this.reportTableStore = reportTableStore;
        this.organisationUnitService = organisationUnitService;
        this.currentUserService = currentUserService;
        this.i18nManager = i18nManager;
    }

    // -------------------------------------------------------------------------
    // ReportTableService implementation
    // -------------------------------------------------------------------------

    @Override
    protected AnalyticalObjectStore<ReportTable> getAnalyticalObjectStore()
    {
        return reportTableStore;
    }

    @Override
    @Transactional( readOnly = true )
    public Grid getReportTableGrid( String uid, Date reportingPeriod, String organisationUnitUid )
    {
        return getReportTableGridByUser( uid, reportingPeriod, organisationUnitUid,
            currentUserService.getCurrentUser() );
    }

    @Override
    @Transactional( readOnly = true )
    public Grid getReportTableGridByUser( String uid, Date reportingPeriod, String organisationUnitUid, User user )
    {
        I18nFormat format = i18nManager.getI18nFormat();

        ReportTable reportTable = getReportTable( uid );

        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( organisationUnitUid );

        List<OrganisationUnit> atLevels = new ArrayList<>();
        List<OrganisationUnit> inGroups = new ArrayList<>();

        if ( reportTable.hasOrganisationUnitLevels() )
        {
            atLevels.addAll( organisationUnitService
                .getOrganisationUnitsAtLevels( reportTable.getOrganisationUnitLevels(),
                    reportTable.getOrganisationUnits() ) );
        }

        if ( reportTable.hasItemOrganisationUnitGroups() )
        {
            inGroups.addAll( organisationUnitService.getOrganisationUnits( reportTable.getItemOrganisationUnitGroups(),
                reportTable.getOrganisationUnits() ) );
        }

        reportTable.init( user, reportingPeriod, organisationUnit, atLevels, inGroups, format );

        Map<String, Object> valueMap = analyticsService.getAggregatedDataValueMapping( reportTable );

        Grid reportTableGrid = reportTable.getGrid( new ListGrid(), valueMap, DisplayProperty.SHORTNAME, true );

        reportTable.clearTransientState();

        return reportTableGrid;
    }

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long saveReportTable( ReportTable reportTable )
    {
        reportTableStore.save( reportTable );

        return reportTable.getId();
    }

    @Override
    @Transactional
    public void updateReportTable( ReportTable reportTable )
    {
        reportTableStore.update( reportTable );
    }

    @Override
    @Transactional
    public void deleteReportTable( ReportTable reportTable )
    {
        reportTableStore.delete( reportTable );
    }

    @Override
    @Transactional( readOnly = true )
    public ReportTable getReportTable( long id )
    {
        return reportTableStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public ReportTable getReportTable( String uid )
    {
        return reportTableStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public ReportTable getReportTableNoAcl( String uid )
    {
        return reportTableStore.getByUidNoAcl( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<ReportTable> getAllReportTables()
    {
        return reportTableStore.getAll();
    }
}
