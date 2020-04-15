/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
require.config({
  paths: {
    'bootstrap-select': '$services.webjars.url("bootstrap-select", "js/bootstrap-select.min")'
  },
  shim: {
    'bootstrap-select' : ['jquery', 'bootstrap']
  }
});

define('xwiki-locale-picker', ['jquery', 'bootstrap-select'], function($) {
  'use strict';

  // Load the required CSS.
  [
    $jsontool.serialize($services.webjars.url('bootstrap-select', 'css/bootstrap-select.min.css'))
  ].forEach(function(url) {
    $('<link rel="stylesheet"/>').attr('href', url).appendTo('head');
  });

  $.fn.localePicker = function(settings) {
    return this.each($.proxy(init, null, settings));
  };

  var defaultSettings = {
    allowEmpty: true,
    multiple: false
  };

  var init = function(settings) {
    var input = $(this);
    var selectedLocales = getLocalesFromInput(input);
    var select = $('<select/>');
    if (settings.multiple) {
      // Hide the input and insert the select after.
      input.hide().after(select.attr('multiple', 'multiple'));
      // Synchronize the value of the hidden input with the selected values from the select widget.
      select.on('changed.bs.select', function (event) {
        input.prop('value', (select.val() || []).join(','));
      });
    } else {
      // Replace the input with the select.
      input.replaceWith(select.attr({
        id: input.attr('id'),
        name: input.attr('name')
      }));
    }
    if (settings.allowEmpty) {
      $('<option/>').attr('value', '').text('None').appendTo(select);
    }
    select.append(locales.map(function(locale) {
      var index = selectedLocales.indexOf(locale.code);
      if (index >= 0) {
        // Remove it from the list of selected locales so that we can add the remaining ones at the end.
        selectedLocales.splice(index, 1);
      }
      return $('<option/>').attr('value', locale.code).html(locale.name).prop('selected', index >= 0);
    }));
    // Add selected locales that are unknown.
    select.append(selectedLocales.map(function(locale) {
      return $('<option/>').attr('value', locale).text(locale).prop('selected', true);
    }));
    select.bootstrapSelect();
  };

  /*!
  #set ($locales = [])
  #set ($currentLocale = $services.localization.currentLocale)
  #foreach ($locale in $collectiontool.sort($services.localization.availableLocales, 'displayName'))
    #if ("$!locale" != '')
      #set ($localeName = $escapetool.xml($stringtool.capitalize($locale.getDisplayName($locale))) +
        ' <small class="text-muted">(' + $stringtool.capitalize($locale.getDisplayName($currentLocale)) + ')</small>')
      #set ($discard = $locales.add({'code': $locale.toString(), 'name': $localeName}))
    #end
  #end
  */
  var locales = $jsontool.serialize($locales);

  /**
   * Get the initial values of the input
   * @return an array of locales codes
   */
  var getLocalesFromInput = function (input) {
    return input.val().split(/\s*,\s*|\s+/).filter(function(locale) {
      return locale !== '';
    });
  };

  $.fn.bootstrapSelect = function() {
    // Force 100% width on the select using the form-control CSS class.
    return this.addClass('form-control').each(function() {
      $(this).selectpicker({
        // Enable filtering if there are a lot of choices.
        liveSearch: $(this).find('option').length > 10
      });
    });
  };
});

