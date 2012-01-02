package net.bzdyl.memoryfs

object WriteMode extends Enumeration {
  type WriteMode = Value
  val DontWrite, Write, Append = Value
  def isModifying(mode: WriteMode) = mode match {
    case DontWrite => false
    case Write     => true
    case Append    => true
  }
}
