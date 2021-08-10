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
package org.hisp.dhis.analytics.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ATTRIBUTEOPTIONCOMBO;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_CATEGORYOPTIONCOMBO;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_DATA_X;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_LATITUDE;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_LONGITUDE;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ORGUNIT;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ORGUNIT_GROUP;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_PERIOD;
import static org.hisp.dhis.analytics.DataQueryParams.KEY_DE_GROUP;
import static org.hisp.dhis.analytics.DataQueryParams.KEY_IN_GROUP;
import static org.hisp.dhis.analytics.DataQueryParams.getMeasureCriteriaFromParam;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.LATITUDE_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.LONGITUDE_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_GROUP_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.asList;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getLocalPeriodIdentifier;
import static org.hisp.dhis.commons.collection.ListUtils.sort;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_LEVEL;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_ORGUNIT_GROUP;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT_CHILDREN;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT_GRANDCHILDREN;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.OutputFormat;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataQueryRequest;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalKeywords;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.i18n.I18n;
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
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.period.comparator.AscendingPeriodComparator;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.analytics.DataQueryService" )
public class DefaultDataQueryService
    implements DataQueryService
{
    private IdentifiableObjectManager idObjectManager;

    private OrganisationUnitService organisationUnitService;

    private DimensionService dimensionService;

    private AnalyticsSecurityManager securityManager;

    private SystemSettingManager systemSettingManager;

    private AclService aclService;

    private CurrentUserService currentUserService;

    private I18nManager i18nManager;

    public DefaultDataQueryService( IdentifiableObjectManager idObjectManager,
        OrganisationUnitService organisationUnitService, DimensionService dimensionService,
        AnalyticsSecurityManager securityManager, SystemSettingManager systemSettingManager, AclService aclService,
        CurrentUserService currentUserService, I18nManager i18nManager )
    {
        checkNotNull( idObjectManager );
        checkNotNull( organisationUnitService );
        checkNotNull( dimensionService );
        checkNotNull( securityManager );
        checkNotNull( systemSettingManager );
        checkNotNull( aclService );
        checkNotNull( currentUserService );
        checkNotNull( i18nManager );

        this.idObjectManager = idObjectManager;
        this.organisationUnitService = organisationUnitService;
        this.dimensionService = dimensionService;
        this.securityManager = securityManager;
        this.systemSettingManager = systemSettingManager;
        this.aclService = aclService;
        this.currentUserService = currentUserService;
        this.i18nManager = i18nManager;
    }

    // -------------------------------------------------------------------------
    // DataQueryService implementation
    // -------------------------------------------------------------------------

    @Override
    public DataQueryParams getFromRequest( DataQueryRequest request )
    {
        I18nFormat format = i18nManager.getI18nFormat();

        DataQueryParams.Builder params = DataQueryParams.newBuilder();

        IdScheme inputIdScheme = ObjectUtils.firstNonNull( request.getInputIdScheme(), IdScheme.UID );

        if ( request.getDimension() != null && !request.getDimension().isEmpty() )
        {
            params.addDimensions( getDimensionalObjects( request.getDimension(), request.getRelativePeriodDate(),
                request.getUserOrgUnit(), format,
                request.isAllowAllPeriods(), inputIdScheme ) );
        }

        if ( request.getFilter() != null && !request.getFilter().isEmpty() )
        {
            params.addFilters( getDimensionalObjects( request.getFilter(), request.getRelativePeriodDate(),
                request.getUserOrgUnit(), format, request.isAllowAllPeriods(), inputIdScheme ) );
        }

        if ( request.getMeasureCriteria() != null && !request.getMeasureCriteria().isEmpty() )
        {
            params.withMeasureCriteria( getMeasureCriteriaFromParam( request.getMeasureCriteria() ) );
        }

        if ( request.getPreAggregationMeasureCriteria() != null
            && !request.getPreAggregationMeasureCriteria().isEmpty() )
        {
            params.withPreAggregationMeasureCriteria(
                getMeasureCriteriaFromParam( request.getPreAggregationMeasureCriteria() ) );
        }

        if ( request.getAggregationType() != null )
        {
            params.withAggregationType( AnalyticsAggregationType.fromAggregationType( request.getAggregationType() ) );
        }

        return params
            .withStartDate( request.getStartDate() )
            .withEndDate( request.getEndDate() )
            .withOrder( request.getOrder() )
            .withTimeField( request.getTimeField() )
            .withOrgUnitField( request.getOrgUnitField() )
            .withSkipMeta( request.isSkipMeta() )
            .withSkipData( request.isSkipData() )
            .withSkipRounding( request.isSkipRounding() )
            .withCompletedOnly( request.isCompletedOnly() )
            .withIgnoreLimit( request.isIgnoreLimit() )
            .withHierarchyMeta( request.isHierarchyMeta() )
            .withHideEmptyRows( request.isHideEmptyRows() )
            .withHideEmptyColumns( request.isHideEmptyColumns() )
            .withShowHierarchy( request.isShowHierarchy() )
            .withIncludeNumDen( request.isIncludeNumDen() )
            .withIncludeMetadataDetails( request.isIncludeMetadataDetails() )
            .withDisplayProperty( request.getDisplayProperty() )
            .withOutputIdScheme( request.getOutputIdScheme() )
            .withOutputDataElementIdScheme( request.getOutputDataElementIdScheme() )
            .withOutputOrgUnitIdScheme( request.getOutputOrgUnitIdScheme() )
            .withOutputFormat( OutputFormat.ANALYTICS )
            .withDuplicatesOnly( request.isDuplicatesOnly() )
            .withApprovalLevel( request.getApprovalLevel() )
            .withApiVersion( request.getApiVersion() )
            .withUserOrgUnitType( request.getUserOrgUnitType() )
            .build();
    }

    @Override
    public DataQueryParams getFromAnalyticalObject( AnalyticalObject object )
    {
        Assert.notNull( object, "Analytical object cannot be null" );

        DataQueryParams.Builder params = DataQueryParams.newBuilder();

        I18nFormat format = i18nManager.getI18nFormat();
        IdScheme idScheme = IdScheme.UID;
        Date date = object.getRelativePeriodDate();

        String userOrgUnit = object.getRelativeOrganisationUnit() != null
            ? object.getRelativeOrganisationUnit().getUid()
            : null;

        List<OrganisationUnit> userOrgUnits = getUserOrgUnits( null, userOrgUnit );

        object.populateAnalyticalProperties();

        for ( DimensionalObject column : object.getColumns() )
        {
            params.addDimension( getDimension( column.getDimension(), getDimensionalItemIds( column.getItems() ), date,
                userOrgUnits, format, false, false, idScheme ) );
        }

        for ( DimensionalObject row : object.getRows() )
        {
            params.addDimension( getDimension( row.getDimension(), getDimensionalItemIds( row.getItems() ), date,
                userOrgUnits, format, false, false, idScheme ) );
        }

        for ( DimensionalObject filter : object.getFilters() )
        {
            params.addFilter( getDimension( filter.getDimension(), getDimensionalItemIds( filter.getItems() ), date,
                userOrgUnits, format, false, false, idScheme ) );
        }

        return params
            .withCompletedOnly( object.isCompletedOnly() )
            .withTimeField( object.getTimeField() )
            .build();
    }

    @Override
    public List<DimensionalObject> getDimensionalObjects( Set<String> dimensionParams,
        Date relativePeriodDate, String userOrgUnit, I18nFormat format, boolean allowAllPeriods,
        IdScheme inputIdScheme )
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
                    list.add( getDimension( dimension, items, relativePeriodDate, userOrgUnits, format, false,
                        allowAllPeriods, inputIdScheme ) );
                }
            }
        }

        return list;
    }

    // TODO Optimize so that org unit levels + boundary are used in query
    // instead of
    // fetching all org units one by one

    @Override
    public DimensionalObject getDimension( String dimension, List<String> items, Date relativePeriodDate,
        List<OrganisationUnit> userOrgUnits, I18nFormat format, boolean allowNull, boolean allowAllPeriodItems,
        IdScheme inputIdScheme )
    {
        final boolean allItems = items.isEmpty();
        User user = currentUserService.getCurrentUser();

        if ( DATA_X_DIM_ID.equals( dimension ) )
        {
            List<DimensionalItemObject> dataDimensionItems = new ArrayList<>();

            DimensionalKeywords dimensionalKeywords = new DimensionalKeywords();

            for ( String uid : items )
            {
                if ( uid.startsWith( KEY_DE_GROUP ) ) // DATA ELEMENT GROUP
                {
                    String groupUid = DimensionalObjectUtils.getUidFromGroupParam( uid );

                    DataElementGroup group = idObjectManager.getObject( DataElementGroup.class, inputIdScheme,
                        groupUid );

                    if ( group != null )
                    {
                        dataDimensionItems.addAll( group.getMembers() );
                        dimensionalKeywords.addGroupBy( group );
                    }
                }
                else if ( uid.startsWith( KEY_IN_GROUP ) ) // INDICATOR GROUP
                {
                    String groupUid = DimensionalObjectUtils.getUidFromGroupParam( uid );

                    IndicatorGroup group = idObjectManager.getObject( IndicatorGroup.class, inputIdScheme, groupUid );

                    if ( group != null )
                    {
                        dataDimensionItems.addAll( group.getMembers() );
                        dimensionalKeywords.addGroupBy( group );
                    }
                }
                else
                {
                    DimensionalItemObject dimItemObject = dimensionService.getDataDimensionalItemObject( inputIdScheme,
                        uid );

                    if ( dimItemObject != null )
                    {
                        dataDimensionItems.add( dimItemObject );
                    }
                }
            }

            if ( dataDimensionItems.isEmpty() )
            {
                throwIllegalQueryEx( ErrorCode.E7124, DimensionalObject.DATA_X_DIM_ID );
            }

            return new BaseDimensionalObject( dimension, DimensionType.DATA_X, null, DISPLAY_NAME_DATA_X,
                dataDimensionItems, dimensionalKeywords );
        }

        else if ( CATEGORYOPTIONCOMBO_DIM_ID.equals( dimension ) )
        {
            return new BaseDimensionalObject( dimension, DimensionType.CATEGORY_OPTION_COMBO, null,
                DISPLAY_NAME_CATEGORYOPTIONCOMBO, getCategoryOptionComboList( items, inputIdScheme ) );
        }

        else if ( ATTRIBUTEOPTIONCOMBO_DIM_ID.equals( dimension ) )
        {
            return new BaseDimensionalObject( dimension, DimensionType.ATTRIBUTE_OPTION_COMBO, null,
                DISPLAY_NAME_ATTRIBUTEOPTIONCOMBO, getCategoryOptionComboList( items, inputIdScheme ) );
        }

        else if ( PERIOD_DIM_ID.equals( dimension ) )
        {
            Calendar calendar = PeriodType.getCalendar();
            I18n i18n = i18nManager.getI18n();
            List<Period> periods = new ArrayList<>();

            DimensionalKeywords dimensionalKeywords = new DimensionalKeywords();

            AnalyticsFinancialYearStartKey financialYearStart = (AnalyticsFinancialYearStartKey) systemSettingManager
                .getSystemSetting( SettingKey.ANALYTICS_FINANCIAL_YEAR_START );

            boolean containsRelativePeriods = false;

            for ( String isoPeriod : items )
            {
                if ( RelativePeriodEnum.contains( isoPeriod ) )
                {
                    containsRelativePeriods = true;
                    RelativePeriodEnum relativePeriod = RelativePeriodEnum.valueOf( isoPeriod );

                    dimensionalKeywords.addGroupBy( isoPeriod, i18n.getString( isoPeriod ) );

                    List<Period> relativePeriods = RelativePeriods.getRelativePeriodsFromEnum( relativePeriod,
                        relativePeriodDate, format, true, financialYearStart );
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

            // Remove duplicates
            periods = periods.stream().distinct().collect( Collectors.toList() );

            if ( containsRelativePeriods )
            {
                periods.sort( new AscendingPeriodComparator() );
            }

            for ( Period period : periods )
            {
                String name = format != null ? format.formatPeriod( period ) : null;

                if ( !period.getPeriodType().getName().contains( WeeklyPeriodType.NAME ) )
                {
                    period.setShortName( name );
                }

                period.setName( name );

                if ( !calendar.isIso8601() )
                {
                    period.setUid( getLocalPeriodIdentifier( period, calendar ) );
                }
            }

            return new BaseDimensionalObject( dimension, DimensionType.PERIOD, null, DISPLAY_NAME_PERIOD,
                asList( periods ), dimensionalKeywords );
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
                else if ( KEY_USER_ORGUNIT_GRANDCHILDREN.equals( ou ) && userOrgUnits != null
                    && !userOrgUnits.isEmpty() )
                {
                    ous.addAll( OrganisationUnit.getSortedGrandChildren( userOrgUnits ) );
                }
                else if ( ou != null && ou.startsWith( KEY_LEVEL ) )
                {
                    String level = DimensionalObjectUtils.getValueFromKeywordParam( ou );

                    Integer orgUnitLevel = organisationUnitService.getOrganisationUnitLevelByLevelOrUid( level );

                    if ( orgUnitLevel != null )
                    {
                        levels.add( orgUnitLevel );
                    }
                }
                else if ( ou != null && ou.startsWith( KEY_ORGUNIT_GROUP ) )
                {
                    String uid = DimensionalObjectUtils.getUidFromGroupParam( ou );

                    OrganisationUnitGroup group = idObjectManager.getObject( OrganisationUnitGroup.class, inputIdScheme,
                        uid );

                    if ( group != null )
                    {
                        groups.add( group );
                    }
                }
                else if ( !inputIdScheme.is( IdentifiableProperty.UID ) || CodeGenerator.isValidUid( ou ) )
                {
                    OrganisationUnit unit = idObjectManager.getObject( OrganisationUnit.class, inputIdScheme, ou );

                    if ( unit != null )
                    {
                        ous.add( unit );
                    }
                }
            }

            ous = ous.stream().distinct().collect( Collectors.toList() ); // Remove
                                                                          // duplicates

            List<DimensionalItemObject> orgUnits = new ArrayList<>();
            List<OrganisationUnit> ousList = asTypedList( ous );
            DimensionalKeywords dimensionalKeywords = new DimensionalKeywords();

            if ( !levels.isEmpty() )
            {
                orgUnits.addAll( sort( organisationUnitService.getOrganisationUnitsAtLevels( levels, ousList ) ) );
                dimensionalKeywords.addGroupBy(
                    levels.stream().map( l -> organisationUnitService.getOrganisationUnitLevelByLevel( l ) )
                        .filter( Objects::nonNull ).collect( Collectors.toList() ) );
            }

            if ( !groups.isEmpty() )
            {
                orgUnits.addAll( sort( organisationUnitService.getOrganisationUnits( groups, ousList ) ) );
                dimensionalKeywords.addGroupBy(
                    groups.stream().map( g -> new BaseNameableObject( g.getUid(), g.getCode(), g.getName() ) )
                        .collect( Collectors.toList() ) );
            }

            // -----------------------------------------------------------------
            // When levels / groups are present, OUs are considered boundaries
            // -----------------------------------------------------------------

            if ( levels.isEmpty() && groups.isEmpty() )
            {
                orgUnits.addAll( ous );
            }

            // Add boundary OUs as keywords

            if ( !dimensionalKeywords.isEmpty() )
            {
                dimensionalKeywords.addGroupBy( ousList );
            }

            if ( orgUnits.isEmpty() )
            {
                throwIllegalQueryEx( ErrorCode.E7124, DimensionalObject.ORGUNIT_DIM_ID );
            }

            orgUnits = orgUnits.stream().distinct().collect( Collectors.toList() ); // Remove
                                                                                    // duplicates

            return new BaseDimensionalObject( dimension, DimensionType.ORGANISATION_UNIT, null, DISPLAY_NAME_ORGUNIT,
                orgUnits, dimensionalKeywords );
        }

        else if ( ORGUNIT_GROUP_DIM_ID.equals( dimension ) )
        {
            List<DimensionalItemObject> ougs = new ArrayList<>();

            for ( String uid : items )
            {
                OrganisationUnitGroup organisationUnitGroup = idObjectManager.getObject( OrganisationUnitGroup.class,
                    inputIdScheme, uid );

                if ( organisationUnitGroup != null )
                {
                    ougs.add( organisationUnitGroup );
                }
            }

            return new BaseDimensionalObject( dimension, DimensionType.ORGANISATION_UNIT_GROUP, null,
                DISPLAY_NAME_ORGUNIT_GROUP, ougs );
        }

        else if ( LONGITUDE_DIM_ID.contains( dimension ) )
        {
            return new BaseDimensionalObject( dimension, DimensionType.STATIC, null, DISPLAY_NAME_LONGITUDE,
                new ArrayList<>() );
        }

        else if ( LATITUDE_DIM_ID.contains( dimension ) )
        {
            return new BaseDimensionalObject( dimension, DimensionType.STATIC, null, DISPLAY_NAME_LATITUDE,
                new ArrayList<>() );
        }

        else
        {
            DimensionalObject dimObject = idObjectManager.get( DataQueryParams.DYNAMIC_DIM_CLASSES, inputIdScheme,
                dimension );

            if ( dimObject != null && dimObject.isDataDimension() )
            {
                Class<?> dimClass = HibernateProxyUtils.getRealClass( dimObject );

                Class<? extends DimensionalItemObject> itemClass = DimensionalObject.DIMENSION_CLASS_ITEM_CLASS_MAP
                    .get( dimClass );

                List<DimensionalItemObject> dimItems = !allItems
                    ? asList( idObjectManager.getOrdered( itemClass, inputIdScheme, items ) )
                    : getCanReadItems( user, dimObject );

                return new BaseDimensionalObject( dimObject.getDimension(), dimObject.getDimensionType(), null,
                    dimObject.getName(),
                    dimItems, allItems );
            }
        }

        if ( allowNull )
        {
            return null;
        }

        throw new IllegalQueryException( new ErrorMessage( ErrorCode.E7125, dimension ) );
    }

    @Override
    public List<OrganisationUnit> getUserOrgUnits( DataQueryParams params, String userOrgUnit )
    {
        final List<OrganisationUnit> units = new ArrayList<>();

        User currentUser = securityManager.getCurrentUser( params );

        if ( userOrgUnit != null )
        {
            units.addAll( DimensionalObjectUtils.getItemsFromParam( userOrgUnit ).stream()
                .map( ou -> idObjectManager.get( OrganisationUnit.class, ou ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() ) );
        }
        else if ( currentUser != null && params != null && params.getUserOrgUnitType() != null )
        {
            switch ( params.getUserOrgUnitType() )
            {
            case DATA_CAPTURE:
                units.addAll( currentUser.getOrganisationUnits().stream().sorted().collect( Collectors.toList() ) );
                break;
            case DATA_OUTPUT:
                units.addAll(
                    currentUser.getDataViewOrganisationUnits().stream().sorted().collect( Collectors.toList() ) );
                break;
            case TEI_SEARCH:
                units.addAll(
                    currentUser.getTeiSearchOrganisationUnits().stream().sorted().collect( Collectors.toList() ) );
                break;
            }
        }
        else if ( currentUser != null )
        {
            units.addAll( currentUser.getOrganisationUnits().stream().sorted().collect( Collectors.toList() ) );
        }

        return units;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns a list of category option combinations based on the given item
     * identifiers.
     *
     * @param items the item identifiers.
     * @param inputIdScheme the {@link IdScheme}.
     * @return a list of {@link DimensionalItemObject}.
     */
    private List<DimensionalItemObject> getCategoryOptionComboList( List<String> items, IdScheme inputIdScheme )
    {
        return items.stream()
            .map( item -> idObjectManager.getObject( CategoryOptionCombo.class, inputIdScheme, item ) )
            .filter( Objects::nonNull )
            .collect( Collectors.toList() );
    }

    /**
     * Returns only objects for which the user has data or metadata read access.
     *
     * @param user the user.
     * @param object the {@link DimensionalObject}.
     * @return a list of {@link DimensionalItemObject}.
     */
    private List<DimensionalItemObject> getCanReadItems( User user, DimensionalObject object )
    {
        return object.getItems().stream()
            .filter( o -> aclService.canDataOrMetadataRead( user, o ) )
            .collect( Collectors.toList() );
    }
}
