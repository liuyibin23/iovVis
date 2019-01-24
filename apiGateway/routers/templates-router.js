var express = require('express')
var router = express.Router()
var toks = require('../middleware/token-verifier')
var util = require('./utils')
const fs = require('fs')
const axios = require('axios')
var request = require('request')

var multipart = require('connect-multiparty');
var multipartMiddleware = multipart();

// Get asset
async function getAsset(assetId, token) {
  let get_asset_api = util.getAPI() + 'assets?assetIds=' + assetId;
  let assetInfo = await util.getSync(get_asset_api, {
    headers: {
      "X-Authorization": token
    }
  });

  if (!assetInfo) return;

  let len = assetInfo.length;
  let data = new Array();
  if (len > 0) {
    console.log('1. Get asset info:%d', len);
    assetInfo.forEach(info => {
      let _dt = { id: '', name: '', type: '' };
      _dt.id = info.id.id;
      _dt.name = info.name;
      _dt.type = info.type;

      data.push(_dt);
    });

    return data;
  }

  let _dt = { id: '', name: '', type: '' };
  data.push(_dt);
  return data;
}

// http://cf.beidouapp.com:8080/api/plugins/telemetry/ASSET/265c7510-1df4-11e9-b372-db8be707c5f4/values/attributes/SERVER_SCOPE
async function getAttributes(assetId, token) {
  let get_attributes_api = util.getAPI() + `/plugins/telemetry/ASSET/${assetId}/values/attributes/SERVER_SCOPE`;
  let attributesInfo = await util.getSync(get_attributes_api, {
    headers: {
      "X-Authorization": token
    }
  });

  if (!attributesInfo) return;
  let data = new Array();
  let len = attributesInfo.length;
  if (len > 0) {
    attributesInfo.forEach(info => {
      if (info.key === 'TEMPLATES') {
        data.push(info);
        return data;
      }
    });
  }
}

// middleware that is specific to this router
router.use(function timeLog(req, res, next) {
  console.log('Templat Time: ', Date.now())
  next()
})

// define the home page route
router.get('/', function (req, res) {
  res.status(200).json({ result: 'Templates Api home page' });
})

// define the about route
router.get('/about', function (req, res) {
  res.send('About templates')
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
        if (info.key === 'TEMPLATES') {
          info.value = JSON.parse(info.value);
          res.status(200).json(info);
        }
      });
    })
    .catch((err) => {
      let status = err.response.status
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

function postTemplates(resp, req, res)
{
  let fileInfo = req.files.template_file;

  let info = {
    'filename': fileInfo.originalFilename,
    'path': fileInfo.path,
    'size': fileInfo.size,
    'type': fileInfo.type
  }

  let token = req.headers['x-authorization'];

  var formData = {
    file: fs.createReadStream(fileInfo.path),
  };
  let host = 'http://sm.schdri.com:80/'
  let uploadFileHost = host + 'api/file/upload/'
  request.post({ url:uploadFileHost, formData: formData }, function (err, httpResponse, body) {
    if (err) {
      return console.error('upload failed:', err);
    }
    console.log('Upload successful!  Server responded with:', body);

    if (JSON.parse(body).success)
    {
      // 保存到属性表
      // http://cf.beidouapp.com:8080/api/plugins/telemetry/ASSET/265c7510-1df4-11e9-b372-db8be707c5f4/SERVER_SCOPE
      let url = util.getAPI() + 'plugins/telemetry/ASSET/' + req.params.id + '/SERVER_SCOPE';
      let bodyData = JSON.parse(body)
      let str = [{
          "template_name": req.body.template_name,
          "template_url": host + bodyData.fileId
        }
      ];
      
      resp.data.forEach(info => {
        if (info.key === 'TEMPLATES') {
          let data = JSON.parse(info.value);
          data.forEach(_dt => {
            str.push(_dt);
          })      
        }
      });

      let val = JSON.stringify(str);

      let data = {
        "TEMPLATES": `${val}`
      }

      axios.post(url, (data), { headers: { "X-Authorization":token } })
        .then(response => {
          res.status(response.status).json('成功创建报表模板并关联到资产。');
        })
        .catch(err => {
          let resMsg = {
            "code":`${err.response.status}`,
            "message:":err.message
          };
          res.status(err.response.status).json(resMsg);
        });
    }
    else
    {
      let resMsg = {
        "code":`501`,
        "message:":'报表模板上传失败。'
      };
      res.status(501).json(resMsg);
    }
  });
}

//POST
router.post('/:id', multipartMiddleware, async function (req, res) {
  let msg = 'Post ID:' + req.params.id + 'template_name:' + req.body.template_name;

  let assetID = req.params.id;
  let token = req.headers['x-authorization'];
  let get_attributes_api = util.getAPI() + `/plugins/telemetry/ASSET/${assetID}/values/attributes/SERVER_SCOPE`;

  axios.get(get_attributes_api, {
    headers: {
      "X-Authorization": token
    }
  })
    .then((resp) => {
      postTemplates(resp, req, res);
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
router.delete('/:id', async function (req, res) {
  let msg = 'Delete ID:' + req.params.id + ' Name:' + req.query.templateName;

  res.status(200).json(msg);
})

// define the about route
router.get('/about', function (req, res) {
  res.send('About templates')
})

module.exports = router