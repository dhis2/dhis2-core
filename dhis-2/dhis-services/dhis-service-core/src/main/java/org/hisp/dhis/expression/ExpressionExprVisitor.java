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

import jdk.internal.jline.internal.Preconditions;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.constant.Constant;
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
 * ANTLR parse tree visitor to compute an expression value, once the expression
 * item values have been retrieved from the database.
 * <p/>
 * Uses the ANTLR visitor partern.
 *
 * @author Jim Grace
 */
public class ExpressionExprVisitor
    extends ExprVisitor
{
    private DimensionService dimensionService;

    private OrganisationUnitGroupService organisationUnitGroupService;

    private ConstantService constantService;

    /**
     * Used to collect the dimensional item ids in the expression.
     */
    private Set<DimensionalItemId> itemIds = new HashSet<>();

    /**
     * Used to collect the organisation unit group ids in the expression.
     */
    private Set<String> orgUnitGroupIds = new HashSet<>();

    /**
     * Used to collect the string replacements to build a description.
     */
    private Map<String, String> itemDescriptions = new HashMap<>();

    /**
     * Constants to use in evaluating an expression.
     */
    Map<String, Double> constantMap = new HashMap<>();

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
        Map<Integer, ExprItem> itemMap,
        ExprFunctionMethod functionMethod, ExprItemMethod itemMethod )
    {
        Preconditions.checkNotNull( functionMap );
        Preconditions.checkNotNull( itemMap );
        Preconditions.checkNotNull( functionMethod );
        Preconditions.checkNotNull( itemMethod );

        this.functionMap = functionMap;
        this.itemMap = itemMap;
        this.functionMethod = functionMethod;
        this.itemMethod = itemMethod;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Makes a hash map that can be used for fast
     * lookup from the identifier found in the expression. This allows the
     * DimensionalItemObject to be identified from the expression without
     * the overhead of calling the IdentifiableObjectManager.
     *
     * @param valueMap the given valueMap.
     */
    private void setValueMap( Map<DimensionalItemObject, Double> valueMap )
    {
        keyValueMap = valueMap.entrySet().stream().collect(
            Collectors.toMap( e -> e.getKey().getDimensionItem(), e -> e.getValue() ) );
    }

    public Object putItemDescription( String exprText, DimensionalItemId itemId )
    {
        DimensionalItemObject item = dimensionService.getDataDimensionalItemObject( itemId );

        if ( item == null )
        {
            throw new ParserExceptionWithoutContext( "Can't find " + itemId.getDimensionItemType().name() + " for '" + exprText + "'" );
        }

        itemDescriptions.put( exprText, item.getDisplayName() );

        return DOUBLE_VALUE_IF_NULL;
    }

    public Object putConstantDescription( String exprText, String constantId )
    {
        Constant constant = constantService.getConstant( constantId );

        if ( constant == null )
        {
            throw new ParserExceptionWithoutContext( "No constant defined for " + exprText );
        }

        itemDescriptions.put( exprText, constant.getDisplayName() );

        return DOUBLE_VALUE_IF_NULL;
    }

    public Object putDescription( String exprText, String description )
    {
        itemDescriptions.put( exprText, description );

        return DOUBLE_VALUE_IF_NULL;
    }

    public Object putItemId( DimensionalItemId itemId)
    {
        itemIds.add( itemId );

        return DOUBLE_VALUE_IF_NULL;
    }

    public Object putOrgUnitGroup( String orgUnitGroupId )
    {
        orgUnitGroupIds.add( orgUnitGroupId );

        return DOUBLE_VALUE_IF_NULL;
    }

    public Double evaluateConstant( String constantUid )
    {
        Double value = constantMap.get( constantUid );

        if ( value == null ) // Shouldn't happen for a valid expression.
        {
            throw new ParserExceptionWithoutContext( "Can't find constant " + constantUid );
        }

        return value;
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

    public Double getOrgUnitGroupCount( String orgUnitGroupId )
    {
        Integer count = orgUnitCountMap.get( orgUnitGroupId );

        if ( count == null ) // Shouldn't happen for a valid expression.
        {
            throw new ParserExceptionWithoutContext( "Can't find count for organisation unit " + orgUnitGroupId );
        }

        return count.doubleValue();
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Set<DimensionalItemId> getItemIds()
    {
        return itemIds;
    }

    public Set<String> getOrgUnitGroupIds()
    {
        return orgUnitGroupIds;
    }

    public Map<String, String> getItemDescriptions()
    {
        return itemDescriptions;
    }

    public void setConstantMap( Map<String, Double> constantMap )
    {
        this.constantMap = constantMap;
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
