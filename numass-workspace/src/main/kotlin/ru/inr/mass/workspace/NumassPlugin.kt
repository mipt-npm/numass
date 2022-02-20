package ru.inr.mass.workspace

import ru.inr.mass.data.proto.NumassProtoPlugin
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.values.Value
import space.kscience.dataforge.workspace.TaskReference
import space.kscience.dataforge.workspace.WorkspacePlugin
import space.kscience.dataforge.workspace.task
import space.kscience.tables.Table
import kotlin.reflect.KClass

class NumassPlugin : WorkspacePlugin() {
    override val tag: PluginTag get() = Companion.tag

    val numassProtoPlugin by require(NumassProtoPlugin)

//    val select by task<NumassSet>(
//        descriptor = MetaDescriptor {
//            info = "Select data from workspace data pool"
//            value("forward", ValueType.BOOLEAN) {
//                info = "Select only forward or only backward sets"
//            }
//        }
//    ) {
//        val forward = meta["forward"]?.boolean
//        val filtered = workspace.data.select<NumassSet> { _, meta ->
//            when (forward) {
//                true -> meta["iteration_info.reverse"]?.boolean?.not() ?: false
//                false -> meta["iteration_info.reverse"]?.boolean ?: false
//                else -> true
//            }
//        }
//
//        emit(Name.EMPTY, filtered)
//    }
//
//    val analyze by task<Table<Value>>(
//        MetaDescriptor {
//            info = "Count the number of events for each voltage and produce a table with the results"
//        }
//    ) {
//        pipeFrom(select) { set, name, meta ->
//            val res = SmartAnalyzer.analyzeSet(set, meta["analyzer"] ?: Meta.EMPTY)
//            val outputMeta = meta.toMutableMeta().apply {
//                "data" put set.meta
//            }
//           // context.output.render(res, stage = "numass.analyze", name = name, meta = outputMeta)
//            res
//        }
//    }

    val monitorTableTask: TaskReference<Table<Value>> by task {

//        descriptor {
//            value("showPlot", types = listOf(ValueType.BOOLEAN), info = "Show plot after complete")
//            value("monitorPoint", types = listOf(ValueType.NUMBER), info = "The voltage for monitor point")
//        }
//        model { meta ->
//            dependsOn(selectTask, meta)
////        if (meta.getBoolean("monitor.correctForThreshold", false)) {
////            dependsOn(subThresholdTask, meta, "threshold")
////        }
//            configure(meta.getMetaOrEmpty("monitor"))
//            configure {
//                meta.useMeta("analyzer") { putNode(it) }
//                setValue("@target", meta.getString("@target", meta.name))
//            }
//        }
//        join<NumassSet, Table> { data ->
//            val monitorVoltage = meta.getDouble("monitorPoint", 16000.0);
//            val analyzer = SmartAnalyzer()
//            val analyzerMeta = meta.getMetaOrEmpty("analyzer")
//
//            //val thresholdCorrection = da
//            //TODO add separator labels
//            val res = ListTable.Builder("timestamp", "count", "cr", "crErr", "index", "set")
//                .rows(
//                    data.values.stream().flatMap { set ->
//                        set.points.stream()
//                            .filter { it.voltage == monitorVoltage }
//                            .parallel()
//                            .map { point ->
//                                analyzer.analyzeParent(point, analyzerMeta).edit {
//                                    "index" to point.index
//                                    "set" to set.name
//                                }
//                            }
//                    }
//
//                ).build()
//
//            if (meta.getBoolean("showPlot", true)) {
//                val plot = DataPlot.plot(name, res, Adapters.buildXYAdapter("timestamp", "cr", "crErr"))
//                context.plot(plot, name, "numass.monitor") {
//                    "xAxis.title" to "time"
//                    "xAxis.type" to "time"
//                    "yAxis.title" to "Count rate"
//                    "yAxis.units" to "Hz"
//                }
//
//                ((context.output["numass.monitor", name] as? PlotOutput)?.frame as? JFreeChartFrame)?.addSetMarkers(data.values)
//            }
//
//            context.output.render(res, stage = "numass.monitor", name = name, meta = meta)
//
//            return@join res;
//        }
    }

    companion object : PluginFactory<NumassPlugin> {
        override val tag: PluginTag = PluginTag("numass", "ru.mipt.npm")
        override val type: KClass<out NumassPlugin> = NumassPlugin::class
        override fun invoke(meta: Meta, context: Context): NumassPlugin = NumassPlugin()
    }
}