package org.hisp.dhis.expression;

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

import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.parser.expression.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.parser.expression.ParserUtils.*;

/**
 * Visitor to the nodes in a parsed expression.
 * <p/>
 * Uses the ANTLR4 visitor partern.
 *
 * @author Jim Grace
 */
public class ExpressionExprVisitor
    extends ExprVisitor
{
    private DimensionService dimensionService;

    private OrganisationUnitGroupService organisationUnitGroupService;

    /**
     * Used to collect the dimensional item ids in the expression.
     */
    private Set<DimensionalItemId> itemIds = new HashSet<>();

    /**
     * Used to collect the organisation unit group ids in the expression.
     */
    private Set<String> orgUnitGroupIds = new HashSet<>();

    /**
     * Organisation unit group counts to use in evaluating an expression.
     */
    Map<String, Integer> orgUnitCountMap = new HashMap<>();

    /**
     * Count of days in period to use in evaluating an expression.
     */
    private Double days = null;

    /**
     * Values to use for dimensional items in evaluating an expression.
     */
    private Map<String, Double> keyValueMap;

    /**
     * Count of dimension items found.
     */
    private int itemsFound = 0;

    /**
     * Count of dimension item values found.
     */
    private int itemValuesFound = 0;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ExpressionExprVisitor( Map<Integer, ExprFunction> functionMap,
        Map<Integer, ExprItem> itemMap, ExprFunctionMethod functionMethod,
        ExprItemMethod itemMethod, DimensionService dimensionService,
        OrganisationUnitGroupService organisationUnitGroupService,
        ConstantService constantService )
    {
        this.functionMap = checkNotNull( functionMap );
        this.itemMap = checkNotNull( itemMap );
        this.functionMethod = checkNotNull( functionMethod );
        this.itemMethod = checkNotNull( itemMethod );

        this.dimensionService = checkNotNull( dimensionService );
        this.organisationUnitGroupService = checkNotNull( organisationUnitGroupService );
        this.constantService = checkNotNull( constantService );
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Makes a hash map that can be used for fast lookup from the identifier
     * found in the expression. This allows the DimensionalItemObject to be
     * identified from the expression without the overhead of calling the
     * IdentifiableObjectManager.
     *
     * @param valueMap the given valueMap.
     */
    public void setValueMap( Map<DimensionalItemObject, Double> valueMap )
    {
        keyValueMap = valueMap.entrySet().stream().collect(
            Collectors.toMap( e -> e.getKey().getDimensionItem(), e -> e.getValue() ) );
    }

    /**
     * Gets an expression item's value from the keyValueMap.
     * <p/>
     * If we should replace nulls with the default value, then do so, and
     * remember how many items found, and how many of them had values, for
     * subsequent MissingValueStrategy analysis.
     * <p/>
     * If we should not replace nulls with the default value, then don't,
     * as this is likely for some function that is testing for nulls, and
     * a missing value should not count towards the MissingValueStrategy.
     *
     * @param itemId the DimensionalItemObject id.
     * @return the item's value.
     */
    public Double getItemValue( String itemId )
    {
        Double value = keyValueMap.get( itemId );

        if ( replaceNulls )
        {
            itemsFound++;

            if ( value == null )
            {
                value = DOUBLE_VALUE_IF_NULL;
            }
            else
            {
                itemValuesFound++;
            }
        }

        return value;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public DimensionService getDimensionService()
    {
        return dimensionService;
    }

    public OrganisationUnitGroupService getOrganisationUnitGroupService()
    {
        return organisationUnitGroupService;
    }

    public Set<DimensionalItemId> getItemIds()
    {
        return itemIds;
    }

    public Set<String> getOrgUnitGroupIds()
    {
        return orgUnitGroupIds;
    }

    public Map<String, Integer> getOrgUnitCountMap()
    {
        return orgUnitCountMap;
    }

    public void setOrgUnitCountMap( Map<String, Integer> orgUnitCountMap )
    {
        this.orgUnitCountMap = orgUnitCountMap;
    }

    public Double getDays()
    {
        return days;
    }

    public void setDays( Double days )
    {
        this.days = days;
    }

    public int getItemsFound()
    {
        return itemsFound;
    }

    public int getItemValuesFound()
    {
        return itemValuesFound;
    }
}
