package net.bzdyl.memoryfs
import java.nio.file.DirectoryStream
import java.nio.file.Path
import scala.collection.JavaConversions

class MemoryFSDirectoryStream(val entries: Set[Node]) extends DirectoryStream[Path] {
	override def iterator(): java.util.Iterator[Path] = {
	  val entriesIterator = entries.view.map(n => n.toPath()).iterator
	  JavaConversions.asJavaIterator(entriesIterator)
	}
	
	override def close() {}
}
