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

import static java.util.Collections.emptyList;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

import org.hisp.dhis.webapi.json.JsonDocument.JsonFormatException;
import org.hisp.dhis.webapi.json.JsonDocument.JsonNode;
import org.hisp.dhis.webapi.json.JsonDocument.JsonNodeType;
import org.hisp.dhis.webapi.json.JsonDocument.JsonPathException;

/**
 * Implements the {@link JsonValue} read-only access abstraction for JSON
 * responses.
 *
 * The way this works is that internally when navigating the JSON object the
 * {@link #path} is extended. The returned value is always either a
 * {@link JsonResponse} or a {@link Proxy} calling all default methods on the
 * declaring interface and all implemented by {@link JsonResponse} on a
 * "wrapped" {@link JsonResponse} instance.
 *
 * When values are accessed the {@link #path} path is extracted from the
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

    private final JsonDocument content;

    private final String path;

    public JsonResponse( String content )
    {
        this( new JsonDocument( content.isEmpty() ? "{}" : content ), "$" );
    }

    private JsonResponse( JsonDocument content, String path )
    {
        this.content = content;
        this.path = path;
    }

    private <T> T value( JsonNodeType expected, Function<JsonNode, T> get, Function<JsonPathException, T> orElse )
    {
        try
        {
            JsonNode node = content.get( path );
            if ( node.getType() != expected )
            {
                throw new UnsupportedOperationException(
                    String.format( "Path %s does not contain a %s but %s", path, expected, node ) );
            }
            return get.apply( node );
        }
        catch ( JsonDocument.JsonPathException ex )
        {
            return orElse.apply( ex );
        }
    }

    private <T> T value( Function<JsonNode, T> get )
    {
        try
        {
            return get.apply( content.get( path ) );
        }
        catch ( JsonPathException | JsonFormatException ex )
        {
            throw noSuchElement( ex );
        }
    }

    private JsonNode value()
    {
        return value( Function.identity() );
    }

    @Override
    public <T extends JsonValue> T get( int index, Class<T> as )
    {
        return asType( as, new JsonResponse( content, path + "[" + index + "]" ) );
    }

    @Override
    public <T extends JsonValue> T get( String name, Class<T> as )
    {
        return asType( as, new JsonResponse( content, path + "." + name ) );
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
    public <A, B> B mapNonNull( A from, Function<A, B> to )
    {
        if ( from == null )
        {
            try
            {
                content.get( path );
            }
            catch ( JsonPathException ex )
            {
                throw noSuchElement( ex );
            }
        }
        return to.apply( from );
    }

    @Override
    public boolean isEmpty()
    {
        return value().isEmpty();
    }

    @Override
    public boolean has( String... names )
    {
        return value( JsonNodeType.OBJECT, node -> node.object().keySet().containsAll( Arrays.asList( names ) ),
            ex -> false );
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
        return value( JsonNodeType.ARRAY, node -> {
            List<T> res = new ArrayList<>();
            for ( JsonNode e : node.array() )
            {
                Object value = e.value();
                if ( !elementType.isInstance( value ) )
                {
                    throw new IllegalArgumentException( "element is not a " + elementType );
                }
                res.add( (T) value );
            }
            return res;
        }, ex -> emptyList() );
    }

    @Override
    public int size()
    {
        return value().size();
    }

    @Override
    public boolean isArray()
    {
        return value().getType() == JsonNodeType.ARRAY;
    }

    @Override
    public boolean isObject()
    {
        return value().getType() == JsonNodeType.OBJECT;
    }

    @Override
    public Boolean bool()
    {
        return (Boolean) value( JsonNodeType.BOOLEAN, JsonNode::value, ex -> null );
    }

    @Override
    public Number number()
    {
        return (Number) value( JsonNodeType.NUMBER, JsonNode::value, ex -> null );
    }

    @Override
    public String string()
    {
        return (String) value( JsonNodeType.STRING, JsonNode::value, ex -> null );
    }

    @Override
    public boolean exists()
    {
        try
        {
            return content.get( path ) != null;
        }
        catch ( JsonPathException ex )
        {
            return false;
        }
    }

    @Override
    public boolean isNull()
    {
        return value().getType() == JsonNodeType.NULL;
    }

    @Override
    public boolean equals( Object obj )
    {
        return obj instanceof JsonResponse
            && path.equals( ((JsonResponse) obj).path )
            && content.equals( ((JsonResponse) obj).content );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( content, path );
    }

    @Override
    public String toString()
    {
        try
        {
            return content.get( path ).getDeclaration();
        }
        catch ( JsonPathException | JsonFormatException ex )
        {
            return path + " in " + content;
        }
    }

    private NoSuchElementException noSuchElement( RuntimeException cause )
    {
        NoSuchElementException ex = new NoSuchElementException();
        ex.initCause( cause );
        return ex;
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
                    Constructor<Lookup> constructor = Lookup.class
                        .getDeclaredConstructor( Class.class );
                    constructor.setAccessible( true );
                    return constructor.newInstance( declaringClass )
                        .in( declaringClass )
                        .unreflectSpecial( method, declaringClass )
                        .bindTo( proxy )
                        .invokeWithArguments();
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
