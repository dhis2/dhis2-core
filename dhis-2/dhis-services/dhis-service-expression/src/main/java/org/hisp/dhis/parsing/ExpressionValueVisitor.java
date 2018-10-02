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

import org.antlr.v4.runtime.tree.ParseTree;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;

import java.util.HashMap;
import java.util.Map;

import static org.hisp.dhis.common.DimensionItemType.*;
import static org.hisp.dhis.parsing.generated.ExpressionParser.*;

/**
 * ANTLR parse tree visitor to get an expression value.
 * <p/>
 * Uses the ANTLR visitor partern.
 *
 * @author Jim Grace
 */
public class ExpressionValueVisitor extends ExpressionVisitor
{
    private Map<String, Double> keyValueMap;

    public Double getExpressionValue( ParseTree parseTree, OrganisationUnit orgUnit, Period period,
        Map<ExpressionItem, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap, int days,
        OrganisationUnitService _organisationUnitService, IdentifiableObjectManager _manager)
    {
        //TODO: Why don't the @Autowired values work?
        organisationUnitService = _organisationUnitService;
        manager = _manager;

        currentOrgUnit = orgUnit;
        currentPeriod = period;
        this.constantMap = constantMap;
        this.orgUnitCountMap = orgUnitCountMap;
        this.days = new Double( days );

        makeKeyValueMap( valueMap );

        return castDouble( visit( parseTree ) );
    }

    // -------------------------------------------------------------------------
    // Visitor methods implemented here
    // -------------------------------------------------------------------------

    @Override
    public Object visitDataElement( DataElementContext ctx )
    {
        return getItemValue( DATA_ELEMENT, ctx.dataElementId().getText() );
    }

    @Override
    public Object visitDataElementOperandWithoutAoc( DataElementOperandWithoutAocContext ctx )
    {
        return getItemValue( DATA_ELEMENT_OPERAND, ctx.dataElementOperandIdWithoutAoc().getText() );
    }

    @Override
    public Object visitDataElementOperandWithAoc( DataElementOperandWithAocContext ctx )
    {
        return getItemValue( DATA_ELEMENT_OPERAND, ctx.dataElementOperandIdWithAoc().getText() );
    }

    @Override
    public Object visitProgramDataElement( ProgramDataElementContext ctx )
    {
        return getItemValue( PROGRAM_DATA_ELEMENT, ctx.programDataElementId().getText() );
    }

    @Override
    public Object visitProgramAttribute ( ProgramAttributeContext ctx )
    {
        return getItemValue( PROGRAM_ATTRIBUTE, ctx.programAttributeId().getText() );
    }

    @Override
    public Object visitProgramIndicator ( ProgramIndicatorContext ctx )
    {
        return getItemValue( PROGRAM_INDICATOR, ctx.programIndicatorId().getText() );
    }

    @Override
    public Object visitReportingRate ( ReportingRateContext ctx )
    {
        return getItemValue( REPORTING_RATE, ctx.reportingRateId().getText() );
    }

    @Override
    public Object visitOrgUnitCount( OrgUnitCountContext ctx )
    {
        Integer count = orgUnitCountMap.get( ctx.orgUnitCountId().getText() );

        if ( count == null )
        {
            throw new LowLevelParsingException( "Can't find count for organisation unit " + ctx.orgUnitCountId().getText() );
        }

        return count.doubleValue();
    }

    // -------------------------------------------------------------------------
    // Logical methods implemented here
    // -------------------------------------------------------------------------

    @Override
    protected Object functionAnd( ExprContext ctx )
    {
        Boolean b1 = castBoolean( visit( ctx.expr( 0 ) ) );

        if ( b1 == null )
        {
            return null;
        }

        if ( b1 == false )
        {
            return false;
        }

        Boolean b2 = castBoolean( visit( ctx.expr( 1 ) ) );

        if ( b2 == null )
        {
            return null;
        }

        return b2;
    }

    @Override
    protected Object functionOr( ExprContext ctx )
    {
        Boolean b1 = castBoolean( visit( ctx.expr( 0 ) ) );

        if ( b1 == null )
        {
            return null;
        }

        if ( b1 == true )
        {
            return true;
        }

        Boolean b2 = castBoolean( visit( ctx.expr( 1 ) ) );

        if ( b2 == null )
        {
            return null;
        }

        return b2;
    }

    @Override
    protected Object functionIf( ExprContext ctx )
    {
        Boolean test = castBoolean( visit( ctx.a3().expr( 0 ) ) );

        if ( test == null )
        {
            return null;
        }

        return test
            ? visit( ctx.a3().expr( 1 ) )
            : visit( ctx.a3().expr( 2 ) );
    }

    @Override
    protected Object functionCoalesce( ExprContext ctx )
    {
        for ( ExprContext c : ctx.a1_n().expr() )
        {
            Object val = visit( c );

            if ( val != null )
            {
                return val;
            }
        }
        return null;
    }

    @Override
    protected final Object functionExcept( ExprContext ctx )
    {
        Boolean test = castBoolean( visit( ctx.a1().expr() ) );

        if ( test == null )
        {
            return null;
        }

        return test
            ? null
            : visit( ctx.expr( 0 ) );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * From the initial valueMap containing expression items with full
     * DimensionalItemObjects, makes a hash map that can be used for fast
     * lookup from the DimensionalItemObject type and identifier found in the
     * expression. This avoids a serious performance penalty for many
     * expressions such as in a large set of validation rules.
     * <p/>
     * If the aggregation type is the default for the DimensionalItemObject,
     * the entry is placed twice in the map, with and without the aggregation
     * type. This allows the value to be accessed whether or not an explicit
     * aggregation type is specified in the expression -- without having access
     * to the full DimensionalItemObject to determine its default aggregation
     * type.
     *
     * @param valueMap the given valueMap.
     */
    private void makeKeyValueMap( Map<ExpressionItem, Double> valueMap )
    {
        keyValueMap = new HashMap<>();

        for ( Map.Entry<ExpressionItem, Double> entry : valueMap.entrySet() )
        {
            ExpressionItem eItem = entry.getKey();

            DimensionalItemObject object = eItem.getDimensionalItemObject();

            AggregationType aggregationType = currentAggregationType != null ? currentAggregationType : eItem.getAggregationType();

            String key = getKey( eItem.getOrgUnit(), eItem.getPeriod(),
                object.getDimensionItemType(), object.getDimensionItem(), aggregationType );

            keyValueMap.put( key, entry.getValue() );

            if ( aggregationType == eItem.getDimensionalItemObject().getAggregationType() )
            {
                key = getKey( eItem.getOrgUnit(), eItem.getPeriod(),
                    object.getDimensionItemType(), object.getDimensionItem(), null );

                keyValueMap.put( key, entry.getValue() );
            }
        }
    }

    /**
     * Gets an expression item's value from the keyValueMap.
     *
     * @param itemType the DimensionalItemObject type.
     * @param itemId the DimensionalItemObject id.
     * @return the item's value.
     */
    private Object getItemValue( DimensionItemType itemType, String itemId )
    {
        String key = getKey( currentOrgUnit, currentPeriod, itemType, itemId, currentAggregationType );

        Object value = keyValueMap.get( key );

        return value;
    }

    /**
     * Generates a key to use in the keyValueMap.
     *
     * @param orgUnit
     * @param period
     * @param dimensionItemType
     * @param itemId
     * @param aggregationType
     * @return
     */
    private String getKey( OrganisationUnit orgUnit, Period period,
        DimensionItemType dimensionItemType, String itemId,
        AggregationType aggregationType )
    {
        return orgUnit.getUid() + "-" + period.getIsoDate() + "-"
            + dimensionItemType.name()  + "-" + itemId
            + ( aggregationType == null ? "" : "-" + aggregationType.name() );
    }
}
