package org.hisp.dhis.db.migration.v34;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.salt.RandomSaltGenerator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * This class is to fix the jira issue DHIS2-7442. The steps are:
 * 1) Look for TrackedEntityInstance's AttributeValues that are encrypted.
 * 2) Check if encrypted value can be decrypted using the user defined password in dhis.conf file.
 * 3) If (2) failed then try to decrypt value using the default password that was added in 2.33.0
 * 4) If (3) success then encrypt value again using the correct password from dhis.conf file.
 */
public class V2_34_22__Fix_encryption_issue_for_TEI_attributeValues
    extends BaseJavaMigration
{
    private static final Log log = LogFactory.getLog( V2_34_22__Fix_encryption_issue_for_TEI_attributeValues.class );

    @Override
    public void migrate( Context context ) throws Exception
    {
        String userDefinedPassword = context.getConfiguration().getPlaceholders().get( "encryption.password" );

        if ( StringUtils.isEmpty( userDefinedPassword ) )
        {
            return;
        }

        StandardPBEStringEncryptor encryptor = initEncryptor();
        encryptor.setPassword( userDefinedPassword );

        StandardPBEStringEncryptor encryptorWithDefaultPassword = initEncryptor();
        encryptorWithDefaultPassword.setPassword( "J7GhAs287hsSQlKd9g5" );

        String selectEncryptedValueQuery = "select trackedentityinstanceid, trackedentityattributeid, encryptedvalue from trackedentityattributevalue where encryptedvalue is not null;";
        String updateEncryptedValueQuery = "update trackedentityattributevalue set encryptedvalue=? where trackedentityinstanceid=? and trackedentityattributeid=?;";

        try ( Statement statement = context.getConnection().createStatement() )
        {
            try ( ResultSet resultSet = statement.executeQuery( selectEncryptedValueQuery ) )
            {
                try ( PreparedStatement preparedStatement = context.getConnection().prepareStatement( updateEncryptedValueQuery ) )
                {
                    while ( resultSet.next() )
                    {
                        long teiId = resultSet.getLong( 1 );
                        long attributeId = resultSet.getLong( 2 );
                        String value = resultSet.getString( 3 );

                        /*
                         * Try to decrypt value using user defined password in dhis.conf
                         */
                        boolean canDecrypt = true;
                        try
                        {
                            encryptor.decrypt( value );
                        }
                        catch ( EncryptionOperationNotPossibleException ex )
                        {
                            canDecrypt = false;
                        }

                        if ( canDecrypt )
                        {
                            continue;
                        }

                        /*
                         * Couldn't decrypt value using user defined password,
                         * try to decrypt it using the default password added in 2.33.0
                         */
                        String decryptedValue;
                        try
                        {
                            decryptedValue = encryptorWithDefaultPassword.decrypt( value );
                        }
                        catch ( EncryptionOperationNotPossibleException ex )
                        {
                            log.error(
                                "Flyway java migration error: Failed to decrypt TrackedEntityInstance AttributeValue.",
                                ex );
                            throw new FlywayException( "Failed to decrypt TrackedEntityInstance AttributeValue." );
                        }

                        /*
                         * Encrypt value again using the correct password
                         */
                        try
                        {
                            preparedStatement.setString( 1, encryptor.encrypt( decryptedValue ) );
                            preparedStatement.setLong( 2, teiId );
                            preparedStatement.setLong( 3, attributeId );
                            preparedStatement.addBatch();
                        }
                        catch ( EncryptionOperationNotPossibleException ex )
                        {
                            log.error(
                                "Flyway java migration error: Failed to encrypt TrackedEntityInstance AttributeValue.",
                                ex );
                            throw new FlywayException( "Failed to encrypt TrackedEntityInstance AttributeValue." );
                        }
                    }

                    preparedStatement.executeBatch();
                }
            }
        }
    }

    private StandardPBEStringEncryptor initEncryptor()
    {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setAlgorithm( "PBEWITHSHA256AND128BITAES-CBC-BC" );
        encryptor.setSaltGenerator( new RandomSaltGenerator() );
        return encryptor;
    }
}