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
package org.hisp.dhis.tracker.table;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeTableManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * This class manages the administrative/maintenance tasks of tracked entity
 * attribute table.
 *
 * @author Ameen Mohamed
 */
@Slf4j
@Service
public class JdbcTrackedEntityAttributeTableManager implements TrackedEntityAttributeTableManager
{

    private static final String TRIGRAM_INDEX_QUERY = "CREATE INDEX CONCURRENTLY IF NOT EXISTS in_gin_teavalue_%d ON "
        + "trackedentityattributevalue USING gin (trackedentityinstanceid,lower(value) gin_trgm_ops) where trackedentityattributeid = %d";

    private static final String VACUUM_QUERY = "VACUUM trackedentityattributevalue";

    private static final String ANALYZE_QUERY = "ANALYZE trackedentityattributevalue";

    private final JdbcTemplate jdbcTemplate;

    public JdbcTrackedEntityAttributeTableManager( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    public void createTrigramIndex( TrackedEntityAttribute trackedEntityAttribute )
    {
        String query = String.format( TRIGRAM_INDEX_QUERY, trackedEntityAttribute.getId(),
            trackedEntityAttribute.getId() );
        jdbcTemplate.execute( query );
    }

    public void runAnalyze()
    {
        jdbcTemplate.execute( ANALYZE_QUERY );
    }

    public void runVacuum()
    {
        jdbcTemplate.execute( VACUUM_QUERY );
    }
}
