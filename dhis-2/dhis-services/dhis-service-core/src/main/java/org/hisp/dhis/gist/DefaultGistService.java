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
import static org.hisp.dhis.gist.GistBuilder.createCountBuilder;
import static org.hisp.dhis.gist.GistBuilder.createFetchBuilder;
import static org.hisp.dhis.gist.GistLogic.isCollectionSizeFilter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.gist.GistQuery.Comparison;
import org.hisp.dhis.gist.GistQuery.Field;
import org.hisp.dhis.gist.GistQuery.Filter;
import org.hisp.dhis.gist.GistQuery.Owner;
import org.hisp.dhis.schema.GistTransform;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.RelativePropertyContext;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    public GistQuery plan( GistQuery query )
    {
        return new GistPlanner( query, createPropertyContext( query ) ).plan();
    }

    @Override
    public List<?> gist( GistQuery query )
    {
        RelativePropertyContext context = createPropertyContext( query );
        validator.validateQuery( query, context );
        GistBuilder queryBuilder = createFetchBuilder( query, context );
        List<Object[]> rows = listWithParameters( query, context,
            getSession().createQuery( queryBuilder.buildFetchHQL(), Object[].class ) );
        queryBuilder.transform( rows );
        return rows;
    }

    @Override
    public GistPager pager( GistQuery query, List<?> rows, Map<String, String[]> params )
    {
        int page = 1 + (query.getPageOffset() / query.getPageSize());
        Schema schema = schemaService.getDynamicSchema( query.getElementType() );
        String prev = null;
        String next = null;
        Integer total = null;
        if ( schema.haveApiEndpoint() )
        {
            String baseURL = computeBaseURL( query, params );
            if ( page > 1 )
            {
                prev = baseURL + "page=" + (page - 1);
            }
            if ( rows.size() == query.getPageSize() )
            {
                next = baseURL + "page=" + (page + 1);
            }
        }
        if ( query.isTotal() )
        {
            if ( rows.size() < query.getPageSize() )
            {
                total = query.getPageOffset() + rows.size();
            }
            else
            {
                RelativePropertyContext context = createPropertyContext( query );
                GistBuilder countBuilder = createCountBuilder( query, context );
                total = countWithParameters( query, context,
                    getSession().createQuery( countBuilder.buildCountHQL(), Long.class ) );
            }
        }
        return new GistPager( page, query.getPageSize(), total, prev, next );
    }

    private String computeBaseURL( GistQuery query, Map<String, String[]> params )
    {
        StringBuilder url = new StringBuilder();
        url.append( query.getEndpointRoot() );
        Owner owner = query.getOwner();
        if ( owner != null )
        {
            Schema o = schemaService.getDynamicSchema( owner.getType() );
            url.append( o.getRelativeApiEndpoint() ).append( '/' ).append( owner.getId() ).append( '/' );
            Property p = o.getProperty( owner.getCollectionProperty() );
            url.append( p.key() ).append( "/gist" );
        }
        else
        {
            Schema s = schemaService.getDynamicSchema( query.getElementType() );
            url.append( s.getRelativeApiEndpoint() );
            url.append( "/gist" );
        }
        url.append( '?' );
        for ( Entry<String, String[]> param : params.entrySet() )
        {
            if ( !param.getKey().equals( "page" ) )
            {
                for ( String value : param.getValue() )
                {
                    appendNextParameter( url );
                    appendParameterKeyValue( url, param, value );
                }
            }
        }
        appendNextParameter( url );
        return url.toString();
    }

    private void appendParameterKeyValue( StringBuilder url, Entry<String, String[]> param, String value )
    {
        try
        {
            url.append( URLEncoder.encode( param.getKey(), "UTF-8" ) );
            url.append( '=' );
            url.append( URLEncoder.encode( value, "UTF-8" ) );
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new IllegalStateException( e );
        }
    }

    private void appendNextParameter( StringBuilder url )
    {
        if ( url.charAt( url.length() - 1 ) != '?' )
        {
            url.append( '&' );
        }
    }

    private RelativePropertyContext createPropertyContext( GistQuery query )
    {
        return new RelativePropertyContext( query.getElementType(), schemaService::getDynamicSchema );
    }

    private <T> List<T> listWithParameters( GistQuery query, RelativePropertyContext context,
        Query<T> dbQuery )
    {
        addFilterParameters( query, context, dbQuery );
        for ( Field field : query.getFields() )
        {
            if ( field.getTransformationArgument() != null && field.getTransformation() != GistTransform.PLUCK )
            {
                dbQuery.setParameter( "p_" + field.getPropertyPath(), field.getTransformationArgument() );
            }
        }
        dbQuery.setMaxResults( query.getPageSize() );
        dbQuery.setFirstResult( query.getPageOffset() );
        dbQuery.setCacheable( false );
        return dbQuery.list();
    }

    private int countWithParameters( GistQuery query, RelativePropertyContext context, Query<Long> dbQuery )
    {
        addFilterParameters( query, context, dbQuery );
        dbQuery.setCacheable( false );
        return ((Long) dbQuery.getSingleResult()).intValue();
    }

    private void addFilterParameters( GistQuery query, RelativePropertyContext context, Query<?> dbQuery )
    {
        Owner owner = query.getOwner();
        if ( owner != null )
        {
            dbQuery.setParameter( "OwnerId", owner.getId() );
        }
        int i = 0;
        for ( Filter filter : query.getFilters() )
        {
            Comparison operator = filter.getOperator();
            if ( !operator.isUnary() )
            {
                Property property = context.resolveMandatory( filter.getPropertyPath() );
                Object cmpValue = getParameterValue( property, filter );
                dbQuery.setParameter( "f_" + (i++), operator.isStringCompare()
                    ? completeLike( operator, (String) cmpValue )
                    : cmpValue );
            }
        }
    }

    private static Object completeLike( Comparison operator, String cmpValue )
    {
        switch ( operator )
        {
        case LIKE:
        case NOT_LIKE:
            return cmpValue.contains( "*" ) || cmpValue.contains( "?" )
                ? cmpValue.replace( "*", "%" ).replace( "?", "_" )
                : "%" + cmpValue + "%";
        case STARTS_LIKE:
        case STARTS_WITH:
        case NOT_STARTS_LIKE:
        case NOT_STARTS_WITH:
            return "%" + cmpValue;
        case ENDS_LIKE:
        case ENDS_WITH:
        case NOT_ENDS_LIKE:
        case NOT_ENDS_WITH:
            return cmpValue + "%";
        default:
            return cmpValue;
        }
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
        if ( isCollectionSizeFilter( filter, property ) )
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
