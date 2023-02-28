/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.security.action;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.i18n.ui.resourcebundle.ResourceBundleManager;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.hisp.dhis.security.oidc.DhisOidcProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mobile.device.Device;
import org.springframework.mobile.device.DeviceResolver;

import com.opensymphony.xwork2.Action;

/**
 * @author mortenoh
 */
public class LoginAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DeviceResolver deviceResolver;

    public void setDeviceResolver( DeviceResolver deviceResolver )
    {
        this.deviceResolver = deviceResolver;
    }

    @Autowired
    private ResourceBundleManager resourceBundleManager;

    @Autowired
    private DhisOidcProviderRepository repository;

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------
    private String cspNonce = "";

    public void setCspNonce( String cspNonce )
    {
        this.cspNonce = cspNonce;
    }

    public String getCspNonce()
    {
        return cspNonce;
    }

    private Boolean twoFactor = false;

    private Boolean failed = false;

    private Boolean oidcFailure = false;

    public void setFailed( Boolean failed )
    {
        this.failed = failed;
    }

    public Boolean getFailed()
    {
        return failed;
    }

    public Boolean getTwoFactor()
    {
        return twoFactor;
    }

    public void setTwoFactor( Boolean twoFactor )
    {
        this.twoFactor = twoFactor;
    }

    public Boolean getOidcFailure()
    {
        return oidcFailure;
    }

    public void setOidcFailure( Boolean oidcFailure )
    {
        this.oidcFailure = oidcFailure;
    }

    private List<Locale> availableLocales;

    public List<Locale> getAvailableLocales()
    {
        return availableLocales;
    }

    private final Map<String, Object> oidcConfig = new HashMap<>();

    public Map<String, Object> getOidcConfig()
    {
        return oidcConfig;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        addRegisteredProviders();

        Device device = deviceResolver.resolveDevice( ServletActionContext.getRequest() );
        HttpSession session = ServletActionContext.getRequest().getSession();
        String nounce = (String) session.getAttribute( "nounce" );
        setCspNonce( nounce );

        ServletActionContext.getResponse().addHeader( "Login-Page", "true" );

        if ( device.isMobile() || device.isTablet() )
        {
            return "mobile";
        }

        availableLocales = new ArrayList<>( resourceBundleManager.getAvailableLocales() );

        return "standard";
    }

    private void addRegisteredProviders()
    {
        List<Map<String, String>> providers = new ArrayList<>();

        Set<String> allRegistrationIds = repository.getAllRegistrationId();

        for ( String registrationId : allRegistrationIds )
        {
            DhisOidcClientRegistration clientRegistration = repository.getDhisOidcClientRegistration( registrationId );

            providers.add( Map.of(
                "id", registrationId,
                "icon", clientRegistration.getLoginIcon(),
                "iconPadding", clientRegistration.getLoginIconPadding(),
                "loginText", clientRegistration.getLoginText() ) );
        }

        if ( !providers.isEmpty() )
        {
            oidcConfig.put( "providers", providers );
        }
    }
}
