"use strict";

/*
 * Copyright (c) 2004-2013, University of Oslo
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

(function ( $, window, document, undefined ) {
    dhis2.storage.InMemoryAdapter = function ( options ) {
        this.storage = {};

        if ( !(this instanceof dhis2.storage.InMemoryAdapter) ) {
            return new dhis2.storage.InMemoryAdapter( options );
        }

        Object.defineProperties( this, {
            'name': {
                value: options.name,
                enumerable: true
            },
            'version': {
                value: options.version,
                enumerable: true
            },
            'objectStoreNames': {
                value: options.objectStores,
                enumerable: true
            },
            'keyPath': {
                value: options.keyPath,
                enumerable: true
            }
        } );

        this.Indexer = function ( name, objectStore, storage ) {
            return {
                key: name + '.' + objectStore + '.**index**',

                all: function () {
                    var a = storage[ this.key ];

                    if ( a ) {
                        try {
                            a = JSON.parse( a );
                        } catch ( e ) {
                            a = null;
                        }
                    }

                    if ( a == null ) {
                        storage[this.key] = JSON.stringify( [] );
                    }

                    return JSON.parse( storage[ this.key ] );
                },

                add: function ( key ) {
                    var a = this.all();
                    a.push( key );
                    storage[this.key] = JSON.stringify( a );
                },

                remove: function ( key ) {
                    var a = this.all();

                    if ( a.indexOf( key ) != -1 ) {
                        dhis2.array.remove( a, a.indexOf( key ), a.indexOf( key ) );
                        storage[this.key] = JSON.stringify( a );
                    }
                },

                find: function ( key ) {
                    var a = this.all();
                    return a.indexOf( key );
                },

                // warning: will just delete index, no check
                destroy: function () {
                    delete storage[this.key];
                }
            }
        };

        return this;
    };

    Object.defineProperties( dhis2.storage.InMemoryAdapter.prototype, {
        'open': {
            value: function () {
                var self = this;
                var deferred = $.Deferred();

                self.indexer = {};

                $.each( self.objectStoreNames, function ( idx, item ) {
                    self.indexer[item] = self.Indexer( self.name, item, self.storage );
                } );

                deferred.resolveWith( self );
                return deferred.promise();
            },
            enumerable: true
        },
        'set': {
            value: function ( store, object ) {
                var self = this;

                if ( typeof object === 'undefined' || typeof object[self.keyPath] === 'undefined' ) {
                    throw new Error( dhis2.storage.INVALID_OBJECT );
                }

                object = JSON.parse( JSON.stringify( object ) );

                var key = object[self.keyPath];
                delete object[self.keyPath];

                key = this.name + '.' + store + '.' + key;
                if ( this.indexer[store].find( key ) == -1 ) this.indexer[store].add( key );

                var deferred = $.Deferred();

                try {
                    this.storage[ key ] = JSON.stringify( object );
                    deferred.resolveWith( self, [ object] );
                } catch ( err ) {
                    deferred.rejectWith( self, [ err ] );
                }

                return deferred.promise();
            },
            enumerable: true
        },
        'setAll': {
            value: function ( store, arr ) {
                var self = this;
                var deferred = $.Deferred();

                $.each( arr, function ( idx, item ) {
                    self.set( store, item );
                } );

                deferred.resolveWith( this );

                return deferred.promise();
            },
            enumerable: true
        },
        'get': {
            value: function ( store, key ) {
                var self = this;

                if ( typeof key === 'undefined' ) {
                    throw new Error( dhis2.storage.INVALID_KEY, key );
                }

                var deferred = $.Deferred();
                var object = this.storage[this.name + '.' + store + '.' + key];

                if ( object ) {
                    object = JSON.parse( object );
                    object[this.keyPath] = key;
                }

                deferred.resolveWith( self, [ object ] );
                return deferred.promise();
            },
            enumerable: true
        },
        'getAll': {
            value: function ( store, predicate ) {
                var self = this;
                var deferred = $.Deferred();
                var idx = this.indexer[store].all();
                var objects = [];
                var filtered = typeof predicate === 'function';

                if ( filtered ) {
                    // just log and continue
                    console.log( 'predicate filtering is currently not supported in dom storage getAll, returning all' );
                }

                for ( var i = 0, len = idx.length; i < len; i++ ) {
                    var object = this.storage[ idx[i] ];

                    if ( object ) {
                        object = JSON.parse( object );
                        object[this.keyPath] = idx[i].replace( self.name + '.' + store + '.', '' );
                        objects.push( object );
                    }
                }

                deferred.resolveWith( self, [ objects ] );
                return deferred.promise();
            },
            enumerable: true
        },
        'getKeys': {
            value: function ( store ) {
                var self = this;
                var deferred = $.Deferred();

                var keys = this.indexer[store].all().map( function ( r ) {
                    return r.replace( self.name + '.' + store + '.', '' )
                } );

                deferred.resolveWith( self, [ keys ] );
                return deferred.promise();
            },
            enumerable: true
        },
        'remove': {
            value: function ( store, key ) {
                var self = this;

                if ( typeof key === 'undefined' ) {
                    throw new Error( dhis2.storage.INVALID_KEY, key );
                }

                var deferred = $.Deferred();

                key = this.name + '.' + store + '.' + key;
                this.indexer[store].remove( key );
                delete this.storage[key ];

                deferred.resolveWith( self );
                return deferred.promise();
            },
            enumerable: true
        },
        'removeAll': {
            value: function ( store ) {
                var self = this;
                var deferred = $.Deferred();

                this.getKeys( store ).done( function ( keys ) {
                    $.each( keys, function ( idx, item ) {
                        self.remove( store, item );
                    } );

                    deferred.resolveWith( self );
                } );

                return deferred.promise();
            },
            enumerable: true
        },
        'contains': {
            value: function ( store, key ) {
                var self = this;

                if ( typeof key === 'undefined' ) {
                    throw new Error( dhis2.storage.INVALID_KEY, key );
                }

                key = this.name + '.' + store + '.' + key;
                var deferred = $.Deferred();
                deferred.resolveWith( self, [ this.indexer[store].find( key ) != -1 ] );

                return deferred.promise();
            },
            enumerable: true
        },
        'count': {
            value: function ( store, key ) {
                var self = this;

                if ( typeof key !== 'undefined' ) {
                    throw new Error( 'key based count is not supported by InMemoryAdapter' );
                }

                var deferred = $.Deferred();
                deferred.resolveWith( self, [ this.indexer[store].all().length ] );
                return deferred.promise();
            },
            enumerable: true
        },
        'close': {
            value: function () {
                var deferred = $.Deferred();
                deferred.resolve();
                return deferred.promise();
            },
            enumerable: true
        },
        'destroy': {
            value: function () {
                var self = this;
                var deferred = $.Deferred();

                $.each( self.objectStoreNames, function ( idx, item ) {
                    self.removeAll( item );
                    self.indexer[item].destroy();
                } );

                deferred.resolve();
                return deferred.promise();
            },
            enumerable: true
        }
    } );

    Object.defineProperties( dhis2.storage.InMemoryAdapter, {
        'adapterName': {
            value: 'InMemoryAdapter',
            enumerable: true
        },
        'isSupported': {
            value: function () {
                return true;
            },
            enumerable: true
        }
    } );
})( jQuery, window, document );
