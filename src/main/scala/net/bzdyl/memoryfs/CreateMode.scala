package net.bzdyl.memoryfs

object CreateMode extends Enumeration {
  type CreateMode = Value
  val DontCreate, Create, CreateNew = Value
}
