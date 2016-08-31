package org.hisp.dhis.api.mobile.model.LWUITmodel;

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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import org.hisp.dhis.api.mobile.model.Model;
import org.hisp.dhis.api.mobile.model.ModelList;
import org.hisp.dhis.api.mobile.model.OptionSet;
import org.hisp.dhis.common.ValueType;

 /**
 * @author Nguyen Kim Lai
 */
public class ProgramStageDataElement extends Model
{
    private String clientVersion;
    
    private String type;
    
    private String numberType;
    
    private boolean compulsory;

    private ModelList categoryOptionCombos;

    private OptionSet optionSet;
    
    private String value;
    
    public static final String TYPE_STRING = "string";

    public static final String TYPE_PHONE_NUMBER = "phoneNumber";

    public static final String TYPE_EMAIL = "email";

    public static final String TYPE_NUMBER = "number";

    public static final String TYPE_LETTER = "letter";

    public static final String TYPE_BOOL = "bool";

    public static final String TYPE_TRUE_ONLY = "trueOnly";

    public static final String TYPE_DATE = "date";

    public static final String TYPE_TRACKER_ASSOCIATE = "trackerAssociate";

    public static final String TYPE_USERS = "users";
    
    @Override
    public void serialize( DataOutputStream dout )
        throws IOException
    {
        super.serialize( dout );
        dout.writeUTF( this.getType() );
        if ( this.getNumberType() != null )
        {    
            dout.writeBoolean( true );
            dout.writeUTF( this.getNumberType() );
        }
        else
        {
            dout.writeBoolean( false );
        }
        dout.writeBoolean( this.isCompulsory() );
        
        List<Model> cateOptCombos = this.getCategoryOptionCombos().getModels();
        if ( cateOptCombos == null || cateOptCombos.size() <= 0 )
        {
            dout.writeInt( 0 );
        }
        else
        {
            dout.writeInt( cateOptCombos.size() );
            for ( Model each : cateOptCombos )
            {
                each.setClientVersion( TWO_POINT_TEN );
                each.serialize( dout );
            }
        }
        
        if ( optionSet == null || optionSet.getOptions().size() <= 0 )
        {
            dout.writeBoolean( false );
        }
        else
        {
            dout.writeBoolean( true );
            optionSet.setClientVersion( TWO_POINT_TEN );
            optionSet.serialize( dout );
        }
        
        if ( this.getValue() == null )
        {
            dout.writeBoolean( false );
        }
        else
        {
            dout.writeBoolean( true );
            dout.writeUTF( this.getValue() );
        }
    }
    
    @Override
    public void deSerialize( DataInputStream dint )
        throws IOException
    {
        super.deSerialize( dint );
        this.setType( dint.readUTF() );
        if( dint.readBoolean() )
        {
            this.setNumberType( dint.readUTF() );
        }
        else
        {
            this.setNumberType( null );
        }
        this.setCompulsory( dint.readBoolean() );
        this.categoryOptionCombos = new ModelList();
        this.categoryOptionCombos.deSerialize( dint );
        
        if( dint.readBoolean() == false )
        {
            this.optionSet = null;
        }
        else
        {
            this.optionSet = new OptionSet();
            this.optionSet.deSerialize( dint );
        }

        if ( dint.readBoolean() == false )
        {
            this.setValue( null );
        }
        else
        {
            this.setValue( dint.readUTF() );
        }
    }
    
    @Override
    public String getClientVersion()
    {
        return clientVersion;
    }

    @Override
    public void setClientVersion( String clientVersion )
    {
        this.clientVersion = clientVersion;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }
    
    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }
    
    public void setType (ValueType type) {
        if ( type == ValueType.BOOLEAN )
        {
            this.setType( TYPE_BOOL );
        }
        else if ( type == ValueType.DATE || type == ValueType.DATETIME )
        {
            this.setType( TYPE_DATE );
        }
        else if ( type == ValueType.EMAIL )
        {
            this.setType( TYPE_EMAIL );
        }
        else if ( type == ValueType.INTEGER || type == ValueType.NUMBER )
        {
            this.setType( TYPE_NUMBER );
        }
        else if ( type == ValueType.LETTER )
        {
            this.setType( TYPE_LETTER );
        }
        else if ( type == ValueType.LONG_TEXT )
        {
            this.setType( TYPE_STRING );
        }
        else if ( type == ValueType.PHONE_NUMBER )
        {
            this.setType( TYPE_PHONE_NUMBER );
        }
        else if ( type == ValueType.TRACKER_ASSOCIATE )
        {
            this.setType( TYPE_TRACKER_ASSOCIATE );
        }
        else if ( type == ValueType.TRUE_ONLY )
        {
            this.setType( TYPE_TRUE_ONLY );
        }
        else if ( type == ValueType.USERNAME )
        {
            this.setType( TYPE_USERS );
        }
        else
        {
            this.setType( TYPE_STRING );
        }
    }

    public boolean isCompulsory()
    {
        return compulsory;
    }

    public void setCompulsory( boolean compulsory )
    {
        this.compulsory = compulsory;
    }

    public ModelList getCategoryOptionCombos()
    {
        return categoryOptionCombos;
    }

    public void setCategoryOptionCombos( ModelList categoryOptionCombos )
    {
        this.categoryOptionCombos = categoryOptionCombos;
    }

    public OptionSet getOptionSet()
    {
        return optionSet;
    }

    public void setOptionSet( OptionSet optionSet )
    {
        this.optionSet = optionSet;
    }

    public String getNumberType()
    {
        return numberType;
    }

    public void setNumberType( String numberType )
    {
        this.numberType = numberType;
    }
    
}
