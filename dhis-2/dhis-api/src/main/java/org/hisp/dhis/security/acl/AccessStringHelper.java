package org.hisp.dhis.security.acl;

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

/**
 * Currently only the two first positions in the access string are used - rw.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class AccessStringHelper
{
    public enum Permission
    {
        READ( 'r', 0 ), WRITE( 'w', 1 );

        private char value;

        private int position;

        Permission( char value, int position )
        {
            this.value = value;
            this.position = position;
        }

        public char getValue()
        {
            return value;
        }

        public int getPosition()
        {
            return position;
        }
    }

    private char[] access = DEFAULT.toCharArray();

    public static final String DEFAULT = "--------";

    public static final String READ = AccessStringHelper.newInstance()
        .enable( Permission.READ )
        .build();

    public static final String WRITE = AccessStringHelper.newInstance()
        .enable( Permission.WRITE )
        .build();

    public static final String READ_WRITE = AccessStringHelper.newInstance()
        .enable( Permission.READ )
        .enable( Permission.WRITE )
        .build();

    public AccessStringHelper()
    {
    }

    public AccessStringHelper( char[] access )
    {
        this.access = access;
    }

    public AccessStringHelper( String access )
    {
        this.access = access.toCharArray();
    }

    public static AccessStringHelper newInstance()
    {
        return new AccessStringHelper();
    }

    public static AccessStringHelper newInstance( char[] access )
    {
        return new AccessStringHelper( access );
    }

    public AccessStringHelper enable( Permission permission )
    {
        access[permission.getPosition()] = permission.getValue();

        return this;
    }

    public AccessStringHelper disable( Permission permission )
    {
        access[permission.getPosition()] = '-';

        return this;
    }

    public String build()
    {
        return new String( access );
    }

    public String toString()
    {
        return build();
    }

    public static boolean canRead( String access )
    {
        return isEnabled( access, Permission.READ );
    }

    public static boolean canWrite( String access )
    {
        return isEnabled( access, Permission.WRITE );
    }

    public static boolean canReadAndWrite( String access )
    {
        return isEnabled( access, Permission.WRITE ) && isEnabled( access, Permission.READ );
    }

    public static boolean canReadOrWrite( String access )
    {
        return isEnabled( access, Permission.WRITE ) || isEnabled( access, Permission.READ );
    }

    public static boolean isEnabled( String access, Permission permission )
    {
        return access != null && access.charAt( permission.getPosition() ) == permission.getValue();
    }
}
