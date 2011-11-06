package net.bzdyl.memoryfs

import scala.collection.mutable
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.Lock
import java.nio.ByteBuffer

class FileData extends mutable.ArrayBuffer[Byte] with ReadWriteLockBuffer[Byte] {
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

class DirectoryEntries extends mutable.HashSet[Node] with mutable.SynchronizedSet[Node]

sealed trait Node {
  val filesystem: MemoryFileSystem
  val parent: Option[Directory]
  val name: String
  def toPath(): ResolvedMFSPath = {
    def buildPath(n: Option[Node], buffer: List[Node]): String = {
      n match {
        case Some(node) => buildPath(node.parent, node :: buffer)
        case None => buffer.mkString("/", "/", "")
      }
    }
    val stringPath = buildPath(Some(this), Nil)
    new ResolvedMFSPath(this, filesystem, stringPath)
  }
}

class File(val filesystem: MemoryFileSystem, val parent: Option[Directory], val name: String)
extends FileData
with Node

class Directory(val filesystem: MemoryFileSystem, val parent: Option[Directory], val name: String)
extends DirectoryEntries
with Node {
  def createNewFile(name: String): Option[File] = synchronized {
    if (!contains(name)) {
    	val newFile = new File(filesystem, Some(this), name)
    	add(newFile)
    	Some(newFile)
    } else {
      None
    }
  }
  
  def contains(name: String): Boolean = synchronized {
    locate(name).isDefined
  }
  
  def locate(name: String): Option[Node] = {
    find(n => n.name == name)
  }
  
  def createOrExisting(name: String): Node = synchronized {
    locate(name) match {
      case Some(node) => node
      case None =>
        val newFile = new File(filesystem, Some(this), name)
        add(newFile)
        newFile
    }
  }
}

class SymLink(val filesystem: MemoryFileSystem, val parent: Option[Directory], val name: String, val target: Node)
extends Node
