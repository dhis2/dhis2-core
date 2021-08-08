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
package org.hisp.dhis.analytics.event.data.sql.transform.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;

import org.hisp.dhis.analytics.event.data.sql.transform.model.element.TableElement;

/**
 * @author Dusan Bernat
 */
public class SqlFromElementProvider
{
    public Function<String, List<TableElement>> getProvider()
    {
        return sqlStatement -> {
            List<TableElement> tableElementList = new ArrayList<>();
            try
            {
                Statement select = CCJSqlParserUtil.parse( sqlStatement );
                select.accept( new StatementVisitorAdapter()
                {
                    @Override
                    public void visit( Select select )
                    {
                        select.getSelectBody().accept( new SelectVisitorAdapter()
                        {
                            @Override
                            public void visit( PlainSelect plainSelect )
                            {
                                plainSelect.getFromItem().accept( new FromItemVisitorAdapter()
                                {
                                    @Override
                                    public void visit( Table table )
                                    {
                                        tableElementList
                                            .add( new TableElement( table.getName(), table.getAlias().toString() ) );
                                    }
                                } );
                            }
                        } );
                    }
                } );
                return tableElementList;
            }
            catch ( JSQLParserException e )
            {
                throw new RuntimeException( e.getMessage() );
            }
        };
    }
}
