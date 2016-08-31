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
 * Convenience class for creating startup routines. Contains a setter for the
 * runlevel property which should be used in bean mappings.
 * 
 * @author <a href="mailto:torgeilo@gmail.com">Torgeir Lorange Ostby</a>
 */
public abstract class AbstractStartupRoutine
    implements StartupRoutine
{
    private String name = this.getClass().getSimpleName();
    
    public void setName( String name )
    {
        this.name = name;
    }

    private int runlevel = 0;

    public void setRunlevel( int runlevel )
    {
        this.runlevel = runlevel;
    }
    
    private boolean skipInTests = false;

    public void setSkipInTests( boolean skipInTests )
    {
        this.skipInTests = skipInTests;
    }
    
    // -------------------------------------------------------------------------
    // StartupRoutine implementation
    // -------------------------------------------------------------------------

    @Override
    public int getRunlevel()
    {
        return runlevel;
    }
    
    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean skipInTests()
    {
        return skipInTests;
    }
}
