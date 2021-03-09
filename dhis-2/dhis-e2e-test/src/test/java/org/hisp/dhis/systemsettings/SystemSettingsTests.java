package org.hisp.dhis.systemsettings;



import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.SystemSettingActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.TestCleanUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import io.restassured.http.ContentType;

/**
 * @author David Katuscak <katuscak.d@gmail.com>
 */
public class SystemSettingsTests extends ApiTest
{
    private static final String APPLICATION_INTRO_KEY = "keyApplicationIntro";
    private static final String APPLICATION_FOOTER_KEY = "keyApplicationFooter";

    private static final String MAX_SYNC_ATTEMPTS_KEY = "syncMaxAttempts";
    private static final int MAX_SYNC_ATTEMPTS_DEFAULT_VALUE = 3;
    private static final String MAX_PASSWORD_LENGTH_KEY = "maxPasswordLength";
    private static final int MAX_PASSWORD_LENGTH_DEFAULT_VALUE = 40;
    private static final String EMAIL_SENDER_KEY = "keyEmailSender";
    private static final String EMAIL_SENDER_DEFAULT_VALUE = "no-reply@dhis2.org";


    private static final String DEFAULT_FOOTER = "Learn more ...";

    private static final String DEFAULT_INTRO = "Default - Welcome to the DHIS2";
    private static final String ENGLISH_INTRO = "English - Welcome to the DHIS2";
    private static final String FRENCH_INTRO = "French - Welcome to the DHIS2";

    private LoginActions loginActions;
    private SystemSettingActions systemSettingActions;

    @BeforeEach
    public void setUp()
    {
        new TestCleanUp().deleteCreatedEntities( "/systemSettings" );

        systemSettingActions = new SystemSettingActions();

        loginActions = new LoginActions();
        loginActions.loginAsDefaultUser();
    }

    private void prepareData()
    {
        QueryParamsBuilder params = new QueryParamsBuilder();
        params.add( "value=" + DEFAULT_FOOTER );

        systemSettingActions.post(
            APPLICATION_FOOTER_KEY,
            ContentType.TEXT.toString(),
            new JsonObject(),
            params );
        // ------------------------

        params = new QueryParamsBuilder();
        params.add( "value=" + DEFAULT_INTRO );

        systemSettingActions.post(
            APPLICATION_INTRO_KEY,
            ContentType.TEXT.toString(),
            new JsonObject(),
            params );
        // ------------------------

        params = new QueryParamsBuilder();
        params.add( "value=" + ENGLISH_INTRO );
        params.add( "locale=en" );

        systemSettingActions.post(
            APPLICATION_INTRO_KEY,
            ContentType.TEXT.toString(),
            new JsonObject(),
            params );
        // ------------------------

        params = new QueryParamsBuilder();
        params.add( "value=" + FRENCH_INTRO );
        params.add( "locale=fr" );

        systemSettingActions.post(
            APPLICATION_INTRO_KEY,
            ContentType.TEXT.toString(),
            new JsonObject(),
            params );
    }

    @Test
    public void addSystemSetting()
    {
        String specificFooter = "Learn more at ";

        QueryParamsBuilder params = new QueryParamsBuilder();
        params.add( "value=" + specificFooter );

        ApiResponse response = systemSettingActions.post(
            APPLICATION_FOOTER_KEY,
            ContentType.TEXT.toString(),
            new JsonObject(), params );

        response
            .validate()
            .statusCode( 200 )
            .body( containsString( specificFooter ) );
    }

    @Test
    @Disabled("This test is broken and will only return 200 OK because the servlet redirects to the login page. //TODO: Remove")
    public void returnDefaultValueWhenUserIsNotLoggedIn()
    {
        prepareData();

        //I need to log out
        loginActions.removeAuthenticationHeader();

        ApiResponse response = systemSettingActions.get(
            APPLICATION_INTRO_KEY,
            ContentType.TEXT.toString(),
            ContentType.TEXT.toString(),
            new QueryParamsBuilder() );

        response
            .validate()
            .statusCode( 200 );
    }

    @Test
    public void returnDefaultValueWhenTranslationIsNotAvailable()
    {
        prepareData();

        ApiResponse response = systemSettingActions.get(
            APPLICATION_INTRO_KEY,
            ContentType.TEXT.toString(),
            ContentType.TEXT.toString(),
            new QueryParamsBuilder().add( "locale=pl" ) );

        response
            .validate()
            .statusCode( 200 )
            .body( containsString( DEFAULT_INTRO ) );
    }

    @Test
    public void returnTranslationForUsersLocale()
    {
        prepareData();

        ApiResponse response = systemSettingActions.get(
            APPLICATION_INTRO_KEY,
            ContentType.TEXT.toString(),
            ContentType.TEXT.toString(),
            new QueryParamsBuilder() );

        response
            .validate().log().all()
            .statusCode( 200 )
            .body( containsString( ENGLISH_INTRO ) );
    }

    @Test
    public void returnTranslationForGivenLocale()
    {
        prepareData();

        ApiResponse response = systemSettingActions.get(
            APPLICATION_INTRO_KEY,
            ContentType.TEXT.toString(),
            ContentType.TEXT.toString(),
            new QueryParamsBuilder().add( "locale=fr" ) );

        response
            .validate()
            .statusCode( 200 )
            .body( containsString( FRENCH_INTRO ) );
    }

    @Test
    public void returnAllSystemSettings()
    {
        prepareData();

        ApiResponse response = systemSettingActions.get();

        response
            .validate()
            .statusCode( 200 )
            .body( APPLICATION_INTRO_KEY, equalTo( DEFAULT_INTRO ) )
            .body( APPLICATION_FOOTER_KEY, equalTo( DEFAULT_FOOTER ) );
    }

    @Test
    public void deleteTranslationForGivenLocaleAndSettingKey()
    {
        prepareData();

        ApiResponse response = systemSettingActions.delete(
            APPLICATION_INTRO_KEY,
            new QueryParamsBuilder().add( "locale=fr" ) );

        response
            .validate()
            .statusCode( 204 );

        response = systemSettingActions.get(
            APPLICATION_INTRO_KEY,
            ContentType.TEXT.toString(),
            ContentType.TEXT.toString(),
            new QueryParamsBuilder().add( "locale=fr" ) );

        response
            .validate()
            .statusCode( 200 )
            .body( containsString( DEFAULT_INTRO ) );
    }

    @Test
    public void deleteSystemSetting()
    {
        prepareData();

        ApiResponse response = systemSettingActions.delete( APPLICATION_INTRO_KEY );

        response
            .validate()
            .statusCode( 204 );

        response = systemSettingActions.get(
            APPLICATION_INTRO_KEY,
            ContentType.TEXT.toString(),
            ContentType.TEXT.toString(),
            new QueryParamsBuilder() );

        response
            .validate()
            .statusCode( 200 );

        assertEquals( StringUtils.EMPTY, response.getAsString() );
    }

    @Test
    public void getDefaultSystemSettingAsText()
    {
        ApiResponse response = systemSettingActions.get(
            MAX_SYNC_ATTEMPTS_KEY,
            ContentType.TEXT.toString(),
            ContentType.TEXT.toString(),
            new QueryParamsBuilder() );

        response
            .validate()
            .statusCode( 200 )
            .body( containsString( String.valueOf( MAX_SYNC_ATTEMPTS_DEFAULT_VALUE ) ) );

        // -----------------------------------------
        response = systemSettingActions.get(
            MAX_PASSWORD_LENGTH_KEY,
            ContentType.TEXT.toString(),
            ContentType.TEXT.toString(),
            new QueryParamsBuilder() );

        response
            .validate()
            .statusCode( 200 )
            .body( containsString( String.valueOf( MAX_PASSWORD_LENGTH_DEFAULT_VALUE ) ) );

        // -----------------------------------------
        response = systemSettingActions.get(
            EMAIL_SENDER_KEY,
            ContentType.TEXT.toString(),
            ContentType.TEXT.toString(),
            new QueryParamsBuilder() );

        response
            .validate()
            .statusCode( 200 )
            .body( containsString( EMAIL_SENDER_DEFAULT_VALUE ) );
    }

    @Test
    public void getDefaultSystemSettingAsJson()
    {
        ApiResponse response = systemSettingActions.get(
            MAX_SYNC_ATTEMPTS_KEY,
            ContentType.JSON.toString(),
            ContentType.JSON.toString(),
            new QueryParamsBuilder() );

        response
            .validate()
            .statusCode( 200 )
            .body( containsString( String.valueOf( MAX_SYNC_ATTEMPTS_DEFAULT_VALUE ) ) );

        // -----------------------------------------
        response = systemSettingActions.get(
            MAX_PASSWORD_LENGTH_KEY,
            ContentType.JSON.toString(),
            ContentType.JSON.toString(),
            new QueryParamsBuilder() );

        response
            .validate()
            .statusCode( 200 )
            .body( containsString( String.valueOf( MAX_PASSWORD_LENGTH_DEFAULT_VALUE ) ) );

        // -----------------------------------------
        response = systemSettingActions.get(
            EMAIL_SENDER_KEY,
            ContentType.JSON.toString(),
            ContentType.JSON.toString(),
            new QueryParamsBuilder() );

        response
            .validate()
            .statusCode( 200 )
            .body( containsString( EMAIL_SENDER_DEFAULT_VALUE ) );
    }

    @Test
    public void getDefaultSystemSettingWithNonSpecifiedContentTypeAndAccept()
    {
        ApiResponse response = systemSettingActions.get(
            MAX_SYNC_ATTEMPTS_KEY,
            "",
            "",
            new QueryParamsBuilder() );

        response
            .validate()
            .statusCode( 200 )
            .body( containsString( String.valueOf( MAX_SYNC_ATTEMPTS_DEFAULT_VALUE ) ) );

        // -----------------------------------------
        response = systemSettingActions.get(
            MAX_PASSWORD_LENGTH_KEY,
            "",
            "",
            new QueryParamsBuilder() );

        response
            .validate()
            .statusCode( 200 )
            .body( containsString( String.valueOf( MAX_PASSWORD_LENGTH_DEFAULT_VALUE ) ) );

        // -----------------------------------------
        response = systemSettingActions.get(
            EMAIL_SENDER_KEY,
            "",
            "",
            new QueryParamsBuilder() );

        response
            .validate()
            .statusCode( 200 )
            .body( containsString( EMAIL_SENDER_DEFAULT_VALUE ) );
    }
}
