// com/emptycastle/novery/recommendation/TagNormalizer.kt
package com.emptycastle.novery.recommendation

/**
 * Normalizes tags across different providers to enable cross-provider recommendations.
 * Maps provider-specific tag names to canonical tags.
 */
object TagNormalizer {

    // ================================================================
    // CANONICAL TAG CATEGORIES - Significantly Expanded
    // ================================================================

    enum class TagCategory {
        // ============ MAIN GENRES ============
        ACTION,
        ADVENTURE,
        COMEDY,
        DRAMA,
        FANTASY,
        HORROR,
        MYSTERY,
        ROMANCE,
        SCI_FI,
        SLICE_OF_LIFE,
        THRILLER,
        TRAGEDY,

        // ============ FANTASY SUB-GENRES ============
        HIGH_FANTASY,
        LOW_FANTASY,
        URBAN_FANTASY,
        DARK_FANTASY,
        EPIC_FANTASY,
        PORTAL_FANTASY,
        SWORD_AND_SORCERY,
        MYTHIC_FANTASY,

        // ============ SCI-FI SUB-GENRES ============
        SPACE_OPERA,
        CYBERPUNK,
        POST_APOCALYPTIC,
        HARD_SCI_FI,
        SOFT_SCI_FI,
        DYSTOPIAN,
        MECHA,
        FIRST_CONTACT,

        // ============ EASTERN/CULTIVATION ============
        CULTIVATION,
        WUXIA,
        XIANXIA,
        XUANHUAN,
        MURIM,
        MARTIAL_ARTS,
        QI,
        IMMORTAL,
        SECT,

        // ============ ISEKAI/REINCARNATION ============
        ISEKAI,
        REINCARNATION,
        TRANSMIGRATION,
        SUMMONED_HERO,
        SECOND_CHANCE,
        REGRESSION,
        TIME_LOOP,
        TIME_TRAVEL,

        // ============ LITRPG/GAMELIT ============
        LITRPG,
        GAMELIT,
        SYSTEM,
        PROGRESSION,
        DUNGEON,
        TOWER,
        VIRTUAL_REALITY,

        // ============ ROMANCE SUB-TYPES ============
        SLOW_BURN,
        ENEMIES_TO_LOVERS,
        FRIENDS_TO_LOVERS,
        FORBIDDEN_LOVE,
        ARRANGED_MARRIAGE,
        REVERSE_HAREM,
        HAREM,
        POLYAMORY,

        // ============ LGBTQ+ / RELATIONSHIP TYPES ============
        BL,              // Boys Love / Yaoi / Shounen Ai / M/M
        GL,              // Girls Love / Yuri / Shoujo Ai / F/F
        LGBT,            // General LGBTQ+ content
        GENDER_BENDER,   // Gender swap / Genderbend
        NON_BINARY,

        // ============ DEMOGRAPHICS ============
        SEINEN,
        SHOUNEN,
        SHOUJO,
        JOSEI,

        // ============ LEAD/CHARACTER TYPES ============
        MALE_LEAD,
        FEMALE_LEAD,
        ANTI_HERO,
        VILLAIN_PROTAGONIST,
        STRONG_LEAD,
        OP_MC,           // Overpowered main character
        WEAK_TO_STRONG,
        SMART_MC,
        RUTHLESS_MC,
        KIND_MC,
        MORALLY_GREY,

        // ============ SETTING/WORLD ============
        HISTORICAL,
        MEDIEVAL,
        MODERN,
        URBAN,
        ACADEMY,
        SCHOOL_LIFE,
        MILITARY,
        KINGDOM_BUILDING,
        SURVIVAL,
        APOCALYPSE,

        // ============ THEMES/TROPES ============
        REVENGE,
        BETRAYAL,
        REDEMPTION,
        COMING_OF_AGE,
        FAMILY,
        FOUND_FAMILY,
        UNDERDOG,
        TOURNAMENT,
        WAR,
        POLITICS,
        STRATEGY,
        BUSINESS,
        COOKING,
        CRAFTING,
        PETS,
        MONSTERS,
        DRAGONS,
        VAMPIRES,
        WEREWOLVES,
        ZOMBIES,
        ELVES,
        DEMONS,
        GODS,
        SPIRITS,

        // ============ NARRATIVE STYLE ============
        FIRST_PERSON,
        MULTIPLE_POV,
        NON_HUMAN_MC,
        SECRET_IDENTITY,
        MISUNDERSTANDINGS,
        FAN_FICTION,
        PARODY,
        SATIRE,
        SHORT_STORY,

        // ============ CONTENT WARNINGS / MATURE ============
        MATURE,
        ADULT,
        SMUT,
        ECCHI,
        GORE,
        VIOLENCE,
        PSYCHOLOGICAL,
        DARK,
        TRAUMA,

        // ============ TONE/MOOD ============
        LIGHTHEARTED,
        GRIMDARK,
        WHOLESOME,
        FLUFFY,
        ANGST,
        BITTERSWEET,

        // ============ PACING/STRUCTURE ============
        FAST_PACED,
        SLOW_PACED,
        EPISODIC,

        // ============ QUALITY INDICATORS ============
        COMPLETED,
        ONGOING,
        HIATUS,

        // ============ SUPERNATURAL/MAGIC ============
        MAGIC,
        SUPERNATURAL,
        WITCHES,
        NECROMANCY,
        SUMMONING,
        ALCHEMY,

        // ============ OTHER ============
        SPORTS,
        MUSIC,
        ART,
        ANIME,  // Anime-style/inspired
        MANHUA,
        MANHWA,
        WUXIA_INSPIRED,
    }

    // ================================================================
    // TAG ALIASES - Comprehensive mapping from provider tags
    // ================================================================

    private val tagAliases: Map<String, TagCategory> = buildMap {
        // ============ ACTION/ADVENTURE ============
        put("action", TagCategory.ACTION)
        put("adventure", TagCategory.ADVENTURE)
        put("quest", TagCategory.ADVENTURE)

        // ============ COMEDY/DRAMA ============
        put("comedy", TagCategory.COMEDY)
        put("humor", TagCategory.COMEDY)
        put("funny", TagCategory.COMEDY)
        put("parody", TagCategory.PARODY)
        put("satire", TagCategory.SATIRE)
        put("drama", TagCategory.DRAMA)
        put("tragedy", TagCategory.TRAGEDY)
        put("melodrama", TagCategory.DRAMA)

        // ============ FANTASY VARIANTS ============
        put("fantasy", TagCategory.FANTASY)
        put("high fantasy", TagCategory.HIGH_FANTASY)
        put("high_fantasy", TagCategory.HIGH_FANTASY)
        put("epic fantasy", TagCategory.EPIC_FANTASY)
        put("low fantasy", TagCategory.LOW_FANTASY)
        put("low_fantasy", TagCategory.LOW_FANTASY)
        put("urban fantasy", TagCategory.URBAN_FANTASY)
        put("urban_fantasy", TagCategory.URBAN_FANTASY)
        put("contemporary fantasy", TagCategory.URBAN_FANTASY)
        put("dark fantasy", TagCategory.DARK_FANTASY)
        put("dark_fantasy", TagCategory.DARK_FANTASY)
        put("grimdark", TagCategory.GRIMDARK)
        put("grim dark", TagCategory.GRIMDARK)
        put("portal fantasy", TagCategory.PORTAL_FANTASY)
        put("portal fantasy / isekai", TagCategory.ISEKAI)
        put("sword and sorcery", TagCategory.SWORD_AND_SORCERY)
        put("sword & sorcery", TagCategory.SWORD_AND_SORCERY)
        put("mythos", TagCategory.MYTHIC_FANTASY)
        put("mythic", TagCategory.MYTHIC_FANTASY)
        put("mythology", TagCategory.MYTHIC_FANTASY)

        // ============ SCI-FI VARIANTS ============
        put("sci-fi", TagCategory.SCI_FI)
        put("sci_fi", TagCategory.SCI_FI)
        put("scifi", TagCategory.SCI_FI)
        put("science fiction", TagCategory.SCI_FI)
        put("sf", TagCategory.SCI_FI)
        put("hard sci-fi", TagCategory.HARD_SCI_FI)
        put("hard_sci-fi", TagCategory.HARD_SCI_FI)
        put("hard scifi", TagCategory.HARD_SCI_FI)
        put("soft sci-fi", TagCategory.SOFT_SCI_FI)
        put("soft_sci-fi", TagCategory.SOFT_SCI_FI)
        put("space opera", TagCategory.SPACE_OPERA)
        put("space_opera", TagCategory.SPACE_OPERA)
        put("space", TagCategory.SPACE_OPERA)
        put("cyberpunk", TagCategory.CYBERPUNK)
        put("cyber punk", TagCategory.CYBERPUNK)
        put("post-apocalyptic", TagCategory.POST_APOCALYPTIC)
        put("post apocalyptic", TagCategory.POST_APOCALYPTIC)
        put("post_apocalyptic", TagCategory.POST_APOCALYPTIC)
        put("apocalypse", TagCategory.APOCALYPSE)
        put("apocalyptic", TagCategory.APOCALYPSE)
        put("dystopia", TagCategory.DYSTOPIAN)
        put("dystopian", TagCategory.DYSTOPIAN)
        put("mecha", TagCategory.MECHA)
        put("mech", TagCategory.MECHA)
        put("robots", TagCategory.MECHA)
        put("first contact", TagCategory.FIRST_CONTACT)
        put("first_contact", TagCategory.FIRST_CONTACT)
        put("aliens", TagCategory.FIRST_CONTACT)

        // ============ EASTERN/CULTIVATION ============
        put("cultivation", TagCategory.CULTIVATION)
        put("cultivator", TagCategory.CULTIVATION)
        put("eastern", TagCategory.CULTIVATION)
        put("eastern fantasy", TagCategory.CULTIVATION)
        put("wuxia", TagCategory.WUXIA)
        put("wu xia", TagCategory.WUXIA)
        put("xianxia", TagCategory.XIANXIA)
        put("xian xia", TagCategory.XIANXIA)
        put("xuanhuan", TagCategory.XUANHUAN)
        put("xuan huan", TagCategory.XUANHUAN)
        put("murim", TagCategory.MURIM)
        put("martial arts", TagCategory.MARTIAL_ARTS)
        put("martial_arts", TagCategory.MARTIAL_ARTS)
        put("martialarts", TagCategory.MARTIAL_ARTS)
        put("kung fu", TagCategory.MARTIAL_ARTS)
        put("kungfu", TagCategory.MARTIAL_ARTS)
        put("immortal", TagCategory.IMMORTAL)
        put("immortality", TagCategory.IMMORTAL)
        put("immortals", TagCategory.IMMORTAL)
        put("daoist", TagCategory.CULTIVATION)
        put("taoism", TagCategory.CULTIVATION)
        put("sect", TagCategory.SECT)
        put("sects", TagCategory.SECT)
        put("clan", TagCategory.SECT)

        // ============ ISEKAI/REINCARNATION ============
        put("isekai", TagCategory.ISEKAI)
        put("portal fantasy / isekai", TagCategory.ISEKAI)
        put("portal fantasy", TagCategory.PORTAL_FANTASY)
        put("summoned hero", TagCategory.SUMMONED_HERO)
        put("summoned_hero", TagCategory.SUMMONED_HERO)
        put("transported to another world", TagCategory.ISEKAI)
        put("another world", TagCategory.ISEKAI)
        put("otherworld", TagCategory.ISEKAI)
        put("other world", TagCategory.ISEKAI)
        put("reincarnation", TagCategory.REINCARNATION)
        put("reincarnated", TagCategory.REINCARNATION)
        put("rebirth", TagCategory.REINCARNATION)
        put("transmigration", TagCategory.TRANSMIGRATION)
        put("transmigrated", TagCategory.TRANSMIGRATION)
        put("second chance", TagCategory.SECOND_CHANCE)
        put("second_chance", TagCategory.SECOND_CHANCE)
        put("regression", TagCategory.REGRESSION)
        put("regressor", TagCategory.REGRESSION)
        put("time loop", TagCategory.TIME_LOOP)
        put("time_loop", TagCategory.TIME_LOOP)
        put("loop", TagCategory.TIME_LOOP)
        put("groundhog day", TagCategory.TIME_LOOP)
        put("time travel", TagCategory.TIME_TRAVEL)
        put("time_travel", TagCategory.TIME_TRAVEL)
        put("timetravel", TagCategory.TIME_TRAVEL)

        // ============ LITRPG/GAMELIT ============
        put("litrpg", TagCategory.LITRPG)
        put("lit rpg", TagCategory.LITRPG)
        put("lit-rpg", TagCategory.LITRPG)
        put("gamelit", TagCategory.GAMELIT)
        put("game lit", TagCategory.GAMELIT)
        put("game-lit", TagCategory.GAMELIT)
        put("game", TagCategory.GAMELIT)
        put("games", TagCategory.GAMELIT)
        put("video games", TagCategory.GAMELIT)
        put("video_games", TagCategory.GAMELIT)
        put("vrmmo", TagCategory.VIRTUAL_REALITY)
        put("vr", TagCategory.VIRTUAL_REALITY)
        put("virtual reality", TagCategory.VIRTUAL_REALITY)
        put("virtual_reality", TagCategory.VIRTUAL_REALITY)
        put("system", TagCategory.SYSTEM)
        put("game system", TagCategory.SYSTEM)
        put("status screen", TagCategory.SYSTEM)
        put("status window", TagCategory.SYSTEM)
        put("progression", TagCategory.PROGRESSION)
        put("progression fantasy", TagCategory.PROGRESSION)
        put("power progression", TagCategory.PROGRESSION)
        put("leveling", TagCategory.PROGRESSION)
        put("level up", TagCategory.PROGRESSION)
        put("dungeon", TagCategory.DUNGEON)
        put("dungeons", TagCategory.DUNGEON)
        put("dungeon core", TagCategory.DUNGEON)
        put("dungeon crawler", TagCategory.DUNGEON)
        put("tower", TagCategory.TOWER)
        put("tower climbing", TagCategory.TOWER)
        put("tower defense", TagCategory.TOWER)

        // ============ ROMANCE ============
        put("romance", TagCategory.ROMANCE)
        put("romantic", TagCategory.ROMANCE)
        put("love", TagCategory.ROMANCE)
        put("slow burn", TagCategory.SLOW_BURN)
        put("slow_burn", TagCategory.SLOW_BURN)
        put("slowburn", TagCategory.SLOW_BURN)
        put("slow romance", TagCategory.SLOW_BURN)
        put("enemies to lovers", TagCategory.ENEMIES_TO_LOVERS)
        put("enemies-to-lovers", TagCategory.ENEMIES_TO_LOVERS)
        put("friends to lovers", TagCategory.FRIENDS_TO_LOVERS)
        put("friends-to-lovers", TagCategory.FRIENDS_TO_LOVERS)
        put("forbidden love", TagCategory.FORBIDDEN_LOVE)
        put("forbidden romance", TagCategory.FORBIDDEN_LOVE)
        put("arranged marriage", TagCategory.ARRANGED_MARRIAGE)
        put("contract marriage", TagCategory.ARRANGED_MARRIAGE)
        put("harem", TagCategory.HAREM)
        put("reverse harem", TagCategory.REVERSE_HAREM)
        put("reverse_harem", TagCategory.REVERSE_HAREM)
        put("otome", TagCategory.REVERSE_HAREM)
        put("polyamory", TagCategory.POLYAMORY)
        put("poly", TagCategory.POLYAMORY)
        put("multiple love interests", TagCategory.POLYAMORY)

        // ============ LGBTQ+ / BL / GL ============
        // Boys Love variants
        put("bl", TagCategory.BL)
        put("boys love", TagCategory.BL)
        put("boys' love", TagCategory.BL)
        put("boy's love", TagCategory.BL)
        put("boyslove", TagCategory.BL)
        put("yaoi", TagCategory.BL)
        put("shounen ai", TagCategory.BL)
        put("shounen-ai", TagCategory.BL)
        put("shounen_ai", TagCategory.BL)
        put("shonen ai", TagCategory.BL)
        put("shonen-ai", TagCategory.BL)
        put("m/m", TagCategory.BL)
        put("mm", TagCategory.BL)
        put("male/male", TagCategory.BL)
        put("mlm", TagCategory.BL)
        put("gay", TagCategory.BL)
        put("gay romance", TagCategory.BL)
        put("danmei", TagCategory.BL)
        put("tanbi", TagCategory.BL)

        // Girls Love variants
        put("gl", TagCategory.GL)
        put("girls love", TagCategory.GL)
        put("girls' love", TagCategory.GL)
        put("girl's love", TagCategory.GL)
        put("girlslove", TagCategory.GL)
        put("yuri", TagCategory.GL)
        put("shoujo ai", TagCategory.GL)
        put("shoujo-ai", TagCategory.GL)
        put("shoujo_ai", TagCategory.GL)
        put("shojo ai", TagCategory.GL)
        put("shojo-ai", TagCategory.GL)
        put("f/f", TagCategory.GL)
        put("ff", TagCategory.GL)
        put("female/female", TagCategory.GL)
        put("wlw", TagCategory.GL)
        put("lesbian", TagCategory.GL)
        put("lesbian romance", TagCategory.GL)
        put("baihe", TagCategory.GL)

        // General LGBTQ+
        put("lgbt", TagCategory.LGBT)
        put("lgbt+", TagCategory.LGBT)
        put("lgbtq", TagCategory.LGBT)
        put("lgbtq+", TagCategory.LGBT)
        put("lgbtqia", TagCategory.LGBT)
        put("lgbtqia+", TagCategory.LGBT)
        put("queer", TagCategory.LGBT)
        put("bisexual", TagCategory.LGBT)
        put("pansexual", TagCategory.LGBT)
        put("transgender", TagCategory.LGBT)
        put("trans", TagCategory.LGBT)
        put("non-binary", TagCategory.NON_BINARY)
        put("nonbinary", TagCategory.NON_BINARY)
        put("enby", TagCategory.NON_BINARY)
        put("genderfluid", TagCategory.NON_BINARY)

        // Gender bender
        put("gender bender", TagCategory.GENDER_BENDER)
        put("gender_bender", TagCategory.GENDER_BENDER)
        put("genderbender", TagCategory.GENDER_BENDER)
        put("genderbend", TagCategory.GENDER_BENDER)
        put("gender swap", TagCategory.GENDER_BENDER)
        put("genderswap", TagCategory.GENDER_BENDER)
        put("body swap", TagCategory.GENDER_BENDER)
        put("sex change", TagCategory.GENDER_BENDER)

        // ============ DEMOGRAPHICS ============
        put("seinen", TagCategory.SEINEN)
        put("shounen", TagCategory.SHOUNEN)
        put("shonen", TagCategory.SHOUNEN)
        put("shoujo", TagCategory.SHOUJO)
        put("shojo", TagCategory.SHOUJO)
        put("josei", TagCategory.JOSEI)
        put("mature audience", TagCategory.SEINEN)

        // ============ LEAD TYPES ============
        put("male lead", TagCategory.MALE_LEAD)
        put("male_lead", TagCategory.MALE_LEAD)
        put("male protagonist", TagCategory.MALE_LEAD)
        put("male mc", TagCategory.MALE_LEAD)
        put("female lead", TagCategory.FEMALE_LEAD)
        put("female_lead", TagCategory.FEMALE_LEAD)
        put("female protagonist", TagCategory.FEMALE_LEAD)
        put("female mc", TagCategory.FEMALE_LEAD)
        put("anti-hero lead", TagCategory.ANTI_HERO)
        put("anti-hero", TagCategory.ANTI_HERO)
        put("antihero", TagCategory.ANTI_HERO)
        put("anti hero", TagCategory.ANTI_HERO)
        put("villain protagonist", TagCategory.VILLAIN_PROTAGONIST)
        put("villain mc", TagCategory.VILLAIN_PROTAGONIST)
        put("evil mc", TagCategory.VILLAIN_PROTAGONIST)
        put("strong lead", TagCategory.STRONG_LEAD)
        put("strong_lead", TagCategory.STRONG_LEAD)
        put("strong protagonist", TagCategory.STRONG_LEAD)
        put("overpowered", TagCategory.OP_MC)
        put("op mc", TagCategory.OP_MC)
        put("op protagonist", TagCategory.OP_MC)
        put("cheats", TagCategory.OP_MC)
        put("cheat", TagCategory.OP_MC)
        put("weak to strong", TagCategory.WEAK_TO_STRONG)
        put("weak-to-strong", TagCategory.WEAK_TO_STRONG)
        put("zero to hero", TagCategory.WEAK_TO_STRONG)
        put("underdog", TagCategory.UNDERDOG)
        put("smart mc", TagCategory.SMART_MC)
        put("intelligent mc", TagCategory.SMART_MC)
        put("genius", TagCategory.SMART_MC)
        put("ruthless", TagCategory.RUTHLESS_MC)
        put("ruthless mc", TagCategory.RUTHLESS_MC)
        put("cold mc", TagCategory.RUTHLESS_MC)
        put("kind mc", TagCategory.KIND_MC)
        put("naive mc", TagCategory.KIND_MC)
        put("morally grey", TagCategory.MORALLY_GREY)
        put("morally gray", TagCategory.MORALLY_GREY)
        put("grey morality", TagCategory.MORALLY_GREY)

        // ============ SETTING ============
        put("historical", TagCategory.HISTORICAL)
        put("history", TagCategory.HISTORICAL)
        put("medieval", TagCategory.MEDIEVAL)
        put("middle ages", TagCategory.MEDIEVAL)
        put("modern", TagCategory.MODERN)
        put("modern day", TagCategory.MODERN)
        put("contemporary", TagCategory.MODERN)
        put("modern life", TagCategory.MODERN)
        put("urban", TagCategory.URBAN)
        put("urban life", TagCategory.URBAN)
        put("city", TagCategory.URBAN)
        put("academy", TagCategory.ACADEMY)
        put("magic academy", TagCategory.ACADEMY)
        put("school", TagCategory.SCHOOL_LIFE)
        put("school life", TagCategory.SCHOOL_LIFE)
        put("school_life", TagCategory.SCHOOL_LIFE)
        put("high school", TagCategory.SCHOOL_LIFE)
        put("college", TagCategory.SCHOOL_LIFE)
        put("university", TagCategory.SCHOOL_LIFE)
        put("military", TagCategory.MILITARY)
        put("army", TagCategory.MILITARY)
        put("navy", TagCategory.MILITARY)
        put("soldier", TagCategory.MILITARY)
        put("war", TagCategory.WAR)
        put("war and military", TagCategory.WAR)
        put("war_and_military", TagCategory.WAR)
        put("kingdom building", TagCategory.KINGDOM_BUILDING)
        put("kingdom_building", TagCategory.KINGDOM_BUILDING)
        put("nation building", TagCategory.KINGDOM_BUILDING)
        put("empire building", TagCategory.KINGDOM_BUILDING)
        put("ruler", TagCategory.KINGDOM_BUILDING)
        put("ruling class", TagCategory.KINGDOM_BUILDING)
        put("survival", TagCategory.SURVIVAL)
        put("survive", TagCategory.SURVIVAL)

        // ============ THEMES/TROPES ============
        put("revenge", TagCategory.REVENGE)
        put("vengeance", TagCategory.REVENGE)
        put("betrayal", TagCategory.BETRAYAL)
        put("betrayed", TagCategory.BETRAYAL)
        put("redemption", TagCategory.REDEMPTION)
        put("redemption arc", TagCategory.REDEMPTION)
        put("coming of age", TagCategory.COMING_OF_AGE)
        put("coming-of-age", TagCategory.COMING_OF_AGE)
        put("growth", TagCategory.COMING_OF_AGE)
        put("family", TagCategory.FAMILY)
        put("family life", TagCategory.FAMILY)
        put("found family", TagCategory.FOUND_FAMILY)
        put("found_family", TagCategory.FOUND_FAMILY)
        put("makeshift family", TagCategory.FOUND_FAMILY)
        put("tournament", TagCategory.TOURNAMENT)
        put("tournament arc", TagCategory.TOURNAMENT)
        put("competition", TagCategory.TOURNAMENT)
        put("politics", TagCategory.POLITICS)
        put("political", TagCategory.POLITICS)
        put("intrigue", TagCategory.POLITICS)
        put("court intrigue", TagCategory.POLITICS)
        put("strategy", TagCategory.POLITICS)
        put("strategy", TagCategory.STRATEGY)
        put("strategic", TagCategory.STRATEGY)
        put("tactics", TagCategory.STRATEGY)
        put("tactical", TagCategory.STRATEGY)
        put("scheming", TagCategory.STRATEGY)
        put("schemes", TagCategory.STRATEGY)
        put("planning", TagCategory.STRATEGY)
        put("mind games", TagCategory.STRATEGY)
        put("business", TagCategory.BUSINESS)
        put("management", TagCategory.BUSINESS)
        put("economics", TagCategory.BUSINESS)
        put("cooking", TagCategory.COOKING)
        put("food", TagCategory.COOKING)
        put("chef", TagCategory.COOKING)
        put("crafting", TagCategory.CRAFTING)
        put("blacksmith", TagCategory.CRAFTING)
        put("alchemy", TagCategory.ALCHEMY)
        put("alchemist", TagCategory.ALCHEMY)
        put("potion", TagCategory.ALCHEMY)
        put("pets", TagCategory.PETS)
        put("monster taming", TagCategory.PETS)
        put("beast taming", TagCategory.PETS)
        put("taming", TagCategory.PETS)
        put("monsters", TagCategory.MONSTERS)
        put("monster", TagCategory.MONSTERS)
        put("beasts", TagCategory.MONSTERS)
        put("dragons", TagCategory.DRAGONS)
        put("dragon", TagCategory.DRAGONS)
        put("vampires", TagCategory.VAMPIRES)
        put("vampire", TagCategory.VAMPIRES)
        put("werewolves", TagCategory.WEREWOLVES)
        put("werewolf", TagCategory.WEREWOLVES)
        put("lycanthrope", TagCategory.WEREWOLVES)
        put("shapeshifter", TagCategory.WEREWOLVES)
        put("zombies", TagCategory.ZOMBIES)
        put("zombie", TagCategory.ZOMBIES)
        put("undead", TagCategory.ZOMBIES)
        put("elves", TagCategory.ELVES)
        put("elf", TagCategory.ELVES)
        put("elven", TagCategory.ELVES)
        put("demons", TagCategory.DEMONS)
        put("demon", TagCategory.DEMONS)
        put("devil", TagCategory.DEMONS)
        put("demonic", TagCategory.DEMONS)
        put("gods", TagCategory.GODS)
        put("god", TagCategory.GODS)
        put("deity", TagCategory.GODS)
        put("divine", TagCategory.GODS)
        put("mythology", TagCategory.GODS)
        put("spirits", TagCategory.SPIRITS)
        put("spirit", TagCategory.SPIRITS)
        put("ghosts", TagCategory.SPIRITS)
        put("ghost", TagCategory.SPIRITS)
        put("haunting", TagCategory.SPIRITS)

        // ============ NARRATIVE STYLE ============
        put("first person", TagCategory.FIRST_PERSON)
        put("first_person", TagCategory.FIRST_PERSON)
        put("1st person", TagCategory.FIRST_PERSON)
        put("multiple pov", TagCategory.MULTIPLE_POV)
        put("multiple povs", TagCategory.MULTIPLE_POV)
        put("multi pov", TagCategory.MULTIPLE_POV)
        put("ensemble cast", TagCategory.MULTIPLE_POV)
        put("non-human mc", TagCategory.NON_HUMAN_MC)
        put("non human mc", TagCategory.NON_HUMAN_MC)
        put("monster mc", TagCategory.NON_HUMAN_MC)
        put("reincarnated as monster", TagCategory.NON_HUMAN_MC)
        put("secret identity", TagCategory.SECRET_IDENTITY)
        put("secret_identity", TagCategory.SECRET_IDENTITY)
        put("hidden identity", TagCategory.SECRET_IDENTITY)
        put("misunderstanding", TagCategory.MISUNDERSTANDINGS)
        put("misunderstandings", TagCategory.MISUNDERSTANDINGS)
        put("fan-fiction", TagCategory.FAN_FICTION)
        put("fanfiction", TagCategory.FAN_FICTION)
        put("fan fiction", TagCategory.FAN_FICTION)
        put("fanfic", TagCategory.FAN_FICTION)
        put("short story", TagCategory.SHORT_STORY)
        put("short_story", TagCategory.SHORT_STORY)
        put("one shot", TagCategory.SHORT_STORY)
        put("one_shot", TagCategory.SHORT_STORY)
        put("oneshot", TagCategory.SHORT_STORY)

        // ============ CONTENT WARNINGS / MATURE ============
        put("mature", TagCategory.MATURE)
        put("mature content", TagCategory.MATURE)
        put("18+", TagCategory.ADULT)
        put("adult", TagCategory.ADULT)
        put("adult content", TagCategory.ADULT)
        put("r-18", TagCategory.ADULT)
        put("r18", TagCategory.ADULT)
        put("smut", TagCategory.SMUT)
        put("explicit", TagCategory.SMUT)
        put("lemon", TagCategory.SMUT)
        put("ecchi", TagCategory.ECCHI)
        put("fanservice", TagCategory.ECCHI)
        put("gore", TagCategory.GORE)
        put("gory", TagCategory.GORE)
        put("graphic violence", TagCategory.GORE)
        put("violence", TagCategory.VIOLENCE)
        put("violent", TagCategory.VIOLENCE)
        put("bloody", TagCategory.VIOLENCE)
        put("psychological", TagCategory.PSYCHOLOGICAL)
        put("psychological horror", TagCategory.PSYCHOLOGICAL)
        put("mind games", TagCategory.PSYCHOLOGICAL)
        put("dark", TagCategory.DARK)
        put("dark themes", TagCategory.DARK)
        put("heavy", TagCategory.DARK)
        put("trauma", TagCategory.TRAUMA)
        put("ptsd", TagCategory.TRAUMA)
        put("abuse", TagCategory.TRAUMA)

        // ============ TONE/MOOD ============
        put("lighthearted", TagCategory.LIGHTHEARTED)
        put("light-hearted", TagCategory.LIGHTHEARTED)
        put("light hearted", TagCategory.LIGHTHEARTED)
        put("fun", TagCategory.LIGHTHEARTED)
        put("wholesome", TagCategory.WHOLESOME)
        put("heartwarming", TagCategory.WHOLESOME)
        put("fluffy", TagCategory.FLUFFY)
        put("fluff", TagCategory.FLUFFY)
        put("cute", TagCategory.FLUFFY)
        put("angst", TagCategory.ANGST)
        put("angsty", TagCategory.ANGST)
        put("emotional", TagCategory.ANGST)
        put("bittersweet", TagCategory.BITTERSWEET)

        // ============ HORROR/THRILLER/MYSTERY ============
        put("horror", TagCategory.HORROR)
        put("scary", TagCategory.HORROR)
        put("thriller", TagCategory.THRILLER)
        put("suspense", TagCategory.THRILLER)
        put("mystery", TagCategory.MYSTERY)
        put("detective", TagCategory.MYSTERY)
        put("crime", TagCategory.MYSTERY)
        put("whodunit", TagCategory.MYSTERY)

        // ============ SUPERNATURAL/MAGIC ============
        put("magic", TagCategory.MAGIC)
        put("magical", TagCategory.MAGIC)
        put("mage", TagCategory.MAGIC)
        put("wizard", TagCategory.MAGIC)
        put("witch", TagCategory.WITCHES)
        put("witches", TagCategory.WITCHES)
        put("witchcraft", TagCategory.WITCHES)
        put("supernatural", TagCategory.SUPERNATURAL)
        put("paranormal", TagCategory.SUPERNATURAL)
        put("necromancy", TagCategory.NECROMANCY)
        put("necromancer", TagCategory.NECROMANCY)
        put("undead magic", TagCategory.NECROMANCY)
        put("summoning", TagCategory.SUMMONING)
        put("summoner", TagCategory.SUMMONING)
        put("summons", TagCategory.SUMMONING)

        // ============ OTHER ============
        put("sports", TagCategory.SPORTS)
        put("sport", TagCategory.SPORTS)
        put("athletic", TagCategory.SPORTS)
        put("music", TagCategory.MUSIC)
        put("musician", TagCategory.MUSIC)
        put("band", TagCategory.MUSIC)
        put("art", TagCategory.ART)
        put("artist", TagCategory.ART)
        put("arts", TagCategory.ART)
        put("anime", TagCategory.ANIME)
        put("manhua", TagCategory.MANHUA)
        put("manhwa", TagCategory.MANHWA)
        put("webtoon", TagCategory.MANHWA)
        put("slice of life", TagCategory.SLICE_OF_LIFE)
        put("slice_of_life", TagCategory.SLICE_OF_LIFE)
        put("sol", TagCategory.SLICE_OF_LIFE)
        put("daily life", TagCategory.SLICE_OF_LIFE)
        put("realistic", TagCategory.SLICE_OF_LIFE)
        put("realistic fiction", TagCategory.SLICE_OF_LIFE)
        put("magical realism", TagCategory.URBAN_FANTASY)
    }

    // ================================================================
    // RELATED TAG GROUPS - Expanded
    // ================================================================

    private val relatedTags: Map<TagCategory, Set<TagCategory>> = mapOf(
        // Cultivation cluster
        TagCategory.XIANXIA to setOf(
            TagCategory.CULTIVATION, TagCategory.MARTIAL_ARTS, TagCategory.FANTASY,
            TagCategory.IMMORTAL, TagCategory.SECT, TagCategory.QI
        ),
        TagCategory.XUANHUAN to setOf(
            TagCategory.CULTIVATION, TagCategory.MARTIAL_ARTS, TagCategory.FANTASY,
            TagCategory.ACTION
        ),
        TagCategory.WUXIA to setOf(
            TagCategory.MARTIAL_ARTS, TagCategory.HISTORICAL, TagCategory.ACTION
        ),
        TagCategory.MURIM to setOf(
            TagCategory.MARTIAL_ARTS, TagCategory.ACTION, TagCategory.CULTIVATION
        ),
        TagCategory.CULTIVATION to setOf(
            TagCategory.PROGRESSION, TagCategory.FANTASY, TagCategory.MARTIAL_ARTS
        ),

        // Isekai cluster
        TagCategory.ISEKAI to setOf(
            TagCategory.FANTASY, TagCategory.REINCARNATION, TagCategory.PORTAL_FANTASY,
            TagCategory.SUMMONED_HERO
        ),
        TagCategory.REINCARNATION to setOf(
            TagCategory.ISEKAI, TagCategory.SECOND_CHANCE, TagCategory.TRANSMIGRATION
        ),
        TagCategory.REGRESSION to setOf(
            TagCategory.TIME_LOOP, TagCategory.SECOND_CHANCE, TagCategory.REVENGE
        ),
        TagCategory.TIME_LOOP to setOf(
            TagCategory.REGRESSION, TagCategory.TIME_TRAVEL, TagCategory.SECOND_CHANCE
        ),

        // LitRPG cluster
        TagCategory.LITRPG to setOf(
            TagCategory.GAMELIT, TagCategory.SYSTEM, TagCategory.PROGRESSION,
            TagCategory.FANTASY
        ),
        TagCategory.GAMELIT to setOf(
            TagCategory.LITRPG, TagCategory.SYSTEM, TagCategory.VIRTUAL_REALITY
        ),
        TagCategory.PROGRESSION to setOf(
            TagCategory.LITRPG, TagCategory.CULTIVATION, TagCategory.WEAK_TO_STRONG
        ),
        TagCategory.DUNGEON to setOf(
            TagCategory.LITRPG, TagCategory.TOWER, TagCategory.ADVENTURE
        ),
        TagCategory.TOWER to setOf(
            TagCategory.DUNGEON, TagCategory.LITRPG, TagCategory.PROGRESSION
        ),

        // Fantasy cluster
        TagCategory.DARK_FANTASY to setOf(
            TagCategory.FANTASY, TagCategory.MATURE, TagCategory.GRIMDARK,
            TagCategory.DARK
        ),
        TagCategory.HIGH_FANTASY to setOf(
            TagCategory.FANTASY, TagCategory.EPIC_FANTASY, TagCategory.MAGIC
        ),
        TagCategory.URBAN_FANTASY to setOf(
            TagCategory.FANTASY, TagCategory.MODERN, TagCategory.SUPERNATURAL
        ),

        // Sci-Fi cluster
        TagCategory.CYBERPUNK to setOf(
            TagCategory.SCI_FI, TagCategory.DYSTOPIAN, TagCategory.URBAN
        ),
        TagCategory.SPACE_OPERA to setOf(
            TagCategory.SCI_FI, TagCategory.ADVENTURE, TagCategory.ACTION
        ),
        TagCategory.POST_APOCALYPTIC to setOf(
            TagCategory.APOCALYPSE, TagCategory.SURVIVAL, TagCategory.DYSTOPIAN
        ),

        // Romance clusters
        TagCategory.BL to setOf(
            TagCategory.ROMANCE, TagCategory.LGBT, TagCategory.DRAMA
        ),
        TagCategory.GL to setOf(
            TagCategory.ROMANCE, TagCategory.LGBT, TagCategory.DRAMA
        ),
        TagCategory.HAREM to setOf(
            TagCategory.ROMANCE, TagCategory.FANTASY, TagCategory.COMEDY
        ),
        TagCategory.REVERSE_HAREM to setOf(
            TagCategory.ROMANCE, TagCategory.FEMALE_LEAD, TagCategory.FANTASY
        ),
        TagCategory.SLOW_BURN to setOf(
            TagCategory.ROMANCE, TagCategory.DRAMA
        ),

        // Lead type associations
        TagCategory.OP_MC to setOf(
            TagCategory.STRONG_LEAD, TagCategory.ACTION, TagCategory.FANTASY
        ),
        TagCategory.WEAK_TO_STRONG to setOf(
            TagCategory.PROGRESSION, TagCategory.UNDERDOG, TagCategory.COMING_OF_AGE
        ),
        TagCategory.VILLAIN_PROTAGONIST to setOf(
            TagCategory.ANTI_HERO, TagCategory.DARK, TagCategory.MORALLY_GREY
        ),

        // Content clusters
        TagCategory.GRIMDARK to setOf(
            TagCategory.DARK_FANTASY, TagCategory.MATURE, TagCategory.VIOLENCE,
            TagCategory.DARK, TagCategory.TRAGEDY
        ),
        TagCategory.WHOLESOME to setOf(
            TagCategory.FLUFFY, TagCategory.LIGHTHEARTED, TagCategory.SLICE_OF_LIFE
        ),

        // Setting clusters
        TagCategory.ACADEMY to setOf(
            TagCategory.SCHOOL_LIFE, TagCategory.MAGIC, TagCategory.COMING_OF_AGE
        ),
        TagCategory.KINGDOM_BUILDING to setOf(
            TagCategory.POLITICS, TagCategory.STRATEGY, TagCategory.FANTASY
        )
    )

    // ================================================================
    // TAG DISPLAY GROUPS - For UI organization
    // ================================================================

    enum class TagGroup(val displayName: String, val tags: Set<TagCategory>) {
        MAIN_GENRES("Main Genres", setOf(
            TagCategory.ACTION, TagCategory.ADVENTURE, TagCategory.COMEDY,
            TagCategory.DRAMA, TagCategory.FANTASY, TagCategory.HORROR,
            TagCategory.MYSTERY, TagCategory.ROMANCE, TagCategory.SCI_FI,
            TagCategory.SLICE_OF_LIFE, TagCategory.THRILLER, TagCategory.TRAGEDY
        )),

        FANTASY_SUBGENRES("Fantasy Sub-genres", setOf(
            TagCategory.HIGH_FANTASY, TagCategory.LOW_FANTASY, TagCategory.URBAN_FANTASY,
            TagCategory.DARK_FANTASY, TagCategory.EPIC_FANTASY, TagCategory.PORTAL_FANTASY,
            TagCategory.SWORD_AND_SORCERY, TagCategory.MYTHIC_FANTASY
        )),

        SCIFI_SUBGENRES("Sci-Fi Sub-genres", setOf(
            TagCategory.SPACE_OPERA, TagCategory.CYBERPUNK, TagCategory.POST_APOCALYPTIC,
            TagCategory.HARD_SCI_FI, TagCategory.SOFT_SCI_FI, TagCategory.DYSTOPIAN,
            TagCategory.MECHA, TagCategory.FIRST_CONTACT
        )),

        EASTERN("Eastern/Cultivation", setOf(
            TagCategory.CULTIVATION, TagCategory.WUXIA, TagCategory.XIANXIA,
            TagCategory.XUANHUAN, TagCategory.MURIM, TagCategory.MARTIAL_ARTS,
            TagCategory.IMMORTAL, TagCategory.SECT
        )),

        ISEKAI_REINCARNATION("Isekai & Reincarnation", setOf(
            TagCategory.ISEKAI, TagCategory.REINCARNATION, TagCategory.TRANSMIGRATION,
            TagCategory.SUMMONED_HERO, TagCategory.SECOND_CHANCE, TagCategory.REGRESSION,
            TagCategory.TIME_LOOP, TagCategory.TIME_TRAVEL
        )),

        LITRPG_GAMELIT("LitRPG & GameLit", setOf(
            TagCategory.LITRPG, TagCategory.GAMELIT, TagCategory.SYSTEM,
            TagCategory.PROGRESSION, TagCategory.DUNGEON, TagCategory.TOWER,
            TagCategory.VIRTUAL_REALITY
        )),

        ROMANCE_TYPES("Romance Types", setOf(
            TagCategory.SLOW_BURN, TagCategory.ENEMIES_TO_LOVERS, TagCategory.FRIENDS_TO_LOVERS,
            TagCategory.FORBIDDEN_LOVE, TagCategory.ARRANGED_MARRIAGE, TagCategory.HAREM,
            TagCategory.REVERSE_HAREM, TagCategory.POLYAMORY
        )),

        LGBTQ("LGBTQ+", setOf(
            TagCategory.BL, TagCategory.GL, TagCategory.LGBT,
            TagCategory.GENDER_BENDER, TagCategory.NON_BINARY
        )),

        DEMOGRAPHICS("Demographics", setOf(
            TagCategory.SEINEN, TagCategory.SHOUNEN, TagCategory.SHOUJO, TagCategory.JOSEI
        )),

        LEAD_TYPES("Lead/Character Types", setOf(
            TagCategory.MALE_LEAD, TagCategory.FEMALE_LEAD, TagCategory.ANTI_HERO,
            TagCategory.VILLAIN_PROTAGONIST, TagCategory.STRONG_LEAD, TagCategory.OP_MC,
            TagCategory.WEAK_TO_STRONG, TagCategory.SMART_MC, TagCategory.RUTHLESS_MC,
            TagCategory.KIND_MC, TagCategory.MORALLY_GREY
        )),

        SETTING("Setting", setOf(
            TagCategory.HISTORICAL, TagCategory.MEDIEVAL, TagCategory.MODERN,
            TagCategory.URBAN, TagCategory.ACADEMY, TagCategory.SCHOOL_LIFE,
            TagCategory.MILITARY, TagCategory.KINGDOM_BUILDING, TagCategory.SURVIVAL,
            TagCategory.APOCALYPSE
        )),

        CREATURES("Creatures & Races", setOf(
            TagCategory.MONSTERS, TagCategory.DRAGONS, TagCategory.VAMPIRES,
            TagCategory.WEREWOLVES, TagCategory.ZOMBIES, TagCategory.ELVES,
            TagCategory.DEMONS, TagCategory.GODS, TagCategory.SPIRITS
        )),

        THEMES("Themes & Tropes", setOf(
            TagCategory.REVENGE, TagCategory.BETRAYAL, TagCategory.REDEMPTION,
            TagCategory.COMING_OF_AGE, TagCategory.FAMILY, TagCategory.FOUND_FAMILY,
            TagCategory.UNDERDOG, TagCategory.TOURNAMENT, TagCategory.WAR,
            TagCategory.POLITICS, TagCategory.STRATEGY
        )),

        ACTIVITIES("Activities", setOf(
            TagCategory.COOKING, TagCategory.CRAFTING, TagCategory.ALCHEMY,
            TagCategory.PETS, TagCategory.SPORTS, TagCategory.MUSIC, TagCategory.ART
        )),

        MAGIC_SUPERNATURAL("Magic & Supernatural", setOf(
            TagCategory.MAGIC, TagCategory.SUPERNATURAL, TagCategory.WITCHES,
            TagCategory.NECROMANCY, TagCategory.SUMMONING
        )),

        TONE("Tone & Mood", setOf(
            TagCategory.LIGHTHEARTED, TagCategory.GRIMDARK, TagCategory.WHOLESOME,
            TagCategory.FLUFFY, TagCategory.ANGST, TagCategory.BITTERSWEET,
            TagCategory.DARK
        )),

        CONTENT_WARNINGS("Content Warnings", setOf(
            TagCategory.MATURE, TagCategory.ADULT, TagCategory.SMUT,
            TagCategory.ECCHI, TagCategory.GORE, TagCategory.VIOLENCE,
            TagCategory.PSYCHOLOGICAL, TagCategory.TRAUMA
        )),

        NARRATIVE("Narrative Style", setOf(
            TagCategory.FIRST_PERSON, TagCategory.MULTIPLE_POV, TagCategory.NON_HUMAN_MC,
            TagCategory.SECRET_IDENTITY, TagCategory.MISUNDERSTANDINGS, TagCategory.FAN_FICTION,
            TagCategory.SHORT_STORY
        ))
    }

    // ================================================================
    // PUBLIC METHODS
    // ================================================================

    /**
     * Normalize a raw tag string to a canonical TagCategory
     */
    fun normalize(rawTag: String): TagCategory? {
        val cleaned = rawTag.lowercase().trim()
        return tagAliases[cleaned]
    }

    /**
     * Normalize a list of raw tags to canonical categories
     */
    fun normalizeAll(rawTags: List<String>): Set<TagCategory> {
        return rawTags.mapNotNull { normalize(it) }.toSet()
    }

    /**
     * Get the canonical display name for a category
     */
    fun getDisplayName(category: TagCategory): String {
        return when (category) {
            TagCategory.BL -> "Boys Love (BL)"
            TagCategory.GL -> "Girls Love (GL)"
            TagCategory.LGBT -> "LGBTQ+"
            TagCategory.OP_MC -> "OP Main Character"
            TagCategory.SCI_FI -> "Sci-Fi"
            TagCategory.LITRPG -> "LitRPG"
            TagCategory.GAMELIT -> "GameLit"
            else -> category.name
                .replace("_", " ")
                .lowercase()
                .split(" ")
                .joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }
        }
    }

    /**
     * Get related tags for a category
     */
    fun getRelatedTags(category: TagCategory): Set<TagCategory> {
        return relatedTags[category] ?: emptySet()
    }

    /**
     * Get the group a tag belongs to
     */
    fun getTagGroup(category: TagCategory): TagGroup? {
        return TagGroup.entries.find { category in it.tags }
    }

    /**
     * Get all tags organized by group
     */
    fun getAllTagsByGroup(): Map<TagGroup, List<TagCategory>> {
        return TagGroup.entries.associateWith { group ->
            group.tags.sortedBy { getDisplayName(it) }
        }
    }

    /**
     * Calculate tag similarity between two sets (Jaccard index with related tag bonus)
     * Returns value between 0.0 and 1.0
     */
    fun calculateTagSimilarity(
        tags1: Set<TagCategory>,
        tags2: Set<TagCategory>
    ): Float {
        if (tags1.isEmpty() || tags2.isEmpty()) return 0f

        // Direct overlap
        val intersection = tags1.intersect(tags2)
        val union = tags1.union(tags2)

        val jaccardScore = intersection.size.toFloat() / union.size.toFloat()

        // Boost for related tag matches
        var relatedBoost = 0f
        for (tag1 in tags1) {
            val related = getRelatedTags(tag1)
            val relatedMatches = related.intersect(tags2)
            relatedBoost += relatedMatches.size * 0.25f
        }

        // Normalize boost
        relatedBoost = (relatedBoost / tags1.size.coerceAtLeast(1)).coerceAtMost(0.35f)

        return (jaccardScore + relatedBoost).coerceAtMost(1f)
    }

    /**
     * Check if tags contain mature/adult content
     */
    fun hasMatureContent(tags: Set<TagCategory>): Boolean {
        return tags.any { it in setOf(
            TagCategory.MATURE, TagCategory.ADULT, TagCategory.SMUT,
            TagCategory.ECCHI, TagCategory.GORE
        )}
    }

    /**
     * Check if tags indicate LGBTQ+ content
     */
    fun hasLGBTContent(tags: Set<TagCategory>): Boolean {
        return tags.any { it in setOf(
            TagCategory.BL, TagCategory.GL, TagCategory.LGBT,
            TagCategory.GENDER_BENDER, TagCategory.NON_BINARY
        )}
    }
}