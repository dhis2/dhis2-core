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
package org.hisp.dhis.analytics;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.hisp.dhis.dto.ApiResponse;

/**
 * Helper class to assist during the validation/assertion in e2e analytics
 * tests.
 *
 * @author maikel arabori
 */
public class ValidationHelper
{
    private ValidationHelper()
    {
    }

    /**
     * Validate/assert all attributes of the given header (represented by the
     * index), matching each argument with its respective header attribute
     * value.
     *
     * @param response
     * @param headerIndex of the header
     * @param name
     * @param column
     * @param valueType
     * @param type
     * @param hidden
     * @param meta
     */
    public static void validateHeader( ApiResponse response, int headerIndex, String name,
        String column, String valueType, String type, boolean hidden, boolean meta )
    {
        response.validate()
            .body( "headers[" + headerIndex + "].name", equalTo( name ) )
            .body( "headers[" + headerIndex + "].column", equalTo( column ) )
            .body( "headers[" + headerIndex + "].valueType", equalTo( valueType ) )
            .body( "headers[" + headerIndex + "].type", equalTo( type ) )
            .body( "headers[" + headerIndex + "].hidden", is( hidden ) )
            .body( "headers[" + headerIndex + "].meta", is( meta ) );
    }

    /**
     * Validate/assert that all values of the given row are present in the given
     * response.
     *
     * @param response
     * @param expectedValues
     */
    public static void validateRow( ApiResponse response, int rowIndex, List<String> expectedValues )
    {
        response.validate()
            .body( "rows[" + rowIndex + "]", equalTo( expectedValues ) );
    }
}
