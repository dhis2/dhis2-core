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
package org.hisp.dhis.schema.introspection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManagerFactory;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CustomType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.SetType;
import org.hibernate.type.SingleColumnType;
import org.hibernate.type.TextType;
import org.hibernate.type.Type;
import org.hisp.dhis.hibernate.HibernateMetadata;
import org.hisp.dhis.schema.Property;

/**
 * A {@link PropertyIntrospector} that extract information from Hibernate
 * mappings.
 *
 * It assumes that no {@link PropertyIntrospector} has already provided any
 * baseline.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com> (original author)
 * @author Jan Bernitt (extraction to this class)
 */
public class HibernatePropertyIntrospector implements PropertyIntrospector
{
    private final EntityManagerFactory entityManagerFactory;

    private final Map<String, String> roleToRole = new ConcurrentHashMap<>();

    private final AtomicBoolean roleToRoleComputing = new AtomicBoolean( false );

    public HibernatePropertyIntrospector( EntityManagerFactory entityManager )
    {
        this.entityManagerFactory = entityManager;
    }

    private void updateJoinTables()
    {
        if ( !roleToRoleComputing.compareAndSet( false, true ) )
        {
            // when we already started the below computation we do not want to
            // do it again
            return;
        }

        Map<String, List<String>> joinTableToRoles = new HashMap<>();

        SessionFactoryImplementor sessionFactoryImplementor = entityManagerFactory
            .unwrap( SessionFactoryImplementor.class );

        MetamodelImplementor metamodelImplementor = sessionFactoryImplementor.getMetamodel();

        for ( CollectionPersister collectionPersister : metamodelImplementor.collectionPersisters().values() )
        {
            CollectionType collectionType = collectionPersister.getCollectionType();

            if ( collectionPersister.isManyToMany() && collectionType.isAssociationType() )
            {
                Joinable associatedJoinable = collectionType.getAssociatedJoinable( sessionFactoryImplementor );

                if ( !joinTableToRoles.containsKey( associatedJoinable.getTableName() ) )
                {
                    joinTableToRoles.put( associatedJoinable.getTableName(), new ArrayList<>() );
                }

                joinTableToRoles.get( associatedJoinable.getTableName() ).add( collectionPersister.getRole() );
            }
            else if ( collectionPersister.isInverse() && collectionType instanceof SetType )
            {
                SetType setType = (SetType) collectionType;
                setType.getAssociatedJoinable( sessionFactoryImplementor );
            }
        }

        joinTableToRoles.entrySet().removeIf( entry -> entry.getValue().size() < 2 );

        for ( Map.Entry<String, List<String>> entry : joinTableToRoles.entrySet() )
        {
            roleToRole.put( entry.getValue().get( 0 ), entry.getValue().get( 1 ) );
            roleToRole.put( entry.getValue().get( 1 ), entry.getValue().get( 0 ) );
        }
    }

    @Override
    public void introspect( Class<?> klass, Map<String, Property> properties )
    {
        updateJoinTables();
        MetamodelImplementor metamodelImplementor = getMetamodelImplementor();

        try
        {
            metamodelImplementor.entityPersister( klass );
        }
        catch ( MappingException ex )
        {
            // Class is not persisted with Hibernate
            return;
        }

        MetadataImplementor metadataImplementor = HibernateMetadata.getMetadataImplementor();

        if ( metadataImplementor == null )
        {
            return;
        }

        PersistentClass persistentClass = metadataImplementor.getEntityBinding( klass.getName() );

        Iterator<?> propertyIterator = persistentClass.getPropertyClosureIterator();

        while ( propertyIterator.hasNext() )
        {
            Property property = createProperty( klass, (org.hibernate.mapping.Property) propertyIterator.next(),
                metamodelImplementor );
            properties.put( property.getName(), property );
        }
    }

    private MetamodelImplementor getMetamodelImplementor()
    {
        return entityManagerFactory.unwrap( SessionFactoryImplementor.class ).getMetamodel();
    }

    private Property createProperty( Class<?> klass, org.hibernate.mapping.Property hibernateProperty,
        MetamodelImplementor metamodelImplementor )
    {
        Property property = new Property( klass );
        property.setRequired( false );
        property.setPersisted( true );
        property.setWritable( true );
        property.setOwner( true );

        Type type = hibernateProperty.getType();

        property.setName( hibernateProperty.getName() );
        property.setFieldName( hibernateProperty.getName() );
        property.setCascade( hibernateProperty.getCascade() );
        property.setCollection( type.isCollectionType() );

        property.setSetterMethod( hibernateProperty.getSetter( klass ).getMethod() );
        property.setGetterMethod( hibernateProperty.getGetter( klass ).getMethod() );

        if ( property.isCollection() )
        {
            initCollectionProperty( metamodelImplementor, property, (CollectionType) type );
        }

        if ( type instanceof SingleColumnType || type instanceof CustomType || type instanceof ManyToOneType )
        {
            Column column = (Column) hibernateProperty.getColumnIterator().next();

            property.setUnique( column.isUnique() );
            property.setRequired( !column.isNullable() );
            property.setMin( 0d );
            property.setMax( (double) column.getLength() );
            property.setLength( column.getLength() );

            if ( type instanceof TextType )
            {
                property.setMin( 0d );
                property.setMax( (double) Integer.MAX_VALUE );
                property.setLength( Integer.MAX_VALUE );
            }
            else if ( type instanceof IntegerType )
            {
                property.setMin( (double) Integer.MIN_VALUE );
                property.setMax( (double) Integer.MAX_VALUE );
                property.setLength( Integer.MAX_VALUE );
            }
            else if ( type instanceof LongType )
            {
                property.setMin( (double) Long.MIN_VALUE );
                property.setMax( (double) Long.MAX_VALUE );
                property.setLength( Integer.MAX_VALUE );
            }
            else if ( type instanceof DoubleType )
            {
                property.setMin( -Double.MAX_VALUE );
                property.setMax( Double.MAX_VALUE );
                property.setLength( Integer.MAX_VALUE );
            }
        }

        if ( type instanceof ManyToOneType )
        {
            property.setManyToOne( true );
            property.setRequired( property.isRequired() && !property.isCollection() );

            if ( property.isOwner() )
            {
                property.setOwningRole( klass.getName() + "." + property.getName() );
            }
            else
            {
                property.setInverseRole( klass.getName() + "." + property.getName() );
            }
        }
        else if ( type instanceof OneToOneType )
        {
            property.setOneToOne( true );
        }
        else if ( type instanceof OneToMany )
        {
            property.setOneToMany( true );
        }
        return property;
    }

    private void initCollectionProperty( MetamodelImplementor metamodelImplementor, Property property,
        CollectionType type )
    {
        CollectionPersister persister = metamodelImplementor.collectionPersister( type.getRole() );

        property.setOwner( !persister.isInverse() );
        property.setManyToMany( persister.isManyToMany() );
        property.setOneToMany( persister.isOneToMany() );

        property.setMin( 0d );
        property.setMax( Double.MAX_VALUE );

        if ( property.isOwner() )
        {
            property.setOwningRole( type.getRole() );
            property.setInverseRole( roleToRole.get( type.getRole() ) );
        }
        else
        {
            property.setOwningRole( roleToRole.get( type.getRole() ) );
            property.setInverseRole( type.getRole() );
        }
    }
}
