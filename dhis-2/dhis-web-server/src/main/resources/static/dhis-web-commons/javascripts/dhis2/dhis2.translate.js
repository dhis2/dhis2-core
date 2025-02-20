"use strict";
/*
 * Copyright (c) 2004-2014, University of Oslo
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


// Make sure that dhis2 object exists
var dhis2 = dhis2 || {};
dhis2.translate = dhis2.translate || {};

/**
 * Created by Mark Polak on 28/01/14.
 */
(function (translate, undefined) {
    var translationCache = {
            get: function (key) {
                if (this.hasOwnProperty(key))
                    return this[key];
                return key;
            }
        },
        getBaseUrl = (function () {
            var href;

            //Add window.location.origin for IE8
            if (!window.location.origin) {
                window.location.origin = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':' + window.location.port: '');
            }

            href = window.location.origin;

            return function () {
                var urlParts = href.split("/"),
                    baseUrl;

                if (dhis2.settings === undefined || dhis2.settings.baseUrl === undefined) {
                    return "..";
                }

                if (typeof dhis2.settings.baseUrl !== "string") {
                    throw new TypeError("Dhis2 settings: baseUrl should be a string");
                }

                if (urlParts[urlParts.length - 1] !== "") {
                    baseUrl = href + '/' + dhis2.settings.baseUrl;
                } else {
                    urlParts.pop();
                    urlParts.push(dhis2.settings.baseUrl);
                    baseUrl = urlParts.join('/');
                }
                return baseUrl;
            }
        })();

    /**
     * Adds translations to the translation cache (overrides already existing ones)
     *
     * @param translations {Object}
     */
    function  addToCache(translations) {
        var translationIndex;

        for (translationIndex in translations) {
            if (typeof translationIndex === 'string' && translationIndex !== 'get') {
                translationCache[translationIndex] = translations[translationIndex];
            }
        }
    }

    /**
     * Asks the server for the translations of the given {translatekeys} and calls {callback}
     * when a successful response is received.
     *
     * @param translateKeys {Array}
     * @param callback {function}
     */
    function getTranslationsFromServer(translateKeys, callback) {
        var http = new XMLHttpRequest();
        var url = getBaseUrl() + "/api/i18n";
        var keysToTranslate = JSON.stringify(translateKeys);
        http.open("POST", url, true);

        //Send the proper header information along with the request
        http.setRequestHeader("Content-type", "application/json; charset=utf-8");

        http.onreadystatechange = function() {//Call a function when the state changes.
            if(http.readyState == 4 && http.status == 200) {
                addToCache(JSON.parse(http.responseText));
                if (typeof callback === 'function') {
                    callback(translationCache);
                }
            }
        }
        http.send(keysToTranslate);
    }

    /**
     * Translates the given keys in the {translate} array and calls callback when request is successful
     * callback currently gets passed an object with all translations that are in the local cache
     *
     * @param translate {Array}
     * @param callback {function}
     */
    translate.get = function (translate, callback) {
        var translateKeys = [];

        //Only ask for the translations that we do not already have
        translate.forEach(function (text, index, translate) {
            if ( ! (text in translationCache)) {
                translateKeys.push(text);
            }
        });

        if (translateKeys.length > 0) {
            //Ask for translations of the app names
            getTranslationsFromServer(translateKeys, callback);
        } else {
            //Call backback right away when we have everything in cache
            callback(translationCache);
        }

    };

})(dhis2.translate);
