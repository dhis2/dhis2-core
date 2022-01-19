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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests specifically the {@code order} parameter aspect of the
 * {@link DatastoreController#getEntries(String, String, boolean, HttpServletRequest, HttpServletResponse)}
 * method using (mocked) REST requests.
 * <p>
 * Tests will use {@code fields} parameter as it is a required parameter for the
 * API.
 *
 * @author Jan Bernitt
 */
class DatastoreOrderControllerTest extends AbstractDatastoreControllerTest
{
    @BeforeEach
    void setUp()
    {
        postPet( "cat", "Miao", 9, List.of( "tuna", "mice", "birds" ) );
        postPet( "cow", "Muuhh", 15, List.of( "gras" ) );
        postPet( "dog", "Pluto", 2, List.of( "rabbits" ) );
        postPet( "pig", "Oink", 6, List.of( "carrots", "potatoes" ) );
    }

    @Test
    void testOrder_Asc_String()
    {
        assertJson( "[{'key':'cat'},{'key':'cow'},{'key':'pig'},{'key':'dog'}]",
            GET( "/dataStore/pets?fields=&headless=true&order=name" ) );
    }

    @Test
    void testOrder_Desc_String()
    {
        assertJson( "[{'key':'dog'},{'key':'pig'},{'key':'cow'},{'key':'cat'}]",
            GET( "/dataStore/pets?fields=&headless=true&order=name:desc" ) );
    }

    @Test
    void testOrder_Asc_Number()
    {
        assertJson( "[{'key':'dog'},{'key':'pig'},{'key':'cat'},{'key':'cow'}]",
            GET( "/dataStore/pets?fields=&headless=true&order=age:nasc" ) );
    }

    @Test
    void testOrder_Desc_Number()
    {
        assertJson( "[{'key':'cow'},{'key':'cat'},{'key':'pig'},{'key':'dog'}]",
            GET( "/dataStore/pets?fields=&headless=true&order=age:ndesc" ) );
    }
}
