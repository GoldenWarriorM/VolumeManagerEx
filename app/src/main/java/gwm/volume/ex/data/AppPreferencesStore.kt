package gwm.volume.ex.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AppPreferencesStore(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val key = stringPreferencesKey("apps")

        private val json = Json { ignoreUnknownKeys = true }
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    @Serializable
    private data class SerializedState(
        val values: MutableList<AppPreferences>,
        val indices: MutableMap<String, Int>,
        val bubble: BubblePreferences = BubblePreferences(),
        val systemSliderVisibility: MutableMap<String, Boolean> = mutableMapOf(),
        val excludedPackages: Set<String> = setOf("android", "com.android.systemui")
    )

    private val lock = Any()
    private var state = SerializedState(mutableListOf(), mutableMapOf())
    val values: List<AppPreferences>
        get() = synchronized(lock) { state.values.toList() }
    val indices: Map<String, Int>
        get() = synchronized(lock) { state.indices.toMap() }
    val bubble: BubblePreferences
        get() = synchronized(lock) { state.bubble.copy() }

    fun getSystemSliderVisible(id: String): Boolean {
        return synchronized(lock) { state.systemSliderVisibility[id] ?: true }
    }

    fun setSystemSliderVisible(id: String, value: Boolean) {
        val changed = synchronized(lock) {
            val oldValue = state.systemSliderVisibility[id] ?: true
            if (oldValue == value) {
                return@synchronized false
            }

            val updated = state.systemSliderVisibility.toMutableMap()
            updated[id] = value
            state = state.copy(systemSliderVisibility = updated)
            true
        }

        if (changed) {
            save()
        }
    }

    var systemSliderVisibility: Map<String, Boolean>
        get() = synchronized(lock) { state.systemSliderVisibility.toMap() }
        set(value) {
            val changed = synchronized(lock) {
                if (state.systemSliderVisibility == value) {
                    return@synchronized false
                }

                state = state.copy(systemSliderVisibility = value.toMutableMap())
                true
            }

            if (changed) {
                save()
            }
        }

    fun track(onChange: (first: Boolean) -> Unit) {
        var first = true

        scope.launch {
            dataStore.data.collect { preferences ->
                val valueJson = preferences[key]
                if (valueJson != null) {
                    synchronized(lock) {
                        state = json.decodeFromString<SerializedState>(valueJson)
                    }
                }

                onChange(first)
                @Suppress("AssignedValueIsNeverRead")
                first = false
            }
        }
    }

    fun getOrCreate(packageName: String): AppPreferences {
        synchronized(lock) {
            val index = state.indices[packageName]
            if (index != null) {
                return state.values[index]
            }

            val value = AppPreferences()
            state.indices[packageName] = state.values.size
            state.values.add(value)
            return value
        }
    }

    fun save() {
        scope.launch {
            dataStore.edit { preferences ->
                preferences[key] = Json.encodeToString(state)
            }
        }
    }

    val excludedPackages: Set<String>
        get() = synchronized(lock) { state.excludedPackages }

    fun setExcludedPackages(value: Set<String>) {
        val changed = synchronized(lock) {
            if (state.excludedPackages == value) return@synchronized false
            state = state.copy(excludedPackages = value)
            true
        }
        if (changed) save()
    }

    fun setBubble(value: BubblePreferences) {
        val changed = synchronized(lock) {
            if (state.bubble == value) {
                return@synchronized false
            }

            state = state.copy(bubble = value)
            true
        }

        if (changed) {
            save()
        }
    }
}
