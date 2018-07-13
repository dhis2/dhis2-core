package org.hisp.dhis.fileresource;

import org.hisp.dhis.common.IdentifiableObjectStore;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultMessageAttachmentService
    implements MessageAttachmentService
{

    @Autowired
    private IdentifiableObjectStore<MessageAttachment> store;

    @Override
    public MessageAttachment getMessagettachment( String uid )
    {
        return store.getByUid( uid );
    }

    @Override
    public void saveMessageAttachment( MessageAttachment messageAttachment )
    {
        store.save( messageAttachment );
    }
}
