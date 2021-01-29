package ru.inr.mass.workspace

import hep.dataforge.workspace.Workspace
import ru.inr.mass.data.proto.NumassProtoPlugin

val numass = Workspace {
    context("numass") {
        plugin(NumassProtoPlugin)
    }
}