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
package org.hisp.dhis.dataintegrity;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Reads {@link DataIntegrityCheck}s from YAML files.
 *
 * @author Jan Bernitt
 */
@Slf4j
class DataIntegrityYamlReader
{
    private DataIntegrityYamlReader()
    {
        throw new UnsupportedOperationException( "util" );
    }

    static class ListYamlFile
    {
        @JsonProperty
        List<String> checks;
    }

    @JsonIgnoreProperties( ignoreUnknown = true )
    static class CheckYamlFile
    {
        @JsonProperty
        String name;

        @JsonProperty
        String description;

        @JsonProperty
        String section;

        @JsonProperty( "summary_sql" )
        String summarySql;

        @JsonProperty( "details_sql" )
        String detailsSql;

        @JsonProperty( "details_id_type" )
        String detailsIdType;

        @JsonProperty
        String introduction;

        @JsonProperty
        String recommendation;

        @JsonProperty
        DataIntegritySeverity severity;
    }

    public static void readDataIntegrityYaml( String listFile, Consumer<DataIntegrityCheck> adder,
        BinaryOperator<String> info,
        Function<String, Function<DataIntegrityCheck, DataIntegritySummary>> sqlToSummary,
        Function<String, Function<DataIntegrityCheck, DataIntegrityDetails>> sqlToDetails )
    {

        ObjectMapper yaml = new ObjectMapper( new YAMLFactory() );
        ListYamlFile file;
        try
        {
            file = yaml.readValue( new ClassPathResource( listFile ).getInputStream(), ListYamlFile.class );
        }
        catch ( Exception ex )
        {
            log.warn( "Failed to load data integrity checks from YAML", ex );
            return;
        }
        for ( String checkFile : file.checks )
        {
            try
            {
                String path = Path.of( listFile ).resolve( ".." )
                    .resolve( "data-integrity-checks" ).resolve( checkFile ).toString();
                CheckYamlFile e = yaml.readValue( new ClassPathResource( path ).getInputStream(),
                    CheckYamlFile.class );

                String name = e.name.trim();
                adder.accept( DataIntegrityCheck.builder()
                    .name( name )
                    .displayName( info.apply( name + ".name", name.replace( '_', ' ' ) ) )
                    .description( info.apply( name + ".description", trim( e.description ) ) )
                    .introduction( info.apply( name + ".introduction", trim( e.introduction ) ) )
                    .recommendation( info.apply( name + ".recommendation", trim( e.recommendation ) ) )
                    .issuesIdType( trim( e.detailsIdType ) )
                    .section( trim( e.section ) )
                    .severity( e.severity )
                    .runSummaryCheck( sqlToSummary.apply( sanitiseSQL( e.summarySql ) ) )
                    .runDetailsCheck( sqlToDetails.apply( sanitiseSQL( e.detailsSql ) ) )
                    .build() );
            }
            catch ( Exception ex )
            {
                log.error( "Failed to load data integrity check " + checkFile, ex );
            }
        }
    }

    private static String trim( String str )
    {
        return str == null ? null : str.trim();
    }

    /**
     * The purpose of this method is to strip some details from the SQL queries
     * that are present for their 2nd use case scenario but are not needed here
     * and might confuse the database (even if this is just in unit tests).
     */
    private static String sanitiseSQL( String sql )
    {
        return trim( sql
            .replaceAll( "select '[^']+' as [^,]+,", "select " )
            .replaceAll( "'[^']+' as description", "" )
            .replace( "::varchar", "" )
            .replace( "|| '%'", "" ) );
    }

}
