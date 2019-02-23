
const express = require('express');
const router = express.Router();
const axios = require('axios');
const util = require('../util/utils');
const fs = require('fs');

const graphQLDispatch = require('./GraphQL/graphQL-dispatch');

async function excuteGraphQL(req, res) {
    graphQLDispatch.dispatchGraphQL(req, res);
}

// define the home page route
router.get('/', function (req, res) {
    if (req.baseUrl === '/api/v1/tables') {
        excuteGraphQL(req, res);
    }
    else {
        res.send('content Api home page');
    }
})

// define the about route
router.get('/about', function (req, res) {
    res.send('About Content')
})

router.post('/:id', async function (req, res) {
    let assetID = req.params.id;
    res.status(200).json({code:200,message:'not support this time!'});
})


router.get('/:id', async function (req, res) {
    let token = req.headers['x-authorization'];
    // download file
    let downloadFileHost = util.getFSVR() + req.params.id;
    axios.get(downloadFileHost, {
        headers: {
            "X-Authorization": token
        },
        responseType: 'arraybuffer'
    }).then((resp) => {
        res.header("Content-Type", 'application/vnd.openxmlformats-officedocument.wordprocessingml.document');

        // fs.writeFile("test2.docx", resp.data , "binary", function (err) {
        //     if (err) {
        //         console.log("保存失败");
        //     }
        
        //      console.log("保存成功"+resp.data.length);
        // });
    
        util.responData(200, resp.data, res);
    }).catch((err) => {
        util.responErrorMsg(err, res);
    });
})

module.exports = router;