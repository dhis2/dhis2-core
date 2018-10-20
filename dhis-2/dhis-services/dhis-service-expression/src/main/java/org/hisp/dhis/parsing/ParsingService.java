package org.hisp.dhis.parsing;

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

import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.Period;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses expressions.
 *
 * @author Jim Grace
 */
public interface ParsingService
{
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
     * Returns all dimensional item objects which are present in the given
     * expressions, in the context of which organisation units and periods
     * they need to be evaluated for.
     *
     * @param expressions the expressions to parse
     * @param orgUnits the list of organisation units
     * @param periods the list of periods
     * @param constantMap constants and their values
     * @param orgUnitCountMap the mapping between organisation unit group uid
     *        and count of organisation units to use in the calculation.
     * @return orgUnits/periods/DimensionalItemObjects to fetch
     */
    Set<ExpressionItem> getExpressionItems( List<Expression> expressions,
        List<OrganisationUnit> orgUnits, List<Period> periods,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap );

    /**
     * Returns all OrganisationUnitGroups in the given expression string.
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
     * @param orgUnit the organisation unit to evaluate for
     * @param period the period to evaluate for
     * @param valueMap the DimensionalItemObject values to use for calculation.
     * @param constantMap map of constants to use for calculation.
     * @param orgUnitCountMap the mapping between organisation unit group uid
     *        and count of organisation units to use in the calculation.
     * @param days the number of days to use in the calculation.
     * @return the calculated value as a double.
     */
    Double getExpressionValue( Expression expression, OrganisationUnit orgUnit, Period period,
        Map<ExpressionItem, Double> valueMap, Map<String, Double> constantMap,
        Map<String, Integer> orgUnitCountMap, int days );
}
