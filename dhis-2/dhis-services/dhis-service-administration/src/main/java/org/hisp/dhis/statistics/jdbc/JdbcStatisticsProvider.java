package org.hisp.dhis.statistics.jdbc;

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

import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.common.Objects;
import org.hisp.dhis.statistics.StatisticsProvider;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class JdbcStatisticsProvider
    implements StatisticsProvider
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    /**
     * Read only JDBC template.
     */
    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    // -------------------------------------------------------------------------
    // StatisticsProvider implementation
    // -------------------------------------------------------------------------
    
    @Override
    public Map<Objects, Integer> getObjectCounts()
    {
        final Map<Objects, Integer> objectCounts = new HashMap<>();
        
        objectCounts.put( Objects.DATAELEMENT, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM dataelement", Integer.class ) );
        objectCounts.put( Objects.DATAELEMENTGROUP, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM dataelementgroup", Integer.class ) );
        objectCounts.put( Objects.INDICATORTYPE, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM indicatortype", Integer.class ) );
        objectCounts.put( Objects.INDICATOR, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM indicator", Integer.class ) );
        objectCounts.put( Objects.INDICATORGROUP, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM indicatorgroup", Integer.class ) );
        objectCounts.put( Objects.DATASET, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM dataset", Integer.class ) );
        objectCounts.put( Objects.ORGANISATIONUNIT, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM organisationunit", Integer.class ) );
        objectCounts.put( Objects.ORGANISATIONUNITGROUP, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM orgunitgroup", Integer.class ) );
        objectCounts.put( Objects.VALIDATIONRULE, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM validationrule", Integer.class ) );
        objectCounts.put( Objects.PERIOD, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM period", Integer.class ) );
        objectCounts.put( Objects.USER, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM users", Integer.class ) );
        objectCounts.put( Objects.USERGROUP, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM usergroup", Integer.class ) );
        objectCounts.put( Objects.REPORTTABLE, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM reporttable", Integer.class ) );
        objectCounts.put( Objects.CHART, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM chart", Integer.class ) );
        objectCounts.put( Objects.MAP, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM map", Integer.class ) );
        objectCounts.put( Objects.DASHBOARD, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM dashboard", Integer.class ) );        
        objectCounts.put( Objects.DATAVALUE, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM datavalue dv where dv.deleted is false", Integer.class ) );
        objectCounts.put( Objects.PROGRAM, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM program", Integer.class ) );
        objectCounts.put( Objects.PROGRAMSTAGEINSTANCE, jdbcTemplate.queryForObject( "SELECT COUNT(*) FROM programstageinstance", Integer.class ) );
        
        return objectCounts;
    }
}
