package org.hisp.dhis.minmax.hibernate;

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

import org.hibernate.Criteria;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementQuery;
import org.hisp.dhis.minmax.MinMaxDataElementStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.QueryUtils;
import org.hisp.dhis.query.planner.QueryPath;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Kristian Nordal
 */
public class HibernateMinMaxDataElementStore
    extends HibernateGenericStore<MinMaxDataElement>
    implements MinMaxDataElementStore
{
    // -------------------------------------------------------------------------
    // MinMaxDataElementStore Implementation
    // -------------------------------------------------------------------------

    @Override
    public MinMaxDataElement get( OrganisationUnit source, DataElement dataElement,
        DataElementCategoryOptionCombo optionCombo )
    {
        return (MinMaxDataElement) getCriteria( 
            Restrictions.eq( "source", source ),
            Restrictions.eq( "dataElement", dataElement ), 
            Restrictions.eq( "optionCombo", optionCombo ) ).uniqueResult();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<MinMaxDataElement> get( OrganisationUnit source, DataElement dataElement )
    {
        return getCriteria( 
            Restrictions.eq( "source", source ), 
            Restrictions.eq( "dataElement", dataElement ) ).list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<MinMaxDataElement> get( OrganisationUnit source, Collection<DataElement> dataElements )
    {
        if ( dataElements.size() == 0 )
        {
            return new ArrayList<>();
        }

        return getCriteria( 
            Restrictions.eq( "source", source ), 
            Restrictions.in( "dataElement", dataElements ) ).list();
    }

    public List<MinMaxDataElement> query(  MinMaxDataElementQuery query )
    {
        Criteria criteria = getSession().createCriteria( MinMaxDataElement.class );
        criteria = parseFilter( criteria, query.getFilters() );

        if ( !query.isSkipPaging() )
        {
            Pager pager = query.getPager();
            criteria.setFirstResult( pager.getOffset() );
            criteria.setMaxResults( pager.getPageSize() );
        }

        return criteria.list();
    }

    @Override
    public int count( MinMaxDataElementQuery query )
    {
        Criteria criteria = getSession().createCriteria( MinMaxDataElement.class );
        criteria = parseFilter( criteria, query.getFilters() );

        return criteria.list().size();
    }
    
    @Override
    public void delete( OrganisationUnit organisationUnit )
    {
        String hql = "delete from MinMaxDataElement m where m.source = :source";
        
        getQuery( hql ).setEntity( "source", organisationUnit ).executeUpdate();
    }
    
    @Override
    public void delete( DataElement dataElement )
    {
        String hql = "delete from MinMaxDataElement m where m.dataElement = :dataElement";
        
        getQuery( hql ).setEntity( "dataElement", dataElement ).executeUpdate();
    }
    
    @Override
    public void delete( DataElementCategoryOptionCombo optionCombo )
    {
        String hql = "delete from MinMaxDataElement m where m.optionCombo = :optionCombo";
        
        getQuery( hql ).setEntity( "optionCombo", optionCombo ).executeUpdate();
    }
    
    @Override
    public void delete( Collection<DataElement> dataElements, Collection<OrganisationUnit> organisationUnits )
    {
        String hql = "delete from MinMaxDataElement m where m.dataElement in (:dataElements) and m.source in (:organisationUnits)";
        
        getQuery( hql ).
            setParameterList( "dataElements", dataElements ).
            setParameterList( "organisationUnits", organisationUnits ).executeUpdate();
    }

    private Criteria parseFilter( Criteria criteria, List<String> filters )
    {
        Conjunction conjunction = Restrictions.conjunction();
        if ( !filters.isEmpty() )
        {
            for ( String filter : filters )
            {
                String[] split = filter.split( ":" );

                if ( split.length != 3 )
                {
                    throw new QueryParserException( "Invalid filter: " + filter );
                }

                criteria = createAlias( criteria, split[0] );
                Criterion restriction = getRestriction( split[0], split[1], split[2] );

                if ( restriction != null )
                {
                    conjunction.add( restriction );
                }
            }
        }
        criteria.add( conjunction );

        return criteria;
    }

    private Criterion getRestriction( String path, String operator, String value )
    {
        Schema schema = schemaService.getDynamicSchema( MinMaxDataElement.class );
        Property property = getProperty( schema, path );
        path = getQueryPath( schema, path ).getPath();

        switch ( operator )
        {
            case "in" : return Restrictions.in( path, QueryUtils.parseValue( Collection.class, property.getKlass(), value ) );
            case "eq" : return Restrictions.eq( path, QueryUtils.parseValue( property.getKlass(), value ) );
            default: return null;
        }
    }


    private Criteria createAlias( Criteria criteria, String property )
    {
        if( property.contains( "dataElement" ) )
        {
             criteria.createAlias( "dataElement", "dataElement" );
        }
        else if ( property.contains( "source" ) )
        {
            criteria.createAlias( "source", "source" );
        }
        else if ( property.contains( "optionCombo" ) )
        {
            criteria.createAlias( "optionCombo", "optionCombo" );
        }
        return criteria;
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

    private QueryPath getQueryPath( Schema schema, String path )
    {
        Schema curSchema = schema;
        Property curProperty = null;
        boolean persisted = true;
        List<String> alias = new ArrayList<>();
        String[] pathComponents = path.split( "\\." );

        if ( pathComponents.length == 0 )
        {
            return null;
        }

        for ( int idx = 0; idx < pathComponents.length; idx++ )
        {
            String name = pathComponents[idx];
            curProperty = curSchema.getProperty( name );

            if ( curProperty == null )
            {
                throw new RuntimeException( "Invalid path property: " + name );
            }

            if ( !curProperty.isPersisted() )
            {
                persisted = false;
            }

            if ( (!curProperty.isSimple() && idx == pathComponents.length - 1) )
            {
                return new QueryPath( curProperty, persisted, alias.toArray( new String[]{} ) );
            }

            if ( curProperty.isCollection() )
            {
                curSchema = schemaService.getDynamicSchema( curProperty.getItemKlass() );
                alias.add( curProperty.getFieldName() );
            }
            else if ( !curProperty.isSimple() )
            {
                curSchema = schemaService.getDynamicSchema( curProperty.getKlass() );
                alias.add( curProperty.getFieldName() );
            }
            else
            {
                return new QueryPath( curProperty, persisted, alias.toArray( new String[]{} ) );
            }
        }

        return new QueryPath( curProperty, persisted, alias.toArray( new String[]{} ) );
    }

}
