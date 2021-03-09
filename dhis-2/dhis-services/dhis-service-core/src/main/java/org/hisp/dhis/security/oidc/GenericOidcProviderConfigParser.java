/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.security.oidc;

import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.AUTHORIZATION_URI;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.CLIENT_ID;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.CLIENT_SECRET;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.DISPLAY_ALIAS;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.ENABLE_LOGOUT;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.ENABLE_PKCE;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.END_SESSION_ENDPOINT;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.EXTRA_REQUEST_PARAMETERS;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.JWK_URI;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.LOGO_IMAGE;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.LOGO_IMAGE_PADDING;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.MAPPING_CLAIM;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.PROVIDER_ID;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.REDIRECT_URL;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.SCOPES;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.TOKEN_URI;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.USERINFO_URI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.validator.routines.UrlValidator;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Parses the DHIS.conf file for valid generic OIDC provider configuration(s).
 * See the DHIS2 manual for how to configure OIDC providers correctly.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public final class GenericOidcProviderConfigParser
{
    public static final String OIDC_PROVIDER_PREFIX = "oidc.provider.";

    // OIDC provider config properties lines starting with these client names
    // will be ignored by this parser,
    // these clients/providers have their own respective provider classes and
    // config parsers.
    private static final Set<String> RESERVED_PROVIDER_IDS = ImmutableSet.of(
        "azure",
        "google",
        "wso2" );

    private static final ImmutableMap<String, Boolean> KEY_REQUIRED_MAP;

    static
    {
        ImmutableMap.Builder<String, Boolean> builder = ImmutableMap.builder();

        builder.put( CLIENT_ID, Boolean.TRUE );
        builder.put( CLIENT_SECRET, Boolean.TRUE );
        builder.put( AUTHORIZATION_URI, Boolean.TRUE );
        builder.put( TOKEN_URI, Boolean.TRUE );
        builder.put( USERINFO_URI, Boolean.TRUE );
        builder.put( JWK_URI, Boolean.TRUE );

        builder.put( REDIRECT_URL, Boolean.FALSE );
        builder.put( ENABLE_LOGOUT, Boolean.FALSE );
        builder.put( ENABLE_PKCE, Boolean.FALSE );
        builder.put( DISPLAY_ALIAS, Boolean.FALSE );
        builder.put( MAPPING_CLAIM, Boolean.FALSE );
        builder.put( END_SESSION_ENDPOINT, Boolean.FALSE );
        builder.put( SCOPES, Boolean.FALSE );
        builder.put( LOGO_IMAGE, Boolean.FALSE );
        builder.put( LOGO_IMAGE_PADDING, Boolean.FALSE );
        builder.put( EXTRA_REQUEST_PARAMETERS, Boolean.FALSE );

        KEY_REQUIRED_MAP = builder.build();
    }

    private static final Set<String> VALID_KEY_NAMES = KEY_REQUIRED_MAP.keySet();

    /**
     * Parses the DHIS.conf file for valid OIDC provider configuration(s). See
     * the DHIS2 manual for how to configure a OIDC provider correctly.
     *
     * @param properties The config file Properties object
     * @return A List of maps for each successfully parsed provider with
     *         corresponding key/values, the valid configuration property keys
     *         are defined in
     *         {@link org.hisp.dhis.security.oidc.provider.AbstractOidcProvider}
     */
    public static List<Map<String, String>> parse( Properties properties )
    {
        Objects.requireNonNull( properties, "Properties argument must not be NULL!" );

        List<Map.Entry<Object, Object>> allOidcProviderConfigKeys = properties.entrySet().stream()
            .filter( o -> ((String) o.getKey()).startsWith( OIDC_PROVIDER_PREFIX ) ).collect(
                Collectors.toList() );

        Map<String, Set<String>> providerAndKeysMap = allOidcProviderConfigKeys.stream()
            .map( x -> ((String) x.getKey()).split( "\\." ) )
            .collect( Collectors.groupingBy(
                x -> x[2],
                Collectors.mapping(
                    x -> String.join( ".", Arrays.copyOfRange( x, 3, x.length ) ),
                    Collectors.toSet() ) ) );

        List<Map<String, String>> allProviders = new ArrayList<>();

        for ( String providerId : providerAndKeysMap.keySet() )
        {
            // Don't parse the default/reserved OIDC client/provider names
            if ( RESERVED_PROVIDER_IDS.contains( providerId ) )
            {
                continue;
            }

            Set<String> configKeys = providerAndKeysMap.get( providerId );

            if ( !validateKeyNames( configKeys, providerId ) )
            {
                continue;
            }

            Map<String, String> providerConfig = new HashMap<>();
            providerConfig.put( PROVIDER_ID, providerId );

            for ( String key : configKeys )
            {
                String configKey = OIDC_PROVIDER_PREFIX + providerId + "." + key;
                String configValue = properties.getProperty( configKey );

                providerConfig.put( key, configValue );
            }

            if ( validateRequiredProperties( providerConfig ) )
            {
                allProviders.add( providerConfig );
            }
        }

        return allProviders;
    }

    /**
     * Makes sure that all required properties are present in the providerConfig
     * map
     *
     * @param providerConfig map of config
     * @return valid or not valid
     */
    private static boolean validateRequiredProperties( Map<String, String> providerConfig )
    {
        String providerId = providerConfig.get( PROVIDER_ID );

        for ( String key : KEY_REQUIRED_MAP.keySet() )
        {
            String value = providerConfig.get( key );

            if ( KEY_REQUIRED_MAP.get( key ) && StringUtils.isEmpty( value ) )
            {
                log.error( String.format(
                    "OpenId Connect (OIDC) configuration for provider: '%s' is missing a required property: '%s'. " +
                        "Failed to configure the provider successfully!",
                    providerId, key ) );

                return false;
            }

            if ( key.endsWith( "uri" ) && !UrlValidator.getInstance().isValid( value ) )
            {
                log.error( String.format(
                    "OpenId Connect (OIDC) configuration for provider: '%s' has a required property: '%s', " +
                        "with a malformed URI: '%s'. Failed to configure the provider successfully!",
                    providerId, key, value ) );

                return false;
            }
        }

        return true;
    }

    /**
     * Validates that all properties have valid names.
     *
     * @param configKeys map of config
     * @param providerId id of provider
     *
     * @return valid or not valid
     */
    private static boolean validateKeyNames( Set<String> configKeys, String providerId )
    {
        Sets.SetView<String> differences = Sets.difference( configKeys, VALID_KEY_NAMES );

        checkKeyNamesForDifferences( providerId, differences );

        if ( !differences.isEmpty() )
        {
            log.error(
                String.format(
                    "OpenID Connect (OIDC) configuration for provider: '%s' contains one or more invalid properties. " +
                        "Failed to configure the provider successfully! " +
                        "See previous errors for more information on what property that failed!",
                    providerId ) );

            return false;
        }

        return true;
    }

    private static void checkKeyNamesForDifferences( String providerId, Sets.SetView<String> difference )
    {
        for ( String key : difference )
        {
            Pair<String, Integer> minDistanceKey = Pair.of( "", Integer.MAX_VALUE );

            for ( String validKeyName : VALID_KEY_NAMES )
            {
                int distance = StringUtils.getLevenshteinDistance( key, validKeyName );

                if ( distance < minDistanceKey.getRight() )
                {
                    minDistanceKey = Pair.of( validKeyName, distance );
                }
            }

            log.error( String
                .format( "OpenID Connect (OIDC) configuration for provider: '%s' contains an invalid property: '%s', " +
                    "did you mean: '%s' ?", providerId, key, minDistanceKey.getLeft() ) );
        }
    }
}
