/* eslint-disable no-console */

require('isomorphic-fetch');
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

app.post('/swapi', (req, res) => {
  let option = {};
  let params = req.body;
  switch (params.chartType) {
    case 'area':
      option = require('./echarts/area');
      break;
    case 'pie':
      option = require('./echarts/pie');
      break
    default:
  }
  console.log(params);
  let config = {
    width: params.chartW ? params.chartW * 100 : 1200, // Image width, type is number.
    height: params.chartH ? params.chartH *100 : 600, // Image height, type is number.
    option: option, // Echarts configuration, type is Object.
    //If the path  is not set, return the Buffer of image.
    // path:  '', // Path is filepath of the image which will be created.
    enableAutoDispose: true  //Enable auto-dispose echarts after the image is created.
  }
  let bytes = node_echarts(config);
  if (bytes)
    res.send('data:image/png;base64,' + bytes.toString('base64'));
  else
    console.error('ERROR: IMG GEN FAILED.\n');
  // let url = '';
  // if (params.chartType === 'Line') {
  //   url = 'http://www.icosky.com/icon/png/Application/3D%20Cartoon%20Icons%20Pack%20III/Adobe%20Bridge.png';
  // } else if (params.chartType === 'Pie') {
  //   url = 'https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png';
  // }
  // axios.get(url, {
  //   headers: {
  //     'Accept': 'image/png'
  //   },
  //   responseType: 'arraybuffer'
  // })
  //   .then(response => {
  //     const outputFilename = 'file.png';
  //     console.log(response);
  //     let bytes = response.data;
  //     fs.writeFileSync(outputFilename, bytes);
  //     res.send('data:image/png;base64,' + bytes.toString('base64'));
  //   })
  //   .catch(ex => {
  //     console.error(ex);
  //   });
});

app.listen(port, err => {
  if (err) {
    console.error('something bad happened', err);
    return;
  }
  console.log(`server is listening on ${port}`);
}).on('error', console.log);