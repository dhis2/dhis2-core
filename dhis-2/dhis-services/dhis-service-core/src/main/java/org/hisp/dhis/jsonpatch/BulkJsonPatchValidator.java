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
package org.hisp.dhis.jsonpatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchOperation;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.schema.Schema;

/**
 * Contains validation methods that can be added to {@link BulkPatchParameters}
 * <p>
 * and are called in {@link BulkPatchManager} before {@link JsonPatch} are
 * applied.
 */
public class BulkJsonPatchValidator
{
    /**
     * Validate if all {@link JsonPatchOperation} of given {@link JsonPatch} are
     * applied to "sharing" property.
     *
     * @param patch {@link JsonPatch} for validating.
     * @return {@link ErrorCode#E4032} if {@link JsonPatchOperation#getPath()}
     *         is different from "sharing"
     */
    public static List<ErrorReport> validateSharingPath( JsonPatch patch )
    {
        List<ErrorReport> errors = new ArrayList<>();

        for ( JsonPatchOperation operation : patch.getOperations() )
        {
            if ( !operation.getPath().matchesProperty( "sharing" ) )
            {
                errors.add( new ErrorReport( JsonPatchException.class, ErrorCode.E4032, operation.getPath() ) );
            }
        }

        return errors;
    }

    /**
     * Validate if given schema is shareable.
     *
     * @param schema {@link Schema} for validation.
     * @return {@link ErrorCode#E3019} if given {@link Schema#isShareable()} is
     *         false.
     */
    public static List<ErrorReport> validateShareableSchema( Schema schema )
    {
        if ( !schema.isShareable() )
        {
            return Collections
                .singletonList( new ErrorReport( JsonPatchException.class, ErrorCode.E3019, schema.getName() ) );
        }

        return Collections.emptyList();
    }
}
