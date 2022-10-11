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
package org.hisp.dhis.common;

import static java.util.Arrays.stream;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.hisp.dhis.feedback.ErrorReport;

public interface ValidationService
{
    enum ValidationScenario
    {
        CREATE,
        UPDATE,
        DELETE
    }

    interface ValidationContext
    {

        /**
         * Does the performing user have the provided authority?
         */
        boolean hasAuthority( String authority );

        <T extends IdentifiableObject> Optional<T> findById( Class<T> type, String uid );

        <T extends IdentifiableObject> List<T> allExisting( Class<T> type );
    }

    <T extends IdentifiableObject> List<ErrorReport> validate( ValidationScenario scenario, Class<T> target,
        ValidationContext context, T object );

    /*
     * Registering individual validations
     */

    interface Validation<T extends IdentifiableObject>
    {

        void validate( T object, ValidationContext context, Consumer<ErrorReport> addError );
    }

    <T extends IdentifiableObject> void register( ValidationScenario scenario, Class<T> target,
        Validation<? super T> validation );

    default <T extends IdentifiableObject> void register( Class<T> target, Validation<? super T> validation )
    {
        stream( ValidationScenario.values() ).forEach( scenario -> register( scenario, target, validation ) );
    }
}
