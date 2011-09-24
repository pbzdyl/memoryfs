package net.bzdyl.memoryfs

import java.nio.file._
import java.nio.file.attribute._
import java.nio.file.spi._
import scala.collection.JavaConversions.{asJavaIterable, setAsJavaSet}
import java.net.URI
import java.nio.channels.SeekableByteChannel
import Conversions._

object MemoryFSUserPrincipalLookupService extends UserPrincipalLookupService {
	override def lookupPrincipalByGroupName(group: String): GroupPrincipal = throw new UserPrincipalNotFoundException(group)
	override def lookupPrincipalByName(name: String): UserPrincipal = throw new UserPrincipalNotFoundException(name)
}

class MemoryFileSystem(override val provider: MemoryFileSystemProvider, uri: URI, env: java.util.Map[String, _]) extends FileSystem {
	lazy val supportedFileAttributeViewsSet: Set[String] = Set.empty
	val root = new MemoryFSPath(this, "/")
	val roots = List(root)
	val rootNode = new Directory(this, None, "/")
	
	override def getPath(first: String, more: String*): Path = null
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
	
	def readAttributes [A <: BasicFileAttributes] (path: Path, t: Class[A], options: LinkOption*): A = {
	  Files.readAttributes(path, t);
	}
	
	def readAttributes(path: Path, attributes: String, options: LinkOption*): java.util.Map[String, AnyRef] = {
	  new java.util.HashMap()
	}
	
	def newDirectoryStream(dir: Path, filter: DirectoryStream.Filter[_ >: Path]): DirectoryStream[Path] =
	  locateNode(dir) match {
	  case Some(node: Directory) => new MemoryFSDirectoryStream(node)
	  case Some(_) => throw new NotDirectoryException(dir.toString())
	  case None => throw new NoSuchFileException(dir.toString())
	}
	
	def newByteChannel(path: Path, options: java.util.Set[_ <: OpenOption], attrs: FileAttribute[_]*): SeekableByteChannel = null
	
	def checkAccess(path: Path, modes: AccessMode*) {}
	
	def copy(source: Path, target: Path, options: CopyOption*) {} 
	
	def createDirectory(dir: Path, attrs: FileAttribute[_]*) {}
	
	def getFileAttributeView[A <: FileAttributeView](path: Path, t: Class[A], options: LinkOption*): A = {
	  Files.getFileAttributeView(path, t)
	}
	
	def locateNode(path: MemoryFSPath): Option[Node] = {
	  def locateNode(p: MemoryFSPath, n: Node): Option[Node] = {
	    None
	  }
	  
	  locateNode(path, rootNode)
	}
}
