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
package org.hisp.dhis.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
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
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.hibernate.HibernateMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public abstract class AbstractPropertyIntrospectorService
    implements PropertyIntrospectorService
{
    // Simple alias map for our concrete implementations of the core interfaces
<<<<<<< HEAD
    private static final ImmutableMap<Class<?>, Class<?>> BASE_ALIAS_MAP = ImmutableMap.<Class<?>, Class<?>>builder()
=======
    private static final ImmutableMap<Class<?>, Class<?>> BASE_ALIAS_MAP = ImmutableMap.<Class<?>, Class<?>> builder()
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
        .put( IdentifiableObject.class, BaseIdentifiableObject.class )
        .put( NameableObject.class, BaseNameableObject.class )
        .put( DimensionalObject.class, BaseDimensionalObject.class )
        .put( DimensionalItemObject.class, BaseDimensionalItemObject.class )
        .put( AnalyticalObject.class, BaseAnalyticalObject.class )
        .build();

    private final Map<Class<?>, Map<String, Property>> classMapCache = new HashMap<>();

    private final Map<String, String> roleToRole = new HashMap<>();

    @Autowired
    protected ApplicationContext context;

    @Autowired
    protected SessionFactory sessionFactory;

    @Override
    public List<Property> getProperties( Class<?> klass )
    {
        return Lists.newArrayList( getPropertiesMap( klass ).values() );
    }

    @Override
    public Map<String, Property> getPropertiesMap( Class<?> klass )
    {
        if ( BASE_ALIAS_MAP.containsKey( klass ) )
        {
            klass = BASE_ALIAS_MAP.get( klass );
        }

        if ( !classMapCache.containsKey( klass ) )
        {
            classMapCache.put( klass, scanClass( klass ) );
        }

        return classMapCache.get( klass );
    }

    @Override
    public Class<?> getConcreteClass( Class<?> klass )
    {
        if ( BASE_ALIAS_MAP.containsKey( klass ) )
        {
            return BASE_ALIAS_MAP.get( klass );
        }

        return klass;
    }

    private void updateJoinTables()
    {
        if ( !roleToRole.isEmpty() )
        {
            return;
        }

        Map<String, List<String>> joinTableToRoles = new HashMap<>();

        SessionFactoryImplementor sessionFactoryImplementor = (SessionFactoryImplementor) sessionFactory;
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
            else if ( collectionPersister.isInverse() )
            {
                if ( collectionType instanceof SetType )
                {
                    SetType setType = (SetType) collectionType;
                    setType.getAssociatedJoinable( sessionFactoryImplementor );
                }
            }
        }

        Iterator<Map.Entry<String, List<String>>> entryIterator = joinTableToRoles.entrySet().iterator();

        while ( entryIterator.hasNext() )
        {
            Map.Entry<String, List<String>> entry = entryIterator.next();

            if ( entry.getValue().size() < 2 )
            {
                entryIterator.remove();
            }
        }

        for ( Map.Entry<String, List<String>> entry : joinTableToRoles.entrySet() )
        {
            roleToRole.put( entry.getValue().get( 0 ), entry.getValue().get( 1 ) );
            roleToRole.put( entry.getValue().get( 1 ), entry.getValue().get( 0 ) );
        }
    }

    /**
     * Introspect a class and return a map with key=property-name, and
     * value=Property class.
     *
     * @param klass Class to scan
     * @return Map with key=property-name, and value=Property class
     */
    protected abstract Map<String, Property> scanClass( Class<?> klass );

    protected Map<String, Property> getPropertiesFromHibernate( Class<?> klass )
    {
        updateJoinTables();
        SessionFactoryImplementor sessionFactoryImplementor = (SessionFactoryImplementor) sessionFactory;
        MetamodelImplementor metamodelImplementor = sessionFactoryImplementor.getMetamodel();

        try
        {
            metamodelImplementor.entityPersister( klass );
        }
        catch ( MappingException ex )
        {
            // Class is not persisted with Hibernate
            return new HashMap<>();
        }

        Map<String, Property> properties = new HashMap<>();

        MetadataImplementor metadataImplementor = HibernateMetadata.getMetadataImplementor();

        if ( metadataImplementor == null )
        {
            return new HashMap<>();
        }

        PersistentClass persistentClass = metadataImplementor.getEntityBinding( klass.getName() );

        Iterator<?> propertyIterator = persistentClass.getPropertyClosureIterator();

        while ( propertyIterator.hasNext() )
        {
            Property property = new Property( klass );
            property.setRequired( false );
            property.setPersisted( true );
            property.setOwner( true );

            org.hibernate.mapping.Property hibernateProperty = (org.hibernate.mapping.Property) propertyIterator.next();
            Type type = hibernateProperty.getType();

            property.setName( hibernateProperty.getName() );
            property.setCascade( hibernateProperty.getCascade() );
            property.setCollection( type.isCollectionType() );

            property.setSetterMethod( hibernateProperty.getSetter( klass ).getMethod() );
            property.setGetterMethod( hibernateProperty.getGetter( klass ).getMethod() );

            if ( property.isCollection() )
            {
                CollectionType collectionType = (CollectionType) type;
                CollectionPersister persister = metamodelImplementor.collectionPersister( collectionType.getRole() );

                property.setOwner( !persister.isInverse() );
                property.setManyToMany( persister.isManyToMany() );

                property.setMin( 0d );
                property.setMax( Double.MAX_VALUE );

                if ( property.isOwner() )
                {
                    property.setOwningRole( collectionType.getRole() );
                    property.setInverseRole( roleToRole.get( collectionType.getRole() ) );
                }
                else
                {
                    property.setOwningRole( roleToRole.get( collectionType.getRole() ) );
                    property.setInverseRole( collectionType.getRole() );
                }
            }

            if ( type instanceof SingleColumnType || type instanceof CustomType
                || type instanceof ManyToOneType )
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

            properties.put( property.getName(), property );
        }

        return properties;
    }
}
