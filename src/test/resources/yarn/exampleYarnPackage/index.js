/*
 * Math functions on shapes
 */

'use strict';

// For package depenency demonstration purposes only
var multiply = require('lodash/multiply');

module.exports = {
    area_rectangle: function (width, height) {
        return multiply(height, width);
    }
}
