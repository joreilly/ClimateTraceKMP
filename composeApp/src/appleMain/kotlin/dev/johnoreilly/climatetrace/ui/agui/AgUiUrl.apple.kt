package dev.johnoreilly.climatetrace.ui.agui

// The simulator shares the host's network, so localhost resolves. A physical device needs the
// host's LAN address.
internal actual fun defaultAgUiUrl(): String = "http://localhost:8082/agui"
