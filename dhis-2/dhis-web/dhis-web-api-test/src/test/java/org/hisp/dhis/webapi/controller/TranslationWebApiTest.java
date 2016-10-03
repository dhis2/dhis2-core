package org.hisp.dhis.webapi.controller;

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

import org.apache.http.HttpStatus;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.translation.ObjectTranslation;
import org.hisp.dhis.translation.TranslationProperty;
import org.hisp.dhis.webapi.DhisWebSpringTest;
import org.hisp.dhis.webapi.documentation.common.TestUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Locale;

import static junit.framework.TestCase.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */


public class TranslationWebApiTest
    extends DhisWebSpringTest
{

    @Autowired
    private IdentifiableObjectManager identifiableObjectManager;

    @Test
    public void testOK() throws Exception
    {
        Locale locale = Locale.FRENCH;

        MockHttpSession session = getSession( "ALL" );

        DataElement dataElementA = createDataElement( 'A' );

        identifiableObjectManager.save( dataElementA );

        String valueToCheck = "frenchTranslated";

        dataElementA.getTranslations().add( new ObjectTranslation( locale.getLanguage(), TranslationProperty.NAME, valueToCheck ) );

        mvc.perform( put( "/dataElements/" + dataElementA.getUid() + "/translations" )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 )
            .content( TestUtils.convertObjectToJsonBytes( dataElementA ) ) )
            .andExpect( status().is( HttpStatus.SC_NO_CONTENT ) );

        MvcResult result = mvc.perform( get( "/dataElements/" + dataElementA.getUid() +"?locale=" + locale.getLanguage() ).session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 ) ).andReturn();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree( result.getResponse().getContentAsString() );

        assertEquals( valueToCheck,  node.get( "displayName" ).asText() );

    }
}
