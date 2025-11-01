package com.bitcleanerx.app.model

import kotlinx.serialization.Serializable

@Serializable
data class SimpleCleanerConfig(
    val windowsList: List<CleanerGroup> = emptyList(),
    val linuxList: List<CleanerGroup> = emptyList(),
    val macosList: List<CleanerGroup> = emptyList(),
    val iosList: List<CleanerGroup> = emptyList(),
    val androidList: List<CleanerGroup> = emptyList()
)

@Serializable
data class CleanerGroup(
    val groupName: String,
    val groupImage: String,
    val items: List<CleanerItem>
)

@Serializable
data class CleanerItem(
    val name: String,
    val type: ItemType,
    val path: String
)

@Serializable
enum class ItemType {
    file,
    folder,
    folders
}

data class ScannedItem(
    val name: String,
    val path: String,
    val size: Long,
    val type: ItemType,
    val subFolders: List<SubFolderItem> = emptyList()
)

data class SubFolderItem(
    val name: String,
    val path: String,
    val size: Long
)

data class FileTreeNode(
    val name: String,
    val path: String,
    var size: Long,
    val isDirectory: Boolean,
    val children: MutableList<FileTreeNode> = mutableListOf(),
    var isDeleted: Boolean = false
)

