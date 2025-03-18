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

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.Query;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// This class is annotated with @Component instead of @Repository because @Repository creates a
// proxy that can't be used to inject the class.
@Component("org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueStore")
class HibernateTrackedEntityAttributeValueStore
    extends HibernateGenericStore<TrackedEntityAttributeValue> {
  public HibernateTrackedEntityAttributeValueStore(
      EntityManager entityManager, JdbcTemplate jdbcTemplate, ApplicationEventPublisher publisher) {
    super(entityManager, jdbcTemplate, publisher, TrackedEntityAttributeValue.class, false);
  }

  // -------------------------------------------------------------------------
  // Implementation methods
  // -------------------------------------------------------------------------

  public void saveVoid(TrackedEntityAttributeValue attributeValue) {
    getSession().save(attributeValue);
  }

  public int deleteByTrackedEntity(TrackedEntity trackedEntity) {
    Query<TrackedEntityAttributeValue> query =
        getQuery("delete from TrackedEntityAttributeValue where trackedEntity = :trackedEntity");
    query.setParameter("trackedEntity", trackedEntity);
    return query.executeUpdate();
  }

  public TrackedEntityAttributeValue get(
      TrackedEntity trackedEntity, TrackedEntityAttribute attribute) {
    String query =
        " from TrackedEntityAttributeValue v where v.trackedEntity =:trackedEntity and attribute =:attribute";

    Query<TrackedEntityAttributeValue> typedQuery =
        getQuery(query)
            .setParameter("trackedEntity", trackedEntity)
            .setParameter("attribute", attribute);

    return getSingleResult(typedQuery);
  }

  public List<TrackedEntityAttributeValue> get(TrackedEntity trackedEntity) {
    String query = " from TrackedEntityAttributeValue v where v.trackedEntity =:trackedEntity";

    Query<TrackedEntityAttributeValue> typedQuery =
        getQuery(query).setParameter("trackedEntity", trackedEntity);

    return getList(typedQuery);
  }

  public List<TrackedEntityAttributeValue> get(TrackedEntityAttribute attribute) {
    String query = " from TrackedEntityAttributeValue v where v.attribute =:attribute";

    Query<TrackedEntityAttributeValue> typedQuery =
        getQuery(query).setParameter("attribute", attribute);

    return getList(typedQuery);
  }

  public List<TrackedEntityAttributeValue> get(
      TrackedEntityAttribute attribute, Collection<String> values) {
    String query =
        " from TrackedEntityAttributeValue v where v.attribute =:attribute and lower(v.plainValue) in :values";

    Query<TrackedEntityAttributeValue> typedQuery =
        getQuery(query)
            .setParameter("attribute", attribute)
            .setParameter("values", values.stream().map(StringUtils::lowerCase).toList());

    return getList(typedQuery);
  }

  public List<TrackedEntityAttributeValue> get(TrackedEntityAttribute attribute, String value) {
    String query =
        " from TrackedEntityAttributeValue v where v.attribute =:attribute and lower(v.plainValue) like :value";

    Query<TrackedEntityAttributeValue> typedQuery =
        getQuery(query)
            .setParameter("attribute", attribute)
            .setParameter("value", StringUtils.lowerCase(value));

    return getList(typedQuery);
  }

  public List<TrackedEntityAttributeValue> get(TrackedEntity trackedEntity, Program program) {
    String query =
        " from TrackedEntityAttributeValue v where v.trackedEntity =:trackedEntity and v.attribute.program =:program";

    Query<TrackedEntityAttributeValue> typedQuery = getQuery(query);
    typedQuery.setParameter("trackedEntity", trackedEntity);
    typedQuery.setParameter("program", program);

    return getList(typedQuery);
  }
}
