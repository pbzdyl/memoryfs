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
}

class DirectoryEntries extends mutable.HashSet[Node] with mutable.SynchronizedSet[Node]

sealed trait Node {
  val filesystem: MemoryFileSystem
  val parent: Option[Directory]
  val name: String
  def toPath(): ResolvedPath = {
    def buildPath(n: Option[Node], buffer: List[Node]): String = {
      n match {
        case Some(node) => buildPath(node.parent, node :: buffer)
        case None => buffer.mkString("/", "/", "")
      }
    }
    val stringPath = buildPath(Some(this), Nil)
    new ResolvedPath(filesystem, stringPath, this)
  }
}

class File(val filesystem: MemoryFileSystem, val parent: Option[Directory], val name: String)
extends FileData
with Node

class Directory(val filesystem: MemoryFileSystem, val parent: Option[Directory], val name: String)
extends DirectoryEntries
with Node

class SymLink(val filesystem: MemoryFileSystem, val parent: Option[Directory], val name: String, val target: Node)
extends Node
