package net.bzdyl.memoryfs

import java.nio.file._
import java.nio.file.StandardOpenOption._
import java.nio.file.attribute._
import java.nio.file.spi._
import scala.collection.mutable
import scala.collection.JavaConversions.{ asJavaIterable, setAsJavaSet, asScalaSet }
import java.net.URI
import java.nio.channels.SeekableByteChannel
import Conversions._
import java.io.FileNotFoundException
import WriteMode._

object MemoryFSUserPrincipalLookupService extends UserPrincipalLookupService {
  override def lookupPrincipalByGroupName(group: String): GroupPrincipal = throw new UserPrincipalNotFoundException(group)
  override def lookupPrincipalByName(name: String): UserPrincipal = throw new UserPrincipalNotFoundException(name)
}

object CreateMode extends Enumeration {
  type CreateMode = Value
  val DontCreate, Create, CreateNew = Value
}

import CreateMode._

class ChannelConfig(val createMode: CreateMode, val writeMode: WriteMode, val readMode: Boolean, val truncateExisting: Boolean) {
  val modifying = isModifying(writeMode)
}

object ChannelConfig {
  def apply(options: java.util.Set[_ <: OpenOption]): ChannelConfig = {
    //val deleteOnClose = options.contains(DELETE_ON_CLOSE) // ignored - all data is in memory only
    //val sparse = options.contains(SPARSE) // ignored - but may be a hint to use a compressed data structure for example
    //val sync = options.contains(SYNC) // ignored
    //val dsync = options.contains(DSYNC) // ignored

    val truncateExisting = options.contains(TRUNCATE_EXISTING)
    val createMode = options2CreateMode(options)
    val writeMode = options2WriteMode(options)

    val read = options.contains(READ)
    val readMode = if (isModifying(writeMode)) read else true

    val config = new ChannelConfig(createMode, writeMode, readMode, truncateExisting)
    validate(config)

    config
  }
  
  private def validate(config: ChannelConfig) {
    if (config.writeMode == Append && config.readMode) throw new IllegalArgumentException("APPEND & READ not allowed")
    if (config.writeMode == Append && config.truncateExisting) throw new IllegalArgumentException("APPEND & TRUNCATE_EXISTING not allowed")
  }

  private def options2CreateMode(options: java.util.Set[_ <: OpenOption]): CreateMode = {
    val createNew = options.contains(CREATE_NEW)
    val create = options.contains(CREATE)

    if (createNew) CreateNew else if (create) Create else DontCreate
  }

  private def options2WriteMode(options: java.util.Set[_ <: OpenOption]): WriteMode = {
    val append = options.contains(APPEND)
    val write = options.contains(WRITE)

    if (append) Append else if (write) Write else DontWrite
  }
}

class MemoryFileSystem(override val provider: MemoryFileSystemProvider, uri: URI, env: java.util.Map[String, _]) extends FileSystem {
  lazy val supportedFileAttributeViewsSet: Set[String] = Set.empty
  val root = new MFSPath(this, "/")
  val roots = List(root)
  @volatile var currentDir = root
  
  val rootNode = new Directory(this, None, "/")

  override def getPath(first: String, more: String*): Path = new MFSPath(this, (Seq(first) ++ more): _*)
  override def isReadOnly(): Boolean = false
  override def isOpen(): Boolean = true

  override lazy val supportedFileAttributeViews: java.util.Set[String] = java.util.Collections.emptySet[String]()
  override def getRootDirectories(): java.lang.Iterable[Path] = asJavaIterable(roots)
  override def getFileStores(): java.lang.Iterable[FileStore] = asJavaIterable(Nil)
  override def getUserPrincipalLookupService(): UserPrincipalLookupService = MemoryFSUserPrincipalLookupService
  override def getSeparator(): String = "/"

  override def close() = rootNode.clear()

  override def getPathMatcher(syntaxAndPattern: String): PathMatcher = throw new UnsupportedOperationException
  override def newWatchService(): WatchService = throw new UnsupportedOperationException

  def delete(path: Path) {

  }

  def isHidden(path: Path): Boolean = false

  def isSameFile(path1: Path, path2: Path): Boolean = false

  def move(source: Path, target: Path, options: CopyOption*) {}

  def setAttribute(path: Path, attribute: String, value: Any, options: LinkOption*) {}

  def readAttributes[A <: BasicFileAttributes](path: Path, t: Class[A], options: LinkOption*): A = {
    Files.readAttributes(path, t);
  }

  def readAttributes(path: Path, attributes: String, options: LinkOption*): java.util.Map[String, AnyRef] = {
    new java.util.HashMap()
  }

  def newDirectoryStream(dir: Path, filter: DirectoryStream.Filter[_ >: Path]): DirectoryStream[Path] =
    locateNode(dir) match {
      case Some(node: Directory) => new MemoryFSDirectoryStream(node)
      case Some(_)               => throw new NotDirectoryException(dir.toString())
      case None                  => throw new NoSuchFileException(dir.toString())
    }

  def newByteChannel(path: Path, options: java.util.Set[_ <: OpenOption], attrs: FileAttribute[_]*): SeekableByteChannel = {
    val config = ChannelConfig(options)
    val fileNode = getFileData(path, config)
    new FileDataSeekableByteChannel(fileNode, config.writeMode, config.readMode)
  }
  
  private def getFileData(path: Path, config: ChannelConfig): FileData = {
    if (isModifying(config.writeMode) && config.createMode == CreateNew) {
      createNewFileData(path, config)
    } else {
      createOrExistingFile(path, config)
    }
  }
  
  private def createOrExistingFile(path: Path, config: ChannelConfig): FileData = {
    val parentPath = path.getParent()
    val dirNode = locateNode(parentPath)
    
    dirNode match {
      case Some(dir: Directory) =>
        dir.createOrExisting(path.getFileName().toString()) match {
          case file: File => file
          case _ => throw new FileAlreadyExistsException(path.toString())
        }
      case Some(_) => throw new FileAlreadyExistsException(path.toString())
      case None => throw new NoSuchFileException(path.toString())
    }
  }

  private def createNewFileData(path: MFSPath, config: ChannelConfig): FileData = {
    locateNode(path.getParent()) match {
      case Some(dir: Directory) => dir.createNewFile(path.getFileName().toString) match {
        case Some(newFile: File) => newFile
        case None                => throw new FileAlreadyExistsException(path.toString)
      }
      case Some(node) => throw new NotDirectoryException(node.toString())
      case None       => throw new NoSuchFileException(path.toString)
    }
  }

  def checkAccess(path: Path, modes: AccessMode*) {}

  def copy(source: Path, target: Path, options: CopyOption*) {}

  def createDirectory(dir: Path, attrs: FileAttribute[_]*) {}

  def getFileAttributeView[A <: FileAttributeView](path: Path, t: Class[A], options: LinkOption*): A = {
    Files.getFileAttributeView(path, t)
  }

  def locateNode(path: MFSPath): Option[Node] = {
    def locateNode(p: MFSPath, n: Node): Option[Node] = {
      None
    }

    locateNode(path, rootNode)
  }
  
}
