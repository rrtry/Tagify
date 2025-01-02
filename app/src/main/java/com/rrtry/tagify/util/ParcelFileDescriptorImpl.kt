package com.rrtry.tagify.util

import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os.ftruncate
import android.system.Os.lseek
import android.system.Os.pread
import android.system.Os.pwrite
import android.system.OsConstants
import java.io.Closeable
import java.io.IOException

class ParcelFileDescriptorImpl(var pfd: ParcelFileDescriptor) : FileReference {

    private var filePointer = 0L

    private fun <T> rethrowIOException(f: () -> T): T {
        return try { f() } catch (e: ErrnoException) { throw IOException(e) }
    }

    override fun setFileReference(ref: Closeable) {
        if (ref is ParcelFileDescriptor) {
            pfd.close()
            pfd = ref
        }
    }

    override fun getFilePointer(): Long {
        return filePointer
    }

    override fun length(): Long {
        val length = rethrowIOException { lseek(pfd.fileDescriptor, 0, OsConstants.SEEK_END) }
        rethrowIOException { lseek(pfd.fileDescriptor, filePointer, OsConstants.SEEK_SET) }
        return length
    }

    override fun read(): Int {
        val buffer = ByteArray(1)
        read(buffer)
        return buffer.first()
            .toUByte()
            .toInt()
    }

    override fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val read = rethrowIOException { pread(pfd.fileDescriptor, b, off, b.size, filePointer) }
        filePointer += read
        return read
    }

    override fun seek(pos: Long) {
        if (pos < 0) throw IOException("Negative seek offset")
        filePointer = rethrowIOException { lseek(pfd.fileDescriptor, pos, OsConstants.SEEK_SET) }
    }

    override fun setLength(newLength: Long) {
        rethrowIOException { ftruncate(pfd.fileDescriptor, newLength) }
    }

    override fun write(b: ByteArray) {
       filePointer += rethrowIOException { pwrite(pfd.fileDescriptor, b, 0, b.size, filePointer) }
    }

    override fun write(b: Int) {
        rethrowIOException { write(ByteArray(1).apply { this[0] = b.toByte() }) }
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        filePointer += rethrowIOException { pwrite(pfd.fileDescriptor, b, off, len, filePointer) }
    }

    override fun close() {
        pfd.close()
    }
}