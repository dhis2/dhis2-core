package org.hisp.dhis.parser.expression;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.*;

import java.util.List;

import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.parser.expression.dataitem.ItemConstant;
import org.hisp.dhis.parser.expression.function.FunctionFirstNonNull;
import org.hisp.dhis.parser.expression.function.FunctionGreatest;
import org.hisp.dhis.parser.expression.function.FunctionIf;
import org.hisp.dhis.parser.expression.function.FunctionIsNotNull;
import org.hisp.dhis.parser.expression.function.FunctionIsNull;
import org.hisp.dhis.parser.expression.function.FunctionLeast;
import org.hisp.dhis.parser.expression.function.PeriodOffset;
import org.hisp.dhis.parser.expression.operator.*;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * Utilities for ANTLR parsing
 *
 * @author Jim Grace
 */
public class ParserUtils
{
    public final static double DOUBLE_VALUE_IF_NULL = 0.0;

    public final static ImmutableMap<Integer, ExpressionItem> COMMON_EXPRESSION_ITEMS = ImmutableMap
        .<Integer, ExpressionItem> builder()

        // Non-comparison operators

        .put( PAREN, new OperatorGroupingParentheses() )
        .put( PLUS, new OperatorMathPlus() )
        .put( MINUS, new OperatorMathMinus() )
        .put( POWER, new OperatorMathPower() )
        .put( MUL, new OperatorMathMultiply() )
        .put( DIV, new OperatorMathDivide() )
        .put( MOD, new OperatorMathModulus() )
        .put( NOT, new OperatorLogicalNot() )
        .put( EXCLAMATION_POINT, new OperatorLogicalNot() )
        .put( AND, new OperatorLogicalAnd() )
        .put( AMPERSAND_2, new OperatorLogicalAnd() )
        .put( OR, new OperatorLogicalOr() )
        .put( VERTICAL_BAR_2, new OperatorLogicalOr() )

        // Comparison operators

        .put( EQ, new OperatorCompareEqual() )
        .put( NE, new OperatorCompareNotEqual() )
        .put( GT, new OperatorCompareGreaterThan() )
        .put( LT, new OperatorCompareLessThan() )
        .put( GEQ, new OperatorCompareGreaterThanOrEqual() )
        .put( LEQ, new OperatorCompareLessThanOrEqual() )

        // Functions

        .put( FIRST_NON_NULL, new FunctionFirstNonNull() )
        .put( GREATEST, new FunctionGreatest() )
        .put( IF, new FunctionIf() )
        .put( IS_NOT_NULL, new FunctionIsNotNull() )
        .put( IS_NULL, new FunctionIsNull() )
        .put( LEAST, new FunctionLeast() )
        .put( PERIOD_OFFSET, new PeriodOffset() )

        // Data items

        .put( C_BRACE, new ItemConstant() )

        .build();

    public final static ExpressionItemMethod ITEM_GET_DESCRIPTIONS = ExpressionItem::getDescription;

    public final static ExpressionItemMethod ITEM_GET_IDS = ExpressionItem::getItemId;

    public final static ExpressionItemMethod ITEM_GET_ORG_UNIT_GROUPS = ExpressionItem::getOrgUnitGroup;

    public final static ExpressionItemMethod ITEM_EVALUATE = ExpressionItem::evaluate;

    public final static ExpressionItemMethod ITEM_GET_SQL = ExpressionItem::getSql;

    public final static ExpressionItemMethod ITEM_REGENERATE = ExpressionItem::regenerate;

    /**
     * Used for syntax checking when we don't have a list of actual periods for
     * collecting samples.
     */
    public final static List<Period> DEFAULT_SAMPLE_PERIODS = Lists
        .newArrayList( PeriodType.getPeriodFromIsoString( "20010101" ) );

    /**
     * Assume that an item of the form #{...} has a syntax that could be used in a
     * program indicator expression for #{programStageUid.dataElementUid}
     *
     * @param ctx the item context
     */
    public static void assumeStageElementSyntax( ExprContext ctx )
    {
        if ( ctx.uid0 == null || ctx.uid1 == null || ctx.uid2 != null || ctx.wild2 != null )
        {
            throw new ParserExceptionWithoutContext( "Invalid Program Stage / DataElement syntax: " + ctx.getText() );
        }
    }

    /**
     * Assume that an item of the form A{...} has a syntax that could be used in an
     * expression for A{progamUid.attributeUid}
     *
     * @param ctx the item context
     */
    public static void assumeExpressionProgramAttribute( ExprContext ctx )
    {
        if ( ctx.uid0 == null || ctx.uid1 == null )
        {
            throw new ParserExceptionWithoutContext( "Program attribute must have two UIDs: " + ctx.getText() );
        }
    }

    /**
     * Assume that an item of the form A{...} has a syntax that could be used be
     * used in an program expression for A{attributeUid}
     *
     * @param ctx the item context
     */
    public static void assumeProgramExpressionProgramAttribute( ExprContext ctx )
    {
        if ( ctx.uid0 == null || ctx.uid1 != null )
        {
            throw new ParserExceptionWithoutContext( "Program attribute must have one UID: " + ctx.getText() );
        }
    }
}
