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

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.gist.GistBuilder.createCountBuilder;
import static org.hisp.dhis.gist.GistBuilder.createFetchBuilder;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
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

import lombok.AllArgsConstructor;

/**
 * @author Jan Bernitt
 */
@Service
@AllArgsConstructor
public class DefaultGistService implements GistService
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
        GistAccessControl access = createGistAccessControl();
        RelativePropertyContext context = createPropertyContext( query );
        validator.validateQuery( query, context );
        GistBuilder queryBuilder = createFetchBuilder( query, context, access,
            this::getUserGroupIdsByUserId );
        List<Object[]> rows = fetchWithParameters( query, queryBuilder,
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
                GistBuilder countBuilder = createCountBuilder( query, context, access,
                    this::getUserGroupIdsByUserId );
                total = countWithParameters( countBuilder,
                    getSession().createQuery( countBuilder.buildCountHQL(), Long.class ) );
            }
        }
        if ( schema.haveApiEndpoint() )
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

    private GistAccessControl createGistAccessControl()
    {
        return new DefaultGistAccessControl( currentUserService.getCurrentUser(), aclService );
    }

    private RelativePropertyContext createPropertyContext( GistQuery query )
    {
        return new RelativePropertyContext( query.getElementType(), schemaService::getDynamicSchema );
    }

    private <T> List<T> fetchWithParameters( GistQuery gistQuery, GistBuilder builder, Query<T> query )
    {
        builder.addFetchParameters( query::setParameter, this::parseFilterArgument );
        query.setMaxResults( gistQuery.getPageSize() );
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

    private List<String> getUserGroupIdsByUserId( String userId )
    {
        return userService.getUser( userId ).getGroups().stream().map( IdentifiableObject::getUid ).collect( toList() );
    }
}
