package ru.inr.mass.workspace

import ru.inr.mass.data.proto.NumassProtoPlugin
import space.kscience.dataforge.workspace.Workspace

val NUMASS = Workspace {
    context{
        plugin(NumassProtoPlugin)
    }
}