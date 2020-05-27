const functions = require('firebase-functions')
const iot = require('@google-cloud/iot')
const admin = require('firebase-admin')
const request = require('request')
const {PubSub} = require('@google-cloud/pubsub');
const { firestore } = require('firebase-admin');
const iotClient = new  iot.v1.DeviceManagerClient()
admin.initializeApp()


exports.onUserDeleted = functions.auth.user().onDelete((user,context)=>{
  const uid = user.uid
  const email = user.email
  const db = admin.firestore()
  if(email.endsWith('installer.com')) {
    return db.collection('installers').doc(uid).update({
      adminId : '',
      acitve:0
    })
  }
  else {
    return db.collection('admins').doc(uid).update({
      active: 0
    })
  }
  
})


exports.onUserAdded = functions.auth.user().onCreate((user,context)=>{
  const uid = user.uid
  const email = user.email
  const name  = email.split('@')[0]
  const db = admin.firestore()
  if(email.endsWith('installer.com')) {
    return db.collection('installers').doc(uid).set({
      adminId : '',
      acitve:1,
      name: name
    })
  }
  else {
    return db.collection('admins').doc(uid).set({
      active: 1,
      name:name
    })
  }
})


exports.requestDeviceDeletion = functions.https.onCall((data,context)=>{
  const deviceId = data.deviceId
  const pubsub = new PubSub()
  const dataBuffer = Buffer.from(JSON.stringify({
    deviceId: deviceId
  }));
  return pubsub.topic('delete_device_topic').publish(dataBuffer)
  .then((v)=>{
    return '1'
  })
  .catch((err)=>{
    console.log(err)
    return '0'
  })
})

exports.deleteDevice = functions.pubsub.topic('delete_device_topic').onPublish((msg,context)=>{
  const data = msg.json
  const deviceId = data.deviceId
  const db = admin.firestore()
  return db.collection('devices').doc(deviceId).get()
  .then(doc=>{
    const registryId = doc.get('registryId')
    const cloudRegion = doc.get('cloudRegion')
    const formattedName = `projects/iot-hub-273405/locations/${cloudRegion}/registries/${registryId}/devices/D${deviceId}`
    const request = {
      name: formattedName
    };
    return iotClient.deleteDevice(request)
  })
  .then(v=>{
    return db.collection('devices').doc(deviceId).update({
      deploymentStatus: 0,
      active:0
    })
  })
  .then(v=>{
    return db.collection('devices').doc(deviceId).get()
  })
  .then(document=>{
    const token = 'rt90vnkss4vQ2mzHTWzCmAA'
    const mdashId = document.get('mdashId')
    const URL = `https://mdash.net/api/v2/devices/${mdashId}?access_token=${token}`
    request.delete(URL,function (error,response,body) {
      console.log('error:', error); // Print the error if one occurred 
      console.log('statusCode:', response && response.statusCode); // Print the response status code if a response was received 
      console.log('body:', body);
    })
  })
  .catch(err=>{
    console.log(err)
  })
})


exports.deployDevice = functions.https.onCall((data,context)=>{
  const deviceId = data.deviceId
  const installerId = context.auth.uid
  const db = admin.firestore()
  return db.collection('devices').doc(deviceId).update({
    deploymentStatus: 1
  })
  .then(v => {
    return db.collection('installers').doc(installerId).get()
  })
  .then(v=>{
    const adminId = v.get('adminId')
    return db.collection('admins').doc(adminId).update({
      devices: firestore.FieldValue.arrayUnion(deviceId)
    })
  })
  .then(y =>{
    return '1'
  })
  .catch(err=> {
    console.log(err)
    return db.collection('devices').doc(deviceId).update({
      deploymentStatus: 0
    })
    .then(v=>{
      return '0'
    })
  })
})

exports.removeDevice = functions.https.onCall((data,context)=>{
  const deviceId = data.deviceId
  const installerId = context.auth.uid
  const db = admin.firestore()
  return db.collection('installers').doc(installerId).get()
  .then(doc=>{
    const adminId = doc.get('adminId')
    return db.collection('admins').doc(adminId).update({
      devices: firestore.FieldValue.arrayRemove(deviceId)
    })
  })
  .then(v=>{
    return db.collection('devices').doc(deviceId).update({
      deploymentStatus: 0
    })
  })
  .then(v =>{
    return '1'
  })
  .catch(err =>{
    console.log(err)
    return '0'
  })
})

exports.commitDeviceOtaUpdate = functions.pubsub.topic('confirm_ota_update').onPublish((msg,context)=>{
  const data = msg.json
  const deviceId = data.deviceId
  const token = 'rt90vnkss4vQ2mzHTWzCmAA';
  const db = admin.firestore()
  const dId = deviceId.slice(1)
  
  return db.collection('devices').doc(dId).get().then(doc=>{
    const mdashId = doc.get('mdashId')
    const URL = 'https://mdash.net/api/v2/devices/' + mdashId +'/rpc/Sys.Reboot?access_token=' + token
    request.get(URL,function (error,response,body) {
      console.log('error:', error); // Print the error if one occurred 
      console.log('statusCode:', response && response.statusCode); // Print the response status code if a response was received 
      console.log('body:', body);
    })
  })
  .catch(err => {
    console.log(err)
  })
})

exports.updateDeviceDisconnected = functions.pubsub.topic('device_diconnected_log').onPublish((data,context)=>{
  const logEntry = data.json
  const device_id = logEntry.labels.device_id.slice(1)
  const db  = admin.firestore()
  return db.collection('devices').doc(device_id).update({
    online : 0
  })
})

exports.updateDeviceConnected = functions.pubsub.topic('device_connected_log').onPublish((data,context)=>{
  const logEntry = data.json
  const device_id = logEntry.labels.device_id.slice(1)
  const db  = admin.firestore()
  return db.collection('devices').doc(device_id).update({
    online : 1
  })
})

//update state of a devices
exports.updateDeviceState = functions.pubsub.topic('registery_1_state_topic').onPublish((msg)=>{
  const message = JSON.parse(Buffer.from(msg.data, 'base64').toString())
  const deviceId = message.deviceId.slice(1)
  const state = message.state
  const lightIntensity =  message.lightIntensity
  const fwVersion = message.fwCurrent
  const mode = message.mode
  const db = admin.firestore()
  return db.collection('devices').doc(deviceId).update({
    state : state,
    fwCurrent : fwVersion,
    mode : mode,
    lightIntensity: lightIntensity
  })
})

//send command to device for changing mode or state by configuration update
exports.updateDeviceConfig = functions.https.onCall((data,context)=>{
  const divdata = {
    state : data.state,
    mode : data.mode
  }
  const dat = JSON.stringify(divdata)
  const dId = `D${data.deviceId}`
  const formattedName = `projects/iot-hub-273405/locations/${data.cloudRegion}/registries/${data.registryId}/devices/${dId}`;
  const binaryData = Buffer.from(dat).toString('base64');  
  const request = {
      name: formattedName,
      binaryData: binaryData,
    };
  return iotClient.modifyCloudToDeviceConfig(request).then(val => {
    return {
      result : val[0].version
    }
  }).catch(err => {
    console.log(err)
  })
})


//update config of a fleat F1
exports.updateAllDevicesConfigMD = functions.https.onCall((data,context)=>{
  const db = admin.firestore()
  const uid = context.auth.uid
  const divdata = {
    state : data.state,
    mode : data.mode
  }

  return db.collection('admins').doc(uid).get().then(doc => {
    const devices = doc.get('devices').slice(1)
    
    let p = []
    const topicName = 'update_devices_config'
    const pubsub = new PubSub()
    devices.forEach(device => {
      const data = {
        deviceId: device,
        data :divdata
      }
      const dataBuffer = Buffer.from(data)
      const v = pubSubClient.topic(topicName).publish(dataBuffer);
      p.push(v)
    })
    return Promise.all(p)
  })
  .then(val => {
    return {
      result : 'requested'
    }
  })
  .catch(reason => {
    console.log(reason)
  })
})

//update config of a fleat F2
exports.updateDeviceConfigMD = functions.pubsub.topic('update_devices_config').onPublish((msg,context)=>{
  const input = msg.json
  const deviceId = input.deviceId
  const data = JSON.stringify(input.data)
  const binaryData = Buffer.from(data)
  const db = admin.firestore()

  return db.collection('devices').doc(`D${deviceId}`).get().then(doc=>{
    const formattedName = `projects/iot-hub-273405/locations/${doc.cloudRegion}/registries/${doc.registryId}/devices/${deviceId}`
    const request = {
      name: formattedName,
      binaryData: binaryData,
    };
    return iotClient.modifyCloudToDeviceConfig(request)
  }).catch(err => {
    console.log({
      event: 'failed to change config of device',
      device : deviceId
    })
  })
})