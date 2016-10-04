package org.hisp.dhis.translation;

/*
 *
 *  Copyright (c) 2004-2016, University of Oslo
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *  Neither the name of the HISP project nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UserContext;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingKey;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class TranslationServiceTest
    extends DhisSpringTest
{
    @Autowired
    private UserService injectUserService;

    @Autowired
    private IdentifiableObjectManager identifiableObjectManager;

    private User user;

    @Override
    public void setUpTest() {
        this.userService = injectUserService;
        user = createUserAndInjectSecurityContext( true );
    }

    @Test
    public void testOK()
    {
        Locale locale = Locale.FRENCH;
        UserContext.setUser( user );
        UserContext.setUserSetting( UserSettingKey.DB_LOCALE, locale );

        DataElement dataElementA = createDataElement( 'A' );
        identifiableObjectManager.save( dataElementA );

        String translatedValue = "translated";

        Set<ObjectTranslation> listObjectTranslation = new HashSet<>( dataElementA.getTranslations() );

        listObjectTranslation.add( new ObjectTranslation( locale.getLanguage(), TranslationProperty.NAME, translatedValue ) );

        identifiableObjectManager.updateTranslations( dataElementA, listObjectTranslation );

        assertEquals( translatedValue, dataElementA.getDisplayName() );
    }
}
