/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.deduplication.hibernate;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.deduplication.DeduplicationStatus;
import org.hisp.dhis.deduplication.ForbiddenPotentialDuplicateException;
import org.hisp.dhis.deduplication.MergeObject;
import org.hisp.dhis.deduplication.PotentialDuplicate;
import org.hisp.dhis.deduplication.PotentialDuplicateQuery;
import org.hisp.dhis.deduplication.PotentialDuplicateStore;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository( "org.hisp.dhis.deduplication.PotentialDuplicateStore" )
public class HibernatePotentialDuplicateStore
    extends HibernateIdentifiableObjectStore<PotentialDuplicate>
    implements PotentialDuplicateStore
{
    private RelationshipStore relationshipStore;

    public HibernatePotentialDuplicateStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CurrentUserService currentUserService, AclService aclService,
        RelationshipStore relationshipStore )
    {
        super( sessionFactory, jdbcTemplate, publisher, PotentialDuplicate.class, currentUserService,
            aclService, false );
        this.relationshipStore = relationshipStore;
    }

    @Override
    public int getCountByQuery( PotentialDuplicateQuery query )
    {
        String queryString = "select count(*) from PotentialDuplicate pr where pr.status in (:status)";

        return Optional.ofNullable( query.getTeis() ).filter( teis -> !teis.isEmpty() ).map( teis -> {
            Query<Long> hibernateQuery = getTypedQuery(
                queryString + " and ( pr.teiA in (:uids) or pr.teiB in (:uids) )" );

            hibernateQuery.setParameterList( "uids", teis );

            setStatusParameter( query.getStatus(), hibernateQuery );

            return hibernateQuery.getSingleResult().intValue();
        } ).orElseGet( () -> {

            Query<Long> hibernateQuery = getTypedQuery( queryString );

            setStatusParameter( query.getStatus(), hibernateQuery );

            return hibernateQuery.getSingleResult().intValue();
        } );
    }

    @Override
    public List<PotentialDuplicate> getAllByQuery( PotentialDuplicateQuery query )
    {
        String queryString = "from PotentialDuplicate pr where pr.status in (:status)";

        return Optional.ofNullable( query.getTeis() ).filter( teis -> !teis.isEmpty() ).map( teis -> {
            Query<PotentialDuplicate> hibernateQuery = getTypedQuery(
                queryString + " and ( pr.teiA in (:uids) or pr.teiB in (:uids) )" );

            hibernateQuery.setParameterList( "uids", teis );

            setStatusParameter( query.getStatus(), hibernateQuery );

            return hibernateQuery.getResultList();
        } ).orElseGet( () -> {

            Query<PotentialDuplicate> hibernateQuery = getTypedQuery( queryString );

            setStatusParameter( query.getStatus(), hibernateQuery );

            return hibernateQuery.getResultList();
        } );
    }

    private void setStatusParameter( DeduplicationStatus status, Query<?> hibernateQuery )
    {
        if ( status == DeduplicationStatus.ALL )
        {
            hibernateQuery.setParameterList( "status", Arrays.stream( DeduplicationStatus.values() )
                .filter( s -> s != DeduplicationStatus.ALL ).collect( Collectors.toSet() ) );
        }
        else
        {
            hibernateQuery.setParameterList( "status", Collections.singletonList( status ) );
        }
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public boolean exists( PotentialDuplicate potentialDuplicate )
    {
        if ( potentialDuplicate.getTeiA() == null || potentialDuplicate.getTeiB() == null )
            throw new ForbiddenPotentialDuplicateException(
                "Can't search for pair of potential duplicates: teiA and teiB must not be null" );

        NativeQuery<BigInteger> query = getSession()
            .createNativeQuery( "select count(potentialduplicateid) from potentialduplicate pd " +
                "where (pd.teiA = :teia and pd.teiB = :teib) or (pd.teiA = :teib and pd.teiB = :teia)" );

        query.setParameter( "teia", potentialDuplicate.getTeiA() );
        query.setParameter( "teib", potentialDuplicate.getTeiB() );

        return query.getSingleResult().intValue() != 0;
    }

    @Override
    public void merge( TrackedEntityInstance original, TrackedEntityInstance duplicate, MergeObject mergeObject )
    {
        moveTrackedEntityAttributeValues( original.getUid(), duplicate.getUid(),
            mergeObject.getTrackedEntityAttributes() );
        moveRelationships( original.getUid(), duplicate.getUid(), mergeObject.getRelationships() );
    }

    @Override
    public void moveTrackedEntityAttributeValues( String originalUid, String duplicateUid,
        List<String> trackedEntityAttributes )
    {

        String removeOldValuesSQL = "DELETE FROM trackedentityattributevalue "
            + "WHERE trackedentityinstanceid = ("
            + "SELECT trackedentityinstanceid FROM trackedentityinstance WHERE uid = :original"
            + ") AND trackedentityattributeid IN ("
            + "SELECT trackedentityattributeid FROM trackedentityattribute WHERE uid IN (:teas)"
            + ")";

        String moveNewValuesSQL = "UPDATE trackedentityattributevalue "
            + "SET trackedentityinstanceid = ("
            + "SELECT trackedentityinstanceid FROM trackedentityinstance WHERE uid = :original"
            + ") WHERE trackedentityinstanceid = ("
            + "SELECT trackedentityinstanceid FROM trackedentityinstance WHERE uid = :duplicate"
            + ") AND trackedentityattributeid IN ("
            + "SELECT trackedentityattributeid FROM trackedentityattribute WHERE uid IN (:teas)"
            + ")";

        getSession()
            .createNativeQuery( removeOldValuesSQL )
            .setParameter( "original", originalUid )
            .setParameterList( "teas", trackedEntityAttributes )
            .executeUpdate();

        getSession()
            .createNativeQuery( moveNewValuesSQL )
            .setParameter( "original", originalUid )
            .setParameter( "duplicate", duplicateUid )
            .setParameterList( "teas", trackedEntityAttributes )
            .executeUpdate();
    }

    @Override
    public void moveRelationships( String originalUid, String duplicateUid, List<String> relationships )
    {
        String moveRelationshipsSQL = "UPDATE relationshipitem "
            + "SET trackedentityinstanceid = ("
            + "SELECT trackedentityinstanceid FROM trackedentityinstance WHERE uid = :original"
            + ") WHERE trackedentityinstanceid = ("
            + "SELECT trackedentityinstanceid FROM trackedentityinstance WHERE uid = :duplicate"
            + ") AND relationshipid IN ("
            + "SELECT relationshipid FROM relationship WHERE uid IN (:relationships)"
            + ")";

        getSession()
            .createNativeQuery( moveRelationshipsSQL )
            .setParameter( "original", originalUid )
            .setParameter( "duplicate", duplicateUid )
            .setParameterList( "relationships", relationships )
            .executeUpdate();
    }

    @Override
    public void removeTrackedEntity( TrackedEntityInstance trackedEntityInstance )
    {
        removeRelationShips( trackedEntityInstance );

        removeTrackedEntityAttributeValues( trackedEntityInstance );

        removeTrackedEntityInstance( trackedEntityInstance );
    }

    private void removeTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance )
    {
        String removeTrackedEntityInstanceSQL = "DELETE FROM trackedentityinstance " +
            "WHERE trackedentityinstanceid = (" +
            "SELECT trackedentityinstanceid FROM trackedentityinstance WHERE uid = :duplicate"
            + ") ";

        getSession().createNativeQuery( removeTrackedEntityInstanceSQL )
            .setParameter( "duplicate", trackedEntityInstance.getUid() )
            .executeUpdate();
    }

    private void removeTrackedEntityAttributeValues( TrackedEntityInstance trackedEntityInstance )
    {
        String removeTrackedEntityAttributeValueSQL = "DELETE FROM trackedentityattributevalue " +
            "WHERE trackedentityinstanceid = (" +
            "SELECT trackedentityinstanceid FROM trackedentityinstance WHERE uid = :duplicate"
            + ") ";

        getSession().createNativeQuery( removeTrackedEntityAttributeValueSQL )
            .setParameter( "duplicate", trackedEntityInstance.getUid() )
            .executeUpdate();
    }

    private void removeRelationShips( TrackedEntityInstance trackedEntityInstance )
    {
        List<Relationship> relationship = relationshipStore.getByTrackedEntityInstance( trackedEntityInstance );

        relationship.forEach( r -> {
            r.setFrom( null );
            r.setTo( null );
            getSession().update( r );
            relationshipStore.delete( r );
        } );
    }
}
