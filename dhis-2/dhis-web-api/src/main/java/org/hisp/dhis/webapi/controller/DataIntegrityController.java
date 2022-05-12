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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dataintegrity.DataIntegrityCheck;
import org.hisp.dhis.dataintegrity.DataIntegrityDetails;
import org.hisp.dhis.dataintegrity.DataIntegrityService;
import org.hisp.dhis.dataintegrity.DataIntegritySummary;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.scheduling.parameters.DataIntegrityJobParameters;
import org.hisp.dhis.scheduling.parameters.DataIntegrityJobParameters.DataIntegrityReportType;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Halvdan Hoem Grelland <halvdanhg@gmail.com>
 */
@Controller
@RequestMapping( "/dataIntegrity" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@AllArgsConstructor
public class DataIntegrityController
{
    private final SchedulingManager schedulingManager;

    private final DataIntegrityService dataIntegrityService;

    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @PostMapping
    @ResponseBody
    public WebMessage runDataIntegrity(
        @RequestParam( required = false ) List<String> checks,
        @CurrentUser User currentUser )
    {
        return runDataIntegrityAsync( checks, currentUser, "runDataIntegrity", DataIntegrityReportType.REPORT )
            .setLocation( "/dataIntegrity/details?checks=" + toChecksList( checks ) );
    }

    private WebMessage runDataIntegrityAsync( Collection<String> checks,
        User currentUser,
        String description,
        DataIntegrityReportType type )
    {
        DataIntegrityJobParameters params = new DataIntegrityJobParameters();
        params.setChecks( toUniformCheckNames( checks ) );
        params.setType( type );
        JobConfiguration config = new JobConfiguration( description, JobType.DATA_INTEGRITY, null,
            params, true, true );
        config.setUserUid( currentUser.getUid() );
        config.setAutoFields();

        if ( !schedulingManager.executeNow( config ) )
        {
            return conflict( "Data integrity check is already running" );
        }
        return jobConfigurationReport( config );
    }

    @GetMapping
    @ResponseBody
    public Collection<DataIntegrityCheck> getAvailableChecks(
        @RequestParam( required = false ) Set<String> checks,
        @RequestParam( required = false ) String section )
    {
        Collection<DataIntegrityCheck> matches = dataIntegrityService
            .getDataIntegrityChecks( toUniformCheckNames( checks ) );
        return section == null || section.isBlank()
            ? matches
            : matches.stream().filter( check -> section.equals( check.getSection() ) ).collect( toList() );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @GetMapping( "/summary" )
    @ResponseBody
    public Map<String, DataIntegritySummary> getSummaries(
        @RequestParam( required = false ) Set<String> checks,
        @RequestParam( required = false, defaultValue = "0" ) long timeout )
    {
        return dataIntegrityService.getSummaries( toUniformCheckNames( checks ), timeout );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @PostMapping( "/summary" )
    @ResponseBody
    public WebMessage runSummariesCheck(
        @RequestParam( required = false ) Set<String> checks,
        @CurrentUser User currentUser )
    {
        return runDataIntegrityAsync( checks, currentUser, "runSummariesCheck", DataIntegrityReportType.SUMMARY )
            .setLocation( "/dataIntegrity/summary?checks=" + toChecksList( checks ) );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @GetMapping( "/details" )
    @ResponseBody
    public Map<String, DataIntegrityDetails> getDetails(
        @RequestParam( required = false ) Set<String> checks,
        @RequestParam( required = false, defaultValue = "0" ) long timeout )
    {
        return dataIntegrityService.getDetails( toUniformCheckNames( checks ), timeout );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @PostMapping( "/details" )
    @ResponseBody
    public WebMessage runDetailsCheck(
        @RequestParam( required = false ) Set<String> checks,
        @CurrentUser User currentUser )
    {
        return runDataIntegrityAsync( checks, currentUser, "runDetailsCheck", DataIntegrityReportType.DETAILS )
            .setLocation( "/dataIntegrity/details?checks=" + toChecksList( checks ) );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @GetMapping( "/{check}/summary" )
    @ResponseBody
    public DataIntegritySummary getSummary( @PathVariable String check,
        @RequestParam( required = false, defaultValue = "0" ) long timeout )
    {
        Set<String> checks = toUniformCheckNames( Set.of( check ) );
        return dataIntegrityService.getSummaries( checks, timeout ).get( checks.iterator().next() );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @GetMapping( "/{check}/details" )
    @ResponseBody
    public DataIntegrityDetails getDetails( @PathVariable String check,
        @RequestParam( required = false, defaultValue = "0" ) long timeout )
    {
        Set<String> checks = toUniformCheckNames( Set.of( check ) );
        return dataIntegrityService.getDetails( checks, timeout ).get( checks.iterator().next() );
    }

    /**
     * Allow both dash or underscore in the API
     */
    private static Set<String> toUniformCheckNames( Collection<String> checks )
    {
        return checks == null
            ? Set.of()
            : checks.stream().map( check -> check.replace( '-', '_' ) ).collect( toUnmodifiableSet() );
    }

    private String toChecksList( Collection<String> checks )
    {
        return checks == null || checks.isEmpty() ? "" : String.join( ",", checks );
    }
}
