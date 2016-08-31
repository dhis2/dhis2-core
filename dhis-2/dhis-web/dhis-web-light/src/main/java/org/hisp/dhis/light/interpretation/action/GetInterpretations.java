package org.hisp.dhis.light.interpretation.action;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.hisp.dhis.analytics.AnalyticsFavoriteType;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.interpretation.InterpretationService;

import com.opensymphony.xwork2.Action;

/**
 * @author Paul Mark Castillo
 */
public class GetInterpretations
    implements Action, Comparator<Interpretation>
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private InterpretationService interpretationService;

    public void setInterpretationService( InterpretationService interpretationService )
    {
        this.interpretationService = interpretationService;
    }

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private List<Interpretation> interpretations;

    public List<Interpretation> getInterpretations()
    {
        return interpretations;
    }

    public void setInterpretations( List<Interpretation> interpretations )
    {
        this.interpretations = interpretations;
    }

    // -------------------------------------------------------------------------
    // Action Implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        List<Interpretation> tempInterpretations = interpretationService.getInterpretations();

        List<Interpretation> finalInterpretations = new ArrayList<>();

        Iterator<Interpretation> i = tempInterpretations.iterator();

        while ( i.hasNext() )
        {
            Interpretation currentInterpretation = i.next();

            if ( currentInterpretation.getType() == AnalyticsFavoriteType.CHART )
            {
                finalInterpretations.add( currentInterpretation );
            }
        }

        Collections.sort( finalInterpretations, this );

        setInterpretations( finalInterpretations );

        return SUCCESS;
    }

    @Override
    public int compare( Interpretation o1, Interpretation o2 )
    {
        long time1 = o1.getCreated().getTime();
        long time2 = o2.getCreated().getTime();

        if ( time1 > time2 )
        {
            return -1;
        }
        else if ( time2 < time1 )
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }
}
