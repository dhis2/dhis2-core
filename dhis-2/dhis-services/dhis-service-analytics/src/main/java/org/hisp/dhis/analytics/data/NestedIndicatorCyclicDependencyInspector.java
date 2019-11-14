/*
 * Copyright (c) 2004-2019, University of Oslo
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

import static java.lang.String.format;
import static org.hisp.dhis.common.DimensionItemType.INDICATOR;
import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import org.hisp.dhis.common.CyclicReferenceException;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.ParseType;
import org.hisp.dhis.indicator.Indicator;

import com.scalified.tree.TreeNode;
import com.scalified.tree.multinode.ArrayMultiTreeNode;

/**
 * Builds a tree structure representing nested Indicators and detects if an Indicator has a cyclic dependency
 * within the same tree.
 *
 *
 * @author Luciano Fiandesio
 */
public class NestedIndicatorCyclicDependencyInspector
{
    /**
     * Holds the tree structure of the nested indicators
     */
    private List<TreeNode<String>> nestedIndicatorTrees;

    private ExpressionService expressionService;

    private final static String ERROR_STRING = "An Indicator with identifier '%s' has a cyclic reference to another Indicator in the Nominator or Denominator expression";

    /**
     * Initialize the component
     *
     * @param indicators a List of Indicators representing the root of the trees. Each Indicator passed in the
     *                   constructor will create a new tree structure
     *
     * @param expressionService the {@see ExpressionService} required to resolve expressions
     */
    public NestedIndicatorCyclicDependencyInspector( List<Indicator> indicators, ExpressionService expressionService )
    {
        this.expressionService = expressionService;
        // init the tree structures
        resetTrees();
        // add root nodes represented by the Indicators
        for ( Indicator indicator : indicators )
        {
            nestedIndicatorTrees.add( getNode( indicator ) );
        }
    }

    public void add( List<Indicator> indicators )
    {
        List<String> alreadyAdded = new ArrayList<>();

        for ( Indicator indicator : indicators )
        {
            if ( !alreadyAdded.contains( indicator.getUid() ) )
            {
                for ( TreeNode<String> node : nestedIndicatorTrees )
                {
                    TreeNode<String> root = node.root();
                    TreeNode<String> nodeToReplace = root.find( indicator.getUid() );

                    if ( nodeToReplace != null && !nodeToReplace.isRoot() )
                    {
                        if ( nodeToReplace.isLeaf() )
                        {
                            // Replace the "write-ahead" node with the "real" node
                            TreeNode<String> parent = nodeToReplace.parent();
                            root.remove( nodeToReplace );
                            parent.add( getNode( indicator ) );
                            alreadyAdded.add( indicator.getUid() );
                            break;
                        }
                        // If the node is not leaf, it means that it was found
                        // up in the tree structure, therefore we need to
                        // throw an exception
                        else
                        {
                            throw new CyclicReferenceException( format( ERROR_STRING, nodeToReplace.data() ) );
                        }
                    }
                }
            }
        }
    }

    /**
     * Create a Node from an Indicator.
     *
     * @param indicator an instance of {@see Indicator}
     * @return a TreeNode
     */
    private TreeNode<String> getNode( Indicator indicator )
    {
        // Create the Node using the Indicator UID as value
        TreeNode<String> node = new ArrayMultiTreeNode<>( indicator.getUid() );

        // Add to the newly created node all the DimensionalItems found in the numerator
        // and denominator expressions as
        // child nodes ("Write-ahead"). The write-ahead nodes are required to "connect"
        // the next iteration of Indicators
        //

        // Merge numerator and denominator
        Set<DimensionalItemId> expressionDataElements = Sets.union(
            expressionService.getExpressionDimensionalItemIds( indicator.getNumerator(), INDICATOR_EXPRESSION ),
            expressionService.getExpressionDimensionalItemIds( indicator.getDenominator(), INDICATOR_EXPRESSION ) );

        for ( DimensionalItemId dimensionalItemId : expressionDataElements )
        {
            // Only add Indicators to the tree, since Indicators can be nested
            if ( dimensionalItemId.getDimensionItemType().equals( INDICATOR ) )
            {
                // Throw exception if the child node is equal to the parent ( A --> A )
                if ( !node.data().equals( dimensionalItemId.getId0() ) )
                {
                    node.add( new ArrayMultiTreeNode<>( dimensionalItemId.getId0() ) );
                }
                else
                {
                    throw new CyclicReferenceException( format( ERROR_STRING, node.data() ) );
                }
            }
        }

        return node;
    }

    private void resetTrees()
    {
        nestedIndicatorTrees = new ArrayList<>();
    }
}
