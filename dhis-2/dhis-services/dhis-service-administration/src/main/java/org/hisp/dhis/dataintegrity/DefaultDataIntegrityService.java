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
package org.hisp.dhis.dataintegrity;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.commons.collection.ListUtils.getDuplicates;
import static org.hisp.dhis.dataintegrity.DataIntegrityDetails.DataIntegrityIssue.toIssue;
import static org.hisp.dhis.dataintegrity.DataIntegrityDetails.DataIntegrityIssue.toRefsList;
import static org.hisp.dhis.dataintegrity.DataIntegrityYamlReader.readDataIntegrityYaml;
import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;
import static org.hisp.dhis.expression.ParseType.VALIDATION_RULE_EXPRESSION;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.dataintegrity.DataIntegrityDetails.DataIntegrityIssue;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.ExpressionValidationOutcome;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.indicator.Indicator;
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
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
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

    private final CacheProvider cacheProvider;

    private final DataIntegrityStore dataIntegrityStore;

    private final SchemaService schemaService;

    private Cache<DataIntegritySummary> summaryCache;

    private Cache<DataIntegrityDetails> detailsCache;

    @PostConstruct
    public void init()
    {
        summaryCache = cacheProvider.createDataIntegritySummaryCache();
        detailsCache = cacheProvider.createDataIntegrityDetailsCache();
    }

    private static int alphabeticalOrder( DataIntegrityIssue a, DataIntegrityIssue b )
    {
        return a.getName().compareTo( b.getName() );
    }

    private static List<DataIntegrityIssue> toSimpleIssueList( Stream<? extends IdentifiableObject> items )
    {
        return items.map( DataIntegrityIssue::toIssue )
            .sorted( DefaultDataIntegrityService::alphabeticalOrder )
            .collect( toUnmodifiableList() );
    }

    private static <T extends IdentifiableObject> List<DataIntegrityIssue> toIssueList( Stream<T> items,
        Function<T, ? extends Collection<? extends IdentifiableObject>> toRefs )
    {
        return items.map( e -> DataIntegrityIssue.toIssue( e, toRefs.apply( e ) ) )
            .sorted( DefaultDataIntegrityService::alphabeticalOrder )
            .collect( toUnmodifiableList() );
    }

    // -------------------------------------------------------------------------
    // DataElement
    // -------------------------------------------------------------------------

    /**
     * Gets all data elements which are not assigned to any data set.
     */
    List<DataIntegrityIssue> getDataElementsWithoutDataSet()
    {
        return toSimpleIssueList( dataElementService.getDataElementsWithoutDataSets().stream() );

    }

    /**
     * Gets all data elements which are not members of any groups.
     */
    List<DataIntegrityIssue> getDataElementsWithoutGroups()
    {
        return toSimpleIssueList( dataElementService.getDataElementsWithoutGroups().stream() );
    }

    /**
     * Returns all data elements which are members of data sets with different
     * period types.
     */
    List<DataIntegrityIssue> getDataElementsAssignedToDataSetsWithDifferentPeriodTypes()
    {
        Collection<DataElement> dataElements = dataElementService.getAllDataElements();

        Collection<DataSet> dataSets = dataSetService.getAllDataSets();

        List<DataIntegrityIssue> issues = new ArrayList<>();

        for ( DataElement element : dataElements )
        {
            final Set<PeriodType> targetPeriodTypes = new HashSet<>();
            final List<DataSet> targetDataSets = new ArrayList<>();

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
                issues.add( DataIntegrityIssue.toIssue( element, targetDataSets ) );
            }
        }

        return issues;
    }

    /**
     * Gets all data elements units which are members of more than one group
     * which enter into an exclusive group set.
     */
    List<DataIntegrityIssue> getDataElementsViolatingExclusiveGroupSets()
    {
        Collection<DataElementGroupSet> groupSets = dataElementService.getAllDataElementGroupSets();

        List<DataIntegrityIssue> issues = new ArrayList<>();

        for ( DataElementGroupSet groupSet : groupSets )
        {
            Set<DataElement> duplicates = getDuplicates( groupSet.getDataElements() );

            for ( DataElement duplicate : duplicates )
            {
                issues.add( DataIntegrityIssue.toIssue( duplicate, duplicate.getGroups() ) );
            }
        }

        return issues;
    }

    /**
     * Returns all data elements which are member of a data set but not part of
     * either the custom form or sections of the data set.
     */
    List<DataIntegrityIssue> getDataElementsInDataSetNotInForm()
    {
        List<DataIntegrityIssue> issues = new ArrayList<>();

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
                    issues.add( DataIntegrityIssue.toIssue( dataSet, dataSetElements ) );
                }
            }
        }

        return issues;
    }

    /**
     * Returns all invalid category combinations.
     */
    List<DataIntegrityIssue> getInvalidCategoryCombos()
    {
        return toSimpleIssueList( categoryService.getAllCategoryCombos().stream().filter( c -> !c.isValid() ) );
    }

    // -------------------------------------------------------------------------
    // DataSet
    // -------------------------------------------------------------------------

    List<DataIntegrityIssue> getDataSetsNotAssignedToOrganisationUnits()
    {
        return toSimpleIssueList( dataSetService.getDataSetsNotAssignedToOrganisationUnits().stream() );
    }

    // -------------------------------------------------------------------------
    // Indicator
    // -------------------------------------------------------------------------

    /**
     * Gets all indicators with identical numerator and denominator.
     */
    List<DataIntegrityIssue> getIndicatorsWithIdenticalFormulas()
    {
        List<DataIntegrityIssue> issues = new ArrayList<>();

        Map<String, List<Indicator>> byFormula = indicatorService.getAllIndicators().stream().collect(
            groupingBy( indicator -> indicator.getNumerator() + FORMULA_SEPARATOR + indicator.getDenominator() ) );

        for ( Entry<String, List<Indicator>> e : byFormula.entrySet() )
        {
            if ( e.getValue().size() > 1 )
            {
                issues.add( new DataIntegrityIssue( null, e.getKey(), null, toRefsList( e.getValue().stream() ) ) );
            }
        }
        return issues;
    }

    /**
     * Gets all indicators which are not assigned to any groups.
     */
    List<DataIntegrityIssue> getIndicatorsWithoutGroups()
    {
        return toSimpleIssueList( indicatorService.getIndicatorsWithoutGroups().stream() );
    }

    /**
     * Gets all indicators with invalid indicator numerators.
     */
    List<DataIntegrityIssue> getInvalidIndicatorNumerators()
    {
        return getInvalidIndicators( Indicator::getNumerator );
    }

    /**
     * Gets all indicators with invalid indicator denominators.
     */
    List<DataIntegrityIssue> getInvalidIndicatorDenominators()
    {
        return getInvalidIndicators( Indicator::getDenominator );
    }

    private List<DataIntegrityIssue> getInvalidIndicators( Function<Indicator, String> getter )
    {
        List<DataIntegrityIssue> issues = new ArrayList<>();
        I18n i18n = i18nManager.getI18n();

        for ( Indicator indicator : indicatorService.getAllIndicators() )
        {
            ExpressionValidationOutcome result = expressionService.expressionIsValid( getter.apply( indicator ),
                INDICATOR_EXPRESSION );

            if ( !result.isValid() )
            {
                issues.add( toIssue( indicator, i18n.getString( result.getKey() ) ) );
            }
        }

        return issues;
    }

    /**
     * Gets all indicators units which are members of more than one group which
     * enter into an exclusive group set.
     */
    List<DataIntegrityIssue> getIndicatorsViolatingExclusiveGroupSets()
    {
        Collection<IndicatorGroupSet> groupSets = indicatorService.getAllIndicatorGroupSets();

        List<DataIntegrityIssue> issues = new ArrayList<>();

        for ( IndicatorGroupSet groupSet : groupSets )
        {
            Collection<Indicator> duplicates = getDuplicates(
                new ArrayList<>( groupSet.getIndicators() ) );

            for ( Indicator duplicate : duplicates )
            {
                issues.add( DataIntegrityIssue.toIssue( duplicate, duplicate.getGroups() ) );
            }
        }

        return issues;
    }

    // -------------------------------------------------------------------------
    // Period
    // -------------------------------------------------------------------------

    /**
     * Lists all Periods which are duplicates, based on the period type and
     * start date.
     */
    List<DataIntegrityIssue> getDuplicatePeriods()
    {
        List<Period> periods = periodService.getAllPeriods();

        List<DataIntegrityIssue> issues = new ArrayList<>();

        for ( Entry<String, List<Period>> group : periods.stream().collect(
            groupingBy( p -> p.getPeriodType().getName() + p.getStartDate().toString() ) ).entrySet() )
        {
            if ( group.getValue().size() > 1 )
            {
                issues.add( new DataIntegrityIssue( null, group.getKey(), null,
                    group.getValue().stream().map( p -> p.toString() + ":" + p.getUid() )
                        .collect( toUnmodifiableList() ) ) );
            }
        }
        return issues;
    }

    // -------------------------------------------------------------------------
    // OrganisationUnit
    // -------------------------------------------------------------------------

    List<DataIntegrityIssue> getOrganisationUnitsWithCyclicReferences()
    {
        return toSimpleIssueList( organisationUnitService.getOrganisationUnitsWithCyclicReferences().stream() );
    }

    List<DataIntegrityIssue> getOrphanedOrganisationUnits()
    {
        return toSimpleIssueList( organisationUnitService.getOrphanedOrganisationUnits().stream() );
    }

    List<DataIntegrityIssue> getOrganisationUnitsWithoutGroups()
    {
        return toSimpleIssueList( organisationUnitService.getOrganisationUnitsWithoutGroups().stream() );
    }

    List<DataIntegrityIssue> getOrganisationUnitsViolatingExclusiveGroupSets()
    {
        return toIssueList( organisationUnitService.getOrganisationUnitsViolatingExclusiveGroupSets().stream(),
            OrganisationUnit::getGroups );
    }

    List<DataIntegrityIssue> getOrganisationUnitGroupsWithoutGroupSets()
    {
        return toSimpleIssueList( organisationUnitGroupService.getOrganisationUnitGroupsWithoutGroupSets().stream() );
    }

    // -------------------------------------------------------------------------
    // ValidationRule
    // -------------------------------------------------------------------------

    List<DataIntegrityIssue> getValidationRulesWithoutGroups()
    {
        return toSimpleIssueList( validationRuleService.getValidationRulesWithoutGroups().stream() );
    }

    /**
     * Gets all ValidationRules with invalid left side expressions.
     */
    List<DataIntegrityIssue> getInvalidValidationRuleLeftSideExpressions()
    {
        return getInvalidValidationRuleExpressions( ValidationRule::getLeftSide );
    }

    /**
     * Gets all ValidationRules with invalid right side expressions.
     */
    List<DataIntegrityIssue> getInvalidValidationRuleRightSideExpressions()
    {
        return getInvalidValidationRuleExpressions( ValidationRule::getRightSide );
    }

    private List<DataIntegrityIssue> getInvalidValidationRuleExpressions(
        Function<ValidationRule, Expression> getter )
    {
        List<DataIntegrityIssue> issues = new ArrayList<>();
        I18n i18n = i18nManager.getI18n();

        for ( ValidationRule rule : validationRuleService.getAllValidationRules() )
        {
            ExpressionValidationOutcome result = expressionService
                .expressionIsValid( getter.apply( rule ).getExpression(), VALIDATION_RULE_EXPRESSION );

            if ( !result.isValid() )
            {
                issues.add( toIssue( rule, i18n.getString( result.getKey() ) ) );
            }
        }

        return issues;
    }

    private void registerNonDatabaseIntegrityCheck( DataIntegrityCheckType type,
        Class<? extends IdentifiableObject> issueIdType,
        Supplier<List<DataIntegrityIssue>> check )
    {
        String name = type.getName();
        I18n i18n = i18nManager.getI18n( DataIntegrityService.class );
        BinaryOperator<String> info = ( property, defaultValue ) -> i18n
            .getString( format( "data_integrity.%s.%s", name, property ), defaultValue );

        try
        {
            Schema issueSchema = issueIdType == null ? null : schemaService.getDynamicSchema( issueIdType );
            String issueIdTypeName = issueSchema == null ? null : issueSchema.getPlural();
            checksByName.put( name, DataIntegrityCheck.builder()
                .name( name )
                .displayName( info.apply( "name", name.replace( '_', ' ' ) ) )
                .severity( DataIntegritySeverity.valueOf(
                    info.apply( "severity", DataIntegritySeverity.WARNING.name() ).toUpperCase() ) )
                .section( info.apply( "section", "Other" ) )
                .description( info.apply( "description", null ) )
                .introduction( info.apply( "introduction", null ) )
                .recommendation( info.apply( "recommendation", null ) )
                .issuesIdType( issueIdTypeName )
                .runDetailsCheck( c -> new DataIntegrityDetails( c, new Date(), null, check.get() ) )
                .runSummaryCheck( c -> new DataIntegritySummary( c, new Date(), null, check.get().size(), null ) )
                .build() );
        }
        catch ( Exception ex )
        {
            log.error( "Failed to register data integrity check " + type, ex );
        }
    }

    /**
     * Maps all "hard coded" checks to their implementation method and registers
     * a {@link DataIntegrityCheck} to perform the method as
     * {@link DataIntegrityDetails}.
     */
    public void initIntegrityChecks()
    {
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.DATA_ELEMENTS_WITHOUT_DATA_SETS,
            DataElement.class, this::getDataElementsWithoutDataSet );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.DATA_ELEMENTS_WITHOUT_GROUPS,
            DataElement.class, this::getDataElementsWithoutGroups );
        registerNonDatabaseIntegrityCheck(
            DataIntegrityCheckType.DATA_ELEMENTS_ASSIGNED_TO_DATA_SETS_WITH_DIFFERENT_PERIOD_TYPES,
            DataElement.class, this::getDataElementsAssignedToDataSetsWithDifferentPeriodTypes );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.DATA_ELEMENTS_VIOLATING_EXCLUSIVE_GROUP_SETS,
            DataElement.class, this::getDataElementsViolatingExclusiveGroupSets );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.DATA_ELEMENTS_IN_DATA_SET_NOT_IN_FORM,
            DataSet.class, this::getDataElementsInDataSetNotInForm );

        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.CATEGORY_COMBOS_BEING_INVALID,
            CategoryCombo.class, this::getInvalidCategoryCombos );

        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.DATA_SETS_NOT_ASSIGNED_TO_ORG_UNITS,
            DataSet.class, this::getDataSetsNotAssignedToOrganisationUnits );

        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.INDICATORS_WITH_IDENTICAL_FORMULAS,
            null, this::getIndicatorsWithIdenticalFormulas );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.INDICATORS_WITHOUT_GROUPS,
            Indicator.class, this::getIndicatorsWithoutGroups );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.INDICATORS_WITH_INVALID_NUMERATOR,
            Indicator.class, this::getInvalidIndicatorNumerators );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.INDICATORS_WITH_INVALID_DENOMINATOR,
            Indicator.class, this::getInvalidIndicatorDenominators );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.INDICATORS_VIOLATING_EXCLUSIVE_GROUP_SETS,
            Indicator.class, this::getIndicatorsViolatingExclusiveGroupSets );

        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.PERIODS_DUPLICATES, null, this::getDuplicatePeriods );

        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.ORG_UNITS_WITH_CYCLIC_REFERENCES,
            OrganisationUnit.class, this::getOrganisationUnitsWithCyclicReferences );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.ORG_UNITS_BEING_ORPHANED,
            OrganisationUnit.class, this::getOrphanedOrganisationUnits );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.ORG_UNITS_WITHOUT_GROUPS,
            OrganisationUnit.class, this::getOrganisationUnitsWithoutGroups );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.ORG_UNITS_VIOLATING_EXCLUSIVE_GROUP_SETS,
            OrganisationUnit.class, this::getOrganisationUnitsViolatingExclusiveGroupSets );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.ORG_UNIT_GROUPS_WITHOUT_GROUP_SETS,
            OrganisationUnitGroup.class, this::getOrganisationUnitGroupsWithoutGroupSets );

        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.VALIDATION_RULES_WITHOUT_GROUPS,
            ValidationRule.class, this::getValidationRulesWithoutGroups );
        registerNonDatabaseIntegrityCheck(
            DataIntegrityCheckType.VALIDATION_RULES_WITH_INVALID_LEFT_SIDE_EXPRESSION,
            ValidationRule.class, this::getInvalidValidationRuleLeftSideExpressions );
        registerNonDatabaseIntegrityCheck(
            DataIntegrityCheckType.VALIDATION_RULES_WITH_INVALID_RIGHT_SIDE_EXPRESSION,
            ValidationRule.class, this::getInvalidValidationRuleRightSideExpressions );

        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.PROGRAM_INDICATORS_WITH_INVALID_EXPRESSIONS,
            ProgramIndicator.class, this::getInvalidProgramIndicatorExpressions );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.PROGRAM_INDICATORS_WITH_INVALID_FILTERS,
            ProgramIndicator.class, this::getInvalidProgramIndicatorFilters );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.PROGRAM_INDICATORS_WITHOUT_EXPRESSION,
            ProgramIndicator.class, this::getProgramIndicatorsWithNoExpression );

        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULES_WITHOUT_CONDITION,
            Program.class, this::getProgramRulesWithNoCondition );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULES_WITHOUT_PRIORITY,
            Program.class, this::getProgramRulesWithNoPriority );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULES_WITHOUT_ACTION,
            Program.class, this::getProgramRulesWithNoAction );

        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULE_VARIABLES_WITHOUT_DATA_ELEMENT,
            Program.class, this::getProgramRuleVariablesWithNoDataElement );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULE_VARIABLES_WITHOUT_ATTRIBUTE,
            Program.class, this::getProgramRuleVariablesWithNoAttribute );

        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULE_ACTIONS_WITHOUT_DATA_OBJECT,
            ProgramRule.class, this::getProgramRuleActionsWithNoDataObject );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULE_ACTIONS_WITHOUT_NOTIFICATION,
            ProgramRule.class, this::getProgramRuleActionsWithNoNotificationTemplate );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULE_ACTIONS_WITHOUT_SECTION,
            ProgramRule.class, this::getProgramRuleActionsWithNoSectionId );
        registerNonDatabaseIntegrityCheck( DataIntegrityCheckType.PROGRAM_RULE_ACTIONS_WITHOUT_STAGE,
            ProgramRule.class, this::getProgramRuleActionsWithNoProgramStageId );
    }

    @Override
    @Transactional( readOnly = true )
    public FlattenedDataIntegrityReport getReport( Set<String> checks, JobProgress progress )
    {
        if ( checks == null || checks.isEmpty() )
        {
            // report only needs these
            checks = Arrays.stream( DataIntegrityCheckType.values() )
                .map( DataIntegrityCheckType::getName )
                .collect( toUnmodifiableSet() );
        }
        runDetailsChecks( checks, progress );
        return new FlattenedDataIntegrityReport( getDetails( checks, -1L ) );
    }

    /**
     * Get all ProgramIndicators with no expression.
     */
    List<DataIntegrityIssue> getProgramIndicatorsWithNoExpression()
    {
        return toSimpleIssueList( programIndicatorService.getProgramIndicatorsWithNoExpression().stream() );
    }

    /**
     * Get all ProgramIndicators with invalid expressions.
     */
    List<DataIntegrityIssue> getInvalidProgramIndicatorExpressions()
    {
        return getInvalidProgramIndicators( ProgramIndicator::getExpression,
            pi -> !programIndicatorService.expressionIsValid( pi.getExpression() ) );
    }

    /**
     * Get all ProgramIndicators with invalid filters.
     */
    List<DataIntegrityIssue> getInvalidProgramIndicatorFilters()
    {
        return getInvalidProgramIndicators( ProgramIndicator::getFilter,
            pi -> !programIndicatorService.filterIsValid( pi.getFilter() ) );
    }

    private List<DataIntegrityIssue> getInvalidProgramIndicators( Function<ProgramIndicator, String> property,
        Predicate<ProgramIndicator> filter )
    {
        List<ProgramIndicator> programIndicators = programIndicatorService.getAllProgramIndicators()
            .stream()
            .filter( filter )
            .collect( toList() );

        List<DataIntegrityIssue> issues = new ArrayList<>();
        for ( ProgramIndicator programIndicator : programIndicators )
        {
            String description = getInvalidExpressionDescription( property.apply( programIndicator ) );
            if ( description != null )
            {
                issues.add( toIssue( programIndicator, description ) );
            }
        }
        return issues;
    }

    /**
     * Get all ProgramRules with no priority and grouped them by {@link Program}
     */
    List<DataIntegrityIssue> getProgramRulesWithNoPriority()
    {
        return groupRulesByProgram( programRuleService.getProgramRulesWithNoPriority() );
    }

    /**
     * Get all ProgramRules with no action and grouped them by {@link Program}
     */
    List<DataIntegrityIssue> getProgramRulesWithNoAction()
    {
        return groupRulesByProgram( programRuleService.getProgramRulesWithNoAction() );
    }

    /**
     * Get all ProgramRules with no condition expression and grouped them by
     * {@link Program}
     */
    List<DataIntegrityIssue> getProgramRulesWithNoCondition()
    {
        return groupRulesByProgram( programRuleService.getProgramRulesWithNoCondition() );
    }

    /**
     * @return all {@link ProgramRuleAction} which are not linked to any
     *         DataElement/TrackedEntityAttribute
     */
    List<DataIntegrityIssue> getProgramRuleActionsWithNoDataObject()
    {
        return groupActionsByProgramRule( programRuleActionService.getProgramActionsWithNoLinkToDataObject() );
    }

    /**
     * @return all {@link ProgramRuleAction} which are not linked to any
     *         {@link org.hisp.dhis.notification.NotificationTemplate}
     */
    List<DataIntegrityIssue> getProgramRuleActionsWithNoNotificationTemplate()
    {
        return groupActionsByProgramRule( programRuleActionService.getProgramActionsWithNoLinkToNotification() );
    }

    /**
     * @return all {@link ProgramRuleAction} which are not linked to any
     *         {@link org.hisp.dhis.program.ProgramStageSection}
     */
    List<DataIntegrityIssue> getProgramRuleActionsWithNoSectionId()
    {
        return groupActionsByProgramRule( programRuleActionService.getProgramRuleActionsWithNoSectionId() );
    }

    /**
     * @return all {@link ProgramRuleAction} which are not linked to any
     *         {@link org.hisp.dhis.program.ProgramStage}
     */
    List<DataIntegrityIssue> getProgramRuleActionsWithNoProgramStageId()
    {
        return groupActionsByProgramRule( programRuleActionService.getProgramRuleActionsWithNoStageId() );
    }

    /**
     * @return all {@link ProgramRuleVariable} which are not linked to any
     *         DataElement and grouped them by {@link Program}
     */
    List<DataIntegrityIssue> getProgramRuleVariablesWithNoDataElement()
    {
        return groupVariablesByProgram( programRuleVariableService.getVariablesWithNoDataElement() );
    }

    /**
     * @return all {@link ProgramRuleVariable} which are not linked to any
     *         TrackedEntityAttribute and grouped them by {@link Program}
     */
    List<DataIntegrityIssue> getProgramRuleVariablesWithNoAttribute()
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

    private static List<DataIntegrityIssue> groupRulesByProgram( List<ProgramRule> rules )
    {
        return groupBy( ProgramRule::getProgram, rules );
    }

    private static List<DataIntegrityIssue> groupVariablesByProgram(
        List<ProgramRuleVariable> variables )
    {
        return groupBy( ProgramRuleVariable::getProgram, variables );
    }

    private static List<DataIntegrityIssue> groupActionsByProgramRule(
        List<ProgramRuleAction> actions )
    {
        return groupBy( ProgramRuleAction::getProgramRule, actions );
    }

    private static <K extends IdentifiableObject, V extends IdentifiableObject> List<DataIntegrityIssue> groupBy(
        Function<V, K> property,
        Collection<V> values )
    {
        return values.stream().collect( groupingBy( property ) )
            .entrySet().stream()
            .map( e -> DataIntegrityIssue.toIssue( e.getKey(), e.getValue() ) )
            .collect( toUnmodifiableList() );
    }

    /*
     * Configuration based data integrity checks
     */

    private final Map<String, DataIntegrityCheck> checksByName = new ConcurrentHashMap<>();

    private final AtomicBoolean configurationsAreLoaded = new AtomicBoolean( false );

    @Override
    public Collection<DataIntegrityCheck> getDataIntegrityChecks( Set<String> checks )
    {
        ensureConfigurationsAreLoaded();
        return checks.isEmpty()
            ? unmodifiableCollection( checksByName.values() )
            : expandChecks( checks ).stream().map( checksByName::get ).collect( toList() );
    }

    @Override
    public Map<String, DataIntegritySummary> getSummaries( Set<String> checks, long timeout )
    {
        return getCached( checks, timeout, summaryCache );
    }

    // OBS! We intentionally do not open the transaction here to have each check
    // be independent
    @Override
    public void runSummaryChecks( Set<String> checks, JobProgress progress )
    {
        runDataIntegrityChecks( "Data Integrity summary checks", expandChecks( checks ), progress, summaryCache,
            check -> check.getRunSummaryCheck().apply( check ),
            ( check, ex ) -> new DataIntegritySummary( check, new Date(), ex.getMessage(), -1, null ) );
    }

    @Override
    public Map<String, DataIntegrityDetails> getDetails( Set<String> checks, long timeout )
    {
        return getCached( checks, timeout, detailsCache );
    }

    // OBS! We intentionally do not open the transaction here to have each check
    // be independent
    @Override
    public void runDetailsChecks( Set<String> checks, JobProgress progress )
    {
        runDataIntegrityChecks( "Data Integrity details checks", expandChecks( checks ), progress, detailsCache,
            check -> check.getRunDetailsCheck().apply( check ),
            ( check, ex ) -> new DataIntegrityDetails( check, new Date(), ex.getMessage(), List.of() ) );
    }

    private <T> Map<String, T> getCached( Set<String> checks, long timeout, Cache<T> cache )
    {
        Set<String> names = expandChecks( checks );
        long giveUpTime = currentTimeMillis() + timeout;
        Map<String, T> resByName = new LinkedHashMap<>();
        boolean retry = false;
        do
        {
            if ( retry )
            {
                try
                {
                    Thread.sleep( Math.max( 10, Math.min( 50, (giveUpTime - currentTimeMillis()) / 2 ) ) );
                }
                catch ( InterruptedException ex )
                {
                    Thread.currentThread().interrupt();
                    return resByName;
                }
            }
            for ( String name : names )
            {
                if ( !resByName.containsKey( name ) )
                {
                    cache.get( name ).ifPresent( res -> resByName.put( name, res ) );
                }
            }
            retry = resByName.size() < names.size() && (timeout < 0 || currentTimeMillis() < giveUpTime);
        }
        while ( retry );
        return resByName;
    }

    private <T> void runDataIntegrityChecks( String stageDesc, Set<String> checks, JobProgress progress,
        Cache<T> cache, Function<DataIntegrityCheck, T> runCheck,
        BiFunction<DataIntegrityCheck, RuntimeException, T> createErrorReport )
    {
        progress.startingProcess( "Data Integrity check" );
        progress.startingStage( stageDesc, checks.size(), SKIP_ITEM );
        progress.runStage( checks.stream().map( checksByName::get ).filter( Objects::nonNull ),
            DataIntegrityCheck::getDescription,
            check -> {
                T res = null;
                try
                {
                    res = runCheck.apply( check );
                }
                catch ( RuntimeException ex )
                {
                    cache.put( check.getName(), createErrorReport.apply( check, ex ) );
                    throw ex;
                }
                if ( res != null )
                {
                    cache.put( check.getName(), res );
                }
            } );
        progress.completedProcess( null );
    }

    private Set<String> expandChecks( Set<String> names )
    {
        ensureConfigurationsAreLoaded();
        if ( names == null || names.isEmpty() )
        {
            return unmodifiableSet( checksByName.keySet() );
        }
        Set<String> expanded = new LinkedHashSet<>();
        for ( String name : names )
        {
            String uniformName = name.toLowerCase().replace( '-', '_' );
            if ( uniformName.contains( "*" ) )
            {
                String pattern = uniformName.replace( "*", ".*" );
                for ( String existingName : checksByName.keySet() )
                {
                    if ( existingName.matches( pattern ) )
                    {
                        expanded.add( existingName );
                    }
                }
            }
            else
            {
                expanded.add( uniformName );
            }
        }
        return expanded;
    }

    private void ensureConfigurationsAreLoaded()
    {
        if ( configurationsAreLoaded.compareAndSet( false, true ) )
        {
            // programmatic checks
            initIntegrityChecks();

            // YAML based checks
            I18n i18n = i18nManager.getI18n( DataIntegrityService.class );
            readDataIntegrityYaml( "data-integrity-checks.yaml",
                check -> checksByName.put( check.getName(), check ),
                ( property, defaultValue ) -> i18n.getString( format( "data_integrity.%s", property ), defaultValue ),
                sql -> check -> dataIntegrityStore.querySummary( check, sql ),
                sql -> check -> dataIntegrityStore.queryDetails( check, sql ) );
        }
    }

}
