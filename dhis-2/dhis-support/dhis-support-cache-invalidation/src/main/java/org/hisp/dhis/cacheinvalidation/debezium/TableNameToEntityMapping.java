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
package org.hisp.dhis.cacheinvalidation.debezium;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.metamodel.EntityType;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.SessionFactory;
import org.hibernate.metamodel.internal.MetamodelImpl;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * TreeMap responsible for keeping the mapping between raw database tables names
 * to Hibernate entity classes. The mapping is constructed by inspecting the
 * Hibernate metadata model {@link MetamodelImpl}
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Profile( { "!test", "!test-h2" } )
@Component
public class TableNameToEntityMapping
{
    @Autowired
    private SessionFactory sessionFactory;

    private final TreeMap<String, List<Object[]>> tableNameToEntity = new TreeMap<>();

    protected void init()
    {
        extractTableNamesFromHibernateMetamodel();

        if ( log.isDebugEnabled() )
        {
            log.debug( "Finished extracting table names from the Hibernate metamodel. "
                + "tableNameToEntity=\n" + printEntityTable( tableNameToEntity ) );
        }
    }

    protected List<Object[]> getEntities( String tableName )
    {
        if ( tableNameToEntity.isEmpty() )
        {
            throw new IllegalStateException( "EntityToDbTableMapping is not initialized yet!" );
        }
        return tableNameToEntity.get( tableName );
    }

    private void extractTableNamesFromHibernateMetamodel()
    {
        for ( EntityType<?> entity : sessionFactory.getMetamodel().getEntities() )
        {
            extractTableNames( entity.getJavaType() );
        }
    }

    public void extractTableNames( final Class<?> modelClazz )
    {
        final MetamodelImpl metamodel = (MetamodelImpl) sessionFactory.getMetamodel();
        final EntityPersister entityPersister = metamodel.entityPersister( modelClazz );

        for ( int i = 0; i < entityPersister.getPropertyTypes().length; i++ )
        {
            Type type = entityPersister.getPropertyTypes()[i];

            if ( type.isCollectionType() )
            {
                CollectionType collectionType = (CollectionType) type;
                CollectionPersister collectionPersister = metamodel.collectionPersister( collectionType.getRole() );

                if ( collectionPersister instanceof BasicCollectionPersister )
                {
                    BasicCollectionPersister bc = (BasicCollectionPersister) collectionPersister;
                    String tableName = bc.getTableName();

                    tableNameToEntity.computeIfAbsent( tableName, s -> new ArrayList<>() )
                        .add( new Object[] { modelClazz, collectionType.getRole() } );
                }
            }
        }

        if ( entityPersister instanceof SingleTableEntityPersister )
        {
            String tableName = ((SingleTableEntityPersister) entityPersister).getTableName();

            tableNameToEntity.computeIfAbsent( tableName, s -> new ArrayList<>() )
                .add( new Object[] { modelClazz } );
        }
        else
        {
            throw new IllegalArgumentException( modelClazz + " does not map to a single table." );
        }
    }

    private String printEntityTable( TreeMap<String, List<Object[]>> entityToTableNames )
    {
        StringBuilder sb = new StringBuilder();
        for ( Map.Entry<String, List<Object[]>> entry : entityToTableNames.entrySet() )
        {
            sb.append( String.format(
                "Table=%s, Entities=%s %n", entry.getKey(), printEntityTableValue( entry.getValue() ) ) );
        }
        return sb.toString();
    }

    public static String printEntityTableValue( List<Object[]> value )
    {
        StringBuilder sb = new StringBuilder( "(" );
        ListIterator<Object[]> listIterator = value.listIterator();
        while ( listIterator.hasNext() )
        {
            Object[] objects = listIterator.next();
            if ( objects.length == 1 )
            {
                sb.append( String.format( " Name=%s ", objects[0] ) );
            }
            else if ( objects.length == 2 )
            {
                sb.append( String.format( " Name=%s, Role=%s ", objects[0], objects[1] ) );
            }
            if ( listIterator.hasNext() )
            {
                sb.append( ";" );
            }
        }
        sb.append( ")" );
        return sb.toString();
    }
}
