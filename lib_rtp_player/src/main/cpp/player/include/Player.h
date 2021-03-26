//
// Created by LN on 2021/1/4.
//

#ifndef PLAYER_PLAYER_H
#define PLAYER_PLAYER_H


#include <android/native_window_jni.h>
#include "StateListener.h"
#include "PlayerInfo.h"


class Player {

private:
    bool isDebug = false;

public:
    Player();

    ~Player();

    void SetDebug(bool isDebug);

    int Configure( ANativeWindow *window, int w, int h);

    int ChangeWindow(ANativeWindow *window, int w, int h);

    int HandleRTPPkt(unsigned char *pkt, unsigned  int pktLen,unsigned  int maxFrameLen,int isLiteMod);

    int Play();

    int Pause(int delay);

    int Stop();

    void SetStateChangeListener(void (*listener)(PlayState));


private :
    static void StartDecodeThread();

};


#endif



