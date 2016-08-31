package org.hisp.dhis.query;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.translation.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultQueryParser implements QueryParser
{
    @Autowired
    private SchemaService schemaService;
    
    @Autowired
    private TranslationService translationService;

    @Override
    public Query parse( Class<?> klass, List<String> filters ) throws QueryParserException
    {
        Schema schema = schemaService.getDynamicSchema( klass );
        Query query = Query.from( schema );

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
                query.add( getRestriction( schema, split[0], split[1], filter.substring( index ) ) );
            }
            else
            {
                query.add( getRestriction( schema, split[0], split[1], null ) );
            }
        }

        return query;
    }

    private Restriction getRestriction( Schema schema, String path, String operator, Object arg ) throws QueryParserException
    {
        // optimize if not translated
        if ( !translationService.isTranslated( schema.getKlass() ) )
        {
            if ( path.startsWith( "displayName:" ) && schema.havePersistedProperty( "name" ) )
            {
                path = path.replace( "displayName:", "name:" );
            }
            else if ( path.startsWith( "displayShortName:" ) && schema.havePersistedProperty( "shortName" ) )
            {
                path = path.replace( "displayShortName:", "shortName:" );
            }
            else if ( path.startsWith( "displayDescription:" ) && schema.havePersistedProperty( "description" ) )
            {
                path = path.replace( "displayDescription:", "description:" );
            }
        }

        Property property = getProperty( schema, path );

        if ( property == null )
        {
            throw new QueryParserException( "Unknown path property: " + path );
        }

        switch ( operator )
        {
            case "eq":
            {
                return Restrictions.eq( path, QueryUtils.getValue( property.getKlass(), arg ) );
            }
            case "!eq":
            {
                return Restrictions.ne( path, QueryUtils.getValue( property.getKlass(), arg ) );
            }
            case "ne":
            {
                return Restrictions.ne( path, QueryUtils.getValue( property.getKlass(), arg ) );
            }
            case "neq":
            {
                return Restrictions.ne( path, QueryUtils.getValue( property.getKlass(), arg ) );
            }
            case "gt":
            {
                return Restrictions.gt( path, QueryUtils.getValue( property.getKlass(), arg ) );
            }
            case "lt":
            {
                return Restrictions.lt( path, QueryUtils.getValue( property.getKlass(), arg ) );
            }
            case "gte":
            {
                return Restrictions.ge( path, QueryUtils.getValue( property.getKlass(), arg ) );
            }
            case "ge":
            {
                return Restrictions.ge( path, QueryUtils.getValue( property.getKlass(), arg ) );
            }
            case "lte":
            {
                return Restrictions.le( path, QueryUtils.getValue( property.getKlass(), arg ) );
            }
            case "le":
            {
                return Restrictions.le( path, QueryUtils.getValue( property.getKlass(), arg ) );
            }
            case "like":
            {
                return Restrictions.like( path, String.valueOf( arg ), MatchMode.ANYWHERE );
            }
            case "!like":
            {
                return Restrictions.notLike( path, String.valueOf( arg ), MatchMode.ANYWHERE );
            }
            case "^like":
            {
                return Restrictions.like( path, String.valueOf( arg ), MatchMode.START );
            }
            case "!^like":
            {
                return Restrictions.notLike( path, String.valueOf( arg ), MatchMode.START );
            }
            case "$like":
            {
                return Restrictions.like( path, String.valueOf( arg ), MatchMode.END );
            }
            case "!$like":
            {
                return Restrictions.notLike( path, String.valueOf( arg ), MatchMode.END );
            }
            case "ilike":
            {
                return Restrictions.ilike( path, String.valueOf( arg ), MatchMode.ANYWHERE );
            }
            case "!ilike":
            {
                return Restrictions.notIlike( path, String.valueOf( arg ), MatchMode.ANYWHERE );
            }
            case "startsWith":
            case "^ilike":
            {
                return Restrictions.ilike( path, String.valueOf( arg ), MatchMode.START );
            }
            case "!^ilike":
            {
                return Restrictions.notIlike( path, String.valueOf( arg ), MatchMode.START );
            }
            case "endsWith":
            case "$ilike":
            {
                return Restrictions.ilike( path, String.valueOf( arg ), MatchMode.END );
            }
            case "!$ilike":
            {
                return Restrictions.notIlike( path, String.valueOf( arg ), MatchMode.END );
            }
            case "in":
            {
                return Restrictions.in( path, QueryUtils.getValue( Collection.class, property.getKlass(), arg ) );
            }
            case "!in":
            {
                return Restrictions.notIn( path, QueryUtils.getValue( Collection.class, property.getKlass(), arg ) );
            }
            case "null":
            {
                return Restrictions.isNull( path );
            }
            case "!null":
            {
                return Restrictions.isNotNull( path );
            }
        }

        return null;
    }

    private Property getProperty( Schema schema, String path ) throws QueryParserException
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

            if ( currentProperty.isSimple() && i != (paths.length - 1) )
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
