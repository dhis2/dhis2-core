package org.hisp.dhis.analytics.data;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ATTRIBUTEOPTIONCOMBO;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_CATEGORYOPTIONCOMBO;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_DATA_X;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_LATITUDE;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_LONGITUDE;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ORGUNIT;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_PERIOD;
import static org.hisp.dhis.analytics.DataQueryParams.KEY_DE_GROUP;
import static org.hisp.dhis.analytics.DataQueryParams.KEY_IN_GROUP;
import static org.hisp.dhis.common.DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.LATITUDE_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.LONGITUDE_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getLocalPeriodIdentifier;
import static org.hisp.dhis.common.DimensionalObjectUtils.asList;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;
import static org.hisp.dhis.commons.collection.ListUtils.sort;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_LEVEL;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_ORGUNIT_GROUP;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT_CHILDREN;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT_GRANDCHILDREN;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.*;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class DefaultDataQueryService
    implements DataQueryService
{
    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private AnalyticsSecurityManager securityManager;

    @Autowired
    private I18nManager i18nManager;

    public void setSecurityManager( AnalyticsSecurityManager securityManager )
    {
        this.securityManager = securityManager;
    }

    // -------------------------------------------------------------------------
    // DataQueryService implementation
    // -------------------------------------------------------------------------

    @Override
    public DataQueryParams getFromUrl( Set<String> dimensionParams, Set<String> filterParams, AggregationType aggregationType,
        String measureCriteria, boolean skipMeta, boolean skipData, boolean skipRounding, boolean completedOnly, boolean hierarchyMeta, boolean ignoreLimit,
        boolean hideEmptyRows, boolean showHierarchy, boolean includeNumDen, DisplayProperty displayProperty, IdentifiableProperty outputIdScheme, IdScheme inputIdScheme,
        String approvalLevel, Date relativePeriodDate, String userOrgUnit, I18nFormat format )
    {
        DataQueryParams.Builder params = DataQueryParams.newBuilder();

        inputIdScheme = ObjectUtils.firstNonNull( inputIdScheme, IdScheme.UID );

        if ( dimensionParams != null && !dimensionParams.isEmpty() )
        {
            params.addDimensions( getDimensionalObjects( dimensionParams, relativePeriodDate, userOrgUnit, format, inputIdScheme ) );
        }

        if ( filterParams != null && !filterParams.isEmpty() )
        {
            params.addFilters( getDimensionalObjects( filterParams, relativePeriodDate, userOrgUnit, format, inputIdScheme ) );
        }

        if ( measureCriteria != null && !measureCriteria.isEmpty() )
        {
            params.withMeasureCriteria( DataQueryParams.getMeasureCriteriaFromParam( measureCriteria ) );
        }

        return params
            .withAggregationType( aggregationType )
            .withSkipMeta( skipMeta )
            .withSkipData( skipData )
            .withSkipRounding( skipRounding )
            .withCompletedOnly( completedOnly )
            .withIgnoreLimit( ignoreLimit )
            .withHierarchyMeta( hierarchyMeta )
            .withHideEmptyRows( hideEmptyRows )
            .withShowHierarchy( showHierarchy )
            .withIncludeNumDen( includeNumDen )
            .withDisplayProperty( displayProperty )
            .withOutputIdScheme( outputIdScheme )
            .withApprovalLevel( approvalLevel ).build();
    }

    @Override
    public DataQueryParams getFromAnalyticalObject( AnalyticalObject object )
    {
        DataQueryParams.Builder params = DataQueryParams.newBuilder();
        
        I18nFormat format = i18nManager.getI18nFormat();
        
        IdScheme idScheme = IdScheme.UID;

        if ( object != null )
        {
            List<OrganisationUnit> userOrgUnits = getUserOrgUnits( null, null );

            Date date = object.getRelativePeriodDate();

            object.populateAnalyticalProperties();

            for ( DimensionalObject column : object.getColumns() )
            {
                params.addDimension( getDimension( column.getDimension(), getDimensionalItemIds( column.getItems() ), date, userOrgUnits, format, false, idScheme ) );
            }

            for ( DimensionalObject row : object.getRows() )
            {
                params.addDimension( getDimension( row.getDimension(), getDimensionalItemIds( row.getItems() ), date, userOrgUnits, format, false, idScheme ) );
            }

            for ( DimensionalObject filter : object.getFilters() )
            {
                params.addFilter( getDimension( filter.getDimension(), getDimensionalItemIds( filter.getItems() ), date, userOrgUnits, format, false, idScheme ) );
            }
        }

        return params.build();
    }

    @Override
    public List<DimensionalObject> getDimensionalObjects( Set<String> dimensionParams, Date relativePeriodDate, String userOrgUnit, I18nFormat format, IdScheme inputIdScheme )
    {
        List<DimensionalObject> list = new ArrayList<>();

        List<OrganisationUnit> userOrgUnits = getUserOrgUnits( null, userOrgUnit );

        if ( dimensionParams != null )
        {
            for ( String param : dimensionParams )
            {
                String dimension = DimensionalObjectUtils.getDimensionFromParam( param );
                List<String> items = DimensionalObjectUtils.getDimensionItemsFromParam( param );

                if ( dimension != null && items != null )
                {
                    list.add( getDimension( dimension, items, relativePeriodDate, userOrgUnits, format, false, inputIdScheme ) );
                }
            }
        }

        return list;
    }

    // TODO verify that current user can read each dimension and dimension item
    // TODO optimize so that org unit levels + boundary are used in query instead of fetching all org units one by one

    @Override
    public DimensionalObject getDimension( String dimension, List<String> items, Date relativePeriodDate,
        List<OrganisationUnit> userOrgUnits, I18nFormat format, boolean allowNull, IdScheme inputIdScheme )
    {
        final boolean allItems = items.isEmpty();

        if ( DATA_X_DIM_ID.equals( dimension ) )
        {
            List<DimensionalItemObject> dataDimensionItems = new ArrayList<>();

            for ( String uid : items )
            {
                if ( uid.startsWith( KEY_DE_GROUP ) )
                {
                    String groupUid = DimensionalObjectUtils.getUidFromGroupParam( uid );

                    DataElementGroup group = idObjectManager.getObject( DataElementGroup.class, inputIdScheme, groupUid );

                    if ( group != null )
                    {
                        dataDimensionItems.addAll( group.getMembers() );
                    }
                }
                else if ( uid.startsWith( KEY_IN_GROUP ) )
                {
                    String groupUid = DimensionalObjectUtils.getUidFromGroupParam( uid );

                    IndicatorGroup group = idObjectManager.getObject( IndicatorGroup.class, inputIdScheme, groupUid );

                    if ( group != null )
                    {
                        dataDimensionItems.addAll( group.getMembers() );
                    }
                }
                else
                {
                    DimensionalItemObject dimItemObject = dimensionService.getOrAddDataDimensionalItemObject( inputIdScheme, uid );

                    if ( dimItemObject != null )
                    {
                        dataDimensionItems.add( dimItemObject );
                    }
                }
            }

            if ( dataDimensionItems.isEmpty() )
            {
                throw new IllegalQueryException( "Dimension dx is present in query without any valid dimension options" );
            }

            DimensionalObject object = new BaseDimensionalObject( dimension, DimensionType.DATA_X, null, DISPLAY_NAME_DATA_X, dataDimensionItems );

            return object;
        }

        else if ( CATEGORYOPTIONCOMBO_DIM_ID.equals( dimension ) )
        {
            List<DimensionalItemObject> cocs = new ArrayList<>();

            for ( String uid : items )
            {
                DataElementCategoryOptionCombo coc = idObjectManager.getObject( DataElementCategoryOptionCombo.class, inputIdScheme, uid );

                if ( coc != null )
                {
                    cocs.add( coc );
                }
            }

            DimensionalObject object = new BaseDimensionalObject( dimension, DimensionType.CATEGORY_OPTION_COMBO, null, DISPLAY_NAME_CATEGORYOPTIONCOMBO, cocs );

            return object;
        }

        else if ( ATTRIBUTEOPTIONCOMBO_DIM_ID.equals( dimension ) )
        {
            List<DimensionalItemObject> aocs = new ArrayList<>();

            for ( String uid : items )
            {
                DataElementCategoryOptionCombo aoc = idObjectManager.getObject( DataElementCategoryOptionCombo.class, inputIdScheme, uid );

                if ( aoc != null )
                {
                    aocs.add( aoc );
                }
            }

            DimensionalObject object = new BaseDimensionalObject( dimension, DimensionType.ATTRIBUTE_OPTION_COMBO, null, DISPLAY_NAME_ATTRIBUTEOPTIONCOMBO, aocs );

            return object;
        }

        else if ( PERIOD_DIM_ID.equals( dimension ) )
        {
            Calendar calendar = PeriodType.getCalendar();

            List<Period> periods = new ArrayList<>();

            for ( String isoPeriod : items )
            {
                if ( RelativePeriodEnum.contains( isoPeriod ) )
                {
                    RelativePeriodEnum relativePeriod = RelativePeriodEnum.valueOf( isoPeriod );
                    List<Period> relativePeriods = RelativePeriods.getRelativePeriodsFromEnum( relativePeriod, relativePeriodDate, format, true );
                    periods.addAll( relativePeriods );
                }
                else
                {
                    Period period = PeriodType.getPeriodFromIsoString( isoPeriod );

                    if ( period != null )
                    {
                        periods.add( period );
                    }
                }
            }

            periods = periods.stream().distinct().collect( Collectors.toList() ); // Remove duplicates

            if ( periods.isEmpty() )
            {
                throw new IllegalQueryException( "Dimension pe is present in query without any valid dimension options" );
            }

            for ( Period period : periods )
            {
                String name = format != null ? format.formatPeriod( period ) : null;
                period.setName( name );
                period.setShortName( name );

                if ( !calendar.isIso8601() )
                {
                    period.setUid( getLocalPeriodIdentifier( period, calendar ) );
                }
            }

            DimensionalObject object = new BaseDimensionalObject( dimension, DimensionType.PERIOD, null, DISPLAY_NAME_PERIOD, asList( periods ) );

            return object;
        }

        else if ( ORGUNIT_DIM_ID.equals( dimension ) )
        {
            List<DimensionalItemObject> ous = new ArrayList<>();
            List<Integer> levels = new ArrayList<>();
            List<OrganisationUnitGroup> groups = new ArrayList<>();

            for ( String ou : items )
            {
                if ( KEY_USER_ORGUNIT.equals( ou ) && userOrgUnits != null && !userOrgUnits.isEmpty() )
                {
                    ous.addAll( userOrgUnits );
                }
                else if ( KEY_USER_ORGUNIT_CHILDREN.equals( ou ) && userOrgUnits != null && !userOrgUnits.isEmpty() )
                {
                    ous.addAll( OrganisationUnit.getSortedChildren( userOrgUnits ) );
                }
                else if ( KEY_USER_ORGUNIT_GRANDCHILDREN.equals( ou ) && userOrgUnits != null && !userOrgUnits.isEmpty() )
                {
                    ous.addAll( OrganisationUnit.getSortedGrandChildren( userOrgUnits ) );
                }
                else if ( ou != null && ou.startsWith( KEY_LEVEL ) )
                {
                    int level = DimensionalObjectUtils.getLevelFromLevelParam( ou );

                    if ( level > 0 )
                    {
                        levels.add( level );
                    }
                }
                else if ( ou != null && ou.startsWith( KEY_ORGUNIT_GROUP ) )
                {
                    String uid = DimensionalObjectUtils.getUidFromGroupParam( ou );

                    OrganisationUnitGroup group = idObjectManager.getObject( OrganisationUnitGroup.class, inputIdScheme, uid );

                    if ( group != null )
                    {
                        groups.add( group );
                    }
                }
                else if ( !inputIdScheme.is( IdentifiableProperty.UID ) || CodeGenerator.isValidCode( ou ) )
                {
                    OrganisationUnit unit = idObjectManager.getObject( OrganisationUnit.class, inputIdScheme, ou );

                    if ( unit != null )
                    {
                        ous.add( unit );
                    }
                }
            }

            ous = ous.stream().distinct().collect( Collectors.toList() ); // Remove duplicates

            List<DimensionalItemObject> orgUnits = new ArrayList<>();
            List<OrganisationUnit> ousList = asTypedList( ous );

            if ( !levels.isEmpty() )
            {
                orgUnits.addAll( sort( organisationUnitService.getOrganisationUnitsAtLevels( levels, ousList ) ) );
            }

            if ( !groups.isEmpty() )
            {
                orgUnits.addAll( sort( organisationUnitService.getOrganisationUnits( groups, ousList ) ) );
            }

            // -----------------------------------------------------------------
            // When levels / groups are present, OUs are considered boundaries
            // -----------------------------------------------------------------

            if ( levels.isEmpty() && groups.isEmpty() )
            {
                orgUnits.addAll( ous );
            }

            if ( orgUnits.isEmpty() )
            {
                throw new IllegalQueryException( "Dimension ou is present in query without any valid dimension options" );
            }

            orgUnits = orgUnits.stream().distinct().collect( Collectors.toList() ); // Remove duplicates

            DimensionalObject object = new BaseDimensionalObject( dimension, DimensionType.ORGANISATION_UNIT, null, DISPLAY_NAME_ORGUNIT, orgUnits );

            return object;
        }

        else if ( LONGITUDE_DIM_ID.contains( dimension ) )
        {
            DimensionalObject object = new BaseDimensionalObject( dimension, DimensionType.STATIC, null, DISPLAY_NAME_LONGITUDE, new ArrayList<>() );

            return object;
        }

        else if ( LATITUDE_DIM_ID.contains( dimension ) )
        {
            DimensionalObject object = new BaseDimensionalObject( dimension, DimensionType.STATIC, null, DISPLAY_NAME_LATITUDE, new ArrayList<>() );

            return object;
        }

        else
        {
            DimensionalObject dimObject = idObjectManager.get( DataQueryParams.DYNAMIC_DIM_CLASSES, inputIdScheme, dimension );

            if ( dimObject != null && dimObject.isDataDimension() )
            {
                Class<?> dimClass = ReflectionUtils.getRealClass( dimObject.getClass() );

                Class<? extends DimensionalItemObject> itemClass = DimensionalObject.DIMENSION_CLASS_ITEM_CLASS_MAP.get( dimClass );

                List<DimensionalItemObject> dimItems = !allItems ? asList( idObjectManager.getByUidOrdered( itemClass, items ) ) : dimObject.getItems();

                DimensionalObject object = new BaseDimensionalObject( dimension, dimObject.getDimensionType(), null, dimObject.getName(), dimItems, allItems );

                return object;
            }
        }

        if ( allowNull )
        {
            return null;
        }

        throw new IllegalQueryException( "Dimension identifier does not reference any dimension: " + dimension );
    }

    @Override
    public List<OrganisationUnit> getUserOrgUnits( DataQueryParams params, String userOrgUnit )
    {
        List<OrganisationUnit> units = new ArrayList<>();

        User currentUser = securityManager.getCurrentUser( params );

        if ( userOrgUnit != null )
        {
            List<String> ous = DimensionalObjectUtils.getItemsFromParam( userOrgUnit );

            for ( String ou : ous )
            {
                OrganisationUnit unit = idObjectManager.get( OrganisationUnit.class, ou );

                if ( unit != null )
                {
                    units.add( unit );
                }
            }
        }
        else if ( currentUser != null && currentUser.hasOrganisationUnit() )
        {
            units = currentUser.getSortedOrganisationUnits();
        }

        return units;
    }
}
