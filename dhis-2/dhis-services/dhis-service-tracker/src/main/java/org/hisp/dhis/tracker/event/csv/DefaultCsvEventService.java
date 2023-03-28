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
package org.hisp.dhis.tracker.event.csv;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramStageInstance;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * @author Enrico Colasante
 */
@Service( "org.hisp.dhis.tracker.event.csv.CsvEventService" )
public class DefaultCsvEventService
    implements CsvEventService<ProgramStageInstance>
{
    private static final CsvMapper CSV_MAPPER = new CsvMapper().enable( CsvParser.Feature.WRAP_AS_ARRAY );

    private static final Pattern TRIM_SINGLE_QUOTES = Pattern.compile( "^'|'$" );

    @Override
    public void writeEvents( OutputStream outputStream, List<ProgramStageInstance> events, boolean withHeader )
        throws IOException
    {
        final CsvSchema csvSchema = CSV_MAPPER.schemaFor( CsvEventDataValue.class )
            .withLineSeparator( "\n" )
            .withUseHeader( withHeader );

        ObjectWriter writer = CSV_MAPPER.writer( csvSchema.withUseHeader( withHeader ) );

        List<CsvEventDataValue> dataValues = new ArrayList<>();

        for ( ProgramStageInstance event : events )
        {
            CsvEventDataValue templateDataValue = new CsvEventDataValue();
            templateDataValue.setEvent( event.getUid() );
            templateDataValue.setStatus( event.getStatus() != null ? event.getStatus().name() : null );
            templateDataValue
                .setEnrollment( event.getProgramInstance() != null ? event.getProgramInstance().getUid() : null );
            templateDataValue
                .setProgram( event.getProgramInstance() != null && event.getProgramInstance().getProgram() != null
                    ? event.getProgramInstance().getProgram().getUid()
                    : null );
            templateDataValue
                .setProgramStage( event.getProgramStage() != null ? event.getProgramStage().getUid() : null );
            templateDataValue
                .setOrgUnit( event.getOrganisationUnit() != null ? event.getOrganisationUnit().getUid() : null );
            templateDataValue
                .setOrgUnitName( event.getOrganisationUnit() != null ? event.getOrganisationUnit().getName() : null );
            templateDataValue
                .setOccurredAt( event.getExecutionDate() == null ? null : event.getExecutionDate().toString() );
            templateDataValue.setScheduledAt( event.getDueDate() == null ? null : event.getDueDate().toString() );
            templateDataValue
                .setFollowup( event.getProgramInstance() != null ? event.getProgramInstance().getFollowup() : false );
            templateDataValue.setDeleted( event.isDeleted() );
            templateDataValue.setCreatedAt( event.getCreated() == null ? null : event.getCreated().toString() );
            templateDataValue.setCreatedAtClient(
                event.getCreatedAtClient() == null ? null : event.getCreatedAtClient().toString() );
            templateDataValue.setUpdatedAt( event.getLastUpdated() == null ? null : event.getLastUpdated().toString() );
            templateDataValue.setUpdatedAtClient(
                event.getLastUpdatedAtClient() == null ? null : event.getLastUpdatedAtClient().toString() );
            templateDataValue
                .setCompletedAt( event.getCompletedDate() == null ? null : event.getCompletedDate().toString() );
            templateDataValue
                .setUpdatedBy( event.getLastUpdatedBy() == null ? null : event.getLastUpdatedBy().getUsername() );
            templateDataValue.setStoredBy( event.getStoredBy() );
            templateDataValue.setCompletedBy( event.getCompletedBy() );
            templateDataValue.setAttributeOptionCombo(
                event.getAttributeOptionCombo() != null ? event.getAttributeOptionCombo().getUid() : null );
            templateDataValue.setAttributeCategoryOptions(
                event.getAttributeOptionCombo() != null ? event.getAttributeOptionCombo().getCategoryOptions().stream()
                    .map( CategoryOption::getUid ).collect( Collectors.joining() ) : null );
            templateDataValue
                .setAssignedUser( event.getAssignedUser() == null ? null : event.getAssignedUser().getUsername() );

            if ( event.getGeometry() != null )
            {
                templateDataValue.setGeometry( event.getGeometry().toText() );

                if ( event.getGeometry().getGeometryType().equals( "Point" ) )
                {
                    templateDataValue.setLongitude( event.getGeometry().getCoordinate().x );
                    templateDataValue.setLatitude( event.getGeometry().getCoordinate().y );
                }
            }

            if ( event.getEventDataValues().isEmpty() )
            {
                dataValues.add( templateDataValue );
                continue;
            }

            for ( EventDataValue value : event.getEventDataValues() )
            {
                CsvEventDataValue dataValue = new CsvEventDataValue( templateDataValue );
                dataValue.setDataElement( value.getDataElement() );
                dataValue.setValue( value.getValue() );
                dataValue.setProvidedElsewhere( value.getProvidedElsewhere() );
                dataValue
                    .setCreatedAtDataValue( value.getCreated() == null ? null : value.getCreated().toString() );
                dataValue
                    .setUpdatedAtDataValue( value.getLastUpdated() == null ? null : value.getLastUpdated().toString() );

                if ( value.getStoredBy() != null )
                {
                    dataValue.setStoredBy( value.getStoredBy() );
                }

                dataValues.add( dataValue );
            }
        }

        writer.writeValue( outputStream, dataValues );
    }
}
