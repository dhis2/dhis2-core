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
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */

dhis2.util.namespace('dhis2.contextmenu');
dhis2.util.namespace('dhis2.contextmenu.utils');

/**
 * Creates a resolver to search within a certain scope
 * @param scope Scope to search within
 * @returns Function
 */
dhis2.contextmenu.utils.findInScope = function( scope ) {
  return function( fnName ) {
    if( typeof scope[fnName] !== 'function' ) {
      throw new Error('target-fn \'' + fnName + '\' does not point to a valid function.')
    }

    return scope[fnName];
  }
};

dhis2.contextmenu.defaultOptions = {
  listId: 'list',
  menuId: 'menu',
  menuItemActiveClass: 'menuItemActive',
  listItemProps: ['id', 'uid', 'name', 'type'],
  functionResolver: dhis2.contextmenu.utils.findInScope(window)
};

dhis2.contextmenu.config = dhis2.contextmenu.defaultOptions;

dhis2.contextmenu.makeContextMenu = function( options ) {
  dhis2.contextmenu.config = $.extend({}, dhis2.contextmenu.defaultOptions, options);
  var config = dhis2.contextmenu.config;

  var $list = $('#' + config.listId);
  window.$menu = $('#' + config.menuId);
  var $menuItems = $menu.find('ul');

  // make sure that all old event handler are removed (with .context namespace)
  $(document).off('click.context');
  $list.off('click.context');
  $menuItems.off('click.context');

  $menuItems.on('click.context', 'li', function( e ) {
    var context = {};

    $.each(config.listItemProps, function( idx, val ) {
      context[val] = $menu.data(val);
    });

    var $target = $(e.target).closest('a');
    var targetFn = $target.data('target-fn');
    var fn = config.functionResolver(targetFn);

    dhis2.contextmenu.disable(false);
    $menu.attr('fn-active', true);
    fn(context);

    return false;
  });

  $list.on('click.context', 'tr', function( e ) {
    if( dhis2.contextmenu.disable() ) {
      return false;
    }

    var $target = $(e.target);

    if( $target.data('id') === undefined ) {
      $target = $target.closest('tr');
    }

    $target.addClass(config.menuItemActiveClass);

    $.each(config.listItemProps, function( idx, val ) {
      $menu.data(val, $target.data(val));
    });

    $menu.find('ul > li').each(function( idx, val ) {
      var $val = $(val);
      var enabledProperty = $val.data('enabled');

      if( enabledProperty ) {
        $target.data(enabledProperty) ? $val.show() : $val.hide();
      }
    });

    var menuHeight = $menu.height();
    var menuWidth = $menu.width();
    var winHeight = $(window).height();
    var winWidth = $(window).width();

    var pageX = e.pageX;
    var pageY = e.pageY;

    $menu.show();

    if( (menuWidth + pageX) > winWidth ) {
      pageX -= menuWidth;
    }

    if( (menuHeight + pageY) > winHeight ) {
      pageY -= menuHeight;

      if( pageY < 0 ) {
        pageY = e.pageY;
      }
    }

    $menu.css({left: pageX, top: pageY});

    return false;
  });

  $(document).on('click.context', function() {
    if( !dhis2.contextmenu.visibleUiDialog() ) {
      dhis2.contextmenu.disable();
      $menu.removeData('id');
    }
  });

  $(document).keyup(function( e ) {
    if( e.keyCode == 27 ) {
      if( !dhis2.contextmenu.visibleUiDialog() ) {
        dhis2.contextmenu.disable();
      }
    }
  });
};

dhis2.contextmenu.visibleUiDialog = function() {
  if( $('.ui-dialog').is(':visible') ) {
    return true;
  }

  if( Boolean($menu.attr('fn-active')) ) {
    $menu.removeAttr('fn-active');
    return true;
  }

  return false;
};

dhis2.contextmenu.disable = function( clearHighlight ) {
  clearHighlight = $.type(clearHighlight) === 'boolean' ? clearHighlight : true;

  var config = dhis2.contextmenu.config;
  var $list = $('#' + config.listId);
  var $menu = $('#' + config.menuId);

  if( clearHighlight ) {
    $list.find('tr').removeClass(config.menuItemActiveClass);
    $list.find('td').removeClass(config.menuItemActiveClass);
  }

  if( $menu.is(":visible") ) {
    $menu.hide();
    return true;
  }

  return false;
};
