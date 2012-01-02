package net.bzdyl.memoryfs

import java.nio.file.ProviderMismatchException
import java.nio.file.Path

object Conversions {
  implicit def pathToMemoryFSPath(path: Path): MFSPath = path match {
    case mfsPath: MFSPath => mfsPath
    case _ => throw new ProviderMismatchException
  }
}
