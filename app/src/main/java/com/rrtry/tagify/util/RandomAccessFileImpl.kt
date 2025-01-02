package com.rrtry.tagify.util

import java.io.Closeable
import java.io.RandomAccessFile

class RandomAccessFileImpl(var file: RandomAccessFile): FileReference {

    override fun setFileReference(ref: Closeable) {
        if (ref is RandomAccessFile) {
            file.close()
            file = ref
        }
    }

    override fun getFilePointer(): Long {
        return file.filePointer
    }

    override fun length(): Long {
        return file.length()
    }

    override fun read(): Int {
        return file.read()
    }

    override fun read(buff: ByteArray): Int {
        return file.read(buff)
    }

    override fun read(buff: ByteArray?, off: Int, len: Int): Int {
        return file.read(buff, off, len)
    }

    override fun write(buff: ByteArray?) {
        file.write(buff)
    }

    override fun write(b: Int) {
        file.write(b)
    }

    override fun write(buff: ByteArray?, off: Int, len: Int) {
        file.write(buff, off, len)
    }

    override fun close() {
        file.close()
    }

    override fun seek(pos: Long) {
        file.seek(pos)
    }

    override fun setLength(len: Long) {
        file.setLength(len)
    }
}