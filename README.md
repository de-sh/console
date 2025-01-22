# Uplink Android SDK

SDKs for setting up and communicating with [uplink](https://github.com/bytebeamio/uplink) from android.

This project consists of two parts.

## Uplink Android SDK

It's an android library that you can import and use into your android application. This library provides `BytebeamService`, that
connects to our backend and allows you to monitor your device. The library comes configured to upload various metrics like
cpu, memory, disk, network/mobile data usage out of the box. If you want to upload some custom metrics, you can bind to
this service. The binder interface has a `pushData` method for uploading custom metrics. The repository also includes
`monitoring_app` as a demo application that uses this library.

### Setup

* Download `uplink` executable from the [releases page]() and copy it to `uplink_android/src/main/assets/uplink` in the library
* Download the `device.json` for your project and copy it to `monitoring_app/src/main/assets/device.json`

## Native module

It's a native module that you can install as a system service in your android project if you are building operating system
images from source. You can see an example setup in `native/module_template`