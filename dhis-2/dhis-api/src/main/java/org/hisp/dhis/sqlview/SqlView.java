package org.hisp.dhis.sqlview;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.common.cache.Cacheable;
import org.hisp.dhis.schema.annotation.PropertyRange;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Dang Duy Hieu
 */
@JacksonXmlRootElement( localName = "sqlView", namespace = DxfNamespaces.DXF_2_0 )
public class SqlView
    extends BaseIdentifiableObject
    implements Cacheable
{
    public static final String PREFIX_VIEWNAME = "_view";

    public static final Set<String> PROTECTED_TABLES = ImmutableSet.<String>builder().add(
        "users", "userinfo", "trackedentityattribute", "trackedentityattributevalue", "oauth_access_token", "oauth2client" ).build();

    public static final Set<String> ILLEGAL_KEYWORDS = ImmutableSet.<String>builder().add(
        "delete", "alter", "update", "create", "drop", "commit", "createdb",
        "createuser", "insert", "rename", "replace", "restore", "write" ).build();

    private static final String CRITERIA_SEP = ":";
    private static final String REGEX_SEP = "|";

    private static final String QUERY_VALUE_REGEX = "^[\\w\\s\\-]*$";

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    private String description;

    private String sqlQuery;

    private SqlViewType type;

    private CacheStrategy cacheStrategy = CacheStrategy.RESPECT_SYSTEM_SETTING;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public SqlView()
    {
    }

    public SqlView( String name, String sqlQuery, SqlViewType type )
    {
        this.name = name;
        this.sqlQuery = sqlQuery;
        this.type = type;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public String getViewName()
    {
        final Pattern p = Pattern.compile( "\\W" );

        String input = name;

        String[] items = p.split( input.trim().replaceAll( "_", "" ) );

        input = "";

        for ( String s : items )
        {
            input += s.isEmpty() ? "" : ("_" + s);
        }

        return PREFIX_VIEWNAME + input.toLowerCase();
    }

    public static Map<String, String> getCriteria( Set<String> params )
    {
        Map<String, String> map = new HashMap<>();

        if ( params != null )
        {
            for ( String param : params )
            {
                if ( param != null && param.split( CRITERIA_SEP ).length == 2 )
                {
                    String[] criteria = param.split( CRITERIA_SEP );
                    String filter = criteria[0];
                    String value = criteria[1];

                    map.put( filter, value );
                }
            }
        }

        return map;
    }

    public static Set<String> getInvalidQueryParams( Set<String> params )
    {
        Set<String> invalid = new HashSet<>();

        for ( String param : params )
        {
            if ( !isValidQueryParam( param ) )
            {
                invalid.add( param );
            }
        }

        return invalid;
    }

    /**
     * Indicates whether the given query parameter is valid.
     */
    public static final boolean isValidQueryParam( String param )
    {
        return StringUtils.isAlphanumeric( param );
    }

    public static Set<String> getInvalidQueryValues( Collection<String> values )
    {
        Set<String> invalid = new HashSet<>();

        for ( String value : values )
        {
            if ( !isValidQueryValue( value ) )
            {
                invalid.add( value );
            }
        }

        return invalid;
    }

    /**
     * Indicates whether the given query value is valid.
     */
    public static final boolean isValidQueryValue( String value )
    {
        return value != null && value.matches( QUERY_VALUE_REGEX );
    }

    public static String getProtectedTablesRegex()
    {
        StringBuffer regex = new StringBuffer( "^.*?(\"|'|`|\\s|^)(" );

        for ( String table : PROTECTED_TABLES )
        {
            regex.append( table ).append( REGEX_SEP );
        }

        regex.delete( regex.length() - 1, regex.length() );

        return regex.append( ")(\"|'|`|\\s|$).*$" ).toString();
    }


    public static final String[] getIllegalKeyWords()
    {
        return ILLEGAL_KEYWORDS.toArray( new String[ILLEGAL_KEYWORDS.size()] );
    }


    /**
     * Indicates whether this SQL view is a query.
     */
    public boolean isQuery()
    {
        return SqlViewType.QUERY.equals( type );
    }

    /**
     * Indicates whether this SQL view is a materalized view.
     */
    public boolean isMaterializedView()
    {
        return SqlViewType.MATERIALIZED_VIEW.equals( type );
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getSqlQuery()
    {
        return sqlQuery;
    }

    public void setSqlQuery( String sqlQuery )
    {
        this.sqlQuery = sqlQuery;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public SqlViewType getType()
    {
        return type;
    }

    public void setType( SqlViewType type )
    {
        this.type = type;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Override
    public CacheStrategy getCacheStrategy()
    {
        return cacheStrategy;
    }

    public void setCacheStrategy( CacheStrategy cacheStrategy )
    {
        this.cacheStrategy = cacheStrategy;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            SqlView sqlView = (SqlView) other;

            if ( mergeMode.isReplace() )
            {
                description = sqlView.getDescription();
                sqlQuery = sqlView.getSqlQuery();
                type = sqlView.getType();
                cacheStrategy = sqlView.getCacheStrategy();
            }
            else if ( mergeMode.isMerge() )
            {
                description = sqlView.getDescription() == null ? description : sqlView.getDescription();
                sqlQuery = sqlView.getSqlQuery() == null ? sqlQuery : sqlView.getSqlQuery();
                type = sqlView.getType() == null ? type : sqlView.getType();
                cacheStrategy = sqlView.getCacheStrategy() == null ? cacheStrategy : sqlView.getCacheStrategy();
            }
        }
    }
}
