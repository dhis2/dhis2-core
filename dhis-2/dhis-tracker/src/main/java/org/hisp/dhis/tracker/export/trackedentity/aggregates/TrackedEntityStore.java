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
package org.hisp.dhis.tracker.export.trackedentity.aggregates;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.mapper.ProgramOwnerRowCallbackHandler;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.mapper.TrackedEntityAttributeRowCallbackHandler;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.mapper.TrackedEntityRowCallbackHandler;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.query.TeAttributeQuery;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.query.TrackedEntityQuery;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.model.TrackedEntityProgramOwner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Luciano Fiandesio
 * @author Ameen Mohamed
 */
@Repository
class TrackedEntityStore extends AbstractStore {
  private static final String GET_TE_SQL = TrackedEntityQuery.getQuery();

  private static final String GET_TE_ATTRIBUTES = TeAttributeQuery.getQuery();

  private static final String GET_PROGRAM_OWNERS =
      "select te.uid as key, p.uid as prguid, o.uid as ouuid "
          + "from trackedentityprogramowner teop "
          + "join program p on teop.programid = p.programid "
          + "join organisationunit o on teop.organisationunitid = o.organisationunitid "
          + "join trackedentity te on teop.trackedentityid = te.trackedentityid "
          + "where teop.trackedentityid in (:ids)";

  TrackedEntityStore(@Qualifier("readOnlyJdbcTemplate") JdbcTemplate jdbcTemplate) {
    super(jdbcTemplate);
  }

  Map<String, TrackedEntity> getTrackedEntities(List<Long> ids) {
    List<List<Long>> idPartitions = Lists.partition(ids, PARITITION_SIZE);

    Map<String, TrackedEntity> trackedEntityMap = new LinkedHashMap<>();

    idPartitions.forEach(
        partition -> trackedEntityMap.putAll(getTrackedEntitiesPartitioned(partition)));
    return trackedEntityMap;
  }

  private Map<String, TrackedEntity> getTrackedEntitiesPartitioned(List<Long> ids) {
    TrackedEntityRowCallbackHandler handler = new TrackedEntityRowCallbackHandler();

    jdbcTemplate.query(
        applySortOrder(GET_TE_SQL, StringUtils.join(ids, ",")), createIdsParam(ids), handler);

    return handler.getItems();
  }

  Multimap<String, TrackedEntityAttributeValue> getAttributes(List<Long> ids) {
    return fetch(GET_TE_ATTRIBUTES, new TrackedEntityAttributeRowCallbackHandler(), ids);
  }

  Multimap<String, TrackedEntityProgramOwner> getProgramOwners(List<Long> ids) {
    return fetch(GET_PROGRAM_OWNERS, new ProgramOwnerRowCallbackHandler(), ids);
  }
}
