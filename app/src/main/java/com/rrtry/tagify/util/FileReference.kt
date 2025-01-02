package com.rrtry.tagify.util

import com.jtagger.FileWrapper
import java.io.Closeable

interface FileReference : FileWrapper {

    fun setFileReference(ref: Closeable)
}