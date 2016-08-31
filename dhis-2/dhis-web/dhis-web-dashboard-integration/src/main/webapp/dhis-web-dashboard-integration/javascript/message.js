function submitMessage()
{
    userCount().done(function(o) {
        if( o.userCount > 200 ) {
            setHeaderDelayMessage(i18n_selected_more_than_200_user.replace("{userCount}", o.userCount))
        } else {
            $( "#messageForm" ).submit();
        }
    });
}

function removeMessage( id )
{
    removeItem( id, "", i18n_confirm_delete_message, "removeMessage.action" );
}

function toggleRowSelected( element )
{
    $( element ).closest( "tr" ).toggleClass( "list-row-selected", element.checked );
}

function read( id )
{
    window.location.href = "readMessage.action?id=" + id;
}

function userCount() {
    var ignoreTree = $("#ignoreTree").val();
    var recipients = $("#recipients").val();

    return $.ajax({
        url: 'userCount.action',
        type: 'POST',
        method: 'POST',
        data: {
            ignoreTree: ignoreTree,
            recipients: recipients
        }
    });
}

function validateMessage()
{
    var subject = $( "#subject" ).val();
    var text = $( "#text" ).val();

    if( isDefined( selectionTreeSelection ) && $( "#recipients" ).length )
    {
        if( !( selectionTreeSelection.isSelected() || $( "#recipients" ).val().length ) )
        {
            setHeaderMessage( i18n_select_one_or_more_recipients );
            return false;
        }
    }

    if( subject == null || subject.trim().length == 0 )
    {
        setHeaderMessage( i18n_enter_subject );
        return false;
    }

    if( text == null || text.trim().length == 0 )
    {
        setHeaderMessage( i18n_enter_text );
        return false;
    }

    return true;
}

function toggleMetaData( id )
{
    $( "#metaData" + id ).toggle();
}

function sendReply()
{
    var id = $( "#conversationId" ).val();
    var text = $( "#text" ).val();

    if( text == null || text.trim() == '' )
    {
        setHeaderMessage( i18n_enter_text );
        return false;
    }

    $( "#replyButton" ).attr( "disabled", "disabled" );

    setHeaderWaitMessage( i18n_sending_message );

    $.postUTF8( "sendReply.action", { id: id, text: text }, function()
    {
        window.location.href = "readMessage.action?id=" + id;
    } );
}

function toggleFollowUp( id, followUp )
{
    var imageId = "followUp" + id;

    var url = "toggleFollowUp.action?id=" + id;

    $.getJSON( url, function( json )
    {
        var imageSrc = json.message == "true" ? "../images/marked.png" : "../images/unmarked.png";

        $( "#" + imageId ).attr( "src", imageSrc );
    } );
}

function formatItem( item )
{
    if( item.id && item.id.indexOf( "u:" ) != -1 )
    {
        return '<img src="../icons/glyphicons_003_user.png" style="width: 12px; height: 12px; padding-right: 5px;"/>' + item.text;
    }
    else if( item.id && item.id.indexOf( 'ug:' ) != -1 )
    {
        return '<img src="../icons/glyphicons_043_group.png" style="width: 12px; height: 12px; padding-right: 5px;"/>' + item.text;
    }
    else
    {
        return item.text;
    }
}

var messageOperations = ( function() {

    //
    // Private
    //

    var removeMessages = function( messages ) {
        if( typeof messages === "undefined" || messages.length < 1 ) {
            return;
        }

        var confirmed = window.confirm( i18n_confirm_delete_all_selected_messages );

        if( confirmed ) {
            setHeaderWaitMessage( i18n_deleting );

            $.ajax( {
                    url: "../api/messageConversations?" + $.param( { mc: messages }, true ),
                    contentType: "application/json",
                    dataType: "json",
                    type: "DELETE",
                    success: function( response ) {
                        for( var i = 0; i < response.removed.length; i++ ) {
                            $( "#messages" ).find( "[name='" + response.removed[i] + "']" ).remove();
                        }
                        setHeaderDelayMessage( i18n_messages_were_deleted );
                    },
                    error: function( response ) {
                        showErrorMessage( response.message, 3 );
                    }
            } );
        }
    };

    var propertyRegExp = new RegExp( "[read|unread|followup|unfollowup]" );

    /**
     * Workhorse function to mark/unmark messages read, unread, followup or unfollowup.
     *
     * @param messages {array} UID of messages to mark
     * @param property {string} property to mark. String values are read|unread|followup|unfollowup
     */
    var markMessages = function( messages, property ) {
        if( typeof messages === "undefined" || messages.length < 1 ) {
            return;
        }

        if( !_.isString( property ) || !propertyRegExp.test( property ) ) {
            throw "Property string must be set.";
        }

        $.ajax( {
            url: "../api/messageConversations/" + property,
            type: "POST",
            data: JSON.stringify( messages ),
            contentType: "application/json",
            dataType: "json",
            success: function( response ) {
                switch( property ) {
                    case "read":
                        toggleMessagesRead( response.markedRead );
                        break;
                    case "unread":
                        toggleMessagesRead( response.markedUnread );
                        break;
                    case "followup":
                        setFollowupIndicator( response.markedFollowup, true);
                        break;
                    case "unfollowup":
                        setFollowupIndicator( response.unmarkedFollowup, false );
                        break;
                }
            },
            error: function( response ) {
                showErrorMessage( response.message, 3);
            }
        } );
    };

    var setFollowupIndicator = function( messageUids, marked ) {
        var messages = $( "#messages" );
        var imgSrc = marked ? "../images/marked.png" : "../images/unmarked.png";

        for( var i = 0; i < messageUids.length; i++ ) {
            messages.find( "[name='" + messageUids[i] + "'] .followup-icon img" ).attr( "src", imgSrc );
        }
    };

    var toggleMessagesRead = function( messageUids ) {
        var messages = $( "#messages" );

        for( var i = 0; i < messageUids.length; i++ ) {
            messages.find( "[name='" + messageUids[i] + "']" ).toggleClass( "unread bold" );
        }
    };

    //
    // Public
    //
    return {
        unmarkMessagesFollowup: function( messages ) {
            return markMessages( messages, "unfollowup" );
        },
        markMessagesFollowup: function( messages) {
            return markMessages( messages, "followup" );
        },
        markMessagesRead: function( messages ) {
            return markMessages( messages, "read" );
        },
        markMessagesUnread: function( messages ) {
            return markMessages( messages, "unread" );
        },
        removeMessages: removeMessages
    };
})( window );