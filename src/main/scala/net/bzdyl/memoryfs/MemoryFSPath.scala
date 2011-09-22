package net.bzdyl.memoryfs

import java.nio.file.Path
import java.nio.file.WatchService
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.LinkOption
import java.nio.file.FileSystem
import java.util.Objects._

class MemoryFSPath(val filesystem: MemoryFileSystem, val path: String, val absolute: Boolean) extends Path {
  def this(filesystem: MemoryFileSystem, path: String) = this(filesystem, path, true)
  
  override def compareTo(other: Path): Int = {
    requireNonNull(other)
    toString().compareTo(other.toString())
  }

  override def iterator(): java.util.Iterator[Path] = new java.util.Iterator[Path] {
    var i = 0
    def hasNext(): Boolean = i < getNameCount()
    def next(): Path = {
      if (hasNext()) {
        val result = getName(i)
        i += 1
        result
      } else {
        throw new NoSuchElementException
      }
    }
    def remove() = throw new UnsupportedOperationException
  }

  override def resolveSibling(other: String): Path = resolveSibling(filesystem.getPath(other))
  override def resolveSibling(other: Path): Path = getParent().resolve(other)
  
  override def resolve(other: String): Path = resolve(filesystem.getPath(other))
  override def resolve(other: Path): Path = null
  
  override def toRealPath(options: LinkOption*): Path = toAbsolutePath()
  override def toAbsolutePath(): Path = null
  override def toUri(): java.net.URI = null
  override def relativize(other: Path): Path = null
  override def normalize(): Path = null
  override def endsWith(other: String): Boolean = false
  override def endsWith(other: Path): Boolean = false
  override def startsWith(other: String): Boolean = false
  override def startsWith(other: Path): Boolean = false
  override def subpath(beginIndex: Int, endIndex: Int): Path = null
  override def getName(index: Int): Path = null
  override def getNameCount(): Int = 0
  override def getParent(): Path = null
  override def getFileName(): Path = null
  override def getRoot(): Path = if (absolute) filesystem.root else null
  override def isAbsolute(): Boolean = absolute
  override def getFileSystem(): MemoryFileSystem = filesystem
  
  override def register(watcher: WatchService, events: WatchEvent.Kind[_]*): WatchKey = throw new UnsupportedOperationException
  override def register(watcher: WatchService, events: Array[WatchEvent.Kind[_]], modifiers: WatchEvent.Modifier*) = throw new UnsupportedOperationException
  override def toFile(): java.io.File = throw new UnsupportedOperationException
}

class ResolvedPath(filesystem: MemoryFileSystem, path: String, val node: Node)
extends MemoryFSPath(filesystem, path, true)

