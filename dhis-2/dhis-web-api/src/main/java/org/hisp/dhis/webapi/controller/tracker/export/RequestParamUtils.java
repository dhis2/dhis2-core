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
package org.hisp.dhis.webapi.controller.tracker.export;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.webapi.common.UID;

/**
 * RequestParamUtils are functions used to parse and transform tracker request
 * parameters. This class is intended to only house functions without any
 * dependencies on services or components.
 */
public class RequestParamUtils
{
    private RequestParamUtils()
    {
        throw new IllegalStateException( "Utility class" );
    }

    /**
     * Helps us transition request parameters that contained semicolon separated
     * UIDs (deprecated) to comma separated UIDs in a backwards compatible way.
     *
     * @param deprecatedParamName request parameter name of deprecated
     *        semi-colon separated parameter
     * @param deprecatedParamUids semicolon separated uids
     * @param newParamName new request parameter replacing deprecated request
     *        parameter
     * @param newParamUids new request parameter uids
     * @return uids from the request parameter containing uids
     * @throws BadRequestException when both deprecated and new request
     *         parameter contain uids
     */
    public static Set<UID> validateDeprecatedUidsParameter( String deprecatedParamName, String deprecatedParamUids,
        String newParamName, Set<UID> newParamUids )
        throws BadRequestException
    {
        Set<String> deprecatedParamParsedUids = parseUids( deprecatedParamUids );
        if ( !deprecatedParamParsedUids.isEmpty() && !newParamUids.isEmpty() )
        {
            throw new BadRequestException(
                String.format(
                    "Only one parameter of '%s' (deprecated; semicolon separated UIDs) and '%s' (comma separated UIDs) must be specified. Prefer '%s' as '%s' will be removed.",
                    deprecatedParamName, newParamName, newParamName, deprecatedParamName ) );
        }

        return !deprecatedParamParsedUids.isEmpty()
            ? deprecatedParamParsedUids.stream().map( UID::of ).collect( Collectors.toSet() )
            : newParamUids;
    }

    /**
     * Helps us transition request parameters from a deprecated to a new one.
     *
     * @param deprecatedParamName request parameter name of deprecated parameter
     * @param deprecatedParam value of deprecated request parameter
     * @param newParamName new request parameter replacing deprecated request
     *        parameter
     * @param newParam value of the request parameter
     * @return value of the one request parameter that is non-empty
     * @throws BadRequestException when both deprecated and new request
     *         parameter are non-empty
     */
    public static UID validateDeprecatedUidParameter( String deprecatedParamName, UID deprecatedParam,
        String newParamName, UID newParam )
        throws BadRequestException
    {
        if ( newParam != null && deprecatedParam != null )
        {
            throw new BadRequestException(
                String.format(
                    "Only one parameter of '%s' and '%s' must be specified. Prefer '%s' as '%s' will be removed.",
                    deprecatedParamName, newParamName, newParamName, deprecatedParamName ) );
        }

        return newParam != null ? newParam : deprecatedParam;
    }

    /**
     * Helps us transition mandatory request parameters from a deprecated to a
     * new one. At least one parameter must be non-empty as the deprecated one
     * was mandatory.
     *
     * @param deprecatedParamName request parameter name of deprecated parameter
     * @param deprecatedParam value of deprecated request parameter
     * @param newParamName new request parameter replacing deprecated request
     *        parameter
     * @param newParam value of the request parameter
     * @return value of the one request parameter that is non-empty
     * @throws BadRequestException when both deprecated and new request
     *         parameter are non-empty
     * @throws BadRequestException when both deprecated and new request
     *         parameter are empty
     */
    public static UID validateMandatoryDeprecatedUidParameter( String deprecatedParamName, UID deprecatedParam,
        String newParamName, UID newParam )
        throws BadRequestException
    {
        UID uid = validateDeprecatedUidParameter( deprecatedParamName, deprecatedParam, newParamName, newParam );

        if ( uid == null )
        {
            throw new BadRequestException(
                String.format( "Required request parameter '%s' is not present", newParamName ) );
        }

        return uid;
    }

    /**
     * Parse semicolon separated string of UIDs.
     *
     * @param input string to parse
     * @return set of uids
     */
    private static Set<String> parseUids( String input )
    {
        return parseUidString( input )
            .collect( Collectors.toSet() );
    }

    private static Stream<String> parseUidString( String input )
    {
        return CollectionUtils.emptyIfNull( TextUtils.splitToSet( input, TextUtils.SEMICOLON ) )
            .stream();
    }
}
