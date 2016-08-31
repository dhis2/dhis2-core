
// -----------------------------------------------------------------------------
// Author:   Torgeir Lorange Ostby, torgeilo@gmail.com
// Version:  $Id: request.js 5910 2008-10-13 02:33:09Z tri $
// -----------------------------------------------------------------------------

/*
 * Usage:
 *
 * function processResponse( response ) { ... }       		// Text or XML
 * function requestFailed( httpStatusCode ) { ... }
 *
 * var request = new Request();
 * request.setResponseTypeXML( 'rootElement' );       		// Optional
 * request.sendAsPost( 'value=1&value=2' );			// Optional
 * request.setCallbackSuccess( processResponse );     		// Optional
 * request.setCallbackError( requestFailed );         		// Optional
 * request.send( 'url.action?value=1' );
 */

function Request()
{
    var request = undefined;
    var responseType = 'TEXT';
    var requestMethod = 'GET';
    var requestParameters = null;
    var rootElementName = undefined;
    var callbackSuccess = undefined;
    var callbackError = undefined;

    this.setResponseTypeXML = function( rootElementName_ )
    {
        responseType = 'XML';
        rootElementName = rootElementName_;
    };
    
    this.sendAsPost = function( requestParameters_ )
    {
        requestMethod = 'POST';
        requestParameters = requestParameters_;
    };

    this.setCallbackSuccess = function( callbackSuccess_ )
    {
        callbackSuccess = callbackSuccess_;
    };
    
    this.setCallbackError = function( callbackError_ )
    {
        callbackError = callbackError_;
    };

    this.send = function( url )
    {
        request = newXMLHttpRequest();

        if ( !request )
        {
            window.alert( "Your browser doesn't support XMLHttpRequest" );
            return;
        }

        request.onreadystatechange = responseReceived;
        request.open( requestMethod, url, true );
        request.setRequestHeader("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");		
        request.send( requestParameters );
    };
	
    function newXMLHttpRequest()
    {
        if ( window.XMLHttpRequest )
        {
            try
            {
                return new XMLHttpRequest();
            }
            catch ( e )
            {
                return false;
            }
        }
        else if ( window.ActiveXObject )
        {
            try
            {
                return new ActiveXObject( 'Msxml2.XMLHTTP' );
            }
            catch ( e )
            {
                try
                {
                    return new ActiveXObject( 'Microsoft.XMLHTTP' );
                }
                catch ( ee )
                {
                    return false;
                }
            }
        }
        
        return false;
    }

    function responseReceived()
    {
        if ( request.readyState == 4 )
        {
            switch( request.status )
            {
                case 200:
                    if ( callbackSuccess )
                    {
                        if ( responseType == 'TEXT' )
                        {
                            callbackSuccess( request.responseText );
                        }
                        else
                        {
                            var xml = textToXML( request.responseText, rootElementName );

                            callbackSuccess( xml );
                        }
                    }
                    break;
                case 204:
                    if ( callbackSuccess )
                    {
                        callbackSuccess( null );
                    }
                    break;
                case 500:
                    var message = 'Operation failed - internal server error';
                
                    var serverMessage = request.responseText;

                    if ( serverMessage )
                    {
                        var maxLength = 512;
                    
                        if ( serverMessage.length > maxLength )
                        {
                            serverMessage = serverMessage.substring( 0, maxLength - 3 ) + '...';
                        }
                    
                        if ( serverMessage.length > 0 )
                        {
                            message += '\n\n' + serverMessage;
                        }
                    }

                    message += '\n\nThe error details are logged';

                    window.alert( message );

                    break;
                default:
                    if ( callbackError )
                    {
                        callbackError( request.status );
                    }
            }
        }
    }

    function textToXML( text, rootElementName )
    {
        var docImpl = document.implementation;
        var parser, dom;

        // For standards compliant browsers
        if ( docImpl && docImpl.createLSParser )
        {
            parser = docImpl.createLSParser( docImpl.MODE_SYNCHRONOUS, null );
            var input = docImpl.createLSInput();
            input.stringData = text;
            return parser.parse( input ).documentElement;
        }

        // For IE
        else if ( window.ActiveXObject )
        {
            dom = new ActiveXObject( 'Microsoft.XMLDOM' );
            dom.async = "false";
            dom.loadXML( text );
            return dom.getElementsByTagName( rootElementName )[0];
        }

        // For Mozilla
        else if ( window.DOMParser )
        {
            parser = new DOMParser();
            dom = parser.parseFromString( text, 'application\/xml' );
            return dom.documentElement;
        }

        // No parsing abilities
        return null;
    }
}
