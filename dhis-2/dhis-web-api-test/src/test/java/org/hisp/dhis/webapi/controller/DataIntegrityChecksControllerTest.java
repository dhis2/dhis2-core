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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.dataintegrity.DataIntegrityCheckType;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityCheck;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link DataIntegrityController} API with focus API returning
 * {@link org.hisp.dhis.dataintegrity.DataIntegrityCheck} information.
 *
 * @author Jan Bernitt
 */
class DataIntegrityChecksControllerTest extends AbstractDataIntegrityControllerTest
{
    @Test
    void testGetAvailableChecks()
    {
        JsonList<JsonDataIntegrityCheck> checks = GET( "/dataIntegrity" ).content()
            .asList( JsonDataIntegrityCheck.class );
        assertFalse( checks.isEmpty() );
        assertCheckExists( "categories_no_options", checks );
        assertCheckExists( "categories_one_default_category", checks );
        assertCheckExists( "categories_one_default_category_option", checks );
        assertCheckExists( "categories_one_default_category_combo", checks );
        assertCheckExists( "categories_one_default_category_option_combo", checks );
        assertCheckExists( "categories_unique_category_combo", checks );
        for ( DataIntegrityCheckType type : DataIntegrityCheckType.values() )
        {
            assertCheckExists( type.getName(), checks );
        }
    }

    private void assertCheckExists( String name, JsonList<JsonDataIntegrityCheck> checks )
    {
        assertTrue( checks.stream().anyMatch( check -> check.getName().equals( name ) ) );
    }
}
