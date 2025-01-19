package io.bytebeam;

parcelable BytebeamPayload;

// IMyService.aidl
interface IBytebeamService {
    void pushData(in BytebeamPayload data);
    void stopService();
}