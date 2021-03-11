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

import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SessionFactory;
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
public class HibernateRelationshipStore
    extends HibernateIdentifiableObjectStore<Relationship>
    implements RelationshipStore
{
    public HibernateRelationshipStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CurrentUserService currentUserService, AclService aclService )
    {
        super( sessionFactory, jdbcTemplate, publisher, Relationship.class, currentUserService, aclService, true );
    }

    @Override
    public List<Relationship> getByTrackedEntityInstance( TrackedEntityInstance tei )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.or(
                builder.equal( root.join( "from" ).get( "trackedEntityInstance" ), tei ),
                builder.equal( root.join( "to" ).get( "trackedEntityInstance" ), tei ) ) ) );
    }

    @Override
    public List<Relationship> getByProgramInstance( ProgramInstance pi )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.or(
                builder.equal( root.join( "from" ).get( "programInstance" ), pi ),
                builder.equal( root.join( "to" ).get( "programInstance" ), pi ) ) ) );
    }

    @Override
    public List<Relationship> getByProgramStageInstance( ProgramStageInstance psi )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.or(
                builder.equal( root.join( "from" ).get( "programStageInstance" ), psi ),
                builder.equal( root.join( "to" ).get( "programStageInstance" ), psi ) ) ) );
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

        criteriaQuery.where( builder.and(
            getFromOrToPredicate( "from", builder, root, relationship ),
            getFromOrToPredicate( "to", builder, root, relationship ),
            builder.equal( root.join( "relationshipType" ), relationship.getRelationshipType() ) ) );

        try
        {
            return getSession().createQuery( criteriaQuery ).setMaxResults( 1 ).getSingleResult();
        }
        catch ( NoResultException nre )
        {
            return null;
        }

    }

    @Override
    public Relationship getByRelationshipKey( String relationshipKeyAsString )
    {
        RelationshipKey relationshipKey = RelationshipKey.fromString( relationshipKeyAsString );

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery<Relationship> criteriaQuery = criteriaBuilder.createQuery( Relationship.class );

        Root<Relationship> root = criteriaQuery.from( Relationship.class );

        Pair<String, String> fromFieldValuePair = getFieldValuePair( relationshipKey.getFrom() );
        Pair<String, String> toFieldValuePair = getFieldValuePair( relationshipKey.getTo() );

        criteriaQuery.where( criteriaBuilder.or(
            nonBidirectionalCriteria( criteriaBuilder, root, fromFieldValuePair, toFieldValuePair ),
            bidirectionalCriteria( criteriaBuilder, root, fromFieldValuePair, toFieldValuePair ) ) );

        try
        {
            return getSession().createQuery( criteriaQuery ).setMaxResults( 1 ).getSingleResult();
        }
        catch ( NoResultException nre )
        {
            return null;
        }
    }

    private Predicate bidirectionalCriteria( CriteriaBuilder criteriaBuilder, Root<Relationship> root,
        Pair<String, String> fromFieldValuePair, Pair<String, String> toFieldValuePair )
    {
        return criteriaBuilder.and(
            criteriaBuilder.equal(
                root.join( "relationshipType" ).get( "bidirectional" ), true ),
            criteriaBuilder.or(
                criteriaBuilder.and(
                    getRelatedEntityCriteria( criteriaBuilder, root, fromFieldValuePair, "from" ),
                    getRelatedEntityCriteria( criteriaBuilder, root, toFieldValuePair, "to" ) ),
                criteriaBuilder.and(
                    getRelatedEntityCriteria( criteriaBuilder, root, fromFieldValuePair, "to" ),
                    getRelatedEntityCriteria( criteriaBuilder, root, toFieldValuePair, "from" ) ) ) );
    }

    private Predicate getRelatedEntityCriteria( CriteriaBuilder criteriaBuilder, Root<Relationship> root,
        Pair<String, String> fromFieldValuePair, String from )
    {
        return criteriaBuilder.equal(
            root.join( from ).join( fromFieldValuePair.getKey() ).get( "uid" ),
            fromFieldValuePair.getValue() );
    }

    private Predicate nonBidirectionalCriteria( CriteriaBuilder criteriaBuilder, Root<Relationship> root,
        Pair<String, String> fromFieldValuePair, Pair<String, String> toFieldValuePair )
    {
        return criteriaBuilder.and(
            criteriaBuilder.equal(
                root.join( "relationshipType" ).get( "bidirectional" ), false ),
            criteriaBuilder.and(
                getRelatedEntityCriteria( criteriaBuilder, root, fromFieldValuePair, "from" ),
                getRelatedEntityCriteria( criteriaBuilder, root, toFieldValuePair, "to" ) ) );
    }

    private Pair<String, String> getFieldValuePair( RelationshipKey.RelationshipItemKey relationshipItemKey )
    {
        if ( relationshipItemKey.isTrackedEntity() )
        {
            return Pair.of( "trackedEntityInstance", relationshipItemKey.getTrackedEntity() );
        }
        if ( relationshipItemKey.isEnrollment() )
        {
            return Pair.of( "programInstance", relationshipItemKey.getEnrollment() );
        }
        if ( relationshipItemKey.isEvent() )
        {
            return Pair.of( "programStageInstance", relationshipItemKey.getEvent() );
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
            return builder.equal( root.join( direction ).get( "trackedEntityInstance" ),
                getItem( direction, relationship ).getTrackedEntityInstance() );
        }
        else if ( relationshipItemDirection.getProgramInstance() != null )
        {
            return builder.equal( root.join( direction ).get( "programInstance" ),
                getItem( direction, relationship ).getProgramInstance() );
        }
        else if ( relationshipItemDirection.getProgramStageInstance() != null )
        {
            return builder.equal( root.join( direction ).get( "programStageInstance" ),
                getItem( direction, relationship ).getProgramStageInstance() );
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
