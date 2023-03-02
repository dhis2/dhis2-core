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
package org.hisp.dhis.analytics.data;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.Validate.notNull;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.fromAggregationType;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ATTRIBUTEOPTIONCOMBO;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_CATEGORYOPTIONCOMBO;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_LATITUDE;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_LONGITUDE;
import static org.hisp.dhis.analytics.DataQueryParams.getMeasureCriteriaFromParam;
import static org.hisp.dhis.analytics.OutputFormat.ANALYTICS;
import static org.hisp.dhis.common.DimensionType.ATTRIBUTE_OPTION_COMBO;
import static org.hisp.dhis.common.DimensionType.CATEGORY_OPTION_COMBO;
import static org.hisp.dhis.common.DimensionType.STATIC;
import static org.hisp.dhis.common.DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.LATITUDE_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.LONGITUDE_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_GROUP_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemsFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;
import static org.hisp.dhis.common.DimensionalObjectUtils.getItemsFromParam;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.hisp.dhis.feedback.ErrorCode.E7125;
import static org.hisp.dhis.util.ObjectUtils.firstNonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.OrgUnitField;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DataQueryRequest;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.analytics.DataQueryService" )
@RequiredArgsConstructor
public class DefaultDataQueryService
    implements DataQueryService
{
    private final DimensionalObjectProducer dimensionalObjectProducer;

    private final IdentifiableObjectManager idObjectManager;

    private final AnalyticsSecurityManager securityManager;

    private final UserSettingService userSettingService;

    // -------------------------------------------------------------------------
    // DataQueryService implementation
    // -------------------------------------------------------------------------

    @Override
    public DataQueryParams getFromRequest( DataQueryRequest request )
    {
        DataQueryParams.Builder params = DataQueryParams.newBuilder();

        IdScheme inputIdScheme = firstNonNull( request.getInputIdScheme(), UID );

        Locale locale = (Locale) userSettingService.getUserSetting( UserSettingKey.DB_LOCALE );

        if ( isNotEmpty( request.getDimension() ) )
        {
            params.addDimensions( getDimensionalObjects( request ) );
        }

        if ( isNotEmpty( request.getFilter() ) )
        {
            params.addFilters( getDimensionalObjects( request.getFilter(), request.getRelativePeriodDate(),
                request.getUserOrgUnit(), request.getDisplayProperty(), inputIdScheme ) );
        }

        if ( isNotEmpty( request.getMeasureCriteria() ) )
        {
            params.withMeasureCriteria( getMeasureCriteriaFromParam( request.getMeasureCriteria() ) );
        }

        if ( isNotEmpty( request.getPreAggregationMeasureCriteria() ) )
        {
            params.withPreAggregationMeasureCriteria(
                getMeasureCriteriaFromParam( request.getPreAggregationMeasureCriteria() ) );
        }

        if ( request.hasAggregationType() )
        {
            params.withAggregationType( fromAggregationType( request.getAggregationType() ) );
        }

        return params
            .withStartDate( request.getStartDate() )
            .withEndDate( request.getEndDate() )
            .withOrder( request.getOrder() )
            .withTimeField( request.getTimeField() )
            .withOrgUnitField( new OrgUnitField( request.getOrgUnitField() ) )
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
            .withDuplicatesOnly( request.isDuplicatesOnly() )
            .withApprovalLevel( request.getApprovalLevel() )
            .withUserOrgUnitType( request.getUserOrgUnitType() )
            .withApiVersion( request.getApiVersion() )
            .withLocale( locale )
            .withOutputFormat( ANALYTICS )
            .build();
    }

    @Override
    public DataQueryParams getFromAnalyticalObject( AnalyticalObject object )
    {
        notNull( object );

        DataQueryParams.Builder params = DataQueryParams.newBuilder();

        IdScheme idScheme = UID;

        Date date = object.getRelativePeriodDate();

        Locale locale = (Locale) userSettingService.getUserSetting( UserSettingKey.DB_LOCALE );

        String userOrgUnit = object.getRelativeOrganisationUnit() != null
            ? object.getRelativeOrganisationUnit().getUid()
            : null;

        List<OrganisationUnit> userOrgUnits = getUserOrgUnits( null, userOrgUnit );

        object.populateAnalyticalProperties();

        for ( DimensionalObject column : object.getColumns() )
        {
            params.addDimension( getDimension( column.getDimension(), getDimensionalItemIds( column.getItems() ), date,
                userOrgUnits, false, null, idScheme ) );
        }

        for ( DimensionalObject row : object.getRows() )
        {
            params.addDimension( getDimension( row.getDimension(), getDimensionalItemIds( row.getItems() ), date,
                userOrgUnits, false, null, idScheme ) );
        }

        for ( DimensionalObject filter : object.getFilters() )
        {
            params.addFilter( getDimension( filter.getDimension(), getDimensionalItemIds( filter.getItems() ), date,
                userOrgUnits, false, null, idScheme ) );
        }

        return params
            .withCompletedOnly( object.isCompletedOnly() )
            .withTimeField( object.getTimeField() )
            .withLocale( locale )
            .build();
    }

    // TODO Optimize so that org unit levels + boundary are used in query
    // instead of fetching all org units one by one.

    @Override
    public DimensionalObject getDimension( String dimension, List<String> items, EventDataQueryRequest request,
        List<OrganisationUnit> userOrgUnits, boolean allowNull, IdScheme inputIdScheme )
    {
        return getDimension( dimension, items, request.getRelativePeriodDate(), request.getDisplayProperty(),
            userOrgUnits, allowNull, inputIdScheme );
    }

    @Override
    public DimensionalObject getDimension( String dimension, List<String> items, Date relativePeriodDate,
        List<OrganisationUnit> userOrgUnits, boolean allowNull, DisplayProperty displayProperty,
        IdScheme inputIdScheme )
    {
        return getDimension( dimension, items, relativePeriodDate, displayProperty,
            userOrgUnits, allowNull, inputIdScheme );
    }

    @Override
    public List<OrganisationUnit> getUserOrgUnits( DataQueryParams params, String userOrgUnit )
    {
        List<OrganisationUnit> units = new ArrayList<>();

        User currentUser = securityManager.getCurrentUser( params );

        if ( userOrgUnit != null )
        {
            units.addAll( getItemsFromParam( userOrgUnit ).stream()
                .map( ou -> idObjectManager.get( OrganisationUnit.class, ou ) )
                .filter( Objects::nonNull )
                .collect( toList() ) );
        }
        else if ( currentUser != null && params != null && params.getUserOrgUnitType() != null )
        {
            switch ( params.getUserOrgUnitType() )
            {
            case DATA_CAPTURE:
                units.addAll( currentUser.getOrganisationUnits().stream().sorted().collect( toList() ) );
                break;
            case DATA_OUTPUT:
                units.addAll(
                    currentUser.getDataViewOrganisationUnits().stream().sorted().collect( toList() ) );
                break;
            case TEI_SEARCH:
                units.addAll( currentUser.getTeiSearchOrganisationUnits().stream().sorted().collect( toList() ) );
                break;
            }
        }
        else if ( currentUser != null )
        {
            units.addAll( currentUser.getOrganisationUnits().stream().sorted().collect( toList() ) );
        }

        return units;
    }

    private List<DimensionalObject> getDimensionalObjects( DataQueryRequest request )
    {
        List<DimensionalObject> list = new ArrayList<>();
        List<OrganisationUnit> userOrgUnits = getUserOrgUnits( null, request.getUserOrgUnit() );

        if ( request.getDimension() != null )
        {
            for ( String param : request.getDimension() )
            {
                String dimension = getDimensionFromParam( param );
                List<String> items = getDimensionItemsFromParam( param );

                if ( dimension != null && items != null )
                {
                    addIgnoreNull( list, getDimension(
                        dimension, items, request.getRelativePeriodDate(), request.getDisplayProperty(),
                        userOrgUnits, false, firstNonNull( request.getInputIdScheme(), UID ) ) );
                }
            }
        }

        return list;
    }

    /**
     * Returns a {@link DimensionalObject}.
     *
     * @param dimension the dimension identifier.
     * @param items the dimension items.
     * @param relativePeriodDate the relative period date.
     * @param displayProperty the relative period date.
     * @param userOrgUnits the list of user {@link OrganisationUnit}.
     * @param allowNull whether to allow returning null.
     * @param inputIdScheme the input {@link IdScheme}.
     * @return a {@link DimensionalObject}.
     */
    private DimensionalObject getDimension( String dimension, List<String> items, Date relativePeriodDate,
        DisplayProperty displayProperty, List<OrganisationUnit> userOrgUnits, boolean allowNull,
        IdScheme inputIdScheme )
    {
        if ( DATA_X_DIM_ID.equals( dimension ) )
        {
            return dimensionalObjectProducer.getDimension( items, inputIdScheme );
        }
        else if ( CATEGORYOPTIONCOMBO_DIM_ID.equals( dimension ) )
        {
            return new BaseDimensionalObject( dimension, CATEGORY_OPTION_COMBO, null,
                DISPLAY_NAME_CATEGORYOPTIONCOMBO, getCategoryOptionComboList( items, inputIdScheme ) );
        }
        else if ( ATTRIBUTEOPTIONCOMBO_DIM_ID.equals( dimension ) )
        {
            return new BaseDimensionalObject( dimension, ATTRIBUTE_OPTION_COMBO, null,
                DISPLAY_NAME_ATTRIBUTEOPTIONCOMBO, getCategoryOptionComboList( items, inputIdScheme ) );
        }
        else if ( PERIOD_DIM_ID.equals( dimension ) )
        {
            return dimensionalObjectProducer.getPeriodDimension( items, relativePeriodDate );
        }
        else if ( ORGUNIT_DIM_ID.equals( dimension ) )
        {
            return dimensionalObjectProducer.getOrgUnitDimension( items, displayProperty, userOrgUnits,
                inputIdScheme );
        }
        else if ( ORGUNIT_GROUP_DIM_ID.equals( dimension ) )
        {
            return dimensionalObjectProducer.getOrgUnitGroupDimension( items, inputIdScheme );
        }
        else if ( LONGITUDE_DIM_ID.contains( dimension ) )
        {
            return new BaseDimensionalObject( dimension, STATIC, null, DISPLAY_NAME_LONGITUDE,
                new ArrayList<>() );
        }
        else if ( LATITUDE_DIM_ID.contains( dimension ) )
        {
            return new BaseDimensionalObject( dimension, STATIC, null, DISPLAY_NAME_LATITUDE,
                new ArrayList<>() );
        }
        else
        {
            Optional<BaseDimensionalObject> baseDimensionalObject = dimensionalObjectProducer.getDynamicDimension(
                dimension, items, displayProperty, inputIdScheme );

            if ( baseDimensionalObject.isPresent() )
            {
                return baseDimensionalObject.get();
            }
        }

        if ( allowNull )
        {
            return null;
        }

        throw new IllegalQueryException( new ErrorMessage( E7125, dimension ) );
    }

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
            .collect( toList() );
    }
}
