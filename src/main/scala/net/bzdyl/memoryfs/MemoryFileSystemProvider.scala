package net.bzdyl.memoryfs

import java.nio.file.spi.FileSystemProvider
import java.nio.file._
import java.nio.file.attribute._
import java.nio.channels.SeekableByteChannel
import java.net.URI
import Conversions._

class MemoryFileSystemProvider extends FileSystemProvider {
	/* delegate data store specific operations to the filesystem instances */
	override def checkAccess(path: Path, modes: AccessMode*) {}
	
	override def copy(source: Path, target: Path, options: CopyOption*) {} 
	
	override def createDirectory(dir: Path, attrs: FileAttribute[_]*) {}
	
	override def delete(path: Path) = path.filesystem.delete(path)
	
	override def getFileAttributeView[A <: FileAttributeView](path: Path, t: Class[A], options: LinkOption*): A = {
	  Files.getFileAttributeView(path, t)
	}
	
	override def isHidden(path: Path) =
	  path.filesystem.isHidden(path)
	
	override def isSameFile(path: Path, path2: Path) =
	  path.filesystem.isSameFile(path, path2)
	
	override def move(source: Path, target: Path, options: CopyOption*) =
	  source.filesystem.move(source, target, options: _*)
	
	override def setAttribute(path: Path, attribute: String, value: Any, options: LinkOption*) =
	  path.filesystem.setAttribute(path, attribute, value, options: _*)
	
	override def readAttributes [A <: BasicFileAttributes] (path: Path, t: Class[A], options: LinkOption*): A = {
	  Files.readAttributes(path, t);
	}
	
	override def readAttributes(path: Path, attributes: String, options: LinkOption*): java.util.Map[String, AnyRef] =
	  path.filesystem.readAttributes(path, attributes, options: _*)
	
	override def newDirectoryStream(dir: Path, filter: DirectoryStream.Filter[_ >: Path]): DirectoryStream[Path] =
	  dir.filesystem.newDirectoryStream(dir, filter)
	
	override def newByteChannel(path: Path, options: java.util.Set[_ <: OpenOption], attrs: FileAttribute[_]*): SeekableByteChannel =
	  path.filesystem.newByteChannel(path, options, attrs: _*)

	override def newFileSystem(uri: URI, env: java.util.Map[String, _]): FileSystem = null
	override def getFileStore(path: Path): FileStore = null
	override def getFileSystem(uri: java.net.URI): FileSystem = null
	override def getPath(uri: java.net.URI): Path = null
	override val getScheme = "memory"
	
}
