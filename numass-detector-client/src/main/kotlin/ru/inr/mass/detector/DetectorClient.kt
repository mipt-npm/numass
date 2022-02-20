package ru.inr.mass.detector

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import ru.inr.mass.data.proto.TaggedNumassEnvelopeFormat
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.io.*
import space.kscience.dataforge.meta.MutableMeta
import java.net.InetSocketAddress

public class DetectorClient(override val context: Context) : ContextAware {
    private val format: EnvelopeFormat = TaggedNumassEnvelopeFormat(context.io)

    private var connection: Connection? = null

    private suspend fun request(metaBuilder: MutableMeta.() -> Unit): Envelope {
        return connection?.let { connection ->
//            val requestEnvelope = Envelope {
//                meta(metaBuilder)
//            }
//            val request = format.toBinary(requestEnvelope).toByteArray()
//            connection.output.writeAvailable(request)
//            val response = connection.input.read



            connection.output.цкasOut
        } ?: error("Not connected")
    }

    public suspend fun connect(host: String, port: Int) {
        connection = aSocket(ActorSelectorManager(Dispatchers.IO))
            .tcp().connect(InetSocketAddress(host, port)).connection()
    }
}