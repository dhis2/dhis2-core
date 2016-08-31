/*
 * Copyright (c) 2004-2013, University of Oslo
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

/**
 * Checkbox/dropdown combo menu for DHIS 2 Dashboard.
 *
 * @author Halvdan Hoem Grelland
 */
( function ( $ ) {
    /*
     Example markup:
     <div>
     <div></div> <!-- Empty, will be filled with button markup -->
     <ul> <!-- Menu markup -->
     <li data-action="actionA">Item A</li>
     <li data-action="actionB">Item B</li>
     <li data-action="actionC">Item C</li>
     </ul>
     </div>
     ...
     <div id="checkboxes">
     ...
     <input type="checkbox" value="someValue" .... />
     ...
     </div>

     The string parameter given in data-action denotes the name of the
     function which is called on click of the menu item.

     The selected checkboxes' values will be given as an array argument
     to the function when called.

     Usage:
     $( "#myDiv" ).multiCheckboxMenu( $( "#myCheckboxContainer" ), {} );

     */

    function getCheckedValues( $checkboxContainer ) {
        var checked = [];
        $checkboxContainer.find( "input:checkbox:checked" ).each( function() {
            checked.push( this.value );
        });
        return checked;
    }

    function executeNamedFn(fnName, ctx ) {
        var args = [].slice.call(arguments).splice(2);
        var namespaces = fnName.split(".");
        var func = namespaces.pop();
        for( var i = 0; i < namespaces.length; i++ ) {
            ctx = ctx[ namespaces[ i ] ];
        }
        return ctx[func].apply( this, args );
    }

    var multiCheckboxMenu = $.fn.multiCheckboxMenu;

    $.fn.multiCheckboxMenu = function( $checkboxContainer, options ) {

        if( typeof options !== "object" ) {
            options = {};
        }

        var $cb = $( "<input>", { type: "checkbox" } );
        options = $.extend( true, options , {
            checkbox: $cb,
            buttonElements: [
                $( "<span>", { "class": "downArrow" } )
            ],
            menuClass: "multiSelectMenu",
            buttonClass: "multiSelectButton"
        });

        var $checkbox = $( options.checkbox );
        var $slaveCheckboxes = $checkboxContainer.find( "input:checkbox" );

        var $button = $( "<a>", { href: "#" } );
        $button.addClass( options.buttonClass );

        $button.append( $( options.checkbox ) );

        $( options.buttonElements ).each( function() {
            $button.append( $( this ) );
        });

        $( this ).find( "div:first" ).append($button);

        var $menu = $( this ).find( "ul" );
        $menu.addClass( options.menuClass );
        $menu.css( { "visibility" : "hidden", "margin-top" : "6px" } );

        $button.click( function ( event ) {
            $( document ).one( "click", function() {
                $menu.css( "visibility", "hidden" );
            });

            if( $menu.css( "visibility" ) !== "visible" )
            {
                $menu.css( "visibility", "visible" );
            }
            else
            {
                $menu.css( "visibility", "hidden" );
            }
            event.stopPropagation();
        });

        $menu.find( "li" ).each( function() {
            var el = $( this );
            el.action = this.getAttribute( "data-action" );

            if( typeof el.action === "undefined" )
            {
                el.action = function(){};
            }

            el.click( function() {
                return executeNamedFn( el.action, window, getCheckedValues( $checkboxContainer ) );
            });
        });

        $checkbox.click( function( event ) {
            if( this.checked )
            {
                $slaveCheckboxes.attr( "checked", "checked" ).trigger( "change" );
            }
            else
            {
                $slaveCheckboxes.removeAttr( "checked" ).trigger( "change" );
            }
            event.stopPropagation();
        });

        $slaveCheckboxes.click( function() {
            var checked = $slaveCheckboxes.filter( ":checked" );

            if( checked.length < 1 )
            {
                $checkbox.prop( "indeterminate", false );
                $checkbox.removeAttr( "checked" );
            }
            else if( checked.length > 0 && checked.length < $slaveCheckboxes.length )
            {
                $checkbox.removeAttr( "checked" );
                $checkbox.prop( "indeterminate", true );
            }
            else
            {
                $checkbox.prop( "indeterminate", false );
                $checkbox.attr( "checked", "checked" );
            }
        });

        // Cross-browser/OS checkbox alignment fix
        $checkbox.css( { "vertical-align": "middle", "position": "relative", "bottom" : "1px" } );
    };
})( jQuery );
