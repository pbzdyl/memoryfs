package net.bzdyl.memoryfs

import java.nio.ByteBuffer

class FileData extends collection.mutable.ArrayBuffer[Byte] with ReadWriteLockBuffer[Byte] {
  def truncate(size: Long) = withWriteLock {
    if (size < 0) {
      throw new IllegalArgumentException("size cannot be negative: " + size)
    }
  
    if (size < this.size) {
      reduceToSize(size.toInt)
    }
  }
  
  def read(fromPosition: Long, dst: ByteBuffer): Int = withReadLock {
    if (fromPosition >= size) {
      -1
    } else {
	    val data = view(fromPosition.toInt, fromPosition.toInt + dst.remaining())
	    val readCount = data.size
	    data.foreach(b => dst.put(b))
	    readCount
    }
  }
  
  def append(src: ByteBuffer): (Int, Int) = withWriteLock { (write(size, src), size) }
  
  def write(fromPosition: Long, src: ByteBuffer): Int = withWriteLock {
    for (i <- this.size until fromPosition.toInt) {
      this += 0
    }
    
    val writtenCount = src.remaining()
    
    val range = this.indices.drop(fromPosition.toInt)
    for (i <- range if src.hasRemaining()) {
      this(i) = src.get()
    }
    
    while (src.hasRemaining()) {
      this += src.get()
    }
    
    writtenCount
  }
}
