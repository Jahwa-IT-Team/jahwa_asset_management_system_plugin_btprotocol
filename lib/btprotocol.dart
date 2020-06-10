library btprotocol;

import 'dart:async';
import 'dart:typed_data';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
//import 'package:rxdart/subjects.dart';
import 'package:rxdart/rxdart.dart';

part './BluetoothState.dart';
part './BluetoothBondState.dart';
part './BluetoothDeviceType.dart';
part './BluetoothDevice.dart';
part './BluetoothPairingRequest.dart';
part './BluetoothDiscoveryResult.dart';
part './BluetoothConnection.dart';

class Btprotocol {
  static const String namespace = 'btprotocol';
  static const MethodChannel _methodChannel =const MethodChannel('btprotocol/method');
  final EventChannel _stateChannel = const EventChannel('btprotocol/state');
  //final EventChannel _dataChannel = const EventChannel('btprotocol/data');
  final StreamController<MethodCall> _methodStreamController = new StreamController.broadcast(); // ignore: close_sinks

  Stream<MethodCall> get _methodStream => _methodStreamController.stream; // Used internally to dispatch methods from platform.

  /// Singleton boilerplate
  Btprotocol._() {
    _methodChannel.setMethodCallHandler((MethodCall call) {
      _methodStreamController.add(call);
      return;
    });

    //_setLogLevelIfAvailable();
  }

  static Btprotocol _instance = new Btprotocol._();
  static Btprotocol get instance => _instance;

  BehaviorSubject<bool> _isScanning = BehaviorSubject.seeded(false);
  Stream<bool> get isScanning => _isScanning.stream;

  static Future<String> get platformVersion async {
    final String version = await _methodChannel.invokeMethod('getPlatformVersion');
    return version;
  }

  /// Returns list of bonded devices.
  Future<List<BluetoothDevice>> getListPairedDevice() async {
    final List list = await _methodChannel.invokeMethod('getListPairedDevice');
    return list.map((map) => BluetoothDevice.fromMap(map)).toList();
  }

  Future<int> connectDevice(String address) async {
    return await _methodChannel.invokeMethod('connectDevice',{"address": address});
  }

  Future<int> disconnectDevice() async{
    return await _methodChannel.invokeMethod('disconnectDevice');
  }

  Future<List<SharkDataInfo>> get getListTag async{
    final List list = await _methodChannel.invokeListMethod('getListTag');
    return list.map((map)=> SharkDataInfo.fromMap(map)).toList();
  }

  Future<int> clearData() async{
    return await _methodChannel.invokeMethod('clearData');
  }

  Future<bool> get isConnected async{
    return await _methodChannel.invokeMethod('isConnected');
  } 

  Future<int> setPower(int power) async{
    return await _methodChannel.invokeMethod('setPower', {"power":power});
  }

  Future<void> initPower() async{
    return await _methodChannel.invokeMethod('initPower');
  }

  Future<int> get getPower async{
    return await _methodChannel.invokeMethod('getPower');
  }

  Future<int> get barcodeStartDecode async{
    return await _methodChannel.invokeMethod('barcode_StartDecode');
  }

  

// Stream<PusherMessage> get onMessage =>
//       _messageChannel.receiveBroadcastStream().map(_toPusherMessage);
  Stream<dynamic> _onChangeState;
  Stream<dynamic> get onChangeState {
    if(_onChangeState == null){
      _onChangeState = _stateChannel.receiveBroadcastStream();
    }
    return _onChangeState;
  }// => _dataChannel.receiveBroadcastStream().


  // Future<String> get connectedDevices{
  //   return _channel
  //     .invokeMethod('getConnectedDevices')
  //     .then((value) => null)
  // }

  /// Retrieve a list of connected devices
  // Future<List<BluetoothDevice>> get connectedDevices {
  //   return _channel
  //       .invokeMethod('getConnectedDevices')
  //       .then((buffer) => protos.ConnectedDevicesResponse.fromBuffer(buffer))
  //       .then((p) => p.devices)
  //       .then((p) => p.map((d) => BluetoothDevice.fromProto(d)).toList());
  // }



}

class SharkDataInfo{
  String type;
  String tagData;

  SharkDataInfo({this.type, this.tagData});

  factory SharkDataInfo.fromMap(Map<dynamic, dynamic> map){
    return SharkDataInfo(
      type: map['type'] as String,
      tagData: map['tagdata'] as String,
    );
  }
}
