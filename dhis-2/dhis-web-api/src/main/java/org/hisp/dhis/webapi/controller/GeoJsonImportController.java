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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.common.IdentifiableProperty.UID;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.dxf2.geojson.GeoJsonImportParams;
import org.hisp.dhis.dxf2.geojson.GeoJsonImportReport;
import org.hisp.dhis.dxf2.geojson.GeoJsonService;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Jan Bernitt
 */
@RequestMapping( "/organisationUnits/geometry" )
@RestController
@AllArgsConstructor
public class GeoJsonImportController
{
    private final GeoJsonService geoJsonService;

    @PostMapping( value = "", consumes = { "application/geo+json", "application/json" } )
    public WebMessage postImport(
        /**
         * If true the {@code id} field/property of a geo-json {feature} node is
         * used to refer to the organisation unit. If false the
         * {@link #geojsonProperty} names the property within the geo-json
         * {@code feature.properties} map that refers to the organisation unit.
         */
        @RequestParam( defaultValue = "true" ) boolean geoJsonId,
        @RequestParam( required = false )
        final String geoJsonProperty,
        @RequestParam( required = false )
        final String orgUnitProperty,
        @RequestParam( required = false )
        final String attributeId,
        @RequestParam( required = false ) boolean dryRun,
        HttpServletRequest request )
        throws IOException
    {
        GeoJsonImportReport report = geoJsonService.importGeoData( GeoJsonImportParams.builder()
            .attributeId( attributeId )
            .dryRun( dryRun )
            .idType( orgUnitProperty == null ? UID : IdentifiableProperty.valueOf( orgUnitProperty.toUpperCase() ) )
            .orgUnitIdProperty( geoJsonId ? "id" : "properties." + geoJsonProperty )
            .build(),
            request.getInputStream() );

        return WebMessageUtils.ok().setResponse( report );
    }
}
