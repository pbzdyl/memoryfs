package net.bzdyl.memoryfs

import java.nio.file.Path
import java.nio.file.WatchService
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.LinkOption
import java.nio.file.FileSystem
import scala.collection.JavaConversions
import java.net.URI
import Conversions._
import scala.collection.mutable.ListBuffer

class MFSPath(val filesystem: MemoryFileSystem, stringPath: String*) extends Path {
  if (stringPath.size < 1) throw new IllegalArgumentException("At least one path element must be provided")

  val pathElements: Seq[String] = for {
    s <- stringPath
    el <- s.split("/")
    if !el.trim().isEmpty()
  } yield el

  val isAbsolute: Boolean = stringPath(0).startsWith("/")
  val isRelative: Boolean = !isAbsolute
  val isEmpty: Boolean = isRelative && pathElements.isEmpty

  def getName(pos: Int): MFSPath =
    if (pathElements.isDefinedAt(pos))
      new MFSPath(filesystem, pathElements(pos))
    else throw new IllegalArgumentException

  val getNameCount: Int = pathElements.size

  def iterator(): java.util.Iterator[Path] = {
    val pathIterator = pathElements.indices.view.map(i => getName(i)).iterator
    JavaConversions.asJavaIterator(pathIterator)
  }

  lazy val toUri: URI = new URI(filesystem.provider.getScheme + "://" + toAbsolutePath.toString)

  def getRoot(): MFSPath =
    if (isAbsolute)
      new MFSPath(filesystem, "/")
    else
      null

  def getFileName(): MFSPath =
    if (pathElements.nonEmpty)
      new MFSPath(filesystem, pathElements.last)
    else
      null

  def getParent(): MFSPath = {
    if (pathElements.nonEmpty) {
      val parentElements = pathElements.init
      if (parentElements.nonEmpty) {
        val parent = Seq(rootString) ++ parentElements
        new MFSPath(filesystem, parent: _*)
      } else if (pathElements.nonEmpty && isAbsolute) {
        getRoot
      } else {
        null
      }
    } else {
      null
    }
  }

  def toAbsolutePath(): MFSPath =
    if (isAbsolute) {
      this
    } else {
      filesystem.currentDir.resolve(this)
    }

  def resolve(name: String): MFSPath = resolve(new MFSPath(filesystem, name))

  def resolve(path: Path): MFSPath = {
    val other: MFSPath = path
    if (other.isAbsolute) {
      other
    } else if (other.isEmpty) {
      this
    } else {
      new MFSPath(filesystem, (Seq(rootString) ++ pathElements ++ other.pathElements): _*)
    }
  }

  def resolveSibling(name: String): MFSPath = resolveSibling(new MFSPath(filesystem, name))

  def resolveSibling(path: Path): MFSPath = {
    val other: MFSPath = path
    if (other.isAbsolute || getParent == null) {
      other
    } else {
      getParent.resolve(other)
    }
  }

  def endsWith(name: String): Boolean = endsWith(new MFSPath(filesystem, name))

  def endsWith(path: Path): Boolean = path match {
    case that: MFSPath if this.filesystem == that.filesystem =>
      if (this.isAbsolute) {
        if (that.isAbsolute) {
          this.equals(that)
        } else {
          pathElements.endsWith(that.pathElements)
        }
      } else {
        if (that.isAbsolute) {
          false
        } else {
          pathElements.endsWith(that.pathElements)
        }
      }
    case _ => false
  }

  def startsWith(name: String): Boolean = startsWith(new MFSPath(filesystem, name))

  def startsWith(path: Path): Boolean = path match {
    case that: MFSPath if this.filesystem == that.filesystem =>
      if (bothAbsoluteOrBothRelative(that)) {
        pathElements.startsWith(that.pathElements)
      } else {
        false
      }
    case _ => false
  }

  def compareTo(other: Path): Int = other match {
    case path: MFSPath =>
      other
      toString.compareTo(path.toString)
    case _ => throw new ClassCastException()
  }

  def subpath(start: Int, end: Int): MFSPath = {
    if (start < 0 || end > pathElements.size || end <= start) {
      throw new IllegalArgumentException("Invalid range: " + start + ", " + end)
    }

    new MFSPath(filesystem, pathElements.slice(start, end): _*)
  }

  def normalize(): MFSPath = {
    val buf = new ListBuffer[String]
    for (p <- pathElements) {
      if (".." == p) {
        if (isAbsolute) {
          if (buf.nonEmpty) buf.trimEnd(1)
        } else {
          if (buf.nonEmpty && buf.last != "..") {
            buf.trimEnd(1)
          } else {
            buf += p
          }
        }
      } else if ("." != p) {
        buf += p
      }
    }

    new MFSPath(filesystem, (Seq(rootString) ++ buf): _*)
  }

  def relativize(other: Path): MFSPath = {
    val that: MFSPath = other
    if (this.filesystem != that.filesystem) {
      throw new IllegalArgumentException("Cannot relativize relative paths from different filesystems: " + this + " and " + that)
    }
    
    if (this.isRelative || that.isRelative) {
      throw new IllegalArgumentException("Cannot relativize relative paths " + this + " and " + that)
    }

    if (this.pathElements.size == that.pathElements.size && this.pathElements.sameElements(that.pathElements)) {
      new MFSPath(filesystem, "")
    } else if (this.pathElements.size > that.pathElements.size && this.pathElements.startsWith(that.pathElements)) {
      val depthDifference = this.pathElements.size - that.pathElements.size
      new MFSPath(filesystem, Seq.fill(depthDifference)(".."): _*)
    } else if (this.pathElements.size < that.pathElements.size && that.pathElements.startsWith(this.pathElements)) {
      new MFSPath(filesystem, that.pathElements.drop(this.pathElements.size): _*)
    } else {
      val p: Seq[String] = Seq.fill(this.pathElements.size)("..") ++ that.pathElements
      new MFSPath(filesystem, p: _*)
    }
  }

  override lazy val hashCode: Int = pathElements.hashCode() + 13 * isAbsolute.hashCode() + 29 * filesystem.hashCode()

  override def equals(o: Any): Boolean = o match {
    case that: MFSPath =>
      this.filesystem == that.filesystem && this.isAbsolute == that.isAbsolute && this.pathElements == that.pathElements
    case _ => false
  }

  override def toString() = pathElements.mkString(rootString, "/", "")

  /* currently we don't support symlinks, so we just normalize the path */
  def toRealPath(options: LinkOption*): MFSPath = normalize()

  def getFileSystem(): FileSystem = filesystem

  /* currently unsupported */
  def register(service: WatchService, kinds: WatchEvent.Kind[_]*): WatchKey = throw new UnsupportedOperationException
  def register(service: WatchService, kinds: Array[WatchEvent.Kind[_]], mods: WatchEvent.Modifier*): WatchKey = throw new UnsupportedOperationException

  def toFile(): java.io.File = throw new UnsupportedOperationException

  private lazy val rootString = if (isAbsolute) "/" else ""
  private def bothAbsolute(that: MFSPath): Boolean = this.isAbsolute && that.isAbsolute
  private def bothRelative(that: MFSPath): Boolean = this.isRelative && that.isRelative
  private def bothAbsoluteOrBothRelative(that: MFSPath): Boolean = bothAbsolute(that) || bothRelative(that)
}

class ResolvedMFSPath(val node: Node, filesystem: MemoryFileSystem, stringPath: String*) extends MFSPath(filesystem, stringPath: _*)
