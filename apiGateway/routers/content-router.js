var express = require('express');
var router = express.Router();
var chart_area = require('./echarts/area');
var chart_pie = require('./echarts/pie');
var multipart = require('connect-multiparty');
var multipartMiddleware = multipart();
const node_echarts = require('node-echarts');
var request = require('request');
const axios = require('axios');

// middleware that is specific to this router
router.use(function timeLog(req, res, next) {
    console.log('content Time: ', Date.now())
    next()
})

async function excuteGraphQL(req, res) {
    let graphQL = req.query.graphQL;
    if (graphQL) {
        axios.post('http://swapi.apis.guru', {query:graphQL},
        {
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json',
            }
        })
        .then(resp => {
            let resMsg = {
                "code": `${resp.status}`,
                "message:": resp.data
            };
            res.status(resp.status).json(resMsg);
        })
        .catch(err => { 
            util.responErrorMsg(err, res);
        });
    }
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
})

router.get('/:id', async function (req, res) {
    let token = req.headers['x-authorization'];
    // download file
    let downloadFileHost = req.params.id;
    axios.get(downloadFileHost, {
        headers: {
            "X-Authorization": token
        }
    }).then((resp) => {
        console.log('Download successful!  Server responded with:', resp.body);

        if (resp.status == 200) {
            let resMsg = {
                "code": `${resp.status}`,
                "message:": `${resp.data}`
            };
            res.status(resp.status).json(resMsg);
        }
    }).catch((err) => {
        util.responErrorMsg(err, res);
    });
})

module.exports = router;