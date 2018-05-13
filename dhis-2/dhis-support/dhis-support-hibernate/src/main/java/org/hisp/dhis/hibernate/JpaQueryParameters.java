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

import javax.persistence.criteria.Expression;
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
public class JpaQueryParameters<T> implements Serializable
{
    private static final long serialVersionUID = 1L;

    // pagination
    private int maxResults ;
    private int firstResult ;
    private int pageSize;

    // query properties
    private boolean caseSensitive = true;

    private boolean useDistinct = false;

    private boolean cachable = false;

    // select attributes
    private List<Attribute<?,?>> attributes = new ArrayList<>();

    private List<Function<Root<T>, Predicate>> predicates = new ArrayList<>();

    private List<Function<Root<T>, Order>> orders = new ArrayList<>();

    private List<Function<Root<T>, Expression<Long>>> countExpressions = new ArrayList<>();

    protected Class clazz;

    private JpaUtils.StringSearchMode searchMode;

    private boolean withSharing = false;

    private Order order;

    public JpaQueryParameters()
    {
        firstResult = -1;
        maxResults = -1;
    }

    // -----------------------------
    // Supporting methods
    // -----------------------------

    public JpaQueryParameters<T> addPredicate( Function<Root<T>, Predicate> predicate )
    {
        predicates.add( predicate );
        return this;
    }

    public JpaQueryParameters<T> addPredicates( List<Function<Root<T>, Predicate>> predicates )
    {
        this.predicates.addAll( predicates );
        return this;
    }

    public JpaQueryParameters<T> addOrder( Function<Root<T>, Order> order )
    {
        orders.add( order );
        return this;
    }

    public JpaQueryParameters<T> count( Function<Root<T>, Expression<Long>> countExpression )
    {
        countExpressions.add( countExpression );
        return this;
    }

    public boolean hasPaging()
    {
        return firstResult > -1 && maxResults > -1 ;
    }

    public boolean hasFirstResult()
    {
        return firstResult > -1 ;
    }

    public boolean hasMaxResult()
    {
        return maxResults > -1;
    }


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
        return maxResults >= 0 ? maxResults : 0;
    }

    public JpaQueryParameters<T> setMaxResults( int maxResults )
    {
        this.maxResults = maxResults;
        return this;
    }

    public int getFirstResult()
    {
        return firstResult;
    }

    public JpaQueryParameters<T> setFirstResult( int firstResult )
    {
        this.firstResult = firstResult;
        return this;
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

    public JpaQueryParameters<T> setCaseSensitive( boolean caseSensitive )
    {
        this.caseSensitive = caseSensitive;
        return this;
    }

    public boolean isUseDistinct()
    {
        return this.useDistinct;
    }

    public JpaQueryParameters<T> setUseDistinct( boolean useDistinct )
    {
        this.useDistinct = useDistinct;
        return this;
    }

    public List<Attribute<?, ?>> getAttributes()
    {
        return this.attributes;
    }

    public void setAttributes( List<Attribute<?, ?>> attributes )
    {
        this.attributes = attributes;
    }

    public JpaUtils.StringSearchMode getSearchMode()
    {
        return this.searchMode;
    }

    public void setSearchMode( JpaUtils.StringSearchMode searchMode )
    {
        this.searchMode = searchMode;
    }

    public Order getOrder()
    {
        return this.order;
    }

    public void setOrder( Order order )
    {
        this.order = order;
    }

    public List<Function<Root<T>, Predicate>> getPredicates()
    {
        return predicates;
    }

    public void setPredicates( List<Function<Root<T>, Predicate>> predicates )
    {
        this.predicates = predicates;
    }

    public List<Function<Root<T>, Order>> getOrders()
    {
        return orders;
    }

    public void setOrders( List<Function<Root<T>, Order>> orders )
    {
        this.orders = orders;
    }

    public boolean isCachable()
    {
        return this.cachable;
    }

    public JpaQueryParameters<T> setCachable( boolean cachable )
    {
        this.cachable = cachable;
        return this;
    }

    public boolean isWithSharing()
    {
        return withSharing;
    }

    public JpaQueryParameters<T> setWithSharing( boolean withSharing )
    {
        this.withSharing = withSharing;
        return this;
    }

    public List<Function<Root<T>, Expression<Long>>> getCountExpressions()
    {
        return countExpressions;
    }

    public void setCountExpressions( List<Function<Root<T>, Expression<Long>>> countExpressions )
    {
        this.countExpressions = countExpressions;
    }
}
