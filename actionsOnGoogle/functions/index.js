const functions = require('firebase-functions');
const {smarthome} = require('actions-on-google');

const app = smarthome()

app.onSync((body, headers) => {
    // TODO Get devices for user
  return {
    requestId: body.requestId,
    payload: {
      agentUserId: "1836.15267389",
      devices: [{
        id: "123",
        type: "action.devices.types.OUTLET",
        traits: [
          "action.devices.traits.OnOff"
        ],
        name: {
          defaultNames: ["My Outlet 1234"],
          name: "Night light",
          nicknames: ["wall plug"]
        },
        willReportState: false,
        roomHint: "kitchen",
        deviceInfo: {
          manufacturer: "lights-out-inc",
          model: "hs1234",
          hwVersion: "3.2",
          swVersion: "11.4"
        },
        otherDeviceIds: [{
          deviceId: "local-device-id"
        }],
        customData: {
          fooValue: 74,
          barValue: true,
          bazValue: "foo"
        }
      }, {
        id: "456",
        type: "action.devices.types.LIGHT",
        traits: [
          "action.devices.traits.OnOff",
          "action.devices.traits.Brightness",
          "action.devices.traits.ColorSetting"
        ],
        name: {
          defaultNames: ["lights out inc. bulb A19 color hyperglow"],
          name: "lamp1",
          nicknames: ["reading lamp"]
        },
        willReportState: false,
        roomHint: "office",
        attributes: {
          colorModel: 'rgb',
          colorTemperatureRange: {
            temperatureMinK: 2000,
            temperatureMaxK: 9000
          },
          commandOnlyColorSetting: false
        },
        deviceInfo: {
          manufacturer: "lights out inc.",
          model: "hg11",
          hwVersion: "1.2",
          swVersion: "5.4"
        },
        customData: {
          fooValue: 12,
          barValue: false,
          bazValue: "bar"
        }
      }]
    }
  };
})

app.onExecute((body,headers)=>{
    // TODO Send command to device
  return {
    requestId: body.requestId,
    payload: {
      commands: [{
        ids: ["123"],
        status: "SUCCESS",
        states: {
          on: true,
          online: true
        }
      }, {
        ids: ["456"],
        status: "ERROR",
        errorCode: "deviceTurnedOff"
      }]
    }
  };
})

app.onQuery((body,headers)=>{
    // TODO Get device state
  return {
    requestId: body.requestId,
    payload: {
      devices: {
        123: {
          on: true,
          online: true
        },
        456: {
          on: true,
          online: true,
          brightness: 80,
          color: {
            name: "cerulean",
            spectrumRGB: 31655
          }
        }
      }
    }
  };
})

app.onDisconnect((body,headers)=>{})

exports.smartHomeHook = functions.https.onRequest(app)

