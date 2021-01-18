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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.validation.comparator.ValidationResultQuery;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the validation of the {@link ValidationResultService}.
 *
 * @author Jan Bernitt
 */
public class ValidationResultServiceTest
{

    private final ValidationResultStore store = mock( ValidationResultStore.class );

    private final PeriodService periodService = mock( PeriodService.class );

    private final OrganisationUnitService organisationUnitService = mock( OrganisationUnitService.class );

    private final ValidationRuleService validationRuleService = mock( ValidationRuleService.class );

    private final ValidationResultService service = new DefaultValidationResultService( store, periodService,
        organisationUnitService, validationRuleService );

    @Before
    public void setUp()
    {
        when( store.count( any() ) ).thenReturn( 0 );
        when( store.query( any() ) ).thenReturn( emptyList() );

        // return an OrganisationUnit for any given valid UID
        when( organisationUnitService.getOrganisationUnitsByUid( any() ) ).then( orgUnitsByUidInvocation -> {
            Collection<String> uids = orgUnitsByUidInvocation.getArgument( 0 );
            List<OrganisationUnit> units = new ArrayList<>();
            for ( String uid : uids )
            {
                if ( CodeGenerator.isValidUid( uid ) )
                {
                    OrganisationUnit unit = new OrganisationUnit();
                    unit.setUid( uid );
                    units.add( unit );
                }
            }
            return units;
        } );

        // return a ValidationRule for any given valid UID
        when( validationRuleService.getValidationRulesByUid( any() ) ).then( validationRuleByUidInvocation -> {
            Collection<String> uids = validationRuleByUidInvocation.getArgument( 0 );
            List<ValidationRule> rules = new ArrayList<>();
            for ( String uid : uids )
            {
                if ( CodeGenerator.isValidUid( uid ) )
                {
                    ValidationRule rule = new ValidationRule();
                    rule.setUid( uid );
                    rules.add( rule );
                }
            }
            return rules;
        } );
    }

    @Test
    public void validationOrganisationUnitMustBeUid()
    {
        BiConsumer<ValidationResultQuery, List<String>> op = ValidationResultQuery::setOu;
        assertIllegal( ErrorCode.E7500, op, "tooShrtUid" );
        assertIllegal( ErrorCode.E7500, op, "tooLooongUid" );
        assertIllegal( ErrorCode.E7500, op, "i//egalUid$" );
        assertIllegal( ErrorCode.E7500, op, "valid678901", "i//egalUid$" );
        assertIllegal( ErrorCode.E7500, op, "valid678901", "valid678902", "i//egalUid$" );
        assertLegal( op, "a2345678901" );
        assertLegal( op, "abcdefghijk" );
        assertLegal( op, "abcdefghijk", "AbCdEfGhIjK", "Ab3d5f7h9j1" );
    }

    @Test
    public void validationValidationRuleMustBeUid()
    {
        BiConsumer<ValidationResultQuery, List<String>> op = ValidationResultQuery::setVr;
        assertIllegal( ErrorCode.E7501, op, "tooShrtUid" );
        assertIllegal( ErrorCode.E7501, op, "tooLooongUid" );
        assertIllegal( ErrorCode.E7501, op, "i//egalUid$" );
        assertIllegal( ErrorCode.E7501, op, "valid678901", "i//egalUid$" );
        assertIllegal( ErrorCode.E7501, op, "valid678901", "valid678902", "i//egalUid$" );
        assertLegal( op, "a2345678901" );
        assertLegal( op, "abcdefghijk" );
        assertLegal( op, "abcdefghijk", "AbCdEfGhIjK", "Ab3d5f7h9j1" );
    }

    @Test
    public void validationPeriodMustBeIsoExpressions()
    {
        BiConsumer<ValidationResultQuery, List<String>> op = ValidationResultQuery::setPe;
        assertIllegal( ErrorCode.E7502, op, "illegal" );
        assertIllegal( ErrorCode.E7502, op, "2019Q1", "illegal" );
        assertIllegal( ErrorCode.E7502, op, "2020W34", "202001", "illegal" );
        assertLegal( op, "2019" );
        assertLegal( op, "2019Q3" );
        assertLegal( op, "201901", "2019-02", "2019BiW3" );
    }

    private void assertLegal( BiConsumer<ValidationResultQuery, List<String>> operation, String... values )
    {
        ValidationResultQuery query = new ValidationResultQuery();
        operation.accept( query, asList( values ) );
        assertEquals( emptyList(), service.getValidationResults( query ) );
        assertEquals( 0, service.countValidationResults( query ) );
    }

    private void assertIllegal( ErrorCode expected,
        BiConsumer<ValidationResultQuery, List<String>> operation, String... values )
    {
        ValidationResultQuery query = new ValidationResultQuery();
        operation.accept( query, asList( values ) );
        IllegalQueryException ex = assertThrows( IllegalQueryException.class,
            () -> service.getValidationResults( query ) );
        String errorValue = values[values.length - 1];
        assertError( ex, expected, errorValue );
        ex = assertThrows( IllegalQueryException.class,
            () -> service.countValidationResults( query ) );
        assertError( ex, expected, errorValue );
    }

    private void assertError( IllegalQueryException ex, ErrorCode expected, String value )
    {
        assertEquals( expected, ex.getErrorCode() );
        assertEquals( expected.getMessage().replace( "{0}", value ), ex.getMessage() );
    }
}
