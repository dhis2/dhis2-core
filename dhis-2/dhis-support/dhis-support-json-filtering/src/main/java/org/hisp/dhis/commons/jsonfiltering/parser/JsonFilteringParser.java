/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.commons.jsonfiltering.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.commons.jsonfiltering.config.JsonFilteringConfig;
import org.hisp.dhis.commons.jsonfiltering.name.AnyDeepName;
import org.hisp.dhis.commons.jsonfiltering.name.AnyShallowName;
import org.hisp.dhis.commons.jsonfiltering.name.ExactName;
import org.hisp.dhis.commons.jsonfiltering.name.JsonFilteringName;
import org.hisp.dhis.commons.jsonfiltering.name.RegexName;
import org.hisp.dhis.commons.jsonfiltering.name.WildcardName;
import org.hisp.dhis.commons.jsonfiltering.util.antlr4.ThrowingErrorListener;
import org.hisp.dhis.commons.jsonfiltering.view.PropertyView;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * The parser takes a filter expression and compiles it to an Abstract Syntax
 * Tree (AST). In this parser's case, the tree doesn't have a root node but
 * rather just returns top level nodes.
 */
public class JsonFilteringParser
{

    // Caches parsed filter expressions
    private static final Cache<String, List<JsonFilteringNode>> CACHE;

    static
    {
        CACHE = CacheBuilder.from( JsonFilteringConfig.getPARSER_NODE_CACHE_SPEC() ).build();
    }

    /**
     * Parse a filter expression.
     *
     * @param filter the filter expression
     * @return compiled nodes
     */
    public List<JsonFilteringNode> parse( String filter )
    {
        filter = StringUtils.trim( filter );

        if ( StringUtils.isEmpty( filter ) )
        {
            return Collections.emptyList();
        }

        // get it from the cache if we can
        List<JsonFilteringNode> cachedNodes = CACHE.getIfPresent( filter );

        if ( cachedNodes != null )
        {
            return cachedNodes;
        }

        JsonFilteringExpressionLexer lexer = ThrowingErrorListener
            .overwrite( new JsonFilteringExpressionLexer( CharStreams.fromString( filter ) ) );
        JsonFilteringExpressionParser parser = ThrowingErrorListener
            .overwrite( new JsonFilteringExpressionParser( new CommonTokenStream( lexer ) ) );

        Visitor visitor = new Visitor();
        List<JsonFilteringNode> nodes = Collections.unmodifiableList( visitor.visit( parser.parse() ) );

        CACHE.put( filter, nodes );
        return nodes;
    }

    private MutableNode analyze( MutableNode node )
    {
        Map<MutableNode, MutableNode> nodesToAdd = new IdentityHashMap<>();
        MutableNode analyze = analyze( node, nodesToAdd );

        for ( Map.Entry<MutableNode, MutableNode> entry : nodesToAdd.entrySet() )
        {
            entry.getKey().addChild( entry.getValue() );
        }

        return analyze;
    }

    private MutableNode analyze( MutableNode node, Map<MutableNode, MutableNode> nodesToAdd )
    {
        if ( node.children != null && !node.children.isEmpty() )
        {
            boolean allNegated = true;

            for ( MutableNode child : node.children.values() )
            {
                if ( !child.negated && !child.negativeParent )
                {
                    allNegated = false;
                    break;
                }
            }

            if ( allNegated )
            {
                nodesToAdd.put( node, new MutableNode( newBaseViewName() ).pathDotted( node.pathDotted ) );
            }

            for ( MutableNode child : node.children.values() )
            {
                analyze( child, nodesToAdd );
            }
        }

        return node;
    }

    private ExactName newBaseViewName()
    {
        return new ExactName( PropertyView.BASE_VIEW );
    }

    private class Visitor extends JsonFilteringExpressionBaseVisitor<List<JsonFilteringNode>>
    {
        @Override
        public List<JsonFilteringNode> visitParse( JsonFilteringExpressionParser.ParseContext ctx )
        {
            MutableNode root = new MutableNode( new ExactName( "root" ) ).pathDotted( true );
            handleExpressionList( ctx.expression_list(), root );
            MutableNode analyzedRoot = analyze( root );
            return analyzedRoot.toJsonFilteringNode().getChildren();
        }

        private void handleExpressionList( JsonFilteringExpressionParser.Expression_listContext ctx,
            MutableNode parent )
        {
            List<JsonFilteringExpressionParser.ExpressionContext> expressions = ctx.expression();

            for ( JsonFilteringExpressionParser.ExpressionContext expressionContext : expressions )
            {
                handleExpression( expressionContext, parent );
            }
        }

        private void handleExpression( JsonFilteringExpressionParser.ExpressionContext ctx, MutableNode parent )
        {

            if ( ctx.negated_expression() != null )
            {
                handleNegatedExpression( ctx.negated_expression(), parent );
            }

            List<JsonFilteringName> names;

            if ( ctx.field() != null )
            {
                names = Collections.singletonList( createName( ctx.field() ) );
            }
            else if ( ctx.dot_path() != null )
            {
                parent.jsonFilter = true;
                for ( int i = 0; i < ctx.dot_path().field().size() - 1; i++ )
                {
                    parent = parent
                        .addChild( new MutableNode( createName( ctx.dot_path().field( i ) ) ).pathDotted( true ) );
                    parent.jsonFilter = true;
                }
                names = Collections
                    .singletonList( createName( ctx.dot_path().field().get( ctx.dot_path().field().size() - 1 ) ) );
            }
            else if ( ctx.field_list() != null )
            {
                names = new ArrayList<>( ctx.field_list().field().size() );
                for ( JsonFilteringExpressionParser.FieldContext fieldContext : ctx.field_list().field() )
                {
                    names.add( createName( fieldContext ) );
                }
            }
            else if ( ctx.deep() != null )
            {
                names = Collections.singletonList( AnyDeepName.get() );
            }
            else
            {
                names = Collections.emptyList();
            }

            for ( JsonFilteringName name : names )
            {
                MutableNode node = parent.addChild( new MutableNode( name ) );

                if ( ctx.empty_nested_expression() != null )
                {
                    node.emptyNested = true;
                }
                else if ( ctx.nested_expression() != null )
                {
                    node.jsonFilter = true;
                    handleExpressionList( ctx.nested_expression().expression_list(), node );
                }
            }
        }

        private JsonFilteringName createName( JsonFilteringExpressionParser.FieldContext ctx )
        {
            JsonFilteringName name;

            if ( ctx.exact_field() != null )
            {
                name = new ExactName( ctx.getText() );
            }
            else if ( ctx.wildcard_field() != null )
            {
                name = new WildcardName( ctx.getText() );
            }
            else if ( ctx.regex_field() != null )
            {
                String regexPattern = ctx.regex_field().regex_pattern().getText();
                Set<String> regexFlags = new HashSet<>( ctx.regex_field().regex_flag().size() );

                for ( JsonFilteringExpressionParser.Regex_flagContext regex_flagContext : ctx.regex_field()
                    .regex_flag() )
                {
                    regexFlags.add( regex_flagContext.getText() );
                }

                name = new RegexName( regexPattern, regexFlags );
            }
            else if ( ctx.wildcard_shallow_field() != null )
            {
                name = AnyShallowName.get();
            }
            else
            {
                throw new IllegalArgumentException( "Unhandled field: " + ctx.getText() );
            }

            return name;
        }

        private void handleNegatedExpression( JsonFilteringExpressionParser.Negated_expressionContext ctx,
            MutableNode parent )
        {
            if ( ctx.field() != null )
            {
                parent.addChild( new MutableNode( createName( ctx.field() ) ).negated( true ) );
            }
            else if ( ctx.dot_path() != null )
            {
                for ( int i = 0; i < ctx.dot_path().field().size(); i++ )
                {
                    JsonFilteringExpressionParser.FieldContext fieldContext = ctx.dot_path().field( i );
                    parent.jsonFilter = true;

                    MutableNode mutableNode = new MutableNode( createName( fieldContext ) );
                    mutableNode.negativeParent = true;

                    parent = parent.addChild( mutableNode.pathDotted( true ) );
                }

                parent.negated( true );
                parent.negativeParent = false;
            }
        }

    }

    private static class MutableNode
    {
        public boolean negativeParent;

        private final JsonFilteringName name;

        private boolean negated;

        private boolean jsonFilter;

        private boolean emptyNested;

        private Map<String, MutableNode> children;

        private boolean pathDotted;

        MutableNode( JsonFilteringName name )
        {
            this.name = name;
        }

        JsonFilteringNode toJsonFilteringNode()
        {
            if ( name == null )
            {
                throw new IllegalArgumentException( "No Names specified" );
            }

            List<JsonFilteringNode> childNodes;

            if ( children == null || children.isEmpty() )
            {
                childNodes = Collections.emptyList();
            }
            else
            {
                childNodes = new ArrayList<>( children.size() );

                for ( MutableNode child : children.values() )
                {
                    childNodes.add( child.toJsonFilteringNode() );
                }

            }

            return newJsonFilteringNode( name, childNodes );
        }

        private JsonFilteringNode newJsonFilteringNode( JsonFilteringName name, List<JsonFilteringNode> childNodes )
        {
            return new JsonFilteringNode( name, childNodes, negated, jsonFilter, emptyNested );
        }

        public MutableNode pathDotted( boolean pathDotted )
        {
            this.pathDotted = pathDotted;
            return this;
        }

        public MutableNode negated( boolean negated )
        {
            this.negated = negated;
            return this;
        }

        public MutableNode addChild( MutableNode childToAdd )
        {
            if ( children == null )
            {
                children = new LinkedHashMap<>();
            }

            String name = childToAdd.name.getName();
            MutableNode existingChild = children.get( name );

            if ( existingChild == null )
            {
                children.put( name, childToAdd );
            }
            else
            {
                if ( childToAdd.children != null )
                {

                    if ( existingChild.children == null )
                    {
                        existingChild.children = childToAdd.children;
                    }
                    else
                    {
                        existingChild.children.putAll( childToAdd.children );
                    }
                }

                existingChild.jsonFilter = existingChild.jsonFilter || childToAdd.jsonFilter;
                existingChild.emptyNested = existingChild.emptyNested && childToAdd.emptyNested;
                existingChild.pathDotted = existingChild.pathDotted && childToAdd.pathDotted;
                existingChild.negativeParent = existingChild.negativeParent && childToAdd.negativeParent;
                childToAdd = existingChild;
            }

            if ( !childToAdd.pathDotted && pathDotted )
            {
                pathDotted = false;
            }

            return childToAdd;
        }
    }
}
