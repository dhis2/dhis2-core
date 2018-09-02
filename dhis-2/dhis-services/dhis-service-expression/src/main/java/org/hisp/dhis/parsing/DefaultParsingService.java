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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.ListMapMap;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.expression.DefaultExpressionService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.parsing.generated.ExpressionLexer;
import org.hisp.dhis.parsing.generated.ExpressionParser;
import org.hisp.dhis.period.Period;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Parses expressions using the ANTLR parser.
 *
 * @author Jim Grace
 */
public class DefaultParsingService
    implements ParsingService
{
    private static final Log log = LogFactory.getLog( DefaultExpressionService.class );

    @Autowired
    private ConstantService constantService;

    private static Cache<String, ParseTree> EXPRESSION_PARSE_TREES = Caffeine.newBuilder()
        .expireAfterAccess( 10, TimeUnit.MINUTES ).initialCapacity( 10000 )
        .maximumSize( 50000 ).build();

    @Override
    public ListMapMap<OrganisationUnit, Period, DimensionalItemObject> getItemsInExpression(
        List<Expression> expressions, List<OrganisationUnit> orgUnits, List<Period> periods,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap )
    {
        ListMapMap<OrganisationUnit, Period, DimensionalItemObject> items = new ListMapMap<>();

        ExpressionItemsVisitor expressionItemsVisitor = new ExpressionItemsVisitor();

        for ( Expression expression : expressions )
        {
            ParseTree parseTree = getParseTree( expression.getExpression(), true );

            expressionItemsVisitor.getDimensionalItemObjects( parseTree, orgUnits, periods, constantMap, items );
        }

        return items;
    }

    @Override
    public Double getExpressionValue( Expression expression, OrganisationUnit orgUnit, Period period,
        MapMapMap<OrganisationUnit, Period, DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap, int days )
    {
        EvaluationVisitor evaluationVisitor = new EvaluationVisitor();

        ParseTree parseTree = getParseTree( expression.getExpression(), true );

        return evaluationVisitor.getExpressionValue( parseTree, orgUnit, period, valueMap,
            constantMap, orgUnitCountMap, days );
    }

    @Override
    public String getExpressionDescription( String expr )
    {
        ExpressionItemsVisitor expressionItemsVisitor = new ExpressionItemsVisitor();

        ParseTree parseTree = getParseTree( expr, false );

        ListMapMap<OrganisationUnit, Period, DimensionalItemObject> items = new ListMapMap<>();

        return expressionItemsVisitor.getExpressionDescription( parseTree, expr );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private ParseTree getParseTree( String expr, boolean logWarnings )
    {
        ParseTree parseTree = null;

        try
        {
            parseTree = EXPRESSION_PARSE_TREES.get( expr, e -> parse( e ) );
        }
        catch ( ParsingException ex )
        {
            if ( logWarnings )
            {
                log.warn( "Parsing error '" + ex.getMessage() + "' in expression '" + expr + "'" );
            }
            else
            {
                throw ex;
            }
        }

        return parseTree;
    }

    /**
     * Parses an expression into an ANTLR ParseTree.
     *
     * @param expr the expression text to parse.
     * @return the ANTLR parse tree.
     */
    private ParseTree parse( String expr )
    {
        AntlrErrorListener errorListener = new AntlrErrorListener(); // Custom error listener.

        CharStream input = CharStreams.fromString( expr ); // Form an ANTLR lexer input stream.

        ExpressionLexer lexer = new ExpressionLexer( input ); // Create a lexer for the input.

        lexer.removeErrorListeners(); // Remove default lexer error listener (prints to console).
        lexer.addErrorListener( errorListener ); // Add custom error listener to throw any errors.

        CommonTokenStream tokens = new CommonTokenStream( lexer ); // Parse the input into a token stream.

        ExpressionParser parser = new ExpressionParser( tokens ); // Create a parser for the token stream.

        parser.removeErrorListeners(); // Remove the default parser error listener (prints to console).
        parser.addErrorListener( errorListener ); // Add custom error listener to throw any errors.

        return parser.expr(); // Parse the expression and return the parse tree.
    }
}
