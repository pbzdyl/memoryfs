package net.bzdyl.memoryfs

import java.nio.channels.SeekableByteChannel
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.file.attribute.FileAttribute
import java.nio.file.OpenOption
import java.nio.channels.NonWritableChannelException
import java.nio.channels.NonReadableChannelException



import WriteMode._

class FileDataSeekableByteChannel(val file: FileData, writeMode: WriteMode, readMode: Boolean) extends SeekableByteChannel {
  def this(file: FileData) = this(file, DontWrite, true)
  private[this] var open = true
  protected var pos: Long = 0

  def size(): Long = synchronized {
    checkOpen()
    file.size
  }

  def position(newPosition: Long) = synchronized {
    checkOpen()
    if (newPosition < 0) {
      throw new IllegalArgumentException("position cannot be negative: " + newPosition)
    }
    pos = newPosition
    this
  }

  def position(): Long = synchronized {
    checkOpen()
    pos
  }

  def truncate(size: Long): FileDataSeekableByteChannel = synchronized {
    if (writeMode == DontWrite) {
   	  throw new NonWritableChannelException
    } else {
      checkOpen()
      file.truncate(size)
      this
    }
  }
  
  def write(src: ByteBuffer): Int = synchronized {
    writeMode match {
      case Append =>
        checkOpen()
        val (bytesWritten, size) = file.append(src)
        pos = size
        bytesWritten
      case Write =>
        checkOpen()
        val bytesWritten = file.write(position, src)
        pos += bytesWritten
        bytesWritten
      case DontWrite =>
        throw new NonWritableChannelException
    }
  }
  
  def read(dst: ByteBuffer): Int = synchronized {
    checkOpen()
    if (readMode) {
	    val bytesRead = file.read(position, dst)
	    if (bytesRead > 0) pos += bytesRead
	    bytesRead
    } else {
        throw new NonReadableChannelException
    }
  }

  def close(): Unit = synchronized {
    open = false
  }

  def isOpen(): Boolean = synchronized { open }

  protected def checkOpen() = synchronized { if (!open) throw new ClosedChannelException }
}
