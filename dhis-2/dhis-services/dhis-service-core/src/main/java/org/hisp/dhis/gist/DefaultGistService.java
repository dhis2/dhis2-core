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
package org.hisp.dhis.gist;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.gist.GistBuilder.createCountBuilder;
import static org.hisp.dhis.gist.GistBuilder.createFetchBuilder;

import java.net.URI;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.schema.RelativePropertyContext;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Jan Bernitt
 */
@Service
@RequiredArgsConstructor
public class DefaultGistService
    implements GistService, GistBuilder.GistBuilderSupport
{
    /**
     * Instead of an actual date value users may use string {@code now} to
     * always get current moment as time for a {@link Date} value.
     */
    private static final String NOW_PARAMETER_VALUE = "now";

    private final SessionFactory sessionFactory;

    private final SchemaService schemaService;

    private final UserService userService;

    private final CurrentUserService currentUserService;

    private final AclService aclService;

    private final AttributeService attributeService;

    private final ObjectMapper jsonMapper;

    private Session getSession()
    {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public GistQuery plan( GistQuery query )
    {
        return new GistPlanner( query, createPropertyContext( query ), createGistAccessControl() ).plan();
    }

    @Override
    public List<?> gist( GistQuery query )
    {
        GistAccessControl access = createGistAccessControl();
        RelativePropertyContext context = createPropertyContext( query );
        new GistValidator( query, context, access ).validateQuery();
        GistBuilder queryBuilder = createFetchBuilder( query, context, access, this );
        List<Object[]> rows = fetchWithParameters( query, queryBuilder,
            getSession().createQuery( queryBuilder.buildFetchHQL(), Object[].class ) );
        return queryBuilder.transform( rows );
    }

    @Override
    public GistPager pager( GistQuery query, List<?> rows, Map<String, String[]> params )
    {
        int page = 1 + (query.getPageOffset() / query.getPageSize());
        Schema schema = schemaService.getDynamicSchema( query.getElementType() );
        String prev = null;
        String next = null;
        Integer total = null;
        if ( query.isTotal() )
        {
            if ( rows.size() < query.getPageSize() && !rows.isEmpty() )
            {
                // NB. only do this when rows are returned as otherwise the page
                // simply might not exist which leads to zero rows
                total = query.getPageOffset() + rows.size();
            }
            else
            {
                GistAccessControl access = createGistAccessControl();
                RelativePropertyContext context = createPropertyContext( query );
                GistBuilder countBuilder = createCountBuilder( query, context, access, this );
                total = countWithParameters( countBuilder,
                    getSession().createQuery( countBuilder.buildCountHQL(), Long.class ) );
            }
        }
        if ( schema.hasApiEndpoint() )
        {
            URI baseURL = GistPager.computeBaseURL( query, params, schemaService::getDynamicSchema );
            if ( page > 1 )
            {
                prev = UriComponentsBuilder.fromUri( baseURL ).replaceQueryParam( "page", page - 1 ).build().toString();
            }
            if ( total != null && query.getPageOffset() + rows.size() < total
                || total == null && query.getPageSize() == rows.size() )
            {
                next = UriComponentsBuilder.fromUri( baseURL ).replaceQueryParam( "page", page + 1 ).build().toString();
            }
        }
        return new GistPager( page, query.getPageSize(), total, prev, next );
    }

    @Override
    public Map<String, ?> describe( GistQuery unplanned )
    {
        GistAccessControl access = createGistAccessControl();

        GistQuery planned = unplanned;
        Map<String, Object> description = new LinkedHashMap<>();
        description.put( "unplanned", unplanned );
        try
        {
            planned = plan( unplanned );
        }
        catch ( RuntimeException ex )
        {
            description.put( "error.type", ex.getClass().getName() );
            description.put( "error.message", ex.getMessage() );
            description.put( "status", "planning-failed" );
            return description;
        }

        RelativePropertyContext context = createPropertyContext( planned );

        // describe query
        description.put( "planned.summary", planned.getFieldNames() );
        description.put( "planned", planned );

        // describe validation
        try
        {
            new GistValidator( planned, context, access ).validateQuery();
        }
        catch ( RuntimeException ex )
        {
            description.put( "error.type", ex.getClass().getName() );
            description.put( "error.message", ex.getMessage() );
            description.put( "status", "validation-failed" );
            return description;
        }

        // describe HQL queries
        if ( access.canReadHQL() )
        {
            if ( planned.isTotal() )
            {
                description.put( "hql.count",
                    createCountBuilder( planned, context, access, this ).buildCountHQL() );
            }
            GistBuilder fetchBuilder = createFetchBuilder( planned, context, access, this );
            description.put( "hql.fetch", fetchBuilder.buildFetchHQL() );
            Map<String, Object> params = new LinkedHashMap<>();
            fetchBuilder.addFetchParameters( params::put, this::parseFilterArgument );
            description.put( "hql.parameters", params );
        }

        description.put( "status", "ok" );
        return description;
    }

    private GistAccessControl createGistAccessControl()
    {
        return new DefaultGistAccessControl( currentUserService.getCurrentUser(), aclService, userService, this );
    }

    private RelativePropertyContext createPropertyContext( GistQuery query )
    {
        return new RelativePropertyContext( query.getElementType(), schemaService::getDynamicSchema );
    }

    private <T> List<T> fetchWithParameters( GistQuery gistQuery, GistBuilder builder, Query<T> query )
    {
        builder.addFetchParameters( query::setParameter, this::parseFilterArgument );
        query.setMaxResults( Math.max( 1, gistQuery.getPageSize() ) );
        query.setFirstResult( gistQuery.getPageOffset() );
        query.setCacheable( false );
        return query.list();
    }

    private int countWithParameters( GistBuilder builder, Query<Long> query )
    {
        builder.addCountParameters( query::setParameter, this::parseFilterArgument );
        query.setCacheable( false );
        return query.getSingleResult().intValue();
    }

    @SuppressWarnings( "unchecked" )
    private <T> T parseFilterArgument( String value, Class<T> type )
    {
        if ( type == Date.class && NOW_PARAMETER_VALUE.equals( value ) )
        {
            return (T) new Date();
        }
        String valueAsJson = value;
        if ( !(Number.class.isAssignableFrom( type ) || type == Boolean.class || type == boolean.class) )
        {
            valueAsJson = '"' + value + '"';
        }
        try
        {
            return jsonMapper.readValue( valueAsJson, type );
        }
        catch ( JsonProcessingException e )
        {
            throw new IllegalArgumentException(
                String.format( "Type %s is not compatible with provided filter value: `%s`", type, value ) );
        }
    }

    @Override
    public List<String> getUserGroupIdsByUserId( String userId )
    {
        return userService.getUser( userId ).getGroups().stream().map( IdentifiableObject::getUid ).collect( toList() );
    }

    @Override
    public Attribute getAttributeById( String attributeId )
    {
        return attributeService.getAttribute( attributeId );
    }

    @Override
    public Object getTypedAttributeValue( Attribute attribute, String value )
    {
        if ( value == null || value.isBlank() )
        {
            return value;
        }
        try
        {
            return attribute.getValueType().isJson() ? jsonMapper.readTree( value ) : value;
        }
        catch ( JsonProcessingException e )
        {
            return value;
        }
    }

}
