/**
 * Form setup for add and update forms
 */
( function( qs ) {
    jQuery( document ).ready( function() {
        var params = qs( '#params' );
        var focusedTextArea = qs( '#subjectTemplate' );
        var recipientSelector = qs( '#notificationRecipient' );
        var userGroup = qs( '#userGroup' );
        var userGroupContainer = qs( '#userGroupContainer' );

        var programAttributeContainer=qs('#programAttributeContainer');
        var programAttribute = qs('#programAttribute');

        var notificationTrigger = qs( '#notificationTrigger' );
        var daysContainer = qs( '#daysContainer' );
        var days = qs( '#days' );
        var deliveryChannelsContainer = qs( '#deliveryChannelsContainer' );
        var subjectTemplateTextArea = qs( '#subjectTemplate' );
        var messageTemplateTextArea = qs( '#messageTemplate' );
        var saveButton = qs( '#save' );
        var cancelButton = qs( '#cancel' );

        var isUpdate = JSON.parse( qs( '#isUpdate' ).value );

        var templateUid = isUpdate ? qs( '#templateUid' ).value : undefined;

        // Event handlers

        subjectTemplateTextArea.addEventListener( "focus", function( e ) {
            focusedTextArea = subjectTemplateTextArea;
        } );

        messageTemplateTextArea.addEventListener( "focus", function ( e ) {
            focusedTextArea = messageTemplateTextArea;
        } );

        // Click handlers

        recipientSelector.addEventListener( "change", function( e ) {
            var recipient = recipientSelector.value;

            if ( recipient === 'USER_GROUP' ) {
                userGroupContainer.style.display = 'table-row';
                userGroup.disabled = false;
            } else {
                userGroupContainer.style.display = 'none';
                userGroup.value = "";
                userGroup.disabled = true;
            }

            if ( isExternalRecipient( recipient ) ) {
                deliveryChannelsContainer.style.display = 'table-row';
            } else {
                deliveryChannelsContainer.style.display = 'none';
                clearDeliveryChannels();
            }
        });

        notificationTrigger.addEventListener( "change", function( e ) {
            if ( notificationTrigger.value == 'COMPLETION' ) {
                daysContainer.style.display = 'none';
            } else {
                daysContainer.style.display = 'table-row';
                days.value = undefined;
            }
        } );

        params.addEventListener( "dblclick", function() {
            insertTextCommon( focusedTextArea.id, params.value );
        } );

        cancelButton.addEventListener( "click", returnToListing );

        saveButton.addEventListener( "click", function() {
            var json = formAsJson();
            isUpdate ? update( json ) : save( json );
        } );

        // Internal

        function isExternalRecipient( recipient ) {
            return recipient === 'ORGANISATION_UNIT_CONTACT';
        }

        function returnToListing() {
                window.location.href = 'dataSetNotifications.action';
        }

        function getSelectedDeliveryChannels() {
            return Array.prototype.slice.call(
                document.forms[ 'deliveryChannelsForm' ].elements[ 'deliveryChannels[]' ] )
                .filter( function( cb ) { return cb.checked; } )
                .map( function( cb ) { return cb.value; } );
        }

        function clearDeliveryChannels() {
            Array.prototype.slice.call(
                document.forms[ 'deliveryChannelsForm' ].elements[ 'deliveryChannels[]' ] )
                .forEach( function( cb ) {
                    cb.checked = false;
                } );
        }

        function getUserGroup() {
            var uid = qs( '#userGroup' ).value || undefined;
            return ( uid === undefined ) ? undefined : { 'id' : uid };
        }


        function getScheduledDays() {
            return ( days.value || 0 ) * ( qs( '#daysModifier' ).value );
        }

        function formAsJson() {
            return {
                name : qs( '#name' ).value || '',
                notificationTrigger : qs( '#notificationTrigger' ).value,
                relativeScheduledDays : getScheduledDays(),
                notificationRecipient : qs( '#notificationRecipient' ).value,
                recipientUserGroup : getUserGroup(),
                deliveryChannels : getSelectedDeliveryChannels(),
                subjectTemplate : qs( '#subjectTemplate' ).value,
                messageTemplate : qs( '#messageTemplate' ).value
            };
        }

        function update( json ) {
            jQuery.ajax( {
                url : '../api/dataSetNotificationTemplates/' + templateUid,
                dataType : 'json',
                data : JSON.stringify( json ),
                contentType : ' application/json',
                type : 'PUT'
            } ).done( returnToListing ).fail( onSaveFail );
        }

        function save( json ) {
            jQuery.ajax( {
                url : '../api/dataSetNotificationTemplates',
                dataType : 'json',
                data : JSON.stringify( json ),
                contentType : 'application/json',
                type : 'POST'
            } ).then( function( result ) {
                returnToListing();

            } ).fail( function( jqXhr, textStatus, error ) {
                    onSaveFail( jqXhr, textStatus, error );
                } );
        }


        function onSaveFail( jqXhr, textStatus, error ) {
            var json = jQuery.parseJSON( jqXhr.responseText );
            var errorMessage = json.response.errorReports[0].message;

            setHeaderDelayMessage( errorMessage || ( textStatus + ': ' + error ) );
        }
    } );
} )( document.querySelector.bind( document ) );