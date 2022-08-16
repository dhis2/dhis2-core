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
package org.hisp.dhis.analytics.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Collections.*;
import static org.hisp.dhis.common.DimensionItemType.INDICATOR;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.*;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.indicator.Indicator;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.scalified.tree.TreeNode;
import com.scalified.tree.multinode.ArrayMultiTreeNode;

/**
 * Builds a tree structure representing nested Indicators and detects if an
 * Indicator has a cyclic dependency within the same tree.
 *
 * See tests for different sequences of nested Indicators
 *
 * TODO: This test is expensive to do query-time, instead move to integrity
 * check or metadata import validation.
 *
 * @author Luciano Fiandesio
 */
@Component
public class NestedIndicatorCyclicDependencyInspector
{
    private final ExpressionService expressionService;

    private final DimensionService dimensionService;

    public NestedIndicatorCyclicDependencyInspector( DimensionService dimensionService,
        ExpressionService expressionService )
    {
        checkNotNull( expressionService );
        checkNotNull( dimensionService );

        this.dimensionService = dimensionService;
        this.expressionService = expressionService;
    }

    private final static String ERROR_STRING = "An Indicator with identifier '%s' has a cyclic reference to another Indicator in the Nominator or Denominator expression";

    /**
     * Initiate the inspection, by invoking the recursive 'inspect' function.
     *
     * @param dimensionalItemObjects a List of root
     *        {@link DimensionalItemObject} as Indicators.
     */
    public void inspect( List<DimensionalItemObject> dimensionalItemObjects )
    {
        List<Indicator> indicators = asTypedList( dimensionalItemObjects.stream()
            .filter( d -> d.getDimensionItemType().equals( INDICATOR ) ).collect( Collectors.toList() ) );

        for ( Indicator indicator : indicators )
        {
            addDescendants( indicator, new ArrayMultiTreeNode<>( indicator.getUid() ), indicator.getUid() );
        }
    }

    /**
     * Recursively add all the given Indicator's nested Indicators (if any) to
     * the tree.
     *
     * @param indicator The Indicator to add to the main Indicators tree.
     * @param tree the complete Indicator tree.
     * @param parent the UID of the parent node to which the Indicator is added.
     */
    private void addDescendants( Indicator indicator, TreeNode<String> tree, String parent )
    {
        // Get a list of indicators from the current Indicator

        List<Indicator> indicators = getDescendants( indicator );

        if ( !indicators.isEmpty() )
        {
            add( indicators, tree, parent );
            for ( Indicator innerIndicator : indicators )
            {
                addDescendants( innerIndicator, tree, innerIndicator.getUid() );
            }
        }
    }

    /**
     * Add the List of Indicators as Nodes to the given Tree. Fails if any of
     * the indicator UIDs is already present in the tree as direct ancestors.
     *
     * @param indicators list of Indicators to add to the tree.
     * @param tree the full tree built so far.
     * @param parent the UID of the parent node to which to attach the
     *        indicators.
     */
    public void add( List<Indicator> indicators, TreeNode<String> tree, String parent )
    {
        for ( Indicator indicator : indicators )
        {
            // Find the parent node to attach indicators
            TreeNode<String> parentNode = tree.find( parent );
            if ( parentNode == null )
            {
                return;
            }
            if ( !parentNode.isRoot() )
            {
                TreeNode<String> mNode = parentNode;

                // Navigate backward from parent node to verify that an ancestor
                // does not have the same UID as current indicator
                do
                {
                    mNode = mNode.parent();
                    if ( indicator.getUid().equals( mNode.data() ) )
                    {
                        throw new CyclicReferenceException( format( ERROR_STRING, indicator.getUid() ) );
                    }
                }
                while ( !mNode.isRoot() );
            }

            // Check that node to add does not have the same value as parent
            if ( parentNode.data().equals( indicator.getUid() ) )
            {
                throw new CyclicReferenceException( format( ERROR_STRING, indicator.getUid() ) );
            }
            else
            {
                parentNode.add( new ArrayMultiTreeNode<>( indicator.getUid() ) );
            }
        }
    }

    /**
     * Fetch the indicators referenced in the numerator and denominator
     * expression for the given indicator.
     *
     * @param indicator an {@link Indicator}.
     * @return a List of direct descendants indicators of the current indicator,
     *         or an empty List if the current indicator has no descendants.
     */
    private List<Indicator> getDescendants( Indicator indicator )
    {
        Set<DimensionalItemId> expressionDataElements = Sets.union(
            expressionService.getExpressionDimensionalItemIds( indicator.getNumerator(), INDICATOR_EXPRESSION ),
            expressionService.getExpressionDimensionalItemIds( indicator.getDenominator(), INDICATOR_EXPRESSION ) );

        if ( !expressionDataElements.isEmpty() )
        {
            return asTypedList( dimensionService.getDataDimensionalItemObjectMap( expressionDataElements ).values()
                .stream().filter( d -> d.getDimensionItemType().equals( INDICATOR ) ).collect( Collectors.toList() ) );
        }
        else
        {
            return emptyList();
        }
    }
}
