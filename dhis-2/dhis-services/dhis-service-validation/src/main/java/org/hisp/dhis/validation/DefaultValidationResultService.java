package org.hisp.dhis.validation;
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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.commons.collection.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.validation.comparator.ValidationResultQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Stian Sandvold
 */
@Transactional
@Slf4j
@Service( "org.hisp.dhis.validation.ValidationResultService" )
public class DefaultValidationResultService
    implements ValidationResultService
{
    private final ValidationResultStore validationResultStore;

    private final PeriodService periodService;

    private final OrganisationUnitService organisationUnitService;

    private final ValidationRuleService validationRuleService;

    public DefaultValidationResultService( ValidationResultStore validationResultStore, PeriodService periodService,
        OrganisationUnitService organisationUnitService, ValidationRuleService validationRuleService )
    {
        checkNotNull( validationResultStore );
        checkNotNull( periodService );
        checkNotNull( organisationUnitService );
        checkNotNull( validationRuleService );

        this.validationResultStore = validationResultStore;
        this.periodService = periodService;
        this.organisationUnitService = organisationUnitService;
        this.validationRuleService = validationRuleService;
    }

    @Override
    public void saveValidationResults( Collection<ValidationResult> validationResults )
    {
        validationResults.forEach( validationResult -> {
            validationResult.setPeriod( periodService.reloadPeriod( validationResult.getPeriod() ) );
            validationResultStore.save( validationResult );
        } );
    }

    @Override
    public List<ValidationResult> getAllValidationResults()
    {
        return validationResultStore.getAll();
    }

    @Override
    public List<ValidationResult> getAllUnReportedValidationResults()
    {
        return validationResultStore.getAllUnreportedValidationResults();
    }

    @Override
    public void deleteValidationResult( ValidationResult validationResult )
    {
        validationResultStore.delete( validationResult );
    }

    @Override
    public void updateValidationResults( Set<ValidationResult> validationResults )
    {
        validationResults.forEach(validationResultStore::update);
    }

    @Override
    public ValidationResult getById( long id )
    {
        return validationResultStore.getById( id );
    }

    @Override
    public List<ValidationResult> getValidationResults( ValidationResultQuery query )
    {
        validate( query );
        return validationResultStore.query( query );
    }

    @Override
    public long countValidationResults( ValidationResultQuery query )
    {
        validate( query );
        return validationResultStore.count( query );
    }

    @Override
    public List<ValidationResult> getValidationResults( OrganisationUnit orgUnit,
        boolean includeOrgUnitDescendants, Collection<ValidationRule> validationRules, Collection<Period> periods )
    {
        List<Period> persistedPeriods = periodService.reloadPeriods( new ArrayList<>( periods ) );
        return validationResultStore.getValidationResults( orgUnit, includeOrgUnitDescendants, validationRules, persistedPeriods );
    }

    private void validate( ValidationResultQuery query )
    {
        // check ou and vr filters to be valid UIDs
        validateExists( query.getOu(), ErrorCode.E7500, ErrorMessage::new, IdentifiableObject::getUid,
            organisationUnitService::getOrganisationUnitsByUid );
        validateExists( query.getVr(), ErrorCode.E7501, ErrorMessage::new, IdentifiableObject::getUid,
            validationRuleService::getValidationRulesByUid );
        // check pe filters to be valid ISO expression
        validateElements( query.getPe(), ErrorCode.E7502, ErrorMessage::new,
            isoPeriod -> PeriodType.getPeriodFromIsoString( isoPeriod ) != null );
    }

    private <T, E> void validateExists( Collection<T> identifiers, ErrorCode code,
        BiFunction<ErrorCode, Object, ErrorMessage> msgFactory, Function<E, T> getIdentifier,
        Function<Collection<T>, List<E>> toObjects )
    {
        if ( !isEmpty( identifiers ) )
        {
            Set<T> existing = toObjects.apply( identifiers ).stream()
                .map( getIdentifier )
                .collect( toSet() );
            for ( T identifier : identifiers )
            {
                if ( !existing.contains( identifier ) )
                {
                    throwValidationError( msgFactory.apply( code, identifier ) );
                }
            }
        }
    }

    private <T> void validateElements( Collection<T> values, ErrorCode code,
        BiFunction<ErrorCode, Object, ErrorMessage> msgFactory, Predicate<T> validator )
    {
        if ( !isEmpty( values ) )
        {
            for ( T val : values )
            {
                if ( !validator.test( val ) )
                {
                    throwValidationError( msgFactory.apply( code, val ) );
                }
            }
        }
    }

    private void throwValidationError( ErrorMessage error )
    {
        log.warn( String.format(
            "Validation result query failed, code: '%s', message: '%s'",
            error.getErrorCode(), error.getMessage() ) );
        throw new IllegalQueryException( error );
    }
}
