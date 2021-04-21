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

import java.util.List;

import org.hisp.dhis.commons.jsonfiltering.name.AnyDeepName;
import org.hisp.dhis.commons.jsonfiltering.name.AnyShallowName;
import org.hisp.dhis.commons.jsonfiltering.name.JsonFilteringName;

import com.google.common.collect.ImmutableList;

/**
 * A json-filtering node represents a component of a filter expression.
 */
public class JsonFilteringNode
{

    private final JsonFilteringName name;

    private final List<JsonFilteringNode> children;

    private final boolean jsonFiltering;

    private final boolean negated;

    private final boolean emptyNested;

    /**
     * Constructor.
     *
     * @param name name of the node
     * @param children child nodes
     * @param negated whether or not the node has been negated
     * @param jsonFiltering whether or not a node is JsonFiltering
     * @param emptyNested whether of not filter specified {}
     * @see #isJsonFiltering()
     */
    public JsonFilteringNode( JsonFilteringName name, List<JsonFilteringNode> children, boolean negated,
        boolean jsonFiltering,
        boolean emptyNested )
    {
        this.name = name;
        this.negated = negated;
        this.children = ImmutableList.copyOf( children );
        this.jsonFiltering = jsonFiltering;
        this.emptyNested = emptyNested;
    }

    /**
     * Performs a match against the name of another node/element.
     *
     * @param otherName the name of the other node
     * @return -1 if no match, MAX_INT if exact match, or positive number for
     *         wildcards
     */
    public int match( String otherName )
    {
        return name.match( otherName );
    }

    /**
     * Get the name of the node.
     *
     * @return name
     */
    public String getName()
    {
        return name.getName();
    }

    /**
     * Get the node's children.
     *
     * @return child nodes
     */
    public List<JsonFilteringNode> getChildren()
    {
        return children;
    }

    /**
     * A node is considered JsonFiltering if it is comes right before a nested
     * expression.
     * <p>
     * For example, given the filter expression:
     * </p>
     * <code>id,foo{bar}</code>
     * <p>
     * The foo node is JsonFiltering, but the bar node is not.
     * </p>
     *
     * @return true/false
     */
    public boolean isJsonFiltering()
    {
        return jsonFiltering;
    }

    /**
     * Says whether this node is **
     *
     * @return true if **, false if not
     */
    public boolean isAnyDeep()
    {
        return AnyDeepName.ID.equals( name.getName() );
    }

    /**
     * Says whether this node is *
     *
     * @return true if *, false if not
     */
    public boolean isAnyShallow()
    {
        return AnyShallowName.ID.equals( name.getName() );
    }

    /**
     * Says whether this node explicitly specified no children. (eg. assignee{})
     *
     * @return true if empty nested, false otherwise
     */
    public boolean isEmptyNested()
    {
        return emptyNested;
    }

    /**
     * Says whether the node started with '-'.
     *
     * @return true if negated false if not
     */
    public boolean isNegated()
    {
        return negated;
    }
}
