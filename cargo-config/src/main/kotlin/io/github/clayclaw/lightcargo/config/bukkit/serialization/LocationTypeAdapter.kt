package io.github.clayclaw.lightcargo.config.bukkit.serialization

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import io.github.clayclaw.lightcargo.config.provider.GsonTypeAdapterComponent
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.*

class LocationTypeAdapter: GsonTypeAdapterComponent<Location> {
    override val type = Location::class.java
    override val adapter: TypeAdapter<Location> = LocationTypeAdapterImpl()

    class LocationTypeAdapterImpl: TypeAdapter<Location>() {
        override fun write(writer: JsonWriter, value: Location?) {
            when (value) {
                null -> writer.nullValue()
                else -> writer.value("${value.world!!.uid} ${value.x} ${value.y} ${value.z} ${value.yaw} ${value.pitch}")
            }
        }

        override fun read(reader: JsonReader): Location? = when (reader.peek()) {
            JsonToken.NULL -> null
            else -> reader.nextString().split(" ").let {
                Location(
                    Bukkit.getWorld(UUID.fromString(it[0])),
                    it[1].toDouble(), it[2].toDouble(), it[3].toDouble(),
                    it[4].toFloat(), it[5].toFloat()
                )
            }
        }
    }
}