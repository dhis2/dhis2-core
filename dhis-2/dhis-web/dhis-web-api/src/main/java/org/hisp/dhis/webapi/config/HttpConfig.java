package org.hisp.dhis.webapi.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.hisp.dhis.common.Compression;
import org.hisp.dhis.node.DefaultNodeService;
import org.hisp.dhis.node.NodeService;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.security.oauth2.DefaultClientDetailsUserDetailsService;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetailsSource;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.handler.CustomExceptionMappingAuthenticationFailureHandler;
import org.hisp.dhis.webapi.handler.DefaultAuthenticationSuccessHandler;
import org.hisp.dhis.webapi.security.Http401LoginUrlAuthenticationEntryPoint;
import org.hisp.dhis.webapi.vote.ActionAccessVoter;
import org.hisp.dhis.webapi.vote.ExternalAccessVoter;
import org.hisp.dhis.webapi.vote.LogicalOrAccessDecisionManager;
import org.hisp.dhis.webapi.vote.ModuleAccessVoter;
import org.hisp.dhis.webapi.vote.SimpleAccessVoter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.access.vote.UnanimousBased;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.access.expression.WebExpressionVoter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Configuration
@ComponentScan( basePackages = { "org.hisp.dhis" } )
//, useDefaultFilters = false, includeFilters = {
//    @ComponentScan.Filter( type = FilterType.ANNOTATION, value = Controller.class ),
//    @ComponentScan.Filter( type = FilterType.ANNOTATION, value = Service.class ),
//    @ComponentScan.Filter( type = FilterType.ANNOTATION, value = Component.class ),
//    @ComponentScan.Filter( type = FilterType.ANNOTATION, value = Repository.class )
//
//} )
@EnableWebSecurity
@EnableWebMvc
//, excludeFilters = @ComponentScan.Filter( Configuration.class ) )
//@Import( {
//    JdbcConfig.class,
//    HibernateConfig.class,
//    FlywayConfig.class,
//    EncryptionConfig.class,
//    ServiceConfig.class,
//    StoreConfig.class,
//    LeaderElectionConfiguration.class,
//    org.hisp.dhis.setting.config.ServiceConfig.class,
//    org.hisp.dhis.external.config.ServiceConfig.class,
//    org.hisp.dhis.dxf2.config.ServiceConfig.class,
//    org.hisp.dhis.support.config.ServiceConfig.class,
//    org.hisp.dhis.validation.config.ServiceConfig.class,
//    org.hisp.dhis.validation.config.StoreConfig.class,
//    org.hisp.dhis.programrule.config.ProgramRuleConfig.class,
//    org.hisp.dhis.reporting.config.StoreConfig.class,
//    org.hisp.dhis.analytics.config.ServiceConfig.class,
//    org.hisp.dhis.commons.config.JacksonObjectMapperConfig.class } )
@EnableGlobalMethodSecurity( prePostEnabled = true )
public class HttpConfig extends WebSecurityConfigurerAdapter implements WebMvcConfigurer
{
//    public HttpConfig()
//    {
//        super( true );
//    }

    @Autowired
    private DefaultClientDetailsUserDetailsService defaultClientDetailsUserDetailsService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private SecurityService securityService;

//    @Autowired
//    private CustomLdapAuthenticationProvider customLdapAuthenticationProvider;

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler()
    {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setDefaultRolePrefix( "" );
        return expressionHandler;
    }

    @Bean
    public PasswordEncoder encoder()
    {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    public void configureGlobal( AuthenticationManagerBuilder auth )
        throws Exception
    {
        TwoFactorAuthenticationProvider twoFactorAuthenticationProvider = new TwoFactorAuthenticationProvider();
        twoFactorAuthenticationProvider.setPasswordEncoder( encoder() );
        twoFactorAuthenticationProvider.setUserService( userService );
        twoFactorAuthenticationProvider.setUserDetailsService( userDetailsService );
        twoFactorAuthenticationProvider.setSecurityService( securityService );

        // configure the Authentication providers

        auth
            // Two factor
            .authenticationProvider( twoFactorAuthenticationProvider )
            // LDAP Authentication
//            .authenticationProvider( customLdapAuthenticationProvider )
            //  OAUTH2
            .userDetailsService( defaultClientDetailsUserDetailsService )
            // Use a non-encoding password for oauth2 secrets, since the secret is generated by the client
            .passwordEncoder( NoOpPasswordEncoder.getInstance() );
    }

//    @Bean( "authenticationManager" )
//    public AuthenticationManager authenticationManager( AuthenticationManagerBuilder auth )
//    {
//        return auth.getOrBuild();
//    }
//        httpSecurity.cors().and().csrf().disable();

    @Bean
    public CustomExceptionMappingAuthenticationFailureHandler authenticationFailureHandler()
    {
        CustomExceptionMappingAuthenticationFailureHandler handler =
            new CustomExceptionMappingAuthenticationFailureHandler();

        handler.setExceptionMappings(
            ImmutableMap.of(
                "org.springframework.security.authentication.CredentialsExpiredException",
                "/dhis-web-commons/security/expired.action" ) );

        handler.setDefaultFailureUrl( "/dhis-web-commons/security/login.action?failed=true" );

        return handler;
    }

    @Bean
    public DefaultAuthenticationSuccessHandler authenticationSuccessHandler()
    {
        return new DefaultAuthenticationSuccessHandler();
    }

    @Bean
    public TwoFactorWebAuthenticationDetailsSource twoFactorWebAuthenticationDetailsSource()
    {
        return new TwoFactorWebAuthenticationDetailsSource();
    }

    @Bean
    public ModuleAccessVoter moduleAccessVoter()
    {
        ModuleAccessVoter v = new ModuleAccessVoter();
        v.setAttributePrefix( "M_" );
        v.setAlwaysAccessible( ImmutableSet.of(
            "dhis-web-commons-menu",
            "dhis-web-commons-oust",
            "dhis-web-commons-ouwt",
            "dhis-web-commons-security",
            "dhis-web-commons-i18n",
            "dhis-web-commons-ajax",
            "dhis-web-commons-ajax-json",
            "dhis-web-commons-ajax-html",
            "dhis-web-commons-stream",
            "dhis-web-commons-help",
            "dhis-web-commons-about",
            "dhis-web-menu-management",
            "dhis-web-apps",
            "dhis-web-api-mobile",
            "dhis-web-portal",
            "dhis-web-uaa"
        ) );
        return v;
    }

    @Bean
    public ActionAccessVoter actionAccessVoter()
    {
        ActionAccessVoter v = new ActionAccessVoter();
        v.setAttributePrefix( "F_" );
        v.setRequiredAuthoritiesKey( "requiredAuthorities" );
        v.setAnyAuthoritiesKey( "anyAuthorities" );
        return v;
    }

    @Bean
    public WebExpressionVoter webExpressionVoter()
    {
        DefaultWebSecurityExpressionHandler h = new DefaultWebSecurityExpressionHandler();
        h.setDefaultRolePrefix( "" );
        WebExpressionVoter v = new WebExpressionVoter();
        v.setExpressionHandler( h );
        return v;
    }

    @Bean
    public LogicalOrAccessDecisionManager accessDecisionManager()
    {
        List<AccessDecisionManager> decisionVoters = Arrays.asList(
            new UnanimousBased( ImmutableList.of( new SimpleAccessVoter( "ALL" ) ) ),
            new UnanimousBased( ImmutableList.of( actionAccessVoter(), moduleAccessVoter() ) ),
            new UnanimousBased( ImmutableList.of( webExpressionVoter() ) ),
            new UnanimousBased( ImmutableList.of( new ExternalAccessVoter() ) ),
            new UnanimousBased( ImmutableList.of( new AuthenticatedVoter() ) )
        );
        return new LogicalOrAccessDecisionManager( decisionVoters );
    }

//
//    <sec:intercept-url pattern="/api/account/username" access="permitAll()" />
//    <sec:intercept-url pattern="/api/account/recovery" access="permitAll()" />
//    <sec:intercept-url pattern="/api/account/restore" access="permitAll()" />
//    <sec:intercept-url pattern="/api/account/password" access="permitAll()" />
//    <sec:intercept-url pattern="/api/account/validatePassword" method="POST" access="permitAll()" />
//    <sec:intercept-url pattern="/api/account/validateUsername" method="POST" access="permitAll()" />
//    <sec:intercept-url pattern="/api/account" access="permitAll()" />
//    <sec:intercept-url pattern="/api/staticContent/*" method="GET" access="permitAll()" />
//    <sec:intercept-url pattern="/api/externalFileResources/*" method="GET" access="permitAll()" />
//    <sec:intercept-url pattern="/api/icons/*/icon.svg" method="GET" access="permitAll()" />
//    <sec:intercept-url request-matcher-ref="analyticPluginResources" access="permitAll()" />
//

    @Bean
    public SessionRegistryImpl sessionRegistry()
    {
        return new org.springframework.security.core.session.SessionRegistryImpl();
    }

    @Bean
    public HttpFirewall allowUrlEncodedSlashHttpFirewall()
    {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedSlash( true );
        firewall.setAllowSemicolon( true );
        return firewall;
    }

//    @Override
//    public void configure( WebSecurity web )
//        throws Exception
//    {
//        super.configure( web );
//        web
//            .httpFirewall( allowUrlEncodedSlashHttpFirewall() )
//
//            .ignoring()
//            .antMatchers( "/resources/**" )
//            .antMatchers( "/publics/**" );
//    }

//
//  <bean id="jsonMessageConverter" class="org.hisp.dhis.webapi.mvc.messageconverter.JsonMessageConverter">
//    <constructor-arg name="compression" value="NONE" />
//  </bean>
//
//  <bean id="jsonMessageConverterGZIP" class="org.hisp.dhis.webapi.mvc.messageconverter.JsonMessageConverter">
//    <constructor-arg name="compression" value="GZIP" />
//  </bean>
//
//  <bean id="jsonMessageConverterZIP" class="org.hisp.dhis.webapi.mvc.messageconverter.JsonMessageConverter">
//    <constructor-arg name="compression" value="ZIP" />
//  </bean>
//
//  <bean id="xmlMessageConverter" class="org.hisp.dhis.webapi.mvc.messageconverter.XmlMessageConverter">
//    <constructor-arg name="compression" value="NONE" />
//  </bean>
//
//  <bean id="xmlMessageConverterGZIP" class="org.hisp.dhis.webapi.mvc.messageconverter.XmlMessageConverter">
//    <constructor-arg name="compression" value="GZIP" />
//  </bean>
//
//  <bean id="xmlMessageConverterZIP" class="org.hisp.dhis.webapi.mvc.messageconverter.XmlMessageConverter">
//    <constructor-arg name="compression" value="ZIP" />
//  </bean>
//
//  <bean id="csvMessageConverter" class="org.hisp.dhis.webapi.mvc.messageconverter.CsvMessageConverter">
//    <constructor-arg name="compression" value="NONE" />
//  </bean>
//
//  <bean id="csvMessageConverterGZIP" class="org.hisp.dhis.webapi.mvc.messageconverter.CsvMessageConverter">
//    <constructor-arg name="compression" value="GZIP" />
//  </bean>
//
//  <bean id="csvMessageConverterZIP" class="org.hisp.dhis.webapi.mvc.messageconverter.CsvMessageConverter">
//    <constructor-arg name="compression" value="ZIP" />
//  </bean>
//    <bean id="jsonPMessageConverter" class="org.hisp.dhis.webapi.mvc.messageconverter.JsonPMessageConverter" />
//
//  <bean id="stringHttpMessageConverter" class="org.springframework.http.converter.StringHttpMessageConverter" />
//
//  <bean id="byteArrayHttpMessageConverter" class="org.springframework.http.converter.ByteArrayHttpMessageConverter" />
//
//  <bean id="formHttpMessageConverter" class="org.springframework.http.converter.FormHttpMessageConverter" />
//
//  <bean id="responseStatusExceptionResolver" class="org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver" />
//
//  <bean id="defaultHandlerExceptionResolver" class="org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver" />

    @Bean
    public NodeService nodeService()
    {
        return new DefaultNodeService();
    }

    @Override
    public void configureMessageConverters(
        List<HttpMessageConverter<?>> converters )
    {

        converters.add(
            new org.hisp.dhis.webapi.mvc.messageconverter.JsonMessageConverter( nodeService(), Compression.NONE ) );
//        converters.add(new org.hisp.dhis.webapi.mvc.messageconverter.JsonMessageConverter(nodeService(), Compression.NONE));

    }

    protected void configure( HttpSecurity http )
        throws Exception
    {
        http.authorizeRequests().antMatchers( "/api/**" )
            .hasAuthority( "ALL" ).and()
            .httpBasic().and().csrf().disable();

//        http
//            .antMatcher("/api/**")
//        .authorizeRequests()
//        .anyRequest().hasRole("ALL")
//        .and()
//        .httpBasic()
//            .and().csrf().disable();
//            .authenticationEntryPoint( new BasicAuthenticationEntryPoint() );

        //http.addFilterAfter( new CustomFilter(), BasicAuthenticationFilter.class );
    }

//    protected void configure( HttpSecurity httpSecurity )
//        throws Exception
//    {
//        httpSecurity.
////            authenticationProvider(  )
////    csrf().disable().
////
////            sessionManagement()
////            .sessionCreationPolicy( SessionCreationPolicy.ALWAYS )
////
////            .enableSessionUrlRewriting( false )
////            .maximumSessions( 10 )
////            .expiredUrl( "/dhis-web-commons-security/logout.action" )
//////            .sessionRegistry( sessionRegistry() )
////            .and()
////
////            .and()
//    formLogin()
//            .loginPage( "/dhis-web-commons/security/login.action" )
//            .loginProcessingUrl( "/dhis-web-commons-security/login.action" )
//
//            .authenticationDetailsSource( twoFactorWebAuthenticationDetailsSource() )
//            .successHandler( authenticationSuccessHandler() )
//            .failureHandler( authenticationFailureHandler() )
////            .defaultSuccessUrl( "/dashboard/welcome.html" )
//            .usernameParameter( "j_username" ).passwordParameter( "j_password" )
//            .and()
//            .logout()
//            .logoutUrl( "/dhis-web-commons-security/logout.action" )
//            .logoutSuccessUrl( "/" )
//            .deleteCookies( "JSESSIONID" )
//
//            .and()
//            .csrf().disable()
//
//            .authorizeRequests()
//            .accessDecisionManager( accessDecisionManager() )
//
//            .antMatchers( "/api/account/username" ).permitAll()
//            .antMatchers( "/api/account/recovery" ).permitAll()
//            .antMatchers( "/api/account/restore" ).permitAll()
//            .antMatchers( "/api/account/password" ).permitAll()
//            .antMatchers( "/api/account/validatePassword" ).permitAll()
//            .antMatchers( "/api/account/validateUsername" ).permitAll()
//            .antMatchers( "/api/account" ).permitAll()
//            .antMatchers( "/api/staticContent/*" ).permitAll()
//            .antMatchers( "/api/externalFileResources/*" ).permitAll()
//            .antMatchers( "/api/icons/*/icon.svg" ).permitAll()
//            .requestMatchers( analyticsPluginResources() ).permitAll()
//
//            .antMatchers( "/dhis-web-dashboard/**" ).hasAnyRole( "ALL", "M_dhis-web-dashboard" )
//            .antMatchers( "/dhis-web-pivot/**" ).hasAnyRole( "ALL", "M_dhis-web-pivot" )
//            .antMatchers( "/dhis-web-visualizer/**" ).hasAnyRole( "ALL", "M_dhis-web-visualizer" )
//            .antMatchers( "/dhis-web-data-visualizer/**" ).hasAnyRole( "ALL", "M_dhis-web-data-visualizer" )
//            .antMatchers( "/dhis-web-mapping/**" ).hasAnyRole( "ALL", "M_dhis-web-mapping" )
//            .antMatchers( "/dhis-web-maps/**" ).hasAnyRole( "ALL", "M_dhis-web-maps" )
//            .antMatchers( "/dhis-web-event-reports/**" ).hasAnyRole( "ALL", "M_dhis-web-event-reports" )
//            .antMatchers( "/dhis-web-event-visualizer/**" ).hasAnyRole( "ALL", "M_dhis-web-event-visualizer" )
//            .antMatchers( "/dhis-web-interpretation/**" ).hasAnyRole( "ALL", "M_dhis-web-interpretation" )
//            .antMatchers( "/dhis-web-settings/**" ).hasAnyRole( "ALL", "M_dhis-web-settings" )
//            .antMatchers( "/dhis-web-maintenance/**" ).hasAnyRole( "ALL", "M_dhis-web-maintenance" )
//            .antMatchers( "/dhis-web-app-management/**" ).hasAnyRole( "ALL", "M_dhis-web-app-management" )
//            .antMatchers( "/dhis-web-usage-analytics/**" ).hasAnyRole( "ALL", "M_dhis-web-usage-analytics" )
//            .antMatchers( "/dhis-web-event-capture/**" ).hasAnyRole( "ALL", "M_dhis-web-event-capture" )
//            .antMatchers( "/dhis-web-tracker-capture/**" ).hasAnyRole( "ALL", "M_dhis-web-tracker-capture" )
//            .antMatchers( "/dhis-web-cache-cleaner/**" ).hasAnyRole( "ALL", "M_dhis-web-cache-cleaner" )
//            .antMatchers( "/dhis-web-data-administration/**" ).hasAnyRole( "ALL", "M_dhis-web-data-administration" )
//            .antMatchers( "/dhis-web-data-quality/**" ).hasAnyRole( "ALL", "M_dhis-web-data-quality" )
//            .antMatchers( "/dhis-web-messaging/**" ).hasAnyRole( "ALL", "M_dhis-web-messaging" )
//            .antMatchers( "/dhis-web-datastore/**" ).hasAnyRole( "ALL", "M_dhis-web-datastore" )
//            .antMatchers( "/dhis-web-scheduler/**" ).hasAnyRole( "ALL", "M_dhis-web-scheduler" )
//            .antMatchers( "/dhis-web-user/**" ).hasAnyRole( "ALL", "M_dhis-web-user" )
//
//            .antMatchers( "/**" ).authenticated()
//
//
//            .and()
//
//
//            .exceptionHandling()
//            .authenticationEntryPoint( entryPoint() )
//
//            .accessDeniedPage( "/dashboard/accessDenied.html" )
//            .and()
//
//            .httpBasic()
//            .authenticationEntryPoint( new BasicAuthenticationEntryPoint() )
//            .authenticationDetailsSource( new HttpBasicWebAuthenticationDetailsSource() );
//    }

    private RequestMatcher analyticsPluginResources()
    {
//        <bean id="analyticPluginResources" class="org.springframework.security.web.util.matcher.RegexRequestMatcher">
//    <constructor-arg name="pattern"
//        value=".*(dhis-web-mapping\/map.js|dhis-web-visualizer\/chart.js|dhis-web-maps\/map.js|dhis-web-event-reports\/eventreport.js|dhis-web-event-visualizer\/eventchart.js|dhis-web-pivot\/reporttable.js)" />
//    <constructor-arg name="httpMethod" value="GET" />
//  </bean>

        String pattern = ".*(dhis-web-mapping\\/map.js|dhis-web-visualizer\\/chart.js|dhis-web-maps\\/map.js|dhis-web-event-reports\\/eventreport.js|dhis-web-event-visualizer\\/eventchart.js|dhis-web-pivot\\/reporttable.js)";

        return new org.springframework.security.web.util.matcher.RegexRequestMatcher( pattern, "GET" );
    }

    @Bean
    public Http401LoginUrlAuthenticationEntryPoint entryPoint()
    {
        return new Http401LoginUrlAuthenticationEntryPoint( "/dhis-web-commons/security/login.action" );
    }

//extends WebSecurityConfigurerAdapter
//    @Autowired
//    public void configureGlobal( AuthenticationManagerBuilder auth)
//        throws Exception
//    {
//        auth
//            .inMemoryAuthentication()
//            .withUser("user")  // #1
//            .password("password")
//            .roles("USER")
//            .and()
//            .withUser("admin") // #2
//            .password("password")
//            .roles("ADMIN","USER");
//    }

//    @Override
//    public void configure( WebSecurity web) throws Exception {
//        web
//            .ignoring()
//            .antMatchers("/resources/**"); // #3
//    }

//    @Override
//    protected void configure( HttpSecurity http )
//        throws Exception
//    {
//
//
//        http.authorizeRequests()
//            .antMatchers( "/superadmin/**" ).access( "hasRole('ROLE_SUPER_ADMIN')" )
//            .antMatchers( "/admin/**" ).access( "hasRole('ROLE_SUPER_ADMIN') or hasRole('ROLE_ADMIN')" )
//            .antMatchers( "/employee/**" )
//            .access( "hasRole('ROLE_SUPER_ADMIN') or hasRole('ROLE_ADMIN') or hasRole('ROLE_EMPLOYEE')" )
//            .and()
//            .formLogin()
//            .loginPage( "/dashboard/login.html" )
//            .loginProcessingUrl( "/dashboard/process-login.html" )
//            .defaultSuccessUrl( "/dashboard/welcome.html" )
//            .failureUrl( "/dashboard/login.html?error" )
//            .usernameParameter( "username" ).passwordParameter( "password" )
//            .and()
//            .logout()
//            .logoutUrl( "/dashboard/logout.html" )
//            .logoutSuccessUrl( "/dashboard/login.html?logout" ).and()
//            .exceptionHandling()
//            .accessDeniedPage( "/dashboard/accessDenied.html" );
//
////        http
////            .authorizeUrls()
////            .antMatchers("/signup","/about").permitAll() // #4
////            .antMatchers("/admin/**").hasRole("ADMIN") // #6
////            .anyRequest().authenticated() // 7
////            .and()
////            .formLogin()  // #8
////            .loginUrl("/login") // #9
////            .permitAll(); // #5
//    }
}
