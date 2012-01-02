package net.bzdyl.memoryfs

import java.nio.file.attribute.UserPrincipalLookupService
import java.nio.file.attribute.GroupPrincipal
import java.nio.file.attribute.UserPrincipalNotFoundException
import java.nio.file.attribute.UserPrincipal

object MFSUserPrincipalLookupService extends UserPrincipalLookupService {
  override def lookupPrincipalByGroupName(group: String): GroupPrincipal = throw new UserPrincipalNotFoundException(group)
  override def lookupPrincipalByName(name: String): UserPrincipal = throw new UserPrincipalNotFoundException(name)
}
