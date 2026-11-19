package ar.jornadas

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ar.jornadas.data.*
import ar.jornadas.lab.ServicioLocal
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Servicios private constructor(context: Context) {
    val mesa = ServicioLocal(context)
    private val archivo = Room.databaseBuilder(context, Archivo::class.java, "cartelera.db")
        .addMigrations(object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS estado_programa (id INTEGER NOT NULL, descargado INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("INSERT INTO estado_programa (id, descargado) SELECT 1, CASE WHEN EXISTS (SELECT 1 FROM charlas) THEN 1 ELSE 0 END")
            }
        }).build()
    private val api = Retrofit.Builder().baseUrl("https://programa.invalid/")
        .client(OkHttpClient.Builder().addInterceptor(mesa).build())
        .addConverterFactory(GsonConverterFactory.create()).build().create(ProgramaApi::class.java)
    val agenda = AgendaRepository(api, archivo)

    companion object {
        @Volatile private var instancia: Servicios? = null

        fun obtener(context: Context): Servicios = instancia ?: synchronized(this) {
            instancia ?: Servicios(context.applicationContext).also { instancia = it }
        }
    }
}
