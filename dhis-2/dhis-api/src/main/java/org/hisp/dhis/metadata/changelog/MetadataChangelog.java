/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.metadata.changelog;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.fileresource.FileResource;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This object will be created when user import a Metadata Package payload which
 * includes a package info object like this:
 *
 * <pre>
 * {@code
 *  // @formatter:off
 * "package": {
 *     "DHIS2Build": "23c23c5",
 *     "DHIS2Version": "2.37.7.1",
 *     "code": "C19_CS",
 *     "description": "COVID-19 Case Surveillance and Contact Tracing",
 *     "lastUpdated": "20220919T104915",
 *     "locale": "en",
 *     "name": "C19_CS_TRK_DHIS2.37.7.1-en",
 *     "type": "TRK",
 *     "version": "1.0.2" }
 *   }
 * // @formatter:on
 * <p>
 * The name of the changelog is a concatenation of code, type, dhisVersion,
 * locale.
 */
@Getter
@Setter
@EqualsAndHashCode( of = { "version", "dhis2Version", "locale" } )
@Accessors( chain = true )
public class MetadataChangelog extends BaseIdentifiableObject
{
    /**
     * The name of the metadata changelog object in the import payload.
     */
    public static final String JSON_OBJECT_NAME = "package";

    /**
     * The build version of the DHIS2 instance which the metadata package was
     * exported from.
     */
    @JsonProperty( value = "DHIS2Build", required = true )
    private String dhis2Build;

    /**
     * The version of the DHIS2 instance which the metadata package was exported
     * from.
     */
    @JsonProperty( value = "DHIS2Version", required = true )
    private String dhis2Version;

    /**
     * Type of the metadata package.
     */
    @JsonProperty
    private String type;

    /**
     * Version of the metadata package.
     */
    @JsonProperty( required = true )
    private String version;

    /**
     * The locale of the metadata package.
     */
    @JsonProperty( required = true )
    private String locale;

    /**
     * A reference to the imported file. Only created if the import is
     * successful.
     */
    @JsonProperty
    private FileResource importFile;
}
