package org.hisp.dhis.common;

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

import javassist.util.proxy.ProxyFactory;
import org.apache.commons.logging.Log;

public class AuditLogUtil
{
    public static final String ACTION_CREATE = "create";
    public static final String ACTION_READ = "read";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_DELETE = "delete";

    public static final String ACTION_CREATE_DENIED = "create denied";
    public static final String ACTION_READ_DENIED = "read denied";
    public static final String ACTION_UPDATE_DENIED = "update denied";
    public static final String ACTION_DELETE_DENIED = "delete denied";

    public static void infoWrapper( Log log, String username, Object object, String action )
    {
        if ( log.isInfoEnabled() )
        {
            if ( username != null && object != null && IdentifiableObject.class.isInstance( object ) )
            {
                IdentifiableObject idObject = (IdentifiableObject) object;
                StringBuilder builder = new StringBuilder();

                builder.append( "'" ).append( username ).append( "' " ).append( action );

                if ( !ProxyFactory.isProxyClass( object.getClass() ) )
                {
                    builder.append( " " ).append( object.getClass().getName() );
                }
                else
                {
                    builder.append( " " ).append( object.getClass().getSuperclass().getName() );
                }

                if ( idObject.getName() != null && !idObject.getName().isEmpty() )
                {
                    builder.append( ", name: " ).append( idObject.getName() );
                }

                if ( idObject.getUid() != null && !idObject.getUid().isEmpty() )
                {
                    builder.append( ", uid: " ).append( idObject.getUid() );
                }

                log.info( builder.toString() );
            }
        }
    }
}
