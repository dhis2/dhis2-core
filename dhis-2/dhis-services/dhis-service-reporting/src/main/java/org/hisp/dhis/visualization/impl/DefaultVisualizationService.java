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
package org.hisp.dhis.visualization.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.common.DisplayProperty.SHORTNAME;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.common.AnalyticalObjectStore;
import org.hisp.dhis.common.GenericAnalyticalObjectService;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service( "org.hisp.dhis.visualization.VisualizationService" )
public class DefaultVisualizationService
    extends
    GenericAnalyticalObjectService<Visualization>
    implements
    VisualizationService
{

    private final AnalyticalObjectStore<Visualization> visualizationStore;

    private final AnalyticsService analyticsService;

    private final OrganisationUnitService organisationUnitService;

    private final CurrentUserService currentUserService;

    private final I18nManager i18nManager;

    public DefaultVisualizationService( final AnalyticsService analyticsService,
        @Qualifier( "org.hisp.dhis.visualization.generic.VisualizationStore" )
        final AnalyticalObjectStore<Visualization> visualizationStore,
        final OrganisationUnitService organisationUnitService, final CurrentUserService currentUserService,
        final I18nManager i18nManager )
    {
        checkNotNull( analyticsService );
        checkNotNull( visualizationStore );
        checkNotNull( organisationUnitService );
        checkNotNull( currentUserService );
        checkNotNull( i18nManager );

        this.analyticsService = analyticsService;
        this.visualizationStore = visualizationStore;
        this.organisationUnitService = organisationUnitService;
        this.currentUserService = currentUserService;
        this.i18nManager = i18nManager;
    }

    @Override
    protected AnalyticalObjectStore<Visualization> getAnalyticalObjectStore()
    {
        return visualizationStore;
    }

    @Override
    @Transactional
    public long save( final Visualization visualization )
    {
        visualizationStore.save( visualization );

        return visualization.getId();
    }

    @Override
    @Transactional( readOnly = true )
    public Visualization loadVisualization( final long id )
    {
        return visualizationStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public Visualization loadVisualization( final String uid )
    {
        return visualizationStore.getByUid( uid );
    }

    @Override
    @Transactional
    public void delete( final Visualization visualization )
    {
        visualizationStore.delete( visualization );
    }

    @Override
    @Transactional( readOnly = true )
    public Grid getVisualizationGrid( final String uid, final Date relativePeriodDate,
        final String organisationUnitUid )
    {
        return getVisualizationGridByUser( uid, relativePeriodDate, organisationUnitUid,
            currentUserService.getCurrentUser() );
    }

    @Override
    @Transactional( readOnly = true )
    public Grid getVisualizationGridByUser( final String uid, final Date relativePeriodDate,
        final String organisationUnitUid, final User user )
    {
        Visualization visualization = loadVisualization( uid );
        final boolean hasPermission = visualization != null;

        if ( hasPermission )
        {
            I18nFormat format = i18nManager.getI18nFormat();
            OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( organisationUnitUid );

            List<OrganisationUnit> atLevels = new ArrayList<>();
            List<OrganisationUnit> inGroups = new ArrayList<>();

            if ( visualization.hasOrganisationUnitLevels() )
            {
                atLevels.addAll( organisationUnitService.getOrganisationUnitsAtLevels(
                    visualization.getOrganisationUnitLevels(), visualization.getOrganisationUnits() ) );
            }

            if ( visualization.hasItemOrganisationUnitGroups() )
            {
                inGroups.addAll( organisationUnitService.getOrganisationUnits(
                    visualization.getItemOrganisationUnitGroups(), visualization.getOrganisationUnits() ) );
            }

            visualization.init( user, relativePeriodDate, organisationUnit, atLevels, inGroups, format );

            Map<String, Object> valueMap = analyticsService.getAggregatedDataValueMapping( visualization );

            Grid visualizationGrid = visualization.getGrid( new ListGrid(), valueMap, SHORTNAME, true );

            visualization.clearTransientState();

            return visualizationGrid;
        }
        else
        {
            return new ListGrid();
        }
    }

    @Override
    @Transactional( readOnly = true )
    public Visualization getVisualizationNoAcl( final String uid )
    {
        return visualizationStore.getByUidNoAcl( uid );
    }
}
