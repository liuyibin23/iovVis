const express = require('express');
const axios = require('axios');
const util = require('./utils');
const router = express.Router();

// define the home page route
router.get('/:assetId', async function (req, res) {
  let assetID = req.params.assetId;
  let token = req.headers['x-authorization'];
  let get_attributes_api = util.getAPI() + `/plugins/telemetry/ASSET/${assetID}/values/attributes/SERVER_SCOPE`;

  axios.get(get_attributes_api, {
    headers: {
      "X-Authorization": token
    }
  })
    .then((resp) => {
      let resMsg = {
        "code": `${resp.status}`,
        info: {}
      };
      resp.data.forEach(info => {
        if (info.key === 'asset_warning_level') {
          // info.value = JSON.parse(info.value);
          // res.status(200).json(info);
          resMsg.info = info;
        }
      });
      res.status(resp.status).json(resMsg);
    })
    .catch((err) => {
      let status = err.response.status
      if (status == 401) {
        let resMsg = {
          "code": `${err.response.status}`,
          "message:": '无授权访问。'
        };
        res.status(err.response.status).json(resMsg);
      }
      else if (status == 500) {
        let resMsg = {
          "code": `${err.response.status}`,
          "message:": '服务器内部错误。'
        };
        res.status(err.response.status).json(resMsg);
      }
      else if (status == 404) {
        let resMsg = {
          "code": `${err.response.status}`,
          "message:": '访问资源不存在。'
        };
        res.status(err.response.status).json(resMsg);
      }
    });
})

module.exports = router