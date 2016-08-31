package org.hisp.dhis.system.util;

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
 * (INCLUDING NEGLIGENCE OR OTHERWISE) AR
 * ISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @author Torgeir Lorange Ostby
 */
public class SystemUtils
{
    private static final int FACTOR_MB = 1024 * 1024;

    /**
     * Indicates whether the current thread is running for testing.
     * @return true if test run.
     */
    public static boolean isTestRun()
    {
        return "true".equals( System.getProperty( "org.hisp.dhis.test", "false" ) );
    }
    
    /**
     * Gets the number of CPU cores available to this JVM.
     * @return the number of available CPU cores.
     */
    public static int getCpuCores()
    {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Gets a String containing info of available and used memory of this JVM.
     * @return an info string.
     */
    public static String getMemoryString()
    {
        return "Mem Total in JVM: " + ( Runtime.getRuntime().totalMemory() / FACTOR_MB ) + 
            " Free in JVM: " + ( Runtime.getRuntime().freeMemory() / FACTOR_MB ) +
            " Max Limit: " + ( Runtime.getRuntime().maxMemory() / FACTOR_MB );     
    }
}
