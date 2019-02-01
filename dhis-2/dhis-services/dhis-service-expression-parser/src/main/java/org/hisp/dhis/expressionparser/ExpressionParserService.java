package org.hisp.dhis.expressionparser;

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

import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.expression.MissingValueStrategy;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorValue;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.Period;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Parses expressions.
 *
 * @author Jim Grace
 */
public interface ExpressionParserService
{
    // -------------------------------------------------------------------------
    // Expression methods
    // -------------------------------------------------------------------------

    /**
     * Creates an expression string containing the names of the
     * DimensionalItemObjects from an expression string.
     *
     * @param expression The expression string.
     * @return An expression string containing DimensionalItemObjects names.
     * @throws IllegalArgumentException if data element id or category option
     *         combo id are not numeric or data element or category option combo
     *         do not exist.
     */
    String getExpressionDescription( String expression );

    /**
     * Returns all dimensional item objects in the given expression.
     *
     * @param expression the expression to parse
     * @return a set of dimensional item objects.
     */
    Set<DimensionalItemObject> getExpressionDimensionalItemObjects( String expression );

    /**
     * Returns all dimensional item object ids in the given expression.
     *
     * @param expression the expression to parse
     * @return a set of dimensional item object ids.
     */
     Set<DimensionalItemId> getExpressionDimensionalItemIds( String expression );

    /**
     * Returns all OrganisationUnitGroups in the given expression.
     *
     * @param expression the expression string.
     * @return a Set of OrganisationUnitGroups included in the expression
     *         string.
     */
    Set<OrganisationUnitGroup> getExpressionOrgUnitGroups( String expression );

    /**
     * Generates the calculated value for an expression bases on the values
     * supplied in the value map, constant map, orgUnit counts, and days.
     *
     * @param expression the expression holding the formula for calculation.
     * @param valueMap the DimensionalItemObject values to use for calculation.
     * @param constantMap map of constants to use for calculation.
     * @param orgUnitCountMap the mapping between organisation unit group uid
     *        and count of organisation units to use in the calculation.
     * @param days the number of days to use in the calculation.
     * @param missingValueStrategy the strategy to use when data values are
     *        missing when calculating the expression.
     * @return the calculated value as a double.
     */
    Double getExpressionValue( String expression,
        Map<DimensionalItemObject, Double> valueMap, Map<String, Double> constantMap,
        Map<String, Integer> orgUnitCountMap, Integer days,
        MissingValueStrategy missingValueStrategy );

    // -------------------------------------------------------------------------
    // Indicator expression methods
    // -------------------------------------------------------------------------

    /**
     * Returns all dimensional item objects which are present in numerator and
     * denominator of the given indicators.
     *
     * @param indicators the collection of indicators.
     * @return a set of dimensional item objects.
     */
    Set<DimensionalItemObject> getIndicatorDimensionalItemObjects( Collection<Indicator> indicators );

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
     * @param period the period for which to calculate the value.
     * @param valueMap the map of data values.
     * @param constantMap the map of constants.
     * @param orgUnitCountMap the map of organisation unit counts.
     * @return the calculated value as a double.
     */
    IndicatorValue getIndicatorValueObject( Indicator indicator, Period period,
        Map<DimensionalItemObject, Double> valueMap, Map<String, Double> constantMap,
        Map<String, Integer> orgUnitCountMap );
}
