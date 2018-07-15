package com.google.bazel.example.android.activities

import com.google.bazel.example.android.IRemoteService

class RemoteService : IRemoteService.Stub() {
    override fun basicTypes(anInt: Int, aLong: Long, aBoolean: Boolean, aFloat: Float, aDouble: Double, aString: String?) {
        TODO("not implemented")
    }

    override fun getPid(): Int {
        TODO("not implemented")
    }
}