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
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.parser.expression.CommonVisitor;
import org.hisp.dhis.parser.expression.ParserExceptionWithoutContext;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.common.DimensionItemType.*;
import static org.hisp.dhis.parser.expression.ParserUtils.*;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.*;

/**
 * ANTLR4 visitor to find the expression items in an expression,
 * find the organisation unit groups in an expression, or find the
 * replacements required to convert the expression into its description.
 * <p/>
 * Uses the ANTLR visitor partern.
 *
 * @author Jim Grace
 */
public class ExpressionItemsVisitor
    extends CommonVisitor
{
    private DimensionService dimensionService;

    private OrganisationUnitGroupService organisationUnitGroupService;

    private ConstantService constantService;

    /**
     * If not null, used to collect the dimensional item ids found
     * in the expression.
     */
    private Set<DimensionalItemId> itemIds = null;

    /**
     * If not null, used to collect the organisation unit group ids found
     * in the expression.
     */
    private Set<String> orgUnitGroupIds = null;

    /**
     * If not null, used to collect the string replacements that can be
     * used to convert the expression into its description.
     */
    private Map<String, String> itemDescriptions = null;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ExpressionItemsVisitor( DimensionService dimensionService,
        OrganisationUnitGroupService organisationUnitGroupService,
        ConstantService constantService )
    {
        checkNotNull( dimensionService );
        checkNotNull( organisationUnitGroupService );
        checkNotNull( constantService );

        this.dimensionService = dimensionService;
        this.organisationUnitGroupService = organisationUnitGroupService;
        this.constantService = constantService;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Set<DimensionalItemId> getItemIds()
    {
        return itemIds;
    }

    public void setItemIds( Set<DimensionalItemId> itemIds )
    {
        this.itemIds = itemIds;
    }

    public Set<String> getOrgUnitGroupsIds()
    {
        return orgUnitGroupIds;
    }

    public void setOrgUnitGroupIds( Set<String> orgUnitGroupIds )
    {
        this.orgUnitGroupIds = orgUnitGroupIds;
    }

    public Map<String, String> getItemDescriptions()
    {
        return itemDescriptions;
    }

    public void setItemDescriptions( Map<String, String> itemDescriptions )
    {
        this.itemDescriptions = itemDescriptions;
    }

    // -------------------------------------------------------------------------
    // Visitor methods
    // -------------------------------------------------------------------------

    @Override
    public Object visitItem( ItemContext ctx )
    {
        switch ( ctx.it.getType() )
        {
            case HASH_BRACE:
                if ( isDataElementOperandSyntax( ctx ) )
                {
                    return getExpressionItem( ctx.getText(),
                        new DimensionalItemId( DATA_ELEMENT_OPERAND,
                            ctx.uid0.getText(),
                            ctx.uid1 == null ? null : ctx.uid1.getText(),
                            ctx.uid2 == null ? null : ctx.uid2.getText() ) );
                }
                else
                {
                    return getExpressionItem( ctx.getText(),
                        new DimensionalItemId( DATA_ELEMENT,
                            ctx.uid0.getText() ) );
                }

            case A_BRACE:
                if ( !isExpressionProgramAttribute( ctx ) )
                {
                    throw new ParserExceptionWithoutContext( "Program attribute must have two UIDs: " + ctx.getText() );
                }

                return getExpressionItem( ctx.getText(),
                    new DimensionalItemId( PROGRAM_ATTRIBUTE,
                        ctx.uid0.getText(),
                        ctx.uid1.getText() ) );

            case C_BRACE:
                return getConstant( ctx );

            case D_BRACE:
                return getExpressionItem( ctx.getText(),
                    new DimensionalItemId( PROGRAM_DATA_ELEMENT,
                        ctx.uid0.getText(),
                        ctx.uid1.getText() ) );

            case I_BRACE:
                return getExpressionItem( ctx.getText(),
                    new DimensionalItemId( PROGRAM_INDICATOR,
                        ctx.uid0.getText() ) );

            case N_BRACE:
                return getExpressionItem( ctx.getText(),
                        new DimensionalItemId( INDICATOR,
                                ctx.uid0.getText() ) );

            case OUG_BRACE:
                return getOrgUnitGroupCount( ctx );

            case R_BRACE:
                return getExpressionItem( ctx.getText(),
                    new DimensionalItemId( REPORTING_RATE,
                        ctx.uid0.getText(),
                        ctx.REPORTING_RATE_TYPE().getText() ) );

            case DAYS:
                return DOUBLE_VALUE_IF_NULL;

            default:
                throw new ParserExceptionWithoutContext( "Item not recognized for this type of expression: " + ctx.getText() );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Object getExpressionItem( String exprText, DimensionalItemId itemId )
    {
        if ( itemIds != null )
        {
            itemIds.add( itemId );
        }

        if ( itemDescriptions != null )
        {
            DimensionalItemObject item = dimensionService.getDataDimensionalItemObject( itemId );

            if ( item == null )
            {
                throw new ParserExceptionWithoutContext( "Can't find " + itemId.getDimensionItemType().name() + " for '" + exprText + "'" );
            }

            itemDescriptions.put( exprText, item.getDisplayName() );
        }

        return DOUBLE_VALUE_IF_NULL;
    }

    private Double getConstant( ItemContext ctx )
    {
        if ( itemDescriptions != null )
        {
            Constant constant = constantService.getConstant( ctx.uid0.getText() );

            if ( constant == null )
            {
                throw new ParserExceptionWithoutContext( "No constant defined for " + ctx.getText() );
            }

            itemDescriptions.put( ctx.getText(), constant.getDisplayName() );
        }

        return DOUBLE_VALUE_IF_NULL;
    }

    public Object getOrgUnitGroupCount( ItemContext ctx )
    {
        String orgUnitGroupId = ctx.uid0.getText();

        if ( orgUnitGroupIds != null )
        {
            orgUnitGroupIds.add( orgUnitGroupId );
        }

        if ( itemDescriptions != null )
        {
            OrganisationUnitGroup orgUnitGroup = organisationUnitGroupService
                .getOrganisationUnitGroup( orgUnitGroupId );

            if ( orgUnitGroup == null )
            {
                throw new ParserExceptionWithoutContext(
                    "Can't find organisation unit group " + orgUnitGroupId );
            }

            itemDescriptions
                .put( ctx.getText(), orgUnitGroup.getDisplayName() );
        }

        return DOUBLE_VALUE_IF_NULL;
    }
}
