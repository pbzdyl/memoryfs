package net.bzdyl.memoryfs
import java.nio.file.NoSuchFileException
import java.nio.file.FileAlreadyExistsException

class LocateNodeOpts[T <: Node](
      val whenPathDoesntExist: (String => Option[T]),
      val whenDifferentTypeExists: (Node => Option[T]))

class CreateOpts[T <: Node](
    val whenAlreadyExists: (T => T),
    whenPathDoesntExist: (String => Option[T]),
    whenDifferentTypeExists: (Node => Option[T]))
extends LocateNodeOpts[T](whenPathDoesntExist, whenDifferentTypeExists)

class SomeOrNone[T <: Node] extends LocateNodeOpts[T](
    whenPathDoesntExist = name => None,
    whenDifferentTypeExists = node => None)
    
class SomeOrException[T <: Node] extends LocateNodeOpts[T](
    whenPathDoesntExist = name => throw new NoSuchFileException(name),
    whenDifferentTypeExists = node => None)

object NodeOrNone extends SomeOrNone[Node]
object NodeOrException extends SomeOrException[Node]
object FileOrNone extends SomeOrNone[File]
object FileOrException extends SomeOrException[File]
object DirOrNone extends SomeOrNone[DirectoryLike]
object DirOrException extends SomeOrException[DirectoryLike]

object CreateNewDirOpts extends CreateOpts[DirectoryLike](
      whenAlreadyExists = dir => throw new FileAlreadyExistsException(dir.path),
      whenPathDoesntExist = name => throw new NoSuchFileException(name),
      whenDifferentTypeExists = node => throw new FileAlreadyExistsException(node.path))
  
object CreateOrExistingDirOpts extends CreateOpts[DirectoryLike](
      whenAlreadyExists = dir => dir,
      whenPathDoesntExist = name => throw new NoSuchFileException(name),
      whenDifferentTypeExists = node => throw new FileAlreadyExistsException(node.path))

object CreateOrExceptionWhenExistingDirOpts extends CreateOpts[DirectoryLike](
      whenAlreadyExists = dir => throw new FileAlreadyExistsException(dir.path),
      whenPathDoesntExist = name => throw new NoSuchFileException(name),
      whenDifferentTypeExists = node => throw new FileAlreadyExistsException(node.path))
  
object CreateNewFileOpts extends CreateOpts[File](
      whenAlreadyExists = file => throw new FileAlreadyExistsException(file.path),
      whenPathDoesntExist = name => throw new NoSuchFileException(name),
      whenDifferentTypeExists = node => throw new FileAlreadyExistsException(node.path))
  
object CreateOrExistingFileOpts extends CreateOpts[File](
      whenAlreadyExists = file => file,
      whenPathDoesntExist = name => throw new NoSuchFileException(name),
      whenDifferentTypeExists = node => throw new FileAlreadyExistsException(node.path))
