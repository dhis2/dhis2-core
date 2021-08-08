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
package org.hisp.dhis.validation;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.expression.ParseType.VALIDATION_RULE_EXPRESSION;

import java.util.*;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.*;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataanalysis.ValidationRuleExpressionDetails;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.validation.notification.ValidationNotificationService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Jim Grace
 * @author Stian Sandvold
 */
@Service( "org.hisp.dhis.validation.ValidationService" )
@Transactional
@Slf4j
public class DefaultValidationService
    implements ValidationService
{
    private final PeriodService periodService;

    private final OrganisationUnitService organisationUnitService;

    private final ExpressionService expressionService;

    private final DimensionService dimensionService;

    private final DataValueService dataValueService;

    private final CategoryService categoryService;

    private final ConstantService constantService;

    private final ValidationNotificationService notificationService;

    private final ValidationRuleService validationRuleService;

    private final ApplicationContext applicationContext;

    private final ValidationResultService validationResultService;

    private AnalyticsService analyticsService;

    private CurrentUserService currentUserService;

    public DefaultValidationService( PeriodService periodService, OrganisationUnitService organisationUnitService,
        ExpressionService expressionService, DimensionService dimensionService, DataValueService dataValueService,
        CategoryService categoryService, ConstantService constantService,
        ValidationNotificationService notificationService, ValidationRuleService validationRuleService,
        ApplicationContext applicationContext, ValidationResultService validationResultService,
        AnalyticsService analyticsService, CurrentUserService currentUserService )
    {
        checkNotNull( periodService );
        checkNotNull( organisationUnitService );
        checkNotNull( expressionService );
        checkNotNull( dimensionService );
        checkNotNull( dataValueService );
        checkNotNull( categoryService );
        checkNotNull( constantService );
        checkNotNull( notificationService );
        checkNotNull( validationRuleService );
        checkNotNull( applicationContext );
        checkNotNull( validationResultService );
        checkNotNull( analyticsService );
        checkNotNull( currentUserService );

        this.periodService = periodService;
        this.organisationUnitService = organisationUnitService;
        this.expressionService = expressionService;
        this.dimensionService = dimensionService;
        this.dataValueService = dataValueService;
        this.categoryService = categoryService;
        this.constantService = constantService;
        this.notificationService = notificationService;
        this.validationRuleService = validationRuleService;
        this.applicationContext = applicationContext;
        this.validationResultService = validationResultService;
        this.analyticsService = analyticsService;
        this.currentUserService = currentUserService;
    }

    /**
     * Used only for testing, remove when test is refactored
     */
    @Deprecated
    public void setAnalyticsService( AnalyticsService analyticsService )
    {
        this.analyticsService = analyticsService;
    }

    /**
     * Used only for testing, remove when test is refactored
     */
    @Deprecated
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    // -------------------------------------------------------------------------
    // ValidationRule business logic
    // -------------------------------------------------------------------------

    @Override
    public List<ValidationResult> validationAnalysis( ValidationAnalysisParams parameters )
    {
        Clock clock = new Clock( log ).startClock().logTime( "Starting validation analysis"
            + (parameters.getOrgUnit() == null ? ""
                : " for orgUnit " + parameters.getOrgUnit().getUid()
                    + (parameters.isIncludeOrgUnitDescendants() ? " with descendants" : ""))
            + ", "
            + (parameters.getPeriods().size() == 1
                ? "period " + Iterables.getOnlyElement( parameters.getPeriods() ).getIsoDate()
                : parameters.getPeriods().size() + " periods")
            + ", "
            + parameters.getValidationRules().size() + " rules"
            + (parameters.isPersistResults() ? ", persisting results" : "")
            + (parameters.isSendNotifications() ? ", sending notifications" : "") );

        ValidationRunContext context = getValidationContext( parameters );

        clock.logTime( "Initialized validation analysis" );

        List<ValidationResult> results = Validator.validate( context, applicationContext, analyticsService );

        if ( context.isPersistResults() )
        {
            validationResultService.saveValidationResults( context.getValidationResults() );
        }

        clock.logTime( "Finished validation analysis, " + context.getValidationResults().size() + " results" ).stop();

        if ( context.isSendNotifications() )
        {
            notificationService.sendNotifications( Sets.newHashSet( results ) );
        }

        return results;
    }

    @Override
    public ValidationRuleExpressionDetails getValidationRuleExpressionDetails( ValidationAnalysisParams parameters )
    {
        ValidationRunContext context = getValidationContext( parameters );

        ValidationRuleExpressionDetails details = new ValidationRuleExpressionDetails();

        context.setValidationRuleExpressionDetails( details );

        Validator.validate( context, applicationContext, analyticsService );

        details.sortByName();

        return details;
    }

    @Override
    public List<DataElementOperand> validateRequiredComments( DataSet dataSet, Period period,
        OrganisationUnit organisationUnit, CategoryOptionCombo attributeOptionCombo )
    {
        List<DataElementOperand> violations = new ArrayList<>();

        if ( dataSet.isNoValueRequiresComment() )
        {
            for ( DataElement de : dataSet.getDataElements() )
            {
                for ( CategoryOptionCombo co : de.getCategoryOptionCombos() )
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
    // Methods for creating ValidationAnalysisParams Builder object
    // -------------------------------------------------------------------------

    @Override
    public ValidationAnalysisParams.Builder newParamsBuilder( Collection<ValidationRule> validationRules,
        OrganisationUnit organisationUnit, Collection<Period> periods )
    {
        return new ValidationAnalysisParams.Builder( validationRules, organisationUnit, periods );
    }

    @Override
    public ValidationAnalysisParams.Builder newParamsBuilder( ValidationRuleGroup validationRuleGroup,
        OrganisationUnit organisationUnit, Date startDate, Date endDate )
    {
        Collection<ValidationRule> validationRules = validationRuleGroup != null ? validationRuleGroup.getMembers()
            : validationRuleService.getAllValidationRules();
        Collection<Period> periods = periodService.getPeriodsBetweenDates( startDate, endDate );

        return new ValidationAnalysisParams.Builder( validationRules, organisationUnit, periods );
    }

    @Override
    @Transactional( readOnly = true )
    public ValidationAnalysisParams.Builder newParamsBuilder( DataSet dataSet, OrganisationUnit organisationUnit,
        Period period )
    {
        Collection<ValidationRule> validationRules = validationRuleService.getValidationRulesForDataSet( dataSet );
        Collection<Period> periods = Sets.newHashSet( period );

        return new ValidationAnalysisParams.Builder( validationRules, organisationUnit, periods );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns a new Builder with basic configuration based on the input
     * parameters.
     *
     * @param parameters ValidationRuleParameters for creating
     *        ValidationRuleContext
     * @return Builder with basic configuration based on input.
     */
    private ValidationRunContext getValidationContext( ValidationAnalysisParams parameters )
    {
        User currentUser = currentUserService.getCurrentUser();

        OrganisationUnit parameterOrgUnit = parameters.getOrgUnit();
        List<OrganisationUnit> orgUnits;
        if ( parameterOrgUnit == null )
        {
            orgUnits = organisationUnitService.getAllOrganisationUnits();
        }
        else if ( parameters.isIncludeOrgUnitDescendants() )
        {
            orgUnits = organisationUnitService.getOrganisationUnitWithChildren( parameterOrgUnit.getUid() );
        }
        else
        {
            orgUnits = Lists.newArrayList( parameterOrgUnit );
        }

        Map<PeriodType, PeriodTypeExtended> periodTypeXMap = new HashMap<>();

        addPeriodsToContext( periodTypeXMap, parameters.getPeriods() );

        Map<DimensionalItemId, DimensionalItemObject> dimensionItemMap = addRulesToContext( periodTypeXMap,
            parameters.getValidationRules() );

        removeAnyUnneededPeriodTypes( periodTypeXMap );

        ValidationRunContext.Builder builder = ValidationRunContext.newBuilder()
            .withOrgUnits( orgUnits )
            .withPeriodTypeXs( new ArrayList<>( periodTypeXMap.values() ) )
            .withConstantMap( constantService.getConstantMap() )
            .withInitialResults( validationResultService
                .getValidationResults( parameterOrgUnit,
                    parameters.isIncludeOrgUnitDescendants(), parameters.getValidationRules(),
                    parameters.getPeriods() ) )
            .withSendNotifications( parameters.isSendNotifications() )
            .withPersistResults( parameters.isPersistResults() )
            .withAttributeCombo( parameters.getAttributeOptionCombo() )
            .withDefaultAttributeCombo( categoryService.getDefaultCategoryOptionCombo() )
            .withDimensionItemMap( dimensionItemMap )
            .withMaxResults( parameters.getMaxResults() );

        if ( currentUser != null )
        {
            builder
                .withCoDimensionConstraints(
                    categoryService.getCoDimensionConstraints( currentUser.getUserCredentials() ) )
                .withCogDimensionConstraints(
                    categoryService.getCogDimensionConstraints( currentUser.getUserCredentials() ) );
        }

        return builder.build();
    }

    /**
     * Adds Periods to the context, grouped by period type.
     *
     * @param periodTypeXMap period type map to extended period types.
     * @param periods periods to group and add.
     */
    private void addPeriodsToContext( Map<PeriodType, PeriodTypeExtended> periodTypeXMap,
        Collection<Period> periods )
    {
        for ( Period period : periods )
        {
            PeriodTypeExtended periodTypeX = getOrCreatePeriodTypeExtended( periodTypeXMap,
                period.getPeriodType() );
            periodTypeX.addPeriod( period );
        }

        generateAllowedPeriods( periodTypeXMap.values() );
    }

    /**
     * For each period type, allow all the longer period types in validation
     * queries.
     *
     * @param periodTypeXs period types to generate allowed period types from.
     */
    private void generateAllowedPeriods( Collection<PeriodTypeExtended> periodTypeXs )
    {
        for ( PeriodTypeExtended p : periodTypeXs )
        {
            for ( PeriodTypeExtended q : periodTypeXs )
            {
                if ( q.getPeriodType().getFrequencyOrder() >= p.getPeriodType().getFrequencyOrder() )
                {
                    p.getAllowedPeriodTypes().add( q.getPeriodType() );
                }
            }
        }
    }

    /**
     * Adds validation rules to the context.
     *
     * @param periodTypeXMap period type map to extended period types.
     * @param rules validation rules to add.
     * @return the map from DimensionalItemId to DimensionalItemObject.
     */
    private Map<DimensionalItemId, DimensionalItemObject> addRulesToContext(
        Map<PeriodType, PeriodTypeExtended> periodTypeXMap,
        Collection<ValidationRule> rules )
    {
        // 1. Find all dimensional object IDs in the expressions of the
        // validation rules.

        Set<DimensionalItemId> allItemIds = new HashSet<>();

        SetMap<PeriodTypeExtended, DimensionalItemId> periodItemIds = new SetMap<>();

        getItemIdsForRules( allItemIds, periodItemIds, periodTypeXMap, rules );

        // 2. Get the dimensional objects from the IDs. (Get them all at once
        // for best performance.)

        Map<DimensionalItemId, DimensionalItemObject> dimensionItemMap = dimensionService
            .getNoAclDataDimensionalItemObjectMap( allItemIds );

        // 3. Save the dimensional objects in the extended period types.

        saveObjectsInPeriodTypeX( periodItemIds, dimensionItemMap );

        return dimensionItemMap;
    }

    /**
     * Finds all the dimensional object IDs in the validation rules expressions.
     *
     * @param allItemIds inserts all IDs here.
     * @param periodItemIds inserts IDs by period type here.
     * @param periodTypeXMap map of extended period types by period type.
     * @param rules validation rules to process.
     */
    private void getItemIdsForRules( Set<DimensionalItemId> allItemIds,
        SetMap<PeriodTypeExtended, DimensionalItemId> periodItemIds,
        Map<PeriodType, PeriodTypeExtended> periodTypeXMap,
        Collection<ValidationRule> rules )
    {
        for ( ValidationRule rule : rules )
        {
            PeriodTypeExtended periodX = periodTypeXMap.get( rule.getPeriodType() );

            if ( periodX == null )
            {
                continue; // Don't include rule.
            }

            ValidationRuleExtended ruleX = new ValidationRuleExtended( rule );

            periodX.getRuleXs().add( ruleX );

            periodX.setSlidingWindows( ruleX.getLeftSlidingWindow() );
            periodX.setSlidingWindows( ruleX.getRightSlidingWindow() );

            Set<DimensionalItemId> leftSideItemIds = expressionService.getExpressionDimensionalItemIds(
                rule.getLeftSide().getExpression(), VALIDATION_RULE_EXPRESSION );

            Set<DimensionalItemId> rightSideItemIds = expressionService.getExpressionDimensionalItemIds(
                rule.getRightSide().getExpression(), VALIDATION_RULE_EXPRESSION );

            periodX.getLeftSideItemIds().addAll( leftSideItemIds );
            periodX.getRightSideItemIds().addAll( rightSideItemIds );

            Set<DimensionalItemId> bothSidesItemIds = Sets.union( leftSideItemIds, rightSideItemIds );

            periodItemIds.putValues( periodX, bothSidesItemIds );

            allItemIds.addAll( bothSidesItemIds );
        }
    }

    /**
     * Saves the dimension item objects in the period type extended, organizing
     * them according to how they will be used in fetching their values.
     *
     * @param periodItemIds map from periodX to set of object IDs.
     * @param dimensionItemMap map from object ID to Object.
     */
    private void saveObjectsInPeriodTypeX(
        SetMap<PeriodTypeExtended, DimensionalItemId> periodItemIds,
        Map<DimensionalItemId, DimensionalItemObject> dimensionItemMap )
    {
        for ( Map.Entry<PeriodTypeExtended, Set<DimensionalItemId>> entry : periodItemIds.entrySet() )
        {
            PeriodTypeExtended periodTypeX = entry.getKey();

            for ( DimensionalItemId itemId : entry.getValue() )
            {
                DimensionalItemObject item = dimensionItemMap.get( itemId );

                if ( item != null )
                {
                    if ( DimensionItemType.DATA_ELEMENT == item.getDimensionItemType() )
                    {
                        periodTypeX.addDataElement( (DataElement) item );
                    }
                    else if ( DimensionItemType.DATA_ELEMENT_OPERAND == item.getDimensionItemType() )
                    {
                        periodTypeX.addDataElementOperand( (DataElementOperand) item );
                    }
                    else if ( DimensionItemType.INDICATOR == item.getDimensionItemType() )
                    {
                        periodTypeX.addIndicator( item );
                    }
                    else if ( hasAttributeOptions( item ) )
                    {
                        periodTypeX.getEventItems().add( item );
                    }
                    else
                    {
                        periodTypeX.getEventItemsWithoutAttributeOptions().add( item );
                    }
                }
            }
        }
    }

    /**
     * Checks to see if a dimensional item object has values stored in the
     * database by attribute option combo.
     *
     * @param object dimensional item object
     * @return true if values are stored by attribuete option combo.
     */
    private boolean hasAttributeOptions( DimensionalItemObject object )
    {
        return object.getDimensionItemType() != DimensionItemType.PROGRAM_INDICATOR
            || ((ProgramIndicator) object).getAnalyticsType() != AnalyticsType.ENROLLMENT;
    }

    /**
     * Removes any period types that don't have rules assigned to them.
     *
     * @param periodTypeXMap period type map to extended period types.
     */
    private void removeAnyUnneededPeriodTypes( Map<PeriodType, PeriodTypeExtended> periodTypeXMap )
    {
        List<PeriodTypeExtended> periodTypeXs = new ArrayList<>( periodTypeXMap.values() );

        for ( PeriodTypeExtended periodTypeX : periodTypeXs )
        {
            if ( periodTypeX.getRuleXs().isEmpty() )
            {
                periodTypeXMap.remove( periodTypeX.getPeriodType() );
            }
        }
    }

    /**
     * Gets the PeriodTypeExtended from the context object. If not found,
     * creates a new PeriodTypeExtended object, puts it into the context object,
     * and returns it.
     *
     * @param periodTypeXMap period type map to extended period types.
     * @param periodType period type to search for
     * @return period type extended from the context object
     */
    private PeriodTypeExtended getOrCreatePeriodTypeExtended( Map<PeriodType, PeriodTypeExtended> periodTypeXMap,
        PeriodType periodType )
    {
        PeriodTypeExtended periodTypeX = periodTypeXMap.get( periodType );

        if ( periodTypeX == null )
        {
            periodTypeX = new PeriodTypeExtended( periodService.reloadPeriodType( periodType ) );
            periodTypeXMap.put( periodType, periodTypeX );
        }

        return periodTypeX;
    }
}
