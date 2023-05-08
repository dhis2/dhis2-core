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
package org.hisp.dhis.deduplication.hibernate;

import static org.hisp.dhis.common.AuditType.CREATE;
import static org.hisp.dhis.common.AuditType.DELETE;
import static org.hisp.dhis.common.AuditType.UPDATE;
import static org.hisp.dhis.external.conf.ConfigurationKey.CHANGELOG_TRACKER;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.AuditableEntity;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.deduplication.DeduplicationMergeParams;
import org.hisp.dhis.deduplication.DeduplicationStatus;
import org.hisp.dhis.deduplication.MergeObject;
import org.hisp.dhis.deduplication.PotentialDuplicate;
import org.hisp.dhis.deduplication.PotentialDuplicateConflictException;
import org.hisp.dhis.deduplication.PotentialDuplicateCriteria;
import org.hisp.dhis.deduplication.PotentialDuplicateStore;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAudit;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditStore;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository( "org.hisp.dhis.deduplication.PotentialDuplicateStore" )
public class HibernatePotentialDuplicateStore
    extends HibernateIdentifiableObjectStore<PotentialDuplicate>
    implements PotentialDuplicateStore
{
    private final AuditManager auditManager;

    private final TrackedEntityInstanceStore trackedEntityInstanceStore;

    private final TrackedEntityAttributeValueAuditStore trackedEntityAttributeValueAuditStore;

    private final DhisConfigurationProvider config;

    public HibernatePotentialDuplicateStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CurrentUserService currentUserService, AclService aclService,
        TrackedEntityInstanceStore trackedEntityInstanceStore, AuditManager auditManager,
        TrackedEntityAttributeValueAuditStore trackedEntityAttributeValueAuditStore,
        DhisConfigurationProvider config )
    {
        super( sessionFactory, jdbcTemplate, publisher, PotentialDuplicate.class, currentUserService,
            aclService, false );
        this.trackedEntityInstanceStore = trackedEntityInstanceStore;
        this.auditManager = auditManager;
        this.trackedEntityAttributeValueAuditStore = trackedEntityAttributeValueAuditStore;
        this.config = config;
    }

    @Override
    public int getCountPotentialDuplicates( PotentialDuplicateCriteria query )
    {
        CriteriaBuilder cb = getCriteriaBuilder();

        CriteriaQuery<Long> countCriteriaQuery = cb.createQuery( Long.class );
        Root<PotentialDuplicate> root = countCriteriaQuery.from( PotentialDuplicate.class );

        countCriteriaQuery
            .select( cb.count( root ) );

        countCriteriaQuery.where( getQueryPredicates( query, cb, root ) );

        TypedQuery<Long> relationshipTypedQuery = getSession()
            .createQuery( countCriteriaQuery );

        return relationshipTypedQuery.getSingleResult().intValue();
    }

    @Override
    public List<PotentialDuplicate> getPotentialDuplicates( PotentialDuplicateCriteria criteria )
    {
        CriteriaBuilder cb = getCriteriaBuilder();

        CriteriaQuery<PotentialDuplicate> cq = cb
            .createQuery( PotentialDuplicate.class );

        Root<PotentialDuplicate> root = cq.from( PotentialDuplicate.class );

        cq.where( getQueryPredicates( criteria, cb, root ) );

        cq.orderBy( criteria.getOrder().stream().map( order -> order.getDirection()
            .isAscending() ? cb.asc( root.get( order.getField() ) )
                : cb.desc( root.get( order.getField() ) ) )
            .collect( Collectors.toList() ) );

        TypedQuery<PotentialDuplicate> relationshipTypedQuery = getSession()
            .createQuery( cq );

        if ( criteria.isPagingRequest() )
        {
            relationshipTypedQuery.setFirstResult( criteria.getFirstResult() );
            relationshipTypedQuery.setMaxResults( criteria.getPageSize() );
        }

        return relationshipTypedQuery.getResultList();
    }

    private Predicate[] getQueryPredicates( PotentialDuplicateCriteria query, CriteriaBuilder builder,
        Root<PotentialDuplicate> root )
    {
        List<Predicate> predicateList = new ArrayList<>();

        predicateList.add( root.get( "status" ).in( getInStatusValue( query.getStatus() ) ) );

        if ( !query.getTeis().isEmpty() )
        {
            predicateList.add( builder.and(
                builder.or(
                    root.get( "original" ).in( query.getTeis() ),
                    root.get( "duplicate" ).in( query.getTeis() ) ) ) );
        }

        return predicateList.toArray( new Predicate[0] );
    }

    private List<DeduplicationStatus> getInStatusValue( DeduplicationStatus status )
    {
        return status == DeduplicationStatus.ALL ? Arrays.stream( DeduplicationStatus.values() )
            .filter( s -> s != DeduplicationStatus.ALL ).collect( Collectors.toList() )
            : Collections.singletonList( status );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public boolean exists( PotentialDuplicate potentialDuplicate )
        throws PotentialDuplicateConflictException
    {
        if ( potentialDuplicate.getOriginal() == null || potentialDuplicate.getDuplicate() == null )
        {
            throw new PotentialDuplicateConflictException(
                "Can't search for pair of potential duplicates: original and duplicate must not be null" );
        }

        NativeQuery<BigInteger> query = getSession()
            .createNativeQuery( "select count(potentialduplicateid) from potentialduplicate pd " +
                "where (pd.teia = :original and pd.teib = :duplicate) or (pd.teia = :duplicate and pd.teib = :original)" );

        query.setParameter( "original", potentialDuplicate.getOriginal() );
        query.setParameter( "duplicate", potentialDuplicate.getDuplicate() );

        return query.getSingleResult().intValue() != 0;
    }

    @Override
    public void moveTrackedEntityAttributeValues( TrackedEntityInstance original, TrackedEntityInstance duplicate,
        List<String> trackedEntityAttributes )
    {
        // Collect existing teav from original for the tea list
        Map<String, TrackedEntityAttributeValue> originalAttributeValueMap = new HashMap<>();
        original.getTrackedEntityAttributeValues().forEach( oav -> {
            if ( trackedEntityAttributes.contains( oav.getAttribute().getUid() ) )
            {
                originalAttributeValueMap.put( oav.getAttribute().getUid(), oav );
            }
        } );

        duplicate.getTrackedEntityAttributeValues()
            .stream()
            .filter( av -> trackedEntityAttributes.contains( av.getAttribute().getUid() ) )
            .forEach( av -> {

                TrackedEntityAttributeValue updatedTeav;
                org.hisp.dhis.common.AuditType auditType;
                if ( originalAttributeValueMap.containsKey( av.getAttribute().getUid() ) )
                {
                    // Teav exists in original, overwrite the value
                    updatedTeav = originalAttributeValueMap.get( av.getAttribute().getUid() );
                    updatedTeav.setValue( av.getValue() );
                    auditType = UPDATE;
                }
                else
                {
                    // teav does not exist in original, so create new and attach
                    // it to original
                    updatedTeav = new TrackedEntityAttributeValue();
                    updatedTeav.setAttribute( av.getAttribute() );
                    updatedTeav.setEntityInstance( original );
                    updatedTeav.setValue( av.getValue() );
                    auditType = CREATE;
                }
                getSession().delete( av );
                // We need to flush to make sure the previous teav is
                // deleted.
                // Or else we might end up breaking a
                // constraint, since hibernate does not respect order.
                getSession().flush();

                getSession().saveOrUpdate( updatedTeav );

                auditTeav( av, updatedTeav, auditType );

            } );
    }

    private void auditTeav( TrackedEntityAttributeValue av, TrackedEntityAttributeValue createOrUpdateTeav,
        org.hisp.dhis.common.AuditType auditType )
    {
        String currentUsername = currentUserService.getCurrentUsername();

        TrackedEntityAttributeValueAudit deleteTeavAudit = new TrackedEntityAttributeValueAudit( av, av.getAuditValue(),
            currentUsername, DELETE );
        TrackedEntityAttributeValueAudit updatedTeavAudit = new TrackedEntityAttributeValueAudit( createOrUpdateTeav,
            createOrUpdateTeav.getValue(), currentUsername, auditType );

        if ( config.isEnabled( CHANGELOG_TRACKER ) )
        {
            trackedEntityAttributeValueAuditStore.addTrackedEntityAttributeValueAudit( deleteTeavAudit );
            trackedEntityAttributeValueAuditStore.addTrackedEntityAttributeValueAudit( updatedTeavAudit );
        }
    }

    @Override
    public void moveRelationships( TrackedEntityInstance original, TrackedEntityInstance duplicate,
        List<String> relationships )
    {
        duplicate.getRelationshipItems()
            .stream()
            .filter( r -> relationships.contains( r.getRelationship().getUid() ) )
            .forEach( ri -> {
                ri.setTrackedEntityInstance( original );

                getSession().update( ri );
            } );
    }

    @Override
    public void moveEnrollments( TrackedEntityInstance original, TrackedEntityInstance duplicate,
        List<String> enrollments )
    {
        List<Enrollment> enrollmentList = duplicate.getEnrollments()
            .stream()
            .filter( e -> !e.isDeleted() )
            .filter( e -> enrollments.contains( e.getUid() ) )
            .collect( Collectors.toList() );

        enrollmentList.forEach( duplicate.getEnrollments()::remove );

        enrollmentList.forEach( e -> {
            e.setEntityInstance( original );
            e.setLastUpdatedBy( currentUserService.getCurrentUser() );
            e.setLastUpdatedByUserInfo( UserInfoSnapshot.from( currentUserService.getCurrentUser() ) );
            e.setLastUpdated( new Date() );
            getSession().update( e );
        } );

        // Flush to update records before we delete duplicate, or else it might
        // be soft-deleted by hibernate.
        getSession().flush();
    }

    @Override
    public void removeTrackedEntity( TrackedEntityInstance trackedEntityInstance )
    {
        trackedEntityInstanceStore.delete( trackedEntityInstance );
    }

    @Override
    public void auditMerge( DeduplicationMergeParams params )
    {
        TrackedEntityInstance duplicate = params.getDuplicate();
        MergeObject mergeObject = params.getMergeObject();

        mergeObject.getRelationships().forEach( rel -> {
            duplicate.getRelationshipItems().stream()
                .map( RelationshipItem::getRelationship )
                .filter( r -> r.getUid().equals( rel ) )
                .findAny()
                .ifPresent( relationship -> auditManager.send( Audit.builder()
                    .auditScope( AuditScope.TRACKER )
                    .auditType( AuditType.UPDATE )
                    .createdAt( LocalDateTime.now() )
                    .object( relationship )
                    .klass( HibernateProxyUtils.getRealClass( relationship ).getCanonicalName() )
                    .uid( rel )
                    .auditableEntity( new AuditableEntity( Relationship.class, relationship ) )
                    .build() ) );
        } );
    }
}
