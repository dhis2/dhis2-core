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
package org.hisp.dhis.subexpression;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.hisp.dhis.analytics.AggregationType.SUM;
import static org.hisp.dhis.common.CodeGenerator.generateUid;
import static org.hisp.dhis.common.DimensionItemType.SUBEXPRESSION_DIMENSION_ITEM;

import java.util.Collection;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.QueryModifiers;

/**
 * Indicator subexpression dimensional item object.
 *
 * @author Jim Grace
 */
@Getter
@EqualsAndHashCode( callSuper = true )
public class SubexpressionDimensionItem
    extends BaseDimensionalItemObject
{
    /**
     * The SQL fragment containing the subexpression logic.
     */
    private final String subexSql;

    /**
     * The subexpression item objects. Must be of type data element or data
     * element operand. Note that it can be a {@link Collection} for normal use
     * or a {@link List} for testing so the items will be inserted into the
     * query in a predictable order.
     */
    private final Collection<DimensionalItemObject> items;

    public SubexpressionDimensionItem( String subexSql, Collection<DimensionalItemObject> items,
        QueryModifiers queryMods )
    {
        this.subexSql = subexSql;
        this.items = items;
        this.queryMods = queryMods;
        this.uid = generateUid();
        this.aggregationType = SUM; // (QueryMods can override this.)

        setDimensionItemType( SUBEXPRESSION_DIMENSION_ITEM );
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Gets a quoted SQL column name for a data element uid and optionally a
     * category option combo uid. These column names are generated in the SQL
     * fragment for each item and referenced in the SQL fragment for the
     * subexpression as a whole.
     * <p>
     * If there is an aggregation type override in the query modifiers, its name
     * is appended to the end of the column name. This is needed in case the
     * same item appears in the same subexpression with and without an
     * aggregation type override.
     */
    public static String getItemColumnName( String deUid, String cocUid, String aocUid, QueryModifiers mods )
    {
        String separator = (isEmpty( cocUid ) && isEmpty( aocUid )) ? "" : "_";

        String coc = isEmpty( cocUid ) ? "" : cocUid;

        String aoc = isEmpty( aocUid ) ? "" : "_" + aocUid;

        String aggregationName = (mods != null && mods.getAggregationType() != null)
            ? mods.getAggregationType().name()
            : "";

        return "\"" + deUid + separator + coc + aoc + aggregationName + "\"";
    }
}
