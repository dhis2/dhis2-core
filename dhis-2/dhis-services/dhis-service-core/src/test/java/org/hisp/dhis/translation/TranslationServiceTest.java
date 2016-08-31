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
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Locale;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class TranslationServiceTest
    extends DhisSpringTest
{
    @Autowired
    private TranslationService translationService;

    // -------------------------------------------------------------------------
    // Testdata
    // -------------------------------------------------------------------------

    private String uid1 = "uid1";
    private String uid2 = "uid2";

    private String locale1 = Locale.UK.toString();
    private String locale2 = Locale.US.toString();
    private String locale3 = Locale.FRANCE.toString();

    private String className1 = "class1";
    private String className2 = "class2";

    private Translation translation1a;
    private Translation translation1b;
    private Translation translation2a;
    private Translation translation2b;
    private Translation translation2c;

    // -------------------------------------------------------------------------
    // Set up/tear down
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
    {
        translation1a = new Translation( className1, locale1, "name", "cheers", uid1 );
        translation1b = new Translation( className1, locale1, "shortName", "goodbye", uid1 );
        translation2a = new Translation( className1, locale2, "name", "hello", uid1 );
        translation2b = new Translation( className2, locale2, "name", "hey", uid1 );
        translation2c = new Translation( className2, locale3, "name", "bonjour", uid2 );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testAddGet()
    {
        translationService.addTranslation( translation1a );
        translationService.addTranslation( translation1b );

        assertEquals( translation1a, translationService.getTranslationNoFallback( className1, Locale.UK, "name", uid1 ) );
        assertEquals( translation1b, translationService.getTranslationNoFallback( className1, Locale.UK, "shortName", uid1 ) );
    }

    @Ignore
    @Test
    public void delete()
    {
        Translation translation1a = new Translation( className1, locale1, "name", "habari",  uid1 );
        Translation translation1b = new Translation( className1, locale1, "shortName", "kesho",  uid1 );

        translationService.addTranslation( translation1a );
        translationService.addTranslation( translation1b );

        assertNotNull( translationService.getTranslationNoFallback( className1, Locale.UK, "name", uid1 ) );
        assertNotNull( translationService.getTranslationNoFallback( className1, Locale.UK, "shortName", uid1 ) );

        translationService.deleteTranslation( translation1a );

        assertNull( translationService.getTranslationNoFallback( className1, Locale.UK, "name", uid1 ) );
        assertNotNull( translationService.getTranslationNoFallback( className1, Locale.UK, "shortName", uid1 ) );

        translationService.deleteTranslations( translation1b.getClassName(), translation1b.getObjectUid() );

        assertNull( translationService.getTranslationNoFallback( className1, Locale.UK, "name", uid1 ) );
        assertNull( translationService.getTranslationNoFallback( className1, Locale.UK, "shortName", uid1 ) );
    }

    @Ignore
    @Test
    public void testUpdateTranslation()
    {
        translationService.addTranslation( translation1a );

        assertEquals( translation1a, translationService.getTranslationNoFallback( className1, Locale.UK, "name",  uid1 ) );

        translation1a.setValue( "regards" );

        translationService.updateTranslation( translation1a );

        assertEquals( "regards", translationService.getTranslationNoFallback( className1, Locale.UK, "name",  uid1 ).getValue() );
    }

    @Test
    public void testGetTranslations()
    {
        translationService.addTranslation( translation1a );
        translationService.addTranslation( translation1b );
        translationService.addTranslation( translation2a );
        translationService.addTranslation( translation2b );
        translationService.addTranslation( translation2c );

        assertEquals( 2, translationService.getTranslations( className1, Locale.UK ).size() );
        assertTrue( translationService.getTranslationsNoFallback( className1, Locale.UK, uid1 ).contains( translation1a ) );
        assertTrue( translationService.getTranslationsNoFallback( className1, Locale.UK, uid1 ).contains( translation1b ) );
    }

    @Test
    public void testGetAllTranslations()
    {
        translationService.addTranslation( translation1a );
        translationService.addTranslation( translation1b );
        translationService.addTranslation( translation2a );
        translationService.addTranslation( translation2b );
        translationService.addTranslation( translation2c );

        assertEquals( 5, translationService.getAllTranslations().size() );
    }
}
