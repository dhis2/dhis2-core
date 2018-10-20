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
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.expression.DefaultExpressionService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.parsing.generated.ExpressionLexer;
import org.hisp.dhis.parsing.generated.ExpressionParser;
import org.hisp.dhis.period.Period;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Autowired
    protected OrganisationUnitService organisationUnitService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    private DimensionService dimensionService;

    private static Cache<String, ParseTree> EXPRESSION_PARSE_TREES = Caffeine.newBuilder()
        .expireAfterAccess( 10, TimeUnit.MINUTES ).initialCapacity( 10000 )
        .maximumSize( 50000 ).build();

    @Override
    public Set<ExpressionItem> getExpressionItems(
        List<Expression> expressions, List<OrganisationUnit> orgUnits, List<Period> periods,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap )
    {
        Set<ExpressionItem> items = new HashSet<>();

        ExpressionItemsVisitor expressionItemsVisitor = new ExpressionItemsVisitor();

        for ( Expression expression : expressions )
        {
            ParseTree parseTree = getParseTree( expression.getExpression(), true );

            if ( parseTree != null )
            {
                try
                {
                    expressionItemsVisitor.getExpressionItems( parseTree, orgUnits, periods,
                        constantMap, orgUnitCountMap, items,
                        organisationUnitService, manager, dimensionService );
                }
                catch ( ParsingException ex )
                {
                    log.warn( ex.getMessage() + " getting value of expression '" + expression.getExpression() + "'" );
                }
            }
        }

        return items;
    }

    @Override
    public Set<OrganisationUnitGroup> getExpressionOrgUnitGroups( String expression )
    {
        Set<OrganisationUnitGroup> orgUnitGroups = new HashSet<>();

        ExpressionItemsVisitor expressionItemsVisitor = new ExpressionItemsVisitor();

        ParseTree parseTree = getParseTree( expression, true );

        if ( parseTree != null )
        {
            try
            {
                orgUnitGroups = expressionItemsVisitor.getExpressionOrgUnitGroups(
                    parseTree, organisationUnitService, manager, dimensionService,
                    organisationUnitGroupService );
            }
            catch ( ParsingException ex )
            {
                log.warn( ex.getMessage() + " getting organisation unit groups of expression '" + expression + "'" );
            }
        }

        return orgUnitGroups;
    }

    @Override
    public Double getExpressionValue( Expression expression, OrganisationUnit orgUnit, Period period,
        Map<ExpressionItem, Double> valueMap, Map<String, Double> constantMap,
        Map<String, Integer> orgUnitCountMap, int days )
    {
        ExpressionValueVisitor expressionValueVisitor = new ExpressionValueVisitor();

        ParseTree parseTree = getParseTree( expression.getExpression(), true );

        if ( parseTree == null )
        {
            return null;
        }

        try
        {
            return expressionValueVisitor.getExpressionValue( parseTree, orgUnit, period, valueMap,
                constantMap, orgUnitCountMap, days, organisationUnitService, manager );
        }
        catch ( ParsingException ex )
        {
            log.warn( ex.getMessage() + " finding items in expression '" + expression.getExpression() + "'" );

            return null;
        }
    }

    @Override
    public String getExpressionDescription( String expr )
    {
        ExpressionItemsVisitor expressionItemsVisitor = new ExpressionItemsVisitor();

        ParseTree parseTree = getParseTree( expr, false );

        if ( parseTree == null )
        {
            return null;
        }

        return expressionItemsVisitor.getExpressionDescription( parseTree, expr,
            constantService.getConstantMap(), organisationUnitService, manager, dimensionService,
            constantService, organisationUnitGroupService );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Gets the ANTLR parse tree for the given expression string, from the cache
     * if possible. If there is an exception, it is either logged as a warning,
     * or thrown.
     *
     * @param expr the expression to parse.
     * @param logWarnings whether to log as warnings (or throw) exceptions.
     * @return the ANTLR parse tree, or null if parsing exception.
     */
    private ParseTree getParseTree( String expr, boolean logWarnings )
    {
        try
        {
            return EXPRESSION_PARSE_TREES.get( expr, e -> parse( e ) );
        }
        catch ( ParsingException ex )
        {
            String message = ex.getMessage() + " parsing expression '" + expr + "'";

            if ( logWarnings )
            {
                log.warn( message );
            }
            else
            {
                throw new ParsingException( message );
            }
        }

        return null;
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

        return parser.expression(); // Parse the expression and return the parse tree.
    }
}
