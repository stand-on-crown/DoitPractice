package com.gi.ch15_outer

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder

class MyAIDLService : Service() {

    private var player: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        // onCreate에서는 초기화하지 않음
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }

    override fun onBind(intent: Intent): IBinder {
        return object : MyAIDLInterface.Stub() {

            override fun getMaxDuration(): Int {
                return player?.let {
                    if (it.isPlaying) it.duration else 0
                } ?: 0
            }

            override fun start() {
                if (player == null) {
                    // 처음 시작하거나 stop 후 재시작
                    player = MediaPlayer.create(this@MyAIDLService, R.raw.music)
                    player?.start()
                } else if (player?.isPlaying == false) {
                    // pause 상태였다면 다시 시작
                    player?.start()
                }
            }

            override fun stop() {
                player?.let {
                    if (it.isPlaying) {
                        it.stop()
                    }
                    it.release()
                }
                player = null
            }
        }
    }
}
