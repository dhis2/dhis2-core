package org.hisp.dhis.query;

/*
 * Copyright (c) 2004-2020, University of Oslo
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



import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.organisationunit.OrganisationUnitQueryParams;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component( "org.hisp.dhis.query.QueryParser" )
public class DefaultQueryParser implements QueryParser
{
    private static final String IDENTIFIABLE = "identifiable";
    
    private static final String ORGANISATION_UNITS = "organisationUnits";

    private final SchemaService schemaService;
    
    private final OrganisationUnitService organisationUnitService;
    
    private final CurrentUserService currentUserService;

    @Autowired
    public DefaultQueryParser( SchemaService schemaService, CurrentUserService currentUserService, OrganisationUnitService organisationUnitService )
    {
        checkNotNull( schemaService );

        this.schemaService = schemaService;
        this.currentUserService = currentUserService;
        this.organisationUnitService = organisationUnitService;
    }

    @Override
    public Query parse( Class<?> klass, List<String> filters ) throws QueryParserException
    {
        return parse( klass, filters, Junction.Type.AND );
    }

    @Override
    public Query parse( Class<?> klass, List<String> filters, Junction.Type rootJunction ) throws QueryParserException
    {
        return parse( klass, filters, rootJunction, false );
    }
    
    @Override
    public Query parse( Class<?> klass, List<String> filters, Junction.Type rootJunction, boolean restrictToCaptureScope ) throws QueryParserException
    {
        Schema schema = schemaService.getDynamicSchema( klass );
        Query query = Query.from( schema, rootJunction );

        for ( String filter : filters )
        {
            String[] split = filter.split( ":" );

            if ( !(split.length >= 2) )
            {
                throw new QueryParserException( "Invalid filter => " + filter );
            }

            if ( split.length >= 3 )
            {
                int index = split[0].length() + ":".length() + split[1].length() + ":".length();

                if ( split[0].equals( IDENTIFIABLE ) && !schema.haveProperty( IDENTIFIABLE ) )
                {
                    handleIdentifiablePath( schema, split[1], filter.substring( index ), query.addDisjunction() );
                }
                else
                {
                    query.add( getRestriction( schema, split[0], split[1], filter.substring( index ) ) );
                }
            }
            else
            {
                query.add( getRestriction( schema, split[0], split[1], null ) );
            }
        }
        
        if ( restrictToCaptureScope && schema.haveProperty( ORGANISATION_UNITS ) )
        {
            User user = currentUserService.getCurrentUser();

            if ( user != null )
            {
                handleCaptureScopeOuFiltering( schema, user, query.addDisjunction() );
                query.setUser( user );
            }
        }
        return query;
    }

    private void handleCaptureScopeOuFiltering( Schema schema, User user, Disjunction disjunction )
    {

        OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
        params.setParents( user.getOrganisationUnits() );
        params.setFetchChildren( true );

        List<String> orgUnits = organisationUnitService.getOrganisationUnitsByQuery( params ).stream().map( orgUnit -> orgUnit.getUid() ).collect(
            Collectors.toList() );

        disjunction.add( getRestriction( schema, "organisationUnits.id", "in", "[" + String.join( ",", orgUnits ) + "]" ) );
        disjunction.add( getRestriction( schema, ORGANISATION_UNITS, "empty", null ) );
    }
    
    private void handleIdentifiablePath( Schema schema, String operator, Object arg, Disjunction disjunction )
    {
        disjunction.add( getRestriction( schema, "id", operator, arg ) );
        disjunction.add( getRestriction( schema, "code", operator, arg ) );
        disjunction.add( getRestriction( schema, "name", operator, arg ) );

        if ( schema.haveProperty( "shortName" ) )
        {
            disjunction.add( getRestriction( schema, "shortName", operator, arg ) );
        }
    }

    @Override
    public Restriction getRestriction( Schema schema, String path, String operator, Object arg ) throws QueryParserException
    {
        Property property = getProperty( schema, path );

        if ( property == null )
        {
            throw new QueryParserException( "Unknown path property: " + path );
        }

        switch ( operator )
        {
            case "eq":
            {
                return Restrictions.eq( path, QueryUtils.parseValue( property.getKlass(), arg ) );
            }
            case "!eq":
            {
                return Restrictions.ne( path, QueryUtils.parseValue( property.getKlass(), arg ) );
            }
            case "ne":
            {
                return Restrictions.ne( path, QueryUtils.parseValue( property.getKlass(), arg ) );
            }
            case "neq":
            {
                return Restrictions.ne( path, QueryUtils.parseValue( property.getKlass(), arg ) );
            }
            case "gt":
            {
                return Restrictions.gt( path, QueryUtils.parseValue( property.getKlass(), arg ) );
            }
            case "lt":
            {
                return Restrictions.lt( path, QueryUtils.parseValue( property.getKlass(), arg ) );
            }
            case "gte":
            {
                return Restrictions.ge( path, QueryUtils.parseValue( property.getKlass(), arg ) );
            }
            case "ge":
            {
                return Restrictions.ge( path, QueryUtils.parseValue( property.getKlass(), arg ) );
            }
            case "lte":
            {
                return Restrictions.le( path, QueryUtils.parseValue( property.getKlass(), arg ) );
            }
            case "le":
            {
                return Restrictions.le( path, QueryUtils.parseValue( property.getKlass(), arg ) );
            }
            case "like":
            {
                return Restrictions.like( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.ANYWHERE );
            }
            case "!like":
            {
                return Restrictions.notLike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.ANYWHERE );
            }
            case "$like":
            {
                return Restrictions.like( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.START );
            }
            case "!$like":
            {
                return Restrictions.notLike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.START );
            }
            case "like$":
            {
                return Restrictions.like( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.END );
            }
            case "!like$":
            {
                return Restrictions.notLike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.END );
            }
            case "ilike":
            {
                return Restrictions.ilike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.ANYWHERE );
            }
            case "!ilike":
            {
                return Restrictions.notIlike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.ANYWHERE );
            }
            case "startsWith":
            case "$ilike":
            {
                return Restrictions.ilike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.START );
            }
            case "!$ilike":
            {
                return Restrictions.notIlike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.START );
            }
            case "token":
            {
                return Restrictions.token( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.START );
            }
            case "!token":
            {
                return Restrictions.notToken( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.START );
            }
            case "endsWith":
            case "ilike$":
            {
                return Restrictions.ilike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.END );
            }
            case "!ilike$":
            {
                return Restrictions.notIlike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.END );
            }
            case "in":
            {
                Collection<?> values = null;

                if ( property.isCollection() )
                {
                    values = QueryUtils.parseValue( Collection.class, property.getItemKlass(), arg );
                }
                else
                {
                    values = QueryUtils.parseValue( Collection.class, property.getKlass(), arg );
                }

                if ( values == null || values.isEmpty() )
                {
                    throw new QueryParserException( "Invalid argument `" + arg + "` for in operator." );
                }

                return Restrictions.in( path, values );
            }
            case "!in":
            {
                Collection<?> values = null;

                if ( property.isCollection() )
                {
                    values = QueryUtils.parseValue( Collection.class, property.getItemKlass(), arg );
                }
                else
                {
                    values = QueryUtils.parseValue( Collection.class, property.getKlass(), arg );
                }

                if ( values == null || values.isEmpty() )
                {
                    throw new QueryParserException( "Invalid argument `" + arg + "` for in operator." );
                }

                return Restrictions.notIn( path, values );
            }
            case "null":
            {
                return Restrictions.isNull( path );
            }
            case "!null":
            {
                return Restrictions.isNotNull( path );
            }
            case "empty":
            {
                return Restrictions.isEmpty( path );
            }
            default:
            {
                throw new QueryParserException( "`" + operator + "` is not a valid operator." );
            }
        }
    }

    @Override
    public Property getProperty( Schema schema, String path ) throws QueryParserException
    {
        String[] paths = path.split( "\\." );
        Schema currentSchema = schema;
        Property currentProperty = null;

        for ( int i = 0; i < paths.length; i++ )
        {
            if ( !currentSchema.haveProperty( paths[i] ) )
            {
                return null;
            }

            currentProperty = currentSchema.getProperty( paths[i] );

            if ( currentProperty == null )
            {
                throw new QueryParserException( "Unknown path property: " + paths[i] + " (" + path + ")" );
            }

            if ( (currentProperty.isSimple() && !currentProperty.isCollection()) && i != (paths.length - 1) )
            {
                throw new QueryParserException( "Simple type was found before finished parsing path expression, please check your path string." );
            }

            if ( currentProperty.isCollection() )
            {
                currentSchema = schemaService.getDynamicSchema( currentProperty.getItemKlass() );
            }
            else
            {
                currentSchema = schemaService.getDynamicSchema( currentProperty.getKlass() );
            }
        }

        return currentProperty;
    }
}
