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

import org.hisp.dhis.common.Pager;
import org.hisp.dhis.schema.Schema;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface LinkService
{
    /**
     * Generate next/prev links for Pager class. Needs to know which class we are generating
     * the pager for, so it can fetch the endpoint.
     *
     * @param pager Pager instance to update with prev/next links
     * @param klass Class type which is paged
     * @see org.hisp.dhis.common.Pager
     */
    void generatePagerLinks( Pager pager, Class<?> klass );

    /**
     * Generate HREF and set it using reflection, required a setHref(String) method in your class.
     * <p/>
     * Uses hrefBase from ContextService.getServletPath().
     *
     * @param object   Object (can be collection) to set HREFs on
     * @param deepScan Generate links also on deeper levels (only one level down)
     * @see javax.servlet.http.HttpServletRequest
     * @see ContextService
     */
    <T> void generateLinks( T object, boolean deepScan );

    /**
     * Generate HREF and set it using reflection, required a setHref(String) method in your class.
     *
     * @param object   Object (can be collection) to set HREFs on
     * @param hrefBase Used as starting point of HREF
     * @param deepScan Generate links also on deeper levels (only one level down)
     * @see javax.servlet.http.HttpServletRequest
     */
    <T> void generateLinks( T object, String hrefBase, boolean deepScan );

    void generateSchemaLinks( List<Schema> schemas );

    void generateSchemaLinks( Schema schema );

    void generateSchemaLinks( Schema schema, String hrefBase );
}
