package com.pcyfox.screen

import android.media.MediaCodec
import com.pcyfox.h264.H264HandlerNative
import java.nio.ByteBuffer

class Sender {
    private val TAG = "RtspBroadcast"
    private var h264HandlerNative: H264HandlerNative = H264HandlerNative()
    // private val broadcastIp = "255.255.255.255"
    //private val broadcastIp = "192.168.41.18"
    private val broadcastIp = "239.0.0.200"
    private val targetPort = 2021
    private val clock = 25L
    private val maxPacketLength = 20000

    init {
        h264HandlerNative.init(true, broadcastIp, targetPort, 1)
        h264HandlerNative.startSend()
    }

    fun send(h264Buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        val buf = ByteArray(info.size)
        h264Buffer.get(buf, info.offset, info.size)
        h264HandlerNative.packH264ToRTP(buf, buf.size, maxPacketLength, info.presentationTimeUs*1000, clock, true, null);
        h264Buffer.clear()
    }


    fun updateSPS_PPS(sps: ByteArray, pps: ByteArray) {
        h264HandlerNative.updateSPS_PPS(sps, sps.size, pps, pps.size)
    }
}