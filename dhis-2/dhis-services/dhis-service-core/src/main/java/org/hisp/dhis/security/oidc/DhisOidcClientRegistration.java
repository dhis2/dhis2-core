package org.hisp.dhis.security.oidc;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Data
@Builder
public class DhisOidcClientRegistration
{
    private ClientRegistration clientRegistration;

    private String mappingClaimKey;

    private String hore;

    private String registrationId;
}
