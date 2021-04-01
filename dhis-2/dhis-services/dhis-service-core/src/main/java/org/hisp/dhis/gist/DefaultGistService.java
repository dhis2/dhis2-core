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
package org.hisp.dhis.gist;

import static java.util.Arrays.stream;
import static org.hisp.dhis.gist.GistBuilder.createBuilder;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.gist.GistQuery.Filter;
import org.hisp.dhis.gist.GistQuery.Owner;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.RelativePropertyContext;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Jan Bernitt
 */
@Slf4j
@Service
@AllArgsConstructor
public class DefaultGistService implements GistService
{

    private final SessionFactory sessionFactory;

    private final SchemaService schemaService;

    private final ObjectMapper jsonMapper;

    private final GistValidator validator;

    private Session getSession()
    {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public <T extends IdentifiableObject> GistQuery<T> plan( GistQuery<T> query )
    {
        return new GistPlanner<>( query, createPropertyContext( query ) ).plan();
    }

    @Override
    public <T extends IdentifiableObject> List<T> queryMemberItemsAsObjects( GistQuery<T> query )
    {
        RelativePropertyContext context = createPropertyContext( query );
        validator.validateQuery( query, context );
        return listWithParameters( query, context,
            getSession().createQuery( createBuilder( query, context ).buildHQL(),
                query.getElementType() ) );
    }

    @Override
    public List<?> gist( GistQuery<?> query )
    {
        RelativePropertyContext context = createPropertyContext( query );
        validator.validateQuery( query, context );
        GistBuilder queryBuilder = createBuilder( query, context );
        List<Object[]> rows = listWithParameters( query, context,
            getSession().createQuery( queryBuilder.buildHQL(), Object[].class ) );
        queryBuilder.transform( rows );
        return rows;
    }

    private RelativePropertyContext createPropertyContext( GistQuery<?> query )
    {
        return new RelativePropertyContext( query.getElementType(), schemaService::getDynamicSchema );
    }

    private <T> List<T> listWithParameters( GistQuery<?> query, RelativePropertyContext context,
        Query<T> dbQuery )
    {
        Owner owner = query.getOwner();
        if ( owner != null )
        {
            dbQuery.setParameter( "OwnerId", owner.getId() );
        }
        int i = 0;
        for ( Filter filter : query.getFilters() )
        {
            Property property = context.resolveMandatory( filter.getPropertyPath() );
            dbQuery.setParameter( "f_" + (i++), getParameterValue( property, filter ) );
        }
        dbQuery.setMaxResults( query.getPageSize() );
        dbQuery.setFirstResult( query.getPageOffset() );
        dbQuery.setCacheable( false );
        return dbQuery.list();
    }

    private Object getParameterValue( Property property, Filter filter )
    {
        String[] value = filter.getValue();
        if ( value.length == 0 )
        {
            return "";
        }
        if ( value.length == 1 )
        {
            return queryTypedValue( property, filter, value[0] );
        }
        return stream( value ).map( e -> queryTypedValue( property, filter, e ) ).toArray();
    }

    private Object queryTypedValue( Property property, Filter filter, String value )
    {
        if ( value == null || property.getKlass() == String.class )
        {
            return value;
        }
        if ( GistLogic.isCollectionSizeFilter( filter, property ) )
        {
            // TODO parse error handling
            return Integer.parseInt( value );
        }
        String valueAsJson = value;
        Class<?> itemType = GistLogic.getBaseType( property );
        if ( !(Number.class.isAssignableFrom( itemType ) || itemType == Boolean.class || itemType == boolean.class) )
        {
            valueAsJson = '"' + value + '"';
        }
        try
        {
            return jsonMapper.readValue( valueAsJson, itemType );
        }
        catch ( JsonProcessingException e )
        {
            throw new IllegalArgumentException( String
                .format( "Property `%s` of type %s is not compatible with provided filter value: `%s`",
                    property.getName(), itemType, value ) );
        }
    }
}
