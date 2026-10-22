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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.hibernate.SessionFactory;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceStore;
import org.hisp.dhis.message.Message;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.message.UserMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
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
        .setParameter("domains", FileResourceDomain.getDomainForMultipleImages())
        .setParameter("contentTypes", IMAGE_CONTENT_TYPES)
        .setParameter("hasMultipleStorageFiles", false)
        .setMaxResults(50)
        .getResultList();
  }

  @Override
  public List<String> findDocumentsByFileResource(@Nonnull String uid) {
    return getSession()
        .createNativeQuery(
            "select d.uid from document d"
                + " inner join fileresource fr on fr.fileresourceid = d.fileresource"
                + " where fr.uid = :uid")
        .addSynchronizedEntityClass(Document.class)
        .addSynchronizedEntityClass(FileResource.class)
        .setParameter("uid", uid)
        .list();
  }

  @Override
  public List<String> findUsersByAvatarFileResource(@Nonnull String uid) {
    return getSession()
        .createNativeQuery(
            "select u.uid from userinfo u"
                + " inner join fileresource fr on fr.fileresourceid = u.avatar"
                + " where fr.uid = :uid")
        .addSynchronizedEntityClass(User.class)
        .addSynchronizedEntityClass(FileResource.class)
        .setParameter("uid", uid)
        .list();
  }

  @Override
  public List<String> findMessageAttachmentParticipants(@Nonnull String uid) {
    return getSession()
        .createNativeQuery(
            "select distinct u.uid from userinfo u"
                + " inner join usermessage um on um.userid = u.userinfoid"
                + " inner join messageconversation_usermessages mcu on mcu.usermessageid = um.usermessageid"
                + " inner join messageconversation_messages mcm on mcm.messageconversationid = mcu.messageconversationid"
                + " inner join messageattachments ma on ma.messageid = mcm.messageid"
                + " inner join fileresource fr on fr.fileresourceid = ma.fileresourceid"
                + " where fr.uid = :uid")
        .addSynchronizedEntityClass(User.class)
        .addSynchronizedEntityClass(UserMessage.class)
        .addSynchronizedEntityClass(MessageConversation.class)
        .addSynchronizedEntityClass(Message.class)
        .addSynchronizedEntityClass(FileResource.class)
        .setParameter("uid", uid)
        .list();
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<DataValueFileReference> findDataValueReferencesByFileResourceValue(
      @Nonnull String uid) {
    // restricting to the file data elements first lets both lookups use the indexes that lead
    // with the data element
    String fileDataElements =
        "select de.dataelementid from dataelement de"
            + " where de.valuetype in ('FILE_RESOURCE', 'IMAGE')";
    List<Object[]> rows =
        getSession()
            .createNativeQuery(
                "select distinct ou.path, coc.uid as coc, aoc.uid as aoc from ("
                    + " select dv.sourceid as ouid, dv.categoryoptioncomboid as cocid,"
                    + " dv.attributeoptioncomboid as aocid"
                    + " from datavalue dv"
                    + " where dv.value = :uid and dv.dataelementid in ("
                    + fileDataElements
                    + ")"
                    + " union"
                    + " select dva.organisationunitid, dva.categoryoptioncomboid,"
                    + " dva.attributeoptioncomboid"
                    + " from datavalueaudit dva"
                    + " where dva.value = :uid and dva.dataelementid in ("
                    + fileDataElements
                    + ")"
                    + ") ref"
                    + " inner join organisationunit ou on ou.organisationunitid = ref.ouid"
                    + " inner join categoryoptioncombo coc on coc.categoryoptioncomboid = ref.cocid"
                    + " inner join categoryoptioncombo aoc on aoc.categoryoptioncomboid = ref.aocid")
            .addSynchronizedEntityClass(DataValue.class)
            .addSynchronizedEntityClass(DataValueAudit.class)
            .addSynchronizedEntityClass(DataElement.class)
            .addSynchronizedEntityClass(OrganisationUnit.class)
            .addSynchronizedEntityClass(CategoryOptionCombo.class)
            .setParameter("uid", uid)
            .list();
    List<DataValueFileReference> references = new ArrayList<>(rows.size());
    for (Object[] row : rows) {
      references.add(new DataValueFileReference((String) row[0], (String) row[1], (String) row[2]));
    }
    return references;
  }
}
