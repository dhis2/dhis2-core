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
package org.hisp.dhis.program.hibernate;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.SingleEventStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.SqlUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository("org.hisp.dhis.program.SingleEventStore")
public class HibernateSingleEventStore extends SoftDeleteHibernateObjectStore<SingleEvent>
    implements SingleEventStore {

  public HibernateSingleEventStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, SingleEvent.class, aclService, false);
  }

  @Override
  public void mergeEventDataValuesWithDataElement(
      @Nonnull Collection<UID> sourceDataElements, @Nonnull UID targetDataElement) {
    if (sourceDataElements.isEmpty()) return;
    String sourceUidsInSingleQuotesString =
        sourceDataElements.stream()
            .map(uid -> SqlUtils.singleQuote(uid.getValue()))
            .collect(Collectors.joining(","));
    String sourceUidsString =
        sourceDataElements.stream().map(UID::getValue).collect(Collectors.joining(","));

    String sql =
        """
            do
            $$
            declare
              source_event record;
              lastupdated_dv jsonb;
              target_de varchar default '%s';
            begin

              -- loop through each event that has a source DataElement in its event data values json
              for source_event in
                select eventid, eventdatavalues from singleevent, jsonb_each(eventdatavalues)
                where key in (%s)
                loop

                -- get last updated data value for the event data value set (sources + target)
                select value
                  into lastupdated_dv
                  from jsonb_each(source_event.eventdatavalues)
                WHERE key IN (%s)
                order by DATE(value ->> 'lastUpdated') desc
                limit 1;

                -- use the last updated value as the new value for the target key
                -- this will override any value for the target key if it is already present
                update singleevent
                set eventdatavalues = jsonb_set(eventdatavalues, '{%s}', lastupdated_dv)
                where singleevent.eventid = source_event.eventid;

                -- remove all source key values as no longer needed
                update singleevent set eventdatavalues = eventdatavalues - '{%s}'::text[]
                where singleevent.eventid = source_event.eventid;

              end loop;
            end;
            $$
            language plpgsql;
            """
            .formatted(
                targetDataElement.getValue(),
                sourceUidsInSingleQuotesString,
                sourceUidsInSingleQuotesString
                    + ","
                    + SqlUtils.singleQuote(targetDataElement.getValue()),
                targetDataElement.getValue(),
                sourceUidsString);

    log.debug("Event data values merging SQL query to be used: \n{}", sql);
    jdbcTemplate.update(sql);
  }

  @Override
  public void deleteEventDataValuesWithDataElement(@Nonnull Collection<UID> sourceDataElements) {
    if (sourceDataElements.isEmpty()) return;
    String sourceUidsInSingleQuotesString =
        sourceDataElements.stream()
            .map(uid -> SqlUtils.singleQuote(uid.getValue()))
            .collect(Collectors.joining(","));
    String sourceUidsString =
        sourceDataElements.stream().map(UID::getValue).collect(Collectors.joining(","));

    String sql =
        """
            update singleevent set eventdatavalues = eventdatavalues - '{%s}'::text[]
            where eventdatavalues::jsonb ?| array[%s];"""
            .formatted(sourceUidsString, sourceUidsInSingleQuotesString);
    log.debug("Event data values deleting SQL query to be used: \n{}", sql);
    jdbcTemplate.update(sql);
  }

  @Override
  public void setAttributeOptionCombo(Set<Long> cocs, long coc) {
    if (cocs.isEmpty()) return;
    String sql =
        """
            update singleevent
            set attributeoptioncomboid = %s
            where attributeoptioncomboid in (%s)"""
            .formatted(coc, cocs.stream().map(String::valueOf).collect(Collectors.joining(",")));

    entityManager.createNativeQuery(sql).executeUpdate();
  }
}
