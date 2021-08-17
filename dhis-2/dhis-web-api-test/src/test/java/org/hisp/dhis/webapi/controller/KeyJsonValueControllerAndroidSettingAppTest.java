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
import static org.hisp.dhis.appmanager.AndroidSettingApp.AUTHORITY;
import static org.hisp.dhis.appmanager.AndroidSettingApp.NAMESPACE;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * This tests verifies the protection of the
 * {@link org.hisp.dhis.appmanager.AndroidSettingApp#NAMESPACE} in the
 * {@link org.hisp.dhis.keyjsonvalue.KeyJsonValueService} that is installed on
 * startup.
 *
 * @author Jan Bernitt
 */
public class KeyJsonValueControllerAndroidSettingAppTest extends DhisControllerConvenienceTest
{

    @Before
    public void setUp()
    {
        assertStatus( HttpStatus.CREATED, POST( "/dataStore/" + NAMESPACE + "/key", "['yes']" ) );
    }

    /**
     * Everyone can read the keys
     */
    @Test
    public void testGetKeysInNamespace()
    {
        switchToNewUser( "not-an-android-manager" );
        assertEquals( singletonList( "key" ), GET( "/dataStore/" + NAMESPACE ).content().stringValues() );
    }

    /**
     * Everyone can read the value
     */
    @Test
    public void testGetKeyJsonValue()
    {
        switchToNewUser( "not-an-android-manager" );
        assertEquals( singletonList( "yes" ), GET( "/dataStore/" + NAMESPACE + "/key" ).content().stringValues() );
    }

    /**
     * Everyone can read the meta data
     */
    @Test
    public void testGetKeyJsonValueMetaData()
    {
        switchToNewUser( "not-an-android-manager" );
        assertStatus( HttpStatus.OK, GET( "/dataStore/" + NAMESPACE + "/key/metaData" ) );
    }

    /**
     * Only user with
     * {@link org.hisp.dhis.appmanager.AndroidSettingApp#AUTHORITY} can delete
     * the NS.
     */
    @Test
    public void testDeleteNamespace()
    {
        switchToNewUser( "not-an-android-manager" );
        assertEquals(
            "The namespace 'ANDROID_SETTING_APP' is protected, and you don't have the right authority to access or modify it.",
            DELETE( "/dataStore/" + NAMESPACE ).error( HttpStatus.FORBIDDEN ).getMessage() );

        switchToNewUser( "andriod-manager", AUTHORITY );
        assertStatus( HttpStatus.OK, DELETE( "/dataStore/" + NAMESPACE ) );
    }

    /**
     * Only user with
     * {@link org.hisp.dhis.appmanager.AndroidSettingApp#AUTHORITY} can add to
     * the NS.
     */
    @Test
    public void testAddKeyJsonValue()
    {
        switchToNewUser( "not-an-android-manager" );
        assertEquals(
            "The namespace 'ANDROID_SETTING_APP' is protected, and you don't have the right authority to access or modify it.",
            POST( "/dataStore/" + NAMESPACE + "/new-key", "[]" ).error( HttpStatus.FORBIDDEN ).getMessage() );

        switchToNewUser( "andriod-manager", AUTHORITY );
        assertStatus( HttpStatus.CREATED, POST( "/dataStore/" + NAMESPACE + "/new-key", "[]" ) );
    }

    @Test
    public void testUpdateKeyJsonValue()
    {
        switchToNewUser( "not-an-android-manager" );
        assertEquals(
            "The namespace 'ANDROID_SETTING_APP' is protected, and you don't have the right authority to access or modify it.",
            PUT( "/dataStore/" + NAMESPACE + "/key", "[]" ).error( HttpStatus.FORBIDDEN ).getMessage() );

        switchToNewUser( "andriod-manager", AUTHORITY );
        assertStatus( HttpStatus.OK, PUT( "/dataStore/" + NAMESPACE + "/key", "[]" ) );
    }

    @Test
    public void testDeleteKeyJsonValue()
    {
        switchToNewUser( "not-an-android-manager" );
        assertEquals(
            "The namespace 'ANDROID_SETTING_APP' is protected, and you don't have the right authority to access or modify it.",
            DELETE( "/dataStore/" + NAMESPACE + "/key" ).error( HttpStatus.FORBIDDEN ).getMessage() );

        switchToNewUser( "andriod-manager", AUTHORITY );
        assertStatus( HttpStatus.OK, DELETE( "/dataStore/" + NAMESPACE + "/key" ) );
    }
}
