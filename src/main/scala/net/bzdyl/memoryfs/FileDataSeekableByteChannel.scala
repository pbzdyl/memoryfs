package net.bzdyl.memoryfs

import java.nio.channels.SeekableByteChannel
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException

class FileDataSeekableByteChannel(val file: FileData) extends SeekableByteChannel {
	private[this] var open = true
	private[this] var position: Long = 0
	
	def truncate(size: Long) = synchronized {
	  checkOpen()
	  file.truncate(size)
	  this
	}
	
	def size(): Long = synchronized {
	  checkOpen()
	  file.size
	}
	
	def position(newPosition: Long) = synchronized {
	  checkOpen()
	  if (newPosition < 0) {
	    throw new IllegalArgumentException("position cannot be negative: " + newPosition)
	  }
	  position = newPosition
	  this
	}
	
	def position(): Long = synchronized {
	  checkOpen()
	  position
	}
	
	def write(src: ByteBuffer): Int = synchronized {
	  checkOpen()
	  val bytesWritten = file.write(position, src)
	  position += bytesWritten
	  bytesWritten
	}
	
	def read(dst: ByteBuffer): Int = synchronized {
	  checkOpen()
	  val bytesRead = file.read(position, dst)
	  if (bytesRead > 0) position += bytesRead
	  bytesRead
	}
	
	def close(): Unit = synchronized {
	  open = false
	}
	
	def isOpen(): Boolean = synchronized { open }
	
	private def checkOpen() = synchronized { if (!open) throw new ClosedChannelException }
}
