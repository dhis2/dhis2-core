package org.hisp.dhis.hibernate;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.google.common.base.Preconditions;
import org.hibernate.Hibernate;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.HibernateProxyHelper;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class HibernateProxyUtils
{
    public static boolean isProxy( Object object )
    {
        return ((object instanceof HibernateProxy) || (object instanceof PersistentCollection));
    }

    public static Class getRealClass( Object o )
    {
        if ( isProxy( o ) )
        {
            Class classWithoutInitializingProxy = HibernateProxyHelper.getClassWithoutInitializingProxy( o );
            return classWithoutInitializingProxy;
        }
        else
        {
            Class<?> aClass = o.getClass();
            return aClass;
        }
    }

    public static <T> T unwrap( T proxy )
    {
        return (T) Hibernate.unproxy( proxy );
//
//        if ( !isProxy( proxy ) )
//        {
//            return proxy;
//        }
//
//        Hibernate.initialize( proxy );
//
//        if ( HibernateProxy.class.isInstance( proxy ) )
//        {
//            Object result = ((HibernateProxy) proxy).writeReplace();
//
//            if ( !SerializableProxy.class.isInstance( result ) )
//            {
//                return (T) result;
//            }
//        }
//
//        if ( PersistentCollection.class.isInstance( proxy ) )
//        {
//            PersistentCollection persistentCollection = (PersistentCollection) proxy;
//
//            if ( PersistentSet.class.isInstance( persistentCollection ) )
//            {
//                Map<?, ?> map = (Map<?, ?>) persistentCollection.getStoredSnapshot();
//                return (T) new LinkedHashSet<>( map.keySet() );
//            }
//
//            return (T) persistentCollection.getStoredSnapshot();
//        }
//
//        return proxy;
    }

    /**
     * Eager fetch all its collections
     *
     * @param proxy Object to check and unwrap
     * @return fully initialized object
     */
    public static <T> T initializeProxy( T proxy )
    {
        Preconditions.checkNotNull( proxy, "Proxy can not be null!" );

        if ( !Hibernate.isInitialized( proxy ) )
        {
            Hibernate.initialize( proxy );
        }

        Field[] fields = proxy.getClass().getDeclaredFields();

        Arrays.stream( fields )
            .filter( f -> Collection.class.isAssignableFrom( f.getType() ) )
            .forEach( f ->
            {
                try
                {
                    PropertyDescriptor pd = new PropertyDescriptor( f.getName(), proxy.getClass() );

                    Object persistentObject = pd.getReadMethod().invoke( proxy );

                    if ( persistentObject != null && PersistentCollection.class.isAssignableFrom( persistentObject.getClass() ) )
                    {
                        Hibernate.initialize( persistentObject );
                    }
                }
                catch ( IllegalAccessException | IntrospectionException | InvocationTargetException e )
                {
                    getStackTrace( e );
                }
            } );

        return proxy;
    }

    public static String getStackTrace( Throwable t )
    {
        StringWriter sw = new StringWriter();

        if ( t != null )
        {
            PrintWriter pw = new PrintWriter( sw, true );
            t.printStackTrace( pw );
            pw.flush();
            sw.flush();
        }

        return sw.toString();
    }
}
