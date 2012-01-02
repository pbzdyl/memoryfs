package net.bzdyl.memoryfs

import java.nio.file.attribute.FileTime
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.UserPrincipal
import java.nio.file.attribute.GroupPrincipal

class MemoryFilesBasicFileAttributes extends PosixFileAttributes {
  override def lastModifiedTime(): FileTime = null
  override def lastAccessTime(): FileTime = null
  override def creationTime(): FileTime = null
  override def isRegularFile(): Boolean = false
  override def isDirectory(): Boolean = false;
  override def isSymbolicLink(): Boolean = false
  override def isOther(): Boolean = false
  override def size(): Long = -1
  override def fileKey(): AnyRef = null
  override def permissions(): java.util.Set[PosixFilePermission] = null
  override def group(): GroupPrincipal = null
  override def owner(): UserPrincipal = null
}
