package org.hisp.dhis.util;

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

import com.opensymphony.xwork2.ActionContext;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class SessionUtils
{
    public static final String KEY_PREVIEW_TYPE = "previewType";
    public static final String KEY_PREVIEW_STATUS = "previewStatus";    
    public static final String KEY_CURRENT_YEAR = "currentYear";
    public static final String KEY_REPORT_TABLE_GRID = "lastReportTableGrid";
    public static final String KEY_REPORT_TABLE_PARAMS = "lastReportTableParams";
    public static final String KEY_DATASET_REPORT_GRID = "lastDataSetReportGrid";
    public static final String KEY_DATABROWSERGRID = "dataBrowserGridResults";
    public static final String KEY_SQLVIEW_GRID = "sqlViewGrid";
    
    public static Object getSessionVar( String name )
    {
        return ActionContext.getContext().getSession().get( name );
    }

    public static Object getSessionVar( String name, Object defaultValue )
    {
        Object object = ActionContext.getContext().getSession().get( name );
        
        return object != null ? object : defaultValue; 
    }

    public static void setSessionVar( String name, Object value )
    {
        ActionContext.getContext().getSession().put( name, value );
    }

    public static boolean containsSessionVar( String name )
    {
        return ActionContext.getContext().getSession().containsKey( name );
    }

    public static void removeSessionVar( String name )
    {
        ActionContext.getContext().getSession().remove( name );
    }
}
