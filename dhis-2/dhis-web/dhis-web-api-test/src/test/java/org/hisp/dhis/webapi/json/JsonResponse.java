package org.hisp.dhis.webapi.json;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.springframework.util.ObjectUtils;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.PathNotFoundException;

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
public final class JsonResponse implements JsonObject, JsonArray, JsonString, JsonNumber, JsonBoolean
{

    private final String content;

    private final String expression;

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
        try
        {
            return path().read( content );
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

    @SuppressWarnings( "unchecked" )
    private <T extends JsonValue> T asType( Class<T> as, JsonValue res )
    {
        return isExtended( as ) ? createProxy( as, res ) : (T) res;
    }

    @Override
    public boolean booleanValue()
    {
        return (Boolean) value( null );
    }

    @Override
    public boolean isEmpty()
    {
        return ObjectUtils.isEmpty( value( null ) );
    }

    @Override
    public int size()
    {
        Object value = value( this::noSuchValue );
        if ( value instanceof String )
        {
            return ((String) value).length();
        }
        if ( value instanceof Collection )
        {
            return ((Collection<?>) value).size();
        }
        if ( value instanceof Object[] )
        {
            return ((Object[]) value).length;
        }
        throw new UnsupportedOperationException( "Size of " + value + " not supported!" );
    }

    @Override
    public Number number()
    {
        return (Number) value( this::noSuchValue );
    }

    @Override
    public String string()
    {
        return (String) value( this::noSuchValue );
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
        NoSuchElementException res = new NoSuchElementException();
        res.initCause( ex );
        return res;
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
                    // call the default method of the proxy itself

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
                return method.invoke( inner, args );
            } );
    }

    private static <E extends JsonValue> boolean isExtended( Class<?> declaringClass )
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
