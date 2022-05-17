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
package org.hisp.dhis.dxf2.geojson;

import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.hisp.dhis.dxf2.importsummary.ImportConflict.createConflict;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import lombok.AllArgsConstructor;

import org.geotools.geojson.geom.GeometryJSON;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to process Geo-JSON imports and (eventually) exports.
 *
 * @author Jan Bernitt
 */
@Service
@AllArgsConstructor
public class DefaultGeoJsonService implements GeoJsonService
{
    private final AttributeService attributeService;

    private final OrganisationUnitStore organisationUnitStore;

    @Override
    @Transactional
    public GeoJsonImportReport importGeoData( GeoJsonImportParams params, InputStream geoJsonFeatureCollection )
    {
        GeoJsonImportReport report = new GeoJsonImportReport();
        JsonObject featureCollection;
        try
        {
            featureCollection = new JsonResponse(
                new String( geoJsonFeatureCollection.readAllBytes(), StandardCharsets.UTF_8 ) );
        }
        catch ( IOException ex )
        {
            report.addConflict( createConflict( GeoJsonImportConflict.INPUT_IO_ERROR, ex.getMessage() ) );
            return report;
        }
        String idProperty = isBlank( params.getOrgUnitIdProperty() ) ? "id" : params.getOrgUnitIdProperty();
        Function<JsonObject, String> readIdentifiers = feature -> feature.getString( idProperty ).string();
        JsonList<JsonObject> features = featureCollection.getList( "features", JsonObject.class );
        Set<String> ouIdentifiers = features.stream()
            .map( readIdentifiers )
            .filter( Objects::nonNull )
            .collect( toUnmodifiableSet() );

        List<OrganisationUnit> units = fetchOrganisationUnits( params, ouIdentifiers );
        Function<OrganisationUnit, String> toKey = getGeoJsonFeatureToOrgUnitIdentifier( params );
        Map<String, OrganisationUnit> unitsByIdentifier = units.stream()
            .collect( toUnmodifiableMap( toKey, Function.identity() ) );

        Attribute attribute = validateAttribute( params.getAttributeId(), report );
        if ( report.getConflictCount() > 0 )
        {
            return report;
        }
        int index = 0;
        for ( JsonObject feature : features )
        {
            String identifier = readIdentifiers.apply( feature );
            if ( identifier == null )
            {
                report.addConflict( createConflict( index, GeoJsonImportConflict.FEATURE_LACKS_IDENTIFIER,
                    params.getOrgUnitIdProperty() ) );
                report.getImportCount().incrementIgnored();
            }
            else
            {
                OrganisationUnit target = unitsByIdentifier.get( identifier );
                JsonObject geometry = feature.getObject( "geometry" );
                updateGeometry( attribute, target, geometry, report, index );
            }
            index++;
        }
        return report;
    }

    private Function<OrganisationUnit, String> getGeoJsonFeatureToOrgUnitIdentifier( GeoJsonImportParams params )
    {
        switch ( params.getIdType() )
        {
        case CODE:
            return OrganisationUnit::getCode;
        case NAME:
            return OrganisationUnit::getName;
        default:
            return OrganisationUnit::getUid;
        }
    }

    private List<OrganisationUnit> fetchOrganisationUnits( GeoJsonImportParams params, Set<String> ouIdentifiers )
    {
        switch ( params.getIdType() )
        {
        case CODE:
            return organisationUnitStore.getByCode( ouIdentifiers, null );
        case NAME:
            return organisationUnitStore.getByName( ouIdentifiers, null );
        default:
            return organisationUnitStore.getByUid( ouIdentifiers, null );
        }
    }

    private Attribute validateAttribute( String attributeId, GeoJsonImportReport report )
    {
        if ( attributeId == null )
        {
            return null;
        }
        Attribute attribute = attributeService.getAttribute( attributeId );
        if ( attribute == null )
        {
            report.addConflict( createConflict( GeoJsonImportConflict.ATTRIBUTE_NOT_FOUND, attributeId ) );
            return null;
        }
        if ( attribute.getValueType() != ValueType.GEOJSON )
        {
            report.addConflict( createConflict( GeoJsonImportConflict.ATTRIBUTE_NOT_GEO_JSON, ValueType.GEOJSON.name(),
                attribute.getValueType().name() ) );
            return attribute;
        }
        if ( !attribute.isAttribute( Attribute.ObjectType.ORGANISATION_UNIT ) )
        {
            report.addConflict( createConflict( GeoJsonImportConflict.ATTRIBUTE_NOT_USABLE ) );
        }
        return attribute;
    }

    private void updateGeometry( Attribute attribute, OrganisationUnit target, JsonObject geometry,
        GeoJsonImportReport report, int index )
    {
        Runnable inc;
        ImportCount stats = report.getImportCount();
        if ( target == null )
        {
            report.addConflict( createConflict( index, GeoJsonImportConflict.ORG_UNIT_NOT_FOUND ) );
            stats.incrementIgnored();
            return;
        }
        String geoJsonValue = geometry.node().getDeclaration();
        if ( attribute != null )
        {
            AttributeValue attributeValue = target.getAttributeValue( attribute );
            if ( attributeValue != null )
            {
                attributeValue.setValue( geoJsonValue );
                inc = stats::incrementUpdated;
            }
            else
            {
                target.getAttributeValues().add( new AttributeValue( attribute, geoJsonValue ) );
                inc = stats::incrementImported;
            }
        }
        else
        {
            try
            {
                Geometry old = target.getGeometry();
                target.setGeometry( new GeometryJSON().read( geoJsonValue ) );
                inc = old != null ? stats::incrementUpdated : stats::incrementImported;
            }
            catch ( Exception ex )
            {
                report.addConflict( createConflict( index, GeoJsonImportConflict.GEOMETRY_INVALID ) );
                stats.incrementIgnored();
                return;
            }
        }
        try
        {
            organisationUnitStore.update( target );
            inc.run(); // now we can count as updated or imported
        }
        catch ( Exception ex )
        {
            stats.incrementIgnored();
            report.addConflict( createConflict( index, GeoJsonImportConflict.ORG_UNIT_INVALID ) );
        }
    }

}
