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
package org.hisp.dhis.schema.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hisp.dhis.schema.GistPreferences;
import org.hisp.dhis.schema.GistPreferences.Flag;
import org.hisp.dhis.schema.GistProjection;
import org.hisp.dhis.schema.Property;

/**
 * {@link Gist} is used to set {@link org.hisp.dhis.schema.GistPreferences} in
 * {@link Property}.
 *
 * Type level annotations apply to all getters. Annotations on getter overwrite
 * those on type.
 *
 * @author Jan Bernitt
 */
@Documented
@Inherited
@Target( { ElementType.METHOD, ElementType.TYPE } )
@Retention( RetentionPolicy.RUNTIME )
public @interface Gist
{
    Flag includeByDefault() default GistPreferences.Flag.AUTO;

    /**
     * @return The list of fields shown when the referenced object is included
     *         in a view. The empty list applies automatic selection based on
     *         schema.
     */
    String[] fields() default {};

    /**
     * @return the type used in case the user has not specified the type
     *         explicitly.
     */
    GistProjection defaultLinkage() default GistProjection.AUTO;

    /**
     * @return The set of types that can be used (are permitted). If a type is
     *         not included in the set but requested by a request the request is
     *         either denied or a permitted type is chosen instead.
     */
    GistProjection[] options() default {
        GistProjection.NONE,
        GistProjection.SIZE,
        GistProjection.IS_EMPTY,
        GistProjection.IS_NOT_EMPTY,
        GistProjection.IDS,
        GistProjection.ID_OBJECTS };

}
