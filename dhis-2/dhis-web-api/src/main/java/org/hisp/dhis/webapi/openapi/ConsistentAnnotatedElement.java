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
package org.hisp.dhis.webapi.openapi;

import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isStatic;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import lombok.Value;

/**
 * Consistency of @{@link java.lang.annotation.Repeatable} annotations:
 * <ul>
 * <li>{@link AnnotatedElement#getAnnotationsByType(Class)} already considers
 * presence within repeater</li>
 * <li>{@link AnnotatedElement#getAnnotation(Class)} now also considers presence
 * of the annotation when within an repeater</li>
 * <li>{@link AnnotatedElement#isAnnotationPresent(Class)} now also considers
 * presence of the annotation when within an repeater</li>
 * </ul>
 *
 * Consistency of @{@link Inherited} annotations:
 * <ul>
 * <li>When a type ({@link Class}) is annotated annotations are already
 * inherited</li>
 * <li>When a {@link Method} is annotated now annotations on a potentially
 * overridden method are included</li>
 * <li>When a {@link Parameter} is annotated now annotations on the same
 * parameter of a potentially overridden method are included</li>
 * <li>When a {@link Constructor} is annotated there is no inheritance</li>
 * <li>When a {@link java.lang.reflect.Field} is annotated there is no
 * inheritance (field shadowing)</li>
 * </ul>
 *
 * @author Jan Bernitt
 */
@Value( staticConstructor = "of" )
public class ConsistentAnnotatedElement implements AnnotatedElement
{
    AnnotatedElement target;

    @Override
    public Annotation[] getAnnotations()
    {
        return target.getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations()
    {
        return target.getDeclaredAnnotations();
    }

    @Override
    public boolean isAnnotationPresent( @Nonnull Class<? extends Annotation> type )
    {
        return target.isAnnotationPresent( type ) || getAnnotationsByType( type ).length > 0;
    }

    @Override
    public <T extends Annotation> T getAnnotation( @Nonnull Class<T> type )
    {
        T a = target.getAnnotation( type );
        if ( a != null )
            return a;
        T[] as = getAnnotationsByType( type );
        return as.length == 0 ? null : as[0];
    }

    @Override
    public <T extends Annotation> T[] getAnnotationsByType( Class<T> type )
    {
        if ( !(target instanceof Method || target instanceof Parameter)
            || !type.isAnnotationPresent( Inherited.class ) )
            return target.getAnnotationsByType( type );
        if ( target instanceof Method )
        {
            return getMethodAnnotationsByType( type, (Method) target );
        }
        // must be a parameter then
        return getParameterAnnotationsByType( type, (Parameter) target );
    }

    private <T extends Annotation> T[] getMethodAnnotationsByType( Class<T> type, Method method )
    {
        if ( isPrivate( method.getModifiers() )
            || isStatic( method.getModifiers() )
            || method.isSynthetic() )
        {
            return target.getAnnotationsByType( type );
        }
        Method m = method;
        Class<?> in = m.getDeclaringClass();
        List<T> annotations = new ArrayList<>();
        while ( in != Object.class )
        {
            if ( m != null )
            {
                annotations.addAll( List.of( m.getAnnotationsByType( type ) ) );
            }
            in = in.getSuperclass();
            if ( in != Object.class )
            {
                try
                {
                    m = in.getDeclaredMethod( method.getName(), method.getParameterTypes() );
                }
                catch ( NoSuchMethodException e )
                {
                    m = null;
                }
            }
        }
        return toArray( annotations, type );
    }

    private <T extends Annotation> T[] getParameterAnnotationsByType( Class<T> type, Parameter parameter )
    {
        Executable on = parameter.getDeclaringExecutable();
        if ( on instanceof Constructor
            || isPrivate( on.getModifiers() )
            || isStatic( on.getModifiers() )
            || on.isSynthetic() || parameter.isImplicit() )
        {
            return target.getAnnotationsByType( type );
        }
        int index = 0;
        Parameter[] parameters = on.getParameters();
        for ( int i = 0; i < on.getParameterCount(); i++ )
        {
            if ( parameters[i].equals( parameter ) )
                index = i;
        }
        Parameter p = parameter;
        Class<?> in = p.getDeclaringExecutable().getDeclaringClass();
        List<T> annotations = new ArrayList<>();
        while ( in != Object.class )
        {
            if ( p != null )
            {
                annotations.addAll( List.of( p.getAnnotationsByType( type ) ) );
            }
            in = in.getSuperclass();
            if ( in != Object.class )
            {
                try
                {
                    Method m = in.getDeclaredMethod( on.getName(), on.getParameterTypes() );
                    p = m.getParameters()[index];
                }
                catch ( NoSuchMethodException e )
                {
                    p = null;
                }
            }
        }
        return toArray( annotations, type );
    }

    private <T extends Annotation> T[] toArray( List<T> list, Class<T> elementType )
    {
        @SuppressWarnings( "unchecked" )
        T[] array = (T[]) Array.newInstance( elementType, list.size() );
        return list.toArray( array );
    }
}
