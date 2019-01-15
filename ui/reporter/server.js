/* eslint-disable no-console */

// require('isomorphic-fetch');
const node_echarts = require('node-echarts');
const path = require('path');
const express = require('express');
const bodyParser = require('body-parser');
const axios = require('axios');
const fs = require('fs');

const app = express();
const port = 30000;

app.use(bodyParser.json());
app.use(express.static(path.join(__dirname, 'public')));

app.get('/', (req, res) => {
  res.sendFile('index.html');
});

app.post('/api/v1/report', (req, res, next) => {
  let option = {};
  let params = req.body;
  let tok = req.headers['x-authorization'];
  axios.get('http://cf.beidouapp.com:8080/api/user/tokenAccessEnabled', { headers: { "X-Authorization": tok } })
    .then(ret => {
      if (ret.data) {
        switch (params.chartType) {
          case 'area':
            chart_data = require('./echarts/area');
            option = chart_data.fillData(params.deviceId, params.chartTsw, tok);
            break;
          case 'pie':
            option = require('./echarts/pie');
            break
          default:
        }
        let config = {
          width: params.chartW ? params.chartW * 100 : 1200, // Image width, type is number.
          height: params.chartH ? params.chartH * 100 : 600, // Image height, type is number.
          option: option, // Echarts configuration, type is Object.
          //If the path  is not set, return the Buffer of image.
          // path:  '', // Path is filepath of the image which will be created.
          enableAutoDispose: true  //Enable auto-dispose echarts after the image is created.
        }
        let bytes = node_echarts(config);
        if (bytes)
          res.send('data:image/png;base64,' + bytes.toString('base64'));
        else {
          let err = new Error('ERROR: IMG GEN FAILED.');
          err.statusCode = 501;
          next(err);
        }
      }
    })
    .catch(err => {
      err.statusCode = err.response.status;
      next(err);
    });
});

app.use(function(err, req, res, next) {
  console.error(err.message); // Log error message in our server's console
  if (!err.statusCode) err.statusCode = 500; // If err has no specified error code, set error code to 'Internal Server Error (500)'
  res.status(err.statusCode).send(err.message); // All HTTP requests must have a response, so let's send back an error with its status code and message
});

app.listen(port, err => {
  if (err) {
    console.error('something bad happened', err);
    return;
  }
  console.log(`server is listening on ${port}`);
}).on('error', console.log);