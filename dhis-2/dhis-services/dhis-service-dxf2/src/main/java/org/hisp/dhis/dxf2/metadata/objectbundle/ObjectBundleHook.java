/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.objectbundle;

import static java.util.Collections.emptyList;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hisp.dhis.feedback.ErrorReport;

/**
 * Contains hooks for object bundle commit phase.
 *
 * A hook is bound to a target type. This is either a specific class type (an
 * actual database object) or an abstract type. This means a hook either handles
 * one specific type or a family of types.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface ObjectBundleHook<T>
{
    /**
     * Is either:
     *
     * A non abstract {@code class} implementing {@link ObjectBundleHook} in
     * which case the hook is considered to only apply to that particular class
     *
     * Or an interface in which case the hook is considered to apply to all
     * classes implementing that interface.
     *
     * Or {@code null} in which case the hook is considered to apply to all
     * objects.
     *
     * @return The target type of object (scope) to which the hook applies.
     */
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    default Class<T> getTarget()
    {
        Class<? extends ObjectBundleHook> hookType = getClass();
        Type base = hookType.getGenericSuperclass();
        if ( base instanceof ParameterizedType
            && ObjectBundleHook.class.isAssignableFrom( (Class<?>) ((ParameterizedType) base).getRawType() ) )
        {
            return (Class<T>) ((ParameterizedType) base)
                .getActualTypeArguments()[0];
        }
        for ( Type t : hookType.getGenericInterfaces() )
        {
            if ( t instanceof ParameterizedType && (((ParameterizedType) t).getRawType() == ObjectBundleHook.class) )
            {
                return (Class<T>) ((ParameterizedType) t).getActualTypeArguments()[0];
            }
        }
        // all objects are targets
        return null;
    }

    /**
     * Hook to run custom validation code. Run before any other validation.
     *
     * @param object the object to validate, not null.
     * @param bundle the current validation phase bundle, not null.
     * @param addReports a consumer for errors identified during the validation,
     *        not null.
     */
    void validate( T object, ObjectBundle bundle, Consumer<ErrorReport> addReports );

    /**
     * Hook to run custom validation code. Run before any other validation.
     *
     * Should only be used in tests as a convenient way to run
     * {@link #validate(Object, ObjectBundle, Consumer)}. Otherwise prefer
     * {@link #validate(Object, ObjectBundle, Consumer)} to avoid intermediate
     * collections.
     *
     * @see #validate(Object, ObjectBundle, Consumer)
     * @param object the object to validate, not null.
     * @param bundle the current validation phase bundle, not null.
     * @return a list of potential validation errors, or empty list if
     *         validation passed.
     */
    default List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        @SuppressWarnings( "unchecked" )
        List<ErrorReport>[] box = new List[1];
        validate( object, bundle, error -> {
            List<ErrorReport> list = box[0];
            if ( list == null )
            {
                list = new ArrayList<>();
                box[0] = list;
            }
            list.add( error );
        } );
        List<ErrorReport> errors = box[0];
        return errors == null ? emptyList() : errors;
    }

    /**
     * Run before commit phase has started.
     *
     * This is only called if the bundle contains objects of this hooks
     * {@link #getTarget()} type. It is never called for hooks with abstract
     * target type.
     *
     * @param bundle the current commit phase bundle, not null.
     */
    void preCommit( ObjectBundle bundle );

    /**
     * Run after commit phase has finished.
     *
     * This is only called if the bundle contains objects of this hooks
     * {@link #getTarget()} type. It is never called for hooks with abstract
     * target type.
     *
     * @param bundle the current commit phase bundle, not null.
     */
    void postCommit( ObjectBundle bundle );

    /**
     * Run before a type import has started. I.e. run before importing orgUnits,
     * dataElements, etc.
     *
     * @param klass the class type of the objects, not null.
     * @param objects the objects, not null.
     * @param bundle the current commit phase bundle, not null.
     */
    <E extends T> void preTypeImport( Class<E> klass, List<E> objects, ObjectBundle bundle );

    /**
     * Run after a type import has finished. I.e. run before importing orgUnits,
     * dataElements, etc.
     *
     * @param klass the class type of the objects, not null.
     * @param objects the objects, not null.
     * @param bundle the current commit phase bundle, not null.
     */
    <E extends T> void postTypeImport( Class<E> klass, List<E> objects, ObjectBundle bundle );

    /**
     * Run before object has been created.
     *
     * @param object the object to be created, not null.
     * @param bundle the current commit phase bundle, not null.
     */
    void preCreate( T object, ObjectBundle bundle );

    /**
     * Run after object has been created.
     *
     * @param persistedObject the created object, not null.
     * @param bundle the current commit phase bundle, not null.
     */
    void postCreate( T persistedObject, ObjectBundle bundle );

    /**
     * Run before object has been updated.
     *
     * @param object the object to be created, not null.
     * @param persistedObject the created object, not null.
     * @param bundle the current commit phase bundle, not null.
     */
    void preUpdate( T object, T persistedObject, ObjectBundle bundle );

    /**
     * Run after object has been updated.
     *
     * @param persistedObject the created object, not null.
     * @param bundle the current commit phase bundle, not null.
     */
    void postUpdate( T persistedObject, ObjectBundle bundle );

    /**
     * Run before object has been deleted.
     *
     * @param persistedObject the created object, not null.
     * @param bundle the current commit phase bundle, not null.
     */
    void preDelete( T persistedObject, ObjectBundle bundle );
}
