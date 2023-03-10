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
package org.hisp.dhis.period;

import static java.time.LocalDate.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Unit tests for {@link PeriodDataProvider}.
 *
 * @author Abyot Asalefew Gizaw
 */
@ExtendWith( MockitoExtension.class )
class PeriodDataProviderTest
{
    private PeriodDataProvider periodDataProvider;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp()
    {
        periodDataProvider = new PeriodDataProvider( jdbcTemplate );
    }

    @Test
    void shouldReturnFiveExtraYearsBeforeAndAfterDataYears()
    {
        int currentYear = now().getYear();

        List<Integer> storedDataYears = Arrays.asList( currentYear - 1, currentYear );

        when( jdbcTemplate.queryForList( anyString(), ArgumentMatchers.<Class<Integer>> any() ) )
            .thenReturn( storedDataYears );

        List<Integer> dataYears = periodDataProvider.getAvailableYears();

        assertEquals( 12, dataYears.size() );
        assertTrue( dataYears.contains( (dataYears.get( storedDataYears.size() - 1 ) + 5) ) );
        assertTrue( dataYears.contains( (storedDataYears.get( 0 ) - 5) ) );
        assertFalse( dataYears.contains( (storedDataYears.get( 0 ) - 6) ) );
    }

    @Test
    void shouldReturnFiveExtraYearsBeforeAndAfterCurrentYearWhenNoDataExists()
    {
        int currentYear = now().getYear();

        List<Integer> storedDataYears = new ArrayList<>();

        when( jdbcTemplate.queryForList( anyString(), ArgumentMatchers.<Class<Integer>> any() ) )
            .thenReturn( storedDataYears );

        List<Integer> dataYears = periodDataProvider.getAvailableYears();

        assertEquals( 11, dataYears.size() );
        assertTrue( dataYears.contains( currentYear + 5 ) );
        assertTrue( dataYears.contains( currentYear - 5 ) );
        assertFalse( dataYears.contains( currentYear - 6 ) );
    }
}