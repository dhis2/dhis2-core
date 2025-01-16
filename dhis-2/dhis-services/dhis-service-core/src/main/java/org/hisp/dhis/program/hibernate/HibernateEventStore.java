/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.program.hibernate;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventStore;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Abyot Asalefew
 */
@Repository("org.hisp.dhis.program.EventStore")
public class HibernateEventStore extends SoftDeleteHibernateObjectStore<Event>
    implements EventStore {

  public HibernateEventStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, Event.class, aclService, false);
  }

  @Override
  protected void preProcessPredicates(
      CriteriaBuilder builder, List<Function<Root<Event>, Predicate>> predicates) {
    predicates.add(root -> builder.equal(root.get("deleted"), false));
  }

  @Override
  protected Event postProcessObject(Event event) {
    return (event == null || event.isDeleted()) ? null : event;
  }

  /**
   * Method which searches the `eventdatavalues` jsonb column. It checks if any of the root keys
   * (which are {@link DataElement}) {@link UID}s, match any of the search strings passed in.
   *
   * @param searchStrings strings to search for, at the root key level
   * @return all Events whose eventdatavalues contain any of the search strings passed in
   */
  @Override
  public List<Event> getAllWithEventDataValuesRootKeysContainingAnyOf(List<String> searchStrings) {
    return nativeSynchronizedTypedQuery(
            """
             select * from event e
             where jsonb_exists_any(e.eventdatavalues, :searchStrings)
              """)
        .setParameter(
            "searchStrings", searchStrings.toArray(String[]::new), StringArrayType.INSTANCE)
        .getResultList();
  }

  @Override
  public void setAttributeOptionCombo(Set<Long> cocs, long coc) {
    if (cocs.isEmpty()) return;
    String sql =
        """
        update event
        set attributeoptioncomboid = %s
        where attributeoptioncomboid in (%s)
        """
            .formatted(coc, cocs.stream().map(String::valueOf).collect(Collectors.joining(",")));

    entityManager.createNativeQuery(sql).executeUpdate();
  }
}
