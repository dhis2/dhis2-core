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
 * Simple plugin for keeping two <select /> elements in sync.
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
!(function( $, window, document, undefined ) {
  var methods = {
    create: function( options ) {
      var context = {};
      $.extend(context, $.fn.selected.defaults, options);

      if( context.target === undefined ) {
        $.error('selected: Missing options.target, please add your target box either as a jqEl or as a query.');
      } else if( context.url === undefined ) {
        $.error('selected: Missing options.url, please give URL of where to find the source data.');
      } else if( !$.isFunction(context.handler) ) {
        $.error('selected: Invalid options.handler.');
      }

      // pass-through if jqEl, query if string
      context.source = this;
      context.target = $(context.target);
      context.search = $(context.search);

      if( !(context.source instanceof $) ) {
        $.error('selected: Invalid source.');
      } else if( !(context.target instanceof $) ) {
        $.error('selected: Invalid target.');
      }

      context.source.data('selected', context);
      context.target.data('selected', context);

      context.source.on('dblclick', 'option', context.defaultSourceDblClickHandler);
      context.target.on('dblclick', 'option', context.defaultTargetDblClickHandler);
      context.source.on('scroll', context.makeScrollHandler(context));

      context.source.on('move-all', function() {
        context.defaultLoader(context).then(function() {
          context.source.find('option').attr('selected', 'selected').trigger('dblclick');
          context.page = undefined;
        });
      });

      context.target.on('move-all', function() {
        context.target.find('option').attr('selected', 'selected').trigger('dblclick');
      });

      if( context.search instanceof $ ) {
        context.search.on('keypress', context.makeSearchHandler(context));
        context.searchButton = $("#" + context.search.attr('id') + "Button");

        context.searchButton.on('click', function() {
          context.search.trigger({type: 'keypress', which: 13, keyCode: 13});
        });
      }

      context.page = 1;
      context.defaultProgressiveLoader(context);
    }
  };

  methods.defaultMethod = methods.create;

  // method dispatcher
  $.fn.selected = function( method ) {
    var args = Array.prototype.slice.call(arguments, 1);

    if( $.isFunction(methods[method]) ) {
      return methods[method].apply(this, args);
    } else if( $.isPlainObject(method) || $.type(method) === 'undefined' ) {
      return methods.defaultMethod.apply(this, arguments);
    } else {
      $.error('selected: Unknown method');
    }
  };

  $.fn.selected.defaults = {
    iterator: 'objects',
    handler: function( item ) {
      return $('<option/>').val(item.id).text(item.displayName);
    },
    defaultMoveSelected: function( sel ) {
      $(sel).find(':selected').trigger('dblclick');
    },
    defaultMoveAll: function( sel ) {
      $(sel).trigger('move-all');
    },
    defaultSourceDblClickHandler: function() {
      var $this = $(this);
      var $selected = $this.parent().data('selected');

      if( $selected === undefined ) {
        $.error('selected: Invalid source.parent, does not contain selected object.');
      }

      $this.removeAttr('selected');
      $selected.target.append($this);
    },
    defaultTargetDblClickHandler: function() {
      var $this = $(this);
      var $selected = $this.parent().data('selected');

      if( $selected === undefined ) {
        $.error('selected: Invalid target.parent, does not contain selected object.');
      }

      $this.removeAttr('selected');
      $selected.source.append($this);
    },
    makeSearchHandler: function( context ) {
      return function( e ) {
        if( e.keyCode == 13 ) {
          context.page = 1;
          context.like = $(this).val();
          context.defaultProgressiveLoader(context);
          e.preventDefault();
          return false;
        }
      }
    },
    makeScrollHandler: function( context ) {
      return function() {
        if( context.source[0].offsetHeight + context.source.scrollTop() >= context.source[0].scrollHeight ) {
          context.defaultProgressiveLoader(context);
        }
      }
    },
    defaultProgressiveLoader: function( context ) {
      if( context.page === undefined ) {
        return;
      }

      var request = {
        url: context.url,
        data: {
          paging: true,
          pageSize: 100 + context.target.children().length,
          page: context.page,
          translate: true,
          fields: 'id,displayName'
        },
        dataType: 'json'
      };

      if( context.like !== undefined && context.like.length > 0 ) {
        request.data.filter = 'name:like:' + context.like;
      }

      context.searchButton.find('i').removeClass('fa-search');
      context.searchButton.find('i').addClass('fa-spinner fa-spin');
      context.searchButton.attr('disabled', true);

      return $.ajax(request).done(function( data ) {
        if( data.pager ) {
          if( data.pager.page == 1 ) {
            context.source.children().remove();
          }

          context.page++;
        }

        if( typeof data.pager === 'undefined' ) {
          context.source.children().remove();
        }

        if( typeof data.pager === 'undefined' || context.page > data.pager.pageCount ) {
          delete context.page;
        }

        if( data[context.iterator] === undefined ) {
          return;
        }

        $.each(data[context.iterator], function() {
          if( context.target.find('option[value=' + this.id + ']').length == 0 ) {
            context.source.append(context.handler(this));
          }
        });
      }).fail(function() {
        context.source.children().remove();
      }).always(function() {
        context.searchButton.find('i').removeClass('fa-spinner fa-spin');
        context.searchButton.find('i').addClass('fa-search');
        context.searchButton.removeAttr('disabled');
      });
    },
    defaultLoader: function( context ) {
      context.source.children().remove();

      var request = {
        url: context.url,
        data: {
          paging: false,
          translate: true,
          fields: 'id,displayName'
        },
        dataType: 'json'
      };

      if( context.like !== undefined && context.like.length > 0 ) {
        request.data.filter = 'name:like:' + context.like;
      }

      context.searchButton.find('i').removeClass('fa-search');
      context.searchButton.find('i').addClass('fa-spinner fa-spin');
      context.searchButton.attr('disabled', true);

      return $.ajax(request).done(function( data ) {
        if( data[context.iterator] === undefined ) {
          return;
        }

        $.each(data[context.iterator], function() {
          if( context.target.find('option[value=' + this.id + ']').length == 0 ) {
            context.source.append(context.handler(this));
          }
        });
      }).fail(function() {
        context.source.children().remove();
      }).always(function() {
        context.searchButton.find('i').removeClass('fa-spinner fa-spin');
        context.searchButton.find('i').addClass('fa-search');
        context.searchButton.removeAttr('disabled');
      });
    }
  };

})(jQuery, window, document);
