package com.taike.lib_udp_player.udp;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.taike.lib_udp_player.BuildConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by Auser on 2018/5/28.
 */

public class RTP_UDPPlayer {
    private static final String TAG = "RTP_UDPPlayer";
    //MediaCodec variable
    private volatile boolean isPlaying = false;
    static String multiCastHost = "239.0.0.200";
    private static final int videoPort = 2021;
    private DatagramSocket dataSocket;
    private MulticastSocket multicastSocket;
    private final Handler handler;
    private boolean isMultiBroadCastMod = true;
    private final static int MAX_UDP_PACKET_LEN = 65507;//UDP包大小限制

    private static final int MAX_FRAME_LEN = 4 * 1024 * 1024;//视频帧大小限制
    private NativeUDPPlayer nativeUDPPlayer;


    public RTP_UDPPlayer(SurfaceView surfaceView) {
        HandlerThread handlerThread = new HandlerThread("FUCK h264Data Handler");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        if (isMultiBroadCastMod) {
            initMultiBroadcast();
        }

        initNativePlayer();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated() called with: holder = [" + holder + "]");
                nativeUDPPlayer.configPlayer(holder.getSurface(), surfaceView.getWidth(), surfaceView.getHeight());
                if (nativeUDPPlayer.getState() == PlayState.PAUSE) {
                    nativeUDPPlayer.play();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged() called with: holder = [" + holder + "], format = [" + format + "], width = [" + width + "], height = [" + height + "]");
                nativeUDPPlayer.changeSurface(holder.getSurface(), width, height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed() called with: holder = [" + holder + "]");
                nativeUDPPlayer.pause();
            }
        });


    }


    private void initMultiBroadcast() {
        try {
            multicastSocket = new MulticastSocket(videoPort);
            InetAddress receiveAddress = InetAddress.getByName(multiCastHost);
            multicastSocket.joinGroup(receiveAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void initNativePlayer() {
        nativeUDPPlayer = new NativeUDPPlayer();
        nativeUDPPlayer.init(BuildConfig.DEBUG);
    }

    /*
    开始播放
     */
    public void startPlay() {
        if (isPlaying) {
            Log.e(TAG, "start play failed.  player is playing");
        } else {
            isPlaying = true;
            nativeUDPPlayer.play();
            handler.post(this::startReceiveData);
        }
    }

    private void startReceiveData() {
        byte[] receiveByte = new byte[MAX_UDP_PACKET_LEN];
        DatagramPacket dataPacket = new DatagramPacket(receiveByte, receiveByte.length);
        while (isPlaying) {
            try {
                if (isMultiBroadCastMod) {
                    multicastSocket.receive(dataPacket);
                } else {
                    dataSocket.receive(dataPacket);
                }
                nativeUDPPlayer.handleRTPPkt(receiveByte, dataPacket.getLength(), MAX_FRAME_LEN,true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.e(TAG, "startReceiveData:() over!");
    }

    /*
     *停止播放
     */
    public void stopPlay() {
        Log.d(TAG, "stopPlay() called");
        isPlaying = false;
    }


    public void pause() {
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (byte b : src) {
            int v = b & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv).append(" ");
        }
        return stringBuilder.toString();
    }

    public static String intToHex(int i) {
        int v = i & 0xFF;
        String hv = Integer.toHexString(v);
        if (hv.length() < 2) {
            return "0" + hv;
        } else {
            return hv;
        }
    }


    public boolean isPlaying() {
        return isPlaying;
    }


    public void release() {
        Log.e(TAG, "release() called");
        isPlaying = false;
    }


}
