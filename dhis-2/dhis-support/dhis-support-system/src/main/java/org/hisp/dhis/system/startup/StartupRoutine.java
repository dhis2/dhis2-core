package org.hisp.dhis.system.startup;

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

/**
 * Defines a startup routine which should be executed when the system is
 * started. The runlevel can be used to group startup routines that are
 * dependent on other startup routines, without too much detail and knowledge.
 * 
 * @author <a href="mailto:torgeilo@gmail.com">Torgeir Lorange Ostby</a>
 * @version $Id: StartupRoutine.java 5781 2008-10-01 12:12:48Z larshelg $
 */
public interface StartupRoutine
{
    /**
     * Executes the startup routine. It should fail hard if it is required to be
     * executed successfully, or if any other unexpected errors occur.
     * 
     * @throws Exception if anything goes wrong.
     */
    void execute()
        throws Exception;

    /**
     * Returns the name of the startup routine.
     * 
     * @return the name.
     */
    String getName();
    
    /**
     * StartupRoutines with lower runlevels will be executed before
     * StartupRoutines with higher runlevel.
     * 
     * @return the runlevel for the StartupRoutine.
     */
    int getRunlevel();
    
    /**
     * Returns whether this StartupRoutine is to be skipped in tests or not.
     * @return true if this StartupRoutine is skipped in tests, false otherwise.
     */
    boolean skipInTests();
}
