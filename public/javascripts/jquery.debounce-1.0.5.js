/**
 * Debounce and throttle function's decorator plugin 1.0.5
 *
 * Copyright (c) 2009 Filatov Dmitry (alpha@zforms.ru)
 * Dual licensed under the MIT and GPL licenses:
 * http://www.opensource.org/licenses/mit-license.php
 * http://www.gnu.org/licenses/gpl.html
 *
 */
!function(n){n.extend({debounce:function(n,t,e,u){3==arguments.length&&"boolean"!=typeof e&&(u=e,e=!1);var o;return function(){var r=arguments;u=u||this,e&&!o&&n.apply(u,r),clearTimeout(o),o=setTimeout(function(){!e&&n.apply(u,r),o=null},t)}},throttle:function(n,t,e){var u,o,r;return function(){o=arguments,r=!0,e=e||this,u||!function(){r?(n.apply(e,o),r=!1,u=setTimeout(arguments.callee,t)):u=null}()}}})}(jQuery);