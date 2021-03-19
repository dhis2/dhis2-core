package org.hisp.dhis.webapi.json.domain;

import org.hisp.dhis.webapi.json.JsonArray;
import org.hisp.dhis.webapi.json.JsonList;
import org.hisp.dhis.webapi.json.JsonMap;
import org.hisp.dhis.webapi.json.JsonMultiMap;
import org.hisp.dhis.webapi.json.JsonObject;
import org.hisp.dhis.webapi.json.JsonString;

/**
 * JSON equivalent of the
 * {@link org.hisp.dhis.dataintegrity.FlattenedDataIntegrityReport}.
 *
 * @author Jan Bernitt
 */
public interface JsonDataIntegrityReport extends JsonObject
{

    default JsonList<JsonString> getDataElementsWithoutDataSet()
    {
        return getList( "dataElementsWithoutDataSet", JsonString.class );
    }

    default JsonList<JsonString> getDataElementsWithoutGroups()
    {
        return getList( "dataElementsWithoutGroups", JsonString.class );
    }

    default JsonMultiMap<JsonString> getDataElementsAssignedToDataSetsWithDifferentPeriodTypes()
    {
        return getMultiMap( "dataElementsAssignedToDataSetsWithDifferentPeriodTypes", JsonString.class );
    }

    default JsonMultiMap<JsonString> getDataElementsViolatingExclusiveGroupSets()
    {
        return getMultiMap( "dataElementsViolatingExclusiveGroupSets", JsonString.class );
    }

    default JsonMultiMap<JsonString> getDataElementsInDataSetNotInForm()
    {
        return getMultiMap( "dataElementsInDataSetNotInForm", JsonString.class );
    }

    default JsonList<JsonString> getInvalidCategoryCombos()
    {
        return getList( "invalidCategoryCombos", JsonString.class );
    }

    default JsonList<JsonString> getDataSetsNotAssignedToOrganisationUnits()
    {
        return getList( "dataSetsNotAssignedToOrganisationUnits", JsonString.class );
    }

    default JsonList<JsonArray> getIndicatorsWithIdenticalFormulas()
    {
        return getList( "indicatorsWithIdenticalFormulas", JsonArray.class );
    }

    default JsonList<JsonString> getIndicatorsWithoutGroups()
    {
        return getList( "indicatorsWithoutGroups", JsonString.class );
    }

    default JsonMap<JsonString> getInvalidIndicatorNumerators()
    {
        return getMap( "invalidIndicatorNumerators", JsonString.class );
    }

    default JsonMap<JsonString> getInvalidIndicatorDenominators()
    {
        return getMap( "invalidIndicatorDenominators", JsonString.class );
    }

    default JsonMultiMap<JsonString> getIndicatorsViolatingExclusiveGroupSets()
    {
        return getMultiMap( "indicatorsViolatingExclusiveGroupSets", JsonString.class );
    }

    default JsonList<JsonString> getDuplicatePeriods()
    {
        return getList( "duplicatePeriods", JsonString.class );
    }

    default JsonList<JsonString> getOrganisationUnitsWithCyclicReferences()
    {
        return getList( "organisationUnitsWithCyclicReferences", JsonString.class );
    }

    default JsonList<JsonString> getOrphanedOrganisationUnits()
    {
        return getList( "orphanedOrganisationUnits", JsonString.class );
    }

    default JsonList<JsonString> getOrganisationUnitsWithoutGroups()
    {
        return getList( "organisationUnitsWithoutGroups", JsonString.class );
    }

    default JsonMultiMap<JsonString> getOrganisationUnitsViolatingExclusiveGroupSets()
    {
        return getMultiMap( "organisationUnitsViolatingExclusiveGroupSets", JsonString.class );
    }

    default JsonList<JsonString> getOrganisationUnitGroupsWithoutGroupSets()
    {
        return getList( "organisationUnitGroupsWithoutGroupSets", JsonString.class );
    }

    default JsonList<JsonString> getValidationRulesWithoutGroups()
    {
        return getList( "validationRulesWithoutGroups", JsonString.class );
    }

    default JsonMap<JsonString> getInvalidValidationRuleLeftSideExpressions()
    {
        return getMap( "invalidValidationRuleLeftSideExpressions", JsonString.class );
    }

    default JsonMap<JsonString> getInvalidValidationRuleRightSideExpressions()
    {
        return getMap( "invalidValidationRuleRightSideExpressions", JsonString.class );
    }

    default JsonMap<JsonString> getInvalidProgramIndicatorExpressions()
    {
        return getMap( "invalidProgramIndicatorExpressions", JsonString.class );
    }

    default JsonList<JsonString> getProgramIndicatorsWithNoExpression()
    {
        return getList( "programIndicatorsWithNoExpression", JsonString.class );
    }

    default JsonMap<JsonString> getInvalidProgramIndicatorFilters()
    {
        return getMap( "invalidProgramIndicatorFilters", JsonString.class );
    }

    default JsonMultiMap<JsonString> getProgramRulesWithNoCondition()
    {
        return getMultiMap( "programRulesWithNoCondition", JsonString.class );
    }

    default JsonMultiMap<JsonString> getProgramRulesWithNoPriority()
    {
        return getMultiMap( "programRulesWithNoPriority", JsonString.class );
    }

    default JsonMultiMap<JsonString> getProgramRulesWithNoAction()
    {
        return getMultiMap( "programRulesWithNoAction", JsonString.class );
    }

    default JsonMultiMap<JsonString> getProgramRuleVariablesWithNoDataElement()
    {
        return getMultiMap( "programRuleVariablesWithNoDataElement", JsonString.class );
    }

    default JsonMultiMap<JsonString> getProgramRuleVariablesWithNoAttribute()
    {
        return getMultiMap( "programRuleVariablesWithNoAttribute", JsonString.class );
    }

    default JsonMultiMap<JsonString> getProgramRuleActionsWithNoDataObject()
    {
        return getMultiMap( "programRuleActionsWithNoDataObject", JsonString.class );
    }

    default JsonMultiMap<JsonString> getProgramRuleActionsWithNoNotification()
    {
        return getMultiMap( "programRuleActionsWithNoNotification", JsonString.class );
    }

    default JsonMultiMap<JsonString> getProgramRuleActionsWithNoSectionId()
    {
        return getMultiMap( "programRuleActionsWithNoSectionId", JsonString.class );
    }

    default JsonMultiMap<JsonString> getProgramRuleActionsWithNoStageId()
    {
        return getMultiMap( "programRuleActionsWithNoStageId", JsonString.class );
    }
}
