package org.hisp.dhis.util;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import org.hisp.dhis.util.SessionUtils;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class InternalProcessUtil
{
    public static final String PROCESS_KEY_DATAMART = "CurrentRunningDataMartInternalProcess";
    public static final String PROCESS_KEY_IMPORT = "CurrentRunningImportInternalProcess";
    public static final String PROCESS_KEY_EXPORT = "CurrentRunningExportInternalProcess";
    public static final String PROCESS_KEY_REPORT = "CurrentRunningReportInternalProcess";
        
    public static String getCurrentRunningProcess( String processKey )
    {
        return String.valueOf( SessionUtils.getSessionVar( processKey ) );
    }

    public static void setCurrentRunningProcess(  String processKey , String id )
    {
        SessionUtils.setSessionVar( processKey, id );
    }
    
    public static boolean processIsRunning( String processKey )
    {
        return SessionUtils.containsSessionVar( processKey );
    }
    
    public static void removeCurrentRunningProcess( String processKey )
    {
        SessionUtils.removeSessionVar( processKey );
    }
}
