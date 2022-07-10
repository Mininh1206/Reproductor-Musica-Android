package com.example.reproductor.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import com.example.reproductor.Cancion
import com.example.reproductor.MainActivity
import com.example.reproductor.R
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.ArrayList

class AdminSQLiteOpenHelper(
    context: Context?,
    name: String?,
    factory: SQLiteDatabase.CursorFactory?,
    version: Int,
) : SQLiteOpenHelper(context, name, factory, version) {

    override fun onCreate(db: SQLiteDatabase) {
        val usuario = "CREATE TABLE IF NOT EXISTS usuario (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, username TEXT NOT NULL UNIQUE, contraseña TEXT NOT NULL, sexo TEXT NOT NULL, fechaNac TEXT NOT NULL, foto BLOB NOT NULL, rutaVideo TEXT NOT NULL)"
        val cancion = "CREATE TABLE IF NOT EXISTS cancion (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, artista TEXT, uri TEXT UNIQUE, portada BLOB)"
        val cancionUsu = "CREATE TABLE IF NOT EXISTS cancionUsu (usuario INTEGER , cancion INTEGER)"

        db.execSQL(usuario)
        db.execSQL(cancion)
        db.execSQL(cancionUsu)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }

    fun reiniciarTablas(){
        val db = writableDatabase

        var usuario = "DROP TABLE usuario"
        var cancion = "DROP TABLE cancion"
        var cancionUsu = "DROP TABLE cancionUsu"

        db.execSQL(usuario)
        db.execSQL(cancion)
        db.execSQL(cancionUsu)

        usuario = "CREATE TABLE IF NOT EXISTS usuario (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, username TEXT NOT NULL UNIQUE, contraseña TEXT NOT NULL, sexo TEXT NOT NULL, fechaNac TEXT NOT NULL, foto BLOB NOT NULL, rutaVideo TEXT NOT NULL)"
        cancion = "CREATE TABLE IF NOT EXISTS cancion (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, artista TEXT, uri TEXT UNIQUE, portada BLOB)"
        cancionUsu = "CREATE TABLE IF NOT EXISTS cancionUsu (usuario INTEGER , cancion INTEGER)"

        db.execSQL(usuario)
        db.execSQL(cancion)
        db.execSQL(cancionUsu)
    }

    fun insertarUsuario(context: Context?, nombre: String, username: String, contraseña: String, sexo: String, fechaNac: String, foto: ByteArray, rutaVideo: String): Boolean {
        try{
            val db = writableDatabase
            val sql = "INSERT INTO usuario (nombre, username, contraseña, sexo, fechaNac, foto, rutaVideo) VALUES (?, ?, ?, ?, ?, ?, ?)"
            val statement = db.compileStatement(sql)
            statement.clearBindings()
            statement.bindString(1, nombre)
            statement.bindString(2, username)
            statement.bindString(3, contraseña)
            statement.bindString(4, sexo)
            statement.bindString(5, fechaNac)
            statement.bindBlob(6, foto)
            statement.bindString(7, rutaVideo)
            statement.executeInsert()

            return true
        } catch (d:Exception){
            if (context!=null) Toast.makeText(context, "Ya existe ese usuario", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    fun insertarCancionUsu(idUsu: Int, idCancion: Int){
        try {
            val db = writableDatabase

            db.execSQL("INSERT INTO cancionUsu (usuario, cancion) VALUES ($idUsu, $idCancion)")

        } catch (e:Exception) {

        }
    }

    fun borrarCancionUsu(idUsu: Int, idCancion: Int){
        try {
            val db = writableDatabase

            db.execSQL("DELETE FROM cancionUsu WHERE usuario = $idUsu AND cancion= $idCancion")

        } catch (e:Exception) {

        }
    }

    fun insertarCancion(c:Cancion){
        try{
            val db = writableDatabase

            val sql = "INSERT INTO cancion (nombre, artista, uri, portada) VALUES (?, ?, ?, ?)"
            val statement = db.compileStatement(sql)
            statement.clearBindings()
            statement.bindString(1, c.titulo)
            statement.bindString(2, c.artista)
            statement.bindString(3, c.uri.toString())
            statement.bindBlob(4, bitmapBlob(c.portada!!))
            statement.executeInsert()

        } catch (e:Exception){

        }
    }

    private fun bitmapBlob(bitmap: Bitmap): ByteArray? {
        try{
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return stream.toByteArray()
        } catch (e: Exception){
            return null
        }
    }

    fun sacarCancionesPorUsuario(cancionesT: List<Cancion>, id: Int): ArrayList<Cancion> {
        val canciones = ArrayList<Cancion>()

        val db = readableDatabase
        val r = db.rawQuery("select id, nombre, artista, uri from cancion c join cancionUsu cu on c.id=cu.cancion where cu.usuario=$id and cu.cancion in (select id from cancion) order by nombre", null) // Extraigo las canciones de un usuario

        while (r.moveToNext()){
            val c = Cancion(r.getInt(0), r.getString(1), r.getString(2), Uri.parse(r.getString(3)))

            for (cT in cancionesT){
                if (cT.uri.toString()==c.uri.toString()){
                    canciones.add(c)
                    break
                }
            }
        }

        return canciones
    }

    fun cancionSeleccionada(usuario: Int, cancion: Int): Boolean{
        val db = readableDatabase
        val r = db.rawQuery("select * from cancionUsu where usuario=$usuario and cancion=$cancion", null) // Extraigo las canciones de un usuario

        if (r.moveToFirst()){
            return true
        }

        return false
    }

    fun getUsuario(id:Int): Cursor {
        val db = readableDatabase
        val r = db.rawQuery("SELECT * FROM usuario WHERE id=$id", null)

        return r
    }

    fun getUsuario(usuario: String, contraseña: String): Cursor {
        val db = readableDatabase
        val r = db.rawQuery("SELECT * FROM usuario WHERE username='$usuario' AND contraseña='$contraseña'", null)

        return r
    }

    fun getIdUsuario(usuario: String): Int {
        val db = readableDatabase
        val r = db.rawQuery("SELECT id FROM usuario WHERE username='$usuario'", null)

        if (r.moveToFirst())
            return r.getInt(0)

        return -1;

    }

    fun getIdCancion(uri: String): Int{
        val db = readableDatabase
        val r = db.rawQuery("SELECT id FROM cancion WHERE uri='$uri'", null)

        if (r.moveToFirst())
            return r.getInt(0)

        return -1;
    }

}