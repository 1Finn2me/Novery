// com/emptycastle/novery/recommendation/SynopsisTagExtractor.kt
package com.emptycastle.novery.recommendation

import com.emptycastle.novery.recommendation.TagNormalizer.TagCategory

/**
 * Extracts probable tags from novel synopsis/description.
 * Used when providers don't supply tags or to enhance sparse tag data.
 */
object SynopsisTagExtractor {

    /**
     * Keyword patterns for each tag category.
     * Each entry is: List of keywords/phrases -> TagCategory
     * Uses word boundary matching where appropriate.
     */
    private val extractionPatterns: List<Pair<List<String>, TagCategory>> = listOf(
        // ============ CULTIVATION/EASTERN ============
        listOf(
            "cultivation", "cultivator", "cultivating", "cultivate",
            "qi ", " qi", "qi,", "qi.", "spiritual qi", "spiritual energy",
            "dantian", "dan tian", "golden core", "nascent soul",
            "breakthrough", "break through", "realm", "realms",
            "foundation establishment", "core formation",
            "tribulation", "heavenly tribulation", "lightning tribulation",
            "immortal", "immortality", "ascend", "ascension",
            "sect", "sects", "elder", "inner disciple", "outer disciple",
            "martial brother", "martial sister", "senior brother", "junior brother"
        ) to TagCategory.CULTIVATION,

        listOf(
            "wuxia", "jianghu", "pugilist"
        ) to TagCategory.WUXIA,

        listOf(
            "xianxia", "daoist", "taoism", "taoist",
            "heavenly dao", "mandate of heaven"
        ) to TagCategory.XIANXIA,

        listOf(
            "murim", "martial world", "martial arts world"
        ) to TagCategory.MURIM,

        listOf(
            "martial arts", "martial artist", "kung fu", "kungfu",
            "fighting style", "combat technique", "fist technique"
        ) to TagCategory.MARTIAL_ARTS,

        // ============ ISEKAI/REINCARNATION ============
        listOf(
            "transported to another world", "summoned to another world",
            "woke up in another world", "found myself in another world",
            "isekai", "otherworld", "other world",
            "transferred to", "teleported to",
            "hit by a truck", "truck-kun", "died and woke up"
        ) to TagCategory.ISEKAI,

        listOf(
            "reincarnated", "reincarnation", "rebirth", "reborn",
            "past life", "previous life", "second life",
            "died and was reborn", "memories of my past",
            "born again", "new life"
        ) to TagCategory.REINCARNATION,

        listOf(
            "transmigrated", "transmigration", "soul transmigration",
            "possessed", "took over the body"
        ) to TagCategory.TRANSMIGRATION,

        listOf(
            "regressor", "regression", "regressed", "went back in time",
            "returned to the past", "second chance at life",
            "before my death"
        ) to TagCategory.REGRESSION,

        listOf(
            "time loop", "stuck in a loop", "same day over",
            "repeated the day", "looping", "groundhog"
        ) to TagCategory.TIME_LOOP,

        listOf(
            "summoned hero", "summoned as a hero", "hero summoning",
            "otherworlder", "summoned champion"
        ) to TagCategory.SUMMONED_HERO,

        // ============ LITRPG/GAMELIT/SYSTEM ============
        listOf(
            "level up", "leveled up", "leveling up",
            "experience points", "exp", "xp gained",
            "skill tree", "skill points", "stat points",
            "class selection", "job class", "chose a class"
        ) to TagCategory.LITRPG,

        listOf(
            "[system", "system notification", "system alert",
            "status window", "status screen", "blue screen",
            "ding!", "notification popped up", "quest received",
            "skill acquired", "achievement unlocked"
        ) to TagCategory.SYSTEM,

        listOf(
            "game-like", "rpg", "mmorpg", "vrmmorpg",
            "full-dive", "logged into", "player",
            "npc", "npcs", "respawn"
        ) to TagCategory.GAMELIT,

        listOf(
            "virtual reality", "vr game", "dive into",
            "capsule", "neurolink", "full immersion"
        ) to TagCategory.VIRTUAL_REALITY,

        listOf(
            "dungeon", "dungeons", "dungeon core", "dungeon master",
            "floor boss", "dungeon boss", "monster room",
            "dive into the dungeon", "cleared the dungeon"
        ) to TagCategory.DUNGEON,

        listOf(
            "tower", "climbing the tower", "tower climber",
            "floor ", "cleared floor", "tower of"
        ) to TagCategory.TOWER,

        listOf(
            "grow stronger", "getting stronger", "become stronger",
            "power up", "powered up", "training arc",
            "improved his strength", "strength grew"
        ) to TagCategory.PROGRESSION,

        // ============ ROMANCE TYPES ============
        listOf(
            "slow burn", "slow-burn", "slowly developed feelings",
            "feelings grew over time", "gradual romance"
        ) to TagCategory.SLOW_BURN,

        listOf(
            "enemies to lovers", "former enemies", "hated each other",
            "rivalry turned to love", "once enemies"
        ) to TagCategory.ENEMIES_TO_LOVERS,

        listOf(
            "childhood friends", "friends to lovers", "best friend",
            "grew up together", "known each other for years"
        ) to TagCategory.FRIENDS_TO_LOVERS,

        listOf(
            "forbidden love", "forbidden romance", "shouldn't be together",
            "taboo relationship", "society forbids"
        ) to TagCategory.FORBIDDEN_LOVE,

        listOf(
            "harem", "multiple wives", "many women", "surrounded by beauties",
            "collected beauties", "polygamy", "concubines"
        ) to TagCategory.HAREM,

        listOf(
            "reverse harem", "surrounded by handsome men",
            "many suitors", "multiple love interests"
        ) to TagCategory.REVERSE_HAREM,

        listOf(
            "arranged marriage", "contract marriage", "marriage of convenience",
            "political marriage", "forced marriage", "engaged to"
        ) to TagCategory.ARRANGED_MARRIAGE,

        // ============ LGBTQ+ ============
        listOf(
            "boys love", "bl", "yaoi", "shounen ai", "shonen ai",
            "male love", "m/m", "gay romance", "danmei",
            "fell for him", "loved another man", "male lover"
        ) to TagCategory.BL,

        listOf(
            "girls love", "gl", "yuri", "shoujo ai", "shojo ai",
            "female love", "f/f", "lesbian", "baihe",
            "fell for her", "loved another woman", "female lover"
        ) to TagCategory.GL,

        listOf(
            "lgbt", "lgbtq", "queer", "non-binary", "nonbinary",
            "transgender", "bisexual", "pansexual"
        ) to TagCategory.LGBT,

        listOf(
            "gender bender", "gender swap", "genderbend",
            "became a woman", "became a man", "changed gender",
            "woke up as a girl", "woke up as a boy", "body swap"
        ) to TagCategory.GENDER_BENDER,

        // ============ LEAD TYPES ============
        listOf(
            "overpowered", "op mc", "strongest", "unbeatable",
            "invincible", "no one could match", "godlike power",
            "strongest being", "most powerful"
        ) to TagCategory.OP_MC,

        listOf(
            "weak to strong", "started weak", "from zero",
            "weakling", "lowest rank", "trash of the family",
            "crippled", "talentless", "no talent"
        ) to TagCategory.WEAK_TO_STRONG,

        listOf(
            "anti-hero", "antihero", "not a hero",
            "morally ambiguous", "grey morality", "gray morality"
        ) to TagCategory.ANTI_HERO,

        listOf(
            "villain", "villain protagonist", "evil mc",
            "became a villain", "dark lord", "demon king"
        ) to TagCategory.VILLAIN_PROTAGONIST,

        listOf(
            "ruthless", "merciless", "cold-blooded", "cold blooded",
            "no mercy", "kill without hesitation", "kills easily"
        ) to TagCategory.RUTHLESS_MC,

        listOf(
            "genius", "prodigy", "intelligent mc", "smart mc",
            "brilliant mind", "strategic genius", "mastermind"
        ) to TagCategory.SMART_MC,

        listOf(
            "underdog", "looked down upon", "despised", "mocked",
            "proved them wrong", "showed them all"
        ) to TagCategory.UNDERDOG,

        // ============ FANTASY CREATURES ============
        listOf(
            "dragon", "dragons", "dragonkin", "dragon rider",
            "wyvern", "wyrm"
        ) to TagCategory.DRAGONS,

        listOf(
            "vampire", "vampires", "vampiric", "blood-sucker",
            "nosferatu", "immortal blood"
        ) to TagCategory.VAMPIRES,

        listOf(
            "werewolf", "werewolves", "lycanthrope", "wolf shifter",
            "shapeshifter", "beast form", "wolf pack", "alpha wolf"
        ) to TagCategory.WEREWOLVES,

        listOf(
            "zombie", "zombies", "undead horde", "living dead",
            "zombie apocalypse", "infected"
        ) to TagCategory.ZOMBIES,

        listOf(
            "elf", "elves", "elven", "high elf", "dark elf",
            "half-elf", "elvish"
        ) to TagCategory.ELVES,

        listOf(
            "demon", "demons", "demonic", "devil", "demon lord",
            "demonkin", "hell", "hellish"
        ) to TagCategory.DEMONS,

        listOf(
            "god", "gods", "deity", "divine", "godhood",
            "pantheon", "olympus", "divine power"
        ) to TagCategory.GODS,

        // ============ THEMES ============
        listOf(
            "revenge", "vengeance", "avenge", "get revenge",
            "pay them back", "make them pay", "retribution"
        ) to TagCategory.REVENGE,

        listOf(
            "betrayal", "betrayed", "backstabbed", "sold out",
            "trusted and betrayed", "treachery"
        ) to TagCategory.BETRAYAL,

        listOf(
            "kingdom building", "build a kingdom", "nation building",
            "empire building", "build an empire", "territory management",
            "domain", "ruling", "governance"
        ) to TagCategory.KINGDOM_BUILDING,

        listOf(
            "survival", "survive", "surviving", "fight to survive",
            "survival of", "wilderness", "stranded"
        ) to TagCategory.SURVIVAL,

        listOf(
            "apocalypse", "apocalyptic", "end of the world",
            "world ended", "humanity fell", "collapse of civilization"
        ) to TagCategory.APOCALYPSE,

        listOf(
            "war", "warfare", "battle", "battlefield", "military campaign",
            "army", "armies", "soldiers", "troops"
        ) to TagCategory.WAR,

        listOf(
            "politics", "political", "court intrigue", "noble houses",
            "power struggle", "throne", "succession"
        ) to TagCategory.POLITICS,

        listOf(
            "strategy", "strategic", "tactics", "tactical",
            "outmaneuvered", "outsmarted", "planned ahead",
            "chess match", "moved like chess", "calculated move",
            "step ahead", "ten steps ahead", "predicted"
        ) to TagCategory.STRATEGY,

        listOf(
            "tournament", "competition", "championship", "arena",
            "gladiator", "fighting tournament", "martial tournament"
        ) to TagCategory.TOURNAMENT,

        listOf(
            "found family", "makeshift family", "became like family",
            "adopted", "orphan", "found a home"
        ) to TagCategory.FOUND_FAMILY,

        // ============ ACTIVITIES ============
        listOf(
            "cooking", "cook", "chef", "culinary", "kitchen",
            "recipe", "ingredients", "delicious food"
        ) to TagCategory.COOKING,

        listOf(
            "alchemy", "alchemist", "potion", "potions", "elixir",
            "brewing", "transmutation", "philosopher's stone"
        ) to TagCategory.ALCHEMY,

        listOf(
            "crafting", "blacksmith", "forge", "forging", "smith",
            "weapon crafting", "armor crafting", "artisan"
        ) to TagCategory.CRAFTING,

        listOf(
            "taming", "tamed", "monster taming", "beast taming",
            "pet", "familiar", "summoned beast", "companion beast"
        ) to TagCategory.PETS,

        // ============ SETTING ============
        listOf(
            "academy", "magic academy", "school of magic",
            "prestigious academy", "enrolled in", "student at"
        ) to TagCategory.ACADEMY,

        listOf(
            "school", "high school", "college", "university",
            "campus", "student life", "classmates"
        ) to TagCategory.SCHOOL_LIFE,

        listOf(
            "medieval", "middle ages", "feudal", "knights",
            "castles", "lords and ladies", "kingdom"
        ) to TagCategory.MEDIEVAL,

        listOf(
            "modern day", "contemporary", "present day",
            "real world", "current era", "21st century"
        ) to TagCategory.MODERN,

        listOf(
            "historical", "ancient", "dynasty", "era",
            "period piece", "set in the past"
        ) to TagCategory.HISTORICAL,

        // ============ TONE ============
        listOf(
            "dark", "darkness", "grim", "bleak", "hopeless",
            "despair", "tragic", "gritty"
        ) to TagCategory.DARK,

        listOf(
            "lighthearted", "light-hearted", "fun", "cheerful",
            "humorous", "upbeat", "carefree"
        ) to TagCategory.LIGHTHEARTED,

        listOf(
            "wholesome", "heartwarming", "feel-good", "feel good",
            "healing", "comforting", "warm"
        ) to TagCategory.WHOLESOME,

        listOf(
            "fluffy", "cute", "adorable", "sweet", "sugary",
            "tooth-rotting", "fluff"
        ) to TagCategory.FLUFFY,

        // ============ CONTENT WARNINGS ============
        listOf(
            "gore", "gory", "bloody", "graphic violence",
            "dismemberment", "brutal", "gruesome"
        ) to TagCategory.GORE,

        listOf(
            "psychological", "mind games", "mental torture",
            "psychological horror", "manipulation", "gaslighting"
        ) to TagCategory.PSYCHOLOGICAL,

        listOf(
            "trauma", "ptsd", "abuse", "abused", "traumatic",
            "scarred", "suffered"
        ) to TagCategory.TRAUMA,

        // ============ MAGIC/SUPERNATURAL ============
        listOf(
            "magic", "magical", "mage", "mages", "wizard", "wizards",
            "sorcerer", "sorcery", "spellcaster", "spells"
        ) to TagCategory.MAGIC,

        listOf(
            "witch", "witches", "witchcraft", "coven",
            "witch hunt", "hex", "curse"
        ) to TagCategory.WITCHES,

        listOf(
            "necromancy", "necromancer", "raise the dead",
            "undead army", "control undead", "death magic"
        ) to TagCategory.NECROMANCY,

        listOf(
            "summoning", "summoner", "summon", "summoned creatures",
            "contracted beasts", "spirit summoning"
        ) to TagCategory.SUMMONING,

        listOf(
            "supernatural", "paranormal", "ghosts", "haunted",
            "spirits", "otherworldly", "unexplained"
        ) to TagCategory.SUPERNATURAL,

        // ============ MISC ============
        listOf(
            "non-human", "monster protagonist", "reborn as a",
            "became a monster", "slime", "skeleton", "goblin mc"
        ) to TagCategory.NON_HUMAN_MC,

        listOf(
            "secret identity", "hidden identity", "disguised",
            "hiding who", "true identity", "mask", "undercover"
        ) to TagCategory.SECRET_IDENTITY,

        listOf(
            "sports", "athlete", "championship", "football",
            "basketball", "soccer", "tennis", "baseball", "boxing"
        ) to TagCategory.SPORTS,
    )

    // Pre-compile lowercase patterns for efficiency
    private val compiledPatterns: List<Pair<List<String>, TagCategory>> by lazy {
        extractionPatterns.map { (keywords, tag) ->
            keywords.map { it.lowercase() } to tag
        }
    }

    /**
     * Extract tags from a synopsis/description.
     *
     * @param synopsis The novel's synopsis text
     * @param maxTags Maximum number of tags to return (prioritizes more confident matches)
     * @return Set of extracted tag categories
     */
    fun extractTags(synopsis: String?, maxTags: Int = 10): Set<TagCategory> {
        if (synopsis.isNullOrBlank()) return emptySet()

        val lowerSynopsis = synopsis.lowercase()
        val tagScores = mutableMapOf<TagCategory, Int>()

        for ((keywords, tag) in compiledPatterns) {
            var matches = 0
            for (keyword in keywords) {
                if (keyword in lowerSynopsis) {
                    matches++
                }
            }

            if (matches > 0) {
                // Weight by number of matching keywords
                tagScores[tag] = (tagScores[tag] ?: 0) + matches
            }
        }

        // Sort by score and take top N
        return tagScores.entries
            .sortedByDescending { it.value }
            .take(maxTags)
            .map { it.key }
            .toSet()
    }

    /**
     * Extract tags with confidence scores
     *
     * @return Map of tag to confidence (0.0 - 1.0)
     */
    fun extractTagsWithConfidence(synopsis: String?): Map<TagCategory, Float> {
        if (synopsis.isNullOrBlank()) return emptyMap()

        val lowerSynopsis = synopsis.lowercase()
        val tagScores = mutableMapOf<TagCategory, Int>()
        val tagMaxScores = mutableMapOf<TagCategory, Int>()

        for ((keywords, tag) in compiledPatterns) {
            var matches = 0
            for (keyword in keywords) {
                if (keyword in lowerSynopsis) {
                    matches++
                }
            }

            // Track max possible score for this tag
            tagMaxScores[tag] = (tagMaxScores[tag] ?: 0) + keywords.size

            if (matches > 0) {
                tagScores[tag] = (tagScores[tag] ?: 0) + matches
            }
        }

        // Convert to confidence scores
        return tagScores.mapValues { (tag, score) ->
            val maxScore = tagMaxScores[tag] ?: 1
            (score.toFloat() / maxScore).coerceIn(0f, 1f)
        }
    }

    /**
     * Combine extracted tags with existing tags, avoiding duplicates
     */
    fun enhanceTags(
        existingTags: Set<TagCategory>,
        synopsis: String?,
        maxAdditional: Int = 5
    ): Set<TagCategory> {
        val extracted = extractTagsWithConfidence(synopsis)
            .filter { (tag, confidence) ->
                tag !in existingTags && confidence >= 0.3f
            }
            .entries
            .sortedByDescending { it.value }
            .take(maxAdditional)
            .map { it.key }
            .toSet()

        return existingTags + extracted
    }
}