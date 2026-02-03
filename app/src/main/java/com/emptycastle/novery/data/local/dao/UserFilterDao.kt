package com.emptycastle.novery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emptycastle.novery.data.local.entity.BlockedAuthorEntity
import com.emptycastle.novery.data.local.entity.HiddenNovelEntity
import com.emptycastle.novery.data.local.entity.UserTagFilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserFilterDao {

    // ============ TAG FILTERS ============

    @Query("SELECT tag FROM user_tag_filters WHERE filterType = 'BLOCKED'")
    suspend fun getBlockedTags(): List<String>

    @Query("SELECT tag FROM user_tag_filters WHERE filterType = 'BOOSTED'")
    suspend fun getBoostedTags(): List<String>

    @Query("SELECT * FROM user_tag_filters")
    suspend fun getAllTagFilters(): List<UserTagFilterEntity>

    @Query("SELECT * FROM user_tag_filters")
    fun observeAllTagFilters(): Flow<List<UserTagFilterEntity>>

    @Query("SELECT * FROM user_tag_filters WHERE tag = :tag")
    suspend fun getTagFilter(tag: String): UserTagFilterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setTagFilter(filter: UserTagFilterEntity)

    @Query("DELETE FROM user_tag_filters WHERE tag = :tag")
    suspend fun removeTagFilter(tag: String)

    @Query("DELETE FROM user_tag_filters")
    suspend fun clearAllTagFilters()

    // ============ HIDDEN NOVELS ============

    @Query("SELECT novelUrl FROM hidden_novels")
    suspend fun getHiddenNovelUrls(): List<String>

    @Query("SELECT * FROM hidden_novels ORDER BY hiddenAt DESC")
    suspend fun getAllHiddenNovels(): List<HiddenNovelEntity>

    @Query("SELECT * FROM hidden_novels ORDER BY hiddenAt DESC")
    fun observeHiddenNovels(): Flow<List<HiddenNovelEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM hidden_novels WHERE novelUrl = :url)")
    suspend fun isNovelHidden(url: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideNovel(novel: HiddenNovelEntity)

    @Query("DELETE FROM hidden_novels WHERE novelUrl = :url")
    suspend fun unhideNovel(url: String)

    @Query("DELETE FROM hidden_novels")
    suspend fun clearAllHiddenNovels()

    // ============ BLOCKED AUTHORS ============

    @Query("SELECT authorNormalized FROM blocked_authors")
    suspend fun getBlockedAuthors(): List<String>

    @Query("SELECT * FROM blocked_authors ORDER BY blockedAt DESC")
    suspend fun getAllBlockedAuthors(): List<BlockedAuthorEntity>

    @Query("SELECT * FROM blocked_authors ORDER BY blockedAt DESC")
    fun observeBlockedAuthors(): Flow<List<BlockedAuthorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockAuthor(author: BlockedAuthorEntity)

    @Query("DELETE FROM blocked_authors WHERE authorNormalized = :author")
    suspend fun unblockAuthor(author: String)

    @Query("DELETE FROM blocked_authors")
    suspend fun clearAllBlockedAuthors()
}