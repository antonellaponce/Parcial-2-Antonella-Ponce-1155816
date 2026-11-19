package ar.jornadas.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import androidx.room.withTransaction

class AgendaRepository(private val api: ProgramaApi, private val archivo: Archivo) {
    private val dao = archivo.cartelera()
    // Patrón del viernes: las señales disparan una lectura coherente de ambas tablas.
    val programa: Flow<ProgramaGuardado> =
        combine(dao.observar(), dao.observarDescarga()) { _, _ -> Unit }
            .map {
                archivo.withTransaction {
                    ProgramaGuardado(dao.leer(), dao.leerDescarga() == true)
                }
            }.distinctUntilChanged()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _cargando = MutableStateFlow(false)
    val cargando = _cargando.asStateFlow()
    private val _aviso = MutableStateFlow("")
    val aviso = _aviso.asStateFlow()
    private var cargaAutomaticaIniciada = false

    fun iniciar() {
        if (cargaAutomaticaIniciada) return
        cargaAutomaticaIniciada = true
        recargar()
    }

    fun recargar() {
        // Los eventos de la UI llegan al hilo principal; se bloquea antes del launch.
        if (_cargando.value) return
        _cargando.value = true
        _aviso.value = ""
        scope.launch {
            try {
                sincronizar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aviso.value = "No se pudo consultar el programa"
            } finally {
                _cargando.value = false
            }
        }
    }

    private suspend fun sincronizar() {
        val fichas = api.consultar().map {
            Charla(it.id, it.title, it.track, it.day, it.time, it.room, it.speaker, it.summary)
        }
        dao.reemplazar(fichas)
    }

    suspend fun cambiarFavorita(id: String) = dao.alternar(id)
}
