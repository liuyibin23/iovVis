var express = require('express')
var router = express.Router()


// define the home page route
var defaultRouter = function (req, res) {
  res.send('ET Portal API gateway');
}

module.exports = defaultRouter