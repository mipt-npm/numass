package ru.inr.mass.scripts

import com.google.cloud.storage.contrib.nio.CloudStorageFileSystem
import ru.inr.mass.data.proto.NumassDirectorySet
import ru.inr.mass.workspace.Numass
import space.kscience.dataforge.data.DataTree

// https://storage.cloud.google.com/numass-data/2020_12/Adiabaticity/16/set_3/p10(10s)(HV1%3D13750)

suspend fun main() {
    CloudStorageFileSystem.forBucket("numass-data").use { fs ->
        val repo: DataTree<NumassDirectorySet> = Numass.readRepository(fs.getPath("2020_12/Adiabaticity"))
        repo.items().forEach{ (key,item)->
            println(key)
        }
    }
}