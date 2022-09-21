/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.validation.ValidationRule;

/**
 * @author Halvdan Hoem Grelland <halvdanhg@gmail.com>
 */
public class DataIntegrityReport
{
    private List<DataElement> dataElementsWithoutDataSet = new ArrayList<>();

    private List<DataElement> dataElementsWithoutGroups = new ArrayList<>();

    private Map<DataElement, Collection<DataSet>> dataElementsAssignedToDataSetsWithDifferentPeriodTypes = new HashMap<>();

    private SortedMap<DataElement, Collection<DataElementGroup>> dataElementsViolatingExclusiveGroupSets = new TreeMap<>();

    private SortedMap<DataSet, Collection<DataElement>> dataElementsInDataSetNotInForm = new TreeMap<>();

    private List<CategoryCombo> invalidCategoryCombos = new ArrayList<>();

    private List<DataSet> dataSetsNotAssignedToOrganisationUnits = new ArrayList<>();

    private Set<Set<Indicator>> indicatorsWithIdenticalFormulas = new HashSet<>();

    private List<Indicator> indicatorsWithoutGroups = new ArrayList<>();

    private Map<Indicator, String> invalidIndicatorNumerators = new HashMap<>();

    private Map<Indicator, String> invalidIndicatorDenominators = new HashMap<>();

    private SortedMap<Indicator, Collection<IndicatorGroup>> indicatorsViolatingExclusiveGroupSets = new TreeMap<>();

    private List<Period> duplicatePeriods = new ArrayList<>();

    private List<OrganisationUnit> organisationUnitsWithCyclicReferences = new ArrayList<>();

    private List<OrganisationUnit> orphanedOrganisationUnits = new ArrayList<>();

    private List<OrganisationUnit> organisationUnitsWithoutGroups = new ArrayList<>();

    private SortedMap<OrganisationUnit, Collection<OrganisationUnitGroup>> organisationUnitsViolatingExclusiveGroupSets = new TreeMap<>();

    private List<OrganisationUnitGroup> organisationUnitGroupsWithoutGroupSets = new ArrayList<>();

    private List<ValidationRule> validationRulesWithoutGroups = new ArrayList<>();

    private Map<ValidationRule, String> invalidValidationRuleLeftSideExpressions = new HashMap<>();

    private Map<ValidationRule, String> invalidValidationRuleRightSideExpressions = new HashMap<>();

    private Map<ProgramIndicator, String> invalidProgramIndicatorExpressions = new HashMap<>();

    private Map<ProgramIndicator, String> invalidProgramIndicatorFilters = new HashMap<>();

    private List<ProgramIndicator> getProgramIndicatorWithNoExpression = new ArrayList<>();

    private Map<Program, Collection<ProgramRule>> programRulesWithoutCondition = new HashMap<>();

    private Map<Program, Collection<ProgramRule>> programRulesWithNoPriority = new HashMap<>();

    private Map<Program, Collection<ProgramRule>> programRulesWithNoAction = new HashMap<>();

    private Map<Program, Collection<ProgramRuleVariable>> programRuleVariablesWithNoDataElement = new HashMap<>();

    private Map<Program, Collection<ProgramRuleVariable>> programRuleVariablesWithNoAttribute = new HashMap<>();

    private Map<ProgramRule, Collection<ProgramRuleAction>> programRuleActionsWithNoDataObject = new HashMap<>();

    private Map<ProgramRule, Collection<ProgramRuleAction>> programRuleActionsWithNoNotification = new HashMap<>();

    private Map<ProgramRule, Collection<ProgramRuleAction>> programRuleActionsWithNoSectionId = new HashMap<>();

    private Map<ProgramRule, Collection<ProgramRuleAction>> programRuleActionsWithNoStageId = new HashMap<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataIntegrityReport()
    {

    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public List<DataElement> getDataElementsWithoutDataSet()
    {
        return dataElementsWithoutDataSet;
    }

    public void setDataElementsWithoutDataSet( List<DataElement> dataElementsWithoutDataSet )
    {
        this.dataElementsWithoutDataSet = dataElementsWithoutDataSet;
    }

    public List<DataElement> getDataElementsWithoutGroups()
    {
        return dataElementsWithoutGroups;
    }

    public void setDataElementsWithoutGroups( List<DataElement> dataElementsWithoutGroups )
    {
        this.dataElementsWithoutGroups = dataElementsWithoutGroups;
    }

    public Map<DataElement, Collection<DataSet>> getDataElementsAssignedToDataSetsWithDifferentPeriodTypes()
    {
        return dataElementsAssignedToDataSetsWithDifferentPeriodTypes;
    }

    public void setDataElementsAssignedToDataSetsWithDifferentPeriodTypes(
        Map<DataElement, Collection<DataSet>> dataElementsAssignedToDataSetsWithDifferentPeriodTypes )
    {
        this.dataElementsAssignedToDataSetsWithDifferentPeriodTypes = dataElementsAssignedToDataSetsWithDifferentPeriodTypes;
    }

    public SortedMap<DataElement, Collection<DataElementGroup>> getDataElementsViolatingExclusiveGroupSets()
    {
        return dataElementsViolatingExclusiveGroupSets;
    }

    public void setDataElementsViolatingExclusiveGroupSets(
        SortedMap<DataElement, Collection<DataElementGroup>> dataElementsViolatingExclusiveGroupSets )
    {
        this.dataElementsViolatingExclusiveGroupSets = dataElementsViolatingExclusiveGroupSets;
    }

    public SortedMap<DataSet, Collection<DataElement>> getDataElementsInDataSetNotInForm()
    {
        return dataElementsInDataSetNotInForm;
    }

    public void setDataElementsInDataSetNotInForm(
        SortedMap<DataSet, Collection<DataElement>> dataElementsInDataSetNotInForm )
    {
        this.dataElementsInDataSetNotInForm = dataElementsInDataSetNotInForm;
    }

    public List<CategoryCombo> getInvalidCategoryCombos()
    {
        return invalidCategoryCombos;
    }

    public void setInvalidCategoryCombos( List<CategoryCombo> invalidCategoryCombos )
    {
        this.invalidCategoryCombos = invalidCategoryCombos;
    }

    public List<DataSet> getDataSetsNotAssignedToOrganisationUnits()
    {
        return dataSetsNotAssignedToOrganisationUnits;
    }

    public void setDataSetsNotAssignedToOrganisationUnits( List<DataSet> dataSetsNotAssignedToOrganisationUnits )
    {
        this.dataSetsNotAssignedToOrganisationUnits = dataSetsNotAssignedToOrganisationUnits;
    }

    public Set<Set<Indicator>> getIndicatorsWithIdenticalFormulas()
    {
        return indicatorsWithIdenticalFormulas;
    }

    public void setIndicatorsWithIdenticalFormulas( Set<Set<Indicator>> indicatorsWithIdenticalFormulas )
    {
        this.indicatorsWithIdenticalFormulas = indicatorsWithIdenticalFormulas;
    }

    public List<Indicator> getIndicatorsWithoutGroups()
    {
        return indicatorsWithoutGroups;
    }

    public void setIndicatorsWithoutGroups( List<Indicator> indicatorsWithoutGroups )
    {
        this.indicatorsWithoutGroups = indicatorsWithoutGroups;
    }

    public Map<Indicator, String> getInvalidIndicatorNumerators()
    {
        return invalidIndicatorNumerators;
    }

    public void setInvalidIndicatorNumerators( Map<Indicator, String> invalidIndicatorNumerators )
    {
        this.invalidIndicatorNumerators = invalidIndicatorNumerators;
    }

    public Map<Indicator, String> getInvalidIndicatorDenominators()
    {
        return invalidIndicatorDenominators;
    }

    public void setInvalidIndicatorDenominators( Map<Indicator, String> invalidIndicatorDenominators )
    {
        this.invalidIndicatorDenominators = invalidIndicatorDenominators;
    }

    public SortedMap<Indicator, Collection<IndicatorGroup>> getIndicatorsViolatingExclusiveGroupSets()
    {
        return indicatorsViolatingExclusiveGroupSets;
    }

    public void setIndicatorsViolatingExclusiveGroupSets(
        SortedMap<Indicator, Collection<IndicatorGroup>> indicatorsViolatingExclusiveGroupSets )
    {
        this.indicatorsViolatingExclusiveGroupSets = indicatorsViolatingExclusiveGroupSets;
    }

    public List<Period> getDuplicatePeriods()
    {
        return duplicatePeriods;
    }

    public void setDuplicatePeriods( List<Period> duplicatePeriods )
    {
        this.duplicatePeriods = duplicatePeriods;
    }

    public List<OrganisationUnit> getOrganisationUnitsWithCyclicReferences()
    {
        return organisationUnitsWithCyclicReferences;
    }

    public void setOrganisationUnitsWithCyclicReferences( List<OrganisationUnit> organisationUnitsWithCyclicReferences )
    {
        this.organisationUnitsWithCyclicReferences = organisationUnitsWithCyclicReferences;
    }

    public List<OrganisationUnit> getOrphanedOrganisationUnits()
    {
        return orphanedOrganisationUnits;
    }

    public void setOrphanedOrganisationUnits( List<OrganisationUnit> orphanedOrganisationUnits )
    {
        this.orphanedOrganisationUnits = orphanedOrganisationUnits;
    }

    public List<OrganisationUnit> getOrganisationUnitsWithoutGroups()
    {
        return organisationUnitsWithoutGroups;
    }

    public void setOrganisationUnitsWithoutGroups( List<OrganisationUnit> organisationUnitsWithoutGroups )
    {
        this.organisationUnitsWithoutGroups = organisationUnitsWithoutGroups;
    }

    public SortedMap<OrganisationUnit, Collection<OrganisationUnitGroup>> getOrganisationUnitsViolatingExclusiveGroupSets()
    {
        return organisationUnitsViolatingExclusiveGroupSets;
    }

    public void setOrganisationUnitsViolatingExclusiveGroupSets(
        SortedMap<OrganisationUnit, Collection<OrganisationUnitGroup>> organisationUnitsViolatingExclusiveGroupSets )
    {
        this.organisationUnitsViolatingExclusiveGroupSets = organisationUnitsViolatingExclusiveGroupSets;
    }

    public List<OrganisationUnitGroup> getOrganisationUnitGroupsWithoutGroupSets()
    {
        return organisationUnitGroupsWithoutGroupSets;
    }

    public void setOrganisationUnitGroupsWithoutGroupSets(
        List<OrganisationUnitGroup> organisationUnitGroupsWithoutGroupSets )
    {
        this.organisationUnitGroupsWithoutGroupSets = organisationUnitGroupsWithoutGroupSets;
    }

    public List<ValidationRule> getValidationRulesWithoutGroups()
    {
        return validationRulesWithoutGroups;
    }

    public void setValidationRulesWithoutGroups( List<ValidationRule> validationRulesWithoutGroups )
    {
        this.validationRulesWithoutGroups = validationRulesWithoutGroups;
    }

    public Map<ValidationRule, String> getInvalidValidationRuleLeftSideExpressions()
    {
        return invalidValidationRuleLeftSideExpressions;
    }

    public void setInvalidValidationRuleLeftSideExpressions(
        Map<ValidationRule, String> invalidValidationRuleLeftSideExpressions )
    {
        this.invalidValidationRuleLeftSideExpressions = invalidValidationRuleLeftSideExpressions;
    }

    public Map<ValidationRule, String> getInvalidValidationRuleRightSideExpressions()
    {
        return invalidValidationRuleRightSideExpressions;
    }

    public void setInvalidValidationRuleRightSideExpressions(
        Map<ValidationRule, String> invalidValidationRuleRightSideExpressions )
    {
        this.invalidValidationRuleRightSideExpressions = invalidValidationRuleRightSideExpressions;
    }

    public Map<ProgramIndicator, String> getInvalidProgramIndicatorFilters()
    {
        return invalidProgramIndicatorFilters;
    }

    public void setInvalidProgramIndicatorFilters( Map<ProgramIndicator, String> invalidProgramIndicatorFilters )
    {
        this.invalidProgramIndicatorFilters = invalidProgramIndicatorFilters;
    }

    public List<ProgramIndicator> getGetProgramIndicatorWithNoExpression()
    {
        return getProgramIndicatorWithNoExpression;
    }

    public void setGetProgramIndicatorWithNoExpression( List<ProgramIndicator> getProgramIndicatorWithNoExpression )
    {
        this.getProgramIndicatorWithNoExpression = getProgramIndicatorWithNoExpression;
    }

    public Map<ProgramIndicator, String> getInvalidProgramIndicatorExpressions()
    {
        return invalidProgramIndicatorExpressions;
    }

    public void setInvalidProgramIndicatorExpressions(
        Map<ProgramIndicator, String> invalidProgramIndicatorExpressions )
    {
        this.invalidProgramIndicatorExpressions = invalidProgramIndicatorExpressions;
    }

    public Map<Program, Collection<ProgramRule>> getProgramRulesWithoutCondition()
    {
        return programRulesWithoutCondition;
    }

    public void setProgramRulesWithoutCondition( Map<Program, Collection<ProgramRule>> programRulesWithoutCondition )
    {
        this.programRulesWithoutCondition = programRulesWithoutCondition;
    }

    public Map<Program, Collection<ProgramRule>> getProgramRulesWithNoPriority()
    {
        return programRulesWithNoPriority;
    }

    public void setProgramRulesWithNoPriority( Map<Program, Collection<ProgramRule>> programRulesWithNoPriority )
    {
        this.programRulesWithNoPriority = programRulesWithNoPriority;
    }

    public Map<Program, Collection<ProgramRule>> getProgramRulesWithNoAction()
    {
        return programRulesWithNoAction;
    }

    public void setProgramRulesWithNoAction( Map<Program, Collection<ProgramRule>> programRulesWithNoAction )
    {
        this.programRulesWithNoAction = programRulesWithNoAction;
    }

    public Map<Program, Collection<ProgramRuleVariable>> getProgramRuleVariablesWithNoDataElement()
    {
        return programRuleVariablesWithNoDataElement;
    }

    public void setProgramRuleVariablesWithNoDataElement(
        Map<Program, Collection<ProgramRuleVariable>> programRuleVariablesWithNoDataElement )
    {
        this.programRuleVariablesWithNoDataElement = programRuleVariablesWithNoDataElement;
    }

    public Map<Program, Collection<ProgramRuleVariable>> getProgramRuleVariablesWithNoAttribute()
    {
        return programRuleVariablesWithNoAttribute;
    }

    public void setProgramRuleVariablesWithNoAttribute(
        Map<Program, Collection<ProgramRuleVariable>> programRuleVariablesWithNoAttribute )
    {
        this.programRuleVariablesWithNoAttribute = programRuleVariablesWithNoAttribute;
    }

    public Map<ProgramRule, Collection<ProgramRuleAction>> getProgramRuleActionsWithNoDataObject()
    {
        return programRuleActionsWithNoDataObject;
    }

    public void setProgramRuleActionsWithNoDataObject(
        Map<ProgramRule, Collection<ProgramRuleAction>> programRuleActionsWithNoDataObject )
    {
        this.programRuleActionsWithNoDataObject = programRuleActionsWithNoDataObject;
    }

    public Map<ProgramRule, Collection<ProgramRuleAction>> getProgramRuleActionsWithNoNotification()
    {
        return programRuleActionsWithNoNotification;
    }

    public void setProgramRuleActionsWithNoNotification(
        Map<ProgramRule, Collection<ProgramRuleAction>> programRuleActionsWithNoNotification )
    {
        this.programRuleActionsWithNoNotification = programRuleActionsWithNoNotification;
    }

    public Map<ProgramRule, Collection<ProgramRuleAction>> getProgramRuleActionsWithNoSectionId()
    {
        return programRuleActionsWithNoSectionId;
    }

    public void setProgramRuleActionsWithNoSectionId(
        Map<ProgramRule, Collection<ProgramRuleAction>> programRuleActionsWithNoSectionId )
    {
        this.programRuleActionsWithNoSectionId = programRuleActionsWithNoSectionId;
    }

    public Map<ProgramRule, Collection<ProgramRuleAction>> getProgramRuleActionsWithNoStageId()
    {
        return programRuleActionsWithNoStageId;
    }

    public void setProgramRuleActionsWithNoStageId(
        Map<ProgramRule, Collection<ProgramRuleAction>> programRuleActionsWithNoStageId )
    {
        this.programRuleActionsWithNoStageId = programRuleActionsWithNoStageId;
    }
}
