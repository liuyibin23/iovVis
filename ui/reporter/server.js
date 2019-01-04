/* eslint-disable no-console */

require('isomorphic-fetch');
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
  let params = req.body;
  let url = '';
  if (params.chartType === 'Line') {
    url = 'http://www.icosky.com/icon/png/Application/3D%20Cartoon%20Icons%20Pack%20III/Adobe%20Bridge.png';
  } else if (params.chartType === 'Pie') {
    url = 'https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png';
  }
  axios.get(url, {
    headers: {
      'Accept': 'image/png'
    },
    responseType: 'arraybuffer'
  })
    .then(response => {
      const outputFilename = 'file.png';
      console.log(response);
      // let bytes = Buffer.from(response.data,'image/png');
      // let bytes = new Blob([response.data],{type: response.headers['content-type']});
      // let bytes = new Buffer([response.data], 'binary');
      let bytes = response.data;
      fs.writeFileSync(outputFilename, bytes);
      res.send('data:image/png;base64,' + bytes.toString('base64'));
    })
    .catch(ex => {
      console.error(ex);
    });
  // .then(function (response) {
  //   console.log(response);
  //   console.log('\n---ok---\n');
  //   console.log('begin send back\n')
  //   res.send(response);
  // })
  // .catch(function (error) {
  //   console.log(error);
  //   res.send(error);
  // });
  // fetch('http://swapi.apis.guru', {
  //   method: 'POST',
  //   headers: {
  //     Accept: 'application/json',
  //     'Content-Type': 'application/json',
  //   },
  //   body: JSON.stringify(req.body),
  // })
  //   .then(response => response.json())
  //   .then(data => res.send(data.data));
});

app.listen(port, err => {
  if (err) {
    console.error('something bad happened', err);
    return;
  }
  console.log(`server is listening on ${port}`);
}).on('error', console.log);