const express = require('express');
const axios = require('axios');
const fs = require('fs');
const request = require('request');
const util = require('../util/utils');

const router = express.Router();

// middleware that is specific to this router
// router.use(function timeLog(req, res, next) {
//   console.log('Reports Time: ', Date.now());
//   next();
// })
// define the home page route
router.get('/', function (req, res) {
  res.send('Statistics Api home page')
})
// define the about route
router.get('/about', function (req, res) {
  res.send('About reports')
})

function QuerySR(keys, api_sr, data, token, res){
    axios.get(api_sr, {
        headers: {
          "X-Authorization": token
        }
      }).then(resp => {
        let klist = keys.split(',');
        let sr = resp.data[0];
        let respData = [];
        for (let i = 0; i < klist.length; i++){
            let key = klist[i];
            let dt = data[key];
            if (dt) {
                let newValue = Math.ceil(Number.parseFloat(dt[0].value) * Number.parseFloat(resp.data.sr[0].value));                
                let tmp = {'key':key, 'value':newValue};
                respData.push(tmp);
            }
        }

        util.responData(util.CST.OK200, respData, res);
      }).catch(err =>{
        util.responErrorMsg(err, res);
      });
}

//GET STAT
router.get('/:id', function (req, res) {
    let id = req.params.id;
    let token = req.headers['x-authorization'];
    let interval = req.query.endTime - req.query.startTime;
    let keys = req.query.keys;
    let api = util.getAPI() + `plugins/telemetry/DEVICE/${id}/values/timeseries?&keys=${keys}&startTs=${req.query.startTime}&endTs=${req.query.endTime}&agg=COUNT&interval=${interval}`;
    api = encodeURI(api);
    axios.get(api, {
        headers: {
          "X-Authorization": token
        }
      }).then(resp => {
        let api_sr = util.getAPI() + `plugins/telemetry/DEVICE/${id}/values/timeseries?&keys=sr&startTs=${req.query.startTime}&endTs=${req.query.endTime}&limit=1`;
        QuerySR(keys, api_sr, resp.data, token, res);
      }).catch(err =>{
        util.responErrorMsg(err, res);
      });
})

module.exports = router