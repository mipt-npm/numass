package ru.inr.mass.workspace

import ru.inr.mass.data.analysis.SmartAnalyzer
import ru.inr.mass.data.api.NumassSet
import ru.inr.mass.data.proto.NumassProtoPlugin
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.data.select
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.boolean
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.value
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.toMutableMeta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.tables.Table
import space.kscience.dataforge.values.Value
import space.kscience.dataforge.values.ValueType
import space.kscience.dataforge.workspace.WorkspacePlugin
import space.kscience.dataforge.workspace.pipeFrom
import space.kscience.dataforge.workspace.task
import kotlin.reflect.KClass

class NumassPlugin : WorkspacePlugin() {
    override val tag: PluginTag get() = Companion.tag

    val numassProtoPlugin by require(NumassProtoPlugin)

    val select by task<NumassSet>(
        descriptor = MetaDescriptor {
            info = "Select data from workspace data pool"
            value("forward", ValueType.BOOLEAN) {
                info = "Select only forward or only backward sets"
            }
        }
    ) {
        val forward = meta["forward"]?.boolean
        val filtered = workspace.data.select<NumassSet> { _, meta ->
            when (forward) {
                true -> meta["iteration_info.reverse"]?.boolean?.not() ?: false
                false -> meta["iteration_info.reverse"]?.boolean ?: false
                else -> true
            }
        }

        emit(Name.EMPTY, filtered)
    }

    val analyze by task<Table<Value>>(
        MetaDescriptor {
            info = "Count the number of events for each voltage and produce a table with the results"
        }
    ) {
        pipeFrom(select) { set, name, meta ->
            val res = SmartAnalyzer.analyzeSet(set, meta["analyzer"] ?: Meta.EMPTY)
            val outputMeta = meta.toMutableMeta().apply {
                "data" put set.meta
            }
           // context.output.render(res, stage = "numass.analyze", name = name, meta = outputMeta)
            res
        }
    }

    companion object : PluginFactory<NumassPlugin> {
        override val tag: PluginTag = PluginTag("numass", "ru.mipt.npm")
        override val type: KClass<out NumassPlugin> = NumassPlugin::class
        override fun invoke(meta: Meta, context: Context): NumassPlugin = NumassPlugin()
    }
}