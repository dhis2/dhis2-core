/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.tracker.trackedentityattributevalue;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * This class implements the administrative/maintenance tasks of tracked entity attribute value
 * table.
 *
 * @author Ameen Mohamed
 */
@RequiredArgsConstructor
@Component("org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeTableManager")
public class JdbcTrackedEntityAttributeTableManager implements TrackedEntityAttributeTableManager {
  private static final String TRIGRAM_INDEX_CREATE_QUERY =
      "CREATE INDEX CONCURRENTLY IF NOT EXISTS in_gin_teavalue_%d ON "
          + "trackedentityattributevalue USING gin (trackedentityid,lower(value) gin_trgm_ops) where trackedentityattributeid = %d";

  private final JdbcTemplate jdbcTemplate;

  // -------------------------------------------------------------------------
  // Implementation
  // -------------------------------------------------------------------------

  @Override
  public void createTrigramIndex(TrackedEntityAttribute trackedEntityAttribute) {
    String query =
        String.format(
            TRIGRAM_INDEX_CREATE_QUERY,
            trackedEntityAttribute.getId(),
            trackedEntityAttribute.getId());
    jdbcTemplate.execute(query);
  }

  @Override
  public void dropTrigramIndex(Long teaId) {
    List<String> indexNames =
        jdbcTemplate.queryForList(
            """
        select idx.indexname
        from pg_indexes idx
        where idx.tablename = 'trackedentityattributevalue'
        and idx.indexdef ilike '%gin_trgm_ops%'
        and idx.indexdef ilike '%WHERE%'
        and idx.indexdef ~ ('[(]?\\s*trackedentityattributeid\\s*=\\s*' || ? || '\\s*[)]?')
        """,
            String.class, String.valueOf(teaId));

    indexNames.forEach(name -> jdbcTemplate.execute("DROP INDEX IF EXISTS " + name));
  }

  @Override
  public List<Long> getAttributeIdsWithTrigramIndex() {
    return jdbcTemplate.queryForList(
        """
            select
                cast(
                    substring(idx.indexdef from 'trackedentityattributeid\\s*=\\s*(\\d+)')
                        as bigint
                ) as teaid
            from
                pg_indexes idx
            where
                idx.tablename = 'trackedentityattributevalue'
                and idx.indexdef ilike '%gin_trgm_ops%'
                and idx.indexdef ilike '%WHERE%'
                and idx.indexdef ~ '[(]?\\s*trackedentityattributeid\\s*=\\s*\\d+\\s*[)]?'
            """,
        Long.class);
  }

  @Override
  public void runAnalyzeOnTrackedEntityAttributeValue() {
    jdbcTemplate.execute("ANALYZE trackedentityattributevalue (value)");
  }
}
