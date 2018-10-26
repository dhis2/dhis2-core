package org.hisp.dhis.db.migration.helper;

import java.util.ArrayList;
import java.util.List;

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
 * Generates sql insert scripts for object translations. Refactored from legacy TableAlteror startup routine.
 * 
 * @author Ameen Mohamed
 *
 */
public class ObjectTranslationHelper
{

    public static List<String> getObjectTranslationScripts( String className, String translationTable, String objectTable, String objectId )
    {
        List<String> scripts = new ArrayList<>();

        scripts.add( " insert into objecttranslation ( objecttranslationid, locale , property , value )  " + " select t.translationid, t.locale,  "
            + " case when t.objectproperty = 'shortName' then 'SHORT_NAME' " + " when t.objectproperty = 'formName' then 'FORM_NAME'   "
            + " when t.objectproperty = 'name' then 'NAME'  " + " when t.objectproperty = 'description' then'DESCRIPTION'" + " else t.objectproperty "
            + " end ," + " t.value " + " from  translation as t " + " where t.objectclass = '" + className + "'" + " and t.objectproperty is not null "
            + " and t.locale is not null " + " and t.value is not null "
            + " and not exists ( select 1 from objecttranslation where objecttranslationid = t.translationid )  " + " and ( " + " exists ( select 1 from "
            + objectTable + "  where " + objectId + " = t.objectid )  " + " or  exists ( select 1 from " + objectTable + " where uid  = t.objectuid ) "
            + " ) ;" );

        scripts.add( " insert into " + translationTable + " ( " + objectId + ", objecttranslationid ) " + " select "
            + " case when t.objectid is not null then t.objectid " + " else ( select " + objectId + " from " + objectTable + " where uid = t.objectuid ) "
            + " end," + " o.objecttranslationid  "
            + " from objecttranslation o inner join translation t on o.objecttranslationid = t.translationid and t.objectclass = '" + className + "'"
            + " and not exists ( select 1 from " + translationTable + " where objecttranslationid = o.objecttranslationid) ;" );
       
        return scripts;
    }

}
