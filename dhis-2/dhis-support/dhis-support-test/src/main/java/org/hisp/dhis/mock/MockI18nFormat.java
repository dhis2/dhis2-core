package org.hisp.dhis.mock;

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

import java.util.Date;
import java.util.Random;

import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.period.Period;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class MockI18nFormat
    extends I18nFormat
{
    private static final DateTimeFormatter FORMAT = DateTimeFormat.forPattern( "yyyy-MM-dd" );
    
    @Override
    public String formatPeriod( Period period )
    {
        Random random = new Random();        
        return "Period_" + FORMAT.print( new DateTime( period.getStartDate() ) ) + "_" + random.nextInt( 1000 );
    }
    
    @Override
    public String formatDate( Date date )
    {
        Random random = new Random();
        return "Date_"  + FORMAT.print( new DateTime( date ) ) + "_" + random.nextInt( 1000 );
    }
    
    @Override
    public Date parseDate( String string )
    {
        return string != null ? FORMAT.parseDateTime( string ).toDate() : null;
    }
}
