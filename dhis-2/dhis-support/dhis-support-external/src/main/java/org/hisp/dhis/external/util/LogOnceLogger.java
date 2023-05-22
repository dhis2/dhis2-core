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
package org.hisp.dhis.external.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * This class maintains a static list of logged statements and check if the
 * statement was already logged before logging it again. It is useful to remove
 * duplicated log entries from code that runs multiple times (e.g. init
 * routines)
 *
 * @author Luciano Fiandesio
 */
public abstract class LogOnceLogger
{
    private static Set<String> logged = ConcurrentHashMap.newKeySet();

    /**
     * Creates a log entry with a specific Log level. The entry will be logged
     * only once
     *
     * @param log The SLF4J log to use
     * @param level The SLF4J log level
     * @param logString The string to log
     */
    protected void log( Logger log, Level level, String logString )
    {
        if ( !logged.contains( logString ) )
        {
            switch ( level )
            {
                case ERROR:
                    log.error( logString );
                    break;
                case WARN:
                    log.warn( logString );
                    break;
                case INFO:
                    log.info( logString );
                    break;
                case DEBUG:
                    log.debug( logString );
                    break;
                case TRACE:
                    log.trace( logString );
                    break;
            }
            logged.add( logString );
        }
    }

    /**
     * Creates a log entry with WARN level. The entry will be logged only once
     *
     * @param log The SLF4J log to use
     * @param logString The string to log
     */
    protected void warn( Logger log, String logString, Exception exception )
    {
        if ( !logged.contains( logString ) )
        {
            log.warn( logString, exception );
            logged.add( logString );
        }
    }
}
