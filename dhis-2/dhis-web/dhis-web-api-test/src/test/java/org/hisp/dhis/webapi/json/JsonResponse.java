/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.json;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.util.ObjectUtils;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.PathNotFoundException;

import net.minidev.json.JSONArray;

/**
 * Implements the {@link JsonValue} read-only access abstraction for JSON
 * responses.
 *
 * The way this works is that internally when navigating the JSON object the
 * {@link #expression} is extended. The returned value is always either a
 * {@link JsonResponse} or a {@link Proxy} calling all default methods on the
 * declaring interface and all implemented by {@link JsonResponse} on a
 * "wrapped" {@link JsonResponse} instance.
 *
 * When values are accessed the {@link #expression} path is extracted from the
 * {@link #content} and eventually converted and checked against expectations.
 * Exceptions are eventually converted to provide a consistent behaviour.
 *
 * It is crucial to understand that the complete JSON object model is purely a
 * typed convenience layer expressing what could or is expected to exist.
 * Whether or not something actually exist is first evaluated when leaf values
 * are accessed or existence is explicitly checked using {@link #exists()}.
 *
 * This also means specific {@link JsonObject}s are modelled by extending the
 * interface and implementing {@code default} methods. No other implementation
 * class is ever written except {@link JsonResponse}.
 *
 * @author Jan Bernitt
 */
public final class JsonResponse implements JsonObject, JsonArray, JsonString, JsonNumber, JsonBoolean, Serializable
{

    private final String content;

    private final String expression;

    /**
     * A simple "cache" so that multiple asserts working on the same path do not
     * extract the actual value multiple times.
     */
    private transient Object valueCache;

    public JsonResponse( String content )
    {
        this( content, "$" );
    }

    private JsonResponse( String content, String expression )
    {
        this.content = content;
        this.expression = expression;
    }

    private JsonPath path()
    {
        return JsonPath.compile( expression );
    }

    private Object value( Function<JsonPathException, Object> orElse )
    {
        if ( valueCache != null )
        {
            return valueCache;
        }
        try
        {
            Object value = path().read( content );
            valueCache = value;
            return value;
        }
        catch ( PathNotFoundException ex )
        {
            Object res = orElse.apply( ex );
            if ( res instanceof RuntimeException )
            {
                throw (RuntimeException) res;
            }
            return res;
        }
    }

    @Override
    public <T extends JsonValue> T get( int index, Class<T> as )
    {
        return asType( as, new JsonResponse( content, expression + "[" + index + "]" ) );
    }

    @Override
    public <T extends JsonValue> T get( String name, Class<T> as )
    {
        return asType( as, new JsonResponse( content, expression + "." + name ) );
    }

    @Override
    public <T extends JsonValue> T as( Class<T> as )
    {
        return asType( as, this );
    }

    @SuppressWarnings( "unchecked" )
    private <T extends JsonValue> T asType( Class<T> as, JsonValue res )
    {
        return isExtended( as ) ? createProxy( as, res ) : (T) res;
    }

    @Override
    public Boolean bool()
    {
        return (Boolean) value( ex -> null );
    }

    @Override
    public <A, B> B mapNonNull( A from, Function<A, B> to )
    {
        if ( from == null )
        {
            throw noSuchValue( null );
        }
        return to.apply( from );
    }

    @Override
    public boolean isEmpty()
    {
        return ObjectUtils.isEmpty( value( this::noSuchValue ) );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public boolean has( String... names )
    {
        Object value = value( this::noSuchValue );
        if ( value instanceof Map )
        {
            return ((Map<String, ?>) value).keySet().containsAll( Arrays.asList( names ) );
        }
        return false;
    }

    @Override
    public List<String> stringValues()
    {
        return arrayList( String.class );
    }

    @Override
    public List<Number> numberValues()
    {
        return arrayList( Number.class );
    }

    @Override
    public List<Boolean> boolValues()
    {
        return arrayList( Boolean.class );
    }

    @SuppressWarnings( "unchecked" )
    private <T> List<T> arrayList( Class<T> elementType )
    {
        Object value = value( this::noSuchValue );
        if ( value instanceof JSONArray )
        {
            for ( Object e : (JSONArray) value )
            {
                if ( !elementType.isInstance( e ) )
                {
                    throw new IllegalArgumentException( "element is not a " + elementType );
                }
            }
            return (List<T>) value;
        }
        throw new IllegalArgumentException( "not an array: " + expression );
    }

    @Override
    public int size()
    {
        Object value = value( this::noSuchValue );
        if ( value instanceof String )
        {
            return ((String) value).length();
        }
        if ( value instanceof java.util.Collection )
        {
            return ((java.util.Collection<?>) value).size();
        }
        if ( value instanceof Object[] )
        {
            return ((Object[]) value).length;
        }
        if ( value instanceof Map )
        {
            return ((Map<?, ?>) value).size();
        }
        throw new UnsupportedOperationException( "Size of " + value + " not supported!" );
    }

    @Override
    public boolean isArray()
    {
        Object value = value( this::noSuchValue );
        return value instanceof JSONArray || value instanceof Object[];
    }

    @Override
    public boolean isObject()
    {
        return value( this::noSuchValue ) instanceof Map;
    }

    @Override
    public Number number()
    {
        return (Number) value( ex -> null );
    }

    @Override
    public String string()
    {
        return (String) value( ex -> null );
    }

    @Override
    public boolean exists()
    {
        return value( ex -> null ) != null;
    }

    @Override
    public boolean isNull()
    {
        return value( this::noSuchValue ) == null;
    }

    private NoSuchElementException noSuchValue( JsonPathException ex )
    {
        NoSuchElementException res = new NoSuchElementException(
            "expected: <" + expression + "> \nbut found: \n" + content );
        if ( ex != null )
        {
            res.initCause( ex );
        }
        return res;
    }

    @Override
    public boolean equals( Object obj )
    {
        return obj instanceof JsonResponse
            && expression.equals( ((JsonResponse) obj).expression )
            && content.equals( ((JsonResponse) obj).content );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( content, expression );
    }

    @Override
    public String toString()
    {
        return expression + " in " + content + (valueCache == null ? "" : " => " + valueCache.toString());
    }

    @SuppressWarnings( "unchecked" )
    private static <E extends JsonValue> E createProxy( Class<E> as, JsonValue inner )
    {
        return (E) Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(), new Class[] { as },
            ( proxy, method, args ) -> {
                // are we dealing with a default method in the extending class?
                Class<?> declaringClass = method.getDeclaringClass();
                if ( method.isDefault() && isExtended( declaringClass ) )
                {
                    // call the default method of the proxied type itself
                    if ( !isJava8() )
                    {
                        return MethodHandles.lookup()
                            .findSpecial( declaringClass, method.getName(),
                                MethodType.methodType( method.getReturnType(), method.getParameterTypes() ),
                                declaringClass )
                            .bindTo( proxy ).invokeWithArguments();
                    }
                    return MethodHandles.lookup()
                        .in( declaringClass )
                        .unreflectSpecial( method, declaringClass )
                        .bindTo( proxy ).invokeWithArguments( args );
                }
                // call the same method on the wrapped object (assuming it has
                // it)
                try
                {
                    return method.invoke( inner, args );
                }
                catch ( InvocationTargetException ex )
                {
                    throw ex.getTargetException();
                }
            } );
    }

    private static boolean isExtended( Class<?> declaringClass )
    {
        return !declaringClass.isAssignableFrom( JsonResponse.class );
    }

    private static boolean isJava8()
    {
        String javaVersion = System.getProperty( "java.version" );
        boolean javaVersionIsBlank = javaVersion.trim().isEmpty();
        return !javaVersionIsBlank && javaVersion.startsWith( "1.8" );
    }
}
