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
/**
 * Created by mark on 11/06/14.
 */
/**
 * Created by Mark Polak on 28/01/14.
 *
 * @description jQuery part of the menu
 *
 * @see jQuery (http://jquery.com)
 * @see jQuery Template Plugin (http://github.com/jquery/jquery-tmpl)
 */
(function ($, undefined) {
    var menu;
    var markup = '',
        selector = 'appsMenu';

    markup += '<li data-id="${id}" data-app-name="${name}" data-app-action="${defaultAction}">';
    markup += '  <a href="${defaultAction}" class="app-menu-item">';
    markup += '    <img src="${icon}" onError="javascript: this.onerror=null; this.src = \'../icons/program.png\';">';
    markup += '    <span>${name}</span>';
    markup += '    <div class="app-menu-item-description"><span class="bold">${name}</span><i class="fa fa-arrows"></i><p>${description}</p></div>';
    markup += '  </a>';
    markup += '</li>';

    $.template('appMenuItemTemplate', markup);

    function renderAppManager(selector) {
        var apps = menu.getOrderedAppList();
        $('#' + selector).html('');
        $('#' + selector).append($('<ul></ul><hr class="app-separator">').addClass('ui-helper-clearfix'));
        $('#' + selector).addClass('app-menu');
        $.tmpl( "appMenuItemTemplate", apps).appendTo('#' + selector + ' ul');

        //Add favorites icon to all the menu items in the manager
        $('#' + selector + ' ul li').each(function (index, item) {
            $(item).children('a').append($('<i class="fa fa-bookmark"></i>'));
        });

        twoColumnRowFix();
    }

    /**
     * Saves the given order to the server using jquery ajax
     *
     * @param menuOrder {Array}
     */
    function saveOrder(menuOrder) {
        if (menuOrder.length !== 0) {
            //Persist the order on the server
            $.ajax({
                contentType:"application/json; charset=utf-8",
                data: JSON.stringify(menuOrder),
                dataType: "json",
                type:"POST",
                url: "../api/menu/"
            }).success(function () {
                    //TODO: Give user feedback for successful save
                }).error(function () {
                    //TODO: Give user feedback for failure to save
                });
        }
    }

    /**
     * Resets the app blocks margin in case of a resize or a sort update.
     * This function adds a margin to the 9th element when the screen is using two columns to have a clear separation
     * between the favorites and the other apps
     *
     * @param event
     * @param ui
     */
    function twoColumnRowFix(event, ui) {
        var self = $('.app-menu ul'),
            elements = $(self).find('li:not(.ui-sortable-helper)');

        elements.each(function (index, element) {
            $(element).css('margin-right', '0px');
            if ($(element).hasClass('app-menu-placeholder')) {
                $(element).css('margin-right', '10px');
            }
            //Only fix the 9th element when we have a small enough screen
            if (index === 8 && (self.width() < 808)) {
                $(element).css('margin-right', '255px');
            }
        });

    }

    /**
     * Render the menumanager and the dropdown menu and attach the update handler
     */
        //TODO: Rename this as the name is not very clear to what it does
    function renderMenu() {
        var options = {
            placeholder: 'app-menu-placeholder',
            connectWith: '.app-menu ul',
            update: function (event, ui) {
                var reorderedApps = $("#" + selector + " ul"). sortable('toArray', {attribute: "data-id"});

                menu.updateOrder(reorderedApps);
                menu.save(saveOrder);
            },
            sort: twoColumnRowFix,
            tolerance: "pointer",
            cursorAt: { left: 55, top: 30 }
        };

        renderAppManager(selector);

        $('.app-menu ul').sortable(options).disableSelection();
    }

    $.when(dhis2.menu.ui.loadingStatus)
        .then(function () {
            menu = dhis2.menu.mainAppMenu.menuItems;
            menu.subscribe(renderMenu);
        });

    /**
     * jQuery events that communicate with the web api
     * TODO: Check the urls (they seem to be specific to the dev location atm)
     */
    $(function () {
        /**
         * Event handler for the sort order box
         */
        $('#menuOrderBy').change(function (event) {
            var orderBy = $(event.target).val();

            menu.displayOrder = orderBy;

            renderMenu();
        });

        /**
         * Check if we need to fix columns when the window resizes
         */
        $(window).resize(twoColumnRowFix);

        $('.drop-down-menu-link').get().forEach(function (element, index, elements) {
            var id = $(element).parent().attr('id'),
                dropdown_menu = $('div#' + id.split('_')[0]);

            function closeAllDropdowns() {
                $('.app-menu-dropdown').each(function () {
                    $(this).attr('data-clicked-open', 'false');
                    $(this).hide();
                });
                hideDropDown();
            }

            $(element).click(function () {
                return function () {
                    var thisDropDownStatus = $(dropdown_menu).attr('data-clicked-open');
                    closeAllDropdowns();

                    if (thisDropDownStatus === 'true') {
                        $(dropdown_menu).attr('data-clicked-open', 'false');
                    } else {
                        $(dropdown_menu).attr('data-clicked-open', 'true');
                        showDropDown(dropdown_menu.attr('id'));
                    }
                }
            }());
        });
    });

})(jQuery);
