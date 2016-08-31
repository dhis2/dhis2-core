package org.hisp.dhis.security;

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
 * This interface provides access to the system configured password hashing method.
 * It is used for encoding passwords and tokens as well as checking the authenticity
 * of a given password or token. The underlying PasswordEncoder should be the same as
 * used by Spring Security to perform password checking on user authentication.
 *
 * @author Torgeir Lorange Ostby
 * @author Halvdan Hoem Grelland
 */
public interface PasswordManager
{
    String ID = PasswordManager.class.getName();

    /**
     * Cryptographically hash a password.
     * Salting (as well as the salt storage scheme) must be handled by the implementation.
     *
     * @param password password to encode.
     * @return the hashed password.
     */
    String encode( String password );

    /**
     * Determines whether the supplied password equals the encoded password or not.
     * Fetching and handling of any required salt value must be done by the implementation.
     *
     * @param rawPassword the raw, unencoded password.
     * @param encodedPassword the encoded password to match against.
     * @return true if the passwords match, false otherwise.
     */
    boolean matches( String rawPassword, String encodedPassword );

    /**
     * Returns the class name of the password encoder used by this instance.
     * @return the name of the password encoder class.
     */
    String getPasswordEncoderClassName();
}
