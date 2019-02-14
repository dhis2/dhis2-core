package org.hisp.dhis.dataintegrity;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.commons.collection.ListUtils.getDuplicates;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.ExpressionValidationOutcome;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.organisationunit.*;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
@Transactional
public class DefaultDataIntegrityService
    implements DataIntegrityService
{
    private static final Log log = LogFactory.getLog( DefaultDataIntegrityService.class );

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

    @Autowired
    public DefaultDataIntegrityService( I18nManager i18nManager, DataElementService dataElementService,
        IndicatorService indicatorService, DataSetService dataSetService,
        OrganisationUnitService organisationUnitService, OrganisationUnitGroupService organisationUnitGroupService,
        ValidationRuleService validationRuleService, ExpressionService expressionService,
        DataEntryFormService dataEntryFormService, CategoryService categoryService, PeriodService periodService,
        ProgramIndicatorService programIndicatorService, ProgramRuleService programRuleService, ProgramRuleVariableService programRuleVariableService,
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

                if ( dataSetElements.size() > 0 )
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
        
        return categoryCombos.stream().filter( c -> !c.isValid() ).collect( Collectors.toList() );
    }

    // -------------------------------------------------------------------------
    // DataSet
    // -------------------------------------------------------------------------

    @Override
    public List<DataSet> getDataSetsNotAssignedToOrganisationUnits()
    {
        Collection<DataSet> dataSets = dataSetService.getAllDataSets();

        return dataSets.stream().filter( ds -> ds.getSources() == null || ds.getSources().isEmpty() ).collect( Collectors.toList() );
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
        SortedMap<Indicator, String> invalids = new TreeMap<>();
        I18n i18n = i18nManager.getI18n();

        for ( Indicator indicator : indicatorService.getAllIndicators() )
        {
            ExpressionValidationOutcome result = expressionService.expressionIsValid( indicator.getNumerator() );

            if ( !result.isValid() )
            {
                invalids.put( indicator, i18n.getString(result.getKey()) );
            }
        }

        return invalids;
    }

    @Override
    public SortedMap<Indicator, String> getInvalidIndicatorDenominators()
    {
        SortedMap<Indicator, String> invalids = new TreeMap<>();
        I18n i18n = i18nManager.getI18n();

        for ( Indicator indicator : indicatorService.getAllIndicators() )
        {
            ExpressionValidationOutcome result = expressionService.expressionIsValid( indicator.getDenominator() );

            if ( !result.isValid() )
            {
                invalids.put( indicator, i18n.getString(result.getKey()) );
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

        for ( String key : map.keySet() )
        {
            List<Period> values = map.get( key );

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
        List<OrganisationUnit> organisationUnits = organisationUnitService.getAllOrganisationUnits();

        Set<OrganisationUnit> cyclic = new HashSet<>();

        Set<OrganisationUnit> visited = new HashSet<>();

        OrganisationUnit parent;

        for ( OrganisationUnit unit : organisationUnits )
        {
            parent = unit;

            while ( (parent = parent.getParent()) != null )
            {
                if ( parent.equals( unit ) ) // Cyclic reference
                {
                    cyclic.add( unit );

                    break;
                }
                else if ( visited.contains( parent ) ) // Ends in cyclic ref
                {
                    break;
                }
                else
                {
                    visited.add( parent ); // Remember visited
                }
            }

            visited.clear();
        }

        return cyclic;
    }

    @Override
    public List<OrganisationUnit> getOrphanedOrganisationUnits()
    {
        List<OrganisationUnit> units = organisationUnitService.getAllOrganisationUnits();
        
        return units.stream().filter( ou -> ou.getParent() == null && ( ou.getChildren() == null || ou.getChildren().size() == 0 ) ).collect( Collectors.toList() );
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitsWithoutGroups()
    {
        return organisationUnitService.getOrganisationUnitsWithoutGroups();
    }

    @Override
    public SortedMap<OrganisationUnit, Collection<OrganisationUnitGroup>> getOrganisationUnitsViolatingExclusiveGroupSets()
    {
        Collection<OrganisationUnitGroupSet> groupSets = organisationUnitGroupService.getAllOrganisationUnitGroupSets();

        TreeMap<OrganisationUnit, Collection<OrganisationUnitGroup>> targets =
            new TreeMap<>();

        for ( OrganisationUnitGroupSet groupSet : groupSets )
        {
            Collection<OrganisationUnit> duplicates = getDuplicates(
                new ArrayList<>( groupSet.getOrganisationUnits() ) );

            for ( OrganisationUnit duplicate : duplicates )
            {
                targets.put( duplicate, new HashSet<>( duplicate.getGroups() ) );
            }
        }

        return targets;
    }

    @Override
    public List<OrganisationUnitGroup> getOrganisationUnitGroupsWithoutGroupSets()
    {
        Collection<OrganisationUnitGroup> groups = organisationUnitGroupService.getAllOrganisationUnitGroups();
        
        return groups.stream().filter( g -> g == null || g.getGroupSets().isEmpty() ).collect( Collectors.toList() );
    }

    // -------------------------------------------------------------------------
    // ValidationRule
    // -------------------------------------------------------------------------

    @Override
    public List<ValidationRule> getValidationRulesWithoutGroups()
    {
        Collection<ValidationRule> validationRules = validationRuleService.getAllValidationRules();
        
        return validationRules.stream().filter( r -> r.getGroups() == null || r.getGroups().isEmpty() ).collect( Collectors.toList() );
    }

    @Override
    public SortedMap<ValidationRule, String> getInvalidValidationRuleLeftSideExpressions()
    {
        SortedMap<ValidationRule, String> invalids = new TreeMap<>();
        I18n i18n = i18nManager.getI18n();

        for ( ValidationRule rule : validationRuleService.getAllValidationRules() )
        {
            ExpressionValidationOutcome result = expressionService.expressionIsValid( rule.getLeftSide().getExpression() );

            if ( !result.isValid() )
            {
                invalids.put( rule, i18n.getString(result.getKey()) );
            }
        }

        return invalids;
    }

    @Override
    public SortedMap<ValidationRule, String> getInvalidValidationRuleRightSideExpressions()
    {
        SortedMap<ValidationRule, String> invalids = new TreeMap<>();
        I18n i18n = i18nManager.getI18n();

        for ( ValidationRule rule : validationRuleService.getAllValidationRules() )
        {
            ExpressionValidationOutcome result = expressionService.expressionIsValid( rule.getRightSide().getExpression() );

            if ( !result.isValid() )
            {
                invalids.put( rule, i18n.getString(result.getKey()) );
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
        report.setDataElementsAssignedToDataSetsWithDifferentPeriodTypes( getDataElementsAssignedToDataSetsWithDifferentPeriodTypes() );
        report.setDataElementsViolatingExclusiveGroupSets( getDataElementsViolatingExclusiveGroupSets() );
        report.setDataElementsInDataSetNotInForm( getDataElementsInDataSetNotInForm() );
        report.setInvalidCategoryCombos( getInvalidCategoryCombos() );

        log.info( "Checked data elements" );

        report.setDataSetsNotAssignedToOrganisationUnits( new ArrayList<>( getDataSetsNotAssignedToOrganisationUnits() ) );

        log.info( "Checked data sets" );

        report.setIndicatorsWithIdenticalFormulas( getIndicatorsWithIdenticalFormulas() );
        report.setIndicatorsWithoutGroups( new ArrayList<>( getIndicatorsWithoutGroups() ) );
        report.setInvalidIndicatorNumerators( getInvalidIndicatorNumerators() );
        report.setInvalidIndicatorDenominators( getInvalidIndicatorDenominators() );
        report.setIndicatorsViolatingExclusiveGroupSets( getIndicatorsViolatingExclusiveGroupSets() );

        log.info( "Checked indicators" );

        report.setDuplicatePeriods( getDuplicatePeriods() );

        log.info( "Checked periods" );

        report.setOrganisationUnitsWithCyclicReferences( new ArrayList<>( getOrganisationUnitsWithCyclicReferences() ) );
        report.setOrphanedOrganisationUnits( new ArrayList<>( getOrphanedOrganisationUnits() ) );
        report.setOrganisationUnitsWithoutGroups( new ArrayList<>( getOrganisationUnitsWithoutGroups() ) );
        report.setOrganisationUnitsViolatingExclusiveGroupSets( getOrganisationUnitsViolatingExclusiveGroupSets() );
        report.setOrganisationUnitGroupsWithoutGroupSets( new ArrayList<>( getOrganisationUnitGroupsWithoutGroupSets() ) );
        report.setValidationRulesWithoutGroups( new ArrayList<>( getValidationRulesWithoutGroups() ) );

        log.info( "Checked organisation units" );

        report.setInvalidValidationRuleLeftSideExpressions( getInvalidValidationRuleLeftSideExpressions() );
        report.setInvalidValidationRuleRightSideExpressions( getInvalidValidationRuleRightSideExpressions() );

        log.info( "Checked validation rules" );

        report.setInvalidProgramIndicatorExpressions( getInvalidProgramIndicatorExpressions() );
        report.setInvalidProgramIndicatorFilters( getInvalidProgramIndicatorFilters() );

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

    @Override
    public FlattenedDataIntegrityReport getFlattenedDataIntegrityReport()
    {
        return new FlattenedDataIntegrityReport( getDataIntegrityReport() );
    }

    @Override
    public Map<ProgramIndicator, String> getInvalidProgramIndicatorExpressions()
    {
        Map<ProgramIndicator, String> invalidExpressions;

        invalidExpressions = programIndicatorService.getAllProgramIndicators().stream()
            .filter( pi -> ! ProgramIndicator.VALID.equals( programIndicatorService.expressionIsValid( pi.getExpression() ) ) )
            .collect( Collectors.toMap( pi -> pi, ProgramIndicator::getExpression ) );

        return invalidExpressions;
    }

    @Override
    public Map<ProgramIndicator, String> getInvalidProgramIndicatorFilters()
    {
        Map<ProgramIndicator, String> invalidFilters;

        invalidFilters = programIndicatorService.getAllProgramIndicators().stream()
            .filter( pi -> ( ! ( pi.hasFilter() ? ProgramIndicator.VALID.equals( programIndicatorService.filterIsValid( pi.getFilter() ) ) : true ) ) )
            .collect( Collectors.toMap( pi -> pi, ProgramIndicator::getFilter ) );

        return invalidFilters;
    }

    @Override
    public Map<Program, Collection<ProgramRule>> getProgramRulesWithNoPriority()
    {
        List<ProgramRule> programRules = programRuleService.getProgramRulesWithNoPriority();

        return groupRulesByProgram( programRules );
    }

    @Override
    public Map<Program, Collection<ProgramRule>> getProgramRulesWithNoAction()
    {
        List<ProgramRule> programRules = programRuleService.getProgramRulesWithNoAction();

        return groupRulesByProgram( programRules );
    }

    @Override
    public Map<Program, Collection<ProgramRule>> getProgramRulesWithNoCondition()
    {
        List<ProgramRule> programRules = programRuleService.getProgramRulesWithNoCondition();

        return groupRulesByProgram( programRules );
    }

    @Override
    public Map<ProgramRule, Collection<ProgramRuleAction>> getProgramRuleActionsWithNoDataObject()
    {
        List<ProgramRuleAction> ruleActions = programRuleActionService.getProgramActionsWithNoLinkToDataObject();

        return groupActionsByProgramRule( ruleActions );
    }

    @Override
    public Map<ProgramRule, Collection<ProgramRuleAction>> getProgramRuleActionsWithNoNotificationTemplate()
    {
        List<ProgramRuleAction> ruleActions = programRuleActionService.getProgramActionsWithNoLinkToNotification();

        return groupActionsByProgramRule( ruleActions );
    }

    @Override
    public Map<ProgramRule, Collection<ProgramRuleAction>> getProgramRuleActionsWithNoSectionId()
    {
        List<ProgramRuleAction> ruleActions = programRuleActionService.getProgramRuleActionsWithNoSectionId();

        return groupActionsByProgramRule( ruleActions );
    }

    @Override
    public Map<ProgramRule, Collection<ProgramRuleAction>> getProgramRuleActionsWithNoProgramStageId()
    {
        List<ProgramRuleAction> ruleActions = programRuleActionService.getProgramRuleActionsWithNoStageId();

        return groupActionsByProgramRule( ruleActions );    }

    @Override
    public  Map<Program, Collection<ProgramRuleVariable>> getProgramRuleVariablesWithNoDataElement()
    {
        List<ProgramRuleVariable> ruleVariables = programRuleVariableService.getVariablesWithNoDataElement();

        return groupVariablesByProgram( ruleVariables );
    }

    @Override
    public Map<Program, Collection<ProgramRuleVariable>> getProgramRuleVariablesWithNoAttribute()
    {
        List<ProgramRuleVariable> ruleVariables = programRuleVariableService.getVariablesWithNoAttribute();

        return groupVariablesByProgram( ruleVariables );
    }

    private Map<Program, Collection<ProgramRule>> groupRulesByProgram( List<ProgramRule> programRules )
    {
        Map<Program, Collection<ProgramRule>> collectionMap = new HashMap<>();

        for ( ProgramRule rule : programRules )
        {
            Program program = rule.getProgram();

            if ( !collectionMap.containsKey( program ) )
            {
                collectionMap.put( program, Sets.newHashSet() );
            }

            collectionMap.get( program ).add( rule );
        }

        return collectionMap;
    }

    private  Map<Program, Collection<ProgramRuleVariable>> groupVariablesByProgram( List<ProgramRuleVariable> ruleVariables )
    {
        Map<Program, Collection<ProgramRuleVariable>> collectionMap = new HashMap<>();

        for ( ProgramRuleVariable variable : ruleVariables )
        {
            Program program = variable.getProgram();

            if ( !collectionMap.containsKey( program ) )
            {
                collectionMap.put( program, Sets.newHashSet() );
            }

            collectionMap.get( program ).add( variable );
        }

        return collectionMap;
    }

    private  Map<ProgramRule, Collection<ProgramRuleAction>> groupActionsByProgramRule( List<ProgramRuleAction> ruleActions )
    {
        Map<ProgramRule, Collection<ProgramRuleAction>> collectionMap = new HashMap<>();

        for ( ProgramRuleAction action : ruleActions )
        {
            ProgramRule programRule = action.getProgramRule();

            if ( !collectionMap.containsKey( programRule ) )
            {
                collectionMap.put( programRule, Sets.newHashSet() );
            }

            collectionMap.get( programRule ).add( action );
        }

        return collectionMap;
    }
}
