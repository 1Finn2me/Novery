package com.emptycastle.novery.domain.model

/**
 * Reading status for library entries.
 */
enum class ReadingStatus {
    READING,
    COMPLETED,
    ON_HOLD,
    PLAN_TO_READ,
    DROPPED;

    fun displayName(): String = when (this) {
        READING -> "Reading"
        COMPLETED -> "Completed"
        ON_HOLD -> "On Hold"
        PLAN_TO_READ -> "Plan to Read"
        DROPPED -> "Dropped"
    }

    companion object {
        fun fromString(value: String): ReadingStatus {
            return when (value.lowercase().replace(" ", "_")) {
                "reading" -> READING
                "completed" -> COMPLETED
                "on_hold", "onhold" -> ON_HOLD
                "plan_to_read", "plantoread" -> PLAN_TO_READ
                else -> READING

            }
        }
    }
}