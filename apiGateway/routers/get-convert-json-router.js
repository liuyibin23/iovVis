const express = require('express');
const axios = require('axios');
const fs = require('fs');
const request = require('request');
const util = require('../util/utils');

const router = express.Router();

// define the home page route
router.get('/', function (req, res) {
  res.send('Get device Json Api home page')
})
// define the about route
router.get('/about', function (req, res) {
  res.send('About Get device Json')
})

// 替换null undefine ==> ""
function converValue(value){
  if (value)
    return value;
  
    return "";
}

function converDeviceJson(data, res){
  let dataNew = [];
  for (let i = 0; i < data.length; i++){
    let _dt = data[i];
    let newDt = {
      "client_attrib": _dt.client_attrib,
      "share_attrib": {
        "manufacturer": "",
        "model": "",
        "port": "",
        "sn": "",
        "type": "",
        "ip": "",
        "channel": "",
        "period": "",
        "token": "",
        "group": "",
        "addrnum": ""
      }
    };

    newDt.client_attrib.param = converValue(newDt.client_attrib.param);
    newDt.client_attrib.balance_clear = converValue(newDt.client_attrib.balance_clear);
    newDt.share_attrib.manufacturer = converValue(_dt.share_attrib.manufacturer);
    newDt.share_attrib.model = converValue(_dt.share_attrib.model);
    newDt.share_attrib.port = converValue(_dt.share_attrib.port);
    newDt.share_attrib.sn = converValue(_dt.share_attrib.sn);
    newDt.share_attrib.type = converValue(_dt.share_attrib.type);
    newDt.share_attrib.ip = converValue(_dt.share_attrib.ip);
    newDt.share_attrib.channel = converValue(_dt.share_attrib.channel);
    newDt.share_attrib.period = converValue(_dt.share_attrib.period);
    newDt.share_attrib.token = converValue(_dt.share_attrib.token);
    newDt.share_attrib.group = converValue(_dt.share_attrib.group);
    newDt.share_attrib.addrnum = converValue(_dt.share_attrib.addrnum);

    dataNew.push(newDt);
  }

  util.responData(util.CST.OK200, dataNew, res);
}

router.get('/:id', function (req, res) {
    let assetId = req.params.id;
    let token = req.headers['x-authorization'];
    let api = util.getAPI() + `batchconfig/devices/${assetId}`;
    axios.get(api, {
        headers: {
          "X-Authorization": token
        }
      }).then(resp => {
        converDeviceJson(resp.data, res);
      }).catch(err =>{
        util.responErrorMsg(err, res);
      });
})

module.exports = router