package ru.inr.mass.workspace

import hep.dataforge.workspace.Workspace
import ru.inr.mass.data.proto.NumassProtoPlugin

val NUMASS = Workspace {
    context("NUMASS") {
        plugin(NumassProtoPlugin)
    }
}