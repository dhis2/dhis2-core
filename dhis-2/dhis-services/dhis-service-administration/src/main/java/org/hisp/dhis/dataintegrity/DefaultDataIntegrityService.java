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

import static com.google.common.base.Preconditions.checkNotNull;
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
import java.util.function.Function;
import java.util.function.Predicate;

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
public class DefaultDataIntegrityService
    implements DataIntegrityService
{
    private static final String FORMULA_SEPARATOR = "#";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

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

    public DefaultDataIntegrityService( I18nManager i18nManager, DataElementService dataElementService,
        IndicatorService indicatorService, DataSetService dataSetService,
        OrganisationUnitService organisationUnitService, OrganisationUnitGroupService organisationUnitGroupService,
        ValidationRuleService validationRuleService, ExpressionService expressionService,
        DataEntryFormService dataEntryFormService, CategoryService categoryService, PeriodService periodService,
        ProgramIndicatorService programIndicatorService,
        ProgramRuleService programRuleService, ProgramRuleVariableService programRuleVariableService,
        ProgramRuleActionService programRuleActionService )
    {
        checkNotNull( i18nManager );
        checkNotNull( dataElementService );
        checkNotNull( indicatorService );
        checkNotNull( dataSetService );
        checkNotNull( organisationUnitService );
        checkNotNull( organisationUnitGroupService );
        checkNotNull( validationRuleService );
        checkNotNull( dataEntryFormService );
        checkNotNull( categoryService );
        checkNotNull( periodService );
        checkNotNull( programIndicatorService );
        checkNotNull( programRuleService );
        checkNotNull( programRuleVariableService );
        checkNotNull( programRuleActionService );

        this.i18nManager = i18nManager;
        this.dataElementService = dataElementService;
        this.indicatorService = indicatorService;
        this.dataSetService = dataSetService;
        this.organisationUnitService = organisationUnitService;
        this.organisationUnitGroupService = organisationUnitGroupService;
        this.validationRuleService = validationRuleService;
        this.expressionService = expressionService;
        this.dataEntryFormService = dataEntryFormService;
        this.categoryService = categoryService;
        this.periodService = periodService;
        this.programIndicatorService = programIndicatorService;
        this.programRuleService = programRuleService;
        this.programRuleVariableService = programRuleVariableService;
        this.programRuleActionService = programRuleActionService;
    }

    // -------------------------------------------------------------------------
    // DataIntegrityService implementation
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // DataElement
    // -------------------------------------------------------------------------

    @Override
    public List<DataElement> getDataElementsWithoutDataSet()
    {
        return dataElementService.getDataElementsWithoutDataSets();
    }

    @Override
    public List<DataElement> getDataElementsWithoutGroups()
    {
        return dataElementService.getDataElementsWithoutGroups();
    }

    @Override
    public SortedMap<DataElement, Collection<DataSet>> getDataElementsAssignedToDataSetsWithDifferentPeriodTypes()
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

    @Override
    public SortedMap<DataElement, Collection<DataElementGroup>> getDataElementsViolatingExclusiveGroupSets()
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

    @Override
    public SortedMap<DataSet, Collection<DataElement>> getDataElementsInDataSetNotInForm()
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

    @Override
    public List<CategoryCombo> getInvalidCategoryCombos()
    {
        List<CategoryCombo> categoryCombos = categoryService.getAllCategoryCombos();

        return categoryCombos.stream().filter( c -> !c.isValid() ).collect( toList() );
    }

    // -------------------------------------------------------------------------
    // DataSet
    // -------------------------------------------------------------------------

    @Override
    public List<DataSet> getDataSetsNotAssignedToOrganisationUnits()
    {
        return dataSetService.getDataSetsNotAssignedToOrganisationUnits();
    }

    // -------------------------------------------------------------------------
    // Indicator
    // -------------------------------------------------------------------------

    @Override
    public Set<Set<Indicator>> getIndicatorsWithIdenticalFormulas()
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

    @Override
    public List<Indicator> getIndicatorsWithoutGroups()
    {
        return indicatorService.getIndicatorsWithoutGroups();
    }

    @Override
    public SortedMap<Indicator, String> getInvalidIndicatorNumerators()
    {
        return getInvalidIndicators( Indicator::getNumerator );
    }

    @Override
    public SortedMap<Indicator, String> getInvalidIndicatorDenominators()
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

    @Override
    public SortedMap<Indicator, Collection<IndicatorGroup>> getIndicatorsViolatingExclusiveGroupSets()
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

    @Override
    public List<Period> getDuplicatePeriods()
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

    @Override
    public Set<OrganisationUnit> getOrganisationUnitsWithCyclicReferences()
    {
        return organisationUnitService.getOrganisationUnitsWithCyclicReferences();
    }

    @Override
    public List<OrganisationUnit> getOrphanedOrganisationUnits()
    {
        return organisationUnitService.getOrphanedOrganisationUnits();
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitsWithoutGroups()
    {
        return organisationUnitService.getOrganisationUnitsWithoutGroups();
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitsViolatingExclusiveGroupSets()
    {
        return organisationUnitService.getOrganisationUnitsViolatingExclusiveGroupSets();
    }

    @Override
    public List<OrganisationUnitGroup> getOrganisationUnitGroupsWithoutGroupSets()
    {
        return organisationUnitGroupService.getOrganisationUnitGroupsWithoutGroupSets();
    }

    // -------------------------------------------------------------------------
    // ValidationRule
    // -------------------------------------------------------------------------

    @Override
    public List<ValidationRule> getValidationRulesWithoutGroups()
    {
        return validationRuleService.getValidationRulesWithoutGroups();
    }

    @Override
    public SortedMap<ValidationRule, String> getInvalidValidationRuleLeftSideExpressions()
    {
        return getInvalidValidationRuleExpressions( ValidationRule::getLeftSide );
    }

    @Override
    public SortedMap<ValidationRule, String> getInvalidValidationRuleRightSideExpressions()
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

    @Override
    public DataIntegrityReport getDataIntegrityReport()
    {
        DataIntegrityReport report = new DataIntegrityReport();

        report.setDataElementsWithoutDataSet( new ArrayList<>( getDataElementsWithoutDataSet() ) );
        report.setDataElementsWithoutGroups( new ArrayList<>( getDataElementsWithoutGroups() ) );
        report.setDataElementsAssignedToDataSetsWithDifferentPeriodTypes(
            getDataElementsAssignedToDataSetsWithDifferentPeriodTypes() );
        report.setDataElementsViolatingExclusiveGroupSets( getDataElementsViolatingExclusiveGroupSets() );
        report.setDataElementsInDataSetNotInForm( getDataElementsInDataSetNotInForm() );
        report.setInvalidCategoryCombos( getInvalidCategoryCombos() );

        log.info( "Checked data elements" );

        report.setDataSetsNotAssignedToOrganisationUnits(
            new ArrayList<>( getDataSetsNotAssignedToOrganisationUnits() ) );

        log.info( "Checked data sets" );

        report.setIndicatorsWithIdenticalFormulas( getIndicatorsWithIdenticalFormulas() );
        report.setIndicatorsWithoutGroups( new ArrayList<>( getIndicatorsWithoutGroups() ) );
        report.setInvalidIndicatorNumerators( getInvalidIndicatorNumerators() );
        report.setInvalidIndicatorDenominators( getInvalidIndicatorDenominators() );
        report.setIndicatorsViolatingExclusiveGroupSets( getIndicatorsViolatingExclusiveGroupSets() );

        log.info( "Checked indicators" );

        report.setDuplicatePeriods( getDuplicatePeriods() );

        log.info( "Checked periods" );

        report
            .setOrganisationUnitsWithCyclicReferences( new ArrayList<>( getOrganisationUnitsWithCyclicReferences() ) );
        report.setOrphanedOrganisationUnits( new ArrayList<>( getOrphanedOrganisationUnits() ) );
        report.setOrganisationUnitsWithoutGroups( new ArrayList<>( getOrganisationUnitsWithoutGroups() ) );
        report.setOrganisationUnitsViolatingExclusiveGroupSets(
            groupsByUnit( getOrganisationUnitsViolatingExclusiveGroupSets() ) );
        report.setOrganisationUnitGroupsWithoutGroupSets(
            new ArrayList<>( getOrganisationUnitGroupsWithoutGroupSets() ) );
        report.setValidationRulesWithoutGroups( new ArrayList<>( getValidationRulesWithoutGroups() ) );

        log.info( "Checked organisation units" );

        report.setInvalidValidationRuleLeftSideExpressions( getInvalidValidationRuleLeftSideExpressions() );
        report.setInvalidValidationRuleRightSideExpressions( getInvalidValidationRuleRightSideExpressions() );

        log.info( "Checked validation rules" );

        report.setInvalidProgramIndicatorExpressions( getInvalidProgramIndicatorExpressions() );
        report.setInvalidProgramIndicatorFilters( getInvalidProgramIndicatorFilters() );
        report.setProgramIndicatorsWithNoExpression( getProgramIndicatorsWithNoExpression() );

        log.info( "Checked ProgramIndicators" );

        report.setProgramRulesWithoutCondition( getProgramRulesWithNoCondition() );
        report.setProgramRulesWithNoPriority( getProgramRulesWithNoPriority() );
        report.setProgramRulesWithNoAction( getProgramRulesWithNoAction() );

        log.info( "Checked ProgramRules" );

        report.setProgramRuleVariablesWithNoDataElement( getProgramRuleVariablesWithNoDataElement() );
        report.setProgramRuleVariablesWithNoAttribute( getProgramRuleVariablesWithNoAttribute() );

        log.info( "Checked ProgramRuleVariables" );

        report.setProgramRuleActionsWithNoDataObject( getProgramRuleActionsWithNoDataObject() );
        report.setProgramRuleActionsWithNoNotification( getProgramRuleActionsWithNoNotificationTemplate() );
        report.setProgramRuleActionsWithNoSectionId( getProgramRuleActionsWithNoSectionId() );
        report.setProgramRuleActionsWithNoStageId( getProgramRuleActionsWithNoProgramStageId() );

        log.info( "Checked ProgramRuleActions" );

        Collections.sort( report.getDataElementsWithoutDataSet() );
        Collections.sort( report.getDataElementsWithoutGroups() );
        Collections.sort( report.getDataSetsNotAssignedToOrganisationUnits() );
        Collections.sort( report.getIndicatorsWithoutGroups() );
        Collections.sort( report.getOrganisationUnitsWithCyclicReferences() );
        Collections.sort( report.getOrphanedOrganisationUnits() );
        Collections.sort( report.getOrganisationUnitsWithoutGroups() );
        Collections.sort( report.getOrganisationUnitGroupsWithoutGroupSets() );
        Collections.sort( report.getValidationRulesWithoutGroups() );

        return report;
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

    @Override
    public FlattenedDataIntegrityReport getFlattenedDataIntegrityReport()
    {
        return new FlattenedDataIntegrityReport( getDataIntegrityReport() );
    }

    @Override
    public List<ProgramIndicator> getProgramIndicatorsWithNoExpression()
    {
        return programIndicatorService.getProgramIndicatorsWithNoExpression();
    }

    @Override
    public Map<ProgramIndicator, String> getInvalidProgramIndicatorExpressions()
    {
        return getInvalidProgramIndicators( ProgramIndicator::getExpression,
            pi -> !programIndicatorService.expressionIsValid( pi.getExpression() ) );
    }

    @Override
    public Map<ProgramIndicator, String> getInvalidProgramIndicatorFilters()
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

    @Override
    public Map<Program, Collection<ProgramRule>> getProgramRulesWithNoPriority()
    {
        return groupRulesByProgram( programRuleService.getProgramRulesWithNoPriority() );
    }

    @Override
    public Map<Program, Collection<ProgramRule>> getProgramRulesWithNoAction()
    {
        return groupRulesByProgram( programRuleService.getProgramRulesWithNoAction() );
    }

    @Override
    public Map<Program, Collection<ProgramRule>> getProgramRulesWithNoCondition()
    {
        return groupRulesByProgram( programRuleService.getProgramRulesWithNoCondition() );
    }

    @Override
    public Map<ProgramRule, Collection<ProgramRuleAction>> getProgramRuleActionsWithNoDataObject()
    {
        return groupActionsByProgramRule( programRuleActionService.getProgramActionsWithNoLinkToDataObject() );
    }

    @Override
    public Map<ProgramRule, Collection<ProgramRuleAction>> getProgramRuleActionsWithNoNotificationTemplate()
    {
        return groupActionsByProgramRule( programRuleActionService.getProgramActionsWithNoLinkToNotification() );
    }

    @Override
    public Map<ProgramRule, Collection<ProgramRuleAction>> getProgramRuleActionsWithNoSectionId()
    {
        return groupActionsByProgramRule( programRuleActionService.getProgramRuleActionsWithNoSectionId() );
    }

    @Override
    public Map<ProgramRule, Collection<ProgramRuleAction>> getProgramRuleActionsWithNoProgramStageId()
    {
        return groupActionsByProgramRule( programRuleActionService.getProgramRuleActionsWithNoStageId() );
    }

    @Override
    public Map<Program, Collection<ProgramRuleVariable>> getProgramRuleVariablesWithNoDataElement()
    {
        return groupVariablesByProgram( programRuleVariableService.getVariablesWithNoDataElement() );
    }

    @Override
    public Map<Program, Collection<ProgramRuleVariable>> getProgramRuleVariablesWithNoAttribute()
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
