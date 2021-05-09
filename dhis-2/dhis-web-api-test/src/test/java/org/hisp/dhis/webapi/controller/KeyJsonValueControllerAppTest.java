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
package org.hisp.dhis.webapi.controller;

import static java.util.Collections.singletonList;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;

/**
 * A test for the {@link KeyJsonValueController} where we test the behaviour of
 * namespaces that belong to {@link org.hisp.dhis.appmanager.App}s.
 *
 * The test installs a tiny (manifest only) {@link org.hisp.dhis.appmanager.App}
 * with the namespace {@code test-app-ns} and checks that only authorised users
 * have access to it.
 *
 * This test is purely about access control aspect and does not spend much
 * effort on verifying correctness of the returned payload. This is handled by
 * {@link KeyJsonValueControllerTest}.
 *
 * Each test scenario tests that the
 * {@link AppManager#WEB_MAINTENANCE_APPMANAGER_AUTHORITY} user can use the
 * store, that a app manager with the {@link App#getSeeAppAuthority()} can use
 * the store but a user lacking any of the two authorities can not.
 *
 * @see KeyJsonValueControllerTest
 *
 * @author Jan Bernitt
 */
public class KeyJsonValueControllerAppTest extends DhisControllerConvenienceTest
{

    @Autowired
    private AppManager appManager;

    @Before
    public void setUp()
        throws IOException
    {
        assertEquals( AppStatus.OK,
            appManager.installApp( new ClassPathResource( "app/test-app.zip" ).getFile(), "test-app.zip" ) );
        // by default we are an app manager
        switchToNewUser( "app-admin", AppManager.WEB_MAINTENANCE_APPMANAGER_AUTHORITY );
    }

    @Test
    public void testGetKeysInNamespace()
    {
        assertStatus( HttpStatus.CREATED, POST( "/dataStore/test-app-ns/key1", "[]" ) );
        assertEquals( singletonList( "key1" ), GET( "/dataStore/test-app-ns" ).content().stringValues() );

        switchToNewUser( "just-test-app-admin", App.SEE_APP_AUTHORITY_PREFIX + "test" );
        assertEquals( singletonList( "key1" ), GET( "/dataStore/test-app-ns" ).content().stringValues() );

        switchToNewUser( "has-no-app-authority" );
        assertEquals(
            "The namespace 'test-app-ns' is protected, and you don't have the right authority to access or modify it.",
            GET( "/dataStore/test-app-ns" ).error( HttpStatus.FORBIDDEN ).getMessage() );
    }

    @Test
    public void testDeleteNamespace()
    {
        assertStatus( HttpStatus.CREATED, POST( "/dataStore/test-app-ns/key1", "[]" ) );

        switchToNewUser( "has-no-app-authority" );
        assertEquals(
            "The namespace 'test-app-ns' is protected, and you don't have the right authority to access or modify it.",
            DELETE( "/dataStore/test-app-ns" ).error( HttpStatus.FORBIDDEN ).getMessage() );

        switchToNewUser( "just-test-app-admin", App.SEE_APP_AUTHORITY_PREFIX + "test" );
        assertStatus( HttpStatus.OK, DELETE( "/dataStore/test-app-ns" ) );
    }

    @Test
    public void testGetKeyJsonValue()
    {
        assertStatus( HttpStatus.CREATED, POST( "/dataStore/test-app-ns/key1", "[]" ) );
        assertStatus( HttpStatus.OK, GET( "/dataStore/test-app-ns/key1" ) );

        switchToNewUser( "just-test-app-admin", App.SEE_APP_AUTHORITY_PREFIX + "test" );
        assertStatus( HttpStatus.OK, GET( "/dataStore/test-app-ns/key1" ) );

        switchToNewUser( "has-no-app-authority" );
        assertEquals(
            "The namespace 'test-app-ns' is protected, and you don't have the right authority to access or modify it.",
            GET( "/dataStore/test-app-ns/key1" ).error( HttpStatus.FORBIDDEN ).getMessage() );
    }

    @Test
    public void testGetKeyJsonValueMetaData()
    {
        assertStatus( HttpStatus.CREATED, POST( "/dataStore/test-app-ns/key1", "[]" ) );
        assertStatus( HttpStatus.OK, GET( "/dataStore/test-app-ns/key1/metaData" ) );

        switchToNewUser( "just-test-app-admin", App.SEE_APP_AUTHORITY_PREFIX + "test" );
        assertStatus( HttpStatus.OK, GET( "/dataStore/test-app-ns/key1/metaData" ) );

        switchToNewUser( "has-no-app-authority" );
        assertEquals(
            "The namespace 'test-app-ns' is protected, and you don't have the right authority to access or modify it.",
            GET( "/dataStore/test-app-ns/key1/metaData" ).error( HttpStatus.FORBIDDEN ).getMessage() );
    }

    @Test
    public void testAddKeyJsonValue()
    {
        assertStatus( HttpStatus.CREATED, POST( "/dataStore/test-app-ns/key1", "[]" ) );

        switchToNewUser( "just-test-app-admin", App.SEE_APP_AUTHORITY_PREFIX + "test" );
        assertStatus( HttpStatus.CREATED, POST( "/dataStore/test-app-ns/key2", "{}" ) );

        switchToNewUser( "has-no-app-authority" );
        assertEquals(
            "The namespace 'test-app-ns' is protected, and you don't have the right authority to access or modify it.",
            POST( "/dataStore/test-app-ns/key3", "{}" ).error( HttpStatus.FORBIDDEN ).getMessage() );
    }

    @Test
    public void testUpdateKeyJsonValue()
    {
        assertStatus( HttpStatus.CREATED, POST( "/dataStore/test-app-ns/key1", "[]" ) );
        assertStatus( HttpStatus.OK, PUT( "/dataStore/test-app-ns/key1", "{}" ) );

        switchToNewUser( "just-test-app-admin", App.SEE_APP_AUTHORITY_PREFIX + "test" );
        assertStatus( HttpStatus.OK, PUT( "/dataStore/test-app-ns/key1", "{}" ) );

        switchToNewUser( "has-no-app-authority" );
        assertEquals(
            "The namespace 'test-app-ns' is protected, and you don't have the right authority to access or modify it.",
            PUT( "/dataStore/test-app-ns/key1", "{}" ).error( HttpStatus.FORBIDDEN ).getMessage() );
    }

    @Test
    public void testDeleteKeyJsonValue()
    {
        assertStatus( HttpStatus.CREATED, POST( "/dataStore/test-app-ns/key1", "[]" ) );
        assertStatus( HttpStatus.OK, DELETE( "/dataStore/test-app-ns/key1" ) );

        switchToNewUser( "just-test-app-admin", App.SEE_APP_AUTHORITY_PREFIX + "test" );
        assertStatus( HttpStatus.CREATED, POST( "/dataStore/test-app-ns/key1", "[]" ) );
        assertStatus( HttpStatus.OK, DELETE( "/dataStore/test-app-ns/key1" ) );

        assertStatus( HttpStatus.CREATED, POST( "/dataStore/test-app-ns/key1", "[]" ) );
        switchToNewUser( "has-no-app-authority" );
        assertEquals(
            "The namespace 'test-app-ns' is protected, and you don't have the right authority to access or modify it.",
            DELETE( "/dataStore/test-app-ns/key1" ).error( HttpStatus.FORBIDDEN ).getMessage() );
    }

    @Test
    public void testStoreIsUnprotectedAfterAppIsDeleted()
    {
        assertStatus( HttpStatus.CREATED, POST( "/dataStore/test-app-ns/key1", "[]" ) );
        appManager.deleteApp( appManager.getApp( "test" ), false );

        switchToNewUser( "has-no-app-authority" );
        assertEquals( singletonList( "key1" ), GET( "/dataStore/test-app-ns" ).content().stringValues() );
    }

    @Test
    public void testNamespaceIsDeletedWhenAppIsDeletedWithData()
    {
        assertStatus( HttpStatus.CREATED, POST( "/dataStore/test-app-ns/key1", "[]" ) );
        appManager.deleteApp( appManager.getApp( "test" ), true );

        assertStatus( HttpStatus.NOT_FOUND, GET( "/dataStore/test-app-ns" ) );
    }

}
