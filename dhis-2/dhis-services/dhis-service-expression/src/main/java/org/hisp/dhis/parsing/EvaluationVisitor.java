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
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;

import java.util.HashMap;
import java.util.Map;

import static org.hisp.dhis.common.DimensionItemType.*;
import static org.hisp.dhis.parsing.generated.ExpressionParser.*;

/**
 * ANTLR parse tree visitor to evaluates expressions.
 *
 * @author Jim Grace
 */
public class EvaluationVisitor extends AbstractVisitor
{
    private Map<String, Double> keyValueMap;

    public Double getExpressionValue( ParseTree parseTree, OrganisationUnit orgUnit, Period period,
        MapMapMap<OrganisationUnit, Period, DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap, int days,
        OrganisationUnitService _organisationUnitService, IdentifiableObjectManager _manager)
    {
        organisationUnitService = _organisationUnitService;
        manager = _manager;

        currentOrgUnit = orgUnit;
        currentPeriod = period;
        this.constantMap = constantMap;
        this.orgUnitCountMap = orgUnitCountMap;
        this.days = days;

        makeKeyValueMap( valueMap );

        return castDouble( visit( parseTree ) );
    }

    // -------------------------------------------------------------------------
    // Visitor methods that are implemented here
    // -------------------------------------------------------------------------

    @Override
    public Object visitDataElement( DataElementContext ctx )
    {
        String itemId = ctx.dataElementId().getText();

        return getItemValue( DATA_ELEMENT, itemId );
    }

    @Override
    public Object visitDataElementOperand( DataElementOperandContext ctx )
    {
        String itemId = ctx.dataElementOperandId().getText();

        return getItemValue( DATA_ELEMENT_OPERAND, itemId );
    }

    @Override
    public Object visitOrgUnitCount( OrgUnitCountContext ctx )
    {
        return orgUnitCountMap.get( ctx.getText() );
    }

    @Override
    public Object visitDays( DaysContext ctx )
    {
        return days;
    }

    // -------------------------------------------------------------------------
    // Logical methods implemented here
    // -------------------------------------------------------------------------

    @Override
    protected Object functionAnd( ExprContext ctx )
    {
        return castBoolean( visit( ctx.expr( 0 ) ) )
            && castBoolean( visit( ctx.expr( 1 ) ) );
    }

    @Override
    protected Object functionOr( ExprContext ctx )
    {
        return castBoolean( visit( ctx.expr( 0 ) ) )
            || castBoolean( visit( ctx.expr( 1 ) ) );
    }

    @Override
    protected Object functionIf( ExprContext ctx )
    {
        return castBoolean( visit( ctx.a3().expr( 0 ) ) )
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
        return castBoolean( visit( ctx.a1().expr() ) )
            ? null
            : visit( ctx.expr( 0 ) );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void makeKeyValueMap( MapMapMap<OrganisationUnit, Period, DimensionalItemObject, Double> valueMap )
    {
        keyValueMap = new HashMap<>();

        for ( Map.Entry<OrganisationUnit, MapMap<Period, DimensionalItemObject, Double>> orgUnitEntry : valueMap.entrySet() )
        {
            for ( Map.Entry<Period, Map<DimensionalItemObject, Double>> periodEntry : orgUnitEntry.getValue().entrySet() )
            {
                for ( Map.Entry<DimensionalItemObject, Double> itemEntry : periodEntry.getValue().entrySet() )
                {
                    keyValueMap.put( getKey( orgUnitEntry.getKey(), periodEntry.getKey(),
                        itemEntry.getKey().getDimensionItemType(), itemEntry.getKey().getDimensionItem() ), itemEntry.getValue() );
                }
            }
        }
    }

    private Double getItemValue( DimensionItemType itemType, String itemId )
    {
        String key = getKey( currentOrgUnit, currentPeriod, itemType, itemId );

        Double value = keyValueMap.get( key );

        return value;
    }

    private String getKey( OrganisationUnit orgUnit, Period period,
        DimensionItemType dimensionItemType, String itemId )
    {
        return orgUnit.getUid() + "-" + period.getIsoDate() + "-"
            + dimensionItemType.name()  + "-" + itemId;
    }
}
