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

import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_DATA_X;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ORGUNIT;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ORGUNIT_GROUP;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_PERIOD;
import static org.hisp.dhis.analytics.DataQueryParams.KEY_DE_GROUP;
import static org.hisp.dhis.analytics.DataQueryParams.KEY_IN_GROUP;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.DimensionalObjectUtils.asList;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
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
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.feedback.ErrorCode;
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
                String groupUid = DimensionalObjectUtils.getUidFromGroupParam( uid );

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
                String groupUid = DimensionalObjectUtils.getUidFromGroupParam( uid );

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
            throwIllegalQueryEx( ErrorCode.E7124, DimensionalObject.DATA_X_DIM_ID );
        }

        return new BaseDimensionalObject( dimension, DimensionType.DATA_X, null, DISPLAY_NAME_DATA_X,
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
            .getSystemSetting( SettingKey.ANALYTICS_FINANCIAL_YEAR_START, AnalyticsFinancialYearStartKey.class );

        boolean containsRelativePeriods = false;

        for ( String isoPeriod : items )
        {
            // Contains isoPeriod and timeField
            IsoPeriodHolder isoPeriodHolder = IsoPeriodHolder.of( isoPeriod );

            if ( RelativePeriodEnum.contains( isoPeriodHolder.getIsoPeriod() ) )
            {
                containsRelativePeriods = true;
                RelativePeriodEnum relativePeriod = RelativePeriodEnum.valueOf( isoPeriodHolder.getIsoPeriod() );

                dimensionalKeywords.addKeyword( isoPeriodHolder.getIsoPeriod(),
                    i18n.getString( isoPeriodHolder.getIsoPeriod() ) );

                List<Period> relativePeriods = RelativePeriods.getRelativePeriodsFromEnum( relativePeriod,
                    relativePeriodDate, format, true, financialYearStart );

                // If custom time filter is specified, set it in periods
                if ( isoPeriodHolder.hasDateField() )
                {
                    relativePeriods.forEach( period -> period.setDateField( isoPeriodHolder.getDateField() ) );
                }

                periods.addAll( relativePeriods );
            }
            else
            {
                Period period = PeriodType.getPeriodFromIsoString( isoPeriodHolder.getIsoPeriod() );

                if ( period != null )
                {
                    if ( isoPeriodHolder.hasDateField() )
                    {
                        period.setDescription( isoPeriodHolder.getIsoPeriod() );
                        period.setDateField( isoPeriodHolder.getDateField() );
                    }

                    dimensionalKeywords.addKeyword( isoPeriodHolder.getIsoPeriod(),
                        format != null ? i18n.getString( format.formatPeriod( period ) )
                            : isoPeriodHolder.getIsoPeriod() );

                    periods.add( period );
                }
                else
                {
                    Optional<Period> optionalPeriod = isoPeriodHolder.toDailyPeriod();
                    if ( optionalPeriod.isPresent() )
                    {
                        Period periodToAdd = optionalPeriod.get();
                        String startDate = i18nManager.getI18nFormat().formatDate( periodToAdd.getStartDate() );
                        String endDate = i18nManager.getI18nFormat().formatDate( periodToAdd.getEndDate() );
                        dimensionalKeywords.addKeyword(
                            isoPeriodHolder.getIsoPeriod(),
                            String.join( " - ", startDate, endDate ) );
                        periods.add( periodToAdd );
                    }
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

    public BaseDimensionalObject fromOrgUnit( String dimension, List<String> items, DisplayProperty displayProperty,
        List<OrganisationUnit> userOrgUnits, IdScheme inputIdScheme )
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

        // Remove duplicates
        ous = ous.stream().distinct().collect( Collectors.toList() );

        List<DimensionalItemObject> orgUnits = new ArrayList<>();
        List<OrganisationUnit> ousList = asTypedList( ous );
        DimensionItemKeywords dimensionalKeywords = new DimensionItemKeywords();

        if ( !levels.isEmpty() )
        {
            orgUnits.addAll( sort( organisationUnitService.getOrganisationUnitsAtLevels( levels, ousList ) ) );

            dimensionalKeywords.addKeywords( levels.stream()
                .map( organisationUnitService::getOrganisationUnitLevelByLevel )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() ) );
        }

        if ( !groups.isEmpty() )
        {
            orgUnits.addAll( sort( organisationUnitService.getOrganisationUnits( groups, ousList ) ) );

            dimensionalKeywords.addKeywords( groups.stream()
                .map( group -> new BaseNameableObject( group.getUid(), group.getCode(),
                    group.getDisplayProperty( displayProperty ) ) )
                .collect( Collectors.toList() ) );
        }

        // -----------------------------------------------------------------
        // When levels / groups are present, OUs are considered boundaries
        // -----------------------------------------------------------------

        if ( levels.isEmpty() && groups.isEmpty() )
        {
            orgUnits.addAll( ous );
        }

        if ( !dimensionalKeywords.isEmpty() )
        {
            dimensionalKeywords.addKeywords( ousList );
        }

        if ( orgUnits.isEmpty() )
        {
            throwIllegalQueryEx( ErrorCode.E7124, DimensionalObject.ORGUNIT_DIM_ID );
        }

        // Remove duplicates
        orgUnits = orgUnits.stream().distinct().collect( Collectors.toList() );

        return new BaseDimensionalObject( dimension, DimensionType.ORGANISATION_UNIT,
            null, DISPLAY_NAME_ORGUNIT, orgUnits, dimensionalKeywords );
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

        return new BaseDimensionalObject( dimension, DimensionType.ORGANISATION_UNIT_GROUP, null,
            DISPLAY_NAME_ORGUNIT_GROUP, ougs );
    }

    public Optional<BaseDimensionalObject> fromDynamic( String dimension, List<String> items,
        DisplayProperty displayProperty, IdScheme inputIdScheme )
    {
        boolean allItems = items.isEmpty();

        DimensionalObject dimObject = idObjectManager.get( DataQueryParams.DYNAMIC_DIM_CLASSES, inputIdScheme,
            dimension );

        if ( dimObject != null && dimObject.isDataDimension() )
        {
            Class<?> dimClass = HibernateProxyUtils.getRealClass( dimObject );

            Class<? extends DimensionalItemObject> itemClass = DimensionalObject.DIMENSION_CLASS_ITEM_CLASS_MAP
                .get( dimClass );

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
            .collect( Collectors.toList() );
    }
}
