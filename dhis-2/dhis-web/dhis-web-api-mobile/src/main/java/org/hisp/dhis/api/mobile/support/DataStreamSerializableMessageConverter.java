package org.hisp.dhis.api.mobile.support;



import java.io.IOException;
import java.util.List;

import org.hisp.dhis.api.mobile.model.DataStreamSerializable;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class DataStreamSerializableMessageConverter
    implements HttpMessageConverter<DataStreamSerializable>
{
    @Override
    public boolean canRead( Class<?> clazz, MediaType mediaType )
    {
        if ( mediaType == null )
        {
            return DataStreamSerializable.class.isAssignableFrom( clazz );
        }
        else
        {
            return MediaTypes.MEDIA_TYPES.contains( mediaType )
                && DataStreamSerializable.class.isAssignableFrom( clazz );
        }
    }

    @Override
    public boolean canWrite( Class<?> clazz, MediaType mediaType )
    {
        if ( mediaType == null )
        {
            return DataStreamSerializable.class.isAssignableFrom( clazz );
        }
        else
        {
            return MediaTypes.MEDIA_TYPES.contains( mediaType )
                && DataStreamSerializable.class.isAssignableFrom( clazz );
        }
    }

    @Override
    public List<MediaType> getSupportedMediaTypes()
    {
        return MediaTypes.MEDIA_TYPES;
    }

    @Override
    public DataStreamSerializable read( Class<? extends DataStreamSerializable> clazz, HttpInputMessage inputMessage )
        throws IOException, HttpMessageNotReadableException
    {
        return DataStreamSerializer.read( clazz, inputMessage.getBody() );

    }

    @Override
    public void write( DataStreamSerializable entity, MediaType contentType, HttpOutputMessage outputMessage )
        throws IOException, HttpMessageNotWritableException
    {
        outputMessage.getHeaders().setContentType( contentType );
        DataStreamSerializer.write( entity, outputMessage.getBody() );
    }
}
