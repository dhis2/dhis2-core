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
package org.hisp.dhis.webapi.controller;

import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.webapi.mvc.FieldFilterMappingJacksonValue;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.webdomain.PeriodType;
import org.hisp.dhis.webapi.webdomain.PeriodTypes;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RestController
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequestMapping( value = "/periodTypes" )
public class PeriodTypeController
{
    private final PeriodService periodService;

    public PeriodTypeController( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    @GetMapping
    public HttpEntity<MappingJacksonValue> getPeriodTypes( @RequestParam( defaultValue = "*" ) Set<String> fields )
    {
        PeriodTypes periodTypes = new PeriodTypes( periodService.getAllPeriodTypes().stream()
            .map( PeriodType::new )
            .collect( Collectors.toList() ) );

        return ResponseEntity.ok( new FieldFilterMappingJacksonValue( periodTypes, fields ) );
    }

    @GetMapping( value = "/relativePeriodTypes", produces = { "application/json", "application/javascript" } )
    public @ResponseBody RelativePeriodEnum[] getRelativePeriodTypes()
    {
        return RelativePeriodEnum.values();
    }
}
