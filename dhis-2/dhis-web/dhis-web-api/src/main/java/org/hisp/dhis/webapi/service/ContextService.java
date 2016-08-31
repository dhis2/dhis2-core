package org.hisp.dhis.webapi.service;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface ContextService
{
    /**
     * Get full path of servlet.
     *
     * @return Full HREF to servlet
     * @see javax.servlet.http.HttpServletRequest
     */
    String getServletPath();

    /**
     * Get HREF to context.
     *
     * @return Full HREF to context (context root)
     * @see javax.servlet.http.HttpServletRequest
     */
    String getContextPath();

    /**
     * Get HREF to Web-API.
     *
     * @return Full HREF to Web-API
     * @see javax.servlet.http.HttpServletRequest
     */
    String getApiPath();

    /**
     * Get active HttpServletRequest
     *
     * @return HttpServletRequest
     */
    HttpServletRequest getRequest();

    /**
     * Returns a list of values from a parameter, if the parameter doesn't exist, it will
     * return a empty list.
     *
     * @param name Parameter to get
     * @return List of parameter values, or empty if not found
     */
    Set<String> getParameterValues( String name );

    /**
     * Get all parameters as a map of key => values, supports more than one pr key (so values is a collection)
     */
    Map<String, List<String>> getParameterValuesMap();
}
