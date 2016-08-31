package org.hisp.dhis.translation;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Locale;

import static org.junit.Assert.*;

/**
 * @author Oyvind Brucker
 */
@Ignore //TODO fails on ci
public class TranslationStoreTest
    extends DhisSpringTest
{
    @Autowired
    private TranslationStore translationStore;

    // -------------------------------------------------------------------------
    // Testdata
    // -------------------------------------------------------------------------

    private String uid1 = "uid1";
    private String uid2 = "uid2";

    private String locale1 = Locale.UK.toString();
    private String locale2 = Locale.US.toString();
    private String locale3 = Locale.FRANCE.toString();

    private String className1 = OrganisationUnit.class.getName();
    private String className2 = DataElement.class.getName();

    private Translation translation1a;
    private Translation translation1b;
    private Translation translation2a;
    private Translation translation2b;
    private Translation translation2c;

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testAddGet()
    {
        translation1a = new Translation( className1, locale1, "name", "cheers", className1 + uid1 );
        translation1b = new Translation( className1, locale1, "shortName", "goodbye", className1 + uid1 );
        translation2a = new Translation( className1, locale2, "name", "hello", className1 + uid1 );
        translation2b = new Translation( className2, locale2, "name", "hey", className1 + uid1 );
        translation2c = new Translation( className2, locale3, "name", "bonjour", className1 + uid2 );

        translationStore.save( translation1a );
        translationStore.save( translation1b );

        assertEquals( translation1a, translationStore.getTranslation( className1, Locale.UK, "name", uid1 ) );
        assertEquals( translation1b, translationStore.getTranslation( className1, Locale.UK, "shortName", uid1 ) );
    }

    @Test
    public void delete()
    {
        translationStore.save( translation1a );
        translationStore.save( translation1b );

        assertNotNull( translationStore.getTranslation( className1, Locale.UK, "name", uid1 ) );
        assertNotNull( translationStore.getTranslation( className1, Locale.UK, "shortName", uid1 ) );

        translationStore.delete( translation1a );

        assertNull( translationStore.getTranslation( className1, Locale.UK, "name", uid1 ) );
        assertNotNull( translationStore.getTranslation( className1, Locale.UK, "shortName", uid1 ) );

        translationStore.delete( translation1b );

        assertNull( translationStore.getTranslation( className1, Locale.UK, "name", uid1 ) );
        assertNull( translationStore.getTranslation( className1, Locale.UK, "shortName", uid1 ) );
    }

    @Test
    public void testUpdateTranslation()
    {
        translationStore.save( translation1a );

        assertEquals( translation1a, translationStore.getTranslation( className1, Locale.UK, "name", uid1 ) );

        translation1a.setValue( "regards" );

        translationStore.update( translation1a );

        assertEquals( "regards", translationStore.getTranslation( className1, Locale.UK, "name", uid1 ).getValue() );
    }

    @Test
    public void testGetTranslations1()
    {
        translationStore.save( translation1a );
        translationStore.save( translation1b );
        translationStore.save( translation2a );
        translationStore.save( translation2b );
        translationStore.save( translation2c );

        assertEquals( 2, translationStore.getTranslations( className1, Locale.UK, uid1 ).size() );
        assertTrue( translationStore.getTranslations( className1, Locale.UK, uid1 ).contains( translation1a ) );
        assertTrue( translationStore.getTranslations( className1, Locale.UK, uid1 ).contains( translation1b ) );
    }

    @Test
    public void testGetTranslations2()
    {
        translationStore.save( translation1a );
        translationStore.save( translation1b );
        translationStore.save( translation2a );
        translationStore.save( translation2b );
        translationStore.save( translation2c );

        assertEquals( 2, translationStore.getTranslations( className1, Locale.UK ).size() );
        assertTrue( translationStore.getTranslations( className1, Locale.UK, uid1 ).contains( translation1a ) );
        assertTrue( translationStore.getTranslations( className1, Locale.UK, uid1 ).contains( translation1b ) );
    }

    @Test
    @Ignore
    public void testGetAllTranslations()
    {
        translationStore.save( translation1a );
        translationStore.save( translation1b );
        translationStore.save( translation2a );
        translationStore.save( translation2b );
        translationStore.save( translation2c );

        assertEquals( 5, translationStore.getAll().size() );
    }

    @Test
    public void testHasTranslations()
    {
        translation1a = new Translation( className1, locale1, "name", "cheers", className1 + uid1 );
        translation1b = new Translation( className1, locale1, "shortName", "goodbye", className1 + uid1 );
        translation2a = new Translation( className1, locale2, "name", "hello", className1 + uid1 );

        translationStore.save( translation1a );
        translationStore.save( translation1b );
        translationStore.save( translation2a );
        
        assertTrue( translationStore.hasTranslations( className1 ) );
        assertFalse( translationStore.hasTranslations( className2 ) );
    }
}
