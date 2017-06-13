package org.hisp.dhis.system.util;

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

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class providing stopwatch-like functionality.
 * 
 * @author Lars Helge Overland
 */
public class Clock
    extends StopWatch
{
    private static final String SEPARATOR = ": ";
    
    private static final Log defaultLog = LogFactory.getLog( Clock.class );
    
    private Log log;

    /**
     * Create a new instance.
     */
    public Clock()
    {
        super();
    }

    /**
     * Create a new instance with a given logger.
     * @param log the logger.
     */
    public Clock( Log log )
    {
        super();
        this.log = log;
    }

    /**
     * Start the clock.
     * @return the Clock.
     */
    public Clock startClock()
    {
        this.start();
        
        return this;
    }

    /**
     * Yields the elapsed time since the Clock was started as an HMS String.
     * @return the elapsed time.
     */
    public String time()
    {
        super.split();
        
        return DurationFormatUtils.formatDurationHMS( super.getSplitTime() );
    }

    /**
     * Timestamps the given message using the elapsed time of this Clock and
     * logs it using the logger.
     * @param message the message to log.
     * @return this Clock.
     */
    public Clock logTime( String message )
    {
        super.split();
        
        String time = DurationFormatUtils.formatDurationHMS( super.getSplitTime() ); 
        
        String msg = message + SEPARATOR + time;
        
        if ( log != null )
        {
            log.info( msg );
        }
        else
        {
            defaultLog.info( msg );
        }
        
        return this;
    }
}
