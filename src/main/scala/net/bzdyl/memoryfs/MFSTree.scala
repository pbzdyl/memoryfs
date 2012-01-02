package net.bzdyl.memoryfs

import scala.collection.mutable
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.Lock
import java.nio.ByteBuffer
import java.nio.file.NoSuchFileException
import java.nio.file.FileAlreadyExistsException
import scala.collection.mutable.ArrayBuffer
import java.nio.file.NotDirectoryException
import java.nio.file.AccessDeniedException
import java.nio.file.StandardCopyOption._
import scala.concurrent.stm._
import java.nio.file.CopyOption
import java.nio.file.DirectoryNotEmptyException

sealed trait Node {
  val filesystem: MemoryFileSystem
  def parentRef: Ref[DirectoryLike]
  def parent: DirectoryLike = parentRef.single()
  
  val isRoot: Boolean
  lazy val notRoot = !isRoot
  
  val name: String
  def toPath(): ResolvedMFSPath = {
    new ResolvedMFSPath(this, filesystem, path)
  }
  
  lazy val path: String = atomic { implicit txn =>
    var buf: List[String] = Nil
    var cur = this
    while (cur.notRoot) {
      buf = cur.name :: buf
      cur = cur.parent
    }
    
    buf.mkString("/", "/", "")
  }
  
  def locate[T <: Node](opts: LocateNodeOpts[T], path: String*)(implicit m: Manifest[T]): Option[T] = None
  def locate[T <: Node](opts: LocateNodeOpts[T], path: MFSPath)(implicit m: Manifest[T]): Option[T] = locate(opts, path.toAbsolutePath.pathElements: _*)
  def locateFile(path: String*): Option[File] = locate(FileOrNone, path: _*)
  def locateFile(path: MFSPath): Option[File] = locate(FileOrNone, path)
  def locateDir(path: String*): Option[DirectoryLike] = locate(DirOrNone, path: _*)
  def locateDir(path: MFSPath): Option[DirectoryLike] = locate(DirOrNone, path)
  def locateFileOrException(path: String*): File = locate(FileOrException, path: _*).get
  def locateFileOrException(path: MFSPath): File = locate(FileOrException, path).get
  def locateDirOrException(path: String*): DirectoryLike = locate(DirOrException, path: _*).get
  def locateDirOrException(path: MFSPath): DirectoryLike = locate(DirOrException, path).get
  
  def delete(): Unit = atomic { implicit txn =>
    parent.delete(this)
  }
  
  def copyTo(targetNode: DirectoryLike, newName: String): Unit
  
  def copyOrMove(targetPath: MFSPath, move: Boolean, options: CopyOption*): Unit = atomic { implicit txn =>
    val rootNode = targetPath.filesystem.rootNode
    val targetNode = rootNode.locate(NodeOrNone, targetPath)
    
    targetNode match {
      // copy/move to itself - noop
      case Some(existingNode) if existingNode eq this =>
        return
      
      // target already exists
      case Some(existingNode) =>
        if (options.contains(REPLACE_EXISTING)) {
          val existingNodeParent = existingNode.parent
          existingNode.delete()
          copyTo(existingNodeParent, existingNode.name)
          if (move) {
            parent.delete(this)
          }
        } else {
          throw new FileAlreadyExistsException(targetPath.toString())
        }
      
      // target doesn't exist
      case None =>
        val targetParent = rootNode.locate(NodeOrException, targetPath.getParent()).get
        targetParent match {
          // parent is a dir - just copy/move to it
          case existingDir: DirectoryLike =>
            copyTo(existingDir, targetPath.getFileName().toString())
            if (move) {
              parent.delete(this)
            }
      
          // parent is not a dir - exception
          case otherNode =>
            throw new FileAlreadyExistsException(targetParent.toString())
        }
    }
  }
  
  def copyTo(targetPath: MFSPath, options: CopyOption*): Unit = copyOrMove(targetPath, false, options: _*)
  def moveTo(targetPath: MFSPath, options: CopyOption*): Unit = copyOrMove(targetPath, true, options: _*)
  
  override def hashCode(): Int = name.hashCode()
  override def equals(other: Any): Boolean = other match {
    case that: Node => this.name == that.name
    case _ => false
  }
}

class File(val filesystem: MemoryFileSystem, val parentRef: Ref[DirectoryLike], val name: String, val bytes: FileData) extends Node {
  def this(filesystem: MemoryFileSystem, parent: DirectoryLike, name: String) = this(filesystem, Ref(parent), name, new FileData)
  def this(filesystem: MemoryFileSystem, parent: DirectoryLike, name: String, bytes: FileData) = this(filesystem, Ref(parent), name, bytes)
  
  val isRoot = false
  
  def newByteChannel(config: FileChannelConfig): FileDataSeekableByteChannel = new FileDataSeekableByteChannel(bytes, config.writeMode, config.readMode)
  
  override def copyTo(targetNode: DirectoryLike, newName: String): Unit = atomic { implicit txn =>
    val copy = new File(filesystem, targetNode, newName, bytes)
    targetNode.add(copy)
  }
  
  override def toString(): String = "File [" + path + "], size=" + bytes.size
}

trait DirectoryLike extends Node {
  val entriesRef = Ref(Set.empty[Node])
  def entries: Set[Node] = entriesRef.single()
  def isEmpty = entries.isEmpty
  def nonEmpty = entries.nonEmpty
  
  override def locate[T <: Node](opts: LocateNodeOpts[T], path: String*)(implicit m: Manifest[T]): Option[T] = atomic { implicit txn =>
    path.toList match {
      case Nil => None
      case n :: Nil => entries.find(_.name == n) match {
        case None => opts.whenPathDoesntExist(path.mkString("/"))
        case Some(n) =>
          if (m.erasure.isAssignableFrom(n.getClass())) {
            Some(n.asInstanceOf[T])
          } else {
            opts.whenDifferentTypeExists(n)
          }
      }
      case n :: rest => entries.find(_.name == n) match {
        case None => opts.whenPathDoesntExist(path.mkString("/"))
        case Some(node) => node.locate(opts, rest: _*)
      }
    }
  }
  
  def createNewDirectory(path: String*): DirectoryLike = createDirectory(CreateNewDirOpts, path: _*)
  def createNewDirectory(path: MFSPath): DirectoryLike = createDirectory(CreateNewDirOpts, path.toAbsolutePath.pathElements: _*)
  def createDirectory(path: String*): DirectoryLike = createDirectory(CreateOrExistingDirOpts, path: _*)
  def createDirectory(path: MFSPath): DirectoryLike = createDirectory(CreateOrExistingDirOpts, path.toAbsolutePath.pathElements: _*)
  
  def createDirectory(opts: CreateOpts[DirectoryLike], path: String*): DirectoryLike = atomic { implicit txn =>
    path.toList match {
	  case Nil => throw new IllegalArgumentException
	  case cur :: Nil => createDirectory(opts, cur)
	  case _ => locate(opts, path.init: _*) match {
	    case Some(dir: DirectoryLike) => dir.createDirectory(opts, path.last)
	    case None => opts.whenPathDoesntExist(path.mkString("/")).get
	  }
	}
  }
  
  def createDirectory(opts: CreateOpts[DirectoryLike], dirName: String): DirectoryLike = atomic { implicit txn =>
    entries.find(_.name == dirName) match {
      case Some(existingDir: DirectoryLike) => opts.whenAlreadyExists(existingDir)
      case Some(notDir) => opts.whenDifferentTypeExists(notDir).get
      case None =>
        val newDir = new Directory(filesystem, Ref(this), dirName)
        atomic { implicit txn =>
          entriesRef() = entries + newDir
        }
        newDir
    }
  }
  
  def createNewFile(path: String*): File= createFile(CreateNewFileOpts, path: _*)
  def createNewFile(path: MFSPath): File= createFile(CreateNewFileOpts, path.toAbsolutePath.pathElements: _*)
  def createFile(path: String*): File= createFile(CreateOrExistingFileOpts, path: _*)
  def createFile(path: MFSPath): File= createFile(CreateOrExistingFileOpts, path.toAbsolutePath.pathElements: _*)
  
  def createFile(opts: CreateOpts[File], path: String*): File = atomic { implicit txn =>
    path.toList match {
      case Nil => throw new IllegalArgumentException
      case cur :: Nil => createFile(opts, cur)
      case _ => locateDir(path.init: _*) match {
        case Some(dir: Directory) => dir.createFile(opts, path.last)
        case None => opts.whenPathDoesntExist(path.mkString("/")).get
      }
    }
  }
  
  def createFile(opts: CreateOpts[File], fileName: String): File = atomic { implicit txn =>
    entries.find(_.name == fileName) match {
      case Some(existingFile: File) => opts.whenAlreadyExists(existingFile)
      case Some(notFile) => opts.whenDifferentTypeExists(notFile).get
      case None =>
        val newFile = new File(filesystem, this, fileName)
        atomic { implicit txn =>
          entriesRef() = entries + newFile
        }
        newFile
    }
  }
  
  def newDirectoryStream() = {
    new MemoryFSDirectoryStream(entries)
  }
  
  override def copyTo(targetNode: DirectoryLike, newName: String): Unit = atomic { implicit txn =>
    val copy = new Directory(filesystem, targetNode, newName)
    val children = entries
    for (ch <- children) {
      copy.add(ch)
    }
    targetNode.add(copy)
  }
  
  def add(child: Node): Unit = atomic { implicit txn =>
    if (entries.contains(child)) {
      throw new FileAlreadyExistsException(child.path)
    }
    entriesRef() = entries + child
    child.parentRef() = this
  }
  
  def delete(child: Node): Unit = atomic { implicit txn =>
    entriesRef() = entries - child
  }
  
  def replace(existing: Node, replacement: Node) = atomic { implicit txn =>
    entriesRef() = entries - existing + replacement
    replacement.parentRef() = this
  }
  
  override def delete(): Unit = atomic { implicit txn =>
    if (nonEmpty) {
      throw new DirectoryNotEmptyException(path)
    } else {
      super.delete()
    }
  }
}

class Directory(val filesystem: MemoryFileSystem, val parentRef: Ref[DirectoryLike], val name: String) extends DirectoryLike {
  def this(filesystem: MemoryFileSystem, parent: DirectoryLike, name: String) = this(filesystem, Ref(parent), name)
  
  val isRoot = false
  
  override def toString(): String = "Directory [" + path + "], entries=(" + entries.map(_.name).mkString(",") + ")"  
}

class Root(val filesystem: MemoryFileSystem, val name:String) extends DirectoryLike {
  def parentRef = throw new NoSuchFileException("parent of root (/)")
  val isRoot = true
  
  override def delete() {
    throw new AccessDeniedException(toString)
  }
  
  override def locate[T <: Node](opts: LocateNodeOpts[T], path: MFSPath)(implicit m: Manifest[T]): Option[T] = {
    if (filesystem.rootPath == path) {
      if (m.erasure.isAssignableFrom(this.getClass())) {
          Some(this.asInstanceOf[T])
        } else {
          opts.whenDifferentTypeExists(this)
        }
    } else {
    	locate(opts, path.toAbsolutePath.pathElements: _*)
    }
  }

  override def toString(): String = "Root [" + path + "], entries=(" + entries.map(_.name).mkString(",") + ")"
}
