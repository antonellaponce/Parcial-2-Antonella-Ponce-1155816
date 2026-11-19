package ar.jornadas.data

import androidx.room.*
import retrofit2.http.GET
import kotlinx.coroutines.flow.Flow

data class FichaRemota(val id: String, val title: String, val track: String, val day: Int,
    val time: String, val room: String, val speaker: String, val summary: String)

interface ProgramaApi {
    @GET("programa") suspend fun consultar(): List<FichaRemota>
}

@Entity(tableName = "charlas")
data class Charla(@PrimaryKey val id: String, val titulo: String, val tema: String, val dia: Int,
    val hora: String, val sala: String, val ponente: String, val resumen: String,
    val favorita: Boolean = false)

@Dao
abstract class CarteleraDao {
    @Query("SELECT * FROM charlas ORDER BY dia, hora, sala")
    abstract fun observar(): Flow<List<Charla>>

    @Query("SELECT * FROM charlas ORDER BY dia, hora, sala")
    abstract suspend fun leer(): List<Charla>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardar(fichas: List<Charla>)

    @Query("SELECT id FROM charlas WHERE favorita = 1")
    abstract suspend fun favoritasActuales(): List<String>

    @Query("DELETE FROM charlas")
    abstract suspend fun borrarCatalogo()

    @Query("SELECT descargado FROM estado_programa WHERE id = 1")
    abstract fun observarDescarga(): Flow<Boolean?>

    @Query("SELECT descargado FROM estado_programa WHERE id = 1")
    abstract suspend fun leerDescarga(): Boolean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardarEstado(estado: EstadoPrograma)

    @Transaction
    open suspend fun reemplazar(fichas: List<Charla>) {
        // Se leen después de la petición, dentro de la misma transacción de escritura.
        val favoritas = favoritasActuales().toSet()
        borrarCatalogo()
        guardar(fichas.map { it.copy(favorita = it.id in favoritas) })
        guardarEstado(EstadoPrograma(1, true))
    }

    @Query("UPDATE charlas SET favorita = NOT favorita WHERE id = :id")
    abstract suspend fun alternar(id: String)
}

@Entity(tableName = "estado_programa")
data class EstadoPrograma(@PrimaryKey val id: Int, val descargado: Boolean)

data class ProgramaGuardado(val charlas: List<Charla>, val edicionValida: Boolean)

@Database(entities = [Charla::class, EstadoPrograma::class], version = 2, exportSchema = false)
abstract class Archivo : RoomDatabase() {
    abstract fun cartelera(): CarteleraDao
}
