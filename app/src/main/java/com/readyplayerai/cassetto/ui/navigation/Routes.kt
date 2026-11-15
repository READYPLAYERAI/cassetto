package com.readyplayerai.cassetto.ui.navigation

import android.net.Uri

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val ALL_LISTS = "lists"
    const val LIST_DETAIL_ARG = "listId"
    const val LIST_DETAIL = "list/{$LIST_DETAIL_ARG}"
    const val ACCOUNT = "account"

    const val DETAIL_ARG = "bookmarkId"
    const val DETAIL = "bookmark/{bookmarkId}"

    fun detail(bookmarkId: String): String = "bookmark/${Uri.encode(bookmarkId)}"

    fun listDetail(listId: String): String = "list/${Uri.encode(listId)}"
}
