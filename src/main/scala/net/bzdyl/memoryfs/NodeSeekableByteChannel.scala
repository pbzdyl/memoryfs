package net.bzdyl.memoryfs

import java.nio.channels.SeekableByteChannel
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException

class NodeSeekableByteChannel(val file: FileData) extends SeekableByteChannel {
	@volatile private[this] var open = true
	@volatile private[this] var position: Long = 0
	
	def truncate(size: Long) = {
	  checkOpen()
	  file.truncate(size)
	  this
	}
	
	def size(): Long = {
	  checkOpen()
	  file.size
	}
	
	def position(newPosition: Long) = {
	  checkOpen()
	  if (newPosition < 0) {
	    throw new IllegalArgumentException("position cannot be negative: " + newPosition)
	  }
	  position = newPosition
	  this
	}
	
	def position(): Long = {
	  checkOpen()
	  position
	}
	
	def write(src: ByteBuffer): Int = {
	  checkOpen()
	  0
	}
	
	def read(dst: ByteBuffer): Int = {
	  checkOpen()
	  val bytesRead = file.read(position, dst)
	  if (bytesRead > 0) position += bytesRead
	  bytesRead
	}
	
	def close(): Unit = synchronized {
	  open = false
	}
	
	def isOpen(): Boolean = open
	
	private def checkOpen() = if (!open) throw new ClosedChannelException
}
