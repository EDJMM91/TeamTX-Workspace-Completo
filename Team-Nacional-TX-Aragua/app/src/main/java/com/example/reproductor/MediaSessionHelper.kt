package com.example.reproductor

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.util.Log

// ═══════════════════════════════════════════════════════════════════════════
// MEDIA SESSION HELPER - REPRODUCTOR TX PRO (TEAM NACIONAL TX ARAGUA)
// Vincula el reproductor con auriculares Bluetooth, lockscreen y Android Auto.
// ═══════════════════════════════════════════════════════════════════════════

class MediaSessionHelper(
    private val context: Context,
    private val callback: MediaSessionCallback
) {
    interface MediaSessionCallback {
        fun onPlay()
        fun onPause()
        fun onSkipToNext()
        fun onSkipToPrevious()
        fun onSeekTo(posMs: Long)
        fun onStop()
    }

    private var mediaSession: MediaSession? = null

    init {
        try {
            mediaSession = MediaSession(context, "TX_PRO_MEDIA_SESSION").apply {
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() {
                        Log.d("TEAM_TX_REPRODUCTOR", "MediaSession: onPlay recibido")
                        callback.onPlay()
                    }

                    override fun onPause() {
                        Log.d("TEAM_TX_REPRODUCTOR", "MediaSession: onPause recibido")
                        callback.onPause()
                    }

                    override fun onSkipToNext() {
                        Log.d("TEAM_TX_REPRODUCTOR", "MediaSession: onSkipToNext recibido")
                        callback.onSkipToNext()
                    }

                    override fun onSkipToPrevious() {
                        Log.d("TEAM_TX_REPRODUCTOR", "MediaSession: onSkipToPrevious recibido")
                        callback.onSkipToPrevious()
                    }

                    override fun onSeekTo(pos: Long) {
                        Log.d("TEAM_TX_REPRODUCTOR", "MediaSession: onSeekTo a $pos ms")
                        callback.onSeekTo(pos)
                    }

                    override fun onStop() {
                        Log.d("TEAM_TX_REPRODUCTOR", "MediaSession: onStop recibido")
                        callback.onStop()
                    }
                })

                setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
                isActive = true
            }
            Log.d("TEAM_TX_REPRODUCTOR", "MediaSessionHelper inicializado con éxito")
        } catch (e: Exception) {
            Log.e("TEAM_TX_REPRODUCTOR", "Error inicializando MediaSession: ${e.message}")
        }
    }

    val sessionToken: MediaSession.Token?
        get() = mediaSession?.sessionToken

    fun actualizarEstado(
        estado: EstadoReproductor,
        posicionMs: Long,
        velocidad: Float = 1.0f
    ) {
        val session = mediaSession ?: return
        try {
            val stateBuilder = PlaybackState.Builder()
            val actions = PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_SEEK_TO or
                    PlaybackState.ACTION_STOP

            stateBuilder.setActions(actions)

            val androidState = when (estado) {
                EstadoReproductor.REPRODUCIENDO -> PlaybackState.STATE_PLAYING
                EstadoReproductor.PAUSADO -> PlaybackState.STATE_PAUSED
                EstadoReproductor.CARGANDO -> PlaybackState.STATE_BUFFERING
                EstadoReproductor.DETENIDO -> PlaybackState.STATE_STOPPED
            }

            stateBuilder.setState(androidState, posicionMs, velocidad)
            session.setPlaybackState(stateBuilder.build())
        } catch (e: Exception) {
            Log.e("TEAM_TX_REPRODUCTOR", "Error actualizando PlaybackState: ${e.message}")
        }
    }

    fun actualizarMetadatos(
        cancion: CancionMotera?,
        caratulaBitmap: Bitmap? = null
    ) {
        val session = mediaSession ?: return
        if (cancion == null) return

        try {
            val metadataBuilder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, cancion.titulo)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, cancion.artista)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, cancion.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, cancion.duracionMs)

            caratulaBitmap?.let {
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it)
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, it)
            }

            session.setMetadata(metadataBuilder.build())
        } catch (e: Exception) {
            Log.e("TEAM_TX_REPRODUCTOR", "Error actualizando MediaMetadata: ${e.message}")
        }
    }

    fun liberar() {
        try {
            mediaSession?.isActive = false
            mediaSession?.release()
            mediaSession = null
            Log.d("TEAM_TX_REPRODUCTOR", "MediaSession liberado limpiamente")
        } catch (e: Exception) {
            Log.e("TEAM_TX_REPRODUCTOR", "Error liberando MediaSession: ${e.message}")
        }
    }
}
