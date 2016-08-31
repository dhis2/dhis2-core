package org.hisp.dhis.interceptor;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;
import com.opensymphony.xwork2.interceptor.PreResultListener;

/**
 * The intention of this class is to stop execution of the pre result listener
 * when an exception is thrown in the action invocation.
 * 
 * @author Torgeir Lorange Ostby
 * @version $Id: AbstractPreResultListener.java 2869 2007-02-20 14:26:09Z andegje $
 */
public abstract class AbstractPreResultListener
    implements Interceptor, PreResultListener
{
    private static final Log LOG = LogFactory.getLog( AbstractPreResultListener.class );

    private boolean executePreResultListener;

    // -------------------------------------------------------------------------
    // Interceptor implementation
    // -------------------------------------------------------------------------

    @Override
    public final void destroy()
    {
    }

    @Override
    public final void init()
    {
    }

    @Override
    public final String intercept( ActionInvocation actionInvocation ) throws Exception
    {
        actionInvocation.addPreResultListener( this );

        executePreResultListener = true;

        try
        {
            return actionInvocation.invoke();
        }
        catch ( Exception e )
        {
            executePreResultListener = false;
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // PreResultListener implementation
    // -------------------------------------------------------------------------

    @Override
    public final void beforeResult( ActionInvocation actionInvocation, String result )
    {
        if ( executePreResultListener )
        {
            try
            {
                executeBeforeResult( actionInvocation, result );
            }
            catch ( Exception e )
            {
                LOG.error( "Error while executing PreResultListener", e );
            }
        }
    }

    // -------------------------------------------------------------------------
    // Abstract method to be implemented by subclasses
    // -------------------------------------------------------------------------

    public abstract void executeBeforeResult( ActionInvocation actionInvocation, String result ) throws Exception;
}
