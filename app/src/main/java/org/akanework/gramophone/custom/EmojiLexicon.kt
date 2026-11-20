// Kindgerechte, rein lokale Emoji-Erkennung für sichtbare Musik-Metadaten.
// Keine Netzwerkzugriffe und keine Änderungen an Titeln, Dateien oder der Musikdatenbank.
package org.akanework.gramophone.custom

import java.util.Locale

/**
 * Ergänzung zum ursprünglichen EmojiDisplay-Wörterbuch.
 *
 * Reihenfolge: mehrwortige Ausdrücke > vollständige Wörter > gebeugte Formen >
 * vollständig belegte Komposita. Unbekannte Wörter bleiben unverändert.
 * Keine beliebige Teilstring-Ersetzung ("Maus" ändert "Mausoleum" nicht).
 */
internal object EmojiLexicon {
    private val german = Locale.GERMAN
    private val wordPattern = Regex("""\p{L}+""")
    private val wordLikePattern = Regex("""^[\p{L}]+$""")
    private const val MAX_CACHE = 512
    private const val MAX_COMPOUND_PARTS = 4
    private const val MIN_COMPOUND_LENGTH = 7

    // Begrenzter Cache: Auch lange Musikbibliotheken halten dadurch keinen unbegrenzten Textspeicher vor.
    private val cache = object : LinkedHashMap<String, String>(MAX_CACHE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > MAX_CACHE
    }

    private val allWords: Map<String, String> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        buildMap {
            putAll(compoundWords)
            putAll(parseRows(ADJECTIVES).apply { generateAdjectiveForms() })
            putAll(parseRows(VERBS).apply { generateVerbForms() })
            putAll(parseRows(IRREGULAR))
        }
    }

    // Only nouns and the legacy dictionary should be combined inside compounds.
    private val compoundWords: Map<String, String> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        parseRows(NOUNS).apply { generateNounForms() }
    }

    private val phraseRules: List<Pair<Regex, String>> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PHRASES.lineSequence().mapNotNull { raw ->
            val trimmed = raw.trim()
            if (trimmed.isEmpty() || '|' !in trimmed) return@mapNotNull null
            val (emoji, phrase) = trimmed.split('|', limit = 2)
            val expression = phrase.trim().split(Regex("""\s+""")).joinToString("""\s+""") {
                Regex.escape(it)
            }
            // Only whole letters; no accidental match inside another word.
            Regex("""(?<!\p{L})${expression}(?!\p{L})""", RegexOption.IGNORE_CASE) to emoji
        }.sortedByDescending { it.first.pattern.length }.toList()
    }

    // Avoid memory leaks by caching only raw title strings and their transformed values.
    @Synchronized
    fun convertText(input: String, originalWords: Map<String, String>): String {
        if (input.isEmpty()) return input
        cache[input]?.let { return it }
        var result = input
        // Phrase replacements before single words. Resulting emoji never matches the letter regex.
        for ((pattern, emoji) in phraseRules) result = result.replace(pattern, emoji)
        result = wordPattern.replace(result) { match ->
            val lower = match.value.lowercase(german)
            originalWords[lower]
                ?: allWords[lower]
                ?: splitCompound(lower, originalWords)
                ?: match.value
        }
        cache[input] = result
        return result
    }

    private fun parseRows(source: String): MutableMap<String, String> {
        val words = LinkedHashMap<String, String>()
        for (row in source.lineSequence()) {
            val text = row.trim()
            if (text.isEmpty() || text.startsWith("#") || '|' !in text) continue
            val (emoji, aliases) = text.split('|', limit = 2)
            if (emoji.isBlank()) continue
            aliases.trim().split(Regex("""\s+""")).forEach { alias ->
                val token = alias.lowercase(german)
                if (wordLikePattern.matches(token)) words.putIfAbsent(token, emoji)
            }
        }
        return words
    }

    private fun MutableMap<String, String>.generateNounForms() {
        // Conservative: no global -en/-er blanket rules. That could turn Eis -> Eisen.
        for ((word, emoji) in toMap()) {
            if (word.length < 4) continue
            when {
                word.endsWith('e') && word.length >= 5 -> putIfAbsent(word + "n", emoji)
                word.endsWith('o') || word.endsWith('a') || word.endsWith('i') -> putIfAbsent(word + "s", emoji)
                word.last() !in "sxzß" && !word.endsWith("en") && !word.endsWith("er") -> {
                    if (word.length >= 6) putIfAbsent(word + "s", emoji)
                }
            }
        }
    }

    private fun MutableMap<String, String>.generateAdjectiveForms() {
        for ((word, emoji) in toMap()) {
            if (word.length < 4 || word.endsWith("en") || word.endsWith("es")) continue
            for (suffix in listOf("e", "en", "em", "er", "es")) putIfAbsent(word + suffix, emoji)
            // For adjectives like "dunkel": dunkle, dunklen; "teuer": teure, teuren.
            if (word.endsWith("el") || word.endsWith("er")) {
                val short = word.dropLast(1)
                for (suffix in listOf("e", "en", "em", "er", "es")) putIfAbsent(short + suffix, emoji)
            }
        }
    }

    private fun MutableMap<String, String>.generateVerbForms() {
        val irregularVerbs = setOf("singen", "fliegen", "rennen", "schlafen", "schwimmen",
            "lesen", "sehen", "laufen", "fahren", "geben", "essen", "trinken", "wachsen",
            "waschen", "backen", "stehen", "gehen", "bleiben", "springen")
        for ((infinitive, emoji) in toMap()) {
            if (infinitive.length < 5 || !infinitive.endsWith("en")) continue
            val stem = infinitive.dropLast(2)
            if (stem.length < 3) continue
            for (suffix in listOf("e", "st", "t", "en", "te", "test", "tet", "ten")) {
                putIfAbsent(stem + suffix, emoji)
            }
            // Present participles (hüpfend, hüpfenden, spielende etc.).
            for (suffix in listOf("end", "ende", "enden", "endem", "ender", "endes")) {
                putIfAbsent(stem + suffix, emoji)
            }
            // Past participle for regular verbs: gespielt, gemalt, getanzt.
            if (stem.length >= 4 && infinitive !in irregularVerbs) {
                putIfAbsent("ge" + stem + "t", emoji)
            }
        }
    }

    internal fun knownFormCount(): Int = allWords.size
    internal fun compoundFormCount(): Int = compoundWords.size

    private fun splitCompound(word: String, originalWords: Map<String, String>): String? {
        if (word.length < MIN_COMPOUND_LENGTH || word.length > 48) return null
        // Bound search to meaningful segments. Prefer longer lexemes and fewer pieces.
        // Look up maps directly rather than copying thousands of entries per title.
        data class Candidate(val emojis: List<String>, val lengths: List<Int>)
        val memo = HashMap<Pair<Int, Int>, Candidate?>()
        fun find(offset: Int, segmentsLeft: Int): Candidate? {
            if (offset == word.length) return Candidate(emptyList(), emptyList())
            if (segmentsLeft == 0) return null
            val key = offset to segmentsLeft
            if (memo.containsKey(key)) return memo[key]
            var best: Candidate? = null
            val minEnd = offset + 3
            if (minEnd <= word.length) {
                for (end in word.length downTo minEnd) {
                    val piece = word.substring(offset, end)
                    val emoji = originalWords[piece] ?: compoundWords[piece] ?: continue
                    val remaining = if (end == word.length) listOf(0) else listOf(0, 1, 2, 3)
                    for (joinLength in remaining) {
                        if (end + joinLength > word.length || (joinLength > 0 && end + joinLength == word.length)) continue
                        val join = word.substring(end, end + joinLength)
                        if (joinLength > 0 && join !in setOf("s", "n", "e", "er", "es", "en")) continue
                        val suffix = find(end + joinLength, segmentsLeft - 1) ?: continue
                        // A compound must contain at least 2 meaningful mapped parts.
                        val choice = Candidate(listOf(emoji) + suffix.emojis, listOf(piece.length) + suffix.lengths)
                        if (best == null || choice.emojis.size < best.emojis.size ||
                            (choice.emojis.size == best.emojis.size &&
                                choice.lengths.joinToString("") { it.toString().padStart(2, '0') } >
                                best.lengths.joinToString("") { it.toString().padStart(2, '0') })) {
                            best = choice
                        }
                    }
                }
            }
            memo[key] = best
            return best
        }
        val parts = find(0, MAX_COMPOUND_PARTS) ?: return null
        if (parts.emojis.size < 2 || parts.emojis.size > MAX_COMPOUND_PARTS) return null
        return parts.emojis.joinToString(" ")
    }

    // Format: emoji|singular plural synonyms variants. New vocabulary can be extended by adding rows.
    private val NOUNS = """
☀️|wetter wetterlage wetterbericht wetterwechsel wetterbild wetterzustand
🤝|bande gruppen gruppe gemeinschaft gemeinschaften zusammenhalt
🏷️|name namen benennung bezeichnung spitzname spitznamen
🪽|flug flüge flugbewegung flugkunst flugbahn
👼|engel engelchen engelskind englein schutzengel weihnachtsengel
💤|sandmann sandmännchen sandmaennchen schlafsand sandmännchenlied
🍼|schnuller nuckel nuckelchen nucki windel windeln windelchen
🧸|puppe puppen puppenkind puppenbaby puppenstube puppenhaus
🧵|faden nähgarn garn stickgarn nähfaden
🎼|komponist komponistin komponieren musikant musikantin
☔|regenschutz regenschirme regenmantel
❄️|schneeballschlacht schneeballwerfen
🪺|nest nester vogelnest nestchen nestbau
🥚|brüten bebrüten ausgebrütet
🚜|ackerbau feldarbeit feldarbeiten
👂|lauscher lauschen hinhören hören
🧠|wissen lernen gelernt nachdenken
👤|kopf köpfe köpfchen köpflein schädel köpchen
👆|finger zeigefinger zeigefingerchen fingerchen fingerlein fingerkuppen
👍|daumen däumchen daumenhoch
💪|arm arme ärmchen oberarm unterarm armchen
🦵|bein beine beinchen knie knien kniechen wade waden kniescheibe
👣|fußspur fußspuren fußabdruck fußabdrücke schritte schritt fußstapfen
👋|handgelenk händedruck kinderhand kinderhände kinderhändchen
👅|zunge zünglein zungen züngchen
👄|mundwinkel mündlein lippenpaar
👃|nasenloch nasenlöcher niesenase
👀|wimper wimpern augenbraue augenbrauen augenlid augenlider
🦷|backenzahn schneidezahn zahnfeezaehnchen zahnlücke zahnluecke
🧒|bruder brüder schwester schwestern geschwister geschwisterkind geschwisterchen
🧔|mann männer männelein männlein männchen herr herren
👩|frau frauen dame damen fräulein
👪|elternteil elternpaar elternhaus elternliebe
💍|hochzeit heirat braut bräutigam heiraten
👟|schritt schritte schrittchen trippeln schuhpaar
🎵|refrain strophe strophen gesangsstrophe liedern kinderreim kinderlied kinderreime
🗣️|stimme stimmen stimmchen stimmelein ruf rufe
🪘|bongos bongotrommel tamburin rhythmus taktgefühl
🎻|kontrabass bassgeige streichinstrument geigenklang
🎺|posaune posaunen tuba tuben hornmusik
📯|tröte tröten luftrüssel
🎶|lalala tralala la-la-la trallala tirlili
🐮|muh muuh muhhh muhen
🐱|miau miauu miauuu miep schnurr schnurren
🐶|wau wuff wauwauwau bell bellbell
🐸|quak quaken quakquak ribbit
🐓|kikeriki kikerikiiii krähen
🐦|piep piepiep zwitscher zwitschern tschip tschilp
🚒|tatütata tatü-tata tatütatata tatüta
🔔|dingdong ding-dong klingeling klingelingeling bimmel bimmeln
💦|plitsch platsch platschen spritzen spritzwasser spritzig
💧|tropf tropft tropftropf plipp plopp
🐾|pfötchen pfötlein tatzenabdruck tatzenabdrücke
⚽|ball bälle bällchen spielball gummiball fußballball
🎈|ballönchen luftballönchen ballonfahrt ballonschnur ballonblume
🪁|flugdrachenschnur flugdrachenspiel drachenschnur
⚔️|ritter ritterlein ritterchen ritterhelm ritterschwert ritterrüstungen ritterrüstung
🛡️|schild schilde schutzschild kampfschild
🏹|bogen pfeil pfeile pfeilbogen pfeilundbogen
🏴‍☠️|räuber räuberbande seeräuberbande räuberhöhle räuberhut
🦸|heldentum heldinnen heldenkind
🧚|wunderfee märchenfee zauberfee
👑|königssohn königstochter königsfamilie königskinder
🌌|zauberland märchenwelt fantasiewelt phantasiewelt traumwelt
🪄|zauberspruch zaubersprüche zaubertrick zaubertricks zauberlicht
🧭|abenteuer abenteurer abenteuerin erkundung entdeckung
🎪|jongleur jongleure akrobat akrobaten akrobatin manege
🤸|turnen turnerin turner rolle purzelbaum kopfstand handstand
🧘|yoga entspannung ausatmen einatmen atmen ruhezeit
🛌|schlafenzeit schlafenszeit schlafstunde bettzeit nachtzeit
🕺|walzer walzertanz polka disco diskotanz
🕹️|spielen spiel spielchen spielerei kinderspiel mitspielen
🤹|jonglieren jongliert jonglierball jonglierbälle
⛸️|schlittschuh schlittschuhe eislaufen eisläufer eisläuferin
🛷|schlitten schlittenfahrt rodeln rodelbahn
⛷️|ski skifahren skifahrer skipiste
☃️|schneemannbauen schneemannbau wintermann schneekamerad
🧣|winterkleidung wintermantel warmeschals
🌅|morgens morgenstunde frühmorgens sonnenmorgen frühlingmorgen
☀️|mittag mittags mittagssonne
🌇|abends spätabend abendstunde
🌙|nachts mondnacht mondnächte
🕰️|stunde stunden minute minuten sekunde sekunden minutenzeiger stundenzeiger
📆|heute gestern morgen übermorgen vorgestern
🎃|herbst herbstzeit herbstlaub herbstblätter herbstwind
🌷|frühling fruehling frühlingszeit frühlingsblüte frühlingswiese
☀️|sommer sommerzeit sommerferien sommerwind sommersonne
❄️|winter winterzeit winterferien wintertag winterkälte
🐰|ostern ostertag osterfest osterkörbchen osterkörb
🕯️|advent adventszeit adventskranz adventskerze adventskalender
🎄|christfest heiligabend weihnachtstag weihnachtsabend
🎆|silvester neujahr feuerwerk knaller böller
🎉|fest feste feiern feiernlernen kinderfest familienfest schulfeier
🎂|geburtstage geburtstagskinder geburtstagsparty geburtstagsgeschenk
🍦|eisportion eiswaffel eisstiel
🧊|gletscher eissee eiswasser vereisung frostwetter
🛤️|schienen schiene bahngleis bahngleise gleis gleise
🚉|bahnhof bahnhöfe haltestelle bushaltestelle bahnsteig
🚍|busfahrt busreisen busreise autobus
🧩|puzzlezeit puzzlebildchen puzzleparty
🌲|walde wäldern waldweg waldwege waldtiere waldlied
🐾|tiergeräusche tierstimmen tierfüße tierpfoten
👁️|gucken hinsehen zuschauen beobachten
🤝|helfen hilfe helfer hilfsbereit
🧶|wolle wollknäuel faden fäden schnur schnüre
🪡|nadel nadeln nähen nähnadel
🪢|knoten knotenband knotenstrick
🎀|schleife schleifen haarschleife zierschleife
👒|sommermütze sonnenkappe sommerhut
🩹|pflaster verband wundpflaster verbandkasten
💊|medizin medikament arznei arzneimittel
🦠|mikrobe mikroben keimchen bakterie bakterien virus viren
🧽|wischen abwischen putzlappen putzschwamm
🧴|creme lotion sonnencreme sonnenmilch hautcreme
🌻|sommerblume sommerblumen
💛|golden goldgelb goldfarbe goldfarben
💜|lila dunkelviolett violettfarben
🩵|hellblau himmelblau himmelsfarben
🩷|rosarot rosafarben rosahell
🕳️|loch löcher lochlein
🪣|eimer eimerchen wassereimer sandeeimer
🏖️|sand sandkasten sandkastenspiel sandkiste sandförmchen sandform
⛲|brunnen springbrunnen wasserfontäne wasserfontaene
🛶|boot boote böötchen bootchen ruderkahn
💡|idee einfall gedanke gedankenblitz
🧭|weg wege pfad pfade strecke reisen reise wanderung
🎒|reisegepäck gepäck kindergepäck reisetasche
👀|versteck versteckspiel versteckenfangen
🧁|nascherei naschen naschkatze naschkram
🍬|bonbonpapier zuckerl süßigkeit süssigkeit zuckerle
🥁|taktmusik musikrhythmus trommelschlag
🎶|musikstückchen musikunterricht musikgruppe
🐱|kätzlein katzenkind katzenbaby mieze miezekatze stubentiger schnurrer schnurrkatze kitten miez miezchen hauskatze tigerkatze wildkatze katzenmama katzenpapa katzenfell
🐶|wauwau wuffi wauwi hundi hundchen hundebaby hundewelpe welpenhund köter vierbeiner dackel pudel schäferhund bulldogge labrador beagle husky collie retriever hundegebell bellen
🐭|mäuslein mäusekind feldmaus wühlmaus hausmaus haselmaus rennmaus spitzmaus mauseloch mausfamilie
🐻|braunbär eisbär grizzly teddybaer kuschelbär bärlein bärenkind bärenfamilie bärenhöhle
🧸|plüschtier stofftier plüschbär kuschelbärchen schmusetier stoffbär plüschhund plüschhase plüschpuppe
🐰|hoppelhase hoppelhäschen hoppelkaninchen hoppel hoppelchen bunny hasenbaby hasenmama hasenpapa feldhase zwergkaninchen
🐹|hamster goldhamster zwerghamster hamsterchen meerschweinchen meerschwein meerschweinchenbaby
🐀|ratte ratten rattenkind rattenfamilie
🐿️|hörnchen eichkatze streifenhörnchen backenhörnchen eichhörnchenbaby nagerhörnchen
🦊|füchslein fuchsbaby fuchskind rotfuchs polarfüchse polarfuchs fuchshöhle
🐺|wolfskind wölfchen wolfshund wolfsrudel wolfsbaby
🦁|löwenbaby löwenjunge löwenmähne löwin löwinnen löwenkönig löwenkinder
🐯|tigermama tigerbaby tigerin tigermädchen tigerchen königstiger bengaltiger
🐆|leopard leoparden gepard geparden jaguar jaguare panther panthern
🐴|gaul ross stute hengst fohlen fohlenkind fohlenbaby pferdchen schimmel ponyfohlen ponymädchen reitpferd reitpony
🦓|zebrafohlen zebrahengst zebrastute zebrakind
🦄|einhörnchen einhornbaby einhornfohlen einhornland einhornzauber unicorn
🐮|rind rinder rindvieh rindviecher kuhbaby kuhlein kälbchenbande bullenbulle bullen ochse ochsen kälbchen
🐷|schweinchen säulein ferkelchen sau säue hausschwein wildschwein schweinchenbaby schweinekind sparschwein
🐑|schäfchen schaflein wollschaf schafbaby schafskind wollschafe schafherde ziegenlamm
🐐|zieglein ziegenbock geiß geißen zicklein zickchen ziegenbaby
🐔|hühnchen hähnchen hähnlein gockel gockelhahn huhnchen hühnerküken hühnerschar
🐣|kueken küklein kükenbaby kükenkind eierküken schlüpfling
🦆|entlein entenbaby entenküken entenfamilie erpel stockente entenmann entenmama
🪿|gans gänse gänschen gänselein gänseküken wildgans hausgans
🦃|truthahn truthenne puter pute puten
🦢|schwänlein schwanenkind schwanenbaby schwanenküken
🦉|uhu käuzchen kauz schneeeule waldeule schleiereule eulenkind
🐦|vögelchen spatz spatzen sperling sperlinge amsel amseln rotkehlchen meise meisen fink finken
🕊️|taube tauben täubchen friedenstaube brieftaube
🐦‍⬛|rabe raben krähe krähen kolkrabe rabenbaby
🦅|falke falken bussard habicht geier seeadler steinadler
🦜|ara aras wellensittich sittich sitttich kakadu kakadus vogelpapagei sprechvogel
🐧|pingi pingus pinguinbaby königspinguin kaiserpinguin
🦚|pfau pfauen pfauenrad pfauenfeder
🦩|flamingobaby rosaflamingo
🦤|dodo dodovogel
🐟|fischchen fischlein fischbaby forelle forellen karpfen hecht hechte hering heringe barsch barsche lachs lachse
🐠|aquariumfisch tropenfisch clownfisch nemo zierfisch korallenfisch goldfischchen
🐡|kugelfisch kugelfische stachelfisch igelfisch
🦈|haifisch haifische hammerhai weißehai haiweibchen
🐬|delphin delphine delfinbaby delphinbaby tursiops
🐳|walfisch walfische walfamilie walbaby blaufisch blauwal pottwal orca killerwal schwertwal
🦭|seehund seehunde seehündchen seelöwe seelöwen robbenbaby
🐙|tintenfisch tintenfische krakenarm achtarm oktopusse octopus
🦑|kalmar kalmare kalmarbaby tintenschnecke
🪼|qualle quallen feuerqualle meduse medusen
🦀|krebs krebse krebschen strandkrabbe taschenkrebs
🦞|languste langusten flusskrebs flusskrebse
🦐|garnele garnelen shrimp schrimps shrimps
🐚|muschel muscheln schneckenhaus meeresmuschel perle perlmuschel
🪸|koralle korallen korallenriff riff
🐢|schildkroete landchildkröte landschildkröte meeresschildkröte tortoise
🐸|fröschlein froschlein froschkönig froschprinz froschbaby kaulquappe kaulquappen
🐍|python pythons boa boas kobra kobras natter nattern ringelnatter viper vipern
🦎|gecko geckos leguan leguane waran warane chamäleon chamäleons
🐊|krokobaby krokokind alligator alligatoren kaiman kaimane
🦕|saurier dinosaurus brontosaurus diplodocus brachiosaurus pflanzenfresser langhalssaurier
🦖|trex t-rex tyrannosaurus velociraptor raptor raptoren raubsaurier
🦣|mammut mammuts mammutbaby urzeittier
🦧|orangutan orang-utan orangutans menschenaffe menschenaffen
🦍|bergorilla gorillababy silberrücken
🐒|äfflein affenbaby schimpanse schimpansen bonobo bonobos makake makaken kapuzineraffe kapuzineraffen
🦥|faultier faultiere faultierbaby
🦦|otter ottern seeotter fischotter
🦨|stinktier stinktiere skunk
🦔|igelchen stacheligel igelbaby igelkind
🦇|fledermäuschen fledermausbaby vampirfledermaus
🐝|bienchen hummel hummeln honigbiene bienenkönigin bienenstock bienenvolk
🦋|falter schmetterlingsbaby schwalbenschwanz pfauenauge zitronenfalter bläuling
🐞|marienkäferchen glückskäfer siebenpunkt käferchen maikäfer rosenkäfer
🐜|ameislein ameisenhaufen ameisenvolk ameisenkönigin
🪲|mistkäfer hirschkäfer skarabäus borkenkäfer käferlein
🐌|schneckchen weinbergschnecke nacktschnecke schneckenbaby
🪱|wurm würmer würmchen regenwurm regenwürmer
🦟|stechmücke moskito moskitos
🪰|fliege fliegen stubenfliege obstfliege fruchtfliege
🕷️|spinnchen spinnenkind webspinne kreuzspinne vogelspinne
🦂|skorpion skorpione
🦗|grille grillen heuschrecke heuschrecken grashüpfer grasshopper
🪳|kakerlake kakerlaken schabe schaben
🐘|elefäntchen elefantenbaby elefantenkind elefantin elefantenherde rüsseltier rüssel rüsselchen
🦒|giraffenbaby giraffenkind giraffenhals
🦏|rhino rhinoceros nashornbaby
🦛|hippopotamus flusspferd flusspferde nilpferdbaby
🐪|kamel kamele dromedar dromedare
🦙|lama lamas alpaka alpakas
🦘|kanguru kanguruh kängurubaby kängurukind beuteltier
🐼|pandabär pandabären pandababy großerpanda rotpanda kleinerpanda
🦫|biber biberbau biberbaby biberschwanz
🦬|bison bisons wisent wisente büffel büffelherde
🦌|hirsch hirsche reh rehe rehkitz rehkitzchen rentier rentiere elch elche damhirsch
🦃|pfauhenne
🐦|schwalbe schwalben specht spechte kolibri kolibris nachtigall nachtigallen
🌳|eiche eichen buche buchen kastanie kastanien ahorn ahorne obstbaum apfelbaum birnbaum baumkrone baumstamm baumhaus wipfel bäumchen
🌲|fichte fichten tannenwald nadelbaum nadelbäume kiefer kiefern forst wäldchen tannengrün tannenwald holzstamm baumstumpf
🌴|kokospalme palmenstrand palmeninsel palmenhain
🌱|pflanze pflanzen keim keime keimling keimlinge spross sprössling samenkorn saat saatgut sämling
🌿|kraut kräuter kräutergarten basilikum petersilie dill rosmarin thymian
🍀|kleeblatt kleeblätter vierblatt vierblättrig glücksbringer
🍃|laub blättchen blattwerk rasen grashalm grashalme grass grünzeug kleeblättchen
🌾|getreide weizen roggen hafer gerste reiskorn reisähre korn ähre ähren acker
🌷|tülpchen tulpenblüte krokus krokusse narzisse narzissen osterglocke osterglocken hyazinthe hyazinthen
🌻|sonnenblümchen sonnenblüte sonnenblumenkern sonnenblumensamen
🌹|röslein rosenblüte dornenrose rosenstrauch blütenrose
🌸|blüte blüten blümlein blütenblatt blütenblätter kirschblüte kirschblüten rosablüte
🌼|margerite margeriten gänseblümchen gänseblume butterblume butterblumen löwenzahn pusteblume pusteblumen
💐|blumenstrauß blumenstrauss blumengruß blumengesteck blumenbouquet
🌺|hibiskus hibiskusblüte tropenblume
🌵|kaktus kakteen kaktusblüte dornpflanze
🍄|fliegenpilz champignon champignons steinpilz steinpilze pfifferling pfifferlinge pilzchen
🍎|äpfelchen apfelchen äpflein apfelstück apfelmus apfelsaft obstapfel
🍐|birnchen birnlein
🍊|clementine clementinen klementine klementinen mandarindose satsuma
🍋|limone limonen zitronensaft saurefrucht
🍌|banänchen bananenfrucht bananenmus bananenschale
🍉|wassermelonenstück melonenstück zuckermelone honigmelone cantaloupe
🍇|weinbeere weinbeeren rebe reben traubensaft traubenrispe
🍓|erdbeer erdbeerchen erdbeermarmelade walderdbeere erdbeerstück
🫐|blaubeere blaubeeren heidelbeere heidelbeeren blaubeer heidelbeer waldbeere waldbeeren
🍒|kirschlein kirschbaum kirschtorte kirschsaft
🍑|nektarine nektarinen pflaume pflaumen zwetschge zwetschgen
🍍|ananas ananasse ananasfrucht
🥝|kiwi kiwis kiwifrucht
🥭|mango mangos mangofrucht
🥑|avocado avocados guacamole
🍅|tomätchen cherrytomate kirschtomate cocktailtomate
🥕|mörchen moehre moehrchen karottchen rübe rüben möhrchen
🥒|gurke gurken salatgurke sauregurke essiggurke
🥬|salat blattsalat kopfsalat eisbergsalat grünkohl spinat mangold
🥦|broccoli brokkoliröschen broccoli-röschen
🥔|kartöffelchen kartoffelbrei stampfkartoffel kartoffelpüree erdapfel erdäpfel
🧅|zwiebel zwiebeln zwiebelring
🧄|knoblauch knoblauchzehe
🌽|maiskolben maiskörner popkorn zuckermais
🫑|paprika paprikaschote peperoni gemüsepaprika
🌶️|chili chilli chilischote pfefferschote scharfmacher
🍞|toast toastbrot brotscheibe brotlaib vollkornbrot brotkruste brotteig bäckerbrot
🥖|baguette weißbrot weissbrot stokkbrot stockbrot
🥨|brezn laugenbrezel laugengebäck
🥐|croissant hörnchen butterhörnchen gipfeli
🥞|pfannkuchen eierkuchen pancake pancakes plinse plinsen crêpe crepe
🧇|waffel waffeln waffelherz
🥯|bagel bagels
🧀|käsewürfel käsebrot käsescheibe gouda cheddar camembert mozarella mozzarella
🥚|eierchen spiegelei rührei frühstücksei hühnerei eierschale
🥛|milchglas milchflasche trinkmilch kakao milchshake trinkkakao
🍼|fläschchen babyflasche milchfläschchen nuckelflasche trinkflasche
🍯|honigtopf bienenhonig honigglas
🍪|plätzchen plätzlein kekschen butterkeks butterkekse lebkuchen spekulatius kekskrümel
🍰|kuchenstück käsekuchen apfelkuchen marmorkuchen sandkuchen blechkuchen
🎂|geburtstagstorte geburtstagsfeier kindergeburtstag geburtstagskerze geburtstagslied birthday geburtstagskind
🧁|cupcake cupcakes törtchen minikuchen
🍩|donut donuts berliner pfannkuchenberliner krapfen kräppel
🍫|schokoriegel schokoladenriegel schokokuchen schokostück schokostückchen schokoladentafel schokoladenkeks praline pralinen
🍬|zuckerbonbon kaugummi kaugummis karamell karamelle lakritz fruchtgummi gummibärchen gummibaerchen gummibären
🍭|zuckerstange lolli lolly schlecker
🍦|eiswaffel eisbecher eiscremebecher vanilleeis schokoeis erdbeereis speiseeis gelato
🍨|sorbet eisportion eiskugel eiskugeln
🍧|wassereis slush slusheis
🍕|pizzastück pizzateig pizzabrot pizzabelag salami-pizza
🍔|hamburgerbrötchen cheeseburger burgerbrötchen
🌭|hotdog hotdogs würstchen bratwurst würste wurst wienerwurst
🥪|sandwich sandwiches stulle butterbrot belegtesbrot pausenbrot
🍟|fritten kartoffelstäbchen pommesfrites
🥗|gemüsesalat obstsalat salatschüssel salatblatt
🍲|eintopf suppentopf hühnersuppe gemüsesuppe kartoffelsuppe nudelsuppe suppe suppen brühe
🍝|spagetti spaghetti bolognese nudel pasta penne macaroni makkaroni
🍚|reis reiskörner reisbrei milchreis
🍿|popcornbecher puffmais
🧃|saftpäckchen trinkpäckchen fruchtsaft apfelsaftpackung orangensaft trinkbox saftkarton
🫖|teekanne kanne teeservice
☕|tee kaffee kakaoheißgetränk heiße-schokolade
🥤|becher trinkbecher limonade limo brause getränk getränke
🍽️|essen mahlzeit frühstück mittagessen abendessen abendbrot speise speisen teller speiseteller
🥄|löffel teelöffel suppenlöffel esslöffel
🍴|gabel gabeln messer besteck
🧂|salz pfeffer gewürz gewürze
🚗|pkw automobil kraftfahrzeug fahrzeug fahrzeuge flitzer rennflitzer autolein autobaby familienwagen kleinwagen
🚙|geländewagen jeep suv offroader allradwagen
🏎️|rennauto rennwagen formelauto formelwagen sportauto rennbolide rennmaschine
🚕|taxe taxen taxifahrer taxiwagen
🚓|streifenwagen polizeiwagen polizeifahrzeug polizeibus
🚑|sanitätswagen sanitäterwagen notarztwagen rettungsfahrzeug notfallwagen
🚒|löschwagen löschfahrzeug löschauto feuerwehrwagen feuerwehrautos feuerwehrfahrzeug feuerwehrstation feuerwehrhaus
🚚|laster transporter transportwagen lieferwagen paketauto möbelwagen umzugslaster
🚛|sattelschlepper sattelzug anhängerzug truck fernlaster
🚜|trecker traktörchen ackerschlepper landmaschine feldtraktor bauerntraktor
🚲|radl velo drahtesel zweirad kinderfahrrad laufrad kinderbike
🛴|scooter tretfahrzeug kickscooter elektroscooter e-scooter
🛵|moped mopeds motorroller vespa motorfahrrad
🏍️|motorräder motorradfahrer motorradrennen
🚌|omnibus omnibusse doppelstockbus linienbus schulbusse busfahrer
🚎|oberleitungsbus trolley
🚐|kleinbus minibuss reisemobil campervan
🚂|dampflok dampflokomotive lokführer eisenbahnzug dampfeisenbahn lokomotivchen
🚆|bahn bahnen regionalbahn fernzug personenzug güterzug zugfahrt
🚊|tram tramway straßenbahnen stadtbahn
🚝|schwebebahn monorail einschienenbahn
🚇|u-bahn untergrundbahn metro subway
🚈|s-bahn schnellbahn pendlerzug
🚃|waggon wagon waggons eisenbahnwagen zugabteil
🚄|schnellzug hochgeschwindigkeitszug ice expresszug
🚋|tramwagen bahnwagen
✈️|fliegerchen flugmaschine flugzeugchen jet düsenjet flugreise flugreisen
🛩️|propellerflugzeug propellermaschine sportflugzeug segelflugzeug
🚁|heli helis helikopterchen drehflügler rettungshubschrauber
🚀|raumschiff weltraumrakete mondrakete raketchen raumfähre
🛸|ufoschiff fliegendeuntertasse untertasse außerirdischeraumschiff
🚢|schiffchen frachter dampfer kreuzfahrtschiff fähre fähren bootsschiff
⛵|segel segler segelschiff jolle yacht yachtboot
🚤|speedboot motorbötchen rennboot
🛶|paddelboot ruderboot kajak kajaks kanadier pferdekanu
🚣|rudern ruderer paddeln paddel paddler
🚠|seilbahn seilbahnen gondelbahn berggondel
🚡|gondel gondeln gondelbahnwagen
🛹|skateboard skateboarden skaten rollbrett
🛼|rollschuh rollschuhe rollerskates inlineskates inline
🚦|verkehrsampel stopplicht rotlicht grüneslicht ampelanlage
🛑|stoppschild halteschild stoppsignal
🛞|reifen autoreifen fahrradreifen radreifen räder radlreifen
⛽|tankstelle tankstellen tanken kraftstoff benzin diesel
🚧|baustelle baustellen absperrung baustellenabsperrung
🛣️|straße strasse straßen strassen wegstrecke landstraße autobahn fahrbahn
🌉|brücke brücken steinbrücke holzbrücke
🏡|häuschen heimathaus bauernhaus landhaus landhäuschen
🏠|wohnhaus gebäude wohnung wohnungen heim zuhaus zuhause zu-hause hüttchen hütte hütten
🏘️|dorf dörfer siedlung siedlungen nachbarschaft ort dorfhaus
🏙️|stadt städte großstadt metropole innenstadt
🏰|ritterburg märchenschloss schlossburg schlösser schloß schlösserburg
🏯|festung wehrburg zitadelle burgfestung
🏟️|stadion sportstadion fußballstadion
🏫|kindergarten kita kindertagesstätte schulkinder schulhaus grundschule vorschule klassenzimmer schulklasse
🏥|klinik arztpraxis krankenstation notaufnahme heilzentrum
🏪|laden geschäft kaufmannsladen supermarkt supermarktregal shop kiosk
🏬|kaufhaus einkaufszentrum shoppingcenter
🏭|fabrik werk werkhalle produktionshalle
⛺|zeltchen zeltlager zeltplatz zeltwiese campingplatz
🏕️|camp campen campinglager ferienlager feriencamp
🛏️|schlafzimmer kinderbett bettchen bettlaken matratze kinderzimmerbett babybett gitterbett
🛋️|sofa couch sessel wohnzimmersofa
🪑|sitz sitzplatz sitzmöbel hocker hochstuhl kinderhochstuhl
🛁|bad badewannen badezimmer badestube planschen planschbecken badebecken
🚿|duschkopf duschbad duschraum
🚽|toilette wc klo klohäuschen töpfchen topf pipi toilettegang
🧻|klopapier toilettenpapier papierrolle
🪥|zähneputzen zahnputzen zahnputzbecher zahnpasta zahnbürstchen
🪟|fensterchen fensterscheibe fensterbank
🚪|türchen haustür zimmertür türrahmen
🔑|schlüssel schlüsselbund hausschlüssel schlüsselloch
🪞|spiegel spiegelbild spiegelchen wandspiegel
💡|licht glühbirne lampe lichtlein nachttischlampe
🔦|taschenlampe taschenlampen lichtstrahl
🕯️|kerze kerzen kerzchen kerzenschein teelicht teelichter
🧹|besen kehrbesen handbesen fegen kehren staubsauger sauger staubsaugen
🧼|seife seifenstück waschlappen waschen handwäsche händewaschen haendewaschen
🧽|schwamm spülschwamm waschschwamm
🧺|wäsche wäschekorb waschkorb wäschehaufen
🧸|puppenteddy schnuffeltuch schmusetuch schnuffel schnuffi
🪆|matroschka matrioschka stapelpuppe holzpuppe
🪅|pinata piñata partyspiel
🪀|jo-jo jojochen
🛝|rutsche rutschen rutschbahn spielrutsche
🛴|rollerchen rollern rollerfahren
🎠|karussell karussellpferd rundfahrt
🎡|riesenrad riesenräder
🎢|achterbahn achterbahnen
🎪|zirkus circus zirkuszelt zirkusnummer
🤡|spaßmacher spaßvogel clownsnase
🎭|theater theaterstück puppentheater schauspiel schauspieler bühne
🎨|kunst kunstwerk farbkasten wasserfarbe wasserfarben tusche pinsel malen malerei buntstifte
🖌️|pinsel pinselchen pinselstrich
🖍️|wachsmalstift wachsmaler buntstift buntstifte filzstift filzstifte kreide kreiden strich striche
✏️|bleistifte kugelschreiber kuli stifte stiftchen
✂️|schere schneiden schnitt papierrauten bastelschere
🧩|puzzeln puzzleteil puzzleteile puzzlekasten puzzlebild
🧱|baustein bausteine klötzchen klotz klötze legostein legosteine lego
🪁|drachenfliegen drachenspiel drachenleine flugdrachen winddrachen fliegerdrachen
⚽|fußbälle fussball football soccer fußballspiel kick kicken torball
🏀|basketbälle korbball basketballspiel
🏐|volley volleyballspiel volleybälle
🏈|rugby americanfootball footballspiel
⚾|baseball softball baseballschläger
🎾|tennisball tennisbälle tennisplatz tennisracket
🏸|badminton federball federbälle federballschläger
🏓|tischtennis pingpong tischtennisball
🥅|tor tore fußballtor torpfosten
🏆|pokal pokale siegerpokal meisterpokal
🥇|gewinner sieger siegerin gewonnen goldmedaille ersteplatz
🎯|ziel zielscheibe darts dartpfeil
🎲|würfelspiel würfelspiele würfeln brettspiel
🃏|kartenspiel kartenkartenspiel spielkarte spielkarten
🎮|videospiel videospiele computerspiel computerspiele game gamer konsole spielkonsole
🧸|spielzeugauto spielzeugpferd puppenspielzeug kinderteddy
🪇|rassel rasseln klapper klappern maracas
🥁|pauke pauken schlagzeug schlagzeuger trommelchen trommelwirbel
🎺|horn trompetchen blechblasinstrument fanfare
🎷|sax saxophone saxophonist
🎸|ukulele laute bassgitarre e-gitarre gitarrist
🎹|keyboard keyboards klaviatur tastaturklavier tasteninstrument pianino
🎻|fiedel bratsche violine geigenbogen cello cellos kontrabass
🪈|blockflöte querflöte flötenmusik flötenspiel
🎤|gesangsmikrofon singstimme stimme singmikrofon karaoke
🎧|kopfhörer ohrhörer headset kopfhoerer
🎶|melodie melodien musikstück musikstücke liedchen liedlein liederchen musiknoten musiknote gesänge singenlied
🎼|notenschlüssel tonleiter notenblatt notenheft takt musikheft
🔔|glocke glocken glöckchen glöcklein klingel klingeln türglocke
📯|posthorn hornruf
📖|erzählung erzählungen vorlesen vorlesegeschichte geschichtenbuch märchenbuch bildergeschichte erzählbuch
📚|büchlein bilderbuch bilderbücher kinderbuch kinderbücher leseheft lesehefte bibliothek
📕|buchdeckel buchseite buchseiten buchband
📝|aufgabe aufgaben hausaufgabe hausaufgaben schreibübung lernen lernspiel üben aufschreiben
📜|schrift schriftrolle buchstabe buchstaben alphabet abcbuch
🔢|zahl nummern nummer nummerierung zählen zählenlernen
🎒|rucksack rucksäcke schulranzen schultasche tasche taschen kinderrucksack
👟|turnschuh turnschuhe sneaker sneakers stiefel stiefelchen gummistiefel
🧦|strumpf strümpfe socke söckchen strümpfchen
👕|tshirt t-shirt hemd hemden unterhemd shirt shirts pullover pulli pullis
👖|hose hosen jeans leggins leggings
👗|kleidchen kleidung kleidungsstück kleidungsstücke röckchen rock röcke
🧥|mantel mäntel regenjacke winterjacke jäckchen parka
🧢|kappe kappchen cap schirmmütze baseballkappe
👒|sonnenhut hüte hutchen sommerhut
🧣|halstuch tuch halswärmer warmerschal
🧤|fäustling fäustlinge handschuhchen
👑|kronen königskrone königreich krönchen
🪄|zauberstab zauberstäbe zauberwort zauberei magie hexerei zauberspruch
🧚|elfe elfen elflein feelein waldfee zahnfee
🧙|magier magierin hexer hexlein zauberin zauberlehrling
🧜|nixe nixen meerjungfräulein seejungfrau meerjungfrauchen
🧝|elf elbin elben elves
🦸|held heldin helden superhelden superkräfte heldentat
🦹|bösewicht bösewichte schurke schurken supergegner
👸|prinzeßin prinzesschen prinzessinnen königstochter königskind
🤴|königssohn märchenprinz prinzchen
👻|spuk spuken geisterchen geisterhaus gespenster gespensterchen
🎃|halloweengeist halloweenkürbis gruselkürbis kürbislaterne
🧛|vampir vampire vampirchen
🧟|zombie zombies untoter untote
🧞|dschinn flaschengeist wunschgeist
🐉|drachenbaby drachenkind feuerdrache feuerspucker drachenhöhle drachenland
🧊|eiswürfel eiskristall eiszapfen eisscholle eisfläche eisfeld eisberg frost gefrierpunkt
❄️|schneegestöber schneefall schneesturm schneeball schneebälle schneeballschlacht winterwetter
☃️|schneefigur schneefrau schneekind schneebub schneemännchen
⛄|schneemannbau schneemannfigur schneemannlied
☀️|sonnenstrahl sonnenstrahlen sonnenlicht sonnenschein sonnenscheibe sonnenaufgang tagessonne
🌅|morgendämmerung dämmerung sonnenaufgangsblick tagesanbruch morgenrot
🌇|sonnenuntergang abendrot abenddämmerung abendsonne feierabendhimmel
🌙|mondlicht mondnacht mondphase mondsichel mondscheinmond mondaufgang
🌕|vollmond vollmondnacht
🌑|neumond dunklermond
⭐|sternlein sternleinchen sternchenhimmel sternenhimmel sternenlicht funkelstern sternschnuppe
🌠|sternschnuppen fallstern fallsterne
🌈|regenbögen regenbogenfarben farbenbogen regenbogenschein
☁️|wölkchen wolkenhimmel wolkenmeer schäfchenwolke nebel nebelwolke
🌧️|regenschauer nieseln nieselregen regenschauerchen regenfront regenwolke regenwolken
☔|regenschirm schirm regenschirme regencape
⛈️|donner donnerwetter gewitterwolke unwetter blitzeinschlag gewittersturm
⚡|blitzlicht blitzstrahl blitzschlag elektrizität stromschlag
🌪️|wirbelwind wirbelsturm windhose sturmböe sturmböen tornadofront
🌬️|windstoß windstöße brise böe böen windhauch windzug
💨|pusten luftzug atemhauch pustewind windwolke
💧|wassertröpfchen wassertropfen wasserperle wassertropfen wasserlache
🌊|meerwasser ozeanwasser ozeanwelle meereswelle surfwelle wellengang brandung
🏖️|badestrand sandstrand strandtag sandburg sandburgen sandkorn sandhaufen
🏝️|einsameinsel tropeninsel inselchen inselparadies
🏔️|gebirge berggipfel bergspitze berglandschaft bergmassiv gipfelkreuz
⛰️|hügel hügelchen berglein bergchen
🌋|vulkan vulkane vulkanausbruch lava lavastrom vulkanberg
🏜️|wüste wüsten sandwüste oase oasen
🏞️|landschaft landschaften tal täler wasserfall wasserfälle flussufer bachlauf bächlein bergbach
🪨|stein steine steinchen fels felsen kies kiesel kieselstein
⏰|weckuhr weckerchen alarmuhr alarm weckerklingeln
⌚|armbanduhr armbanduhrchen zeigeruhr
🕰️|uhrzeit zeit zeitpunkt stundenzahl
📅|kalender kalenderblatt datum wochentag woche wochen
🌞|tag tageslicht taglicht sonnensonne
🌃|nachts nachtzeit nächtchen dunkelheit dunkelstunde
🌆|spätabend abendhimmel abendstunde abendzeit
👶|babychen babykind säugling säuglinge neugeborenes neugeborene
🧒|kind kinder kindchen kleinkind kleinkinder kindlein kindleinchen
👧|mädel mädchenkind töchterchen tochter töchter kindermädchen
👦|bub buben knabe knaben söhnchen sohn söhne
👩|mutti mutterchen mammilein mamilein mamaa mamae
👨|vati vaterchen papae papilein papii
👵|omi großmama großmutti großmami urgroßmutter urmi
👴|opi großpapa großvati urgroßvater
👨‍👩‍👧‍👦|familienmitglieder großfamilie eltern familienbande verwandschaft verwandtschaft
🫂|umarmung umarmen kuscheln kuschelig kuschelei drücken knuddeln knuddelchen
❤️|herzchen lieblingsherz herzlein herzschlag herzklopfen liebeshertz
💕|liebesherz liebelei herzensliebe herzensmensch verliebtheit
💋|küssen küsschen küsslein kussmund küsse
🧠|gedanke gedanken denken nachdenken kopfidee
👁️|blick ansehen schauen anblick hinschauen augelein
👀|äuglein äugchen äugleinchen augenpaar
👂|öhrchen öhrlein gehör lauschen hören
👃|näschen riechen schnuppern schnüffeln schnuppernase
👄|lippen lippe mundwinkel mündchen
🦷|zähnchen milchzahn milchzähne gebiss zahnkind
🦴|knochen knöchel knochenstück
✋|handfläche handchen händchen handschlag handschuhhand
🦶|füßchen füsse fuss fußsohle zehe zehen
🏃|rennen rennenlernen sprint lauf laufschritt rennerei
🚶|spaziergang spazieren spaziergänge spaziergänger spazierweg fußweg gehen
💃|tänzchen tänze tanzschritt tänzerin ballett tanze tanzmusik
😴|schlummer schlummern schlummerlied schlaflied einschlafen ausruhen mümmeln dösen müdig müdezeit
🥱|gähnen gähnt gähnend gähner müdigkeit
😂|lachen lachenlernen lacher gelächter kichern kichert
😊|lächeln lächelt lächelnd grinsen grinst froh zufrieden fröhlichkeit
🥰|liebhaben liebhabenlernen liebevoll herzallerliebst geliebt verliebtsein
😢|träne tränen tränenreich schluchzen schnieft heulen heult weinen
😭|weinkrampf weinerlich schluchzer weinend weiner
😠|ärgerlich wut wütendsein zorn zornig schimpfen schimpfend schimpft
😱|angst ängstlich fürchten erschrecken schreck schrecklich gruseln
😮|staunen staunt erstaunen überraschung überrascht verwundert
🤒|krank krankheit fieber husten schnupfen erkältung
🤧|niesen niest niese niesanfall
🤕|weh schmerz schmerzen verletzt verletzung aua
🤗|willkommen begrüßung begrüßen hallochen
👋|winke winkewinke winkewinkehand abschied wiedersehen lebewohl
🙏|danke dankeschön dankeschoen bitten bitte dankbarkeit
🤝|händedruck freundschaft freundehand freundegruß
💤|schlafenszeit traumland schlummerland
💭|traum träumen träumt träumerei träumchen
🎁|geschenkchen geschenkpaket geschenkpapier bescherung weihnachtsgeschenk
🎄|weihnachtszeit weihnachtstanne weihnachtsfest christbaum christbaumschmuck
🎅|weihnachtsopa weihnachtsmannchen weihnachtsmannkind santa santaclaus
🧑‍🎄|nikolausfrau weihnachtsfrau
🧑‍🚒|feuerwehrleute feuerwehrmenschen feuerwehrmänner feuerwehrfrauen brandmeister feuerwehrmensch
👮|polizeibeamter polizeibeamtin schutzmann schutzfrau polizeileute
👨‍⚕️|doktor doktorin kinderarzt kinderärztin hausarzt ärztemann
👩‍🍳|köchlein bäcker bäckerin bäckermeister kochmütze küchenchef kochen
👨‍🌾|bauer bäuerin landwirt landwirtin farm farmer bauersmann
👨‍🔧|mechaniker mechanikerin monteure monteurin schrauber schrauben reparieren
👷|bauarbeiter bauarbeiterin handwerker handwerkerin bauen bauhelfer
🧑‍🏫|lehrer lehrerin schulmeister schulmeisterin unterrichten
👩‍🎨|maler malerin künstler künstlerin
🧑‍🚀|astronaut astronautin raumfahrer raumfahrerin kosmonaut
🏴‍☠️|pirat piraten piratin seeräuber seeräuberin freibeuter
🧭|kompass richtung orientierung nordpol südpol
🌍|erdkugel erdboden planetenerde weltall erdenwelt
🌌|galaxie galaxien sternenwelt milchstraße milchstrasse kosmos universum
🪐|saturn jupiter uranus neptun planeten planetensystem
🐾|pfote pfoten tatze tatzen fährte fährten tierpfote
🥁|klopf klopfen klopfgeräusch klopflaut
🔊|geräusch geräusche lärm laute lauter ton töne klang klänge klappergeräusch
🔕|still ruhe leise lautlos stille schweigen
""".trimIndent()
    private val ADJECTIVES = """
🔴|rot roetlich rötlich feuerrot knallrot dunkelrot hellrot
🔵|blau blaulich bläulich himmelblau dunkelblau hellblau meerblau
🟢|grün gruen grünlich grasgrün dunkelgrün hellgrün
🟡|gelb gelblich sonnengelb hellgelb goldgelb
🟠|orangefarben orangefarbig orangefarbenen orangefarbenes
🟣|lila violett purpurfarben dunkelviolett fliederfarben
⚫|schwarz rabenschwarz pechschwarz kohlschwarz
⚪|weiß weiss schneeweiß schneeweiss reinweiß reinweiss
🩷|pink rosafarben rosa pinkfarben
🟤|braun braeunlich bräunlich kastanienbraun
🩶|grau gräulich grauhaarig aschgrau
🎨|bunt farbenfroh kunterbunt farbig farbenreich regenbogenbunt
✨|glitzernd funkelnd glänzend glanzvoll glitzerig leuchtend strahlend
🌞|hell sonnig sonnenhell licht
🌃|dunkel finster nachtschwarz schummrig
🔥|heiß heiss glühend brennend feurig feuerheiß
🧊|kalt eiskalt frostig frostkalt eisig klirrend
❄️|winterlich verschneit schneereich
🌧️|regnerisch verregnet nass triefend
🌬️|windig windreich windzerzaust
😊|fröhlich froh glücklich heiter vergnügt lustig vergnuegt
😢|traurig betrübt unglücklich bekümmert
😠|wütend böse zornig grimmig sauer ärgerlich
🥰|lieb liebevoll freundlich nett herzlich zauberhaft süß suess niedlich putzig
😴|müde schläfrig verschlafen schlaftrunken
🧸|kuschelig weich flauschig plüschig gemütlich gemuetlich
🏃|schnell flink eilig rasant blitzschnell
🐢|langsam gemütlichlangsam schleichend
💪|stark kräftig riesenstark superkräftig
🤏|klein winzig winzigklein mini klitzeklein
📏|groß gross riesig riesengroß gigantisch
🆕|neu nagelneu frisch brandneu
👴|alt uralt steinalt
❤️|schön hübsch wundervoll prächtig
🎉|toll klasse prima super großartig fantastisch
🤫|leise ruhig mucksmäuschenstill still
📢|laut lärmend ohrenbetäubend
🍬|süß zuckersüß honigsüß
🍋|sauer säuerlich
🌶️|scharf pfeffrig
🥱|müde gähnend
🥶|frierend gefroren
🥵|schwitzend aufgeheizt
""".trimIndent()
    private val VERBS = """
🎶|singen trällern summen zwitschern musizieren
💃|tanzen hüpftanzen schwingen rocken
😀|lachen kichern glucksen
😊|lächeln grinsen
😢|weinen schluchzen heulen
😴|schlafen schlummern dösen
💤|träumen
🏃|rennen sprinten jagen hetzen
🚶|wandern spazieren marschieren bummeln
🦘|hüpfen springen hoppen hoppeln
🛝|rutschen
🚲|radeln strampeln
🚗|fahren sausen düsen
✈️|fliegen schweben
🛁|baden planschen plantschen
🏊|schwimmen tauchen
🚿|duschen
🪥|putzen schrubben
🧼|waschen abwaschen spülen
🖍️|malen zeichnen kritzeln
✂️|schneiden basteln kleben falten
📚|lesen vorlesen schmökern
✏️|schreiben kritzeln
🗣️|sprechen reden erzählen rufen
👂|hören lauschen zuhören
👀|sehen schauen gucken betrachten
👃|riechen schnuppern
👋|winken grüßen begrüßen
👏|klatschen applaudieren
🙏|danken
🎁|schenken verschenken
🫂|kuscheln knuddeln umarmen
💋|küssen schmatzen
💞|lieben liebhaben
🍽️|essen futtern mampfen kauen
🥤|trinken schlürfen nippen
🍲|kochen köcheln
🍞|backen
🧹|fegen kehren saugen staubsaugen
💡|leuchten strahlen
✨|funkeln glitzern glänzen
💧|tropfen tröpfeln
🌧️|regnen nieseln
❄️|schneien
⚡|blitzen
⛈️|donnern
🔥|brennen lodern
🌬️|wehen stürmen pusten
🔔|klingeln läuten bimmeln
🥁|trommeln klopfen hämmern
🧸|spielen
🎸|musizieren
🧩|puzzeln knobeln rätseln
🛏️|liegen ausruhen
🪑|sitzen
🧍|stehen
🫴|geben reichen
🔍|suchen entdecken finden
🙈|verstecken
🧱|bauen errichten
⚙️|drehen schrauben
🌱|wachsen sprießen blühen
🐣|schlüpfen
🐶|bellen jaulen
🐱|miauen schnurren
🐮|muhen
🐷|grunzen
🐑|blöken
🐓|krähen
🐸|quaken
🐦|piepsen zwitschern
🐝|summen
🐴|wiehern
🦁|brüllen
🐍|zischen
🐺|heulen
🦆|schnattern
""".trimIndent()
    private val IRREGULAR = """
😴|schläfst schläft schlief schliefen geschlafen eingeschlafen
🏃|läuft laufst lauft lief liefen gelaufen
🚶|geht gehst gehen ging gingen gegangen
✈️|fliegt fliegst flog flogen geflogen
🐝|fliegt fliegen
👀|sieht siehst sah sahen gesehen
👂|hört hörst hörte hörten gehört
🍽️|isst isst essen aß aßen gegessen
🥤|trinkt trinkst trank tranken getrunken
🎶|singt singst sang sangen gesungen
💃|tanzt tanzte tanzten getanzt
😀|lacht lachte lachten gelacht
😢|weint weinte weinten geweint
🔔|klingt klang klangen geklungen
🌱|wächst wuchsen wuchs gewachsen
💤|träumt träumte träumten geträumt
🐶|bellt bellte bellten gebellt
🐱|miaut miaute miauten gemiaut
🐮|muht muhte muhten gemuht
🐷|grunzt grunzte grunzten gegrunzt
🐸|quakt quakte quakten gequakt
🌧️|regnet regnete regneten geregnet
❄️|schneit schneite schneiten geschneit
⛈️|donnert donnerte donnerten gedonnert
🏊|schwimmt schwamm schwammen geschwommen
🛁|badet badete badeten gebadet
🍞|bäckt backt backte backten gebacken
🧸|kuschelt kuschelte kuschelten gekuschelt
""".trimIndent()
    private val PHRASES = """
🌙|gute nacht
🌅|guten morgen
🌇|guten abend
🎂|happy birthday
🎂|alles gute zum geburtstag
🧼|hände waschen
🪥|zähne putzen
🦆|alle meine entchen
🔔|bruder jakob
🧸|schlaf gut
👋|auf wiedersehen
👋|bis bald
❤️|hab dich lieb
🎄|frohe weihnachten
🐰|frohe ostern
🌠|schlaf schön
""".trimIndent()
}
