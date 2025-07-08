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
package org.hisp.dhis.tracker.export.event;

import static java.util.Map.entry;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Session;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.singleevent.SingleEventChangeLog;

public abstract class HibernateEventChangeLogStore<T> {
  private static final String COLUMN_CHANGELOG_CREATED = "ecl.created";
  private static final String COLUMN_CHANGELOG_USER = "ecl.createdByUsername";
  private static final String COLUMN_CHANGELOG_DATA_ELEMENT = "d.uid";
  private static final String COLUMN_CHANGELOG_FIELD = "ecl.eventField";
  private static final String ORDER_CHANGE_EXPRESSION =
      "CONCAT(COALESCE(LOWER(d.formName), ''), COALESCE(LOWER(d.name), ''), COALESCE(LOWER("
          + COLUMN_CHANGELOG_FIELD
          + "), ''))";
  private static final String DEFAULT_ORDER =
      COLUMN_CHANGELOG_CREATED + " " + SortDirection.DESC.getValue();

  /**
   * Event change logs can be ordered by given fields which correspond to fields on {@link
   * SingleEventChangeLog}. Maps fields to DB columns, except when sorting by 'change'. In that case
   * we need to sort by concatenation, to treat the dataElement and eventField as a single entity.
   */
  private static final Map<String, String> ORDERABLE_FIELDS =
      Map.ofEntries(
          entry("createdAt", COLUMN_CHANGELOG_CREATED),
          entry("username", COLUMN_CHANGELOG_USER),
          entry("change", ORDER_CHANGE_EXPRESSION));

  private static final Map<Pair<String, Class<?>>, String> FILTERABLE_FIELDS =
      Map.ofEntries(
          entry(Pair.of("username", String.class), COLUMN_CHANGELOG_USER),
          entry(Pair.of("dataElement", UID.class), COLUMN_CHANGELOG_DATA_ELEMENT),
          entry(Pair.of("field", String.class), COLUMN_CHANGELOG_FIELD));

  private final EntityManager entityManager;

  protected abstract String getTableName();

  protected HibernateEventChangeLogStore(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public void addEventChangeLog(T eventChangeLog) {
    entityManager.unwrap(Session.class).save(eventChangeLog);
  }

  public Page<EventChangeLog> getEventChangeLogs(
      @Nonnull UID event,
      @Nonnull EventChangeLogOperationParams operationParams,
      @Nonnull PageParams pageParams) {
    if (pageParams.isPageTotal()) {
      throw new UnsupportedOperationException("pageTotal is not supported");
    }

    Pair<String, QueryFilter> filter = operationParams.getFilter();

    String hql =
        String.format(
            """
        select \
            ecl.event, \
            ecl.dataElement, \
            ecl.eventField, \
            ecl.previousValue, \
            ecl.currentValue, \
            ecl.changeLogType, \
            ecl.created, \
            ecl.createdByUsername, \
            u.firstName, \
            u.surname, \
            u.uid \
        from %s ecl \
        join ecl.event e \
        left join ecl.dataElement d \
        left join ecl.createdBy u \
        where e.uid = :eventUid
        """,
            getTableName());

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
    query.setParameter("eventUid", event.getValue());
    query.setFirstResult(pageParams.getOffset());
    query.setMaxResults(
        pageParams.getPageSize() + 1); // get extra changeLog to determine if there is a nextPage

    if (filter != null) {
      query.setParameter("filterValue", filter.getValue().getFilter());
    }

    List<Object[]> results = query.getResultList();
    List<EventChangeLog> eventChangeLogs =
        results.stream()
            .map(
                row -> {
                  DataElement dataElement = (DataElement) row[1];
                  String eventField = (String) row[2];
                  String previousValue = (String) row[3];
                  String currentValue = (String) row[4];
                  ChangeLogType changeLogType = (ChangeLogType) row[5];
                  Date created = (Date) row[6];

                  UserInfoSnapshot createdBy =
                      new UserInfoSnapshot((String) row[7], (String) row[8], (String) row[9]);
                  createdBy.setUid((String) row[10]);

                  return new EventChangeLog(
                      dataElement,
                      eventField,
                      previousValue,
                      currentValue,
                      changeLogType,
                      created,
                      createdBy);
                })
            .toList();

    return new Page<>(eventChangeLogs, pageParams);
  }

  public void deleteEventChangeLog(DataElement dataElement) {
    String hql = String.format("delete from %s where dataElement = :dataElement", getTableName());

    entityManager.createQuery(hql).setParameter("dataElement", dataElement).executeUpdate();
  }

  public void deleteEventChangeLog(Event event) {
    String hql = String.format("delete from %s where event = :event", getTableName());

    entityManager.createQuery(hql).setParameter("event", event).executeUpdate();
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

  public Set<String> getOrderableFields() {
    return ORDERABLE_FIELDS.keySet();
  }

  public Set<Pair<String, Class<?>>> getFilterableFields() {
    return FILTERABLE_FIELDS.keySet();
  }
}
