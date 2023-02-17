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
package org.hisp.dhis.validation;

import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.commons.collection.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

/**
 * @author Stian Sandvold
 */
@Slf4j
@RequiredArgsConstructor
@Service( "org.hisp.dhis.validation.ValidationResultService" )
public class DefaultValidationResultService
    implements ValidationResultService
{
    private final ValidationResultStore validationResultStore;

    private final PeriodService periodService;

    private final OrganisationUnitService organisationUnitService;

    private final ValidationRuleService validationRuleService;

    @Transactional
    @Override
    public void saveValidationResults( Collection<ValidationResult> validationResults )
    {
        validationResults.forEach( validationResult -> {
            validationResult.setPeriod( periodService.reloadPeriod( validationResult.getPeriod() ) );
            validationResultStore.save( validationResult );
        } );
    }

    @Transactional( readOnly = true )
    @Override
    public List<ValidationResult> getAllValidationResults()
    {
        return validationResultStore.getAll();
    }

    @Transactional( readOnly = true )
    @Override
    public List<ValidationResult> getAllUnReportedValidationResults()
    {
        return validationResultStore.getAllUnreportedValidationResults();
    }

    @Transactional
    @Override
    public void deleteValidationResult( ValidationResult validationResult )
    {
        validationResultStore.delete( validationResult );
    }

    @Transactional
    @Override
    public void deleteValidationResults( ValidationResultsDeletionRequest request )
    {
        if ( !request.isUnconstrained() )
        {
            validate( request );
            validationResultStore.delete( request );
        }
    }

    @Transactional
    @Override
    public void updateValidationResults( Set<ValidationResult> validationResults )
    {
        validationResults.forEach( validationResultStore::update );
    }

    @Transactional( readOnly = true )
    @Override
    public ValidationResult getById( long id )
    {
        return validationResultStore.getById( id );
    }

    @Transactional( readOnly = true )
    @Override
    public List<ValidationResult> getValidationResults( ValidationResultQuery query )
    {
        validate( query );
        return validationResultStore.query( query );
    }

    @Transactional( readOnly = true )
    @Override
    public long countValidationResults( ValidationResultQuery query )
    {
        validate( query );
        return validationResultStore.count( query );
    }

    @Transactional( readOnly = true )
    @Override
    public List<ValidationResult> getValidationResults( OrganisationUnit orgUnit,
        boolean includeOrgUnitDescendants, Collection<ValidationRule> validationRules, Collection<Period> periods )
    {
        List<Period> persistedPeriods = periodService.reloadPeriods( new ArrayList<>( periods ) );
        return validationResultStore.getValidationResults( orgUnit, includeOrgUnitDescendants, validationRules,
            persistedPeriods );
    }

    private void validate( ValidationResultsDeletionRequest request )
    {
        validateExists( request.getOu(), ErrorCode.E7500, ErrorMessage::new, IdentifiableObject::getUid,
            organisationUnitService::getOrganisationUnitsByUid );
        validateExists( request.getVr(), ErrorCode.E7501, ErrorMessage::new, IdentifiableObject::getUid,
            validationRuleService::getValidationRulesByUid );
        validateElement( request.getPe(), ErrorCode.E7502, ErrorMessage::new,
            DefaultValidationResultService::isIsoPeriod );
        validateElement( request.getCreated(), ErrorCode.E7503, ErrorMessage::new,
            DefaultValidationResultService::isIsoPeriod );
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
            DefaultValidationResultService::isIsoPeriod );
    }

    private static boolean isIsoPeriod( String isoPeriod )
    {
        return PeriodType.getPeriodFromIsoString( isoPeriod ) != null;
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
                validateElement( val, code, msgFactory, validator );
            }
        }
    }

    private <T> void validateElement( T val, ErrorCode code, BiFunction<ErrorCode, Object, ErrorMessage> msgFactory,
        Predicate<T> validator )
    {
        if ( val != null && !validator.test( val ) )
        {
            throwValidationError( msgFactory.apply( code, val ) );
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
