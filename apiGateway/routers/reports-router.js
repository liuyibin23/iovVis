var express = require('express');
var router = express.Router();
var toks  = require('../middleware/token-verifier');
var util  = require('./utils');
var multipart = require('connect-multiparty');  
var multipartMiddleware = multipart();
const axios = require('axios');

// middleware that is specific to this router
router.use(function timeLog (req, res, next) {
  console.log('Time: ', Date.now());
  next();
})

// middleware that is specific to this router
router.use(function timeLog (req, res, next) {
  console.log('Reports Time: ', Date.now());
  next();
})

// GET
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
      resp.data.forEach(info => {
        if (info.key === 'REPORTS') {
          info.value = JSON.parse(info.value);
          res.status(200).json(info);
        }
      });
    })
    .catch((err) => {
      let status = err.response.status;
      if (status == 401){
        let resMsg = {
          "code":`${err.response.status}`,
          "message:":'无授权访问。'
        };
        res.status(err.response.status).json(resMsg);
      }
      else if (status == 500){
        let resMsg = {
          "code":`${err.response.status}`,
          "message:":'服务器内部错误。'
        };
        res.status(err.response.status).json(resMsg);
      }
      else if (status == 404){
        let resMsg = {
          "code":`${err.response.status}`,
          "message:":'访问资源不存在。'
        };
        res.status(err.response.status).json(resMsg);
      }      
    });
})

function PostReports(resp, req, res)
{
  let token = req.headers['x-authorization'];
  let host = 'http://sm.schdri.com:80/';
  // 保存到属性表
  // http://cf.beidouapp.com:8080/api/plugins/telemetry/ASSET/265c7510-1df4-11e9-b372-db8be707c5f4/SERVER_SCOPE
  let url = util.getAPI() + `plugins/telemetry/ASSET/${req.params.id}/SERVER_SCOPE`;
  //let bodyData = JSON.parse(body)
  let fileInfo = req.files.report_file;
  let str = [{
      "report_name": req.body.report_name,
      "report_url": host + 'testReport.docx'
    }
  ];

  resp.data.forEach(info => {
    if (info.key === 'REPORTS') {
      let data = JSON.parse(info.value);
      data.forEach(_dt => {
        str.push(_dt);
      })      
    }
  });

  let val = JSON.stringify(str);

  let data = {
    "REPORTS": `${val}`
  };

  axios.post(url, (data), { headers: { "X-Authorization":token } })
    .then(response => {
      res.status(response.status).json('成功创建报表并关联到资产。');
    })
    .catch(err => {
      let resMsg = {
        "code":`${err.response.status}`,
        "message:":err.message
      };
      res.status(err.response.status).json(resMsg);
    });
}

//POST
router.post('/:id', multipartMiddleware, async function(req, res){
  let assetID = req.params.id;
  let token = req.headers['x-authorization'];
  let get_attributes_api = util.getAPI() + `/plugins/telemetry/ASSET/${assetID}/values/attributes/SERVER_SCOPE`;

  axios.get(get_attributes_api, {
    headers: {
      "X-Authorization": token
    }
  })
    .then((resp) => {
      PostReports(resp, req, res);
    })
    .catch((err) => {
      let resMsg = {
        "code":`${err.response.status}`,
        "message:":err.message
      };
      res.status(err.response.status).json(resMsg);
    });
})


//DELETE
router.delete('/:id', async function(req, res){
  let msg = 'Delete ID:' + req.params.id + ' Name:' + req.query.reportemplateName;

  res.status(200).json(msg);
})

// define the home page route
router.get('/', function (req, res) {
  res.send('Reports Api home page')
})
// define the about route
router.get('/about', function (req, res) {
  res.send('About reports')
})

module.exports = router