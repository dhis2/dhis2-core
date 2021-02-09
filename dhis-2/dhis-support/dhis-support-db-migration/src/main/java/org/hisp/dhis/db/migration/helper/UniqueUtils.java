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
package org.hisp.dhis.db.migration.helper;

import java.util.Set;

/**
 * @author Jan Bernitt
 */
public class UniqueUtils
{

    /**
     * Finds a value with the provided maximum length that is not already
     * contained in the provided set of unique values, adds it to the set and
     * returns it.
     *
     * Preconditions are: the provided set does not contain duplicates and the
     * number of entries in the set is smaller than the largest decimal number
     * that has equal or less digits then the maximum length.
     *
     * @param value the value we would like to add to the set given it is
     *        unique, otherwise we try to make it unique
     * @param maxLength the maximum number of characters the value is allowed to
     *        have
     * @param uniques the set of unique values the provided value should be
     *        added to
     * @return the found unique value, which also was added to the set
     */
    public static String addUnique( String value, int maxLength, Set<String> uniques )
    {
        String unique = value;
        if ( unique.length() > maxLength )
        {
            unique = unique.substring( 0, maxLength );
        }
        if ( !uniques.contains( unique ) )
        {
            uniques.add( unique );
            return unique;
        }
        for ( int i = 1; i <= uniques.size() + 1; i++ )
        {
            String nr = String.valueOf( i );
            String attempt = unique.length() + nr.length() <= maxLength
                ? unique + nr
                : unique.substring( 0, maxLength - nr.length() ) + nr;
            if ( !uniques.contains( attempt ) )
            {
                uniques.add( attempt );
                return attempt;
            }
        }
        return unique;
    }

    private UniqueUtils()
    {
        throw new UnsupportedOperationException( "util" );
    }

}
