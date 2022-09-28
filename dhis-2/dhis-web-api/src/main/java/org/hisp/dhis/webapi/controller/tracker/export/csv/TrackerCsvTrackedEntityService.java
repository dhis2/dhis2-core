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
package org.hisp.dhis.webapi.controller.tracker.export.csv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hisp.dhis.dxf2.events.event.csv.CsvEventService;
import org.hisp.dhis.webapi.controller.tracker.view.Attribute;
import org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

@Service
public class TrackerCsvTrackedEntityService implements CsvEventService<TrackedEntity>
{

    private static final CsvMapper CSV_MAPPER = new CsvMapper().enable( CsvParser.Feature.WRAP_AS_ARRAY );

    @Override
    public void writeEvents( OutputStream outputStream, List<TrackedEntity> trackedEntities, boolean withHeader )
        throws IOException
    {
        final CsvSchema csvSchema = CSV_MAPPER.schemaFor( CsvTrackedEntityDataValue.class )
            .withLineSeparator( "\n" )
            .withUseHeader( withHeader );

        ObjectWriter writer = CSV_MAPPER.writer( csvSchema.withUseHeader( withHeader ) );

        List<CsvTrackedEntityDataValue> dataValues = new ArrayList<>();

        for ( TrackedEntity trackedEntity : trackedEntities )
        {
            CsvTrackedEntityDataValue dataValue = new CsvTrackedEntityDataValue();
            dataValue.setTrackedEntity( trackedEntity.getTrackedEntity() );
            dataValue.setTrackedEntityType( trackedEntity.getTrackedEntityType() );
            dataValue
                .setCreatedAt( trackedEntity.getCreatedAt() == null ? null : trackedEntity.getCreatedAt().toString() );
            dataValue.setCreatedAtClient(
                trackedEntity.getCreatedAtClient() == null ? null : trackedEntity.getCreatedAtClient().toString() );
            dataValue
                .setUpdatedAt( trackedEntity.getUpdatedAt() == null ? null : trackedEntity.getUpdatedAt().toString() );
            dataValue.setUpdatedAtClient(
                trackedEntity.getUpdatedAtClient() == null ? null : trackedEntity.getUpdatedAtClient().toString() );
            dataValue.setOrgUnit( trackedEntity.getOrgUnit() );
            dataValue.setInactive( trackedEntity.isInactive() );
            dataValue.setDeleted( trackedEntity.isDeleted() );
            dataValue.setPotentialDuplicate( trackedEntity.isPotentialDuplicate() );

            if ( trackedEntity.getAttributes().isEmpty() )
            {
                dataValues.add( dataValue );
            }
            else
            {
                addAttributes( trackedEntity, dataValue, dataValues );
            }
        }

        writer.writeValue( outputStream, dataValues );
    }

    private void addAttributes( TrackedEntity trackedEntity, CsvTrackedEntityDataValue currentDataValue,
        List<CsvTrackedEntityDataValue> dataValues )
    {
        for ( Attribute attribute : trackedEntity.getAttributes() )
        {
            CsvTrackedEntityDataValue csvTrackedEntityDataValue = new CsvTrackedEntityDataValue( currentDataValue );
            csvTrackedEntityDataValue.setAttribute( attribute.getAttribute() );
            csvTrackedEntityDataValue.setDisplayName( attribute.getDisplayName() );
            csvTrackedEntityDataValue
                .setAttrCreatedAt( attribute.getCreatedAt() == null ? null : attribute.getCreatedAt().toString() );
            csvTrackedEntityDataValue
                .setAttrUpdatedAt( attribute.getUpdatedAt() == null ? null : attribute.getUpdatedAt().toString() );
            csvTrackedEntityDataValue.setValueType( attribute.getValueType().toString() );
            csvTrackedEntityDataValue.setValue( attribute.getValue() );
            dataValues.add( csvTrackedEntityDataValue );
        }
    }

    @Override
    public List<TrackedEntity> readEvents( InputStream inputStream, boolean skipFirst )
    {
        return Collections.emptyList();
    }
}