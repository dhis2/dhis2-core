package org.hisp.dhis.webapi.controller;

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
    public void verify_fetch_withInvalid_Key()
        throws Exception
    {

        mvc.perform( get( URL + "idontexist" ).session( session ).contentType( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andExpect( status().is( HttpStatus.SC_NOT_FOUND ) );

    }

    @Test
    public void verify_fetch_default_key()
        throws Exception
    {

        mvc.perform( get( URL + StaticContentController.LOGO_BANNER ).session( session ) )
            .andExpect( redirectedUrlPattern( "**/dhis-web-commons/css/light_blue/logo_banner.png" ) )
            .andExpect( status().is( HttpStatus.SC_MOVED_TEMPORARILY ) );

    }

    @Test
    public void verify_fetch_custom()
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
    public void verify_store_image()
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
    public void verify_error_whenStoring_invalid_Key()
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
