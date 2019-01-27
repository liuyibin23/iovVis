var ruleTables = {
    "265c7510-1df4-11e9-b372-db8be707c5f4": {
        "blueRules": [{
            "andRule": ["devA"]
        }, {
            "andRule": ["devB", "devC"]
        }],
        "orangeRules": [{
            "andRule": ["devD", "devA"]
        }]
    }
};
/* warning rule tables */
function tryRules(assetId, msg) {
    //check orangeRules first, then blueRules
    if (ruleTables[assetId]) {
        blueRules = ruleTables[assetId].blueRules;
        orangeRules = ruleTables[assetId].orangeRules;
        //橙色预警规则
        //msg中包含资产中报警的设备（keys:true）
        //input: msg.devA,msg.devB
        //rule: [{andRule:[devA,devB]},{andRule:[devC]}]
        //
        let orange = false;
        for (var idx = 0; idx < orangeRules.length; idx++) {
            andRules = orangeRules[idx].andRule;
            //andRules:['devA','devB'],msg必须全部都具备
            for (var jdx = 0; jdx < andRules.length; jdx++) {
                var alarmDev = andRules[jdx];
                if (!msg[alarmDev]) {
                    // orange = false;
                    break;
                }
                if (jdx === andRules.length - 1)
                    orange = true;
            }
            if (!orange) { // test next rule (or)
                continue;
            } else
                break;
        }
        if (orange)
            return 'orange';

        let blue = false;
        for (var idx = 0; idx < blueRules.length; idx++) {
            andRules = blueRules[idx].andRule;
            //andRules:['devA','devB'],msg必须全部都具备
            for (var jdx = 0; jdx < andRules.length; jdx++) {
                var alarmDev = andRules[jdx];
                if (!msg[alarmDev]) {
                    // blue = false;
                    break;
                }
                if (jdx === andRules.length - 1)
                    blue = true;
            }
            if (!blue) { // test next rule (or)
                continue;
            } else
                break;
        }
        if (blue)
            return 'blue';
    } else
        return;
}

function nextRelation(metadata, msg) {
    var condition = 'default';
    switch (tryRules(metadata.asset_id, msg)) {
        case 'blue':
            // code
            condition = ['blue'];
            break;
        case 'orange':
            condition = ['orange'];
            break;
        default:
            condition = ['disappear']
            break;
    }
    return condition;
}


metadata = {
    asset_id: '265c7510-1df4-11e9-b372-db8be707c5f4'
};
msg = {
    "devX": true,
    "devC": true
}

var ret = nextRelation(metadata, msg);
console.log(ret);