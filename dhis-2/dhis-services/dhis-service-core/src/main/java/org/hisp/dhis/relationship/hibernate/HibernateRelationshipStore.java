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
package org.hisp.dhis.relationship.hibernate;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipKey;
import org.hisp.dhis.relationship.RelationshipStore;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Abyot Asalefew
 */
@Repository( "org.hisp.dhis.relationship.RelationshipStore" )
public class HibernateRelationshipStore extends HibernateIdentifiableObjectStore<Relationship>
    implements RelationshipStore
{
    private static final String TRACKED_ENTITY_INSTANCE = "trackedEntityInstance";

    private static final String PROGRAM_INSTANCE = "programInstance";

    private static final String PROGRAM_STAGE_INSTANCE = "programStageInstance";

    public HibernateRelationshipStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CurrentUserService currentUserService, AclService aclService )
    {
        super( sessionFactory, jdbcTemplate, publisher, Relationship.class, currentUserService, aclService, true );
    }

    @Override
    public List<Relationship> getByTrackedEntityInstance( TrackedEntityInstance tei,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter )
    {
        TypedQuery<Relationship> relationshipTypedQuery = getRelationshipTypedQuery( tei,
            pagingAndSortingCriteriaAdapter );

        return getList( relationshipTypedQuery );
    }

    @Override
    public List<Relationship> getByProgramInstance( ProgramInstance pi,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter )
    {
        TypedQuery<Relationship> relationshipTypedQuery = getRelationshipTypedQuery( pi,
            pagingAndSortingCriteriaAdapter );

        return getList( relationshipTypedQuery );
    }

    @Override
    public List<Relationship> getByProgramStageInstance( ProgramStageInstance psi,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter )
    {
        TypedQuery<Relationship> relationshipTypedQuery = getRelationshipTypedQuery( psi,
            pagingAndSortingCriteriaAdapter );

        return getList( relationshipTypedQuery );
    }

    private <T extends IdentifiableObject> TypedQuery<Relationship> getRelationshipTypedQuery( T entity,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        CriteriaQuery<Relationship> relationshipItemCriteriaQuery = builder.createQuery( Relationship.class );
        Root<Relationship> root = relationshipItemCriteriaQuery.from( Relationship.class );

        setRelationshipItemCriteriaQueryExistsCondition( entity, builder, relationshipItemCriteriaQuery, root );

        return getRelationshipTypedQuery( pagingAndSortingCriteriaAdapter, builder, relationshipItemCriteriaQuery,
            root );
    }

    private <T extends IdentifiableObject> void setRelationshipItemCriteriaQueryExistsCondition( T entity,
        CriteriaBuilder builder, CriteriaQuery<Relationship> relationshipItemCriteriaQuery, Root<Relationship> root )
    {
        Subquery<RelationshipItem> fromSubQuery = relationshipItemCriteriaQuery.subquery( RelationshipItem.class );
        Root<RelationshipItem> fromRoot = fromSubQuery.from( RelationshipItem.class );

        String relationshipEntityType = getRelationshipEntityType( entity );

        fromSubQuery.where( builder.equal( root.get( "from" ), fromRoot.get( "id" ) ),
            builder.equal( fromRoot.get( relationshipEntityType ),
                entity.getId() ) );

        fromSubQuery.select( fromRoot.get( "id" ) );

        Subquery<RelationshipItem> toSubQuery = relationshipItemCriteriaQuery.subquery( RelationshipItem.class );
        Root<RelationshipItem> toRoot = toSubQuery.from( RelationshipItem.class );

        toSubQuery.where( builder.equal( root.get( "to" ), toRoot.get( "id" ) ),
            builder.equal( toRoot.get( relationshipEntityType ),
                entity.getId() ) );

        toSubQuery.select( toRoot.get( "id" ) );

        relationshipItemCriteriaQuery
            .where( builder.or( builder.exists( fromSubQuery ), builder.exists( toSubQuery ) ) );

        relationshipItemCriteriaQuery.select( root );
    }

    private <T extends IdentifiableObject> String getRelationshipEntityType( T entity )
    {
        if ( entity instanceof TrackedEntityInstance )
            return TRACKED_ENTITY_INSTANCE;
        else if ( entity instanceof ProgramInstance )
            return PROGRAM_INSTANCE;
        else if ( entity instanceof ProgramStageInstance )
            return PROGRAM_STAGE_INSTANCE;
        else
            throw new IllegalArgumentException( entity.getClass()
                .getSimpleName() + " not supported in relationship" );
    }

    private TypedQuery<Relationship> getRelationshipTypedQuery(
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter, CriteriaBuilder builder,
        CriteriaQuery<Relationship> relationshipItemCriteriaQuery, Root<Relationship> root )
    {
        JpaQueryParameters<Relationship> jpaQueryParameters = newJpaParameters( pagingAndSortingCriteriaAdapter,
            builder );

        relationshipItemCriteriaQuery.orderBy( jpaQueryParameters.getOrders()
            .stream()
            .map( o -> o.apply( root ) )
            .collect( Collectors.toList() ) );

        TypedQuery<Relationship> relationshipTypedQuery = getSession().createQuery( relationshipItemCriteriaQuery );

        if ( jpaQueryParameters.hasFirstResult() )
        {
            relationshipTypedQuery.setFirstResult( jpaQueryParameters.getFirstResult() );
        }

        if ( jpaQueryParameters.hasMaxResult() )
        {
            relationshipTypedQuery.setMaxResults( jpaQueryParameters.getMaxResults() );
        }

        return relationshipTypedQuery;
    }

    @Override
    public List<Relationship> getByRelationshipType( RelationshipType relationshipType )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.join( "relationshipType" ), relationshipType ) ) );

    }

    @Override
    public boolean existsIncludingDeleted( String uid )
    {
        Query query = getSession().createNativeQuery( "select uid from relationship where uid=:uid limit 1;" );
        query.setParameter( "uid", uid );
        int count = ((Number) query.getSingleResult()).intValue();

        return count > 0;
    }

    private JpaQueryParameters<Relationship> newJpaParameters(
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter, CriteriaBuilder criteriaBuilder )
    {

        JpaQueryParameters<Relationship> jpaQueryParameters = newJpaParameters();

        if ( Objects.nonNull( pagingAndSortingCriteriaAdapter ) )
        {
            if ( pagingAndSortingCriteriaAdapter.isSortingRequest() )
            {
                pagingAndSortingCriteriaAdapter.getOrder()
                    .forEach( orderCriteria -> addOrder( jpaQueryParameters, orderCriteria, criteriaBuilder ) );
            }

            if ( pagingAndSortingCriteriaAdapter.isPagingRequest() )
            {
                jpaQueryParameters.setFirstResult( pagingAndSortingCriteriaAdapter.getFirstResult() );
                jpaQueryParameters.setMaxResults( pagingAndSortingCriteriaAdapter.getPageSize() );
            }
        }

        return jpaQueryParameters;
    }

    private void addOrder( JpaQueryParameters<Relationship> jpaQueryParameters, OrderCriteria orderCriteria,
        CriteriaBuilder builder )
    {
        jpaQueryParameters.addOrder( relationshipRoot -> orderCriteria.getDirection()
            .isAscending() ? builder.asc( relationshipRoot.get( orderCriteria.getField() ) )
                : builder.desc( relationshipRoot.get( orderCriteria.getField() ) ) );
    }

    @Override
    public Relationship getByRelationship( Relationship relationship )
    {
        CriteriaBuilder builder = getCriteriaBuilder();
        CriteriaQuery<Relationship> criteriaQuery = builder.createQuery( Relationship.class );

        Root<Relationship> root = criteriaQuery.from( Relationship.class );

        criteriaQuery.where( builder.and( getFromOrToPredicate( "from", builder, root, relationship ),
            getFromOrToPredicate( "to", builder, root, relationship ),
            builder.equal( root.join( "relationshipType" ), relationship.getRelationshipType() ) ) );

        try
        {
            return getSession().createQuery( criteriaQuery )
                .setMaxResults( 1 )
                .getSingleResult();
        }
        catch ( NoResultException nre )
        {
            return null;
        }

    }

    @Override
    public List<String> getUidsByRelationshipKeyIncludeDeleted( List<String> relationshipKeyList )
    {
        List<Object> c = getSession().createNativeQuery( new StringBuilder().append( "SELECT R.uid " )
            .append( "FROM relationship R " )
            .append( "INNER JOIN relationshiptype RT ON RT.relationshiptypeid = R.relationshiptypeid " )
            .append( "WHERE R.key IN (:keys) " )
            .append( "OR (R.inverted_key IN (:keys) AND RT.bidirectional = TRUE)" )
            .toString() )
            .setParameter( "keys", relationshipKeyList )
            .getResultList();

        return c.stream()
            .map( String::valueOf )
            .collect( Collectors.toList() );

    }

    @Override
    public List<Relationship> getByUidIncludeDeleted( List<String> uids )
    {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();

        CriteriaQuery<Relationship> query = criteriaBuilder.createQuery( Relationship.class );

        Root<Relationship> root = query.from( Relationship.class );

        query.where( criteriaBuilder.in( root.get( "uid" ) )
            .value( uids ) );

        try
        {
            return getSession().createQuery( query )
                .getResultList();
        }
        catch ( NoResultException nre )
        {
            return null;
        }
    }

    @Override
    protected void preProcessPredicates( CriteriaBuilder builder,
        List<Function<Root<Relationship>, Predicate>> predicates )
    {
        predicates.add( root -> builder.equal( root.get( "deleted" ), false ) );
    }

    @Override
    protected Relationship postProcessObject( Relationship relationship )
    {
        return (relationship == null || relationship.isDeleted()) ? null : relationship;
    }

    private Predicate bidirectionalCriteria( CriteriaBuilder criteriaBuilder, Root<Relationship> root,
        Pair<String, String> fromFieldValuePair, Pair<String, String> toFieldValuePair )
    {
        return criteriaBuilder.and( criteriaBuilder.equal( root.join( "relationshipType" )
            .get( "bidirectional" ), true ),
            criteriaBuilder.or(
                criteriaBuilder.and( getRelatedEntityCriteria( criteriaBuilder, root, fromFieldValuePair, "from" ),
                    getRelatedEntityCriteria( criteriaBuilder, root, toFieldValuePair, "to" ) ),
                criteriaBuilder.and( getRelatedEntityCriteria( criteriaBuilder, root, fromFieldValuePair, "to" ),
                    getRelatedEntityCriteria( criteriaBuilder, root, toFieldValuePair, "from" ) ) ) );
    }

    private Predicate getRelatedEntityCriteria( CriteriaBuilder criteriaBuilder, Root<Relationship> root,
        Pair<String, String> fromFieldValuePair, String from )
    {
        return criteriaBuilder.equal( root.join( from )
            .join( fromFieldValuePair.getKey() )
            .get( "uid" ), fromFieldValuePair.getValue() );
    }

    private Predicate nonBidirectionalCriteria( CriteriaBuilder criteriaBuilder, Root<Relationship> root,
        Pair<String, String> fromFieldValuePair, Pair<String, String> toFieldValuePair )
    {
        return criteriaBuilder.and( criteriaBuilder.equal( root.join( "relationshipType" )
            .get( "bidirectional" ), false ),
            criteriaBuilder.and( getRelatedEntityCriteria( criteriaBuilder, root, fromFieldValuePair, "from" ),
                getRelatedEntityCriteria( criteriaBuilder, root, toFieldValuePair, "to" ) ) );
    }

    private Pair<String, String> getFieldValuePair( RelationshipKey.RelationshipItemKey relationshipItemKey )
    {
        if ( relationshipItemKey.isTrackedEntity() )
        {
            return Pair.of( TRACKED_ENTITY_INSTANCE, relationshipItemKey.getTrackedEntity() );
        }
        if ( relationshipItemKey.isEnrollment() )
        {
            return Pair.of( PROGRAM_INSTANCE, relationshipItemKey.getEnrollment() );
        }
        if ( relationshipItemKey.isEvent() )
        {
            return Pair.of( PROGRAM_STAGE_INSTANCE, relationshipItemKey.getEvent() );
        }
        throw new IllegalStateException(
            "Unable to determine relationshipType for relationshipItem: " + relationshipItemKey.asString() );
    }

    private Predicate getFromOrToPredicate( String direction, CriteriaBuilder builder, Root<Relationship> root,
        Relationship relationship )
    {

        RelationshipItem relationshipItemDirection = getItem( direction, relationship );

        if ( relationshipItemDirection.getTrackedEntityInstance() != null )
        {
            return builder.equal( root.join( direction )
                .get( TRACKED_ENTITY_INSTANCE ), getItem( direction, relationship ).getTrackedEntityInstance() );
        }
        else if ( relationshipItemDirection.getProgramInstance() != null )
        {
            return builder.equal( root.join( direction )
                .get( PROGRAM_INSTANCE ), getItem( direction, relationship ).getProgramInstance() );
        }
        else if ( relationshipItemDirection.getProgramStageInstance() != null )
        {
            return builder.equal( root.join( direction )
                .get( PROGRAM_STAGE_INSTANCE ), getItem( direction, relationship ).getProgramStageInstance() );
        }
        else
        {
            return null;
        }
    }

    private RelationshipItem getItem( String direction, Relationship relationship )
    {
        return (direction.equalsIgnoreCase( "from" ) ? relationship.getFrom() : relationship.getTo());
    }
}
