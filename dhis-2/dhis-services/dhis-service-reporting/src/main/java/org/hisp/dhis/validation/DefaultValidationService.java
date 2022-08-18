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
package org.hisp.dhis.validation;

import static org.hisp.dhis.expression.ParseType.VALIDATION_RULE_EXPRESSION;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_STAGE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.dataanalysis.ValidationRuleExpressionDetails;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionInfo;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.CurrentUserServiceTarget;
import org.hisp.dhis.user.User;
import org.hisp.dhis.validation.notification.ValidationNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequiredArgsConstructor
public class DefaultValidationService implements ValidationService, CurrentUserServiceTarget
{
    private final PeriodService periodService;

    private final OrganisationUnitService organisationUnitService;

    private final ExpressionService expressionService;

    private final DataValueService dataValueService;

    private final CategoryService categoryService;

    private final ValidationNotificationService notificationService;

    private final ValidationRuleService validationRuleService;

    private final ValidationResultService validationResultService;

    private final DataValidationRunner runner;

    private CurrentUserService currentUserService;

    @Override
    @Autowired
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    // -------------------------------------------------------------------------
    // ValidationRule business logic
    // -------------------------------------------------------------------------

    @Override
    public List<ValidationResult> validationAnalysis( ValidationAnalysisParams parameters, JobProgress progress )
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

        List<ValidationResult> results = Validator.validate( context, runner, progress );

        if ( context.isPersistResults() )
        {
            progress.startingStage( "Persisting Results", SKIP_STAGE );
            progress.runStage( () -> validationResultService.saveValidationResults( context.getValidationResults() ) );
        }

        clock.logTime( "Finished validation analysis, " + context.getValidationResults().size() + " results" ).stop();

        if ( context.isSendNotifications() )
        {
            notificationService.sendNotifications( results, progress );
        }

        return results;
    }

    @Override
    public ValidationRuleExpressionDetails getValidationRuleExpressionDetails( ValidationAnalysisParams parameters )
    {
        ValidationRunContext context = getValidationContext( parameters );

        ValidationRuleExpressionDetails details = new ValidationRuleExpressionDetails();

        context.setValidationRuleExpressionDetails( details );

        Validator.validate( context, runner, NoopJobProgress.INSTANCE );

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

        Map<PeriodType, PeriodTypeExtended> periodTypeXMap = getExtendedPeriods( parameters );

        ExpressionParams baseExParams = getExpressionInfo( periodTypeXMap, parameters.getValidationRules() );

        ValidationRunContext.Builder builder = ValidationRunContext.newBuilder()
            .withOrgUnits( getOrganisationUnits( parameters ) )
            .withPeriodTypeXs( new ArrayList<>( periodTypeXMap.values() ) )
            .withSendNotifications( parameters.isSendNotifications() )
            .withPersistResults( parameters.isPersistResults() )
            .withAttributeCombo( parameters.getAttributeOptionCombo() )
            .withDefaultAttributeCombo( categoryService.getDefaultCategoryOptionCombo() )
            .withBaseExParams( baseExParams )
            .withItemMap( baseExParams.getItemMap() )
            .withMaxResults( parameters.getMaxResults() );

        if ( currentUser != null )
        {
            builder
                .withCoDimensionConstraints( categoryService.getCoDimensionConstraints( currentUser ) )
                .withCogDimensionConstraints( categoryService.getCogDimensionConstraints( currentUser ) );
        }

        List<ValidationResult> initialResults = validationResultService
            .getValidationResults( parameters.getOrgUnit(),
                parameters.isIncludeOrgUnitDescendants(), parameters.getValidationRules(),
                parameters.getPeriods() );
        return builder.build()
            .addInitialResults( initialResults );
    }

    private Map<PeriodType, PeriodTypeExtended> getExtendedPeriods(
        ValidationAnalysisParams parameters )
    {
        Map<PeriodType, PeriodTypeExtended> byType = new HashMap<>();

        addPeriodsToContext( byType, parameters.getPeriods() );

        setRulesAndSlidingWindows( byType, parameters.getValidationRules() );

        removeAnyUnneededPeriodTypes( byType );
        return byType;
    }

    private List<OrganisationUnit> getOrganisationUnits( ValidationAnalysisParams parameters )
    {
        OrganisationUnit ou = parameters.getOrgUnit();
        if ( ou == null )
        {
            return organisationUnitService.getAllOrganisationUnits();
        }
        if ( parameters.isIncludeOrgUnitDescendants() )
        {
            return organisationUnitService.getOrganisationUnitWithChildren( ou.getUid() );
        }
        return Lists.newArrayList( ou );
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

    private void setRulesAndSlidingWindows( Map<PeriodType, PeriodTypeExtended> periodTypeXMap,
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
        }
    }

    private ExpressionParams getExpressionInfo( Map<PeriodType, PeriodTypeExtended> periodTypeXMap,
        Collection<ValidationRule> rules )
    {
        SetMap<PeriodTypeExtended, DimensionalItemId> periodItemIds = new SetMap<>();

        Set<DimensionalItemId> allItemIds = new HashSet<>();

        ExpressionInfo expressionInfo = new ExpressionInfo();

        for ( ValidationRule rule : rules )
        {
            Set<DimensionalItemId> leftItemIds = addToExpressionInfo( expressionInfo, rule.getLeftSide() );
            Set<DimensionalItemId> rightItemIds = addToExpressionInfo( expressionInfo, rule.getRightSide() );

            processItemIds( leftItemIds, rightItemIds, rule, periodTypeXMap, periodItemIds, allItemIds );
        }

        expressionInfo.setItemIds( allItemIds );

        ExpressionParams baseExParams = expressionService.getBaseExpressionParams( expressionInfo );

        saveObjectsInPeriodTypeX( periodItemIds, baseExParams.getItemMap() );

        return baseExParams;
    }

    private Set<DimensionalItemId> addToExpressionInfo( ExpressionInfo exInfo, Expression expr )
    {
        exInfo.setItemIds( new HashSet<>() );

        expressionService.getExpressionInfo( ExpressionParams.builder()
            .expression( expr.getExpression() )
            .parseType( VALIDATION_RULE_EXPRESSION )
            .expressionInfo( exInfo )
            .build() );

        return exInfo.getItemIds();
    }

    private void processItemIds( Set<DimensionalItemId> leftItemIds, Set<DimensionalItemId> rightItemIds,
        ValidationRule rule, Map<PeriodType, PeriodTypeExtended> periodTypeXMap,
        SetMap<PeriodTypeExtended, DimensionalItemId> periodItemIds, Set<DimensionalItemId> allItemIds )
    {
        PeriodTypeExtended periodX = periodTypeXMap.get( rule.getPeriodType() );

        if ( periodX == null )
        {
            return; // Don't include rule.
        }

        periodX.getLeftSideItemIds().addAll( leftItemIds );
        periodX.getRightSideItemIds().addAll( rightItemIds );

        Set<DimensionalItemId> bothSidesItemIds = Sets.union( leftItemIds, rightItemIds );

        periodItemIds.putValues( periodX, bothSidesItemIds );

        allItemIds.addAll( bothSidesItemIds );
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
