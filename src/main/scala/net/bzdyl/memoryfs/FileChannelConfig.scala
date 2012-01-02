package net.bzdyl.memoryfs

import java.nio.file.StandardOpenOption._
import CreateMode._
import WriteMode._
import java.nio.file.OpenOption

class FileChannelConfig(val createMode: CreateMode, val writeMode: WriteMode, val readMode: Boolean, val truncateExisting: Boolean) {
  val modifying = isModifying(writeMode)
}

object FileChannelConfig {
  def apply(options: java.util.Set[_ <: OpenOption]): FileChannelConfig = {
    //val deleteOnClose = options.contains(DELETE_ON_CLOSE) // ignored - all data is in memory only
    //val sparse = options.contains(SPARSE) // ignored - but may be a hint to use a compressed data structure for example
    //val sync = options.contains(SYNC) // ignored
    //val dsync = options.contains(DSYNC) // ignored

    val truncateExisting = options.contains(TRUNCATE_EXISTING)
    val createMode = options2CreateMode(options)
    val writeMode = options2WriteMode(options)

    val read = options.contains(READ)
    val readMode = if (isModifying(writeMode)) read else true

    val config = new FileChannelConfig(createMode, writeMode, readMode, truncateExisting)
    validate(config)

    config
  }
  
  private def validate(config: FileChannelConfig) {
    if (config.writeMode == Append && config.readMode) throw new IllegalArgumentException("APPEND & READ not allowed")
    if (config.writeMode == Append && config.truncateExisting) throw new IllegalArgumentException("APPEND & TRUNCATE_EXISTING not allowed")
  }

  private def options2CreateMode(options: java.util.Set[_ <: OpenOption]): CreateMode = {
    val createNew = options.contains(CREATE_NEW)
    val create = options.contains(CREATE)

    if (createNew) CreateNew else if (create) Create else DontCreate
  }

  private def options2WriteMode(options: java.util.Set[_ <: OpenOption]): WriteMode = {
    val append = options.contains(APPEND)
    val write = options.contains(WRITE)

    if (append) Append else if (write) Write else DontWrite
  }
}
