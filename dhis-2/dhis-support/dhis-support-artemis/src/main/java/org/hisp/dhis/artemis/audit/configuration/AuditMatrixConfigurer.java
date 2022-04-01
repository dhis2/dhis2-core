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
package org.hisp.dhis.artemis.audit.configuration;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

/**
 * Configures the Audit Matrix based on configuration properties from dhis.conf
 * <p>
 * This configurator uses properties with prefix "audit.". Each property
 * prefixed with "audit." must match the (lowercase) name of an
 * {@see AuditScope} and must contain a semi-colon list of valid
 * {@see AuditType} names: (READ;UPDATE;...).
 * <p>
 * Example:
 * <p>
 * audit.tracker=CREATE;READ;UPDATE;DELETE
 * <p>
 * Misspelled entries are ignored, and the specific type is set to false.
 * Missing {@see AuditScope} are replaced with all-false types. To disable
 * Auditing completely, simply do not declare any audit.* property in dhis.conf
 *
 * @author Luciano Fiandesio
 */
@Component
public class AuditMatrixConfigurer
{
    private final DhisConfigurationProvider config;

    private final static String PROPERTY_PREFIX = "audit.";

    private final static String AUDIT_TYPE_STRING_SEPAR = ";";

    /**
     * Default Audit configuration: CREATE, UPDATE and DELETE operations are
     * audited by default. Other Audit types have to be explicitly enabled by
     * the user
     */
    private static final Map<AuditType, Boolean> DEFAULT_AUDIT_CONFIGURATION = ImmutableMap
        .<AuditType, Boolean> builder()
        .put( AuditType.CREATE, true )
        .put( AuditType.UPDATE, true )
        .put( AuditType.DELETE, true )
        .put( AuditType.READ, false )
        .put( AuditType.SEARCH, false )
        .put( AuditType.SECURITY, false )
        .build();

    public AuditMatrixConfigurer( DhisConfigurationProvider dhisConfigurationProvider )
    {
        checkNotNull( dhisConfigurationProvider );

        this.config = dhisConfigurationProvider;
    }

    public Map<AuditScope, Map<AuditType, Boolean>> configure()
    {
        Map<AuditScope, Map<AuditType, Boolean>> matrix = new HashMap<>();

        for ( AuditScope value : AuditScope.values() )
        {
            Optional<ConfigurationKey> confKey = ConfigurationKey
                .getByKey( PROPERTY_PREFIX + value.name().toLowerCase() );

            if ( confKey.isPresent() && !StringUtils.isEmpty( config.getProperty( confKey.get() ) ) )
            {
                String[] configuredTypes = config.getProperty( confKey.get() ).split( AUDIT_TYPE_STRING_SEPAR );

                Map<AuditType, Boolean> matrixAuditTypes = new HashMap<>();

                for ( AuditType auditType : AuditType.values() )
                {
                    matrixAuditTypes.put( auditType, ArrayUtils.contains( configuredTypes, auditType.name() ) );
                }

                matrix.put( value, matrixAuditTypes );

            }
            else
            {
                matrix.put( value, DEFAULT_AUDIT_CONFIGURATION );
            }
        }

        return matrix;
    }
}
