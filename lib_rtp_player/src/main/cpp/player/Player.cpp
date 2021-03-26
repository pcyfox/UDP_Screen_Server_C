#include "Player.h"
#include "Android_log.h"
#include "include/Utils.h"
#include <android/native_window_jni.h>
#include <android/native_window.h>
#include <media/NdkMediaCodec.h>
#include <media/NdkMediaFormat.h>


#include<queue>
#include <pthread.h>

#ifdef __cplusplus
extern "C" {
#include "RTPUnPacket.h"
#endif
#ifdef __cplusplus
}
#endif


PlayerInfo playerInfo;

Player::Player() {
    playerInfo.SetPlayState(INITIALIZED);
    LOGD("init player info over");
}

int GetNALUType(AVPacket *packet) {
    int nalu_type = -1;
    const unsigned char *buf = packet->data;
    bool hasLongStartCode = buf[0] == 0 && buf[1] == 0 && buf[2] == 0 && buf[3] == 1;
    bool hasShortStartCode = buf[0] == 0 && buf[1] == 0 && buf[2] == 1;
    if (hasLongStartCode || hasShortStartCode) {
        if (hasShortStartCode) {
            nalu_type = buf[3] & 0xFF;
        } else {
            nalu_type = buf[4] & 0xFF;
        }
    }
    return nalu_type;
}


int createAMediaCodec(AMediaCodec **mMediaCodec, int width, int height, uint8_t *sps, int spsSize,
                      uint8_t *pps, int ppsSize,
                      ANativeWindow *surface, const char *mine) {

    LOGI("createAMediaCodec() called width=%d,height=%d,spsSize=%d,ppsSize=%d,mine=%s\n", width,
         height,
         spsSize, ppsSize, mine);

    if (width * height == 0) {
        LOGE("createAMediaCodec() not support video size");
        return PLAYER_RESULT_ERROR;
    }

    if (!*mMediaCodec) {
        AMediaCodec *mediaCodec = AMediaCodec_createDecoderByType(mine);
        if (!mediaCodec) {
            LOGE("createAMediaCodec() fail!");
            return PLAYER_RESULT_ERROR;
        } else {
            LOGI("createAMediaCodec() success!");
        }
        *mMediaCodec = mediaCodec;
    } else {
        AMediaCodec_flush(*mMediaCodec);
        AMediaCodec_stop(*mMediaCodec);
    }

    AMediaFormat *videoFormat = AMediaFormat_new();
    AMediaFormat_setString(videoFormat, "mime", mine);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_WIDTH, width); // 视频宽度
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_HEIGHT, height); // 视频高度

    if (spsSize && sps) {
        AMediaFormat_setBuffer(videoFormat, "csd-0", sps, spsSize); // sps
    }
    if (ppsSize && pps) {
        AMediaFormat_setBuffer(videoFormat, "csd-1", pps, ppsSize); // pps
    }

    media_status_t status = AMediaCodec_configure(*mMediaCodec, videoFormat, surface, NULL, 0);
    if (status != AMEDIA_OK) {
        LOGE("configure AMediaCodec fail!,ret=%d", status);
        AMediaCodec_delete(*mMediaCodec);
        mMediaCodec = NULL;
        return PLAYER_RESULT_ERROR;
    } else {
        LOGD("configure AMediaCodec success!");
    }
    return PLAYER_RESULT_OK;
}


void *Decode(void *info) {
    auto *pInfo = (PlayerInfo *) info;
    AMediaCodec *codec = pInfo->AMediaCodec;
    while (pInfo->GetPlayState() == STARTED) {
        AVPacket *packet = NULL;
        pInfo->packetQueue.get(&packet);
        if (packet == NULL || packet->data == NULL) {
            continue;
        }
        // 获取buffer的索引
        ssize_t index = AMediaCodec_dequeueInputBuffer(codec, 500);
        if (index >= 0) {
            size_t out_size;
            int length = packet->size;
            uint8_t *inputBuf = AMediaCodec_getInputBuffer(codec, index, &out_size);
            if (inputBuf != NULL && length <= out_size) {
                //clear buf
                memset(inputBuf, 0, out_size);
                // 将待解码的数据copy到硬件中
                memcpy(inputBuf, packet->data, length);
                int64_t pts = packet->pts;
                if (pts < 0 || !pts) {
                    pts = getCurrentTime();
                }

                media_status_t status = AMediaCodec_queueInputBuffer(codec, index, 0, length,
                                                                     pts, 0);

                delete packet;
                packet = NULL;
                if (status != AMEDIA_OK) {
                    LOGE("Decode queue input buffer error status=%d", status);
                }
            }
        }

        AMediaCodecBufferInfo bufferInfo;
        ssize_t status = AMediaCodec_dequeueOutputBuffer(codec, &bufferInfo, 100);
        if (status >= 0) {
            AMediaCodec_releaseOutputBuffer(codec, status, bufferInfo.size != 0);
            if (bufferInfo.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) {
                LOGE("Decode() video producer output EOS");
            }
        } else if (status == AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED) {
            LOGE("Decode() output buffers changed");
        } else if (status == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
        } else if (status == AMEDIACODEC_INFO_TRY_AGAIN_LATER) {
            //     LOGE("Decode() video no output buffer right now");
        } else {
            LOGE("Decode() unexpected info code: %zd", status);
        }
    }
    LOGD("-------Decode over!---------");
    return NULL;
}


void Player::StartDecodeThread() {
    LOGI("Start decode thread");
    pthread_create(&playerInfo.decode_thread, NULL, Decode, (void *) &playerInfo);
    pthread_setname_np(playerInfo.decode_thread, "decode_thread");
    pthread_detach(playerInfo.decode_thread);
}


void Player::SetDebug(bool debug) {
    LOGD("SetDebug() called with %d", debug);
    isDebug = debug;
}


int Player::Configure(ANativeWindow *window, int w, int h) {
    LOGD("----------Configure() called with: w=%d,h=%d", w, h);
    if (playerInfo.GetPlayState() != ERROR) {
        playerInfo.window = window;
        playerInfo.windowWith = w;
        playerInfo.windowHeight = h;
        int ret = createAMediaCodec(&playerInfo.AMediaCodec, playerInfo.windowWith,
                                    playerInfo.windowHeight,
                                    NULL,
                                    NULL,
                                    NULL,
                                    NULL,
                                    playerInfo.window, playerInfo.mine);
        if (ret == PLAYER_RESULT_ERROR) {
            return ret;
        } else {
            playerInfo.SetPlayState(PREPARED);
        }
    } else {
        LOGE("can't configure due to init player ERROR\n");
        return PLAYER_RESULT_ERROR;
    }
    LOGD("----------Configure Over-------------");
    return PLAYER_RESULT_OK;
}

int Player::ChangeWindow(ANativeWindow *window, int w, int h) {
    LOGI("--------ChangeWindow() called with w=%d,h=%d", w, h);
    if (playerInfo.GetPlayState() == PAUSE) {
        playerInfo.window = window;
        playerInfo.windowWith = w;
        playerInfo.windowHeight = h;
        int ret = createAMediaCodec(&playerInfo.AMediaCodec, playerInfo.windowWith,
                                    playerInfo.windowHeight,
                                    NULL,
                                    NULL,
                                    NULL,
                                    NULL,
                                    playerInfo.window, playerInfo.mine);

        AMediaCodec_start(playerInfo.AMediaCodec);
        if (ret == PLAYER_RESULT_OK) {
            LOGI("--------OnWindowChange() success! ");
            playerInfo.SetPlayState(STARTED);
            StartDecodeThread();
        }
    } else {
        LOGE("player not init or it not pause");
        return PLAYER_RESULT_ERROR;
    }
    return PLAYER_RESULT_OK;
}


void Player::SetStateChangeListener(void (*listener)(PlayState)) {
    playerInfo.SetStateListener(listener);
}


int Player::Play() {
    LOGI("--------Play()  called-------");
    if (playerInfo.GetPlayState() == PAUSE) {
        playerInfo.SetPlayState(STARTED);
        return PLAYER_RESULT_OK;
    }
    if (playerInfo.GetPlayState() != PREPARED) {
        LOGE("player is not PREPARED!\n");
        return PLAYER_RESULT_ERROR;
    }

    media_status_t status = AMediaCodec_start(playerInfo.AMediaCodec);
    if (status != AMEDIA_OK) {
        LOGE("start AMediaCodec fail!\n");
        AMediaCodec_delete(playerInfo.AMediaCodec);
        playerInfo.AMediaCodec = NULL;
        return PLAYER_RESULT_ERROR;
    } else {
        LOGI("------------AMediaCodec start success!!\n");
    }
    playerInfo.SetPlayState(STARTED);
    StartDecodeThread();
    return PLAYER_RESULT_OK;
}


int Player::Pause(int delay) {
    LOGI("--------Pause()  called-------");
    if (playerInfo.GetPlayState() != STARTED) {
        LOGE("--------Pause()  called-,fail player not started------");
        return PLAYER_RESULT_ERROR;
    }
    playerInfo.SetPlayState(PAUSE);
    return PLAYER_RESULT_OK;
}


int Player::Stop() {
    LOGI("--------Stop()  called-------");
    if (playerInfo.GetPlayState() != STARTED) {
        LOGE("playerInfo is not started");
        return PLAYER_RESULT_ERROR;
    }
    playerInfo.SetPlayState(STOPPED);
    AMediaCodec_stop(playerInfo.AMediaCodec);
    LOGD("--------Stop Over------");
    return PLAYER_RESULT_OK;
}


void unpackCallback(UnpackResult result) {
    auto *avPacket = new AVPacket();
    avPacket->data = result->data;
    avPacket->size = result->length;
    avPacket->nalu_type = result->packet_NAL_unit_type;
    avPacket->cq = result->curr_Sq;
    avPacket->pkt_interval = result->pkt_interval;
    playerInfo.packetQueue.put(avPacket);
    free(result);
    result = NULL;
}


int Player::HandleRTPPkt(unsigned char *pkt, unsigned int len, unsigned int maxFrameLen,
                         int isLiteMod) {
    if (playerInfo.GetPlayState() != STARTED) {
        LOGE("HandleRTPPkt() fail,player not started!");
        return PLAYER_RESULT_ERROR;
    }
    return UnPacket(pkt, len, maxFrameLen, isLiteMod, unpackCallback);
}

Player::~Player() = default;


