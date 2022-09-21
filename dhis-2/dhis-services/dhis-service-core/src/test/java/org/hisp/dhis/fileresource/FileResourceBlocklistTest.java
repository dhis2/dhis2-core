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
package org.hisp.dhis.fileresource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author Lars Helge Overland
 */
public class FileResourceBlocklistTest
{
    @Test
    public void testValid()
    {
        FileResource frA = new FileResource( "My_Checklist.pdf", "application/pdf", 324, "",
            FileResourceDomain.DATA_VALUE );
        FileResource frB = new FileResource( "Evaluation.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 541, "",
            FileResourceDomain.MESSAGE_ATTACHMENT );
        FileResource frC = new FileResource( "FinancialReport.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 143, "",
            FileResourceDomain.DATA_VALUE );

        assertTrue( FileResourceBlocklist.isValid( frA ) );
        assertTrue( FileResourceBlocklist.isValid( frB ) );
        assertTrue( FileResourceBlocklist.isValid( frC ) );
    }

    @Test
    public void testInvalid()
    {
        FileResource frA = new FileResource( "Click_Me.exe", "application/x-ms-dos-executable", 451, "",
            FileResourceDomain.DATA_VALUE );
        FileResource frB = new FileResource( "evil_script.sh", "application/pdf", 125, "",
            FileResourceDomain.MESSAGE_ATTACHMENT ); // Fake content type
        FileResource frC = new FileResource( "cookie_stealer", "text/javascript", 631, "",
            FileResourceDomain.USER_AVATAR ); // No file extension
        FileResource frD = new FileResource( "malicious_software.msi", null, 235, "", FileResourceDomain.USER_AVATAR ); // No
                                                                                                                        // content
                                                                                                                        // type

        assertFalse( FileResourceBlocklist.isValid( frA ) );
        assertFalse( FileResourceBlocklist.isValid( frB ) );
        assertFalse( FileResourceBlocklist.isValid( frC ) );
        assertFalse( FileResourceBlocklist.isValid( frD ) );
        assertFalse( FileResourceBlocklist.isValid( null ) );
    }
}
