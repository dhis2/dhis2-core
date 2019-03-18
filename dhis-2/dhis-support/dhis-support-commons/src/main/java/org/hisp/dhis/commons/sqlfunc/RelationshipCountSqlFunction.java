package org.hisp.dhis.commons.sqlfunc;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

/**
 * Function which counts the number of relationships each tracked entity instance has of a given type.
 *
 * @author Markus Bekken
 */
public class RelationshipCountSqlFunction
    implements SqlFunction
{
    public static final String KEY = "relationshipCount";

    @Override
    public String evaluate( String... args )
    {
        if ( args.length > 1 )
        {
            throw new IllegalArgumentException( "Illegal arguments, expected 1 or 0 arguments." );
        }

        String relationshipIdConstraint = "";
        
        if ( args != null )
        {
            String relationShipId = args[0].replaceAll( "^\"|^'|\"$|'$", "" ).trim();
            if ( relationShipId.length() == 11 )
            {
                relationshipIdConstraint = " join relationshiptype rt on r.relationshiptypeid = rt.relationshiptypeid and rt.uid = '" 
                    + relationShipId + "'";
            }
        }

        return "(select count(*) from relationship r" + relationshipIdConstraint +
                " join relationshipitem rifrom on rifrom.relationshipid = r.relationshipid" +
                " join trackedentityinstance tei on rifrom.trackedentityinstanceid = tei.trackedentityinstanceid and tei.uid = ax.tei)";
    }
    
    public String getSampleValue()
    {
        return "1";
    }
}
