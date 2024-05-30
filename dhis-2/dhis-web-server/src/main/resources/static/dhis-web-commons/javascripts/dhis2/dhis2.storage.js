"use strict";

/*
 * Copyright (c) 2004-2014, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of the HISP project nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
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

dhis2.util.namespace( 'dhis2.storage' );

dhis2.storage.DATABASE_IS_NOT_OPEN = "Database is not open, please call .open() on your store before using it.";
dhis2.storage.INVALID_KEY = "No valid key was provided.";
dhis2.storage.INVALID_OBJECT = "No valid object was provided";

dhis2.storage.Store = function( options ) {
    var self = this;

    if( !(this instanceof dhis2.storage.Store) ) {
        return new dhis2.storage.Store( options );
    }

    if( typeof options.name === 'undefined' ) {
        throw Error( 'Constructor needs a valid database name as a argument' );
    }

    if( typeof options.objectStores === 'undefined' || !$.isArray( options.objectStores ) || options.objectStores.length == 0 ) {
        throw Error( 'Constructor needs a valid objectStores array as a argument' );
    }

    options.keyPath = options.keyPath || 'id';
    options.version = options.version || 1;

    if( !JSON ) throw 'JSON unavailable! Include http://www.json.org/json2.js to fix.';

    var objectStores = {};
    var ObjectStoreAdapters = {};
    var defaultAdapterName;

    if( typeof options.adapters !== 'undefined' ) {
        for( var i = 0, len = options.adapters.length; i < len; i++ ) {
            if( dhis2.storage.Store.verifyAdapter( options.adapters[i] ) && options.adapters[i].isSupported() ) {
                ObjectStoreAdapters[options.adapters[i].adapterName] = options.adapters[i];
                defaultAdapterName = options.adapters[i].adapterName;
                objectStores[defaultAdapterName] = [];
                break;
            }
        }
    }

    $.each( options.objectStores, function( idx, item ) {
        if( typeof item === 'object' ) {
            if( typeof item.adapters !== 'undefined' && typeof item.name !== 'undefined' ) {
                for( var i = 0, len = item.adapters.length; i < len; i++ ) {
                    if( dhis2.storage.Store.verifyAdapter( item.adapters[i] ) && item.adapters[i].isSupported() ) {
                        ObjectStoreAdapters[item.adapters[i].adapterName] = item.adapters[i];

                        if( defaultAdapterName === item.adapters[i].adapterName ) {
                            objectStores[defaultAdapterName].push( item.name );
                        } else {
                            if( typeof objectStores[item.adapters[i].adapterName] === 'undefined' ) {
                                objectStores[item.adapters[i].adapterName] = [];
                            }

                            objectStores[item.adapters[i].adapterName].push( item.name );
                        }
                        break;
                    }
                }
            }
        } else {
            objectStores[defaultAdapterName].push( item );
        }
    } );

    if( Object.keys( ObjectStoreAdapters ).length == 0 ) throw 'Could not find a valid adapter.';

    var objectStoreAdapters = {};

    // simple map from objectStoreName => adapter
    var objectStoreCache = {};

    $.each( Object.keys( ObjectStoreAdapters ), function( idx, item ) {
        options.objectStores = objectStores[item];

        objectStoreAdapters[item] = new ObjectStoreAdapters[item]( options );

        $.each( options.objectStores, function( idx, objectStore ) {
            objectStoreCache[objectStore] = objectStoreAdapters[item];
        } );
    } );

    Object.defineProperty( self, 'objectStoreAdapters', {
        value: objectStoreAdapters,
        enumerable: true
    } );

    var adapterMethods = "open set setAll get getAll getKeys count contains remove removeAll close destroy".split( ' ' );

    $.each( adapterMethods, function( idx, item ) {
        Object.defineProperty( self, item, {
            value: function() {
                // if arguments is empty, apply to all objectStore adapters also
                if( arguments.length == 0 ) {
                    // TODO add deferred chain

                    var deferred1 = $.Deferred();
                    var deferred2 = $.Deferred();
                    var promise = deferred2.promise();

                    $.each( self.objectStoreAdapters, function( idx, adapter ) {
                        promise = promise.then( function() {
                            return adapter[item].apply( adapter, arguments );
                        } );
                    } );

                    promise = promise.then( function() {
                        deferred1.resolveWith( self );
                    } );

                    deferred2.resolve();

                    return deferred1.promise();
                } else if( typeof arguments[0] === 'string' && typeof objectStoreCache[arguments[0]] !== 'undefined' ) {
                    var adapter = objectStoreCache[arguments[0]];
                    return adapter[item].apply( adapter, arguments );
                }

                return self.objectStoreAdapters[defaultAdapterName][item].apply( self.objectStoreAdapters[defaultAdapterName], arguments );
            },
            enumerable: true
        } );
    } );
};

dhis2.storage.Store.adapterMethods = "open set setAll get getAll getKeys count contains remove removeAll close destroy".split( ' ' );

dhis2.storage.Store.verifyAdapter = function( Adapter ) {
    var failed = [];

    if( typeof Adapter === 'undefined' ) {
        return false;
    }

    $.each( dhis2.storage.Store.adapterMethods, function( idx, item ) {
        // should probably go up the prototype chain here
        var descriptor = Object.getOwnPropertyDescriptor( Adapter, item ) || Object.getOwnPropertyDescriptor( Adapter.prototype, item );

        if( typeof descriptor.value !== 'function' ) {
            failed.push( item );
        }
    } );

    return failed.length === 0;
};
