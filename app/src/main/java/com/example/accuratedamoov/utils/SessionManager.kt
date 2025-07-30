package com.example.accuratedamoov.utils

import android.content.Context
import android.content.SharedPreferences


class SessionManager(context: Context) {
    var pref: SharedPreferences
    var editor: SharedPreferences.Editor

    init {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        editor = pref.edit()
    }

    var isRegistered: Boolean
        get() = pref.getBoolean(KEY_IS_REGISTERED, false)
        set(registered) {
            editor.putBoolean(KEY_IS_REGISTERED, registered)
            editor.apply()
        }

    companion object {
        private const val PREF_NAME = "damoov_user_pref"
        private const val KEY_IS_REGISTERED = "is_registered"
    }
}
