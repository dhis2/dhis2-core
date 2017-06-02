package org.hisp.dhis.program;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.Date;
import java.util.List;

/**
 * @author Chau Thu Tran
 */
public interface ProgramIndicatorService
{
    /**
     * Adds an {@link ProgramIndicator}
     *
     * @param programIndicator The to ProgramIndicator add.
     * @return A generated unique id of the added {@link ProgramIndicator}.
     */
    int addProgramIndicator( ProgramIndicator programIndicator );

    /**
     * Updates an {@link ProgramIndicator}.
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
    ProgramIndicator getProgramIndicator( int id );

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
     * @return a List of all ProgramIndicator, or an empty List if
     * there are no ProgramIndicators.
     */
    List<ProgramIndicator> getAllProgramIndicators();

    /**
     * Get description of an indicator expression.
     *
     * @param expression An expression string
     * @return The description
     */
    String getExpressionDescription( String expression );

    /**
     * Get the expression as an analytics SQL clause. Ignores missing numeric
     * values for data elements and attributes.
     * 
     * @param expression the expression.
     * @return the SQL string.
     */
    String getAnalyticsSQl( String expression, AnalyticsType aalyticsType, Date startDate, Date endDate );
    
    /**
     * Get the expression as an analytics SQL clause.
     * 
     * @param expression the expression.
     * @param whether to ignore missing values for data elements and attributes.
     * @return the SQL string.
     */
    String getAnalyticsSQl( String expression, AnalyticsType analyticsType, boolean ignoreMissingValues, Date startDate, Date endDate );
    
    /**
     * Returns a SQL clause which matches any value for the data elements and
     * attributes in the given expression.
     * 
     * @param expression the expression.
     * @return the SQL string.
     */
    String getAnyValueExistsClauseAnalyticsSql( String expression, AnalyticsType analyticsType );

    /**
     * Indicates whether the given program indicator expression is valid.
     * 
     * @param expression An expression string.
     * @return the string {@link ProgramIndicator.VALID} if valid, if not any of
     *         {@link ProgramIndicator.EXPRESSION_NOT_VALID},
     *         {@link ProgramIndicator.INVALID_VARIABLES_IN_EXPRESSION}.
     */
    String expressionIsValid( String expression );

    /**
     * Indicates whether the given program indicator expression is valid.
     * 
     * @param expression An expression string.
     * @return the string {@link ProgramIndicator.VALID} if valid, if not any of
     *         {@link ProgramIndicator.FILTER_NOT_EVALUATING_TO_TRUE_OR_FALSE},
     *         {@link ProgramIndicator.INVALID_VARIABLES_IN_EXPRESSION}.
     */
    String filterIsValid( String filter );

    // -------------------------------------------------------------------------
    // ProgramIndicatorGroup
    // -------------------------------------------------------------------------

    int addProgramIndicatorGroup( ProgramIndicatorGroup ProgramIndicatorGroup );

    void updateProgramIndicatorGroup( ProgramIndicatorGroup ProgramIndicatorGroup );

    void deleteProgramIndicatorGroup( ProgramIndicatorGroup ProgramIndicatorGroup );

    ProgramIndicatorGroup getProgramIndicatorGroup( int id );

    ProgramIndicatorGroup getProgramIndicatorGroup( String uid );

    List<ProgramIndicatorGroup> getAllProgramIndicatorGroups();

}
