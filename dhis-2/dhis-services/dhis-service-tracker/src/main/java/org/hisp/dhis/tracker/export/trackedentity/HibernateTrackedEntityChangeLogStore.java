/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.export.trackedentity;

import static java.util.Map.entry;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Session;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.export.Order;
import org.springframework.stereotype.Repository;

@Repository("org.hisp.dhis.tracker.export.trackedentity.HibernateTrackedEntityChangeLogStore")
public class HibernateTrackedEntityChangeLogStore {
  private static final String COLUMN_CHANGELOG_CREATED = "tecl.created";
  private static final String COLUMN_CHANGELOG_USER = "tecl.createdByUsername";
  private static final String COLUMN_CHANGELOG_TEA_UID = "tea.uid";
  private static final String ORDER_ATTRIBUTE_EXPRESSION =
      "CONCAT(COALESCE(LOWER(tea.formName), ''), COALESCE(LOWER(tea.name), ''))";

  private static final String DEFAULT_ORDER =
      COLUMN_CHANGELOG_CREATED + " " + SortDirection.DESC.getValue();

  private static final Map<String, String> ORDERABLE_FIELDS =
      Map.ofEntries(
          entry("createdAt", COLUMN_CHANGELOG_CREATED),
          entry("username", COLUMN_CHANGELOG_USER),
          entry("attribute", ORDER_ATTRIBUTE_EXPRESSION));

  private static final Map<Pair<String, Class<?>>, String> FILTERABLE_FIELDS =
      Map.ofEntries(
          entry(Pair.of("username", String.class), COLUMN_CHANGELOG_USER),
          entry(Pair.of("attribute", UID.class), COLUMN_CHANGELOG_TEA_UID));

  private final EntityManager entityManager;
  private final Session session;

  public HibernateTrackedEntityChangeLogStore(EntityManager entityManager) {
    this.entityManager = entityManager;
    this.session = entityManager.unwrap(Session.class);
  }

  public void addTrackedEntityChangeLog(TrackedEntityChangeLog trackedEntityChangeLog) {
    session.save(trackedEntityChangeLog);
  }

  public Page<TrackedEntityChangeLog> getTrackedEntityChangeLogs(
      @Nonnull UID trackedEntity,
      @CheckForNull UID program,
      @Nonnull Set<UID> attributes,
      @Nonnull TrackedEntityChangeLogOperationParams operationParams,
      @Nonnull PageParams pageParams) {
    if (pageParams.isPageTotal()) {
      throw new UnsupportedOperationException("pageTotal is not supported");
    }

    String hql =
        """
        select \
            tecl.trackedEntity, \
            tecl.trackedEntityAttribute, \
            tecl.previousValue, \
            tecl.currentValue, \
            tecl.changeLogType, \
            tecl.created, \
            tecl.createdByUsername, \
            u.firstName, \
            u.surname, \
            u.uid \
        from TrackedEntityChangeLog tecl \
        join tecl.trackedEntity t \
        join tecl.trackedEntityAttribute tea \
        left join tecl.createdBy u \
        """;

    if (program != null) {
      hql +=
          """
          join tecl.programAttribute pa \
          join pa.program p \
          where t.uid = :trackedEntity \
          and p.uid = :program \
          """;

    } else {
      hql +=
          """
          where t.uid = :trackedEntity \
          """;
    }

    if (!attributes.isEmpty()) {
      hql +=
          """
          and tea.uid in (:attributes) \
          """;
    }

    Pair<String, QueryFilter> filter = operationParams.getFilter();
    if (filter != null) {
      String filterField =
          FILTERABLE_FIELDS.entrySet().stream()
              .filter(entry -> entry.getKey().getLeft().equals(filter.getKey()))
              .findFirst()
              .map(Entry::getValue)
              .get();

      hql += String.format(" and %s = :filterValue ", filterField);
    }

    hql += String.format("order by %s".formatted(sortExpressions(operationParams.getOrder())));

    Query query = entityManager.createQuery(hql);
    query.setParameter("trackedEntity", trackedEntity.getValue());

    if (program != null) {
      query.setParameter("program", program.getValue());
    }

    if (!attributes.isEmpty()) {
      query.setParameter(
          "attributes", attributes.stream().map(UID::getValue).collect(Collectors.toSet()));
    }

    query.setFirstResult(pageParams.getOffset());
    query.setMaxResults(
        pageParams.getPageSize() + 1); // get extra changeLog to determine if there is a nextPage

    if (filter != null) {
      query.setParameter("filterValue", filter.getValue().getFilter());
    }

    List<Object[]> results = query.getResultList();
    List<TrackedEntityChangeLog> trackedEntityChangeLogs =
        results.stream()
            .map(
                row -> {
                  TrackedEntity t = (TrackedEntity) row[0];
                  TrackedEntityAttribute trackedEntityAttribute = (TrackedEntityAttribute) row[1];
                  String previousValue = (String) row[2];
                  String currentValue = (String) row[3];
                  ChangeLogType changeLogType = (ChangeLogType) row[4];
                  Date created = (Date) row[5];

                  UserInfoSnapshot createdBy =
                      new UserInfoSnapshot((String) row[6], (String) row[7], (String) row[8]);
                  createdBy.setUid((String) row[9]);

                  return new TrackedEntityChangeLog(
                      t,
                      trackedEntityAttribute,
                      previousValue,
                      currentValue,
                      changeLogType,
                      created,
                      createdBy);
                })
            .toList();

    return new Page<>(trackedEntityChangeLogs, pageParams);
  }

  public void deleteTrackedEntityChangeLogs(TrackedEntity trackedEntity) {
    org.hibernate.query.Query<?> query =
        session.createQuery("delete TrackedEntityChangeLog where trackedEntity = :trackedEntity");
    query.setParameter("trackedEntity", trackedEntity);
    query.executeUpdate();
  }

  public Set<String> getOrderableFields() {
    return ORDERABLE_FIELDS.keySet();
  }

  public Set<Pair<String, Class<?>>> getFilterableFields() {
    return FILTERABLE_FIELDS.keySet();
  }

  private static String sortExpressions(Order order) {
    if (order == null) {
      return DEFAULT_ORDER;
    }

    StringBuilder orderBuilder = new StringBuilder();
    orderBuilder.append(ORDERABLE_FIELDS.get(order.getField()));
    orderBuilder.append(" ");
    orderBuilder.append(order.getDirection().getValue());

    if (!order.getField().equals("createdAt")) {
      orderBuilder.append(", ").append(DEFAULT_ORDER);
    }

    return orderBuilder.toString();
  }
}
