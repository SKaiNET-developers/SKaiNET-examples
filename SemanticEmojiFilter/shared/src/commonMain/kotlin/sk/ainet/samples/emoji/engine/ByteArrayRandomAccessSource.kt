package sk.ainet.samples.emoji.engine

import sk.ainet.io.RandomAccessSource

/**
 * In-memory [RandomAccessSource] over a fully materialised byte array. The web build has no
 * filesystem: the SafeTensors files arrive as Compose resource bytes and the streaming reader
 * wants random access, so this wraps them. Desktop and Android read straight off disk instead.
 */
class ByteArrayRandomAccessSource(private val bytes: ByteArray) : RandomAccessSource {
    override val size: Long = bytes.size.toLong()

    override fun readAt(position: Long, length: Int): ByteArray {
        require(position >= 0 && length >= 0 && position + length <= size) { "read [$position, +$length) outside $size bytes" }
        return bytes.copyOfRange(position.toInt(), (position + length).toInt())
    }

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, length: Int): Int {
        val n = minOf(length, (size - position).toInt())
        bytes.copyInto(buffer, offset, position.toInt(), position.toInt() + n)
        return n
    }

    override fun close() {}
}
