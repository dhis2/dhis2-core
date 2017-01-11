package org.hisp.dhis.dxf2.metadata.collection;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
public class DefaultCollectionService
    implements CollectionService
{
    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private DbmsManager dbmsManager;

    @Autowired
    private HibernateCacheManager cacheManager;

    @Autowired
    private AclService aclService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private CurrentUserService currentUserService;

    @Override
    @SuppressWarnings( "unchecked" )
    public void addCollectionItems( IdentifiableObject object, String propertyName, List<IdentifiableObject> objects ) throws Exception
    {
        Schema schema = schemaService.getDynamicSchema( object.getClass() );

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), object ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        if ( !schema.haveProperty( propertyName ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Property " + propertyName + " does not exist on " + object.getClass().getName() ) );
        }

        Property property = schema.getProperty( propertyName );

        if ( !property.isCollection() || !property.isIdentifiableObject() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Only identifiable object collections can be added to." ) );
        }

        Collection<String> itemCodes = objects.stream().map( IdentifiableObject::getUid ).collect( Collectors.toList() );

        if ( itemCodes.isEmpty() )
        {
            return;
        }

        List<? extends IdentifiableObject> items = manager.get( ((Class<? extends IdentifiableObject>) property.getItemKlass()), itemCodes );

        manager.refresh( object );

        if ( property.isOwner() )
        {
            Collection<IdentifiableObject> collection = (Collection<IdentifiableObject>) property.getGetterMethod().invoke( object );

            for ( IdentifiableObject item : items )
            {
                if ( !collection.contains( item ) ) collection.add( item );
            }

            manager.update( object );
        }
        else
        {
            Schema owningSchema = schemaService.getDynamicSchema( property.getItemKlass() );
            Property owningProperty = owningSchema.propertyByRole( property.getOwningRole() );

            for ( IdentifiableObject item : items )
            {
                try
                {
                    Collection<IdentifiableObject> collection = (Collection<IdentifiableObject>) owningProperty.getGetterMethod().invoke( item );

                    if ( !collection.contains( object ) )
                    {
                        collection.add( object );
                        manager.update( item );
                    }
                }
                catch ( Exception ex )
                {
                }
            }
        }

        dbmsManager.clearSession();
        cacheManager.clearCache();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public void delCollectionItems( IdentifiableObject object, String propertyName, List<IdentifiableObject> objects ) throws Exception
    {
        Schema schema = schemaService.getDynamicSchema( object.getClass() );

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), object ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        if ( !schema.haveProperty( propertyName ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Property " + propertyName + " does not exist on " + object.getClass().getName() ) );
        }

        Property property = schema.getProperty( propertyName );

        if ( !property.isCollection() || !property.isIdentifiableObject() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Only identifiable object collections can be removed from." ) );
        }

        Collection<String> itemCodes = objects.stream().map( IdentifiableObject::getUid ).collect( Collectors.toList() );

        if ( itemCodes.isEmpty() )
        {
            return;
        }

        List<? extends IdentifiableObject> items = manager.get( ((Class<? extends IdentifiableObject>) property.getItemKlass()), itemCodes );

        manager.refresh( object );

        if ( property.isOwner() )
        {
            Collection<IdentifiableObject> collection = (Collection<IdentifiableObject>) property.getGetterMethod().invoke( object );

            for ( IdentifiableObject item : items )
            {
                if ( collection.contains( item ) ) collection.remove( item );
            }
        }
        else
        {
            Schema owningSchema = schemaService.getDynamicSchema( property.getItemKlass() );
            Property owningProperty = owningSchema.propertyByRole( property.getOwningRole() );

            for ( IdentifiableObject item : items )
            {
                try
                {
                    Collection<IdentifiableObject> collection = (Collection<IdentifiableObject>) owningProperty.getGetterMethod().invoke( item );

                    if ( collection.contains( object ) )
                    {
                        collection.remove( object );
                        manager.update( item );
                    }
                }
                catch ( Exception ex )
                {
                }
            }
        }

        manager.update( object );

        dbmsManager.clearSession();
        cacheManager.clearCache();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public void clearCollectionItems( IdentifiableObject object, String pvProperty ) throws WebMessageException, InvocationTargetException, IllegalAccessException
    {
        Schema schema = schemaService.getDynamicSchema( object.getClass() );

        if ( !schema.haveProperty( pvProperty ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Property " + pvProperty + " does not exist on " + object.getClass().getName() ) );
        }

        Property property = schema.getProperty( pvProperty );

        if ( !property.isCollection() || !property.isIdentifiableObject() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Only identifiable collections are allowed to be cleared." ) );
        }

        Collection<IdentifiableObject> collection = (Collection<IdentifiableObject>) property.getGetterMethod().invoke( object );

        manager.refresh( object );

        if ( property.isOwner() )
        {
            collection.clear();
            manager.update( object );
        }
        else
        {
            for ( IdentifiableObject itemObject : collection )
            {
                Schema itemSchema = schemaService.getDynamicSchema( property.getItemKlass() );
                Property itemProperty = itemSchema.propertyByRole( property.getOwningRole() );
                Collection<IdentifiableObject> itemCollection = (Collection<IdentifiableObject>) itemProperty.getGetterMethod().invoke( itemObject );
                itemCollection.remove( object );

                manager.update( itemObject );
                manager.refresh( itemObject );
            }
        }
    }
}
