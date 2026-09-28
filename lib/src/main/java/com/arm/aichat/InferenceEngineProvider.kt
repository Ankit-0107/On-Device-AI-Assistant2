package com.arm.aichat

import android.content.Context
import com.arm.aichat.internal.InferenceEngineImpl

object InferenceEngineProvider {

    fun get(context: Context): InferenceEngine {
        return InferenceEngineImpl.getInstance(context.applicationContext)
    }
}