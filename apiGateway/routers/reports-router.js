var express = require('express');
var router = express.Router();
var toks = require('../middleware/token-verifier');
var util = require('./utils');
var multipart = require('connect-multiparty');
var multipartMiddleware = multipart();
const axios = require('axios');
const fs = require('fs');
var request = require('request');

// middleware that is specific to this router
router.use(function timeLog(req, res, next) {
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
      let find = false;
      for (var i = 0; i < resp.data.length; i++)
      {
          let info = resp.data[i];
          if (info.key === 'REPORTS') {
            find = true;
            info.value = JSON.parse(info.value);
            
            util.responData(200, info.value, res);
            break;
          }
      }
      if (!find) {
        util.responData(resp.status, '无数据。', res);
      }
    })
    .catch((err) => {
      util.responErrorMsg(err, res);
    });
})

// 生成报表文件
async function generateReports(hisReprtsData, reportFilePath, req, res, token) {
  // 上传生成的报表文件    ==> 返回上传成功的地址
  var formData = {
    file: fs.createReadStream(reportFilePath),
  };
  let host = 'http://sm.schdri.com:80/';
  let uploadFileHost = host + 'api/file/upload/';
  request.post({ url: uploadFileHost, formData: formData }, function (err, httpResponse, body) {
    if (err) {
      return console.error('upload failed:', err);
    }
    else {
      console.log('Upload successful!  Server responded with:', body);

      if (JSON.parse(body).success) {
        let bodyData = JSON.parse(body)
        let urlPath = host + bodyData.fileId;
        // 保存到属性表
        saveAssetSererScope(hisReprtsData, req.params.id, req.body, urlPath, req, res, token);
      }
      else {
        util.responData(501, '报表文件上传失败。', res);
      }
    }
  });
}

// 保存到属性表
function saveAssetSererScope(hisReprtsData, assetID, reportInfo, urlPath, req, res, token) {
  let host = 'http://sm.schdri.com:80/';
  // http://cf.beidouapp.com:8080/api/plugins/telemetry/ASSET/265c7510-1df4-11e9-b372-db8be707c5f4/SERVER_SCOPE
  let url = util.getAPI() + `plugins/telemetry/ASSET/${assetID}/SERVER_SCOPE`;
  //let bodyData = JSON.parse(body)
  let fileInfo = req.files.report_file;
  let str = [{
    "report_name": reportInfo.report_name,
    "report_type": reportInfo.report_type,
    "report_date": reportInfo.report_date,
    "report_url": urlPath
  }
  ];

  hisReprtsData.data.forEach(info => {
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

  axios.post(url, (data), { headers: { "X-Authorization": token } })
    .then(response => {
      util.responData(200, '成功创建报表并关联到资产。', res);
    })
    .catch(err => {
      util.responErrorMsg(err, res);
    });
}

function PostReports(hisReprtsData, req, res) {
  let token = req.headers['x-authorization'];
  let fileInfo = req.files.report_file;
  // 根据发送过来的报表模板 ==> 生成报表文件
  generateReports(hisReprtsData, fileInfo.path, req, res, token);
}

//POST
router.post('/:id', multipartMiddleware, async function (req, res) {
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
      util.responErrorMsg(err, res);
    });
})

//DELETE
function processDeleteReq(resp, req, res, token)
{
  var info = null;
  // 遍历，查找REPORTS属性
  for (var i = 0; i < resp.data.length; i++)
  {
    info = resp.data[i];
    if (info.key === 'REPORTS')
    {
        break;
    }
  }
  
  // 遍历REPORTS属性，删除匹配的
  let find = false;
  let new_value = new Array();
  let report_url = null;
  if (info)
  {
    let jsonVal = JSON.parse(info.value);
    for (var i = 0; i < jsonVal.length; i++)
    {
      let _dt = jsonVal[i];
      if (_dt.report_name === req.query.reportemplateName)
      {
        find = true;
        report_url = _dt.report_url;
      }
      else
      {
        new_value.push(_dt);
      }
    }
  }

  if (find && report_url)
  {
    // 从文件服务器删除
    let host = 'http://sm.schdri.com:80/';
    let deleteFileHost = host + 'api/file/delete/';
    let filePath = report_url.substr(host.length); 
    request.post({ url:deleteFileHost, form:{fileId:filePath} }, function (err, httpResponse, body) {
    if (err) {
      util.responData(501, '删除资产的报表失败。', res);
    }
    else
    {
      let result = JSON.parse(body);
      if (result.success)
      {
        // 更新删除后的属性
        let val = JSON.stringify(new_value);
        let data = {
          "REPORTS": `${val}`
        };
        let url = util.getAPI() + `plugins/telemetry/ASSET/${req.params.id}/SERVER_SCOPE`;
        axios.post(url, (data), { headers: { "X-Authorization":token } })
          .then(response => {
            util.responData(200, '成功删除资产的报表。', res);
          })
          .catch(err => {
            util.responErrorMsg(err, res);
          });
        }
      }
    })
  }
  else
  {
    util.responData(200, '未找到匹配数据。', res);
  }
}

//DELETE
router.delete('/:id', async function (req, res) {
  // 查询属性
  let assetID = req.params.id;
  let token = req.headers['x-authorization'];
  let get_attributes_api = util.getAPI() + `plugins/telemetry/ASSET/${assetID}/values/attributes/SERVER_SCOPE`;

  axios.get(get_attributes_api, {
    headers: {
      "X-Authorization": token
    }
  })
    .then((resp) => {
      processDeleteReq(resp, req, res, token);
    })
    .catch((err) => {
      util.responErrorMsg(err, res);
    });
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