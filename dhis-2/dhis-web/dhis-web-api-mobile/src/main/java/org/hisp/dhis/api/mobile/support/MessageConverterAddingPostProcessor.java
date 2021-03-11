package org.hisp.dhis.api.mobile.support;



import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

@Component
public class MessageConverterAddingPostProcessor
    implements BeanPostProcessor
{
    private HttpMessageConverter<?> messageConverter = new DataStreamSerializableMessageConverter();

    @Override
    public Object postProcessBeforeInitialization( Object bean, String beanName )
        throws BeansException
    {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization( Object bean, String beanName )
        throws BeansException
    {
        if ( !(bean instanceof RequestMappingHandlerAdapter) )
        {
            return bean;
        }

        RequestMappingHandlerAdapter handlerAdapter = (RequestMappingHandlerAdapter) bean;
        List<HttpMessageConverter<?>> converters = handlerAdapter.getMessageConverters();
        converters.add( 0, messageConverter );
        handlerAdapter.setMessageConverters( converters );
        return handlerAdapter;
    }
}
