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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.hisp.dhis.common.ValueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

class EnumBindingTest
{
    private final static String ENDPOINT = "/enum";

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup( new EnumController() ).build();
    }

    @Test
    void verifyValidUpperCaseParameterReturnsOk()
        throws Exception
    {
        mockMvc.perform( get( ENDPOINT )
            .param( "valueType", "TEXT" ) )
            .andExpect( status().isOk() )
            .andExpect( content().string( "TEXT" ) );
    }

    @Test
    void verifyValidLowerCaseParameterReturnsOk()
        throws Exception
    {
        mockMvc.perform( get( ENDPOINT )
            .param( "valueType", "text" ) )
            .andExpect( status().isOk() )
            .andExpect( content().string( "TEXT" ) );
    }

    @Test
    void verifyValidUpperCaseCriteriaReturnsOk()
        throws Exception
    {
        mockMvc.perform( get( ENDPOINT + "/criteria" )
            .param( "valueTypeInCriteria", "TEXT" ) )
            .andExpect( status().isOk() )
            .andExpect( content().string( "TEXT" ) );
    }

    @Test
    void verifyValidLowerCaseCriteriaReturnsOk()
        throws Exception
    {
        mockMvc.perform( get( ENDPOINT + "/criteria" )
            .param( "valueTypeInCriteria", "text" ) )
            .andExpect( status().isOk() )
            .andExpect( content().string( "TEXT" ) );
    }

    @Controller
    private class EnumController extends CrudControllerAdvice
    {
        @GetMapping( value = ENDPOINT )
        public @ResponseBody String getEnumValue( @RequestParam ValueType valueType )
        {
            return valueType.name();
        }

        @GetMapping( value = ENDPOINT + "/criteria" )
        public @ResponseBody String getEnumValue( ValueTypeCriteria valueTypeCriteria )
        {
            return valueTypeCriteria.getValueTypeInCriteria().name();
        }
    }

    @AllArgsConstructor
    @Getter
    private class ValueTypeCriteria
    {
        private final ValueType valueTypeInCriteria;
    }
}
