package org.hisp.dhis.analytics.partition;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import static org.hisp.dhis.analytics.AnalyticsTableManager.ANALYTICS_TABLE_NAME;
import static org.hisp.dhis.analytics.AnalyticsTableManager.EVENT_ANALYTICS_TABLE_NAME;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Lars Helge Overland
 */
public class JdbcPartitionManager
    implements PartitionManager
{
    private static final Log log = LogFactory.getLog( JdbcPartitionManager.class );
    
    private Set<String> analyticsPartitions = null;
    private Set<String> analyticsEventPartitions = null;

    @Autowired
    private JdbcTemplate jdbcTemplate;
  
    @Override
    public Set<String> getAnalyticsPartitions()
    {
        if ( analyticsPartitions != null )
        {
            return analyticsPartitions;
        }
                
        final String sql =
            "select table_name from information_schema.tables " +
            "where table_name like '" + ANALYTICS_TABLE_NAME + "%' " +
            "and table_type = 'BASE TABLE'";
        
        log.info( "Information schema analytics SQL: " + sql );

        Set<String> partitions = new HashSet<>( jdbcTemplate.queryForList( sql, String.class ) );
        analyticsPartitions = partitions;
        return partitions;
    }
    
    @Override
    public Set<String> getEventAnalyticsPartitions()
    {
        if ( analyticsEventPartitions != null )
        {
            return analyticsEventPartitions;
        }
        
        final String sql = 
            "select table_name from information_schema.tables " +
            "where table_name like '" + EVENT_ANALYTICS_TABLE_NAME + "%' " +
            "and table_type = 'BASE TABLE'";
        
        log.info( "Information schema event analytics SQL: " + sql );
        
        Set<String> partitions = new HashSet<>( jdbcTemplate.queryForList( sql, String.class ) );
        analyticsEventPartitions = partitions;
        return partitions;
    }
    
    @Override
    public void clearCaches()
    {
        analyticsPartitions = null;
        analyticsEventPartitions = null;
    }
}
