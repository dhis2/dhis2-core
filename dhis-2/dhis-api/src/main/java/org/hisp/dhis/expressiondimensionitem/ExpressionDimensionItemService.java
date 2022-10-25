/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.expressiondimensionitem;

import java.util.List;

/**
 * Expressions are mathematical formulas and can contain references to various
 * elements.
 *
 * @author Dusan Bernat
 */
public interface ExpressionDimensionItemService
{
    String ID = ExpressionDimensionItemService.class.getName();

    // -------------------------------------------------------------------------
    // Expression CRUD operations
    // -------------------------------------------------------------------------

    /**
     * Adds a new Expression to the database.
     *
     * @param expression The Expression to add.
     * @return The generated identifier for this Expression.
     */
    long addExpressionDimensionItem( ExpressionDimensionItem expression );

    /**
     * Updates an Expression.
     *
     * @param expression The Expression to update.
     */
    void updateExpressionDimensionItem( ExpressionDimensionItem expression );

    /**
     * Deletes an Expression from the database.
     *
     * @param expression the expression.
     */
    void deleteExpressionDimensionItem( ExpressionDimensionItem expression );

    /**
     * Get the Expression with the given identifier.
     *
     * @param id The identifier.
     * @return an Expression with the given identifier.
     */
    ExpressionDimensionItem getExpressionDimensionItem( long id );

    /**
     * Gets all Expressions.
     *
     * @return A list with all Expressions.
     */
    List<ExpressionDimensionItem> getAllExpressionDimensionItems();
}
