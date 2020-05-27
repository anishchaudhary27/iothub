const { exec } = require("child_process");
const { stderr, stdout } = require("process");
const { Console } = require("console");

const deviceId = '2xStqbeF6jC3eCZ878ZP'
const projectId = 'iot-hub-273405'
const regionId = 'us-central1'
const registryId = 'registery_1'

// exec(`mos flash`,(err,stdout,stderr)=>{
//     if(err) {
//         console.log(err)
//         return
//     }
//     if(stdout == '') {
//         console.log("done flash")
//         setDeviceId()
//     }
// })

setDeviceId()

function setDeviceId() {
    exec(`mos config-set device.id=D${deviceId}`,(err,stdout,stderr)=>{
        if(err) {
            console.log(err)
            return
        }
        if(stdout == '') {
            console.log("done deviceId")
            setWifi()
        }
    })
}

function setWifi() {
    exec('mos wifi Devdutt anish27j',(err,stdout,stderr)=>{
        if(err) {
            console.log(err)
            return
        }
        if(stdout == '') {
            console.log("done WiFi")
            setupGCP()
        }
    })
}

function setBT() {
    exec(`mos config-set bt.enable=true bt.dev_name=${deviceId}`,(err,stdout,stderr)=>{
        if(err) {
            console.log(err)
            return
        }
        if(stdout == '') {
            console.log("done bt")
            setupGCP()
        }
    })
}

function setupGCP() {
    exec(`mos gcp-iot-setup --gcp-project ${projectId} --gcp-region ${regionId} --gcp-registry ${registryId}`,(err,stdout,stderr)=>{
        if(err) {
            console.log(err)
            return
        }
        if(stdout == '') {
            console.log("done gcp")
        }
    })
}