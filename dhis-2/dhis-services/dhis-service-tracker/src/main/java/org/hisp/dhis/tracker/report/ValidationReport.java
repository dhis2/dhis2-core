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
package org.hisp.dhis.tracker.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.hisp.dhis.tracker.validation.Validation;
import org.hisp.dhis.tracker.validation.ValidationResult;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@ToString
@EqualsAndHashCode
public class ValidationReport
{
    @JsonProperty( "errorReports" )
    private final List<Error> errors;

    @JsonProperty( "warningReports" )
    private final List<Warning> warnings;

    public static ValidationReport emptyReport()
    {
        return new ValidationReport();
    }

    public static ValidationReport merge( ValidationResult validationResult, ValidationResult anotherValidationResult )
    {
        ValidationReport validationReport = fromResult( validationResult );
        ValidationReport anotherValidationReport = fromResult( anotherValidationResult );
        validationReport.addErrors( anotherValidationReport.getErrors() );
        validationReport.addWarnings( anotherValidationReport.getWarnings() );
        return validationReport;
    }

    public static ValidationReport fromResult( ValidationResult validationResult )
    {
        ValidationReport validationReport = new ValidationReport();
        validationReport.addErrors( convertToError( List.copyOf( validationResult.getErrors() ) ) );
        validationReport.addWarnings( convertToWarning( List.copyOf( validationResult.getWarnings() ) ) );
        return validationReport;
    }

    private static List<Error> convertToError( List<Validation> errors )
    {
        return errors.stream()
            .map( e -> Error.builder()
                .errorMessage( e.getMessage() )
                .errorCode( e.getCode() )
                .trackerType( e.getType() )
                .uid( e.getUid() )
                .build() )
            .collect( Collectors.toList() );
    }

    private static List<Warning> convertToWarning( List<Validation> warnings )
    {
        return warnings.stream()
            .map( e -> Warning.builder()
                .warningMessage( e.getMessage() )
                .warningCode( e.getCode() )
                .trackerType( e.getType() )
                .uid( e.getUid() )
                .build() )
            .collect( Collectors.toList() );
    }

    private ValidationReport()
    {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    public ValidationReport( List<Error> errors, List<Warning> warnings )
    {
        this.errors = errors;
        this.warnings = warnings;
    }

    // -----------------------------------------------------------------------------------
    // Utility Methods
    // -----------------------------------------------------------------------------------

    public List<Error> getErrors()
    {
        return Collections.unmodifiableList( errors );
    }

    public List<Warning> getWarnings()
    {
        return Collections.unmodifiableList( warnings );
    }

    public boolean hasErrors()
    {
        return !errors.isEmpty();
    }

    public boolean hasError( Predicate<Error> test )
    {
        return errors.stream().anyMatch( test );
    }

    public boolean hasWarnings()
    {
        return !warnings.isEmpty();
    }

    public boolean hasWarning( Predicate<Warning> test )
    {
        return warnings.stream().anyMatch( test );
    }

    /**
     * Returns the size of all the Tracker DTO that did not pass validation
     */
    public long size()
    {

        return this.getErrors().stream().map( Error::getUid ).distinct().count();
    }

    public void addErrors( List<Error> errors )
    {
        for ( Error error : errors )
        {
            addErrorIfNotExisting( error );
        }
    }

    public void addWarnings( List<Warning> warnings )
    {
        for ( Warning warning : warnings )
        {
            addWarningIfNotExisting( warning );
        }
    }

    private void addErrorIfNotExisting( Error error )
    {
        if ( !this.errors.contains( error ) )
        {
            this.errors.add( error );
        }
    }

    private void addWarningIfNotExisting( Warning warning )
    {
        if ( !this.warnings.contains( warning ) )
        {
            this.warnings.add( warning );
        }
    }
}
