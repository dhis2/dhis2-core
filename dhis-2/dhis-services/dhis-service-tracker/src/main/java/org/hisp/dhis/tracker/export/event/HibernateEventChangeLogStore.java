/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.export.event;

import static java.util.Map.entry;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.Session;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.springframework.stereotype.Repository;

@Repository("org.hisp.dhis.tracker.export.event.HibernateEventChangeLogStore")
public class HibernateEventChangeLogStore {
  private static final String COLUMN_CHANGELOG_CREATED = "ecl.created";
  private static final String DEFAULT_ORDER =
      COLUMN_CHANGELOG_CREATED + " " + SortDirection.DESC.getValue();

  /**
   * Event change logs can be ordered by given fields which correspond to fields on {@link
   * EventChangeLog}. Maps fields to DB columns. The order implementation for change logs is
   * different from other tracker exporters {@link EventChangeLog} is the view which is already
   * returned from the service/store. Tracker exporter services return a representation we have to
   * map to a view model. This mapping is not necessary for change logs.
   */
  private static final Map<String, String> ORDERABLE_FIELDS =
      Map.ofEntries(entry("createdAt", COLUMN_CHANGELOG_CREATED));

  private final EntityManager entityManager;

  public HibernateEventChangeLogStore(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public void addEventChangeLog(EventChangeLog eventChangeLog) {
    entityManager.unwrap(Session.class).save(eventChangeLog);
  }

  public Page<EventChangeLog> getEventChangeLogs(
      UID event, List<Order> order, PageParams pageParams) {

    String hql =
        String.format(
            """
          select ecl.event,
                 ecl.dataElement,
                 ecl.eventProperty,
                 ecl.previousValue,
                 ecl.currentValue,
                 ecl.changeLogType,
                 ecl.created,
                 ecl.createdByUsername,
                 u.firstName,
                 u.surname,
                 u.uid
          from EventChangeLog ecl
          join ecl.event e
          left join ecl.createdBy u
          where e.uid = :eventUid
          order by %s
      """
                .formatted(sortExpressions(order)));

    Query query = entityManager.createQuery(hql);
    query.setParameter("eventUid", event.getValue());
    query.setFirstResult((pageParams.getPage() - 1) * pageParams.getPageSize());
    query.setMaxResults(pageParams.getPageSize() + 1);

    List<Object[]> results = query.getResultList();
    List<EventChangeLog> eventChangeLogs =
        results.stream()
            .map(
                row -> {
                  Event e = (Event) row[0];
                  DataElement dataElement = (DataElement) row[1];
                  String eventProperty = (String) row[2];
                  String previousValue = (String) row[3];
                  String currentValue = (String) row[4];
                  ChangeLogType changeLogType = (ChangeLogType) row[5];
                  Date created = (Date) row[6];

                  UserInfoSnapshot createdBy =
                      new UserInfoSnapshot((String) row[7], (String) row[8], (String) row[9]);
                  createdBy.setUid((String) row[10]);

                  return new EventChangeLog(
                      e,
                      dataElement,
                      eventProperty,
                      previousValue,
                      currentValue,
                      changeLogType,
                      created,
                      createdBy);
                })
            .toList();

    Integer prevPage = pageParams.getPage() > 1 ? pageParams.getPage() - 1 : null;
    if (eventChangeLogs.size() > pageParams.getPageSize()) {
      return Page.withPrevAndNext(
          eventChangeLogs.subList(0, pageParams.getPageSize()),
          pageParams.getPage(),
          pageParams.getPageSize(),
          prevPage,
          pageParams.getPage() + 1);
    }

    return Page.withPrevAndNext(
        eventChangeLogs, pageParams.getPage(), pageParams.getPageSize(), prevPage, null);
  }

  public void deleteEventChangeLog(DataElement dataElement) {
    String hql = "delete from EventChangeLog where dataElement = :dataElement";

    entityManager.createQuery(hql).setParameter("dataElement", dataElement).executeUpdate();
  }

  public void deleteEventChangeLog(Event event) {
    String hql = "delete from EventChangeLog where event = :event";

    entityManager.createQuery(hql).setParameter("event", event).executeUpdate();
  }

  private static String sortExpressions(List<Order> order) {
    if (order.isEmpty()) {
      return DEFAULT_ORDER;
    }

    return ORDERABLE_FIELDS.get(order.get(0).getField())
        + " "
        + order.get(0).getDirection().getValue();
  }

  public Set<String> getOrderableFields() {
    return ORDERABLE_FIELDS.keySet();
  }
}
