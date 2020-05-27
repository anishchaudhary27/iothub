load('api_config.js');
load('api_mqtt.js');
load('api_gpio.js');
load('api_timer.js');

let topic = '/devices/' + Cfg.get('device.id') + '/state';
let topics = '/devices/' + Cfg.get('device.id') + '/config';
let topicss = '/devices/' + Cfg.get('device.id') + '/events/commit';

Timer.set(10000 /* milliseconds */, Timer.REPEAT, function() {
  if(MQTT.isConnected()){
    let msg = JSON.stringify({deviceId:Cfg.get('device.id'),fwCurrent:Cfg.get('fw.version'),lightIntensity:123,state:Cfg.get("fw.state"),mode:Cfg.get("fw.mode")});
    print(topic, '->', msg);
    MQTT.pub(topic, msg, 1);
  }
}, null);

GPIO.set_mode(2,GPIO.MODE_OUTPUT);

MQTT.sub(topics, function(conn, topic, msg) {
  print('Topic:', topic, 'message:', msg);
  let obj = JSON.parse(msg) || {state: 0,mode: 0};
  Cfg.set({fw:{state:obj.state}});
  Cfg.set({fw:{mode:obj.mode}});
  GPIO.write(2, obj.state);
  if(Cfg.get("fw.dirty") === 1){
    if(MQTT.isConnected()){
      let msg = JSON.stringify({deviceId: Cfg.get('device.id')});
      print(topicss, '->', msg);
      MQTT.pub(topicss, msg, 1);
      Cfg.set({fw:{dirty:0}});
    }
  }
}, null);