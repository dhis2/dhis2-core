package org.hisp.dhis.hibernate;

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

import org.hibernate.Hibernate;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.pojo.javassist.SerializableProxy;

import java.util.LinkedHashSet;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class HibernateUtils
{
    public static boolean isProxy( Object object )
    {
        return object != null && ((object instanceof HibernateProxy) || (object instanceof PersistentCollection));
    }

    /**
     * If object is proxy, get unwrapped non-proxy object.
     *
     * @param proxy Object to check and unwrap
     * @return Unwrapped object if proxyied, if not just returns same object
     */
    @SuppressWarnings( "unchecked" )
    public static <T> T unwrap( T proxy )
    {
        if ( !isProxy( proxy ) )
        {
            return proxy;
        }

        Hibernate.initialize( proxy );

        if ( HibernateProxy.class.isInstance( proxy ) )
        {
            Object result = ((HibernateProxy) proxy).writeReplace();

            if ( !SerializableProxy.class.isInstance( result ) )
            {
                return (T) result;
            }
        }

        if ( PersistentCollection.class.isInstance( proxy ) )
        {
            PersistentCollection persistentCollection = (PersistentCollection) proxy;

            if ( PersistentSet.class.isInstance( persistentCollection ) )
            {
                Map<?, ?> map = (Map<?, ?>) persistentCollection.getStoredSnapshot();
                return (T) new LinkedHashSet<>( map.keySet() );
            }

            return (T) persistentCollection.getStoredSnapshot();
        }

        return proxy;
    }
}
