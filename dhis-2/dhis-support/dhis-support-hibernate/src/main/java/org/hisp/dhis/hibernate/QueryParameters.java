package org.hisp.dhis.hibernate;

/*
 *
 *  Copyright (c) 2004-2018, University of Oslo
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *  Neither the name of the HISP project nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class QueryParameters implements Serializable
{
    private static final long serialVersionUID = 1L;

    // pagination
    private int maxResults = -1;
    private int first = 0;
    private int pageSize = 0;

    // query properties
    private boolean caseSensitive = true;

    private boolean useDistinct = false;

    // select attributes
    private List<Attribute<?,?>> attributes = new ArrayList<>();

    private List<Function<Root<?>, Predicate>> predicates = new ArrayList<>();

    private List<Function<Root<?>, Order>> orders = new ArrayList<>();

    protected Class<?> clazz;

    private StringSearchMode searchMode;

    private Predicate predicate;

    private Order order;

    // -----------------------------
    // Supporting methods
    // -----------------------------

    // -----------------------------
    // Getters & Setters
    // -----------------------------

    /**
     * Could be overridden programmatically.
     */
    public Class<?> getClazz()
    {
        return clazz;
    }

    public int getMaxResults()
    {
        return this.maxResults;
    }

    public void setMaxResults( int maxResults )
    {
        this.maxResults = maxResults;
    }

    public int getFirst()
    {
        return this.first;
    }

    public void setFirst( int first )
    {
        this.first = first;
    }

    public int getPageSize()
    {
        return this.pageSize;
    }

    public void setPageSize( int pageSize )
    {
        this.pageSize = pageSize;
    }

    public boolean isCaseSensitive()
    {
        return this.caseSensitive;
    }

    public void setCaseSensitive( boolean caseSensitive )
    {
        this.caseSensitive = caseSensitive;
    }

    public boolean isUseDistinct()
    {
        return this.useDistinct;
    }

    public void setUseDistinct( boolean useDistinct )
    {
        this.useDistinct = useDistinct;
    }

    public List<Attribute<?, ?>> getAttributes()
    {
        return this.attributes;
    }

    public void setAttributes( List<Attribute<?, ?>> attributes )
    {
        this.attributes = attributes;
    }

    public StringSearchMode getSearchMode()
    {
        return this.searchMode;
    }

    public void setSearchMode( StringSearchMode searchMode )
    {
        this.searchMode = searchMode;
    }

    public Predicate getPredicate()
    {
        return this.predicate;
    }

    public void setPredicate( Predicate predicate )
    {
        this.predicate = predicate;
    }

    public Order getOrder()
    {
        return this.order;
    }

    public void setOrder( Order order )
    {
        this.order = order;
    }

    public List<Function<Root<?>, Predicate>> getPredicates()
    {
        return this.predicates;
    }

    public void setPredicates( List<Function<Root<?>, Predicate>> predicates )
    {
        this.predicates = predicates;
    }

    public List<Function<Root<?>, Order>> getOrders()
    {
        return this.orders;
    }

    public void setOrders( List<Function<Root<?>, Order>> orders )
    {
        this.orders = orders;
    }
}
