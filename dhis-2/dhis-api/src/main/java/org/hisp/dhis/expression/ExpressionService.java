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
package org.hisp.dhis.expression;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.Period;

/**
 * Expressions are mathematical formulas and can contain references to various
 * elements.
 *
 * @author Margrethe Store
 * @author Lars Helge Overland
 * @author Jim Grace
 */
public interface ExpressionService
{
    String ID = ExpressionService.class.getName();

    String DAYS_DESCRIPTION = "[Number of days]";

    String SYMBOL_DAYS = "[days]";

    String SYMBOL_WILDCARD = "*";

    String UID_EXPRESSION = "[a-zA-Z]\\w{10}";

    String INT_EXPRESSION = "^(0|-?[1-9]\\d*)$";

    // -------------------------------------------------------------------------
    // Expression CRUD operations
    // -------------------------------------------------------------------------

    /**
     * Adds a new Expression to the database.
     *
     * @param expression The Expression to add.
     * @return The generated identifier for this Expression.
     */
    long addExpression( Expression expression );

    /**
     * Updates an Expression.
     *
     * @param expression The Expression to update.
     */
    void updateExpression( Expression expression );

    /**
     * Deletes an Expression from the database.
     *
     * @param expression the expression.
     */
    void deleteExpression( Expression expression );

    /**
     * Get the Expression with the given identifier.
     *
     * @param id The identifier.
     * @return an Expression with the given identifier.
     */
    Expression getExpression( long id );

    /**
     * Gets all Expressions.
     *
     * @return A list with all Expressions.
     */
    List<Expression> getAllExpressions();

    // -------------------------------------------------------------------------
    // Indicator expression logic
    // -------------------------------------------------------------------------

    /**
     * Returns all dimensional item objects which are present in numerator and
     * denominator of the given indicators, as a map from id to object.
     *
     * @param indicators the collection of indicators.
     * @return a map from dimensional item id to object.
     */
    Map<DimensionalItemId, DimensionalItemObject> getIndicatorDimensionalItemMap( Collection<Indicator> indicators );

    /**
     * Returns all OrganisationUnitGroups in the numerator and denominator
     * expressions in the given Indicators. Returns an empty set if the given
     * indicators are null or empty.
     *
     * @param indicators the set of indicators.
     * @return a Set of OrganisationUnitGroups.
     */
    Set<OrganisationUnitGroup> getIndicatorOrgUnitGroups( Collection<Indicator> indicators );

    /**
     * Generates the calculated value for the given parameters based on the
     * values in the given maps.
     *
     * @param indicator the indicator for which to calculate the value.
     * @param periods a List of periods for which to calculate the value.
     * @param itemMap map of dimensional item id to object in expression.
     * @param valueMap the map of data values.
     * @param constantMap the map of constants.
     * @param orgUnitCountMap the map of organisation unit group member counts.
     * @return the calculated value as a double.
     */
    IndicatorValue getIndicatorValueObject( Indicator indicator, List<Period> periods,
        Map<DimensionalItemId, DimensionalItemObject> itemMap, Map<DimensionalItemObject, Object> valueMap,
        Map<String, Constant> constantMap, Map<String, Integer> orgUnitCountMap );

    /**
     * Substitutes any constants and org unit group member counts in the
     * numerator and denominator on all indicators in the given collection.
     *
     * @param indicators the set of indicators.
     */
    void substituteIndicatorExpressions( Collection<Indicator> indicators );

    // -------------------------------------------------------------------------
    // Get information about the expression
    // -------------------------------------------------------------------------

    /**
     * Tests whether the expression is valid.
     *
     * @param expression the expression string.
     * @param parseType the type of expression to parse.
     * @return the ExpressionValidationOutcome of the validation.
     */
    ExpressionValidationOutcome expressionIsValid( String expression, ParseType parseType );

    /**
     * Creates an expression description containing the names of the
     * DimensionalItemObjects from a numeric valued expression.
     *
     * @param expression the expression string.
     * @param parseType the type of expression to parse.
     * @return An description containing DimensionalItemObjects names.
     */
    String getExpressionDescription( String expression, ParseType parseType );

    /**
     * Creates an expression description containing the names of the
     * DimensionalItemObjects from an expression string, for an expression that
     * will return the specified data type.
     *
     * @param expression the expression string.
     * @param parseType the type of expression to parse.
     * @param dataType the data type for the expression to return.
     * @return An description containing DimensionalItemObjects names.
     */
    String getExpressionDescription( String expression, ParseType parseType, DataType dataType );

    /**
     * Returns UIDs of Data Elements and associated Option Combos (if any) found
     * in the Data Element Operands an expression.
     * <p/>
     * If the Data Element Operand consists of just a Data Element, or if the
     * Option Combo is a wildcard "*", returns just dataElementUID.
     * <p/>
     * If an Option Combo is present, returns dataElementUID.optionComboUID.
     *
     * @param expression the expression string.
     * @param parseType the type of expression to parse.
     * @return a Set of data element identifiers.
     */
    Set<String> getExpressionElementAndOptionComboIds( String expression, ParseType parseType );

    /**
     * Returns all data elements found in the given expression string, including
     * those found in data element operands. Returns an empty set if the given
     * expression is null.
     *
     * @param expression the expression string.
     * @param parseType the type of expression to parse.
     * @return a Set of data elements included in the expression string.
     */
    Set<DataElement> getExpressionDataElements( String expression, ParseType parseType );

    /**
     * Returns all CategoryOptionCombo uids in the given expression string that
     * are used as a data element operand categoryOptionCombo or
     * attributeOptionCombo. Returns an empty set if the expression is null.
     *
     * @param expression the expression string.
     * @param parseType the type of expression to parse.
     * @return a Set of CategoryOptionCombo uids in the expression string.
     */
    Set<String> getExpressionOptionComboIds( String expression, ParseType parseType );

    /**
     * Returns all dimensional item objects in the given expression, returning
     * separately the items to be sampled inside any vector functions. Items are
     * returned as a map from itemId to object.
     *
     * @param expression the expression string.
     * @param parseType the type of expression to parse.
     * @param dataType the data type the expression should return.
     * @param itemMap map to insert the items into.
     * @param sampleItemMap map to insert the sampled items into.
     */
    void getExpressionDimensionalItemMaps( String expression, ParseType parseType, DataType dataType,
        Map<DimensionalItemId, DimensionalItemObject> itemMap,
        Map<DimensionalItemId, DimensionalItemObject> sampleItemMap );

    /**
     * Returns all dimensional item object ids in the given expression.
     *
     * @param expression the expression string.
     * @param parseType the type of expression to parse.
     * @return a Set of dimensional item object ids.
     */
    Set<DimensionalItemId> getExpressionDimensionalItemIds( String expression, ParseType parseType );

    /**
     * Returns all OrganisationUnitGroups in the given expression.
     *
     * @param expression the expression string.
     * @param parseType the type of expression to parse.
     * @return a Set of OrganisationUnitGroups in the expression string.
     */
    Set<OrganisationUnitGroup> getExpressionOrgUnitGroups( String expression, ParseType parseType );

    /**
     * Returns set of all OrganisationUnitGroup UIDs in the given expression.
     *
     * @param expression the expression string.
     * @param parseType the type of expression to parse.
     * @return Map of UIDs to OrganisationUnitGroups in the expression string.
     */
    Set<String> getExpressionOrgUnitGroupIds( String expression, ParseType parseType );

    // -------------------------------------------------------------------------
    // Compute the value of the expression
    // -------------------------------------------------------------------------

    /**
     * Generates the calculated value for an expression.
     *
     * @param expression the expression string.
     * @param parseType the type of expression to parse.
     * @return the calculated value.
     */
    Object getExpressionValue( String expression, ParseType parseType );

    /**
     * Generates the calculated numeric value for an expression.
     *
     * @param expression the expression string.
     * @param parseType the type of expression to parse.
     * @param itemMap map of dimensional item objects to value in expression.
     * @param valueMap the dimensional item object values to use for
     *        calculation.
     * @param constantMap map of constants to use for calculation.
     * @param orgUnitCountMap the map of organisation unit group member counts.
     * @param orgUnitGroupMap the map of organisation unit groups.
     * @param days the number of days to use in the calculation.
     * @param missingValueStrategy the strategy to use when data values are
     *        missing when calculating the expression.
     * @param orgUnit the current organisation unit.
     * @return the calculated value as a double.
     */
    Double getExpressionValue( String expression, ParseType parseType,
        Map<DimensionalItemId, DimensionalItemObject> itemMap, Map<DimensionalItemObject, Object> valueMap,
        Map<String, Constant> constantMap, Map<String, Integer> orgUnitCountMap,
        Map<String, OrganisationUnitGroup> orgUnitGroupMap, Integer days,
        MissingValueStrategy missingValueStrategy, OrganisationUnit orgUnit );

    /**
     * Generates the calculated value for an expression.
     *
     * @param expression the expression string.
     * @param parseType the type of expression to parse.
     * @param itemMap map of dimensional item objects to value in expression.
     * @param valueMap the dimensional item object values to use for
     *        calculation.
     * @param constantMap map of constants to use for calculation.
     * @param orgUnitCountMap the map of organisation unit group member counts.
     * @param orgUnitGroupMap the map of organisation unit groups.
     * @param days the number of days to use in the calculation.
     * @param missingValueStrategy the strategy to use when data values are
     *        missing when calculating the expression.
     * @param orgUnit the current organisation unit.
     * @param samplePeriods periods for samples to aggregate.
     * @param periodValueMap values for aggregate functions by period.
     * @param dataType the data type the expression should return.
     * @return the calculated value.
     */
    Object getExpressionValue( String expression, ParseType parseType,
        Map<DimensionalItemId, DimensionalItemObject> itemMap, Map<DimensionalItemObject, Object> valueMap,
        Map<String, Constant> constantMap, Map<String, Integer> orgUnitCountMap,
        Map<String, OrganisationUnitGroup> orgUnitGroupMap, Integer days,
        MissingValueStrategy missingValueStrategy, OrganisationUnit orgUnit, List<Period> samplePeriods,
        MapMap<Period, DimensionalItemObject, Object> periodValueMap, DataType dataType );
}
