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

package org.hisp.dhis.analytics.data.pipeline;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.hisp.dhis.analytics.*;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.expressionparser.ExpressionParserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hisp.dhis.common.DimensionalObject.*;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getLocalPeriodIdentifiers;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentGraphMap;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentNameGraphMap;

/**
 * Adds meta data values to the given grid based on the given data query
 * parameters.
 *
 */
public class addMetaDataStep
    extends
    BaseStep
{
    private final AnalyticsSecurityManager securityManager;

    public addMetaDataStep(AnalyticsSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public void execute( DataQueryParams params, Grid grid )
    {
        if ( !params.isSkipMeta() )
        {
            Map<String, Object> metaData = new HashMap<>();
            Map<String, Object> internalMetaData = new HashMap<>();

            // -----------------------------------------------------------------
            // Items / names element
            // -----------------------------------------------------------------

            Map<String, String> cocNameMap = AnalyticsUtils.getCocNameMap( params );

            metaData.put( AnalyticsMetaDataKey.ITEMS.getKey(), AnalyticsUtils.getDimensionMetadataItemMap( params ) );

            // -----------------------------------------------------------------
            // Item order elements
            // -----------------------------------------------------------------

            Map<String, Object> dimensionItems = new HashMap<>();

            Calendar calendar = PeriodType.getCalendar();

            List<String> periodUids = calendar.isIso8601()
                ? getUids( params.getDimensionOrFilterItems( PERIOD_DIM_ID ) )
                : getLocalPeriodIdentifiers( params.getDimensionOrFilterItems( PERIOD_DIM_ID ), calendar );

            dimensionItems.put( PERIOD_DIM_ID, periodUids );
            dimensionItems.put( CATEGORYOPTIONCOMBO_DIM_ID, Sets.newHashSet( cocNameMap.keySet() ) );

            for ( DimensionalObject dim : params.getDimensionsAndFilters() )
            {
                if ( !dimensionItems.keySet().contains( dim.getDimension() ) )
                {
                    dimensionItems.put( dim.getDimension(), getDimensionalItemIds( dim.getItems() ) );
                }
            }

            metaData.put( AnalyticsMetaDataKey.DIMENSIONS.getKey(), dimensionItems );

            // -----------------------------------------------------------------
            // Organisation unit hierarchy
            // -----------------------------------------------------------------

            User user = securityManager.getCurrentUser( params );

            List<OrganisationUnit> organisationUnits = asTypedList(
                params.getDimensionOrFilterItems( ORGUNIT_DIM_ID ) );
            Collection<OrganisationUnit> roots = user != null ? user.getOrganisationUnits() : null;

            if ( params.isHierarchyMeta() )
            {
                metaData.put( AnalyticsMetaDataKey.ORG_UNIT_HIERARCHY.getKey(),
                    getParentGraphMap( organisationUnits, roots ) );
            }

            if ( params.isShowHierarchy() )
            {
                Map<Object, List<?>> ancestorMap = organisationUnits.stream()
                    .collect( Collectors.toMap( OrganisationUnit::getUid, ou -> ou.getAncestorNames( roots, true ) ) );

                internalMetaData.put( AnalyticsMetaDataKey.ORG_UNIT_ANCESTORS.getKey(), ancestorMap );
                metaData.put( AnalyticsMetaDataKey.ORG_UNIT_NAME_HIERARCHY.getKey(),
                    getParentNameGraphMap( organisationUnits, roots, true ) );
            }

            grid.setMetaData( ImmutableMap.copyOf( metaData ) );
            grid.setInternalMetaData( ImmutableMap.copyOf( internalMetaData ) );
        }
    }
}
