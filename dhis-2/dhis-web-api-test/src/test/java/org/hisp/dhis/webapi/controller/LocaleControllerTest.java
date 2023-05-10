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

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.jsontree.*;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonWebLocale;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link LocaleController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class LocaleControllerTest extends DhisControllerConvenienceTest
{

    @Test
    void testAddLocale()
    {
        assertWebMessage( "Created", 201, "OK", "Locale created successfully",
            POST( "/locales/dbLocales?language=en&country=GB" ).content( HttpStatus.CREATED ) );
    }

    @Test
    void testAddLocale_InvalidCountry()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Invalid country or language code.",
            POST( "/locales/dbLocales?language=en&country=" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testAddLocale_InvalidLanguage()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Invalid country or language code.",
            POST( "/locales/dbLocales?language=&country=GB" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testAddLocale_AlreadyExists()
    {
        assertStatus( HttpStatus.CREATED, POST( "/locales/dbLocales?language=en&country=GB" ) );
        assertWebMessage( "Conflict", 409, "ERROR", "Locale code existed.",
            POST( "/locales/dbLocales?language=en&country=GB" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testGetUiLocales()
    {
        JsonArray response = GET( "/locales/ui" ).content();
        assertEquals( 38, response.size() );
    }

    @Test
    void testGetUiLocalesInUserLanguage()
    {
        String userEnglishLocale = GET( "/userSettings/keyUiLocale/?userId=" + ADMIN_USER_UID )
            .content( "text/plain; charset=UTF-8" );
        assertEquals( "en", userEnglishLocale );

        JsonArray response = GET( "/locales/ui" ).content();
        JsonWebLocale firstElement = response.getObject( 0 ).as( JsonWebLocale.class );
        assertEquals( "ar", firstElement.getLocale() );
        assertEquals( "العربية", firstElement.getName() );
        assertEquals( "Arabic", firstElement.getDisplayName() );
    }

    @Test
    void testGetUiLocaleAfterUserLanguageChange()
    {
        POST( "/userSettings/keyUiLocale/?userId=" + ADMIN_USER_UID + "&value=fr" );
        JsonArray response = GET( "/locales/ui" ).content();
        JsonWebLocale firstElement = response.getObject( 0 ).as( JsonWebLocale.class );
        assertEquals( "en", firstElement.getLocale() );
        assertEquals( "English", firstElement.getName() );
        assertEquals( "anglais", firstElement.getDisplayName() );
    }

    @Test
    void testGetDbLocales()
    {
        POST( "/locales/dbLocales?country=IE&language=en" );
        JsonArray response = GET( "/locales/db" ).content();
        assertEquals( 1, response.size() );
        JsonWebLocale firstElement = response.getObject( 0 ).as( JsonWebLocale.class );
        assertEquals( "en_IE", firstElement.getLocale() );
        assertEquals( "English (Ireland)", firstElement.getName() );
        assertEquals( "English (Ireland)", firstElement.getDisplayName() );
    }

    @Test
    void testGetDbLocalesAfterUserLanguageChange()
    {
        POST( "/locales/dbLocales?country=IE&language=en" );
        JsonArray response = GET( "/locales/db" ).content();
        assertEquals( 1, response.size() );
        JsonWebLocale firstElement = response.getObject( 0 ).as( JsonWebLocale.class );
        assertEquals( "en_IE", firstElement.getLocale() );
        assertEquals( "English (Ireland)", firstElement.getName() );
        assertEquals( "English (Ireland)", firstElement.getDisplayName() );

        POST( "/userSettings/keyUiLocale/?userId=" + ADMIN_USER_UID + "&value=fr" );
        JsonArray response2 = GET( "/locales/db" ).content();
        JsonWebLocale dbLocaleElement = response2.getObject( 0 ).as( JsonWebLocale.class );
        assertEquals( "en_IE", dbLocaleElement.getLocale() );
        assertEquals( "English (Ireland)", dbLocaleElement.getName() );
        assertEquals( "anglais (Irlande)", dbLocaleElement.getDisplayName() );
    }
}
