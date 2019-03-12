package org.hisp.dhis.parser.program;

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

import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;

import java.util.Date;
import java.util.Map;

/**
 * Parses program indicators/filters/rules using the ANTLR parser.
 *
 * @author Chau Thu Tran
 * @author Jim Grace
 */
public interface ProgramParserService
{
    /**
     * Get the description of any program indicator expression (expression or
     * filter).
     *
     * @Depreicated Does not do type-checking on the expression.
     * Use getExpressionDescription or getFilterDescription instead.
     *
     * @param expression A program indicator expression or filter string
     * @return The description
     */
    @Deprecated String getUntypedDescription( String expression );

    /**
     * Gets a program indicator expression description (must evaluate to a
     * Double).
     *
     * @param expression A program indicator expression string
     * @return The description
     */
    String getExpressionDescription( String expression );

    /**
     * Gets a program indicator expression filter (must evaluate to a
     * Boolean).
     *
     * @param expression A program indicator expression or filter string
     * @return The description
     */
    String getFilterDescription( String expression );

    /**
     * Indicates whether the given program indicator expression is valid.
     *
     * @param expression the expression to evaluate.
     * @return whether the expression is valid.
     */
    boolean expressionIsValid( String expression );

    /**
     * Indicates whether the given program indicator filter is valid.
     *
     * @param filter the filter to evaluate.
     * @return whether the filter is valid.
     */
    boolean filterIsValid( String filter );

    /**
     * Validates that an expression returns a value from a class.
     * Also collects descriptions of individual items, so that an
     * expression description may be formed if the caller desires.
     * <p/>
     * This method is made public so that the parser validation routines
     * can use it to validate quoted sub-expressions.
     *
     * @param expression the expression to validate.
     * @param clazz the class to check the expression's value against.
     * @param itemDescriptions map of item descriptions (to add to).
     */
    void validate( String expression, Class clazz, Map<String, String> itemDescriptions );

    /**
     * Gets the program indicator expression as an analytics SQL clause.
     * Ignores missing numeric values for data elements and attributes.
     *
     * @param programIndicator the program indicator to evaluate.
     * @param startDate the start date.
     * @param endDate the end date.
     * @return the SQL string.
     */
    String getExpressionAnalyticsSql( ProgramIndicator programIndicator, Date startDate, Date endDate );

    /**
     * Gets the program indicator filter as an analytics SQL clause.
     * Does not ignore missing numeric values for data elements and attributes.
     *
     * @param programIndicator the program indicator to evaluate.
     * @param startDate the start date.
     * @param endDate the end date.
     * @return the SQL string.
     */
    String getFilterAnalyticsSql( ProgramIndicator programIndicator, Date startDate, Date endDate );

    /**
     * Gets the the analytics SQL clause of an expression.
     * Does not ignore missing numeric values for data elements and attributes.
     *
     * @param expression the expression.
     * @param programIndicator the program indicator to evaluate.
     * @param startDate the start date.
     * @param endDate the end date.
     * @return the SQL string.
     */
    String getAnalyticsSql( String expression, ProgramIndicator programIndicator, Date startDate, Date endDate, boolean ignoreMissingValues );

    /**
     * Returns a SQL clause which matches any value for the data elements and
     * attributes in the given expression.
     *
     * @param expression the expression.
     * @return the SQL string.
     */
    String getAnyValueExistsClauseAnalyticsSql( String expression, AnalyticsType analyticsType );
}
