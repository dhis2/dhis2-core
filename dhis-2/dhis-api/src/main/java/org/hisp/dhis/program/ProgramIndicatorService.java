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
package org.hisp.dhis.program;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.analytics.DataType;

/**
 * @author Chau Thu Tran
 * @author Jim Grace
 */
public interface ProgramIndicatorService
{
    // -------------------------------------------------------------------------
    // ProgramIndicator CRUD
    // -------------------------------------------------------------------------

    /**
     * Adds a {@link ProgramIndicator}.
     *
     * @param programIndicator The to ProgramIndicator add.
     * @return A generated unique id of the added {@link ProgramIndicator}.
     */
    long addProgramIndicator( ProgramIndicator programIndicator );

    /**
     * Updates a {@link ProgramIndicator}.
     *
     * @param programIndicator the ProgramIndicator to update.
     */
    void updateProgramIndicator( ProgramIndicator programIndicator );

    /**
     * Deletes a {@link ProgramIndicator}.
     *
     * @param programIndicator the ProgramIndicator to delete.
     */
    void deleteProgramIndicator( ProgramIndicator programIndicator );

    /**
     * Returns a {@link ProgramIndicator}.
     *
     * @param id the id of the ProgramIndicator to return.
     * @return the ProgramIndicator with the given id
     */
    ProgramIndicator getProgramIndicator( long id );

    /**
     * Returns a {@link ProgramIndicator} with a given name.
     *
     * @param name the name of the ProgramIndicator to return.
     * @return the ProgramIndicator with the given name, or null if no match.
     */
    ProgramIndicator getProgramIndicator( String name );

    /**
     * Returns the {@link ProgramIndicator} with the given UID.
     *
     * @param uid the UID.
     * @return the ProgramIndicator with the given UID, or null if no match.
     */
    ProgramIndicator getProgramIndicatorByUid( String uid );

    /**
     * Returns all {@link ProgramIndicator}.
     *
     * @return a List of all ProgramIndicator, or an empty List if there are no
     *         ProgramIndicators.
     */
    List<ProgramIndicator> getAllProgramIndicators();

    List<ProgramIndicator> getProgramIndicatorsWithNoExpression();

    // -------------------------------------------------------------------------
    // ProgramIndicator logic
    // -------------------------------------------------------------------------

    /**
     * Get the description of any program indicator expression (expression or
     * filter).
     *
     * @deprecated Does not do type-checking on the expression. Use
     *             getExpressionDescriptionRegEx or getFilterDescription
     *             instead.
     *
     * @param expression A program indicator expression or filter string
     * @return The description
     */
    @Deprecated
    String getUntypedDescription( String expression );

    /**
     * Gets a program indicator expression description (must evaluate to a
     * Double).
     *
     * @param expression A program indicator expression string
     * @return The description
     */
    String getExpressionDescription( String expression );

    /**
     * Gets a program indicator expression filter (must evaluate to a Boolean).
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
     * Validates that an expression returns a value from a class. Also collects
     * descriptions of individual items, so that an expression description may
     * be formed if the caller desires.
     * <p/>
     * This method is made public so that the parser validation routines can use
     * it to validate quoted sub-expressions.
     *
     * @param expression the expression to validate.
     * @param clazz the class to check the expression's value against.
     * @param itemDescriptions map of item descriptions (to add to).
     */
    void validate( String expression, Class<?> clazz, Map<String, String> itemDescriptions );

    /**
     * Gets the the analytics SQL clause of an expression. Does not ignore
     * missing numeric values for data elements and attributes.
     *
     * @param expression the expression.
     * @param dataType the data type to return.
     * @param programIndicator the program indicator to evaluate.
     * @param startDate the start date.
     * @param endDate the end date.
     * @return the SQL string.
     */
    String getAnalyticsSql( String expression, DataType dataType, ProgramIndicator programIndicator,
        Date startDate, Date endDate );

    /**
     * Gets the the analytics SQL clause of an expression. Does not ignore
     * missing numeric values for data elements and attributes.
     *
     * @param expression the expression.
     * @param dataType the data type to return.
     * @param programIndicator the program indicator to evaluate.
     * @param startDate the start date.
     * @param endDate the end date.
     * @param tableAlias use this table alias for expression returning a inner
     *        query
     * @return the SQL string.
     */

    String getAnalyticsSql( String expression, DataType dataType, ProgramIndicator programIndicator,
        Date startDate, Date endDate, String tableAlias );

    /**
     * Returns a SQL clause which matches any value for the data elements and
     * attributes in the given expression.
     *
     * @param expression the expression.
     * @return the SQL string.
     */
    String getAnyValueExistsClauseAnalyticsSql( String expression, AnalyticsType analyticsType );

    // -------------------------------------------------------------------------
    // ProgramIndicatorGroup
    // -------------------------------------------------------------------------

    long addProgramIndicatorGroup( ProgramIndicatorGroup ProgramIndicatorGroup );

    void updateProgramIndicatorGroup( ProgramIndicatorGroup ProgramIndicatorGroup );

    void deleteProgramIndicatorGroup( ProgramIndicatorGroup ProgramIndicatorGroup );

    ProgramIndicatorGroup getProgramIndicatorGroup( long id );

    ProgramIndicatorGroup getProgramIndicatorGroup( String uid );

    List<ProgramIndicatorGroup> getAllProgramIndicatorGroups();

}
