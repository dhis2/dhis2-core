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
package org.hisp.dhis.dxf2.metadata.objectbundle.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.translation.Translation;
import org.springframework.stereotype.Component;

/**
 * This class contains all validations to be performed on {@link Translation}
 * objects as a part of the validation sequence in MetadataImportService
 *
 * @author viet@dhis2.org
 */
@Component
public class TranslationsCheck implements ObjectValidationCheck
{
    @Override
    public <T extends IdentifiableObject> void check( ObjectBundle bundle, Class<T> klass, List<T> persistedObjects,
        List<T> nonPersistedObjects, ImportStrategy importStrategy, ValidationContext context,
        Consumer<ObjectReport> addReports )
    {
        List<T> objects = selectObjects( persistedObjects, nonPersistedObjects, importStrategy );

        if ( CollectionUtils.isEmpty( objects ) )
        {
            return;
        }

        Schema schema = context.getSchemaService().getDynamicSchema( klass );

        for ( int i = 0; i < objects.size(); i++ )
        {
            run( objects.get( i ), klass, addReports, schema, i );
        }
    }

    public <T extends IdentifiableObject> void run( IdentifiableObject object, Class<T> klass,
        Consumer<ObjectReport> addReports, Schema schema, int index )
    {
        Set<Translation> translations = object.getTranslations();

        if ( CollectionUtils.isEmpty( translations ) )
        {
            return;
        }

        ObjectReport objectReport = new ObjectReport( klass, index );

        if ( !schema.isTranslatable() )
        {
            objectReport.addErrorReport(
                new ErrorReport( Translation.class, ErrorCode.E1107, klass.getSimpleName() ).setErrorKlass( klass ) );
            addReports.accept( objectReport );
            return;
        }

        Set<String> setPropertyLocales = new HashSet<>();

        for ( Translation translation : translations )
        {
            String key = String.join( "_", translation.getProperty(), translation.getLocale() );

            if ( setPropertyLocales.contains( key ) )
            {
                objectReport.addErrorReport(
                    new ErrorReport( Translation.class, ErrorCode.E1106, translation.getProperty(),
                        translation.getLocale() )
                        .setErrorKlass( klass ) );
            }
            else
            {
                setPropertyLocales.add( key );
            }

            if ( translation.getLocale() == null )
            {
                objectReport.addErrorReport(
                    new ErrorReport( Translation.class, ErrorCode.E4000, "locale" ).setErrorKlass( klass ) );
            }

            if ( translation.getProperty() == null )
            {
                objectReport.addErrorReport( new ErrorReport( Translation.class, ErrorCode.E4000, "property" )
                    .setErrorKlass( klass ) );
            }

            if ( translation.getValue() == null )
            {
                objectReport.addErrorReport(
                    new ErrorReport( Translation.class, ErrorCode.E4000, "value" ).setErrorKlass( klass ) );
            }

        }

        if ( objectReport.hasErrorReports() )
        {
            addReports.accept( objectReport );
        }
    }
}
