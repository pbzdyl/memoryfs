package net.bzdyl.memoryfs
import scala.collection.mutable.StringBuilder

object TreePrinter {
  def stdoutPrintTree(start: Node) {
    println(printTree(start))
  }
  
  def printTree(start: Node): String = {
    val builder = new StringBuilder
    printTree(start, builder, 0)
  }
  
  def printTree(node: Node, builder: StringBuilder, level: Int): String = {
    drawLines(builder, level)
    
    node match {
      case dir: DirectoryLike =>
        builder.append(node.name)
        builder.append(" [Dir]")
        builder.append("\n")
        for (child <- dir.entries) printTree(child, builder, level + 1)
      case file: File =>
        builder.append(node.name)
        builder.append(" [File]")
        builder.append("\n")
    }
    
    builder.toString()
  }
  
  def drawLines(builder: StringBuilder, level: Int) {
    builder.append("  " * level)
  }
}