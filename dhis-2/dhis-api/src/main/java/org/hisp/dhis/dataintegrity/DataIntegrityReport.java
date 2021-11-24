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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import lombok.Getter;
import lombok.Setter;

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
@Getter
@Setter
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

    private List<ProgramIndicator> programIndicatorsWithNoExpression = new ArrayList<>();

    private Map<Program, Collection<ProgramRule>> programRulesWithoutCondition = new HashMap<>();

    private Map<Program, Collection<ProgramRule>> programRulesWithNoPriority = new HashMap<>();

    private Map<Program, Collection<ProgramRule>> programRulesWithNoAction = new HashMap<>();

    private Map<Program, Collection<ProgramRuleVariable>> programRuleVariablesWithNoDataElement = new HashMap<>();

    private Map<Program, Collection<ProgramRuleVariable>> programRuleVariablesWithNoAttribute = new HashMap<>();

    private Map<ProgramRule, Collection<ProgramRuleAction>> programRuleActionsWithNoDataObject = new HashMap<>();

    private Map<ProgramRule, Collection<ProgramRuleAction>> programRuleActionsWithNoNotification = new HashMap<>();

    private Map<ProgramRule, Collection<ProgramRuleAction>> programRuleActionsWithNoSectionId = new HashMap<>();

    private Map<ProgramRule, Collection<ProgramRuleAction>> programRuleActionsWithNoStageId = new HashMap<>();

}
