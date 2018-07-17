package org.hisp.dhis.fileresource;

import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Transactional
public class DefaultMessageAttachmentService
    implements MessageAttachmentService
{

    private IdentifiableObjectStore<MessageAttachment> messageAttachmentStore;

    public void setMessageAttachmentStore( IdentifiableObjectStore<MessageAttachment> messageAttachmentStore )
    {
        this.messageAttachmentStore = messageAttachmentStore;
    }

    @Override
    public MessageAttachment getMessagettachment( String uid )
    {
        return messageAttachmentStore.getByUid( uid );
    }

    @Override
    public void saveMessageAttachment( MessageAttachment messageAttachment )
    {
        messageAttachmentStore.save( messageAttachment );
    }

    @Override
    public void linkAttachments( Set<MessageAttachment> attachments, Message message )
    {
        if ( attachments == null )
        {
            return;
        }

        message.setAttachments( attachments );

        attachments.stream()
            .forEach( attachment ->
            {
                attachment.setMessage( message );
                attachment.getAttachment().setAssigned( true );
            });
    }
}
