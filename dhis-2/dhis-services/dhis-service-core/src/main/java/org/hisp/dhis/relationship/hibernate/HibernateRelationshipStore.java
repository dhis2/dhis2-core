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
package org.hisp.dhis.relationship.hibernate;

import java.util.List;
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
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
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
    public List<Relationship> getByTrackedEntityInstance( TrackedEntityInstance tei )
    {
        TypedQuery<Relationship> relationshipTypedQuery = getRelationshipTypedQuery( tei );

        return getList( relationshipTypedQuery );
    }

    @Override
    public List<Relationship> getByProgramInstance( ProgramInstance pi )
    {
        TypedQuery<Relationship> relationshipTypedQuery = getRelationshipTypedQuery( pi );

        return getList( relationshipTypedQuery );
    }

    @Override
    public List<Relationship> getByProgramStageInstance( ProgramStageInstance psi )
    {
        TypedQuery<Relationship> relationshipTypedQuery = getRelationshipTypedQuery( psi );

        return getList( relationshipTypedQuery );
    }

    private <T extends IdentifiableObject> TypedQuery<Relationship> getRelationshipTypedQuery( T entity )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        CriteriaQuery<Relationship> relationshipItemCriteriaQuery = builder.createQuery( Relationship.class );
        Root<Relationship> root = relationshipItemCriteriaQuery.from( Relationship.class );

        setRelationshipItemCriteriaQueryExistsCondition( entity, builder, relationshipItemCriteriaQuery, root );

        return getSession().createQuery( relationshipItemCriteriaQuery );
    }

    private <T extends IdentifiableObject> void setRelationshipItemCriteriaQueryExistsCondition( T entity,
        CriteriaBuilder builder,
        CriteriaQuery<Relationship> relationshipItemCriteriaQuery, Root<Relationship> root )
    {
        String relationShipEntityType = getRelationShipEntityType( entity );

        Subquery<RelationshipItem> relationshipItemFromSubQuery = relationshipItemCriteriaQuery
            .subquery( RelationshipItem.class );
        Root<RelationshipItem> relationshipItemFromRoot = relationshipItemFromSubQuery.from( RelationshipItem.class );

        Predicate predicateFromEqualsId = builder.equal( root.get( "from" ), relationshipItemFromRoot.get( "id" ) );
        Predicate predicateFromEqualsTei = builder.equal( relationshipItemFromRoot.get( relationShipEntityType ),
            entity.getId() );

        relationshipItemFromSubQuery.where( predicateFromEqualsId, predicateFromEqualsTei );

        relationshipItemFromSubQuery.select( relationshipItemFromRoot.get( "id" ) );

        Subquery<RelationshipItem> relationshipItemToSubQuery = relationshipItemCriteriaQuery
            .subquery( RelationshipItem.class );
        Root<RelationshipItem> relationshipItemToRoot = relationshipItemToSubQuery.from( RelationshipItem.class );

        Predicate predicateToEqualsId = builder.equal( root.get( "to" ), relationshipItemToRoot.get( "id" ) );
        Predicate predicateToEqualsTei = builder.equal( relationshipItemToRoot.get( relationShipEntityType ),
            entity.getId() );

        relationshipItemToSubQuery.where( predicateToEqualsId, predicateToEqualsTei );

        relationshipItemToSubQuery.select( relationshipItemToRoot.get( "id" ) );

        relationshipItemCriteriaQuery.where( builder.or( builder.exists( relationshipItemFromSubQuery ),
            builder.exists( relationshipItemToSubQuery ) ) );

        relationshipItemCriteriaQuery.select( root );
    }

    private <T extends IdentifiableObject> String getRelationShipEntityType( T entity )
    {
        if ( entity instanceof TrackedEntityInstance )
            return TRACKED_ENTITY_INSTANCE;
        else if ( entity instanceof ProgramInstance )
            return PROGRAM_INSTANCE;
        else
            return PROGRAM_STAGE_INSTANCE;
    }

    @Override
    public List<Relationship> getByRelationshipType( RelationshipType relationshipType )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.join( "relationshipType" ), relationshipType ) ) );

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
    public List<String> getUidsByRelationshipKeys( List<String> relationshipKeyList )
    {
        List<Object> c = getSession()
            .createNativeQuery( new StringBuilder()
                .append( "WITH relationshipitemuids AS ( " )
                .append( "SELECT RI.relationshipitemid, coalesce(TE.uid, PI.uid, PSI.uid) as uid " )
                .append( "FROM relationshipitem RI " )
                .append(
                    "LEFT JOIN trackedentityinstance TE ON TE.trackedentityinstanceid = RI.trackedentityinstanceid " )
                .append( "LEFT JOIN programinstance PI ON PI.programinstanceid = RI.programinstanceid " )
                .append(
                    "LEFT JOIN programstageinstance PSI ON PSI.programstageinstanceid = RI.programstageinstanceid " )
                .append( ") " )
                .append( "SELECT R.uid " )
                .append( "FROM relationship R " )
                .append( "INNER JOIN relationshiptype RT ON RT.relationshiptypeid = R.relationshiptypeid " )
                .append( "INNER JOIN relationshipitemuids F ON F.relationshipitemid = R.from_relationshipitemid " )
                .append( "INNER JOIN relationshipitemuids T ON T.relationshipitemid = R.to_relationshipitemid " )
                .append( "WHERE concat(RT.uid, '-', F.uid, '-', T.uid) IN (:keys) " )
                .append( "OR (RT.bidirectional AND concat(RT.uid, '-', T.uid, '-', F.uid) IN (:keys)) " )
                .toString() )
            .setParameter( "keys", relationshipKeyList )
            .getResultList();

        return c.stream().map( String::valueOf ).collect( Collectors.toList() );

    }

    @Override
    public List<Relationship> getByUids( List<String> uids )
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
