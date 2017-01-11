package org.hisp.dhis.dataadmin.action.sqlview;

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

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.sqlview.SqlViewService;
import org.hisp.dhis.sqlview.SqlViewType;

import com.opensymphony.xwork2.Action;

/**
 * @author Dang Duy Hieu
 */
public class ValidateAddUpdateSqlViewAction
    implements Action
{
    private static final String ADD = "add";
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SqlViewService sqlViewService;

    public void setSqlViewService( SqlViewService sqlViewService )
    {
        this.sqlViewService = sqlViewService;
    }

    // -------------------------------------------------------------------------
    // I18n
    // -------------------------------------------------------------------------

    private I18n i18n;

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String mode;

    public void setMode( String mode )
    {
        this.mode = mode;
    }

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private String sqlquery;

    public void setSqlquery( String sqlquery )
    {
        this.sqlquery = sqlquery;
    }

    private boolean query;

    public void setQuery( boolean query )
    {
        this.query = query;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private String message;

    public String getMessage()
    {
        return message;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        message = null;
        
        if ( name == null || name.trim().isEmpty() )
        {
            message = i18n.getString( "name_is_null" );

            return INPUT;
        }

        if ( mode.equals( ADD ) && sqlViewService.getSqlView( name ) != null )
        {
            message = i18n.getString( "name_in_use" );

            return INPUT;
        }

        if ( sqlquery == null || sqlquery.trim().isEmpty() )
        {
            message = i18n.getString( "sqlquery_is_empty" );

            return INPUT;
        }

        try
        {
            SqlView sqlView = new SqlView( name, sqlquery, SqlViewType.VIEW ); // Avoid variable check
            
            sqlViewService.validateSqlView( sqlView, null, null );
        }
        catch ( IllegalQueryException ex )
        {
            message = ex.getMessage();
            
            return INPUT;
        }

        if ( !query )
        {
            message = sqlViewService.testSqlGrammar( sqlquery );
        }
        
        if ( message != null )
        {
            return INPUT;
        }

        return SUCCESS;
    }
}
