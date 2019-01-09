package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.apache.http.HttpStatus;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.JCloudsFileResourceContentStore;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.webapi.DhisWebSpringTest;
import org.hisp.dhis.webapi.documentation.common.TestUtils;
import org.hisp.dhis.webapi.utils.FileResourceUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.MimeTypeUtils;

/**
 * @author Luciano Fiandesio
 */
public class StaticContentControllerTest
    extends
    DhisWebSpringTest
{

    private final static String URL = "/staticContent/";

    private final static String MIME_PNG = MimeTypeUtils.IMAGE_PNG.toString();

    private MockHttpSession session;

    private MockMultipartFile mockMultipartFile;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private JCloudsFileResourceContentStore fileResourceContentStore;

    @Before
    public void setUp()
    {
        this.session = getSession( "ALL" );
        this.mockMultipartFile = new MockMultipartFile( "file", "testlogo.png", MIME_PNG, "image".getBytes() );
        systemSettingManager.saveSystemSetting( SettingKey.USE_CUSTOM_LOGO_BANNER, Boolean.FALSE );

    }

    @Test
    public void verifyFetchWithInvalidKey()
        throws Exception
    {

        mvc.perform( get( URL + "idontexist" ).session( session ).contentType( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andExpect( status().is( HttpStatus.SC_NOT_FOUND ) );

    }

    @Test
    public void verifyFetchWithDefaultKey()
        throws Exception
    {

        mvc.perform( get( URL + StaticContentController.LOGO_BANNER ).session( session ) )
            .andExpect( redirectedUrlPattern( "**/dhis-web-commons/css/light_blue/logo_banner.png" ) )
            .andExpect( status().is( HttpStatus.SC_MOVED_TEMPORARILY ) );

    }

    @Test
    public void verifyFetchCustom()
        throws Exception
    {
        // store a mock file to the content store, before fetching it
        fileResourceContentStore.saveFileResourceContent( FileResourceUtils.build( StaticContentController.LOGO_BANNER,
            mockMultipartFile, FileResourceDomain.DOCUMENT ), "image".getBytes() );

        systemSettingManager.saveSystemSetting( SettingKey.USE_CUSTOM_LOGO_BANNER, Boolean.TRUE );

        mvc.perform( get( URL + StaticContentController.LOGO_BANNER ).session( session ) )
            .andExpect( content().contentType( MIME_PNG ) ).andExpect( content().bytes( mockMultipartFile.getBytes() ) )
            .andExpect( status().is( HttpStatus.SC_OK ) );
    }

    @Test
    public void verifyStoreImage()
        throws Exception
    {
        mvc.perform(
            fileUpload( URL + StaticContentController.LOGO_BANNER ).file( mockMultipartFile ).session( session ) )
            .andExpect( status().is( HttpStatus.SC_NO_CONTENT ) );
    }

    @Test
    public void verify_error_whenStoring_invalid_MimeType()
        throws Exception
    {
        final String error = buildResponse( "Unsupported Media Type", 415, "WARNING", null );

        mvc.perform( fileUpload( URL + StaticContentController.LOGO_BANNER ).file(
            new MockMultipartFile( "file", "testlogo.png", MimeTypeUtils.IMAGE_JPEG.toString(), "image".getBytes() ) )
            .session( session ) ).andExpect( content().json( error ) )
            .andExpect( status().is( HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE ) );
    }

    @Test
    public void verifyErrorWhenStoringInvalidKey()
        throws Exception
    {
        final String error = buildResponse( "Bad Request", 400, "ERROR", "This key is not supported." );

        mvc.perform( fileUpload( URL + "idontexist" ).file( mockMultipartFile ).session( session ) )
            .andExpect( content().json( error ) ).andExpect( status().is( HttpStatus.SC_BAD_REQUEST ) );
    }

    private String buildResponse( String httpStatus, int code, String status, String message )
        throws Exception
    {
        return new JSONObject().put( "httpStatus", httpStatus ).put( "httpStatusCode", code ).put( "status", status )
            .putOpt( "message", message ).toString();
    }

}