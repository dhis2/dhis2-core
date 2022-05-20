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
package org.hisp.dhis.analytics.shared.visitor.select;

import java.util.List;

import org.hisp.dhis.analytics.shared.Column;
import org.hisp.dhis.analytics.shared.component.element.select.EnrollmentDateValueSelectElement;
import org.hisp.dhis.analytics.shared.component.element.select.EventDateValueElement;
import org.hisp.dhis.analytics.shared.component.element.select.ExecutionDateValueElement;
import org.hisp.dhis.analytics.shared.component.element.select.ProgramEnrollmentFlagElement;
import org.hisp.dhis.analytics.shared.component.element.select.SimpleSelectElement;
import org.hisp.dhis.analytics.shared.component.element.select.TeaValueSelectElement;

/**
 * Visitor for 'select' section element of sql statement
 *
 * @author dusan bernat
 */
public interface SelectVisitor
{
    /**
     * see Visitor design pattern
     *
     * @param element
     */
    void visit( TeaValueSelectElement element );

    /**
     * see Visitor design pattern
     *
     * @param element
     */
    void visit( ProgramEnrollmentFlagElement element );

    /**
     * see Visitor design pattern
     *
     * @param element
     */
    void visit( EnrollmentDateValueSelectElement element );

    /**
     * see Visitor design pattern
     *
     * @param element
     */
    void visit( ExecutionDateValueElement element );

    /**
     * see Visitor design pattern
     *
     * @param element
     */
    void visit( EventDateValueElement element );

    /**
     * see Visitor design pattern
     *
     * @param element
     */
    void visit( SimpleSelectElement element );

    /**
     * Visitor produced iteratively all columns for select part of sql statement
     *
     */
    List<Column> getColumns();
}
