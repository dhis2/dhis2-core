package org.hisp.dhis.interpretation;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.reporttable.ReportTable;

import java.util.Date;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
public interface InterpretationService
{
    int saveInterpretation( Interpretation interpretation );

    Interpretation getInterpretation( int id );

    Interpretation getInterpretation( String uid );

    void updateInterpretation( Interpretation interpretation );

    void deleteInterpretation( Interpretation interpretation );

    List<Interpretation> getInterpretations();

    List<Interpretation> getInterpretations( Date lastUpdated );

    List<Interpretation> getInterpretations( int first, int max );

    InterpretationComment addInterpretationComment( String uid, String text );

    void updateCurrentUserLastChecked();

    long getNewInterpretationCount();

    /**
     * Adds a like to the given interpretation for the current user. This method
     * will have a "repeatable read" transaction isolation level to ensure an
     * atomic increment of the like count interpretation property.
     * 
     * @param id the interpretation id.
     * @return true if the current user had not already liked the interpretation.
     */
    boolean likeInterpretation( int id );

    /**
     * Removes a like from the given interpretation for the current user. This method
     * will have a "repeatable read" transaction isolation level to ensure an
     * atomic decrease of the like count interpretation property.
     * 
     * @param id the interpretation id.
     * @return true if the current user had previously liked the interpretation.
     */
    boolean unlikeInterpretation( int id );
    
    int countMapInterpretations( Map map );

    int countChartInterpretations( Chart chart );

    int countReportTableInterpretations( ReportTable reportTable );
    
    Interpretation getInterpretationByChart( int id );
}
