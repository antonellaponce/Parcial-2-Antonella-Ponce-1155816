package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import ar.jornadas.Servicios
import ar.jornadas.data.ProgramaGuardado
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProgramaModel(app: Application, private val estado: SavedStateHandle) : AndroidViewModel(app) {
    private val servicios = Servicios.obtener(app)
    val mesa = servicios.mesa
    private val repo = servicios.agenda
    val programa: StateFlow<ProgramaGuardado?> =
        repo.programa.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val dia = estado.getStateFlow("dia", 0)
    val tema = estado.getStateFlow("tema", "Todos")
    val aviso = repo.aviso
    val cargando = repo.cargando

    fun elegirDia(valor: Int) { estado["dia"] = valor }
    fun elegirTema(valor: String) { estado["tema"] = valor }
    fun iniciar() = repo.iniciar()
    fun recargar() = repo.recargar()

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }
}
