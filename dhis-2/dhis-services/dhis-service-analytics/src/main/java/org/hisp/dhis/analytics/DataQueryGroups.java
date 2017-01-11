package org.hisp.dhis.analytics;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.ListMap;

/**
 * Immutable class representing a group of data query parameters. Should be
 * instantiated using the Builder class. Example usage:
 * <p>
 * <pre>
 * {@code
 * DataQueryGroups groups = DataQueryGroups.newBuilder()
 *     .withQueries( queries )
 *     .build();
 * }
 * </pre>
 * 
 * @author Lars Helge Overland
 */
public class DataQueryGroups
{
    private List<DataQueryParams> queries = new ArrayList<>();
    
    private List<List<DataQueryParams>> sequentialQueries = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    private DataQueryGroups()
    {
    }
    
    public static Builder newBuilder()
    {
        return new Builder();
    }
    
    // -------------------------------------------------------------------------
    // Public methods
    // -------------------------------------------------------------------------
    
    /**
     * Gets all queries.
     * 
     * @return all queries.
     */
    public List<DataQueryParams> getAllQueries()
    {
        return queries;
    }
    
    /**
     * Gets groups of queries which should be run in sequence for optimal
     * performance. Currently queries with different aggregation type are run
     * in sequence due to the typical indicator query, where few data elements
     * have the average aggregation operator and many have the sum. Performance
     * will increase if optimal number of queries can be run in parallel for the
     * queries which take most time, which in this case are the ones with sum 
     * aggregation type.
     * 
     * @return groups of queries which should be run in sequence
     */
    public List<List<DataQueryParams>> getSequentialQueries()
    {
        return sequentialQueries;
    }
    
    /**
     * Indicates whether the current number of queries in this group is optimal. 
     * Uses the given optimal query number compared to the size of the largest 
     * query group to determine the outcome.
     * 
     * @return true if the current number of queries in this group is optimal.
     */
    public boolean isOptimal( int optimalQueries )
    {
        return getLargestGroupSize() >= optimalQueries;
    }

    /**
     * Gets the size of the largest query group of the sequential queries.
     * 
     * @return the size of the largest query group of the sequential queries.
     */
    public int getLargestGroupSize()
    {        
        int max = 0;
        
        for ( List<DataQueryParams> list : sequentialQueries )
        {
            max = list.size() > max ? list.size() : max;
        }
        
        return max;
    }
    
    @Override
    public String toString()
    {
        return "[Seq queries: " + sequentialQueries.size() + ", all queries: " + queries.size() + "]";        
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private static ListMap<String, DataQueryParams> getListMap( List<DataQueryParams> queries )
    {
        ListMap<String, DataQueryParams> map = new ListMap<>();
        
        for ( DataQueryParams query : queries )
        {
            map.putValue( query.getSequentialQueryGroupKey(), query );
        }
        
        return map;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static class Builder
    {
        private DataQueryGroups groups;
        
        private Builder()
        {
            groups = new DataQueryGroups();
        }
        
        public Builder withQueries( List<DataQueryParams> queries )
        {
            this.groups.queries = queries;
            this.groups.sequentialQueries.addAll( getListMap( queries ).values() );
            return this;
        }
        
        public DataQueryGroups build()
        {
            return groups;
        }
    }    
}
