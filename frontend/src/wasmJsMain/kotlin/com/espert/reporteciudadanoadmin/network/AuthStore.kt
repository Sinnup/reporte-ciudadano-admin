package com.espert.reporteciudadanoadmin.network

import kotlinx.browser.sessionStorage

object AuthStore {
    private const val KEY_TOKEN = "rc_admin_token"

    fun save(token: String) {
        sessionStorage.setItem(KEY_TOKEN, token)
    }

    fun load(): String? {
        return sessionStorage.getItem(KEY_TOKEN)
    }

    fun clear() {
        sessionStorage.removeItem(KEY_TOKEN)
    }
}
