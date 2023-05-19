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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.datavalue.DataValueKey;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.joda.time.DateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository( "org.hisp.dhis.fileresource.FileResourceStore" )
public class HibernateFileResourceStore
    extends HibernateIdentifiableObjectStore<FileResource>
    implements FileResourceStore
{
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
        "image/jpg",
        "image/png",
        "image/jpeg" );

    public HibernateFileResourceStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CurrentUserService currentUserService, AclService aclService )
    {
        super( sessionFactory, jdbcTemplate, publisher, FileResource.class, currentUserService, aclService, false );
    }

    @Override
    public List<FileResource> getExpiredFileResources( DateTime expires )
    {
        List<FileResource> results = getSession()
            .createNativeQuery( "select fr.* " +
                "from fileresource fr " +
                "inner join (select dva.value " +
                "from datavalueaudit dva " +
                "where dva.created < :date " +
                "and dva.audittype in ('DELETE', 'UPDATE') " +
                "and dva.dataelementid in " +
                "(select dataelementid from dataelement where valuetype = 'FILE_RESOURCE')) dva " +
                "on dva.value = fr.uid " +
                "where fr.isassigned = true; ", FileResource.class )
            .setParameter( "date", expires.toDate() )
            .getResultList();

        return results;
    }

    @Override
    public List<FileResource> getAllUnProcessedImages()
    {
        return getQuery(
            "FROM FileResource fr WHERE fr.domain IN ( :domains ) AND fr.contentType IN ( :contentTypes ) AND hasMultipleStorageFiles = :hasMultipleStorageFiles" )
                .setParameter( "domains", FileResourceDomain.DOMAIN_FOR_MULTIPLE_IMAGES )
                .setParameter( "contentTypes", IMAGE_CONTENT_TYPES )
                .setParameter( "hasMultipleStorageFiles", false )
                .setMaxResults( 50 ).getResultList();
    }

    @Override
    public Optional<FileResource> findByStorageKey( @Nonnull String storageKey )
    {
        return getSession()
            .createNativeQuery( "select fr.* from fileresource fr where fr.storagekey = :key", FileResource.class )
            .setParameter( "key", storageKey )
            .getResultStream().findFirst();
    }

    @Override
    public Optional<FileResource> findByUidAndDomain( @Nonnull String uid, @Nonnull FileResourceDomain domain )
    {
        return getSession()
            .createNativeQuery( "select * from fileresource where uid = :uid and domain = :domain", FileResource.class )
            .setParameter( "uid", uid ).setParameter( "domain", domain.name() )
            .getResultList().stream().findFirst();
    }

    @Override
    public List<String> findOrganisationUnitsByImageFileResource( @Nonnull String uid )
    {
        String sql = "select o.uid from organisationunit o left join fileresource fr on fr.fileresourceid = o.image where fr.uid = :uid";
        return getSession().createNativeQuery( sql ).setParameter( "uid", uid ).list();
    }

    @Override
    public List<String> findUsersByAvatarFileResource( @Nonnull String uid )
    {
        String sql = "select u.uid from userinfo u left join fileresource fr on fr.fileresourceid = u.avatar where fr.uid = :uid";
        return getSession().createNativeQuery( sql ).setParameter( "uid", uid ).list();
    }

    @Override
    public List<String> findDocumentsByFileResource( @Nonnull String uid )
    {
        String sql = "select d.uid from document d left join fileresource fr on fr.fileresourceid = d.fileresource where fr.uid = :uid";
        return getSession().createNativeQuery( sql ).setParameter( "uid", uid ).list();
    }

    @Override
    public List<String> findMessagesByFileResource( @Nonnull String uid )
    {
        String sql = "select m.uid from message m "
            + "left join messageattachments ma on m.messageid = ma.messageid "
            + "left join fileresource fr on fr.fileresourceid = ma.fileresourceid where fr.uid = :uid";
        return getSession().createNativeQuery( sql ).setParameter( "uid", uid ).list();
    }

    @Override
    public List<DataValueKey> findDataValuesByFileResourceValue( @Nonnull String uid )
    {
        String sql = "select de.uid as de, o.uid as ou, dv.periodid as pe, co.uid as co from dataelement de"
            + " inner join datavalue dv on de.dataelementid = dv.dataelementid"
            + " inner join organisationunit o on dv.sourceid = o.organisationunitid"
            + " inner join categoryoptioncombo co on dv.categoryoptioncomboid = co.categoryoptioncomboid"
            + " where de.valuetype = 'FILE_RESOURCE' and dv.value = :uid";
        Stream<Object[]> stream = getSession().createNativeQuery( sql ).setParameter( "uid", uid ).stream();
        return stream
            .map( col -> new DataValueKey( (String) (col)[0], (String) col[1], ((Number) col[2]).longValue(),
                (String) col[3] ) )
            .collect( Collectors.toUnmodifiableList() );
    }
}
