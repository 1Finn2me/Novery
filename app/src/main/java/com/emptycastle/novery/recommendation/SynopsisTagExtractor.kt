package com.emptycastle.novery.recommendation

import com.emptycastle.novery.recommendation.TagNormalizer.TagCategory

/**
 * Extracts probable tags from novel synopsis/description and title.
 * Used when providers don't supply tags or to enhance sparse tag data.
 */
object SynopsisTagExtractor {

    /**
     * Title patterns - shorter, more specific patterns for titles
     */
    private val titlePatterns: List<Pair<List<String>, TagCategory>> = listOf(
        // Cultivation/Eastern
        listOf("cultivation", "cultivator", "immortal", "dao", "sect", "martial") to TagCategory.CULTIVATION,
        listOf("wuxia") to TagCategory.WUXIA,
        listOf("xianxia") to TagCategory.XIANXIA,
        listOf("murim") to TagCategory.MURIM,

        // Isekai/Reincarnation
        listOf("isekai", "another world", "otherworld", "transported") to TagCategory.ISEKAI,
        listOf("reincarnated", "reincarnation", "rebirth", "reborn") to TagCategory.REINCARNATION,
        listOf("transmigrat") to TagCategory.TRANSMIGRATION,
        listOf("regressor", "regression", "return", "second chance") to TagCategory.REGRESSION,
        listOf("time loop", "loop") to TagCategory.TIME_LOOP,
        listOf("summoned hero", "hero summoning") to TagCategory.SUMMONED_HERO,

        // LitRPG/System
        listOf("litrpg", "lit rpg") to TagCategory.LITRPG,
        listOf("system", "status window", "status screen") to TagCategory.SYSTEM,
        listOf("dungeon") to TagCategory.DUNGEON,
        listOf("tower") to TagCategory.TOWER,
        listOf("level", "leveling") to TagCategory.PROGRESSION,
        listOf("vrmmo", "virtual reality", "vr") to TagCategory.VIRTUAL_REALITY,

        // Romance types
        listOf("harem") to TagCategory.HAREM,
        listOf("reverse harem", "otome") to TagCategory.REVERSE_HAREM,
        listOf("romance", "love") to TagCategory.ROMANCE,

        // LGBTQ+
        listOf("bl", "boys love", "yaoi", "danmei") to TagCategory.BL,
        listOf("gl", "girls love", "yuri", "baihe") to TagCategory.GL,
        listOf("gender bender", "genderbend") to TagCategory.GENDER_BENDER,

        // Lead types
        listOf("villainess", "villain") to TagCategory.VILLAIN_PROTAGONIST,
        listOf("op mc", "overpowered", "strongest", "invincible") to TagCategory.OP_MC,
        listOf("weak to strong", "zero to hero") to TagCategory.WEAK_TO_STRONG,

        // Creatures
        listOf("dragon") to TagCategory.DRAGONS,
        listOf("vampire") to TagCategory.VAMPIRES,
        listOf("werewolf", "wolf") to TagCategory.WEREWOLVES,
        listOf("zombie", "undead") to TagCategory.ZOMBIES,
        listOf("demon", "devil") to TagCategory.DEMONS,
        listOf("god", "divine", "deity") to TagCategory.GODS,
        listOf("slime", "skeleton", "goblin", "monster") to TagCategory.NON_HUMAN_MC,

        // Themes
        listOf("revenge", "vengeance") to TagCategory.REVENGE,
        listOf("kingdom building", "empire") to TagCategory.KINGDOM_BUILDING,
        listOf("apocalypse", "post-apocalyptic") to TagCategory.APOCALYPSE,
        listOf("survival", "survive") to TagCategory.SURVIVAL,
        listOf("academy", "school", "magic academy") to TagCategory.ACADEMY,

        // Genres
        listOf("fantasy") to TagCategory.FANTASY,
        listOf("sci-fi", "scifi", "science fiction") to TagCategory.SCI_FI,
        listOf("horror") to TagCategory.HORROR,
        listOf("mystery", "detective") to TagCategory.MYSTERY,
        listOf("thriller") to TagCategory.THRILLER,
        listOf("comedy", "humor") to TagCategory.COMEDY,

        // Setting
        listOf("medieval", "knight") to TagCategory.MEDIEVAL,
        listOf("modern", "contemporary") to TagCategory.MODERN,
        listOf("historical", "dynasty", "ancient") to TagCategory.HISTORICAL,
        listOf("space", "galactic", "starship") to TagCategory.SPACE_OPERA,
        listOf("cyberpunk") to TagCategory.CYBERPUNK,

        // Activities
        listOf("cooking", "chef", "food") to TagCategory.COOKING,
        listOf("alchemy", "alchemist", "potion") to TagCategory.ALCHEMY,
        listOf("blacksmith", "crafting") to TagCategory.CRAFTING,
    )

    /**
     * Keyword patterns for each tag category (synopsis).
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

    private val compiledTitlePatterns: List<Pair<List<String>, TagCategory>> by lazy {
        titlePatterns.map { (keywords, tag) ->
            keywords.map { it.lowercase() } to tag
        }
    }

    /**
     * Extract tags from novel title.
     * Titles often contain genre hints like "Reincarnated as a Slime" or "My Cultivation System"
     */
    fun extractFromTitle(title: String?, maxTags: Int = 5): Set<TagCategory> {
        if (title.isNullOrBlank()) return emptySet()

        val lowerTitle = title.lowercase()
        val tagScores = mutableMapOf<TagCategory, Int>()

        for ((keywords, tag) in compiledTitlePatterns) {
            for (keyword in keywords) {
                if (lowerTitle.contains(keyword)) {
                    tagScores[tag] = (tagScores[tag] ?: 0) + 2 // Title matches are high confidence
                }
            }
        }

        return tagScores.entries
            .sortedByDescending { it.value }
            .take(maxTags)
            .map { it.key }
            .toSet()
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
                tagScores[tag] = (tagScores[tag] ?: 0) + matches
            }
        }

        return tagScores.entries
            .sortedByDescending { it.value }
            .take(maxTags)
            .map { it.key }
            .toSet()
    }

    /**
     * Extract tags from both title and synopsis, combining results.
     * Title matches get priority.
     */
    fun extractFromTitleAndSynopsis(
        title: String?,
        synopsis: String?,
        maxTags: Int = 12
    ): Set<TagCategory> {
        val titleTags = extractFromTitle(title, maxTags = 5)
        val synopsisTags = extractTags(synopsis, maxTags = 10)

        // Title tags first, then fill with synopsis tags
        val combined = titleTags.toMutableSet()
        for (tag in synopsisTags) {
            if (combined.size >= maxTags) break
            combined.add(tag)
        }

        return combined
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

            tagMaxScores[tag] = (tagMaxScores[tag] ?: 0) + keywords.size

            if (matches > 0) {
                tagScores[tag] = (tagScores[tag] ?: 0) + matches
            }
        }

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
        title: String?,
        synopsis: String?,
        maxAdditional: Int = 5
    ): Set<TagCategory> {
        // Extract from both title and synopsis
        val titleTags = extractFromTitle(title, maxTags = 3)
        val synopsisTags = extractTagsWithConfidence(synopsis)
            .filter { (tag, confidence) ->
                tag !in existingTags && tag !in titleTags && confidence >= 0.3f
            }
            .entries
            .sortedByDescending { it.value }
            .take(maxAdditional)
            .map { it.key }
            .toSet()

        return existingTags + titleTags + synopsisTags
    }

    /**
     * Extract significant keywords from synopsis for similarity matching.
     * These are general content words, not just tag-related.
     */
    fun extractContentKeywords(synopsis: String?, maxKeywords: Int = 30): Set<String> {
        if (synopsis.isNullOrBlank()) return emptySet()

        val stopWords = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "as", "is", "was", "are", "were", "been",
            "be", "have", "has", "had", "do", "does", "did", "will", "would",
            "could", "should", "may", "might", "must", "shall", "can", "need",
            "he", "she", "it", "they", "we", "you", "i", "his", "her", "its",
            "their", "our", "your", "my", "this", "that", "these", "those",
            "who", "whom", "which", "what", "where", "when", "why", "how",
            "all", "each", "every", "both", "few", "more", "most", "other",
            "some", "such", "no", "nor", "not", "only", "own", "same", "so",
            "than", "too", "very", "just", "also", "now", "here", "there",
            "into", "through", "during", "before", "after", "above", "below",
            "between", "under", "again", "further", "then", "once", "upon",
            "about", "out", "up", "down", "off", "over", "any", "because",
            "while", "until", "unless", "although", "though", "since", "whether",
            "him", "her", "them", "me", "us", "himself", "herself", "itself",
            "themselves", "myself", "ourselves", "yourself", "yourselves",
            "been", "being", "having", "doing", "going", "coming", "getting",
            "making", "taking", "seeing", "knowing", "thinking", "wanting"
        )

        // Significant words to boost (genre/theme indicators)
        val boostWords = setOf(
            "magic", "sword", "power", "battle", "fight", "kingdom", "empire",
            "dragon", "demon", "god", "goddess", "hero", "villain", "dark",
            "light", "shadow", "blood", "death", "life", "love", "hate",
            "revenge", "justice", "evil", "good", "ancient", "modern",
            "future", "past", "time", "space", "world", "realm", "dimension",
            "skill", "level", "system", "class", "dungeon", "tower", "gate",
            "monster", "beast", "spirit", "soul", "immortal", "mortal",
            "cultivation", "martial", "sect", "clan", "family", "noble",
            "princess", "prince", "king", "queen", "emperor", "empress"
        )

        return synopsis
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { word ->
                word.length >= 3 &&
                        word !in stopWords &&
                        !word.all { it.isDigit() }
            }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> {
                    if (it.key in boostWords) it.value * 2 else it.value
                }
            )
            .take(maxKeywords)
            .map { it.key }
            .toSet()
    }
}