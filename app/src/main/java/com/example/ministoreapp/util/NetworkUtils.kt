package com.example.ministoreapp.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkUtils {
    /**
     * Checks whether the device currently has an active network connection.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Translates raw network exceptions (including Cronet net::ERR_NAME_NOT_RESOLVED)
     * into user-friendly error messages.
     */
    fun getFriendlyErrorMessage(throwable: Throwable?): String {
        if (throwable == null) return "An unknown error occurred."
        val message = throwable.message ?: ""
        return when {
            message.contains("ERR_NAME_NOT_RESOLVED", ignoreCase = true) ||
            message.contains("ERR_INTERNET_DISCONNECTED", ignoreCase = true) ||
            message.contains("ERR_NAME_NOT_FOUND", ignoreCase = true) ||
            message.contains("Unable to resolve host", ignoreCase = true) ||
            message.contains("No address associated with hostname", ignoreCase = true) ||
            throwable is UnknownHostException -> {
                "Unable to resolve server address. Please check your internet connection."
            }
            message.contains("ERR_CONNECTION_TIMED_OUT", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            throwable is SocketTimeoutException -> {
                "Connection timed out. Please check your network connection and try again."
            }
            throwable is ConnectException || message.contains("ERR_CONNECTION_REFUSED", ignoreCase = true) -> {
                "Unable to connect to server. Please try again later."
            }
            message.contains("CronetUrlRequest", ignoreCase = true) -> {
                "Network error occurred. Please check your internet connection."
            }
            else -> message.ifBlank { "Network request failed. Please check your connection." }
        }
    }
}
