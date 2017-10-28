package org.hisp.dhis.webapi;

/*
 *
 *  Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.quick.StatementManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.ParameterContentNegotiationStrategy;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@Configuration
@EnableAuthorizationServer
@EnableWebSecurity
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter
{
    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private AuthorizationServerTokenServices tokenServices;

    @Autowired
    private AuthorizationCodeServices authorizationCodeServices;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ParameterContentNegotiationStrategy parameterContentNegotiationStrategy;

    @Autowired
    private StatementBuilder statementBuilder;

    @Autowired
    private StatementManager statementManager;

    @Override
    public void configure( AuthorizationServerEndpointsConfigurer endpoints ) throws Exception
    {
        initOauth2();
        endpoints
            .authenticationManager( authenticationManager )
            .tokenServices( tokenServices )
            .authorizationCodeServices( authorizationCodeServices )
            .setClientDetailsService( clientDetailsService );
    }

    @Override
    public void configure( AuthorizationServerSecurityConfigurer security ) throws Exception
    {
        security.addObjectPostProcessor( new ObjectPostProcessor<Object>()
        {
            public <T> T postProcess(T object) {
                return (T) this;
            }
        } );
        super.configure( security );
    }

    @Bean
    public ContentNegotiationStrategy contentNegotiationStrategy()
    {
        return parameterContentNegotiationStrategy;
    }


    private void initOauth2()
    {
        // OAuth2
        executeSql( "CREATE TABLE oauth_code (" +
            "  code VARCHAR(256), authentication " + statementBuilder.getLongVarBinaryType() +
            ")" );

        executeSql( "CREATE TABLE oauth_access_token (" +
            "  token_id VARCHAR(256)," +
            "  token " + statementBuilder.getLongVarBinaryType() + "," +
            "  authentication_id VARCHAR(256) PRIMARY KEY," +
            "  user_name VARCHAR(256)," +
            "  client_id VARCHAR(256)," +
            "  authentication " + statementBuilder.getLongVarBinaryType() + "," +
            "  refresh_token VARCHAR(256)" +
            ")" );

        executeSql( "CREATE TABLE oauth_refresh_token (" +
            "  token_id VARCHAR(256)," +
            "  token " + statementBuilder.getLongVarBinaryType() + "," +
            "  authentication " + statementBuilder.getLongVarBinaryType() +
            ")" );
    }

    private int executeSql( String sql )
    {
        try
        {
            return statementManager.getHolder().executeUpdate( sql );
        }
        catch ( Exception ex )
        {
            return -1;
        }
    }
}
