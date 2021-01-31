package ru.inr.mass.workspace

import ru.inr.mass.data.proto.NumassDirectorySet
import ru.inr.mass.data.proto.readNumassDirectory

fun readNumassDirectory(path: String): NumassDirectorySet = NUMASS.context.readNumassDirectory(path)