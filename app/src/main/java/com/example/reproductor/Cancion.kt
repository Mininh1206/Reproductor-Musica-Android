package com.example.reproductor

import android.graphics.Bitmap
import android.net.Uri

class Cancion {

    var id = 0
        private set
    var titulo: String? = null
        private set
    var artista: String? = null
        private set
    var uri: Uri? = null
        private set
    var portada: Bitmap? = null

    constructor(id: Int, titulo: String, artista: String, uri: Uri, portada: Bitmap) {
        this.id = id
        this.titulo = titulo
        this.artista = artista
        this.uri = uri
        this.portada = portada
    }

    constructor(id: Int, titulo: String, artista: String, uri: Uri) {
        this.id = id
        this.titulo = titulo
        this.artista = artista
        this.uri = uri
    }

}