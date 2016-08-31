/**
 * @license Copyright (c) 2003-2013, CKSource - Frederico Knabben. All rights reserved.
 * For licensing, see LICENSE.html or http://ckeditor.com/license
 */

/**
 * DHIS2 Config for CKEditor 4.4.2
 */

CKEDITOR.editorConfig = function( config ) {
  /* Remove unneeded plugins */
  config.removePlugins = 'flash, iframe, smiley, save, templates';

  /**
   * Disable Advanced Content Filter (ACF).
   *
   * Note that enabling ACF will break any custom forms which use HTML tags or includes scripts (though a more involved
   * configuration can be written to allow specific content to pass the filter). Consider yourself warned.
   */
  config.allowedContent = true;

};
