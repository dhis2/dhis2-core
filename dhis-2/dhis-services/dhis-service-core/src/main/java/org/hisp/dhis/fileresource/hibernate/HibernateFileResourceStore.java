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
package org.hisp.dhis.fileresource.hibernate;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.joda.time.DateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository("org.hisp.dhis.fileresource.FileResourceStore")
public class HibernateFileResourceStore extends HibernateIdentifiableObjectStore<FileResource>
    implements FileResourceStore {
  private static final Set<String> IMAGE_CONTENT_TYPES =
      new ImmutableSet.Builder<String>()
          .add("image/jpg")
          .add("image/png")
          .add("image/jpeg")
          .build();

  public HibernateFileResourceStore(
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CurrentUserService currentUserService,
      AclService aclService) {
    super(
        sessionFactory,
        jdbcTemplate,
        publisher,
        FileResource.class,
        currentUserService,
        aclService,
        false);
  }

  @Override
  public List<FileResource> getExpiredFileResources(DateTime expires) {
    List<FileResource> results =
        getSession()
            .createNativeQuery(
                "select fr.* "
                    + "from fileresource fr "
                    + "inner join (select dva.value "
                    + "from datavalueaudit dva "
                    + "where dva.created < :date "
                    + "and dva.audittype in ('DELETE', 'UPDATE') "
                    + "and dva.dataelementid in "
                    + "(select dataelementid from dataelement where valuetype = 'FILE_RESOURCE')) dva "
                    + "on dva.value = fr.uid "
                    + "where fr.isassigned = true; ",
                FileResource.class)
            .setParameter("date", expires.toDate())
            .getResultList();

    return results;
  }

  @Override
  public List<FileResource> getAllUnProcessedImages() {
    return getQuery(
            "FROM FileResource fr WHERE fr.domain IN ( :domains ) AND fr.contentType IN ( :contentTypes ) AND hasMultipleStorageFiles = :hasMultipleStorageFiles")
        .setParameter("domains", FileResourceDomain.DOMAIN_FOR_MULTIPLE_IMAGES)
        .setParameter("contentTypes", IMAGE_CONTENT_TYPES)
        .setParameter("hasMultipleStorageFiles", false)
        .setMaxResults(50)
        .getResultList();
  }
}
