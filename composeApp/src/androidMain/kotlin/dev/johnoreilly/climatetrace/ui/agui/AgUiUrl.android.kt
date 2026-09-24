package dev.johnoreilly.climatetrace.ui.agui

// 10.0.2.2 is the emulator's alias for the host machine's loopback. On a physical device this
// needs to be the host's LAN address.
internal actual fun defaultAgUiUrl(): String = "http://10.0.2.2:8082/agui"
