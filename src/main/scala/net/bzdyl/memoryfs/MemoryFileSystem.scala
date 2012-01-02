package net.bzdyl.memoryfs

import java.nio.file._
import java.nio.file.StandardCopyOption._
import java.nio.file.attribute._
import scala.collection.mutable
import scala.collection.JavaConversions.{ asJavaIterable, setAsJavaSet, asScalaSet }
import scala.concurrent.stm._
import java.net.URI
import java.nio.channels.SeekableByteChannel
import Conversions._
import java.io.FileNotFoundException
import WriteMode._
import CreateMode._

class MemoryFileSystem(override val provider: MemoryFileSystemProvider, uri: URI, env: java.util.Map[String, _]) extends FileSystem {
  lazy val supportedFileAttributeViewsSet: Set[String] = Set.empty
  val rootPath = new MFSPath(this, "/")
  val rootPaths = List(rootPath)
  @volatile var currentDir = rootPath
  
  val rootNode = new Root(this, "/")

  override def getPath(first: String, more: String*): MFSPath = new MFSPath(this, (Seq(first) ++ more): _*)
  override def isReadOnly(): Boolean = false
  override def isOpen(): Boolean = true

  override lazy val supportedFileAttributeViews: java.util.Set[String] = java.util.Collections.emptySet[String]()
  override def getRootDirectories(): java.lang.Iterable[Path] = asJavaIterable(rootPaths)
  override def getFileStores(): java.lang.Iterable[FileStore] = asJavaIterable(Nil)
  override def getUserPrincipalLookupService(): UserPrincipalLookupService = MFSUserPrincipalLookupService
  override def getSeparator(): String = "/"

  override def close() {}

  override def getPathMatcher(syntaxAndPattern: String): PathMatcher = throw new UnsupportedOperationException
  override def newWatchService(): WatchService = throw new UnsupportedOperationException

  def delete(path: Path) = atomic { implicit txn =>
    rootNode.locate(NodeOrException, path).get.delete
  }

  def isSameFile(path1: Path, path2: Path): Boolean = path1.normalize().equals(path2.normalize())

  def move(source: Path, target: Path, options: CopyOption*): Unit = copyOrMove(source, target, true, options: _*)
  
  def copy(source: Path, target: Path, options: CopyOption*): Unit = copyOrMove(source, target, false, options: _*)
  
  def copyOrMove(source: MFSPath, target: MFSPath, move: Boolean, options: CopyOption*):Unit = atomic { implicit txn =>
    val sourceNode = rootNode.locate(NodeOrException, source).get
    
    sourceNode.copyOrMove(target, move, options: _*)
  }

  def setAttribute(path: Path, attribute: String, value: Any, options: LinkOption*) = atomic { implicit txn =>
    // todo
  }

  def readAttributes[A <: BasicFileAttributes](path: Path, t: Class[A], options: LinkOption*): A = atomic { implicit txn =>
    // todo
    Files.readAttributes(path, t);
  }

  def readAttributes(path: Path, attributes: String, options: LinkOption*): java.util.Map[String, AnyRef] = atomic { implicit txn =>
    // todo
    new java.util.HashMap()
  }

  def newDirectoryStream(dir: Path, filter: DirectoryStream.Filter[_ >: Path]): DirectoryStream[Path] = atomic { implicit txn =>
    rootNode.locateDirOrException(dir).newDirectoryStream()
  }

  def newByteChannel(path: Path, options: java.util.Set[_ <: OpenOption], attrs: FileAttribute[_]*): SeekableByteChannel = atomic { implicit txn =>
    val config = FileChannelConfig(options)
    if (isModifying(config.writeMode) && config.createMode == CreateNew) {
      rootNode.createNewFile(path).newByteChannel(config)
    } else {
      rootNode.createFile(path).newByteChannel(config)
    }
  }
  
  def checkAccess(path: Path, modes: AccessMode*) = atomic { implicit txn =>
    // todo
  }

  def createDirectory(dir: Path, attrs: FileAttribute[_]*): Unit = atomic { implicit txn =>
    val newDir: MFSPath = if (dir.isAbsolute()) dir else dir.toAbsolutePath()
    val parent = newDir.getParent()
    rootNode.locate(NodeOrException, parent).get match {
      case dirNode: Directory => dirNode.createDirectory(newDir.getFileName().toString())
      case fileNode => throw new NotDirectoryException(parent.toString())
    }
  }

  def getFileAttributeView[A <: FileAttributeView](path: Path, t: Class[A], options: LinkOption*): A = atomic { implicit txn =>
    // todo
    Files.getFileAttributeView(path, t)
  }
}
