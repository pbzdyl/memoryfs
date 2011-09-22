package net.bzdyl.memoryfs
import java.nio.file.ProviderMismatchException
import java.nio.file.Path

object Conversions {
  implicit def pathToMemoryFSPath(path: Path): MemoryFSPath = path match {
    case memoryFSPath: MemoryFSPath => memoryFSPath
    case _ => throw new ProviderMismatchException
  }
}
