package com.cz.custommenu

import android.app.Application
import android.media.projection.MediaProjection
import com.example.wisdomclassroomgroup.service.MediaReaderService


class MyApplication : Application() {
    lateinit var mediaProjection: MediaProjection
    var serverStatus= MediaReaderService.ServerStatus.UNSTART
}