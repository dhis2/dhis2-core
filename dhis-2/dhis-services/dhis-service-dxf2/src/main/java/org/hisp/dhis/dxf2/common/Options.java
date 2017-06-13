package org.hisp.dhis.dxf2.common;

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

import org.hisp.dhis.system.util.DateUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class Options
{
    //--------------------------------------------------------------------------
    // Internal State
    //--------------------------------------------------------------------------

    protected Map<String, String> options = new HashMap<>();

    protected boolean assumeTrue;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    public Options( Map<String, String> options )
    {
        this.options = options;
        this.assumeTrue = options.get( "assumeTrue" ) == null || options.get( "assumeTrue" ).equalsIgnoreCase( "true" );
    }

    public Options()
    {
    }

    //--------------------------------------------------------------------------
    // Object helpers
    //--------------------------------------------------------------------------

    /**
     * Indicates whether the given object type is enabled. Takes the assumeTrue
     * parameter into account.
     */
    public boolean isEnabled( String type )
    {
        String enabled = options.get( type );

        return stringIsTrue( enabled ) || (enabled == null && assumeTrue);
    }

    /**
     * Indicates whether the given object type is disabled. Takes the assumeTrue
     * parameter into account.
     */
    public boolean isDisabled( String type )
    {
        return !isEnabled( type );
    }

    //--------------------------------------------------------------------------
    // Options helpers
    //--------------------------------------------------------------------------

    public Date getDate( String key )
    {
        return DateUtils.parseDate( options.get( key ) );
    }

    /**
     * Indicates whether the options contains the given parameter key.
     */
    public boolean contains( String key )
    {
        return options.containsKey( key );
    }

    /**
     * Indicates whether the options contains a non-null option value for the given
     * parameter key.
     */
    public boolean containsValue( String key )
    {
        return options.get( key ) != null;
    }

    /**
     * Returns the option value for the given parameter key.
     */
    public String get( String key )
    {
        return options.get( key );
    }

    /**
     * Returns the option value for the given parameter key as in Integer.
     */
    public Integer getInt( String key )
    {
        return options.containsKey( key ) ? Integer.parseInt( options.get( key ) ) : null;
    }

    /**
     * Indicates whether the option value for the parameter key is true.
     */
    public boolean isTrue( String key )
    {
        return options.containsKey( key ) && Boolean.parseBoolean( options.get( key ) );
    }

    //--------------------------------------------------------------------------
    // Getters and Setters
    //--------------------------------------------------------------------------

    public Map<String, String> getOptions()
    {
        return options;
    }

    public void setOptions( Map<String, String> options )
    {
        this.options = options;
    }

    public boolean isAssumeTrue()
    {
        return assumeTrue;
    }

    public void setAssumeTrue( boolean assumeTrue )
    {
        this.assumeTrue = assumeTrue;
    }

    //--------------------------------------------------------------------------
    // Getters for standard options
    //--------------------------------------------------------------------------

    public Date getLastUpdated()
    {
        return getDate( "lastUpdated" );
    }

    //--------------------------------------------------------------------------
    // Adding options
    //--------------------------------------------------------------------------

    public void addOption( String option, String value )
    {
        options.put( option, value );
    }

    public void addOptions( Map<String, String> newOptions )
    {
        options.putAll( options );
    }

    //--------------------------------------------------------------------------
    // Static helpers
    //--------------------------------------------------------------------------

    protected static String stringAsString( String str, String defaultValue )
    {
        if ( str == null )
        {
            return defaultValue;
        }

        return str;
    }

    protected static boolean stringAsBoolean( String str, boolean defaultValue )
    {
        return str != null ? Boolean.parseBoolean( str ) : defaultValue;
    }

    protected static boolean stringIsTrue( String str )
    {
        return stringAsBoolean( str, false );
    }

    protected static int stringAsInt( String str )
    {
        return stringAsInt( str, 0 );
    }

    protected static int stringAsInt( String str, int defaultValue )
    {
        if ( str != null )
        {
            try
            {
                return Integer.parseInt( str );
            }
            catch ( NumberFormatException ignored )
            {
            }
        }

        return defaultValue;
    }
}
