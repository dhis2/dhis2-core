package org.hisp.dhis.webapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.utils.TestUtils;
import org.hisp.dhis.webapi.json.JsonResponse;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

/**
 * Base class for convenient testing of the web API on basis of
 * {@link JsonResponse}.
 *
 * @author Jan Bernitt
 */
@RunWith( SpringRunner.class )
@WebAppConfiguration
@ContextConfiguration( classes = { MvcTestConfig.class, WebTestConfiguration.class } )
@ActiveProfiles( "test-h2" )
@Transactional
public abstract class DhisControllerConvenienceTest extends DhisConvenienceTest
{
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService _userService;

    private MockMvc mvc;

    private MockHttpSession session;

    @Before
    public final void setup()
        throws Exception
    {
        userService = _userService;
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding( "UTF-8" );
        characterEncodingFilter.setForceEncoding( true );
        mvc = MockMvcBuilders.webAppContextSetup( webApplicationContext ).build();
        TestUtils.executeStartupRoutines( webApplicationContext );
        session = startSessionWith( "ALL" );
    }

    private MockHttpSession startSessionWith( String... authorities )
    {
        createAndInjectAdminUser( authorities );

        MockHttpSession session = new MockHttpSession();
        session.setAttribute( HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext() );
        return session;
    }

    public static class Expect
    {
        private final ResultActions actions;

        public Expect( ResultActions actions )
        {
            this.actions = actions;
        }

        public JsonResponse when( HttpStatus status )
        {
            return failUnless( () -> new JsonResponse(
                actions.andExpect( status().is( status.value() ) )
                    .andReturn().getResponse().getContentAsString( StandardCharsets.UTF_8 ) ) );
        }

    }

    static <T> T failUnless( Callable<T> op )
    {
        try
        {
            return op.call();
        }
        catch ( Exception ex )
        {
            throw new AssertionError( ex );
        }
    }

    protected final Expect GET( String url )
    {
        return GET( url, new String[0] );
    }

    protected final Expect GET( String url, String... args )
    {
        String fullUrl = args.length == 0 ? url : String.format( url, (Object[]) args );
        return failUnless( () -> new Expect( mvc.perform( get( fullUrl ).session( session ) ) ) );
    }

    protected final Expect POST( String url, String body )
    {
        return null;
    }

    protected final Expect POST( String url, Path body )
    {
        return null;
    }

}
