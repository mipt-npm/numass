package ru.inr.mass.data.proto

import io.ktor.utils.io.core.*
import java.io.InputStream

// TODO move to dataforge-io

/**
 * Sequentially read Utf8 lines from the input until it is exhausted
 */
public fun Input.lines(): Sequence<String> = sequence {
    while (!endOfInput) {
        readUTF8Line()?.let { yield(it) }
    }
}

private class InputAsInputStream(val input: Input) : InputStream() {


    override fun read(): Int = input.run {
        if (endOfInput) {
            -1
        } else {
            readUByte().toInt()
        }
    }

    override fun readAllBytes(): ByteArray = input.readBytes()

    override fun read(b: ByteArray): Int = input.readAvailable(b)

    override fun close() {
        input.close()
    }
}

public fun Input.asInputStream(): InputStream = InputAsInputStream(this)