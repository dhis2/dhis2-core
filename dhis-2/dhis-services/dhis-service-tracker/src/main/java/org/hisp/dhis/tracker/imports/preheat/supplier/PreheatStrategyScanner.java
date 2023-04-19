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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.tracker.imports.preheat.supplier.strategy.StrategyFor;

import io.github.classgraph.AnnotationClassRef;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

/**
 * This class is responsible for creating an associative map where the key is
 * the name of a Strategy class and the key is the name of the domain object
 * class to cache (based on the {@see StrategyFor} annotation)
 *
 * @author Luciano Fiandesio
 */
public class PreheatStrategyScanner
{
    public Map<String, String> scanSupplierStrategies()
    {
        Map<String, String> classMap = new HashMap<>();
        final String pkg = getCurrentPackage();
        final String annotation = StrategyFor.class.getName();
        try ( ScanResult scanResult = new ClassGraph()
            .enableClassInfo()
            .acceptPackages( pkg )
            .enableAnnotationInfo()
            .scan() )
        {
            for ( ClassInfo classInfo : scanResult.getClassesWithAnnotation( annotation ) )
            {
                classMap.put( getTargetClass( classInfo, annotation ), classInfo.getSimpleName() );
            }
        }
        return classMap;
    }

    private String getCurrentPackage()
    {
        return this.getClass().getPackage().getName();
    }

    private String getTargetClass( ClassInfo classInfo, String annotation )
    {
        AnnotationInfo annotationInfo = classInfo.getAnnotationInfo( annotation );

        AnnotationClassRef klazz = (AnnotationClassRef) annotationInfo.getParameterValues().get( 0 ).getValue();

        return klazz.getClassInfo().getSimpleName();
    }
}
