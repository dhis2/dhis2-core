package org.hisp.dhis.system.deletion;

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

import javassist.util.proxy.ProxyObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * TODO: Add support for failed allow tests on "transitive" deletion handlers which
 * are called as part of delete methods.
 * 
 * @author Lars Helge Overland
 */
public class DefaultDeletionManager
    implements DeletionManager
{
    private static final Log log = LogFactory.getLog( DefaultDeletionManager.class );

    private static final String DELETE_METHOD_PREFIX = "delete";
    private static final String ALLOW_METHOD_PREFIX = "allowDelete";

    /**
     * Deletion handlers registered in context are subscribed to deletion 
     * notifications through auto-wiring.
     */
    @Autowired(required = false)
    private List<DeletionHandler> deletionHandlers;

    // -------------------------------------------------------------------------
    // DeletionManager implementation
    // -------------------------------------------------------------------------

    @Override
    public void execute( Object object )
    {
        if ( deletionHandlers == null || deletionHandlers.isEmpty() )
        {
            log.info( "No deletion handlers registered, aborting deletion handling" );
            return;
        }
        
        log.debug( "Deletion handlers detected: " + deletionHandlers.size() );
        
        Class<?> clazz = getClazz( object );

        String className = clazz.getSimpleName();

        // ---------------------------------------------------------------------
        // Verify that object is allowed to be deleted
        // ---------------------------------------------------------------------

        String allowMethodName = ALLOW_METHOD_PREFIX + className;

        String currentHandler = null;

        try
        {
            Method allowMethod = DeletionHandler.class.getMethod( allowMethodName, new Class[]{ clazz } );

            for ( DeletionHandler handler : deletionHandlers )
            {
                currentHandler = handler.getClass().getSimpleName();

                log.debug( "Check if allowed using " + currentHandler + " for class " + className );

                Object allow = allowMethod.invoke( handler, object );

                if ( allow != null )
                {
                    String hint = String.valueOf( allow );

                    String message = "Could not delete due to association with another object: " +
                        handler.getClassName() + ( hint.isEmpty() ? hint : ( " (" + hint + ")" ) );

                    log.info( "Delete was not allowed by " + currentHandler + ": " + message );
                    
                    throw new DeleteNotAllowedException( DeleteNotAllowedException.ERROR_ASSOCIATED_BY_OTHER_OBJECTS, message );
                }
            }
        }
        catch ( NoSuchMethodException e )
        {
            log.error( "Method '" + allowMethodName + "' does not exist on class '" + clazz + "'", e );
            return;
        }
        catch ( IllegalAccessException ex )
        {
            log.error( "Method '" + allowMethodName + "' can not be invoked on DeletionHandler '" + currentHandler + "'", ex );
            return;
        }
        catch ( InvocationTargetException ex )
        {
            log.error( "Method '" + allowMethodName + "' threw exception on DeletionHandler '" + currentHandler + "'", ex );
            return;
        }

        // ---------------------------------------------------------------------
        // Delete associated objects
        // ---------------------------------------------------------------------

        String deleteMethodName = DELETE_METHOD_PREFIX + className;

        try
        {
            Method deleteMethod = DeletionHandler.class.getMethod( deleteMethodName, new Class[]{ clazz } );

            for ( DeletionHandler handler : deletionHandlers )
            {
                currentHandler = handler.getClass().getSimpleName();

                log.debug( "Deleting object using " + currentHandler + " for class " + className );

                deleteMethod.invoke( handler, object );
            }
        }
        catch ( Exception ex )
        {
            log.error( "Failed to invoke method " + deleteMethodName + " on DeletionHandler '" + currentHandler + "'", ex );
            return;
        }

        log.info( "Deleted objects associated with object of type " + className );
    }

    private Class<?> getClazz( Object object )
    {
        Class<?> clazz = object.getClass();

        while ( ProxyObject.class.isAssignableFrom( clazz ) )
        {
            clazz = clazz.getSuperclass();
        }

        return clazz;
    }
}
