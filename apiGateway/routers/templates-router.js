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
      "X-Authorization": "Bearer " + token
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
      "X-Authorization": "Bearer " + token
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
  console.log('assetId=' + assetID);

  let token = toks.getTokenStr();
  //let data = getAttributes(assetID, token);

  let get_attributes_api = util.getAPI() + `/plugins/telemetry/ASSET/${assetID}/values/attributes/SERVER_SCOPE`;

  axios.get(get_attributes_api, {
    headers: {
      "X-Authorization": "Bearer " + token
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
      res.status(500).json('');
    });
})

// function to encode file data to base64 encoded string
function base64_encode(file) {
  // read binary data
  var bitmap = fs.readFileSync(file);
  // convert binary data to base64 encoded string
  return new Buffer(bitmap).toString('base64');
}

// function to create file from base64 encoded string
function base64_decode(base64str, file) {
  // create buffer object from base64 encoded string, it is important to tell the constructor that the string is base64 encoded
  var bitmap = new Buffer(base64str, 'base64');
  // write buffer to file
  fs.writeFileSync(file, bitmap);
  console.log('******** File created from base64 encoded string ********');
}

/*
 * 上传报表模版，关联报表模板url到基础设施
 */
async function addDocx2asset(assetId, docx, tk) {
  let docxRes;
  try {
    let sBase64 = 'data:doc/docx;base64,' + base64_encode(docx);
    let file64 = 'file=' + encodeURIComponent(sBase64);
    let host = 'http://sm.schdri.com';
    docxRes = await axios.post(host + '/api/file/upload/base64',
      file64,
      {
        headers: {
          "Content-Type": 'application/x-www-form-urlencoded'
        }
      });
    let fileUrl = host + '/' + docxRes.data.fileId;
    let apiUrl = `${host}/api/plugins/telemetry/ASSET/${assetId}/attributes/SERVER_SCOPE`;
    let assetRes = await axios.post(apiUrl, { rpt_docx: fileUrl },
      {
        headers: {
          "X-Authorization": "Bearer " + tk
        }
      });

    return assetRes;
  } catch (err) {
    throw new Error("Add docx to asset failed!");
  }
}

router.post('/abc', multipartMiddleware, async function (req, res) {
  let msg = 'Post ID:' + req.params.id + 'template_name:' + req.body.template_name;

})

//POST
router.post('/:id', multipartMiddleware, async function (req, res) {
  let msg = 'Post ID:' + req.params.id + 'template_name:' + req.body.template_name;

  let fileInfo = req.files.template_file;

  let info = {
    'filename': fileInfo.originalFilename,
    'path': fileInfo.path,
    'size': fileInfo.size,
    'type': fileInfo.type
  }

  let token = toks.getTokenStr();
  //上传到服务器
  //addDocx2asset(req.params.id, fileInfo.path, token);

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

    // 保存到属性表
    // http://cf.beidouapp.com:8080/api/plugins/telemetry/ASSET/265c7510-1df4-11e9-b372-db8be707c5f4/SERVER_SCOPE
    let url = util.getAPI() + 'plugins/telemetry/ASSET/' + req.params.id + '/SERVER_SCOPE';
    let bodyData = JSON.parse(body)
    let str = [{
      "template_name": req.body.template_name,
      "template_url:": host + bodyData.fileId
    }];
    let val = JSON.stringify(str);

    let data = {
      "TEMPLATES": `${val}`
    }
    axios.post(url, (data), { headers: { "X-Authorization": "Bearer " + toks.getTokenStr() } })
      .then(response => {
        res.status(response.status).json('成功创建报表模板并关联到资产。');
      })
      .catch(err => {
        console.log(err);
        res.end();
      });
  });

  // 返回url地址
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