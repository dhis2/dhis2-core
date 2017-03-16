package org.hisp.dhis.validation;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.validation.notification.ValidationNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hisp.dhis.common.DimensionItemType.*;

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

/**
 * @author Jim Grace
 * @author Stian Sandvold
 */
@Transactional
public class DefaultValidationService
    implements ValidationService
{
    private static final Log log = LogFactory.getLog( DefaultValidationService.class );

    private static final ImmutableSet<DimensionItemType> EVENT_DIM_ITEM_TYPES = ImmutableSet.of(
        PROGRAM_DATA_ELEMENT, PROGRAM_ATTRIBUTE, PROGRAM_INDICATOR );

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private ConstantService constantService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ValidationNotificationService notificationService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private ValidationRuleService validationRuleService;

    @Autowired
    private ApplicationContext applicationContext;

    private CurrentUserService currentUserService;



    // -------------------------------------------------------------------------
    // ValidationRule business logic
    // -------------------------------------------------------------------------

    @Override
    public Collection<ValidationResult> startInteractiveValidationAnalysis( Date startDate, Date endDate,
        Collection<OrganisationUnit> sources,
        DataElementCategoryOptionCombo attributeOptionCombo, ValidationRuleGroup group, boolean sendNotifications,
        I18nFormat format )
    {
        Collection<Period> periods = periodService.getPeriodsBetweenDates( startDate, endDate );

        Collection<ValidationRule> rules =
            group != null ? group.getMembers() : validationRuleService.getAllValidationRules();

        ValidationRunContext context = getValidationContext( sources, periods, rules )
            .withAttributeCombo( attributeOptionCombo )
            .withMaxResults( MAX_INTERACTIVE_ALERTS )
            .withSendNotifications( sendNotifications )
            .build();

        return startValidationAnalysis( context );
    }

    @Override
    public Collection<ValidationResult> startInteractiveValidationAnalysis( DataSet dataSet, Period period,
        OrganisationUnit source,
        DataElementCategoryOptionCombo attributeOptionCombo )
    {
        Collection<ValidationRule> rules = validationRuleService
            .getValidationRulesForDataElements( dataSet.getDataElements() );

        Collection<OrganisationUnit> sources = Sets.newHashSet( source );

        Collection<Period> periods = Sets.newHashSet( period );

        ValidationRunContext context = getValidationContext( sources, periods, rules )
            .withAttributeCombo( attributeOptionCombo )
            .withMaxResults( MAX_INTERACTIVE_ALERTS )
            .build();

        return startValidationAnalysis( context );
    }

    @Override
    public void startScheduledValidationAnalysis()
    {
        List<OrganisationUnit> sources = organisationUnitService.getAllOrganisationUnits();

        // Find all rules which might generate notifications
        Set<ValidationRule> rules = getValidationRulesWithNotificationTemplates();

        Set<Period> periods = extractNotificationPeriods( rules );

        log.info( "Scheduled validation analysis started, sources: " + sources.size() + ", periods: " + periods.size() +
            ", rules:" +
            rules.size() );

        // TODO: We are not actively using LAST_MONITORING_RUN anymore, remove when sure we don't need it.
        systemSettingManager.saveSystemSetting( SettingKey.LAST_MONITORING_RUN, new Date() );

        ValidationRunContext context = getValidationContext( sources, periods, rules )
            .withMaxResults( MAX_SCHEDULED_ALERTS )
            .withSendNotifications( true )
            .build();

        startValidationAnalysis( context );
    }

    private Collection<ValidationResult> startValidationAnalysis( ValidationRunContext context )
    {

        Collection<ValidationResult> results = Validator.validate( context, applicationContext );

        if ( context.isSendNotifications() )
        {
            notificationService.sendNotifications( Sets.newHashSet( results ) );
        }

        return results;
    }

    @Override
    public List<DataElementOperand> validateRequiredComments( DataSet dataSet, Period period,
        OrganisationUnit organisationUnit, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        List<DataElementOperand> violations = new ArrayList<>();

        if ( dataSet.isNoValueRequiresComment() )
        {
            for ( DataElement de : dataSet.getDataElements() )
            {
                for ( DataElementCategoryOptionCombo co : de.getCategoryOptionCombos() )
                {
                    DataValue dv = dataValueService
                        .getDataValue( de, period, organisationUnit, co, attributeOptionCombo );

                    boolean missingValue = dv == null || StringUtils.trimToNull( dv.getValue() ) == null;
                    boolean missingComment = dv == null || StringUtils.trimToNull( dv.getComment() ) == null;

                    if ( missingValue && missingComment )
                    {
                        violations.add( new DataElementOperand( de, co ) );
                    }
                }
            }
        }

        return violations;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Set<ValidationRule> getValidationRulesWithNotificationTemplates()
    {
        return Sets.newHashSet( validationRuleService.getValidationRulesWithNotificationTemplates() );
    }

    /**
     * Get the current and most recent periods to use when performing validation
     * for generating notifications (previously 'alerts').
     * <p>
     * The periods are filtered against existing (persisted) periods.
     * <p>
     * TODO Consider:
     * This method assumes that the last successful validation run was one day ago.
     * If this is not the case (more than one day ago) adding additional (daily)
     * periods to 'fill the gap' could be considered.
     */
    private Set<Period> extractNotificationPeriods( Set<ValidationRule> rules )
    {
        return rules.stream()
            .map( rule -> periodService.getPeriodTypeByName( rule.getPeriodType().getName() ) )
            .map( periodType -> {
                Period current = periodType.createPeriod(), previous = periodType.getPreviousPeriod( current );
                Date start = previous.getStartDate(), end = current.getEndDate();

                return periodService.getIntersectingPeriodsByPeriodType( periodType, start, end );
            } )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );
    }

    /**
     * Formats and sets name on the period of each result.
     *
     * @param results the collection of validation results.
     * @param format  the i18n format.
     */
    private void formatPeriods( Collection<ValidationResult> results, I18nFormat format )
    {
        if ( format != null )
        {
            for ( ValidationResult result : results )
            {
                if ( result != null && result.getPeriod() != null )
                {
                    result.getPeriod().setName( format.formatPeriod( result.getPeriod() ) );
                }
            }
        }
    }

    /**
     * Gets the event dimension item for the validationsrules
     * @param validationRules
     * @return
     */
    private Set<DimensionalItemObject> getDimensionItems( Collection<ValidationRule> validationRules )
    {
        return validationRules.stream()
            .map( validationRule -> validationRuleService
                .getDimensionalItemObjects( validationRule, EVENT_DIM_ITEM_TYPES ) )
            .reduce( Sets::union )
            .orElseGet( Sets::newHashSet );
    }

    /**
     * Returns a new Builder with basic configuration based on the input parameters.
     * @param sources org units to include in analysis
     * @param periods periods to include in analysis
     * @param validationRules rules to include in analysis
     * @return Builder with basic configuration based on input
     */
    private ValidationRunContext.Builder getValidationContext( Collection<OrganisationUnit> sources,
        Collection<Period> periods, Collection<ValidationRule> validationRules )
    {
        User currentUser = currentUserService.getCurrentUser();

        Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap = new HashMap<>();

        addPeriodsToContext( periodTypeExtendedMap, periods );
        addRulesToContext( periodTypeExtendedMap, validationRules );
        removeAnyUnneededPeriodTypes( periodTypeExtendedMap );
        Set<OrganisationUnitExtended> sourceXs = addSourcesToContext( periodTypeExtendedMap, sources, true );

        ValidationRunContext.Builder builder = ValidationRunContext.newBuilder()
            .withPeriodTypeExtendedMap( periodTypeExtendedMap )
            .withSourceXs( sourceXs )
            .withDimensionItems( getDimensionItems( validationRules ) )
            .withConstantMap( constantService.getConstantMap() );

        if ( currentUser != null )
        {
            builder
                .withCoDimensionConstraints(
                    categoryService.getCoDimensionConstraints( currentUser.getUserCredentials() ) )
                .withCogDimensionConstraints(
                    categoryService.getCogDimensionConstraints( currentUser.getUserCredentials() ) );
        }

        return builder;
    }

    /**
     * Adds Periods to the context, grouped by period type.
     *
     * @param periods Periods to group and add
     */
    private void addPeriodsToContext( Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap,
        Collection<Period> periods )
    {
        for ( Period period : periods )
        {
            PeriodTypeExtended periodTypeX = getOrCreatePeriodTypeExtended( periodTypeExtendedMap,
                period.getPeriodType() );
            periodTypeX.getPeriods().add( period );
        }
    }

    /**
     * Adds validation rules to the context.
     *
     * @param rules validation rules to add.
     */
    private void addRulesToContext( Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap,
        Collection<ValidationRule> rules )
    {
        Map<ValidationRule, Set<DataElement>> ruleDataElementsMap = rules.stream()
            .collect( Collectors.toMap( Function.identity(), vr -> validationRuleService.getDataElements( vr ) ) );

        for ( ValidationRule rule : rules )
        {
            Set<DataElement> dataElements = ruleDataElementsMap.get( rule );

            // Find the period type extended for this rule
            PeriodTypeExtended periodTypeX = getOrCreatePeriodTypeExtended( periodTypeExtendedMap,
                rule.getPeriodType() );
            periodTypeX.getRules().add( rule );

            // Add data elements of rule to the period extended
            periodTypeX.getDataElements().addAll( dataElements );

            // Add the allowed period types for data elements of rule
            periodTypeX.getAllowedPeriodTypes().addAll(
                getAllowedPeriodTypesForDataElements( dataElements, rule.getPeriodType() ) );
        }
    }

    /**
     * Removes any period types that don't have rules assigned to them.
     */
    private void removeAnyUnneededPeriodTypes( Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap )
    {
        Set<PeriodTypeExtended> periodTypeXs = new HashSet<>( periodTypeExtendedMap.values() );

        for ( PeriodTypeExtended periodTypeX : periodTypeXs )
        {
            if ( periodTypeX.getRules().isEmpty() )
            {
                periodTypeExtendedMap.remove( periodTypeX.getPeriodType() );
            }
        }
    }

    /**
     * Adds a collection of organisation units to the validation run context.
     *
     * @param sources             organisation units to add
     * @param ruleCheckThisSource true if these organisation units should be
     *                            evaluated with validation rules, false if not. (This is false when
     *                            adding descendants of organisation units for the purpose of getting
     *                            aggregated expression values from descendants, but these organisation
     *                            units are not in the main list to be evaluated.)
     */
    private Set<OrganisationUnitExtended> addSourcesToContext(
        Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap,
        Collection<OrganisationUnit> sources, boolean ruleCheckThisSource )
    {
        Set<OrganisationUnitExtended> sourceXs = new HashSet<>();

        for ( OrganisationUnit source : sources )
        {
            OrganisationUnitExtended sourceX = new OrganisationUnitExtended( source, ruleCheckThisSource );
            sourceXs.add( sourceX );

            Map<PeriodType, Set<DataElement>> sourceElementsMap = source.getDataElementsInDataSetsByPeriodType();

            for ( PeriodTypeExtended periodTypeX : periodTypeExtendedMap.values() )
            {
                periodTypeX.getSourceDataElements().put( source, new HashSet<>() );

                for ( PeriodType allowedType : periodTypeX.getAllowedPeriodTypes() )
                {
                    Set<DataElement> sourceDataElements = sourceElementsMap.get( allowedType );

                    if ( sourceDataElements != null )
                    {
                        periodTypeX.getSourceDataElements().get( source ).addAll( sourceDataElements );
                    }
                }
            }
        }

        return sourceXs;
    }

    /**
     * Gets the PeriodTypeExtended from the context object. If not found,
     * creates a new PeriodTypeExtended object, puts it into the context object,
     * and returns it.
     *
     * @param periodType period type to search for
     * @return period type extended from the context object
     */
    private PeriodTypeExtended getOrCreatePeriodTypeExtended( Map<PeriodType, PeriodTypeExtended> periodTypeExtendedMap,
        PeriodType periodType )
    {
        PeriodTypeExtended periodTypeX = periodTypeExtendedMap.get( periodType );

        if ( periodTypeX == null )
        {
            periodTypeX = new PeriodTypeExtended( periodType );
            periodTypeExtendedMap.put( periodType, periodTypeX );
        }

        return periodTypeX;
    }

    /**
     * Finds all period types that may contain given data elements, whose period
     * type interval is at least as long as the given period type.
     *
     * @param dataElements data elements to look for
     * @param periodType   the minimum-length period type
     * @return all period types that are allowed for these data elements
     */
    private static Set<PeriodType> getAllowedPeriodTypesForDataElements( Collection<DataElement> dataElements,
        PeriodType periodType )
    {
        Set<PeriodType> allowedPeriodTypes = new HashSet<>();

        if ( dataElements != null )
        {
            for ( DataElement dataElement : dataElements )
            {
                for ( DataSet dataSet : dataElement.getDataSets() )
                {
                    if ( dataSet.getPeriodType().getFrequencyOrder() >= periodType.getFrequencyOrder() )
                    {
                        allowedPeriodTypes.add( dataSet.getPeriodType() );
                    }
                }
            }
        }

        return allowedPeriodTypes;
    }

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }
}
