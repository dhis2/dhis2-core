package org.hisp.dhis.expression;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorValue;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.validation.ValidationRule;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Expressions are mathematical formulas and can contain references to various
 * elements.
 * <p>
 * <ul>
 * <li>Data element operands on the form #{dataelementuid.categoryoptioncombouid}</li>
 * <li>Data element totals on the form #{dataelementuid}</li>
 * <li>Program data elements on the form D{programuid.dataelementuid}</li>
 * <li>Program tracked entity attribute on the form A{programuid.attributeuid}</li>
 * <li>Program indicators on the form I{programindicatoruid}</li>
 * <li>Constants on the form C{constantuid}</li>
 * <li>Organisation unit group member counts on the form OUG{orgunitgroupuid}</li>
 * <li>Days in aggregation period as the symbol [days]</li>
 * </ul>
 *
 * @author Margrethe Store
 * @author Lars Helge Overland
 */
public interface ExpressionService
{
    String ID = ExpressionService.class.getName();

    String DAYS_DESCRIPTION = "[Number of days]";
    String NULL_REPLACEMENT = "0";
    String SPACE = " ";
    String DAYS_SYMBOL = "[days]";

    String VARIABLE_EXPRESSION = "(#|D|A|I)\\{(([a-zA-Z]\\w{10})\\.?(\\w*))\\}";
    String OPERAND_EXPRESSION = "#\\{([a-zA-Z]\\w{10})\\.?(\\w*)\\}";
    String PROGRAM_DATA_ELEMENT_EXPRESSION = "D\\{([a-zA-Z]\\w{10})\\.?([a-zA-Z]\\w{10})\\}";
    String OPERAND_UID_EXPRESSION = "([a-zA-Z]\\w{10})\\.?(\\w*)";
    String DATA_ELEMENT_TOTAL_EXPRESSION = "#\\{([a-zA-Z]\\w{10})\\}";
    String OPTION_COMBO_OPERAND_EXPRESSION = "#\\{([a-zA-Z]\\w{10})\\.([a-zA-Z]\\w{10})\\}";
    String CONSTANT_EXPRESSION = "C\\{([a-zA-Z]\\w{10})\\}";
    String OU_GROUP_EXPRESSION = "OUG\\{([a-zA-Z]\\w{10})\\}";
    String DAYS_EXPRESSION = "\\[days\\]";

    Pattern VARIABLE_PATTERN = Pattern.compile( VARIABLE_EXPRESSION );
    Pattern OPERAND_PATTERN = Pattern.compile( OPERAND_EXPRESSION );
    Pattern OPERAND_UID_PATTERN = Pattern.compile( OPERAND_UID_EXPRESSION );
    Pattern PROGRAM_DATA_ELEMENT_PATTERN = Pattern.compile( PROGRAM_DATA_ELEMENT_EXPRESSION );
    Pattern DATA_ELEMENT_TOTAL_PATTERN = Pattern.compile( DATA_ELEMENT_TOTAL_EXPRESSION );
    Pattern OPTION_COMBO_OPERAND_PATTERN = Pattern.compile( OPTION_COMBO_OPERAND_EXPRESSION );
    Pattern CONSTANT_PATTERN = Pattern.compile( CONSTANT_EXPRESSION );
    Pattern OU_GROUP_PATTERN = Pattern.compile( OU_GROUP_EXPRESSION );
    Pattern DAYS_PATTERN = Pattern.compile( DAYS_EXPRESSION );

    /**
     * Adds a new Expression to the database.
     *
     * @param expression The Expression to add.
     * @return The generated identifier for this Expression.
     */
    int addExpression( Expression expression );

    /**
     * Updates an Expression.
     *
     * @param expression The Expression to update.
     */
    void updateExpression( Expression expression );

    /**
     * Deletes an Expression from the database.
     *
     * @param id Identifier of the Expression to delete.
     */
    void deleteExpression( Expression expression );

    /**
     * Get the Expression with the given identifier.
     *
     * @param id The identifier.
     * @return an Expression with the given identifier.
     */
    Expression getExpression( int id );

    /**
     * Gets all Expressions.
     *
     * @return A list with all Expressions.
     */
    List<Expression> getAllExpressions();

    /**
     * Generates the calculated value for the given parameters based on the values
     * in the given maps.
     *
     * @param indicator       the indicator for which to calculate the value.
     * @param period          the period for which to calculate the value.
     * @param valueMap        the map of data values.
     * @param constantMap     the map of constants.
     * @param orgUnitCountMap the map of organisation unit counts.
     * @return the calculated value as a double.
     */
    Double getIndicatorValue( Indicator indicator, Period period, Map<? extends DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap );

    /**
     * Generates the calculated value for the given parameters based on the values
     * in the given maps.
     *
     * @param indicator       the indicator for which to calculate the value.
     * @param period          the period for which to calculate the value.
     * @param valueMap        the map of data values.
     * @param constantMap     the map of constants.
     * @param orgUnitCountMap the map of organisation unit counts.
     * @return the calculated value as a double.
     */
    IndicatorValue getIndicatorValueObject( Indicator indicator, Period period,
        Map<? extends DimensionalItemObject, Double> valueMap, Map<String, Double> constantMap,
        Map<String, Integer> orgUnitCountMap );

    /**
     * Generates the calculated value for the given expression base on the values
     * supplied in the value map, constant map and days.
     *
     * @param expression      the expression which holds the formula for the calculation.
     * @param valueMap        the mapping between data element operands and values to
     *                        use in the calculation.
     * @param constantMap     the mapping between the constant uid and value to use
     *                        in the calculation.
     * @param orgUnitCountMap the mapping between organisation unit group uid and
     *                        count of organisation units to use in the calculation.
     * @param days            the number of days to use in the calculation.
     * @return the calculated value as a double.
     */
    Double getExpressionValue( Expression expression, Map<? extends DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap, Integer days );

    /**
     * Generates the calculated value for the given expression base on the values
     * supplied in the value map, constant map and days.
     *
     * @param expression      the expression which holds the formula for the calculation.
     * @param valueMap        the mapping between data element operands and values to
     *                        use in the calculation.
     * @param constantMap     the mapping between the constant uid and value to use
     *                        in the calculation.
     * @param orgUnitCountMap the mapping between organisation unit group uid and
     *                        count of organisation units to use in the calculation.
     * @param days            the number of days to use in the calculation.
     * @param set             of data element operands that have values but they are incomplete
     *                        (for example due to aggregation from organisationUnit children where
     *                        not all children had a value.)
     * @return the calculated value as a double.
     */
    Double getExpressionValue( Expression expression, Map<? extends DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap, Integer days,
        Set<DataElementOperand> incompleteValues );

    /**
     * Generates the calculated value for the given expression base on the values
     * supplied in the value map, constant map and days.
     *
     * @param expression      the expression which holds the formula for the calculation.
     * @param valueMap        the mapping between data element operands and values to
     *                        use in the calculation.
     * @param constantMap     the mapping between the constant uid and value to use
     *                        in the calculation.
     * @param orgUnitCountMap the mapping between organisation unit group uid and
     *                        count of organisation units to use in the calculation.
     * @param days            the number of days to use in the calculation.
     * @param set             of data element operands that have values but they are incomplete
     *                        (for example due to aggregation from organisationUnit children where
     *                        not all children had a value.)
     * @param a               map of subexpression strings to List(s) of aggregated samples for the expression
     * @return the calculated value as a double.
     */
    Double getExpressionValue( Expression expression, Map<? extends DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap, Integer days,
        Set<DataElementOperand> incompleteValues, ListMap<String, Double> aggregateMap );

    /**
     * Generates the calculated value for the given expression base on the values
     * supplied in the value map, constant map and days.
     *
     * @param expression       the expression which holds the formula for the calculation.
     * @param valueMap         the mapping between data element operands and values to
     *                         use in the calculation.
     * @param constantMap      the mapping between the constant uid and value to use
     *                         in the calculation.
     * @param orgUnitCountMap  the mapping between organisation unit group uid and
     *                         count of organisation units to use in the calculation.
     * @param days             the number of days to use in the calculation.
     * @param incompleteValues of data element operands that have values but they are incomplete
     *                         (for example due to aggregation from organisationUnit children where
     *                         not all children had a value.)
     * @param aggregateMap     map of subexpression strings to List(s) of aggregated samples for the expression
     * @return the calculated object result
     */
    public Object getExpressionObjectValue( Expression expression, Map<? extends DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap, Integer days,
        Set<DataElementOperand> incompleteValues, ListMap<String, Double> aggregateMap );

    /**
     * Returns all data elements included in the given expression string. Returns
     * an empty set if the given expression is null.
     *
     * @param expression the expression string.
     * @return a set of data elements included in the expression string.
     */
    Set<DataElement> getDataElementsInExpression( String expression );

    /**
     * Returns all CategoryOptionCombos in the given expression string. Only
     * operands with a category option combo will be included. Returns an empty
     * set if the given expression is null.
     *
     * @param expression the expression string.
     * @return a Set of CategoryOptionCombos included in the expression string.
     */
    Set<DataElementCategoryOptionCombo> getOptionCombosInExpression( String expression );

    /**
     * Returns all operands included in an expression string. The operand is on
     * the form #{data-element-id.category-option combo-id}. Only operands with
     * a category option combo will be included. Requires that the expression
     * has been exploded in order to handle data element totals. Returns an
     * empty set if the given expression is null.
     *
     * @param expression The expression string.
     * @return A Set of Operands.
     */
    Set<DataElementOperand> getOperandsInExpression( String expression );

    /**
     * Returns all data elements and data element operands included in an expression string.
     * This includes all of the operands, as in getOperandsInExpression as well as
     * data elements without any modifiers, bundled as a DataElementOperand object.
     *
     * @param expression The expression string.
     * @return A Set of Operands.
     */
    public Set<BaseDimensionalItemObject> getDataInputsInExpression( String expression );

    /**
     * Returns all aggregates included in an expression string. An aggregate has the form
     * #[expr]  where expr is a well-formed sub-expression.  This returns the
     * empty set if the given expression is null or there are no aggregates.
     *
     * @param expression The expression string.
     * @return A Set of Expression strings.
     */
    Set<String> getAggregatesInExpression( String expression );

    /**
     * Returns all data elements included in aggregates from the given
     * expression string. Returns an empty set if the given expression
     * is null.
     *
     * @param expression the expression string.
     * @return a set of data elements included in aggregate  expressions
     * within the expression string.
     */
    Set<DataElement> getSampleElementsInExpression( String expression );

    /**
     * Returns all data elements which are present in the numerator and denominator
     * of the given indicators.
     *
     * @param indicators the collection of indicators.
     * @return a set of data elements.
     */
    Set<DataElement> getDataElementsInIndicators( Collection<Indicator> indicators );

    /**
     * Returns all data elements which are present in the numerator and denominator
     * of the given indicators which represent totals.
     *
     * @param indicators the collection of indicators.
     * @return a set of data elements.
     */
    Set<DataElement> getDataElementTotalsInIndicators( Collection<Indicator> indicators );

    /**
     * Returns all data elements which are present in the numerator and denominator
     * of the given indicators which include category option combinations.
     *
     * @param indicators the collection of indicators.
     * @return a set of data elements.
     */
    Set<DataElement> getDataElementWithOptionCombosInIndicators( Collection<Indicator> indicators );

    /**
     * Returns all dimensional item objects which are present in the given expression.
     *
     * @param expression the expression.
     * @return a set of dimensional item objects.
     */
    Set<DimensionalItemObject> getDimensionalItemObjectsInExpression( String expression );

    /**
     * Returns all dimensional item objects which are present in numerator and
     * denominator of the given indicators.
     *
     * @param indicators the collection of indicators.
     * @return a set of dimensional item objects.
     */
    Set<DimensionalItemObject> getDimensionalItemObjectsInIndicators( Collection<Indicator> indicators );

    /**
     * Returns all OrganisationUnitGroups in the given expression string. Returns
     * an set list if the given indicators are null or empty.
     *
     * @param expression the expression string.
     * @return a Set of OrganisationUnitGroups included in the expression string.
     */
    Set<OrganisationUnitGroup> getOrganisationUnitGroupsInExpression( String expression );

    /**
     * Returns all OrganisationUnitGroups in the numerator and denominator
     * expressions in the given Indicators. Returns an empty set if the given
     * indicators are null or empty.
     *
     * @param indicators the set of indicators.
     * @return a Set of OrganisationUnitGroups.
     */
    Set<OrganisationUnitGroup> getOrganisationUnitGroupsInIndicators( Collection<Indicator> indicators );

    /**
     * Filters indicators from the given collection where the numerator and /
     * or the denominator are invalid.
     *
     * @param indicators collection of Indicators.
     */
    void filterInvalidIndicators( List<Indicator> indicators );

    /**
     * Tests whether the expression is valid. Returns a positive value if the
     * expression is valid, or a negative value if not.
     *
     * @param formula the expression formula.
     * @return the ExpressionValidationOutcome of the validation.
     */
    ExpressionValidationOutcome expressionIsValid( String formula );

    /**
     * Creates an expression string containing DataElement names and the names of
     * the CategoryOptions in the CategoryOptionCombo from a string consisting
     * of identifiers.
     *
     * @param expression The expression string.
     * @return An expression string containing DataElement names and the names of
     * the CategoryOptions in the CategoryOptionCombo.
     * @throws IllegalArgumentException if data element id or category option combo
     *                                  id are not numeric or data element or category option combo do not exist.
     */
    String getExpressionDescription( String expression );

    /**
     * Populates the explodedExpression property on the Expression object of all
     * validation rules in the given collection. This method uses
     * explodeExpression( String ) internally to generate the exploded expressions.
     * Replaces references to data element totals with references to all
     * category option combos in the category combo for that data element.
     *
     * @param validationRules the collection of validation rules.
     */
    void explodeValidationRuleExpressions( Collection<ValidationRule> validationRules );

    /**
     * Replaces references to data element totals with references to all
     * category option combos in the category combo for that data element.
     *
     * @param expression the expression to explode.
     * @return the exploded expression string.
     */
    String explodeExpression( String expression );

    /**
     * Substitutes potential constant and days in the numerator and denominator
     * on all indicators in the given collection.
     */
    void substituteExpressions( Collection<Indicator> indicators, Integer days );

    /**
     * Generates an expression where the Operand identifiers, consisting of
     * data element id and category option combo id, are replaced
     * by the aggregated value for the relevant combination of data element,
     * period, and source.
     *
     * @param formula              formula to parse.
     * @param valueMap             the mapping between data element operands and values to
     *                             use in the calculation.
     * @param constantMap          the mapping between the constant uid and value to use
     *                             in the calculation.
     * @param orgUnitCountMap      the mapping between organisation unit group uid and
     *                             count of organisation units to use in the calculation.
     * @param days                 the number of days to use in the calculation.
     * @param missingValueStrategy the strategy to use when data values are missing
     *                             when calculating the expression. Strategy defaults to NEVER_SKIP if null.
     */
    String generateExpression( String expression, Map<? extends DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap, Integer days,
        MissingValueStrategy missingValueStrategy );

    /**
     * Returns all Operands included in the formulas for the given collection of
     * Indicators. Requires that the explodedNumerator and explodedDenominator
     * properties have been populated in order to handle totals.
     *
     * @param indicators the collection of Indicators.
     */
    List<DataElementOperand> getOperandsInIndicators( List<Indicator> indicators );
}
