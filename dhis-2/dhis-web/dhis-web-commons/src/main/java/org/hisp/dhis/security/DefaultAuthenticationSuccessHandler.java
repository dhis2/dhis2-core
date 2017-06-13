package org.hisp.dhis.security;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.intercept.LoginInterceptor;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.ObjectUtils;
import org.joda.time.DateTimeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.util.Assert;

/**
 * Since ActionContext is not available at this point, we set a mark in the
 * session that signals that login has just occurred, and that LoginInterceptor
 * should be run.
 *
 * @author mortenoh
 */
public class DefaultAuthenticationSuccessHandler
    extends SavedRequestAwareAuthenticationSuccessHandler
{
    private static final Log log = LogFactory.getLog( DefaultAuthenticationSuccessHandler.class );
    
    private static final int SESSION_MIN = DateTimeConstants.SECONDS_PER_MINUTE * 10;
    private static final int SESSION_DEFAULT = Integer.parseInt( ConfigurationKey.SYSTEM_SESSION_TIMEOUT.getDefaultValue() ); // 3600 s
    private static final String SESSION_MIN_MSG = "Session timeout must be greater than %d seconds";
    private static final String SESSION_INFO_MSG = "Session timeout set to %d seconds";
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SecurityService securityService;

    @Autowired
    private DhisConfigurationProvider config;
    
    private int systemSessionTimeout;
    
    /**
     * Configurable session timeout.
     */
    private Integer sessionTimeout;
    
    public void setSessionTimeout( Integer sessionTimeout )
    {
        this.sessionTimeout = sessionTimeout;
    }
    
    @PostConstruct
    public void init()
    {
        systemSessionTimeout = ObjectUtils.firstNonNull( sessionTimeout, SESSION_DEFAULT );
        
        Assert.isTrue( systemSessionTimeout >= SESSION_MIN, String.format( SESSION_MIN_MSG, SESSION_MIN ) );
        
        log.info( String.format( SESSION_INFO_MSG, systemSessionTimeout ) );
    }

    @Override
    public void onAuthenticationSuccess( HttpServletRequest request, HttpServletResponse response, Authentication authentication )
        throws ServletException, IOException
    {   
        HttpSession session = request.getSession();
        
        final String username = authentication.getName();
        
        session.setAttribute( "userIs", username );
        session.setAttribute( LoginInterceptor.JLI_SESSION_VARIABLE, Boolean.TRUE );
        session.setMaxInactiveInterval( systemSessionTimeout );

        UserCredentials credentials = userService.getUserCredentialsByUsername( username );

        boolean readOnly = config.isReadOnlyMode();
        
        if ( credentials != null && !readOnly )
        {
            credentials.updateLastLogin();
            userService.updateUserCredentials( credentials );            
        }
        
        if ( credentials != null )
        {
            securityService.registerSuccessfulLogin( username );
        }
        
        super.onAuthenticationSuccess( request, response, authentication );
    }
}
