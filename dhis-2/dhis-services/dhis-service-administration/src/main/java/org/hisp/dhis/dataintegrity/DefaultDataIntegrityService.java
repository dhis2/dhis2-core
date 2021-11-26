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
package org.hisp.dhis.dataintegrity;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.commons.collection.ListUtils.getDuplicates;
import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;
import static org.hisp.dhis.expression.ParseType.VALIDATION_RULE_EXPRESSION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.ExpressionValidationOutcome;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service( "org.hisp.dhis.dataintegrity.DataIntegrityService" )
@Transactional
@AllArgsConstructor
public class DefaultDataIntegrityService
    implements DataIntegrityService
{
    private static final String FORMULA_SEPARATOR = "#";

    private final I18nManager i18nManager;

    private final ProgramRuleService programRuleService;

    private final ProgramRuleActionService programRuleActionService;

    private final ProgramRuleVariableService programRuleVariableService;

    private final DataElementService dataElementService;

    private final IndicatorService indicatorService;

    private final DataSetService dataSetService;

    private final OrganisationUnitService organisationUnitService;

    private final OrganisationUnitGroupService organisationUnitGroupService;

    private final ValidationRuleService validationRuleService;

    private final ExpressionService expressionService;

    private final DataEntryFormService dataEntryFormService;

    private final CategoryService categoryService;

    private final PeriodService periodService;

    private final ProgramIndicatorService programIndicatorService;

    // -------------------------------------------------------------------------
    // DataElement
    // -------------------------------------------------------------------------

    /**
     * Gets all data elements which are not assigned to any data set.
     */
    List<DataElement> getDataElementsWithoutDataSet()
    {
        return sorted( new ArrayList<>( dataElementService.getDataElementsWithoutDataSets() ) );
    }

    /**
     * Gets all data elements which are not members of any groups.
     */
    List<DataElement> getDataElementsWithoutGroups()
    {
        return sorted( new ArrayList<>( dataElementService.getDataElementsWithoutGroups() ) );
    }

    /**
     * Returns all data elements which are members of data sets with different
     * period types.
     */
    SortedMap<DataElement, Collection<DataSet>> getDataElementsAssignedToDataSetsWithDifferentPeriodTypes()
    {
        Collection<DataElement> dataElements = dataElementService.getAllDataElements();

        Collection<DataSet> dataSets = dataSetService.getAllDataSets();

        SortedMap<DataElement, Collection<DataSet>> targets = new TreeMap<>();

        for ( DataElement element : dataElements )
        {
            final Set<PeriodType> targetPeriodTypes = new HashSet<>();
            final Collection<DataSet> targetDataSets = new HashSet<>();

            for ( DataSet dataSet : dataSets )
            {
                if ( dataSet.getDataElements().contains( element ) )
                {
                    targetPeriodTypes.add( dataSet.getPeriodType() );
                    targetDataSets.add( dataSet );
                }
            }

            if ( targetPeriodTypes.size() > 1 )
            {
                targets.put( element, targetDataSets );
            }
        }

        return targets;
    }

    /**
     * Gets all data elements units which are members of more than one group
     * which enter into an exclusive group set.
     */
    SortedMap<DataElement, Collection<DataElementGroup>> getDataElementsViolatingExclusiveGroupSets()
    {
        Collection<DataElementGroupSet> groupSets = dataElementService.getAllDataElementGroupSets();

        SortedMap<DataElement, Collection<DataElementGroup>> targets = new TreeMap<>();

        for ( DataElementGroupSet groupSet : groupSets )
        {
            Collection<DataElement> duplicates = getDuplicates(
                new ArrayList<>( groupSet.getDataElements() ) );

            for ( DataElement duplicate : duplicates )
            {
                targets.put( duplicate, duplicate.getGroups() );
            }
        }

        return targets;
    }

    /**
     * Returns all data elements which are member of a data set but not part of
     * either the custom form or sections of the data set.
     */
    SortedMap<DataSet, Collection<DataElement>> getDataElementsInDataSetNotInForm()
    {
        SortedMap<DataSet, Collection<DataElement>> map = new TreeMap<>();

        Collection<DataSet> dataSets = dataSetService.getAllDataSets();

        for ( DataSet dataSet : dataSets )
        {
            if ( !dataSet.getFormType().isDefault() )
            {
                Set<DataElement> formElements = new HashSet<>();

                if ( dataSet.hasDataEntryForm() )
                {
                    formElements.addAll( dataEntryFormService.getDataElementsInDataEntryForm( dataSet ) );
                }
                else if ( dataSet.hasSections() )
                {
                    formElements.addAll( dataSet.getDataElementsInSections() );
                }

                Set<DataElement> dataSetElements = new HashSet<>( dataSet.getDataElements() );

                dataSetElements.removeAll( formElements );

                if ( !dataSetElements.isEmpty() )
                {
                    map.put( dataSet, dataSetElements );
                }
            }
        }

        return map;
    }

    /**
     * Returns all invalid category combinations.
     */
    List<CategoryCombo> getInvalidCategoryCombos()
    {
        List<CategoryCombo> categoryCombos = categoryService.getAllCategoryCombos();

        return categoryCombos.stream().filter( c -> !c.isValid() ).collect( toList() );
    }

    // -------------------------------------------------------------------------
    // DataSet
    // -------------------------------------------------------------------------

    List<DataSet> getDataSetsNotAssignedToOrganisationUnits()
    {
        return sorted( new ArrayList<>( dataSetService.getDataSetsNotAssignedToOrganisationUnits() ) );
    }

    // -------------------------------------------------------------------------
    // Indicator
    // -------------------------------------------------------------------------

    /**
     * Gets all indicators with identical numerator and denominator.
     */
    Set<Set<Indicator>> getIndicatorsWithIdenticalFormulas()
    {
        Map<String, Indicator> formulas = new HashMap<>();

        Map<String, Set<Indicator>> targets = new HashMap<>();

        List<Indicator> indicators = indicatorService.getAllIndicators();

        for ( Indicator indicator : indicators )
        {
            final String formula = indicator.getNumerator() + FORMULA_SEPARATOR + indicator.getDenominator();

            if ( formulas.containsKey( formula ) )
            {
                if ( targets.containsKey( formula ) )
                {
                    targets.get( formula ).add( indicator );
                }
                else
                {
                    Set<Indicator> elements = new HashSet<>();

                    elements.add( indicator );
                    elements.add( formulas.get( formula ) );

                    targets.put( formula, elements );
                    targets.get( formula ).add( indicator );
                }
            }
            else
            {
                formulas.put( formula, indicator );
            }
        }

        return Sets.newHashSet( targets.values() );
    }

    /**
     * Gets all indicators which are not assigned to any groups.
     */
    List<Indicator> getIndicatorsWithoutGroups()
    {
        return sorted( new ArrayList<>( indicatorService.getIndicatorsWithoutGroups() ) );
    }

    /**
     * Gets all indicators with invalid indicator numerators.
     */
    SortedMap<Indicator, String> getInvalidIndicatorNumerators()
    {
        return getInvalidIndicators( Indicator::getNumerator );
    }

    /**
     * Gets all indicators with invalid indicator denominators.
     */
    SortedMap<Indicator, String> getInvalidIndicatorDenominators()
    {
        return getInvalidIndicators( Indicator::getDenominator );
    }

    private SortedMap<Indicator, String> getInvalidIndicators( Function<Indicator, String> getter )
    {
        SortedMap<Indicator, String> invalids = new TreeMap<>();
        I18n i18n = i18nManager.getI18n();

        for ( Indicator indicator : indicatorService.getAllIndicators() )
        {
            ExpressionValidationOutcome result = expressionService.expressionIsValid( getter.apply( indicator ),
                INDICATOR_EXPRESSION );

            if ( !result.isValid() )
            {
                invalids.put( indicator, i18n.getString( result.getKey() ) );
            }
        }

        return invalids;
    }

    /**
     * Gets all indicators units which are members of more than one group which
     * enter into an exclusive group set.
     */
    SortedMap<Indicator, Collection<IndicatorGroup>> getIndicatorsViolatingExclusiveGroupSets()
    {
        Collection<IndicatorGroupSet> groupSets = indicatorService.getAllIndicatorGroupSets();

        SortedMap<Indicator, Collection<IndicatorGroup>> targets = new TreeMap<>();

        for ( IndicatorGroupSet groupSet : groupSets )
        {
            Collection<Indicator> duplicates = getDuplicates(
                new ArrayList<>( groupSet.getIndicators() ) );

            for ( Indicator duplicate : duplicates )
            {
                targets.put( duplicate, duplicate.getGroups() );
            }
        }

        return targets;
    }

    // -------------------------------------------------------------------------
    // Period
    // -------------------------------------------------------------------------

    /**
     * Lists all Periods which are duplicates, based on the period type and
     * start date.
     */
    List<Period> getDuplicatePeriods()
    {
        Collection<Period> periods = periodService.getAllPeriods();

        List<Period> duplicates = new ArrayList<>();

        ListMap<String, Period> map = new ListMap<>();

        for ( Period period : periods )
        {
            String key = period.getPeriodType().getName() + period.getStartDate().toString();

            period.setName( period.toString() );

            map.putValue( key, period );
        }

        for ( List<Period> values : map.values() )
        {
            if ( values != null && values.size() > 1 )
            {
                duplicates.addAll( values );
            }
        }

        return duplicates;
    }

    // -------------------------------------------------------------------------
    // OrganisationUnit
    // -------------------------------------------------------------------------

    List<OrganisationUnit> getOrganisationUnitsWithCyclicReferences()
    {
        return sorted( new ArrayList<>( organisationUnitService.getOrganisationUnitsWithCyclicReferences() ) );
    }

    List<OrganisationUnit> getOrphanedOrganisationUnits()
    {
        return sorted( new ArrayList<>( organisationUnitService.getOrphanedOrganisationUnits() ) );
    }

    List<OrganisationUnit> getOrganisationUnitsWithoutGroups()
    {
        return sorted( new ArrayList<>( organisationUnitService.getOrganisationUnitsWithoutGroups() ) );
    }

    SortedMap<OrganisationUnit, Collection<OrganisationUnitGroup>> getOrganisationUnitsViolatingExclusiveGroupSets()
    {
        return groupsByUnit( organisationUnitService.getOrganisationUnitsViolatingExclusiveGroupSets() );
    }

    List<OrganisationUnitGroup> getOrganisationUnitGroupsWithoutGroupSets()
    {
        return sorted( new ArrayList<>( organisationUnitGroupService.getOrganisationUnitGroupsWithoutGroupSets() ) );
    }

    // -------------------------------------------------------------------------
    // ValidationRule
    // -------------------------------------------------------------------------

    List<ValidationRule> getValidationRulesWithoutGroups()
    {
        return sorted( new ArrayList<>( validationRuleService.getValidationRulesWithoutGroups() ) );
    }

    /**
     * Gets all ValidationRules with invalid left side expressions.
     */
    SortedMap<ValidationRule, String> getInvalidValidationRuleLeftSideExpressions()
    {
        return getInvalidValidationRuleExpressions( ValidationRule::getLeftSide );
    }

    /**
     * Gets all ValidationRules with invalid right side expressions.
     */
    SortedMap<ValidationRule, String> getInvalidValidationRuleRightSideExpressions()
    {
        return getInvalidValidationRuleExpressions( ValidationRule::getRightSide );
    }

    private SortedMap<ValidationRule, String> getInvalidValidationRuleExpressions(
        Function<ValidationRule, Expression> getter )
    {
        SortedMap<ValidationRule, String> invalids = new TreeMap<>();
        I18n i18n = i18nManager.getI18n();

        for ( ValidationRule rule : validationRuleService.getAllValidationRules() )
        {
            ExpressionValidationOutcome result = expressionService
                .expressionIsValid( getter.apply( rule ).getExpression(), VALIDATION_RULE_EXPRESSION );

            if ( !result.isValid() )
            {
                invalids.put( rule, i18n.getString( result.getKey() ) );
            }
        }

        return invalids;
    }

    @Getter
    @AllArgsConstructor
    private static class DataIntegrityCheck<T>
    {
        private final DataIntegrityCheckType type;

        private final Supplier<T> check;

        private final BiConsumer<DataIntegrityReport, T> setter;
    }

    private final Map<DataIntegrityCheckType, DataIntegrityCheck<?>> integrityChecks = new ConcurrentHashMap<>();

    private <T> void registerIntegrityCheck( DataIntegrityCheckType type, Supplier<T> check,
        BiConsumer<DataIntegrityReport, T> setter )
    {
        integrityChecks.put( type, new DataIntegrityCheck<>( type, check, setter ) );
    }

    /**
     * Maps all {@link DataIntegrityCheck}s to their implementation and the
     * report field to set with the result.
     */
    @PostConstruct
    public void initIntegrityChecks()
    {
        registerIntegrityCheck( DataIntegrityCheckType.DATA_ELEMENTS_WITHOUT_DATA_SETS,
            this::getDataElementsWithoutDataSet, DataIntegrityReport::setDataElementsWithoutDataSet );
        registerIntegrityCheck( DataIntegrityCheckType.DATA_ELEMENTS_WITHOUT_GROUPS, this::getDataElementsWithoutGroups,
            DataIntegrityReport::setDataElementsWithoutGroups );
        registerIntegrityCheck( DataIntegrityCheckType.DATA_ELEMENTS_ASSIGNED_TO_DATA_SETS_WITH_DIFFERENT_PERIOD_TYPES,
            this::getDataElementsAssignedToDataSetsWithDifferentPeriodTypes,
            DataIntegrityReport::setDataElementsAssignedToDataSetsWithDifferentPeriodTypes );
        registerIntegrityCheck( DataIntegrityCheckType.DATA_ELEMENTS_VIOLATING_EXCLUSIVE_GROUP_SETS,
            this::getDataElementsViolatingExclusiveGroupSets,
            DataIntegrityReport::setDataElementsViolatingExclusiveGroupSets );
        registerIntegrityCheck( DataIntegrityCheckType.DATA_ELEMENTS_IN_DATA_SET_NOT_IN_FORM,
            this::getDataElementsInDataSetNotInForm, DataIntegrityReport::setDataElementsInDataSetNotInForm );

        registerIntegrityCheck( DataIntegrityCheckType.CATEGORY_COMBOS_BEING_INVALID, this::getInvalidCategoryCombos,
            DataIntegrityReport::setInvalidCategoryCombos );

        registerIntegrityCheck( DataIntegrityCheckType.DATA_SETS_NOT_ASSIGNED_TO_ORG_UNITS,
            this::getDataSetsNotAssignedToOrganisationUnits,
            DataIntegrityReport::setDataSetsNotAssignedToOrganisationUnits );

        registerIntegrityCheck( DataIntegrityCheckType.INDICATORS_WITH_IDENTICAL_FORMULAS,
            this::getIndicatorsWithIdenticalFormulas, DataIntegrityReport::setIndicatorsWithIdenticalFormulas );
        registerIntegrityCheck( DataIntegrityCheckType.INDICATORS_WITHOUT_GROUPS, this::getIndicatorsWithoutGroups,
            DataIntegrityReport::setIndicatorsWithoutGroups );
        registerIntegrityCheck( DataIntegrityCheckType.INDICATORS_WITH_INVALID_NUMERATOR,
            this::getInvalidIndicatorNumerators, DataIntegrityReport::setInvalidIndicatorNumerators );
        registerIntegrityCheck( DataIntegrityCheckType.INDICATORS_WITH_INVALID_DENOMINATOR,
            this::getInvalidIndicatorDenominators, DataIntegrityReport::setInvalidIndicatorDenominators );
        registerIntegrityCheck( DataIntegrityCheckType.INDICATORS_VIOLATING_EXCLUSIVE_GROUP_SETS,
            this::getIndicatorsViolatingExclusiveGroupSets,
            DataIntegrityReport::setIndicatorsViolatingExclusiveGroupSets );

        registerIntegrityCheck( DataIntegrityCheckType.PERIODS_DUPLICATES, this::getDuplicatePeriods,
            DataIntegrityReport::setDuplicatePeriods );

        registerIntegrityCheck( DataIntegrityCheckType.ORG_UNITS_WITH_CYCLIC_REFERENCES,
            this::getOrganisationUnitsWithCyclicReferences,
            DataIntegrityReport::setOrganisationUnitsWithCyclicReferences );
        registerIntegrityCheck( DataIntegrityCheckType.ORG_UNITS_BEING_ORPHANED, this::getOrphanedOrganisationUnits,
            DataIntegrityReport::setOrphanedOrganisationUnits );
        registerIntegrityCheck( DataIntegrityCheckType.ORG_UNITS_WITHOUT_GROUPS,
            this::getOrganisationUnitsWithoutGroups, DataIntegrityReport::setOrganisationUnitsWithoutGroups );
        registerIntegrityCheck( DataIntegrityCheckType.ORG_UNITS_VIOLATING_EXCLUSIVE_GROUP_SETS,
            this::getOrganisationUnitsViolatingExclusiveGroupSets,
            DataIntegrityReport::setOrganisationUnitsViolatingExclusiveGroupSets );
        registerIntegrityCheck( DataIntegrityCheckType.ORG_UNIT_GROUPS_WITHOUT_GROUP_SETS,
            this::getOrganisationUnitGroupsWithoutGroupSets,
            DataIntegrityReport::setOrganisationUnitGroupsWithoutGroupSets );

        registerIntegrityCheck( DataIntegrityCheckType.VALIDATION_RULES_WITHOUT_GROUPS,
            this::getValidationRulesWithoutGroups, DataIntegrityReport::setValidationRulesWithoutGroups );
        registerIntegrityCheck( DataIntegrityCheckType.VALIDATION_RULES_WITH_INVALID_LEFT_SIDE_EXPRESSION,
            this::getInvalidValidationRuleLeftSideExpressions,
            DataIntegrityReport::setInvalidValidationRuleLeftSideExpressions );
        registerIntegrityCheck( DataIntegrityCheckType.VALIDATION_RULES_WITH_INVALID_RIGHT_SIDE_EXPRESSION,
            this::getInvalidValidationRuleRightSideExpressions,
            DataIntegrityReport::setInvalidValidationRuleRightSideExpressions );

        registerIntegrityCheck( DataIntegrityCheckType.PROGRAM_INDICATORS_WITH_INVALID_EXPRESSIONS,
            this::getInvalidProgramIndicatorExpressions, DataIntegrityReport::setInvalidProgramIndicatorExpressions );
        registerIntegrityCheck( DataIntegrityCheckType.PROGRAM_INDICATORS_WITH_INVALID_FILTERS,
            this::getInvalidProgramIndicatorFilters, DataIntegrityReport::setInvalidProgramIndicatorFilters );
        registerIntegrityCheck( DataIntegrityCheckType.PROGRAM_INDICATORS_WITHOUT_EXPRESSION,
            this::getProgramIndicatorsWithNoExpression, DataIntegrityReport::setProgramIndicatorsWithNoExpression );

        registerIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULES_WITHOUT_CONDITION,
            this::getProgramRulesWithNoCondition, DataIntegrityReport::setProgramRulesWithoutCondition );
        registerIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULES_WITHOUT_PRIORITY,
            this::getProgramRulesWithNoPriority, DataIntegrityReport::setProgramRulesWithNoPriority );
        registerIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULES_WITHOUT_ACTION, this::getProgramRulesWithNoAction,
            DataIntegrityReport::setProgramRulesWithNoAction );

        registerIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULE_VARIABLES_WITHOUT_DATA_ELEMENT,
            this::getProgramRuleVariablesWithNoDataElement,
            DataIntegrityReport::setProgramRuleVariablesWithNoDataElement );
        registerIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULE_VARIABLES_WITHOUT_ATTRIBUTE,
            this::getProgramRuleVariablesWithNoAttribute, DataIntegrityReport::setProgramRuleVariablesWithNoAttribute );

        registerIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULE_ACTIONS_WITHOUT_DATA_OBJECT,
            this::getProgramRuleActionsWithNoDataObject, DataIntegrityReport::setProgramRuleActionsWithNoDataObject );
        registerIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULE_ACTIONS_WITHOUT_NOTIFICATION,
            this::getProgramRuleActionsWithNoNotificationTemplate,
            DataIntegrityReport::setProgramRuleActionsWithNoNotification );
        registerIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULE_ACTIONS_WITHOUT_SECTION,
            this::getProgramRuleActionsWithNoSectionId, DataIntegrityReport::setProgramRuleActionsWithNoSectionId );
        registerIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULE_ACTIONS_WITHOUT_STAGE,
            this::getProgramRuleActionsWithNoProgramStageId, DataIntegrityReport::setProgramRuleActionsWithNoStageId );
    }

    @Override
    @Transactional( readOnly = true )
    public FlattenedDataIntegrityReport getFlattenedDataIntegrityReport( Set<DataIntegrityCheckType> checks,
        JobProgress progress )
    {
        return new FlattenedDataIntegrityReport( getDataIntegrityReport( checks, progress ) );
    }

    @Override
    @Transactional( readOnly = true )
    public DataIntegrityReport getDataIntegrityReport( Set<DataIntegrityCheckType> checks, JobProgress progress )
    {
        progress.startingProcess( "Data Integrity check" );
        DataIntegrityReport report = new DataIntegrityReport();
        for ( DataIntegrityCheckType type : checks )
        {
            performDataIntegrityCheck( integrityChecks.get( type ), report, progress );
        }
        progress.completedProcess( null );
        return report;
    }

    private <T> void performDataIntegrityCheck( DataIntegrityCheck<T> check, DataIntegrityReport report,
        JobProgress progress )
    {
        progress.startingStage( check.getType().name().toLowerCase().replace( '_', ' ' ) );
        progress.runStage( () -> check.getSetter().accept( report, check.getCheck().get() ) );
    }

    private static <T extends Comparable<? super T>> List<T> sorted( List<T> list )
    {
        Collections.sort( list );
        return list;
    }

    private static SortedMap<OrganisationUnit, Collection<OrganisationUnitGroup>> groupsByUnit(
        Collection<OrganisationUnit> units )
    {
        SortedMap<OrganisationUnit, Collection<OrganisationUnitGroup>> groupsByUnit = new TreeMap<>();
        for ( OrganisationUnit unit : units )
        {
            groupsByUnit.put( unit, new HashSet<>( unit.getGroups() ) );
        }
        return groupsByUnit;
    }

    /**
     * Get all ProgramIndicators with no expression.
     */
    List<ProgramIndicator> getProgramIndicatorsWithNoExpression()
    {
        return programIndicatorService.getProgramIndicatorsWithNoExpression();
    }

    /**
     * Get all ProgramIndicators with invalid expressions.
     */
    Map<ProgramIndicator, String> getInvalidProgramIndicatorExpressions()
    {
        return getInvalidProgramIndicators( ProgramIndicator::getExpression,
            pi -> !programIndicatorService.expressionIsValid( pi.getExpression() ) );
    }

    /**
     * Get all ProgramIndicators with invalid filters.
     */
    Map<ProgramIndicator, String> getInvalidProgramIndicatorFilters()
    {
        return getInvalidProgramIndicators( ProgramIndicator::getFilter,
            pi -> !programIndicatorService.filterIsValid( pi.getFilter() ) );
    }

    private Map<ProgramIndicator, String> getInvalidProgramIndicators( Function<ProgramIndicator, String> property,
        Predicate<ProgramIndicator> filter )
    {
        List<ProgramIndicator> programIndicators = programIndicatorService.getAllProgramIndicators()
            .stream()
            .filter( filter )
            .collect( toList() );

        Map<ProgramIndicator, String> invalids = new HashMap<>();
        for ( ProgramIndicator programIndicator : programIndicators )
        {
            String description = getInvalidExpressionDescription( property.apply( programIndicator ) );
            if ( description != null )
            {
                invalids.put( programIndicator, description );
            }
        }
        return invalids;
    }

    /**
     * Get all ProgramRules with no priority and grouped them by {@link Program}
     */
    Map<Program, Collection<ProgramRule>> getProgramRulesWithNoPriority()
    {
        return groupRulesByProgram( programRuleService.getProgramRulesWithNoPriority() );
    }

    /**
     * Get all ProgramRules with no action and grouped them by {@link Program}
     */
    Map<Program, Collection<ProgramRule>> getProgramRulesWithNoAction()
    {
        return groupRulesByProgram( programRuleService.getProgramRulesWithNoAction() );
    }

    /**
     * Get all ProgramRules with no condition expression and grouped them by
     * {@link Program}
     */
    Map<Program, Collection<ProgramRule>> getProgramRulesWithNoCondition()
    {
        return groupRulesByProgram( programRuleService.getProgramRulesWithNoCondition() );
    }

    /**
     * @return all {@link ProgramRuleAction} which are not linked to any
     *         DataElement/TrackedEntityAttribute
     */
    Map<ProgramRule, Collection<ProgramRuleAction>> getProgramRuleActionsWithNoDataObject()
    {
        return groupActionsByProgramRule( programRuleActionService.getProgramActionsWithNoLinkToDataObject() );
    }

    /**
     * @return all {@link ProgramRuleAction} which are not linked to any
     *         {@link org.hisp.dhis.notification.NotificationTemplate}
     */
    Map<ProgramRule, Collection<ProgramRuleAction>> getProgramRuleActionsWithNoNotificationTemplate()
    {
        return groupActionsByProgramRule( programRuleActionService.getProgramActionsWithNoLinkToNotification() );
    }

    /**
     * @return all {@link ProgramRuleAction} which are not linked to any
     *         {@link org.hisp.dhis.program.ProgramStageSection}
     */
    Map<ProgramRule, Collection<ProgramRuleAction>> getProgramRuleActionsWithNoSectionId()
    {
        return groupActionsByProgramRule( programRuleActionService.getProgramRuleActionsWithNoSectionId() );
    }

    /**
     * @return all {@link ProgramRuleAction} which are not linked to any
     *         {@link org.hisp.dhis.program.ProgramStage}
     */
    Map<ProgramRule, Collection<ProgramRuleAction>> getProgramRuleActionsWithNoProgramStageId()
    {
        return groupActionsByProgramRule( programRuleActionService.getProgramRuleActionsWithNoStageId() );
    }

    /**
     * @return all {@link ProgramRuleVariable} which are not linked to any
     *         DataElement and grouped them by {@link Program}
     */
    Map<Program, Collection<ProgramRuleVariable>> getProgramRuleVariablesWithNoDataElement()
    {
        return groupVariablesByProgram( programRuleVariableService.getVariablesWithNoDataElement() );
    }

    /**
     * @return all {@link ProgramRuleVariable} which are not linked to any
     *         TrackedEntityAttribute and grouped them by {@link Program}
     */
    Map<Program, Collection<ProgramRuleVariable>> getProgramRuleVariablesWithNoAttribute()
    {
        return groupVariablesByProgram( programRuleVariableService.getVariablesWithNoAttribute() );
    }

    private String getInvalidExpressionDescription( String expression )
    {
        try
        {
            expressionService.getExpressionDescription( expression, INDICATOR_EXPRESSION );
        }
        catch ( ParserException e )
        {
            return e.getMessage();
        }

        return null;
    }

    private static Map<Program, Collection<ProgramRule>> groupRulesByProgram( List<ProgramRule> rules )
    {
        return groupBy( ProgramRule::getProgram, rules );
    }

    private static Map<Program, Collection<ProgramRuleVariable>> groupVariablesByProgram(
        List<ProgramRuleVariable> variables )
    {
        return groupBy( ProgramRuleVariable::getProgram, variables );
    }

    private static Map<ProgramRule, Collection<ProgramRuleAction>> groupActionsByProgramRule(
        List<ProgramRuleAction> actions )
    {
        return groupBy( ProgramRuleAction::getProgramRule, actions );
    }

    private static <K, V> Map<K, Collection<V>> groupBy( Function<V, K> property,
        Collection<V> values )
    {
        Map<K, Collection<V>> res = new HashMap<>();
        for ( V value : values )
        {
            res.computeIfAbsent( property.apply( value ), key -> Sets.newHashSet() ).add( value );
        }
        return res;
    }
}
