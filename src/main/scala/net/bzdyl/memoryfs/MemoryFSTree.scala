package net.bzdyl.memoryfs

import scala.collection.mutable
import java.nio.file.Path

class FileData extends mutable.ArrayBuffer[Byte] with mutable.SynchronizedBuffer[Byte]
class DirectoryEntries extends mutable.HashSet[Node] with mutable.SynchronizedSet[Node]

sealed abstract class Node(val filesystem: MemoryFileSystem, val parent: Option[Directory], val name: String) {
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

class File(filesystem: MemoryFileSystem, parent: Option[Directory], name: String)
extends Node(filesystem, parent, name) {
  val data = new FileData
}

class Directory(filesystem: MemoryFileSystem, parent: Option[Directory], name: String)
extends Node(filesystem, parent, name) {
  val entries = new DirectoryEntries
}

class SymLink(filesystem: MemoryFileSystem, parent: Option[Directory], name: String, val target: Node)
extends Node(filesystem, parent, name)
