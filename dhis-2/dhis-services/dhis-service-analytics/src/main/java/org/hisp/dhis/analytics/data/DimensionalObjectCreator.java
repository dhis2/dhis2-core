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

import static java.lang.String.join;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_DATA_X;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ORGUNIT;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ORGUNIT_GROUP;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_PERIOD;
import static org.hisp.dhis.analytics.DataQueryParams.DYNAMIC_DIM_CLASSES;
import static org.hisp.dhis.analytics.DataQueryParams.KEY_DE_GROUP;
import static org.hisp.dhis.analytics.DataQueryParams.KEY_IN_GROUP;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT_GROUP;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_CLASS_ITEM_CLASS_MAP;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.asList;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.getUidFromGroupParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getValueFromKeywordParam;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getLocalPeriodIdentifier;
import static org.hisp.dhis.common.IdentifiableProperty.UID;
import static org.hisp.dhis.commons.collection.ListUtils.sort;
import static org.hisp.dhis.feedback.ErrorCode.E7124;
import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_LEVEL;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_ORGUNIT_GROUP;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT_CHILDREN;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT_GRANDCHILDREN;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getSortedChildren;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getSortedGrandChildren;
import static org.hisp.dhis.period.PeriodType.getPeriodFromIsoString;
import static org.hisp.dhis.period.RelativePeriods.getRelativePeriodsFromEnum;
import static org.hisp.dhis.period.WeeklyPeriodType.NAME;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_FINANCIAL_YEAR_START;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElementGroup;
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
import org.hisp.dhis.period.comparator.AscendingPeriodComparator;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DimensionalObjectCreator
{
    private final IdentifiableObjectManager idObjectManager;

    private final OrganisationUnitService organisationUnitService;

    private final SystemSettingManager systemSettingManager;

    private final I18nManager i18nManager;

    private final DimensionService dimensionService;

    private final AclService aclService;

    private final CurrentUserService currentUserService;

    public BaseDimensionalObject fromDx( String dimension, List<String> items, IdScheme inputIdScheme )
    {
        List<DimensionalItemObject> dataDimensionItems = new ArrayList<>();

        DimensionItemKeywords dimensionalKeywords = new DimensionItemKeywords();

        for ( String uid : items )
        {
            if ( uid.startsWith( KEY_DE_GROUP ) ) // DATA ELEMENT GROUP
            {
                String groupUid = getUidFromGroupParam( uid );

                DataElementGroup group = idObjectManager.getObject( DataElementGroup.class, inputIdScheme,
                    groupUid );

                if ( group != null )
                {
                    dataDimensionItems.addAll( group.getMembers() );
                    dimensionalKeywords.addKeyword( group );
                }
            }
            else if ( uid.startsWith( KEY_IN_GROUP ) ) // INDICATOR GROUP
            {
                String groupUid = getUidFromGroupParam( uid );

                IndicatorGroup group = idObjectManager.getObject( IndicatorGroup.class, inputIdScheme, groupUid );

                if ( group != null )
                {
                    dataDimensionItems.addAll( group.getMembers() );
                    dimensionalKeywords.addKeyword( group );
                }
            }
            else
            {
                DimensionalItemObject dimItemObject = dimensionService.getDataDimensionalItemObject(
                    inputIdScheme, uid );

                if ( dimItemObject != null )
                {
                    dataDimensionItems.add( dimItemObject );
                }
            }
        }

        if ( dataDimensionItems.isEmpty() )
        {
            throwIllegalQueryEx( E7124, DATA_X_DIM_ID );
        }

        return new BaseDimensionalObject( dimension, DATA_X, null, DISPLAY_NAME_DATA_X,
            dataDimensionItems, dimensionalKeywords );
    }

    public BaseDimensionalObject fromPeriod( String dimension, List<String> items, Date relativePeriodDate,
        I18nFormat format )
    {
        Calendar calendar = PeriodType.getCalendar();
        I18n i18n = i18nManager.getI18n();
        List<Period> periods = new ArrayList<>();

        DimensionItemKeywords dimensionalKeywords = new DimensionItemKeywords();

        AnalyticsFinancialYearStartKey financialYearStart = systemSettingManager
            .getSystemSetting( ANALYTICS_FINANCIAL_YEAR_START, AnalyticsFinancialYearStartKey.class );

        boolean containsRelativePeriods = false;

        for ( String isoPeriod : items )
        {
            // Contains isoPeriod and timeField
            IsoPeriodHolder isoPeriodHolder = IsoPeriodHolder.of( isoPeriod );

            if ( RelativePeriodEnum.contains( isoPeriodHolder.getIsoPeriod() ) )
            {
                containsRelativePeriods = true;

                addRelativePeriods( relativePeriodDate, format, i18n, periods, dimensionalKeywords, financialYearStart,
                    isoPeriodHolder );
            }
            else
            {
                Period period = getPeriodFromIsoString( isoPeriodHolder.getIsoPeriod() );

                if ( period != null )
                {
                    addDatePeriods( format, i18n, periods, dimensionalKeywords, isoPeriodHolder, period );
                }
                else
                {
                    addDailyPeriods( periods, dimensionalKeywords, isoPeriodHolder );
                }
            }
        }

        // Remove duplicates
        periods = periods.stream().distinct().collect( toList() );

        if ( containsRelativePeriods )
        {
            periods.sort( new AscendingPeriodComparator() );
        }

        overridePeriodsAttributes( format, calendar, periods );

        return new BaseDimensionalObject( dimension, PERIOD, null, DISPLAY_NAME_PERIOD,
            asList( periods ), dimensionalKeywords );
    }

    private void addDailyPeriods( List<Period> periods, DimensionItemKeywords dimensionalKeywords,
        IsoPeriodHolder isoPeriodHolder )
    {
        Optional<Period> optionalPeriod = isoPeriodHolder.toDailyPeriod();
        if ( optionalPeriod.isPresent() )
        {
            Period periodToAdd = optionalPeriod.get();
            String startDate = i18nManager.getI18nFormat().formatDate( periodToAdd.getStartDate() );
            String endDate = i18nManager.getI18nFormat().formatDate( periodToAdd.getEndDate() );
            dimensionalKeywords.addKeyword( isoPeriodHolder.getIsoPeriod(),
                join( " - ", startDate, endDate ) );
            periods.add( periodToAdd );
        }
    }

    private void addDatePeriods( I18nFormat format, I18n i18n, List<Period> periods,
        DimensionItemKeywords dimensionalKeywords, IsoPeriodHolder isoPeriodHolder, Period period )
    {
        if ( isoPeriodHolder.hasDateField() )
        {
            period.setDescription( isoPeriodHolder.getIsoPeriod() );
            period.setDateField( isoPeriodHolder.getDateField() );
        }

        dimensionalKeywords.addKeyword( isoPeriodHolder.getIsoPeriod(),
            format != null
                ? i18n.getString( format.formatPeriod( period ) )
                : isoPeriodHolder.getIsoPeriod() );

        periods.add( period );
    }

    private void addRelativePeriods( Date relativePeriodDate, I18nFormat format, I18n i18n, List<Period> periods,
        DimensionItemKeywords dimensionalKeywords, AnalyticsFinancialYearStartKey financialYearStart,
        IsoPeriodHolder isoPeriodHolder )
    {
        RelativePeriodEnum relativePeriod = RelativePeriodEnum.valueOf( isoPeriodHolder.getIsoPeriod() );

        dimensionalKeywords.addKeyword( isoPeriodHolder.getIsoPeriod(),
            i18n.getString( isoPeriodHolder.getIsoPeriod() ) );

        List<Period> relativePeriods = getRelativePeriodsFromEnum( relativePeriod, relativePeriodDate, format,
            true, financialYearStart );

        // If custom time filter is specified, set it in periods
        if ( isoPeriodHolder.hasDateField() )
        {
            relativePeriods.forEach( period -> period.setDateField( isoPeriodHolder.getDateField() ) );
        }

        periods.addAll( relativePeriods );
    }

    private void overridePeriodsAttributes( I18nFormat format, Calendar calendar, List<Period> periods )
    {
        for ( Period period : periods )
        {
            String name = format != null ? format.formatPeriod( period ) : null;

            if ( !period.getPeriodType().getName().contains( NAME ) )
            {
                period.setShortName( name );
            }

            period.setName( name );

            if ( !calendar.isIso8601() )
            {
                period.setUid( getLocalPeriodIdentifier( period, calendar ) );
            }
        }
    }

    public BaseDimensionalObject fromOrgUnit( String dimension, List<String> items, DisplayProperty displayProperty,
        List<OrganisationUnit> userOrgUnits, IdScheme inputIdScheme )
    {
        List<Integer> levels = new ArrayList<>();
        List<OrganisationUnitGroup> groups = new ArrayList<>();
        List<DimensionalItemObject> ous = getOrgUnitsDimension( items, userOrgUnits, inputIdScheme, levels, groups );

        List<DimensionalItemObject> orgUnitAtLevels = new ArrayList<>();
        List<OrganisationUnit> ousList = asTypedList( ous );

        DimensionItemKeywords dimensionalKeywords = new DimensionItemKeywords();

        if ( !levels.isEmpty() )
        {
            orgUnitAtLevels.addAll( sort( organisationUnitService.getOrganisationUnitsAtLevels( levels, ousList ) ) );

            dimensionalKeywords.addKeywords( levels.stream()
                .map( organisationUnitService::getOrganisationUnitLevelByLevel )
                .filter( Objects::nonNull )
                .collect( toList() ) );
        }

        if ( !groups.isEmpty() )
        {
            orgUnitAtLevels.addAll( sort( organisationUnitService.getOrganisationUnits( groups, ousList ) ) );

            dimensionalKeywords.addKeywords( groups.stream()
                .map( group -> new BaseNameableObject( group.getUid(), group.getCode(),
                    group.getDisplayProperty( displayProperty ) ) )
                .collect( toList() ) );
        }

        // -----------------------------------------------------------------
        // When levels / groups are present, OUs are considered boundaries
        // -----------------------------------------------------------------

        if ( levels.isEmpty() && groups.isEmpty() )
        {
            orgUnitAtLevels.addAll( ous );
        }

        if ( !dimensionalKeywords.isEmpty() )
        {
            dimensionalKeywords.addKeywords( ousList );
        }

        if ( orgUnitAtLevels.isEmpty() )
        {
            throwIllegalQueryEx( E7124, ORGUNIT_DIM_ID );
        }

        // Remove duplicates
        orgUnitAtLevels = orgUnitAtLevels.stream().distinct().collect( toList() );

        return new BaseDimensionalObject( dimension, ORGANISATION_UNIT, null, DISPLAY_NAME_ORGUNIT, orgUnitAtLevels,
            dimensionalKeywords );
    }

    private List<DimensionalItemObject> getOrgUnitsDimension( List<String> items, List<OrganisationUnit> userOrgUnits,
        IdScheme inputIdScheme, List<Integer> levels, List<OrganisationUnitGroup> groups )
    {
        List<DimensionalItemObject> ous = new ArrayList<>();

        for ( String ou : items )
        {
            if ( KEY_USER_ORGUNIT.equals( ou ) && isNotEmpty( userOrgUnits ) )
            {
                ous.addAll( userOrgUnits );
            }
            else if ( KEY_USER_ORGUNIT_CHILDREN.equals( ou ) && isNotEmpty( userOrgUnits ) )
            {
                ous.addAll( getSortedChildren( userOrgUnits ) );
            }
            else if ( KEY_USER_ORGUNIT_GRANDCHILDREN.equals( ou ) && isNotEmpty( userOrgUnits ) )
            {
                ous.addAll( getSortedGrandChildren( userOrgUnits ) );
            }
            else if ( ou != null && ou.startsWith( KEY_LEVEL ) )
            {
                String level = getValueFromKeywordParam( ou );
                Integer orgUnitLevel = organisationUnitService.getOrganisationUnitLevelByLevelOrUid( level );
                addIgnoreNull( levels, orgUnitLevel );
            }
            else if ( ou != null && ou.startsWith( KEY_ORGUNIT_GROUP ) )
            {
                String uid = getUidFromGroupParam( ou );
                OrganisationUnitGroup group = idObjectManager.getObject( OrganisationUnitGroup.class, inputIdScheme,
                    uid );
                addIgnoreNull( groups, group );
            }
            else if ( !inputIdScheme.is( UID ) || isValidUid( ou ) )
            {
                OrganisationUnit unit = idObjectManager.getObject( OrganisationUnit.class, inputIdScheme, ou );
                addIgnoreNull( ous, unit );
            }
        }

        // Remove duplicates
        return ous.stream().distinct().collect( toList() );
    }

    public BaseDimensionalObject fromOrgUnitGroup( String dimension, List<String> items, IdScheme inputIdScheme )
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

        return new BaseDimensionalObject( dimension, ORGANISATION_UNIT_GROUP, null,
            DISPLAY_NAME_ORGUNIT_GROUP, ougs );
    }

    public Optional<BaseDimensionalObject> fromDynamic( String dimension, List<String> items,
        DisplayProperty displayProperty, IdScheme inputIdScheme )
    {
        boolean allItems = items.isEmpty();

        DimensionalObject dimObject = idObjectManager.get( DYNAMIC_DIM_CLASSES, inputIdScheme, dimension );

        if ( dimObject != null && dimObject.isDataDimension() )
        {
            Class<?> dimClass = getRealClass( dimObject );

            Class<? extends DimensionalItemObject> itemClass = DIMENSION_CLASS_ITEM_CLASS_MAP.get( dimClass );

            List<DimensionalItemObject> dimItems = !allItems
                ? asList( idObjectManager.getOrdered( itemClass, inputIdScheme, items ) )
                : getCanReadItems( currentUserService.getCurrentUser(), dimObject );

            return Optional.of( new BaseDimensionalObject( dimObject.getDimension(), dimObject.getDimensionType(), null,
                dimObject.getDisplayProperty( displayProperty ), dimItems, allItems ) );
        }

        return Optional.empty();
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
            .collect( toList() );
    }
}
