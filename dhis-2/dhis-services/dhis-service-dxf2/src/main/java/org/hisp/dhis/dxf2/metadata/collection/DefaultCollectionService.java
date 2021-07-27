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
package org.hisp.dhis.dxf2.metadata.collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.validateAndThrowErrors;

import java.util.Collection;
import java.util.List;

import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.validation.SchemaValidator;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service( "org.hisp.dhis.dxf2.metadata.collection.CollectionService" )
public class DefaultCollectionService
    implements CollectionService
{
    private final IdentifiableObjectManager manager;

    private final DbmsManager dbmsManager;

    private final HibernateCacheManager cacheManager;

    private final AclService aclService;

    private final SchemaService schemaService;

    private final CurrentUserService currentUserService;

    private final SchemaValidator schemaValidator;

    public DefaultCollectionService( IdentifiableObjectManager manager, DbmsManager dbmsManager,
        HibernateCacheManager cacheManager, AclService aclService, SchemaService schemaService,
        CurrentUserService currentUserService, SchemaValidator schemaValidator )
    {
        checkNotNull( manager );
        checkNotNull( dbmsManager );
        checkNotNull( cacheManager );
        checkNotNull( aclService );
        checkNotNull( schemaService );
        checkNotNull( currentUserService );
        checkNotNull( schemaValidator );

        this.manager = manager;
        this.dbmsManager = dbmsManager;
        this.cacheManager = cacheManager;
        this.aclService = aclService;
        this.schemaService = schemaService;
        this.currentUserService = currentUserService;
        this.schemaValidator = schemaValidator;
    }

    @Override
    @Transactional
    public void addCollectionItems( IdentifiableObject object, String propertyName,
        Collection<? extends IdentifiableObject> objects )
        throws Exception
    {
        Property property = validateUpdate( object, propertyName,
            "Only identifiable object collections can be added to." );

        Collection<String> itemCodes = getItemCodes( objects );

        if ( itemCodes.isEmpty() )
        {
            return;
        }

        manager.refresh( object );

        if ( property.isOwner() )
        {
            addOwnedCollectionItems( object, property, itemCodes );
        }
        else
        {
            addNonOwnedCollectionItems( object, property, itemCodes );
        }

        dbmsManager.clearSession();
        cacheManager.clearCache();
    }

    private void addOwnedCollectionItems( IdentifiableObject object, Property property, Collection<String> itemCodes )
        throws Exception
    {
        Collection<IdentifiableObject> collection = getCollection( object, property );

        for ( IdentifiableObject item : getItems( property, itemCodes ) )
        {
            if ( !collection.contains( item ) )
                collection.add( item );
        }
        validateAndThrowErrors( () -> schemaValidator.validateProperty( property, object ) );
        manager.update( object );
    }

    private void addNonOwnedCollectionItems( IdentifiableObject object, Property property,
        Collection<String> itemCodes )
    {
        Schema owningSchema = schemaService.getDynamicSchema( property.getItemKlass() );
        Property owningProperty = owningSchema.propertyByRole( property.getOwningRole() );

        for ( IdentifiableObject item : getItems( property, itemCodes ) )
        {
            try
            {
                Collection<IdentifiableObject> collection = getCollection( item, owningProperty );

                if ( !collection.contains( object ) )
                {
                    collection.add( object );
                    validateAndThrowErrors( () -> schemaValidator.validateProperty( owningProperty, object ) );
                    manager.update( item );
                }
            }
            catch ( Exception ex )
            {
                /* Ignore */
            }
        }
    }

    @Override
    @Transactional
    public void delCollectionItems( IdentifiableObject object, String propertyName,
        Collection<? extends IdentifiableObject> objects )
        throws Exception
    {
        Property property = validateUpdate( object, propertyName,
            "Only identifiable object collections can be removed from." );

        Collection<String> itemCodes = getItemCodes( objects );

        if ( itemCodes.isEmpty() )
        {
            return;
        }

        manager.refresh( object );

        if ( property.isOwner() )
        {
            delOwnedCollectionItems( object, property, itemCodes );
        }
        else
        {
            delNonOwnedCollectionItems( object, property, itemCodes );
        }

        validateAndThrowErrors( () -> schemaValidator.validateProperty( property, object ) );
        manager.update( object );

        dbmsManager.clearSession();
        cacheManager.clearCache();
    }

    private void delOwnedCollectionItems( IdentifiableObject object, Property property, Collection<String> itemCodes )
        throws Exception
    {
        Collection<IdentifiableObject> collection = getCollection( object, property );

        for ( IdentifiableObject item : getItems( property, itemCodes ) )
        {
            collection.remove( item );
        }
    }

    private void delNonOwnedCollectionItems( IdentifiableObject object, Property property,
        Collection<String> itemCodes )
    {
        Schema owningSchema = schemaService.getDynamicSchema( property.getItemKlass() );
        Property owningProperty = owningSchema.propertyByRole( property.getOwningRole() );

        for ( IdentifiableObject item : getItems( property, itemCodes ) )
        {
            try
            {
                Collection<IdentifiableObject> collection = getCollection( item, owningProperty );

                if ( collection.contains( object ) )
                {
                    collection.remove( object );
                    validateAndThrowErrors( () -> schemaValidator.validateProperty( owningProperty, item ) );
                    manager.update( item );
                }
            }
            catch ( Exception ex )
            {
                /* Ignore */
            }
        }
    }

    @Override
    @Transactional
    public void replaceCollectionItems( IdentifiableObject object, String propertyName,
        Collection<? extends IdentifiableObject> objects )
        throws Exception
    {
        Property property = validateUpdate( object, propertyName,
            "Only identifiable object collections can be replaced." );

        delCollectionItems( object, propertyName, getCollection( object, property ) );
        addCollectionItems( object, propertyName, objects );
    }

    private Property validateUpdate( IdentifiableObject object, String propertyName, String message )
        throws WebMessageException
    {
        Schema schema = schemaService.getDynamicSchema( HibernateProxyUtils.getRealClass( object ) );

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), object ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        if ( !schema.haveProperty( propertyName ) )
        {
            throw new WebMessageException( WebMessageUtils
                .notFound( "Property " + propertyName + " does not exist on " + object.getClass().getName() ) );
        }

        Property property = schema.getProperty( propertyName );

        if ( !property.isCollection() || !property.isIdentifiableObject() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( message ) );
        }
        return property;
    }

    private Collection<String> getItemCodes( Collection<? extends IdentifiableObject> objects )
    {
        return objects.stream().map( IdentifiableObject::getUid ).collect( toList() );
    }

    @SuppressWarnings( "unchecked" )
    private List<? extends IdentifiableObject> getItems( Property property, Collection<String> itemCodes )
    {
        return manager.getByUid( ((Class<? extends IdentifiableObject>) property.getItemKlass()), itemCodes );
    }

    @SuppressWarnings( "unchecked" )
    private Collection<IdentifiableObject> getCollection( IdentifiableObject object, Property property )
        throws Exception
    {
        return (Collection<IdentifiableObject>) property.getGetterMethod().invoke( object );
    }

}
