var http = require('http')

function getRandomIntInclusive(min, max) {
    min = Math.ceil(min);
    max = Math.floor(max);
    return Math.floor(Math.random() * (max - min + 1)) + min; 
  //The maximum is inclusive and the minimum is inclusive 
}  
var conf = {
    'devHeadToken': '8HjSKu6151uiWNejE0uU',
    'devTailToken': 'AoI6jmm0Vo9twSXezKeR',
    switch: {
        '8HjSKu6151uiWNejE0uU' : false,
        'AoI6jmm0Vo9twSXezKeR' : false
    },
    id : {
        '8HjSKu6151uiWNejE0uU' : 0,
        'AoI6jmm0Vo9twSXezKeR' : 0
    }
}
var options = {
    host:'121.69.130.154',
    port:9099
};

function getMessage() {
    let strainoffset = getRandomIntInclusive(-5.0,5.0)
    let stressoffset = getRandomIntInclusive(-1.0,1.0)
    let shearoffset = getRandomIntInclusive(-10.0,10.0)
    let momentoffset = getRandomIntInclusive(-500.0,500.0)
    let tempoffset = getRandomIntInclusive(25.0,28.0)
    let humioffset = getRandomIntInclusive(65.0,70.0)
    let attr = getRandomIntInclusive(100.0,500.0)
    var currParam = {
        'strain': (121+strainoffset),
        'stress': (70+stressoffset),
        'shear': (700+shearoffset),
        'bending_moment': (5000+momentoffset),
        'temp': tempoffset,
        'humi': humioffset,
        'new_attr' : attr
    };
    return JSON.stringify(currParam)
}
function replyRpc(token,id){
    var paramStr = JSON.stringify({ 'result': 'ok' })
    options.path = '/api/v1/' + token + '/rpc/' + id
    options.method = 'POST'
    options.headers = {
        'Content-Type' : 'application/json',// 不写这个参数，后台会接收不到数据
        'Content-Length' : paramStr.length
    }
    var reply = http.request(options, (res) => {
        console.log('REPLY STATUS:' + res.statusCode)
        res.on('data',(body) => {
            console.log(body)
        })
        res.on('end',() => {
            console.log('reply res end')
        })
    })
    reply.write(paramStr)
    reply.end()
    return 
}

function clientRpc(token){
    var paramStr = JSON.stringify({ 'timeout': 1000 })
    options.path = '/api/v1/' + token + '/rpc'
    options.method = 'GET'
    options.headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36',
        'Content-Type' : 'application/json',// 不写这个参数，后台会接收不到数据
        'Content-Length' : paramStr.length
    }
    var req = http.request(options, function (res) {
        console.log('STATUS:' + res.statusCode);
        res.setEncoding('utf-8')
        res.on('data', (body) => {
            console.log('BODY:' + body)
            if(JSON.parse(body).id === undefined) {
            } else {
                conf.id[token] = JSON.parse(body).id
                conf.switch[token] = JSON.parse(body).params
            }
        })
        res.on('end',function(){
            if(conf.id[token]!=0) {
                console.log(conf.id[token])
                replyRpc(token,conf.id[token])
            }
        })
        res.on('error',function(e){
            console.info(e)
        })
    })

    req.on('err',function(err){
        if(err){
            console.info(err);
        }
    });
    // console.log(paramStr);
    req.write(paramStr,'utf-8');
    req.end(() => {
        setTimeout(() => {
            clientRpc(token);
        },1000)
    });
}



function uploadMeas(token,index){
    if(index >= 10000)return;
    var paramStr = getMessage()
    options.method = 'POST'
    options.headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36',
        'Content-Type' : 'application/json',// 不写这个参数，后台会接收不到数据
        'Content-Length' : paramStr.length
    }
    options.path = '/api/v1/'+ token +'/telemetry'

    var req = http.request(options,function(res){
        console.log('STATUS:' + res.statusCode);
        // console.log('HEADERS:' + JSON.stringify(res.headers));
        res.setEncoding('utf-8');
        res.on('data',function(body){
            console.log('BODY：' + body);
        });
        res.on('end',function(){
            console.log('end')
        })
        res.on('error',function(e){
            console.info(e)
        })
    });
    req.on('err',function(err){
        if(err){
            console.info(err);
        }
    });
    if(conf.switch[token]){
        req.write(paramStr,'utf-8');
    }
    req.end(() => {
        if(conf.switch[token]){
            setTimeout(() => {
                // console.log(index)
                uploadMeas(token,index + 1);
            },850)
        }
    });
}

clientRpc(conf.devHeadToken)
clientRpc(conf.devTailToken)
uploadMeas(conf.devHeadToken,0)
uploadMeas(conf.devTailToken,0)