/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.attribute;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.hibernate.SessionFactory;
import org.hisp.dhis.attribute.exception.NonUniqueAttributeValueException;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.util.SystemUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service( "org.hisp.dhis.attribute.AttributeService" )
public class DefaultAttributeService
    implements AttributeService
{
    private Cache<Attribute> attributeCache;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final AttributeStore attributeStore;

    private final IdentifiableObjectManager manager;

    private SessionFactory sessionFactory;

    private final Environment env;

    public DefaultAttributeService( AttributeStore attributeStore, IdentifiableObjectManager manager,
        SessionFactory sessionFactory, Environment env )
    {
        checkNotNull( attributeStore );
        checkNotNull( manager );

        this.attributeStore = attributeStore;
        this.manager = manager;
        this.sessionFactory = sessionFactory;
        this.env = env;
    }

    @PostConstruct
    public void init()
    {
        attributeCache = new SimpleCacheBuilder<Attribute>()
            .forRegion( "metadataAttributes" )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .withMaximumSize( SystemUtils.isTestRun( env.getActiveProfiles() ) ? 0 : 10000 ).build();
    }

    // -------------------------------------------------------------------------
    // Attribute implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void addAttribute( Attribute attribute )
    {
        attributeStore.save( attribute );
    }

    @Override
    @Transactional
    public void deleteAttribute( Attribute attribute )
    {
        attributeCache.invalidate( attribute.getUid() );
        attributeStore.delete( attribute );
    }

    @Override
    public void invalidateCachedAttribute( String attributeUid )
    {
        attributeCache.invalidate( attributeUid );
    }

    @Override
<<<<<<< HEAD
    @Transactional(readOnly = true)
=======
    @Transactional( readOnly = true )
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    public Attribute getAttribute( long id )
    {
        return attributeStore.get( id );
    }

    @Override
    public Attribute getAttribute( String uid )
    {
        Optional<Attribute> attribute = attributeCache.get( uid, attr -> attributeStore.getByUid( uid ) );
        return attribute.orElse( null );
    }

    @Override
    @Transactional( readOnly = true )
    public Attribute getAttributeByName( String name )
    {
        return attributeStore.getByName( name );
    }

    @Override
    @Transactional( readOnly = true )
    public Attribute getAttributeByCode( String code )
    {
        return attributeStore.getByCode( code );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Attribute> getAllAttributes()
    {
        return new ArrayList<>( attributeStore.getAll() );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Attribute> getAttributes( Class<?> klass )
    {
        return new ArrayList<>( attributeStore.getAttributes( klass ) );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Attribute> getMandatoryAttributes( Class<?> klass )
    {
        return new ArrayList<>( attributeStore.getMandatoryAttributes( klass ) );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Attribute> getUniqueAttributes( Class<?> klass )
    {
        return new ArrayList<>( attributeStore.getUniqueAttributes( klass ) );
    }

    // -------------------------------------------------------------------------
    // AttributeValue implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public <T extends IdentifiableObject> void addAttributeValue( T object, AttributeValue attributeValue )
        throws NonUniqueAttributeValueException
    {
        if ( object == null || attributeValue == null || attributeValue.getAttribute() == null )
        {
            return;
        }

        Attribute attribute = getAttribute( attributeValue.getAttribute().getUid() );

        if ( Objects.isNull( attribute ) || !attribute.getSupportedClasses().contains( object.getClass() ) )
        {
            return;
        }
        if ( attribute.isUnique() )
        {

            if ( !manager.isAttributeValueUnique( object.getClass(), object, attributeValue ) )
            {
                throw new NonUniqueAttributeValueException( attributeValue );
            }
        }

        object.getAttributeValues().add( attributeValue );
        sessionFactory.getCurrentSession().save( object );
    }

    @Override
    @Transactional
    public <T extends IdentifiableObject> void deleteAttributeValue( T object, AttributeValue attributeValue )
    {
        object.getAttributeValues()
            .removeIf( a -> a.getAttribute() == attributeValue.getAttribute() );
        manager.update( object );
    }

    @Override
    @Transactional
    public <T extends IdentifiableObject> void deleteAttributeValues( T object, Set<AttributeValue> attributeValues )
    {
        object.getAttributeValues().removeAll( attributeValues );

        manager.update( object );
    }

    @Override
    @Transactional( readOnly = true )
    public <T extends IdentifiableObject> void generateAttributes( List<T> entityList )
    {
        entityList.forEach( entity -> entity.getAttributeValues()
<<<<<<< HEAD
            .forEach( attributeValue -> attributeValue.setAttribute( getAttribute( attributeValue.getAttribute().getUid() ) ) ) );
=======
            .forEach( attributeValue -> attributeValue
                .setAttribute( getAttribute( attributeValue.getAttribute().getUid() ) ) ) );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    }
}
