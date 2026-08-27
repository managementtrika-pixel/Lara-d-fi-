package com.metahumanlegacy.game

import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class Scope(val label: String) { STREET("Rue"), DISTRICT("Quartier"), CITY("Ville"), REGION("Région"), COUNTRY("Pays"), WORLD("Monde") }

data class CharacterBlueprint(
    val firstName: String,
    val lastName: String,
    val alias: String,
    val pronouns: String,
    val city: String,
    val district: String,
    val socialBackground: String,
    val origin: String,
    val powerFamily: String,
    val weakness: String,
    val motivation: String,
    val visualStyle: String
) {
    val fullName: String get() = listOf(firstName.trim(), lastName.trim()).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Alex Vesper" }
}

data class Choice(
    val label: String,
    val moral: Int,
    val prestige: Int,
    val opinion: Int,
    val fear: Int,
    val power: Int,
    val impact: Int,
    val risk: Int,
    val approach: String,
    val stakes: Int,
    val sourceCategory: String,
    val threadId: String? = null,
    val relationDelta: Int = 0,
    val flag: String? = null
)

data class EventNode(
    val id: String,
    val title: String,
    val text: String,
    val choices: List<Choice>,
    val category: String,
    val provocation: String,
    val stakes: Int,
    val threadId: String? = null,
    val threadStage: Int = 0
)

data class StoryThread(
    val id: String,
    val openedTurn: Int,
    val lastTurn: Int,
    val stage: Int,
    val lastApproach: String,
    val intensity: Int
)

data class Resolution(val campaign: Campaign, val outcome: String)

private data class SituationTemplate(
    val title: String,
    val setup: String,
    val provocation: String,
    val protectTarget: String,
    val objective: String,
    val actor: String,
    val tacticalAngle: String,
    val stakes: Int,
    val threadId: String?
)

data class Campaign(
    val seed: Long,
    val name: String,
    val alias: String,
    val origin: String,
    val powerFamily: String,
    val weakness: String,
    val modifier: String,
    val pronouns: String = "iel",
    val city: String = "Vesper",
    val district: String = "Centre",
    val socialBackground: String = "Classe moyenne",
    val motivation: String = "Protéger les miens",
    val visualStyle: String = "Masque minimal",
    val turn: Int = 0,
    val morality: Int = 0,
    val prestige: Int = 0,
    val opinion: Int = 0,
    val fear: Int = 0,
    val power: Int = 28,
    val control: Int = 25,
    val influence: Int = 0,
    val health: Int = 100,
    val civilianCasualties: Int = 0,
    val identityExposure: Int = 0,
    val familyBond: Int = 50,
    val rivalStanding: Int = 0,
    val governmentStanding: Int = 0,
    val factionStanding: Int = 0,
    val mediaStanding: Int = 0,
    val flags: Set<String> = emptySet(),
    val threads: List<StoryThread> = emptyList(),
    val lastCategory: String = "",
    val lastApproach: String = "",
    val timeline: List<String> = emptyList()
) {
    val age: Int get() = 18 + turn / 4
    val scope: Scope get() = when {
        influence >= 900 -> Scope.WORLD
        influence >= 560 -> Scope.COUNTRY
        influence >= 340 -> Scope.REGION
        influence >= 180 -> Scope.CITY
        influence >= 75 -> Scope.DISTRICT
        else -> Scope.STREET
    }
    val moralLabel: String get() = when {
        morality >= 70 -> "Héroïque"
        morality >= 35 -> "Bienveillant"
        morality >= 10 -> "Altruiste"
        morality > -10 -> "Ambigu"
        morality > -35 -> "Impitoyable"
        morality > -70 -> "Corrompu"
        else -> "Monstrueux"
    }
    val finished: Boolean get() = turn >= 140 || health <= 0
}

object GameEngine {
    val pronouns = listOf("il", "elle", "iel")
    val cities = listOf("Vesper", "Greybridge", "Noxhaven", "Solara", "Kade City", "Oris", "Meridian", "Eidolon")
    val districts = listOf("Centre", "Les Docks", "Vieille-Ville", "Nord-Est", "Ceinture Sud", "Hauteurs", "Rives", "Secteur industriel")
    val socialBackgrounds = listOf("Quartier populaire", "Classe moyenne", "Milieu privilégié", "Foyer instable", "Famille militaire", "Milieu scientifique", "Autodidacte précaire", "Héritier d'une organisation")
    val motivations = listOf("Protéger les miens", "Justice", "Reconnaissance", "Pouvoir", "Liberté", "Réparer une faute", "Comprendre mes pouvoirs", "Changer le système")
    val visualStyles = listOf("Masque minimal", "Capuche tactique", "Silhouette civile", "Armure artisanale", "Tenue symbolique", "Visage découvert", "Manteau long", "Équipement modulaire")
    val origins = listOf(
        "Mutation naturelle", "Accident scientifique", "Expérience clandestine", "Programme militaire",
        "Technologie personnelle", "Héritage familial", "Artefact mystérieux", "Pacte occulte",
        "Origine extraterrestre", "Énergie cosmique", "Entraînement humain extrême", "Intelligence augmentée"
    )
    val powers = listOf(
        "Force", "Résistance", "Vitesse", "Vol", "Énergie", "Feu", "Glace", "Électricité",
        "Télékinésie", "Télépathie", "Illusion", "Influence mentale limitée", "Métamorphose", "Invisibilité",
        "Régénération", "Technologie", "Armes spécialisées", "Magie", "Matière", "Gravité", "Espace",
        "Duplication", "Invocation", "Absorption", "Adaptation", "Humain exceptionnel"
    )
    val weaknesses = listOf(
        "Surcharge", "Fatigue extrême", "Concentration", "Énergie externe", "Fréquence sonore",
        "Instabilité émotionnelle", "Temps de récupération", "Environnement", "Vulnérabilité psychique",
        "Vulnérabilité mystique", "Pouvoir difficile à dissimuler", "Précision limitée"
    )
    private val modifiers = listOf(
        "Âge des héros", "Première génération", "Société méfiante", "Culture héroïque", "État autoritaire",
        "Criminalité endémique", "Ère technologique", "Menace occulte", "Silence cosmique", "Médias omniprésents"
    )
    private val categories = listOf("RUE", "IDENTITÉ", "FAMILLE", "MÉDIAS", "FACTION", "RIVAL", "SAUVETAGE", "CRIME", "POUVOIR", "GOUVERNEMENT", "MENTOR", "CRISE")

    fun randomBlueprint(seed: Long): CharacterBlueprint {
        val r = Random(seed)
        val firstNames = listOf("Malik", "Nora", "Elias", "Maya", "Soren", "Lina", "Ilyan", "Kael", "Naël", "Ava", "Milo", "Yara", "Nell", "Zayn")
        val lastNames = listOf("Voss", "Deren", "Kess", "Arden", "Vale", "Nox", "Raine", "Sol", "Marek", "Serrin", "Vey", "Korr")
        val aliases = listOf("Vesper", "Axiom", "Morrow", "Cipher", "Silex", "Halo", "Noctis", "Vector", "Rift", "Cinder", "Mantis", "Aster")
        return CharacterBlueprint(
            firstNames[r.nextInt(firstNames.size)], lastNames[r.nextInt(lastNames.size)], aliases[r.nextInt(aliases.size)],
            pronouns[r.nextInt(pronouns.size)], cities[r.nextInt(cities.size)], districts[r.nextInt(districts.size)],
            socialBackgrounds[r.nextInt(socialBackgrounds.size)], origins[r.nextInt(origins.size)], powers[r.nextInt(powers.size)],
            weaknesses[r.nextInt(weaknesses.size)], motivations[r.nextInt(motivations.size)], visualStyles[r.nextInt(visualStyles.size)]
        )
    }

    fun newCampaign(seed: Long, blueprint: CharacterBlueprint = randomBlueprint(seed)): Campaign {
        val r = Random(seed xor 0x51A7L)
        val originBoost = when (blueprint.origin) {
            "Programme militaire", "Entraînement humain extrême" -> 6
            "Énergie cosmique", "Origine extraterrestre" -> 9
            "Technologie personnelle", "Intelligence augmentée" -> 3
            else -> 5
        }
        val controlBoost = when (blueprint.socialBackground) {
            "Famille militaire" -> 7
            "Milieu scientifique" -> 5
            "Foyer instable" -> -3
            else -> 1
        }
        val moralStart = when (blueprint.motivation) {
            "Protéger les miens" -> 6
            "Justice" -> 4
            "Réparer une faute" -> 2
            "Pouvoir" -> -6
            else -> 0
        }
        val identityStart = if (blueprint.visualStyle == "Visage découvert") 38 else if (blueprint.visualStyle == "Silhouette civile") 10 else 0
        return Campaign(
            seed = seed,
            name = blueprint.fullName,
            alias = blueprint.alias.trim().ifBlank { "Vesper" },
            origin = blueprint.origin,
            powerFamily = blueprint.powerFamily,
            weakness = blueprint.weakness,
            modifier = modifiers[r.nextInt(modifiers.size)],
            pronouns = blueprint.pronouns,
            city = blueprint.city,
            district = blueprint.district,
            socialBackground = blueprint.socialBackground,
            motivation = blueprint.motivation,
            visualStyle = blueprint.visualStyle,
            morality = moralStart,
            power = clamp(22 + originBoost + r.nextInt(12), 10, 60),
            control = clamp(20 + controlBoost + r.nextInt(15), 10, 60),
            identityExposure = identityStart,
            familyBond = if (blueprint.motivation == "Protéger les miens") 62 else 50,
            flags = setOf("origin:${blueprint.origin}", "motivation:${blueprint.motivation}", "background:${blueprint.socialBackground}")
        )
    }

    private val situations = mapOf(
        "RUE" to listOf(
            S("La caisse et la fumée", "Une supérette brûle pendant qu'un braqueur profite de la panique.", "Il te crie que tu n'auras jamais le temps de sauver tout le monde et de l'arrêter.", "les deux personnes à l'étage", "couper sa fuite", "le braqueur", "ouvrir le rideau métallique pour créer une sortie", 2, "STREET"),
            S("Le mauvais trottoir", "Une bande encercle un adolescent accusé d'avoir parlé à la police.", "Le meneur promet que le quartier paiera dès que tu auras le dos tourné.", "l'adolescent", "identifier celui qui donne réellement les ordres", "le meneur", "faire parler les riverains sans les exposer", 2, "STREET"),
            S("Collision", "Un véhicule volé percute un arrêt de bus et continue sa fuite.", "Le conducteur ralentit juste assez pour t'obliger à choisir.", "l'enfant coincé sous l'abri", "stopper le véhicule", "le conducteur", "utiliser le carrefour pour le forcer à ralentir", 3, "STREET"),
            S("La dette du bloc", "Des racketteurs viennent punir un commerçant qui refuse de payer.", "Ils annoncent devant les voisins que ta présence ne changera aucune règle.", "le commerçant et sa famille", "briser le système d'extorsion", "les racketteurs", "les suivre jusqu'à leur collecteur", 2, "CRIME"),
            S("Après le match", "Une bagarre dégénère en mouvement de foule et quelqu'un poignarde un passant.", "Ses amis te donnent volontairement une fausse direction.", "la victime", "retrouver l'agresseur", "ses complices", "observer qui quitte la scène sans paniquer", 2, "STREET"),
            S("Le toit d'en face", "Un tireur menace une place depuis un toit invisible à la foule.", "Il t'envoie : montre-toi ou je choisis quelqu'un au hasard.", "la foule", "localiser le tireur", "le tireur", "couper son angle de vue sans révéler ta position", 3, "RIVAL"),
            S("Le tunnel", "Un accident bloque un tunnel pendant qu'un groupe vole les véhicules immobilisés.", "Leur chef filme la scène et affirme que tu protèges les biens plus que les gens.", "les automobilistes piégés", "empêcher le pillage organisé", "le chef du groupe", "éteindre l'éclairage pour déplacer la foule", 2, "STREET"),
            S("Une minute de silence", "Une veillée tourne à l'affrontement entre deux groupes du quartier.", "Quelqu'un utilise ton symbole pour appeler à la vengeance.", "les familles présentes", "identifier l'usurpateur", "l'agitateur", "faire baisser la tension avant de révéler la manipulation", 2, "MEDIA")
        ),
        "IDENTITÉ" to listOf(
            S("Le visage flou", "Une vidéo améliore image par image une silhouette qui pourrait être toi.", "Le compte promet un visage net dans une heure.", "tes proches", "retrouver la source du fichier", "le diffuseur", "injecter une fausse correspondance dans son raisonnement", 3, "IDENTITY"),
            S("Une photo de trop", "Un photographe possède une image de toi entrant blessé dans un lieu civil.", "Il réclame une exclusivité au nom du droit de savoir.", "ta vie civile", "récupérer le contexte complet de la photo", "le photographe", "prouver qu'une autre explication est possible", 2, "IDENTITY"),
            S("Le faux témoin", "Une personne affirme à la police t'avoir reconnu sans masque.", "Elle connaît des détails qui ne devraient pas être publics.", "ton secret", "comprendre qui l'a renseignée", "le témoin", "tester un détail qu'un vrai témoin devrait connaître", 3, "IDENTITY"),
            S("Dossier croisé", "Une base privée relie tes horaires, blessures et apparitions.", "Son propriétaire offre d'effacer le résultat contre un service futur.", "ton identité civile", "neutraliser le croisement de données", "le propriétaire", "modifier un seul maillon plutôt que tout détruire", 3, "IDENTITY"),
            S("Chez toi", "Un symbole d'adversaire apparaît devant ta porte.", "L'absence de message prouve seulement qu'ils peuvent venir jusque-là.", "ton foyer", "déterminer qui a trouvé l'adresse", "l'intrus inconnu", "surveiller discrètement au lieu de fuir", 3, "IDENTITY"),
            S("Le proche interrogé", "Un journaliste approche un de tes proches avec une photo précise.", "Il laisse entendre qu'il publiera même sans confirmation.", "ton proche", "couper la piste sans le compromettre", "le journaliste", "utiliser une chronologie impossible à concilier", 2, "IDENTITY"),
            S("Double parfait", "Quelqu'un portant une copie de ton apparence commet un acte public.", "La ressemblance est assez bonne pour diviser immédiatement les témoins.", "ta réputation civile", "identifier le faux", "l'imposteur", "laisser une signature que lui ne peut reproduire", 3, "IDENTITY"),
            S("Le dossier médical", "Une clinique détecte une anomalie compatible avec ton pouvoir.", "Un employé propose de faire disparaître le résultat contre de l'argent.", "ta confidentialité", "sécuriser le dossier légalement", "l'employé", "remonter qui consulte déjà tes données", 2, "IDENTITY")
        ),
        "FAMILLE" to listOf(
            S("Deux urgences", "Un proche t'attend pour un moment important au moment exact où une alerte tombe.", "Il te dit que cette fois partir changera quelque chose entre vous.", "votre relation", "gérer l'urgence extérieure", "ton proche", "trouver quelqu'un d'autre capable d'intervenir", 2, "FAMILY"),
            S("Ce que tu caches", "Un membre de ta famille découvre du sang et du matériel chez toi.", "Il refuse une nouvelle excuse.", "la confiance familiale", "protéger ton secret", "ce proche", "dire une vérité partielle mais vérifiable", 2, "FAMILY"),
            S("La cible facile", "Une menace cite le prénom d'un proche sans demander d'argent.", "Le message demande seulement ce que tu es prêt à sacrifier.", "ton proche", "identifier l'auteur", "le maître-chanteur", "transformer la menace en piège contrôlé", 3, "FAMILY"),
            S("La promesse", "Tu avais juré de ne plus utiliser tes pouvoirs devant les tiens quand un accident survient.", "Une seconde d'hésitation peut coûter une vie.", "la personne en danger", "tenir ou briser ta promesse", "ta propre peur", "agir de façon assez discrète pour limiter l'exposition", 3, "FAMILY"),
            S("Partir loin", "Tes proches veulent quitter la ville à cause de ce que ta vie attire.", "Ils te demandent si tu les suivrais vraiment.", "ta famille", "préserver ta mission", "la personne qui veut partir", "organiser une protection indépendante de toi", 2, "FAMILY"),
            S("Le mensonge utile", "La police questionne un proche après une intervention.", "Il peut te couvrir, mais devra mentir officiellement.", "ton proche", "éviter que l'enquête remonte jusqu'à toi", "l'enquêteur", "fournir une explication qui ne l'oblige pas à mentir", 2, "FAMILY"),
            S("Héritage involontaire", "Un jeune membre de ta famille imite tes méthodes dans la rue.", "Il affirme qu'il ne fait que ce que toi-même lui as appris sans le vouloir.", "le jeune proche", "arrêter son imitation dangereuse", "son ego", "l'impliquer dans une mission sans violence", 2, "FAMILY"),
            S("La chambre vide", "Après une menace répétée, un proche disparaît volontairement sans prévenir.", "Son seul message dit qu'il refuse d'être ta faiblesse.", "ce proche", "le retrouver sans l'étouffer", "la situation", "laisser un canal sûr pour qu'il revienne de lui-même", 3, "FAMILY")
        ),
        "MÉDIAS" to listOf(
            S("Direct national", "Une chaîne te propose un direct sans montage après une intervention controversée.", "Le présentateur annonce qu'il te demandera si tu te crois au-dessus de la loi.", "ta crédibilité", "reprendre le récit", "le présentateur", "arriver avec des faits impossibles à contourner", 2, "MEDIA"),
            S("Le montage", "Une vidéo virale coupe les secondes où tu sauves des civils.", "Le média refuse de corriger sans ta présence sur son plateau.", "la vérité des faits", "obtenir la séquence complète", "le média", "faire témoigner une personne neutre", 2, "MEDIA"),
            S("La victime parle", "Une personne sauvée dit publiquement avoir eu plus peur de toi que de l'agresseur.", "Ses mots deviennent un slogan politique.", "sa parole", "éviter que son témoignage soit instrumentalisé", "les commentateurs", "la rencontrer sans caméra", 3, "MEDIA"),
            S("Offre d'exclusivité", "Un média propose de contrôler ton image pendant six mois.", "Il veut en échange un accès privilégié à tes interventions.", "ta vie privée", "garder le contrôle de ton récit", "le réseau", "négocier un droit de retrait total", 2, "MEDIA"),
            S("Question piège", "Après une catastrophe, les micros se tournent vers toi avant la fin des secours.", "On exige que tu nommes immédiatement un responsable.", "les victimes", "éviter une accusation sans preuve", "la journaliste", "retourner la question vers les faits confirmés", 2, "MEDIA"),
            S("Le héros fabriqué", "Un réseau transforme une intervention banale en exploit historique.", "Accepter le récit ferait exploser ton prestige, mais plusieurs détails sont faux.", "ta crédibilité future", "corriger le récit", "le réseau", "garder l'élan tout en corrigeant les mensonges", 2, "MEDIA"),
            S("Le sondage", "Une enquête nationale te place parmi les figures les plus appréciées ou détestées du pays.", "Un conseiller propose d'adapter tes interventions pour protéger ton score.", "ton indépendance", "refuser de jouer pour les chiffres", "le conseiller", "utiliser le sondage pour comprendre les peurs réelles", 2, "MEDIA"),
            S("Micro ouvert", "Une conversation privée est diffusée accidentellement avant une interview.", "Une phrase sortie du contexte devient immédiatement virale.", "la personne avec qui tu parlais", "rétablir le contexte", "le public", "publier l'échange complet malgré ce qu'il révèle", 3, "MEDIA")
        ),
        "FACTION" to listOf(
            S("Contrat sans signature", "Une organisation t'offre refuge, renseignement et équipement.", "Elle refuse de dire ce qu'elle exigera le jour de sa dette.", "ton autonomie", "obtenir des garanties", "l'organisation", "accepter seulement une ressource traçable", 2, "FACTION"),
            S("Territoire partagé", "Une faction exige que tu cesses d'intervenir dans deux quartiers.", "Son émissaire présente ton refus comme une déclaration de guerre.", "les habitants concernés", "préserver ton accès au terrain", "l'émissaire", "proposer une opération commune test", 3, "FACTION"),
            S("L'ennemi de mon ennemi", "Un groupe que tu méprises possède la seule piste vers une menace supérieure.", "Il ne parlera qu'en échange d'une opération commune visible.", "les futures victimes", "obtenir l'information", "ce groupe", "coopérer sans blanchir son passé", 3, "FACTION"),
            S("Place vide", "Une faction vient de perdre son chef et plusieurs membres veulent te rallier.", "Ils exigent une règle claire immédiatement.", "les membres hésitants", "empêcher une guerre de succession", "les prétendants", "imposer une transition temporaire plutôt qu'un chef", 2, "FACTION"),
            S("Le traître utile", "Un membre ennemi offre des dossiers internes contre ta protection.", "Il reconnaît avoir participé à des opérations meurtrières.", "les futures victimes", "récupérer les dossiers", "le transfuge", "le faire témoigner plutôt que le blanchir", 3, "FACTION"),
            S("La réunion", "Trois groupes acceptent une rencontre neutre.", "L'un arrive armé et prétend que c'est par respect pour toi.", "les négociateurs", "éviter une escalade", "la faction armée", "faire déposer les armes sans humilier personne", 2, "FACTION"),
            S("Vote interne", "Une faction alliée veut adopter une règle que tu juges dangereuse.", "Ses dirigeants te demandent de ne pas intervenir dans leur démocratie interne.", "les membres minoritaires", "infléchir la décision", "le conseil", "convaincre un membre charnière plutôt que menacer", 2, "FACTION"),
            S("Le prix du refuge", "Une organisation accepte de cacher un de tes proches.", "Elle exige en échange que tu ignores une de ses opérations futures.", "ton proche", "obtenir le refuge sans dette morale", "le chef de faction", "proposer une autre contrepartie publique", 3, "FACTION")
        ),
        "RIVAL" to listOf(
            S("Même rue, deux règles", "Un autre surhumain intervient brutalement dans ta zone.", "Devant les témoins, il te traite d'amateur et te demande de partir.", "les civils", "reprendre le contrôle de l'intervention", "ton rival", "lui donner une tâche où sa puissance est utile", 2, "RIVAL"),
            S("Le défi", "Un rival publie une heure et un lieu pour prouver qu'il est plus puissant.", "Ton absence sera présentée comme un aveu de faiblesse.", "les spectateurs potentiels", "éviter son terrain de jeu", "ton rival", "transformer son rendez-vous en opération utile", 2, "RIVAL"),
            S("Victoire volée", "Un rival revendique le mérite d'une opération que tu as préparée.", "Il sait que le contredire révélerait des informations sensibles.", "ton réseau", "rétablir les faits", "ton rival", "laisser une preuve apparaître par un tiers", 2, "RIVAL"),
            S("Sauvetage concurrent", "Vous arrivez ensemble sur une catastrophe avec des méthodes incompatibles.", "Il te somme de suivre ses ordres devant les civils.", "les victimes", "coordonner l'intervention", "ton rival", "séparer les zones selon vos forces", 3, "RIVAL"),
            S("Faiblesse connue", "Ton rival comprend ce qui limite ton pouvoir.", "Il gardera le secret uniquement tant que tu ne l'humilies plus publiquement.", "ton secret", "neutraliser son levier", "ton rival", "lui révéler volontairement une fausse limite", 3, "RIVAL"),
            S("Alliance impossible", "Une menace dépasse chacun de vous séparément.", "Ton rival coopère seulement s'il dirige l'opération.", "la ville", "vaincre la menace commune", "ton rival", "partager le commandement par phase", 3, "RIVAL"),
            S("Le protégé", "Ton rival commence à entraîner un jeune surhumain.", "Le jeune répète déjà ses insultes contre toi.", "le jeune", "éviter qu'il hérite d'une guerre personnelle", "ton rival", "parler au protégé sans contourner son mentor", 2, "RIVAL"),
            S("Trêve brisée", "Après des mois de calme, une preuve semble montrer que ton rival t'a trahi.", "Il jure qu'il s'agit d'un montage et te demande une seule chance.", "la trêve", "vérifier la preuve", "ton rival", "tester une information que seul le vrai traître connaîtrait", 3, "RIVAL")
        ),
        "SAUVETAGE" to listOf(
            S("L'escalier qui cède", "Un immeuble menace de s'effondrer avec plusieurs familles à l'intérieur.", "Le responsable de l'explosion profite de chaque seconde pour fuir.", "les familles", "empêcher la fuite du responsable", "le fuyard", "stabiliser un étage pour gagner du temps", 3, "RESCUE"),
            S("Sous le fleuve", "Un véhicule collectif sombre alors qu'une autre victime appelle depuis la rive.", "Ton pouvoir ne te permettra pas d'être partout.", "les passagers", "atteindre la victime isolée", "le courant", "utiliser un objet flottant comme relais", 3, "RESCUE"),
            S("Le mauvais étage", "Un incendie hospitalier coupe deux ailes et les secours se contredisent.", "Une infirmière te supplie d'ignorer le protocole.", "les patients", "trouver la zone réellement prioritaire", "le feu", "suivre l'infirmière tout en balisant une issue", 3, "RESCUE"),
            S("Pont suspendu", "Une portion de pont s'affaisse avec des véhicules dessus.", "Un criminel impliqué dans l'accident réclame ton aide en premier.", "les conducteurs", "stabiliser le pont", "le criminel", "le sauver sans lui permettre de fuir", 3, "RESCUE"),
            S("Respirer", "Une fuite toxique se répand dans un métro bondé.", "Fermer les accès ralentirait le nuage mais enfermerait des dizaines de personnes.", "les passagers", "contenir le nuage", "la fuite", "créer une dépression vers un tunnel vide", 3, "RESCUE"),
            S("La foule contre toi", "Après une explosion, la panique bloque les secours.", "Plusieurs personnes refusent de suivre tes instructions par méfiance.", "les blessés", "ouvrir un passage aux secours", "la foule", "convaincre une personne respectée de relayer tes ordres", 2, "RESCUE"),
            S("Ascenseur 17", "Un ascenseur est bloqué entre deux étages après une panne massive.", "Une seconde cabine chute plus bas au même moment.", "les personnes de la première cabine", "ralentir la seconde", "la panne", "répartir ton effort entre maintien et évacuation", 3, "RESCUE"),
            S("Le stade", "Une tribune commence à céder pendant un événement bondé.", "Les organisateurs veulent éviter une évacuation générale pour ne pas provoquer de panique.", "les spectateurs", "stabiliser la tribune", "les organisateurs", "évacuer par secteurs sans annoncer l'effondrement", 3, "RESCUE")
        ),
        "CRIME" to listOf(
            S("Test de territoire", "Un réseau multiplie de petits délits coordonnés pour mesurer ton temps de réaction.", "Son chef diffuse que tu es prévisible.", "les victimes dispersées", "remonter jusqu'au coordinateur", "le réseau", "laisser volontairement une zone sans réponse pour suivre le relais", 2, "CRIME"),
            S("Le coffre vide", "Une saisie spectaculaire ne contient presque rien : quelqu'un a prévenu le réseau.", "Tu reçois ensuite un message moqueur.", "ton informateur éventuel", "identifier la fuite", "le réseau", "donner trois fausses informations différentes", 2, "CRIME"),
            S("L'informateur", "Un lieutenant veut livrer toute une structure criminelle contre l'immunité.", "Il refuse un seul nom avant d'avoir ta parole.", "les futures victimes", "obtenir les noms", "l'informateur", "vérifier un détail avant de promettre quoi que ce soit", 3, "CRIME"),
            S("Protection", "Des commerces paient parce qu'ils pensent que tu ne peux pas être présent chaque nuit.", "Le réseau affirme offrir une sécurité que toi tu ne garantis pas.", "les commerçants", "casser l'extorsion", "le réseau", "organiser une résistance collective discrète", 2, "CRIME"),
            S("Le jeune coursier", "Un adolescent transporte quelque chose pour un groupe dangereux.", "Le chef te provoque en disant que l'arrêter détruira sa vie avant la leur.", "l'adolescent", "remonter au recruteur", "le chef", "laisser le coursier continuer sous surveillance", 3, "CRIME"),
            S("Banque fantôme", "Une série de comptes finance plusieurs groupes rivaux.", "Tous affirment ne pas connaître la source de l'argent.", "les personnes forcées de travailler pour eux", "identifier le financeur", "le commanditaire invisible", "suivre une transaction minuscule plutôt qu'un gros transfert", 2, "CRIME"),
            S("Territoire sans chef", "L'arrestation d'un baron déclenche une guerre de succession.", "Trois groupes te proposent séparément une trêve avantageuse.", "les habitants", "empêcher la guerre", "les prétendants", "faire croire à chacun que les deux autres négocient déjà", 3, "CRIME"),
            S("Le juge acheté", "Une enquête suggère qu'un responsable judiciaire protège un réseau.", "Publier trop tôt pourrait détruire l'affaire entière.", "les témoins", "sécuriser les preuves", "le responsable corrompu", "le pousser à déplacer lui-même les preuves", 3, "CRIME")
        ),
        "POUVOIR" to listOf(
            S("Seconde impulsion", "Ton pouvoir produit un effet que tu n'avais jamais observé.", "Plus tu tentes de le reproduire, plus ton contrôle devient instable.", "les personnes autour", "comprendre la nouvelle capacité", "ton propre pouvoir", "mesurer précisément ce qui déclenche l'effet", 2, "POWER"),
            S("Surcharge publique", "Une intervention pousse ton énergie au-delà de ta limite habituelle.", "Les témoins voient que tu n'es plus totalement maître de toi.", "les civils proches", "stabiliser ton pouvoir", "la surcharge", "décharger l'excès dans un environnement sûr", 3, "POWER"),
            S("Pouvoir muet", "Ta capacité principale disparaît pendant plusieurs heures.", "Un adversaire choisit précisément ce moment pour agir.", "les personnes menacées", "survivre sans ton avantage", "l'adversaire", "utiliser ce qu'il croit être ta dépendance contre lui", 3, "POWER"),
            S("Écho", "Chaque utilisation récente semble renforcer un effet secondaire inquiétant.", "Un spécialiste t'avertit que continuer pourrait rendre le changement permanent.", "ton avenir", "comprendre l'effet secondaire", "le spécialiste", "tester une utilisation minimale plutôt qu'arrêter totalement", 2, "POWER"),
            S("Nouvelle faiblesse", "Une matière banale provoque soudain une réaction anormale.", "Quelqu'un d'autre remarque ton hésitation.", "le secret de ta faiblesse", "confirmer la cause", "le témoin", "feindre une autre raison à ton recul", 3, "POWER"),
            S("Dépendance", "Ton pouvoir réclame de plus en plus une source externe précise.", "La seule réserve disponible appartient à une faction qui le sait.", "ton autonomie", "trouver une alternative", "la faction", "réduire ta consommation avant de négocier", 3, "POWER"),
            S("Deuxième voix", "Ton pouvoir réagit parfois avant ta propre décision consciente.", "Une action automatique sauve quelqu'un mais blesse un adversaire neutralisé.", "ta maîtrise", "comprendre l'automatisme", "ton pouvoir", "reproduire la situation en environnement contrôlé", 3, "POWER"),
            S("Plafond", "Après des mois d'entraînement, ta progression s'arrête brutalement.", "Un mentor affirme que tu dois changer totalement de méthode pour continuer.", "ton potentiel", "franchir le plateau", "le mentor", "travailler la précision plutôt que la puissance", 2, "POWER")
        ),
        "GOUVERNEMENT" to listOf(
            S("Convocation", "Une unité spéciale exige une rencontre officielle.", "Elle affirme que ton absence suffira à justifier une surveillance renforcée.", "ta liberté", "définir ton statut", "l'unité spéciale", "venir avec tes propres conditions écrites", 2, "GOVERNMENT"),
            S("Registre", "Une loi propose l'enregistrement de tous les surhumains actifs.", "Les autorités présentent le refus comme un danger public.", "les identités secrètes", "infléchir le texte", "le gouvernement", "proposer un contrôle indépendant sans identité civile", 3, "GOVERNMENT"),
            S("Mandat", "Un mandat vise un allié surhumain après une opération contestée.", "On t'ordonne de ne pas interférer.", "ton allié", "vérifier les preuves", "l'unité spéciale", "organiser une reddition sous garanties", 3, "GOVERNMENT"),
            S("Immunité", "Le pouvoir exécutif t'offre une protection juridique exceptionnelle.", "En échange, certaines missions deviendraient obligatoires.", "ton indépendance", "obtenir un cadre équitable", "le ministre", "limiter l'accord à une crise précise", 2, "GOVERNMENT"),
            S("Unité miroir", "L'État crée une équipe équipée pour contrer exactement tes capacités.", "Son commandant dit qu'elle n'existe que parce que personne ne peut te garantir éternellement fiable.", "la confiance publique", "comprendre leur doctrine", "le commandant", "participer à un exercice contrôlé", 3, "GOVERNMENT"),
            S("Ordre illégal", "Un responsable te demande officieusement une action qu'il ne veut pas signer.", "Il promet que personne ne saura jamais que tu as obéi.", "les personnes ciblées", "refuser l'abus", "le responsable", "exiger une trace écrite pour le faire reculer", 3, "GOVERNMENT"),
            S("État d'urgence", "Une crise donne au gouvernement des pouvoirs temporaires très larges.", "On te demande publiquement de soutenir ces mesures.", "les libertés civiles", "gérer la crise", "le gouvernement", "soutenir uniquement les mesures limitées dans le temps", 3, "GOVERNMENT"),
            S("Commission", "Une commission publique enquête sur tes interventions passées.", "Des opposants utilisent chaque incident non résolu contre toi.", "ta réputation", "répondre aux faits", "la commission", "admettre une erreur précise plutôt que tout défendre", 2, "GOVERNMENT")
        ),
        "MENTOR" to listOf(
            S("Première leçon", "Une figure expérimentée propose de t'entraîner gratuitement.", "Elle refuse toutefois d'expliquer pourquoi elle s'intéresse à toi.", "ton autonomie", "comprendre ses intentions", "le mentor", "accepter une seule séance test", 2, "MENTOR"),
            S("La méthode interdite", "Ton mentor connaît une technique très efficace mais dangereuse.", "Il te dit que tous les grands protecteurs ont un jour franchi une limite.", "ta maîtrise", "progresser sans te perdre", "le mentor", "décomposer la technique en version non létale", 3, "MENTOR"),
            S("Ancienne faute", "Tu découvres que ton mentor a caché un grave échec de sa carrière.", "Il affirme que cette histoire ne change rien à ce qu'il peut t'apprendre.", "les personnes encore affectées", "comprendre la vérité", "le mentor", "rencontrer un témoin de l'époque", 2, "MENTOR"),
            S("Désaccord", "Ton mentor désapprouve publiquement l'une de tes méthodes.", "Il te demande de choisir entre son enseignement et cette voie.", "votre relation", "définir ta propre doctrine", "le mentor", "lui proposer une mission où vos méthodes seront comparées", 2, "MENTOR"),
            S("Disparition", "Ton mentor manque un rendez-vous et laisse seulement un message cryptique.", "Le message avertit de ne surtout pas venir le chercher.", "ton mentor", "comprendre le danger", "son ravisseur potentiel", "décrypter ce qu'il n'aurait jamais écrit par hasard", 3, "MENTOR"),
            S("Succession", "Ton mentor veut te transmettre un lieu, un réseau ou un symbole.", "Accepter ferait de toi l'héritier de ses alliés comme de ses ennemis.", "son héritage", "décider ce que tu reprends", "le mentor", "accepter le réseau sans reprendre le symbole", 3, "MENTOR"),
            S("Le deuxième élève", "Ton mentor entraîne quelqu'un dont les valeurs s'opposent aux tiennes.", "Il refuse de choisir entre vous.", "le nouvel élève", "éviter une rivalité toxique", "le mentor", "travailler ensemble sur une mission neutre", 2, "MENTOR"),
            S("Dernière limite", "Blessé, ton mentor te demande de terminer une mission qu'il sait presque suicidaire.", "Il affirme qu'il n'y aura pas de seconde chance.", "les personnes menacées", "évaluer la mission", "le mentor", "chercher une troisième option malgré l'urgence", 3, "MENTOR")
        ),
        "CRISE" to listOf(
            S("Panne noire", "Une coupure massive plonge plusieurs quartiers dans le noir.", "Des groupes profitent déjà du chaos pendant qu'un hôpital perd son alimentation.", "les patients", "rétablir une source d'énergie", "les opportunistes", "sécuriser un générateur mobile avant de poursuivre", 3, "CRISIS"),
            S("Ciel ouvert", "Une anomalie lumineuse apparaît au-dessus de la ville et perturbe les pouvoirs.", "Une voix inconnue diffuse ton alias sur toutes les fréquences.", "la population", "comprendre l'anomalie", "la voix", "tester si le signal réagit à ton pouvoir", 3, "COSMIC"),
            S("Évacuation", "Une zone entière doit être évacuée avant une catastrophe probable.", "Le maire refuse d'ordonner l'évacuation sans certitude.", "les habitants", "obtenir une décision rapide", "le maire", "commencer une évacuation volontaire par les zones les plus exposées", 3, "CRISIS"),
            S("Trois fronts", "Trois incidents graves éclatent en même temps dans la ville.", "Chaque faction affirme que son secteur est prioritaire.", "les civils sur les trois sites", "choisir un ordre d'intervention", "les factions", "déléguer selon les compétences plutôt que la loyauté", 3, "CRISIS"),
            S("Le faux désastre", "Une alerte majeure mobilise tous les héros connus.", "Des signaux suggèrent qu'il pourrait s'agir d'une diversion gigantesque.", "la ville", "identifier la vraie cible", "l'organisateur", "garder une réserve cachée", 3, "CRISIS"),
            S("Zone rouge", "Une partie de la ville devient temporairement inaccessible.", "Des civils refusent de partir parce que leurs proches sont encore dedans.", "les familles", "ouvrir un corridor", "la menace", "faire entrer une petite équipe plutôt qu'une évacuation massive", 3, "CRISIS"),
            S("Confiance zéro", "Plusieurs héros et institutions s'accusent mutuellement après une attaque.", "Personne ne veut partager ses informations.", "les prochaines victimes", "réunir les données", "les groupes rivaux", "proposer un échange simultané vérifiable", 3, "CRISIS"),
            S("Dernière heure", "Une menace annonce publiquement une échéance avant une attaque d'échelle nationale.", "Elle exige que toi seul viennes négocier.", "la population", "comprendre l'objectif réel", "la menace", "préparer une équipe invisible sans rompre officiellement les conditions", 3, "CRISIS")
        )
    )

    fun event(c: Campaign): EventNode {
        val eligible = c.threads.filter { c.turn - it.lastTurn >= 2 + it.stage }
        val hookRoll = positiveMod(mix(c.seed xor 0xA11CL, c.turn.toLong()), 100)
        if (eligible.isNotEmpty() && (hookRoll < 58 || c.turn % 5 == 0)) {
            val thread = eligible[positiveMod(mix(c.seed, c.turn.toLong() + 77), eligible.size)]
            return threadEvent(c, thread)
        }
        val index = positiveMod(mix(c.seed, c.turn.toLong() * 31 + 7), 960)
        var category = categories[index % categories.size]
        if (category == c.lastCategory) category = categories[(categories.indexOf(category) + 1 + index % 3) % categories.size]
        val pool = situations.getValue(category)
        val template = pool[(index / categories.size) % pool.size]
        val context = adaptiveContext(c, category)
        return EventNode(
            id = "evt_${index.toString().padStart(3, '0')}",
            title = template.title,
            text = "${template.setup} ${template.provocation} $context",
            choices = contextualChoices(c, category, template, index),
            category = category,
            provocation = template.provocation,
            stakes = template.stakes,
            threadId = template.threadId
        )
    }

    private fun threadEvent(c: Campaign, thread: StoryThread): EventNode {
        val stage = thread.stage + 1
        val past = approachLabel(thread.lastApproach)
        val data = when (thread.id) {
            "RIVAL" -> listOf(
                Triple("Il n'a pas oublié", "Ton rival revient après avoir étudié ta manière de $past. Cette fois il a préparé le terrain pour empêcher exactement cette réponse.", "Il te demande si tu sais faire autre chose que répéter tes habitudes."),
                Triple("Respect ou obsession", "Votre rivalité est devenue assez visible pour attirer des imitateurs et des opportunistes.", "Ton rival propose une trêve : un tiers profite désormais de votre guerre."),
                Triple("Dernier mot", "Une crise oblige enfin chacun à décider si cette rivalité vaut plus que les vies autour.", "Ton rival te laisse le choix : coopération totale, rupture définitive ou confrontation finale.")
            )
            "IDENTITY" -> listOf(
                Triple("La piste revient", "Un détail laissé lors de l'affaire précédente refait surface dans une nouvelle enquête.", "Quelqu'un relie désormais deux événements que tu croyais séparés."),
                Triple("Le cercle se resserre", "Les soupçons ne visent plus seulement ton visage : ils visent ton entourage, tes horaires et tes habitudes.", "Une seule contradiction pourrait confirmer tout le reste."),
                Triple("Un nom à choisir", "La preuve presque complète de ton identité existe désormais dans les mains d'un acteur déterminé.", "Tu peux encore décider qui racontera la vérité et dans quelles conditions.")
            )
            "FAMILY" -> listOf(
                Triple("Ce qui n'a pas été dit", "Ton proche revient sur la décision prise des mois plus tôt. Ce n'est plus l'urgence qu'il juge, mais ce qu'elle disait de ta place dans sa vie.", "Il te demande une réponse qui ne peut plus être reportée."),
                Triple("La limite des proches", "Ta double vie a changé les habitudes de toute ta famille. Certains commencent à agir sans te prévenir pour ne plus dépendre de toi.", "Les protéger signifie peut-être accepter de perdre du contrôle."),
                Triple("Rester ou partir", "Une conséquence directe de tes choix force enfin ta famille à décider si elle peut continuer à vivre dans ton orbite.", "Cette fois il n'existe aucune solution qui conserve tout intact.")
            )
            "GOVERNMENT" -> listOf(
                Triple("Dossier actif", "L'administration ressort ton ancien dossier avec une lecture différente de ta décision précédente.", "Ton comportement passé devient un argument dans une nouvelle négociation."),
                Triple("La doctrine", "Ce qui était une relation personnelle devient une politique officielle à ton sujet.", "On te propose un cadre qui dépend directement de la confiance que tu as construite ou détruite."),
                Triple("Pouvoir contre pouvoir", "Le gouvernement est prêt à institutionnaliser définitivement sa manière de te traiter.", "Coopérer, résister ou imposer un nouveau rapport changera bien plus que ta propre liberté.")
            )
            "FACTION" -> listOf(
                Triple("La dette", "La faction revient réclamer ce qu'elle estime avoir gagné lors de votre première négociation.", "Elle cite précisément ta vieille décision comme précédent."),
                Triple("Ligne de fracture", "Tes choix ont créé deux camps internes : ceux qui te voient comme partenaire et ceux qui te voient comme menace.", "Un vote ou une rupture approche."),
                Triple("Qui dirige qui", "La relation arrive au point où coopération et dépendance deviennent impossibles à distinguer.", "Il faut redéfinir le pacte, absorber la faction ou rompre définitivement.")
            )
            "MENTOR" -> listOf(
                Triple("La seconde leçon", "Ton mentor adapte son enseignement à la manière dont tu avais $past.", "Il te propose maintenant une méthode qui attaque exactement ta faiblesse de caractère."),
                Triple("Dépasser le maître", "Tes progrès rendent le rapport moins vertical. Ton mentor commence à craindre ce que tu feras de ce qu'il t'a transmis.", "Il te demande de prouver que tu n'es pas devenu son erreur."),
                Triple("Héritage", "Le moment arrive où la relation ne peut plus rester celle d'un élève et d'un maître.", "Tu dois choisir ce que tu conserves de lui et ce que tu refuses d'emporter.")
            )
            "CRIME" -> listOf(
                Triple("Ils se sont adaptés", "Le réseau criminel a observé ta précédente méthode et modifié ses habitudes pour la contourner.", "Le chef te fait savoir qu'il avait besoin de toi pour comprendre comment survivre."),
                Triple("Le vide attire", "Chaque structure détruite a laissé de la place à des groupes plus petits et parfois plus violents.", "Ton ancienne victoire devient le problème que tu dois maintenant gérer."),
                Triple("Règle du territoire", "La ville souterraine doit désormais décider si elle négocie avec toi, te combat ou disparaît de ta zone.", "Ta doctrine criminelle ou anticriminelle devient durable.")
            )
            "POWER" -> listOf(
                Triple("Le corps se souvient", "Ton pouvoir reproduit spontanément une conséquence liée à ta précédente prise de risque.", "Ce qui semblait temporaire commence à ressembler à une évolution."),
                Triple("Mutation de méthode", "Ta façon d'utiliser ton pouvoir a créé un nouveau potentiel mais aussi une nouvelle limite.", "Tu peux stabiliser cette branche ou chercher encore plus loin."),
                Triple("Forme définitive", "L'évolution arrive à un point où l'une de tes habitudes peut devenir permanente.", "Choisir la puissance, le contrôle ou l'abandon redéfinira la suite de ta carrière.")
            )
            else -> listOf(
                Triple("Une conséquence revient", "Un ancien choix produit enfin un effet que personne n'avait vu venir.", "Les personnes impliquées se souviennent précisément de ce que tu avais décidé."),
                Triple("Effet domino", "Ce qui semblait local s'est propagé à d'autres acteurs.", "Ta prochaine réponse décidera si cette chaîne s'arrête ou grandit."),
                Triple("Point de non-retour", "Le fil commencé des années plus tôt arrive à sa conclusion.", "Tu ne peux plus préserver toutes les options à la fois.")
            )
        }
        val triple = data[(stage - 1).coerceIn(0, 2)]
        val synthetic = SituationTemplate(triple.first, triple.second, triple.third, "les personnes liées à cette histoire", "résoudre le fil sans effacer son passé", "l'acteur revenu", "utiliser ce que tu sais de la première rencontre", 3, thread.id)
        return EventNode(
            id = "thread_${thread.id.lowercase()}_${stage}_${positiveMod(mix(c.seed, thread.openedTurn.toLong()), 99)}",
            title = triple.first,
            text = "${triple.second} ${triple.third} Tu avais choisi de $past. Le monde s'en souvient.",
            choices = contextualChoices(c, threadToCategory(thread.id), synthetic, c.turn + stage * 71).map { it.copy(threadId = thread.id, stakes = 3) },
            category = threadToCategory(thread.id),
            provocation = triple.third,
            stakes = 3,
            threadId = thread.id,
            threadStage = stage
        )
    }

    private fun adaptiveContext(c: Campaign, category: String): String {
        val local = "À ${c.city}, dans ${c.district},"
        val reputation = when {
            c.fear >= 65 -> " ta réputation de menace te précède : les gens obéissent parfois avant même de comprendre."
            c.opinion >= 45 -> " le public te laisse encore le bénéfice du doute."
            c.opinion <= -35 -> " beaucoup attendent déjà la faute qui confirmera leurs soupçons."
            c.prestige >= 250 -> " chaque geste peut désormais devenir un précédent bien au-delà de la ville."
            else -> " personne ne sait encore quelle doctrine tu veux vraiment incarner."
        }
        val past = if (c.lastApproach.isNotBlank()) " Ton dernier réflexe était de ${approachLabel(c.lastApproach)} ; certains acteurs l'ont remarqué." else ""
        val personal = when (category) {
            "FAMILLE" -> " Ton lien familial est à ${c.familyBond}/100."
            "RIVAL" -> " Ton rapport avec ton rival est à ${c.rivalStanding}/100."
            "GOUVERNEMENT" -> " Ta relation institutionnelle est à ${c.governmentStanding}/100."
            "FACTION" -> " Ta position auprès des factions est à ${c.factionStanding}/100."
            "MÉDIAS" -> " Ta relation avec les médias est à ${c.mediaStanding}/100."
            else -> ""
        }
        return local + reputation + past + personal
    }

    private fun contextualChoices(c: Campaign, category: String, s: SituationTemplate, index: Int): List<Choice> {
        val thread = s.threadId
        val variant = positiveMod(index + c.turn, 4)
        fun phrase(vararg options: String) = options[variant % options.size]
        fun ch(label: String, moral: Int, prestige: Int, opinion: Int, fear: Int, power: Int, impact: Int, risk: Int, approach: String, stakes: Int = s.stakes, relation: Int = 0, flag: String? = null) =
            Choice(label, moral, prestige, opinion, fear, power, impact, risk, approach, stakes, category, thread, relation, flag)

        val base = listOf(
            ch(phrase("Protéger ${s.protectTarget} avant tout", "Mettre ${s.protectTarget} hors de danger d'abord", "Refuser de sacrifier ${s.protectTarget} pour aller plus vite", "Sécuriser ${s.protectTarget}, même si ${s.objective} devient plus difficile"), 7, 3, 5, -1, 0, 4, 2, "PROTECT", relation = 3),
            ch(phrase("Prioriser l'objectif : ${s.objective}", "Ne pas laisser la provocation détourner de l'objectif : ${s.objective}", "Prendre le risque de ${s.objective} immédiatement", "Forcer une résolution rapide : ${s.objective}"), -1, 7, -2, 4, 2, 8, 6, "PURSUE", relation = -1),
            ch(phrase("Faire parler ${s.actor} et gagner du temps", "Négocier avec ${s.actor} sans céder l'essentiel", "Retourner les mots de ${s.actor} contre lui", "Offrir à ${s.actor} une sortie qui évite l'escalade"), 2, 3, 2, -2, 0, 5, 3, "NEGOTIATE", relation = 2),
            ch(phrase("Faire comprendre à ${s.actor} que la provocation a un prix", "Écraser publiquement la position de ${s.actor}", "Imposer ta solution sans demander la permission", "Utiliser la peur pour couper court à ${s.actor}"), -8, 8, -5, 10, 2, 9, 8, "DOMINATE", relation = -5, flag = "used_fear"),
            ch(phrase("${s.tacticalAngle.replaceFirstChar { it.uppercase() }}", "Construire un plan autour de ceci : ${s.tacticalAngle}", "Ne pas répondre frontalement : ${s.tacticalAngle}", "Exploiter le terrain : ${s.tacticalAngle}"), 3, 5, 2, 0, 1, 7, 5, "TACTICAL", relation = 1)
        )
        val special = specialChoice(c, category, s)
        val offset = positiveMod(index / 5, base.size)
        val rotated = base.drop(offset) + base.take(offset)
        return (rotated + special).distinctBy { it.label }.take(6)
    }

    private fun specialChoice(c: Campaign, category: String, s: SituationTemplate): Choice {
        fun choice(label: String, moral: Int, prestige: Int, opinion: Int, fear: Int, power: Int, impact: Int, risk: Int, approach: String, relation: Int = 0, flag: String? = null) =
            Choice(label, moral, prestige, opinion, fear, power, impact, risk, approach, 3, category, s.threadId, relation, flag)
        return when {
            category == "IDENTITÉ" && c.identityExposure >= 65 -> choice("Abandonner ton ancienne identité civile et reprendre le contrôle publiquement", 2, 10, 3, 2, 0, 9, 8, "REVEAL", -2, "public_identity")
            category == "RIVAL" && c.rivalStanding >= 25 -> choice("Faire confiance à ton rival sur une partie critique du plan", 5, 6, 5, -3, 0, 9, 5, "TRUST", 8, "rival_trusted")
            category == "GOUVERNEMENT" && c.governmentStanding >= 25 -> choice("Activer ton contact officiel et imposer une procédure déjà négociée", 2, 6, 4, -2, 0, 8, 2, "COOPERATE", 5, "gov_protocol")
            category == "FAMILLE" && c.familyBond >= 70 -> choice("Dire toute la vérité, y compris ce que tu avais juré de cacher", 7, 1, 3, -2, 0, 5, 5, "REVEAL", 8, "family_knows")
            category == "POUVOIR" && c.control >= 60 -> choice("Utiliser ${c.powerFamily} avec une précision que tu n'aurais pas osée plus tôt", 3, 7, 4, 0, 3, 9, 5, "PRECISION", 0, "mastery_breakthrough")
            c.motivation == "Pouvoir" -> choice("Transformer cette crise en démonstration de ${c.powerFamily}", -5, 10, -3, 8, 4, 10, 9, "AMBITION", -2, "power_first")
            c.motivation == "Protéger les miens" && category == "FAMILLE" -> choice("Tout sacrifier pour que ${s.protectTarget} ne paie pas le prix de ta mission", 9, -3, 4, -2, 0, 2, 4, "SACRIFICE", 10, "family_first")
            c.socialBackground == "Milieu scientifique" || c.origin.contains("scientifique", true) -> choice("Construire une réponse expérimentale à partir de ce que tu sais déjà", 2, 5, 2, 0, 2, 8, 6, "ANALYZE", 1, "science_solution")
            else -> choice("Utiliser ${c.powerFamily} d'une manière directement adaptée à ${s.objective}", 1, 7, 1, 2, 3, 9, 7, "POWER_PLAY", 0, "signature_move")
        }
    }

    fun choose(c: Campaign, choice: Choice): Campaign {
        val roll = outcomeRoll(c, choice)
        val stakes = choice.stakes.coerceIn(1, 3)
        val danger = max(0, choice.risk + stakes - c.control / 18)
        val injury = if (roll < danger * 4) 6 + danger * 2 else 0
        val casualties = when {
            choice.approach == "DOMINATE" && roll < 28 -> 1 + roll % 3
            choice.approach in setOf("POWER_PLAY", "AMBITION") && roll < 16 -> 1
            choice.approach == "PROTECT" -> 0
            else -> 0
        }
        val exposureBase = if (choice.approach in setOf("REVEAL", "POWER_PLAY", "AMBITION")) 8 else if (roll < choice.risk * 3) 2 + choice.risk else 0
        val weight = when (stakes) { 3 -> 2; 2 -> 1; else -> 1 }
        val nextTurn = c.turn + 1
        val controlDelta = when (choice.approach) {
            "PRECISION", "ANALYZE" -> 3
            "TACTICAL", "PROTECT", "COOPERATE" -> 1
            "DOMINATE", "AMBITION" -> -1
            else -> 0
        }
        var flags = c.flags + listOfNotNull(choice.flag)
        var threads = c.threads
        val tid = choice.threadId
        if (tid != null) {
            val existing = threads.firstOrNull { it.id == tid }
            if (existing == null) {
                threads = (threads + StoryThread(tid, c.turn, c.turn, 0, choice.approach, stakes)).takeLast(6)
                flags = flags + "thread_opened:$tid"
            } else if (existing.stage >= 2) {
                threads = threads.filterNot { it.id == tid }
                flags = flags + "thread_resolved:$tid:${choice.approach}"
            } else {
                threads = threads.map {
                    if (it.id == tid) it.copy(lastTurn = c.turn, stage = it.stage + 1, lastApproach = choice.approach, intensity = max(it.intensity, stakes)) else it
                }
            }
        }
        val relation = choice.relationDelta * weight
        val family = clamp(c.familyBond + if (choice.sourceCategory == "FAMILLE") relation else if (choice.approach == "SACRIFICE") 5 else 0, 0, 100)
        val rival = clamp(c.rivalStanding + if (choice.sourceCategory == "RIVAL") relation else 0, -100, 100)
        val government = clamp(c.governmentStanding + if (choice.sourceCategory == "GOUVERNEMENT") relation else if (choice.approach == "DOMINATE") -2 else 0, -100, 100)
        val faction = clamp(c.factionStanding + if (choice.sourceCategory == "FACTION") relation else 0, -100, 100)
        val media = clamp(c.mediaStanding + if (choice.sourceCategory == "MÉDIAS") relation else if (choice.approach == "REVEAL") 2 else 0, -100, 100)
        val summary = "${c.age} ans — ${choice.label} [enjeu ${stakes}/3]"
        return c.copy(
            turn = nextTurn,
            morality = clamp(c.morality + choice.moral * weight, -100, 100),
            prestige = max(0, c.prestige + choice.prestige * weight + c.scope.ordinal * stakes),
            opinion = clamp(c.opinion + choice.opinion * weight, -100, 100),
            fear = clamp(c.fear + choice.fear * weight, 0, 100),
            power = clamp(c.power + choice.power + if (nextTurn % 10 == 0) 1 else 0, 0, 100),
            control = clamp(c.control + controlDelta, 0, 100),
            influence = max(0, c.influence + choice.impact * weight + max(0, c.prestige / 180)),
            health = clamp(c.health - injury, 0, 100),
            civilianCasualties = c.civilianCasualties + casualties,
            identityExposure = clamp(c.identityExposure + exposureBase, 0, 100),
            familyBond = family,
            rivalStanding = rival,
            governmentStanding = government,
            factionStanding = faction,
            mediaStanding = media,
            flags = flags,
            threads = threads,
            lastCategory = choice.sourceCategory,
            lastApproach = choice.approach,
            timeline = (c.timeline + summary).takeLast(120)
        )
    }

    fun resolve(c: Campaign, event: EventNode, choice: Choice): Resolution {
        val next = choose(c, choice)
        val roll = outcomeRoll(c, choice)
        val injury = c.health - next.health
        val casualties = next.civilianCasualties - c.civilianCasualties
        val exposure = next.identityExposure - c.identityExposure
        val opener = categoryReaction(event.category, choice.approach, roll)
        val echo = if (event.threadStage > 0) " Ce moment existe parce qu'une ancienne décision n'a jamais vraiment disparu." else if (choice.threadId != null) " Sans le savoir, tu viens d'ouvrir un fil qui pourra revenir plus tard." else ""
        val consequence = when {
            casualties > 0 -> "$casualties victime${if (casualties > 1) "s" else ""} civile${if (casualties > 1) "s sont" else " est"} désormais liée${if (casualties > 1) "s" else ""} à cette décision. Ce coût ne sera pas effacé par une future bonne action."
            injury > 0 -> "Tu obtiens un résultat, mais ton corps paie $injury points de dégâts. Ta faiblesse devient une donnée que d'autres peuvent apprendre."
            exposure > 0 -> "L'action laisse une trace exploitable : ton exposition d'identité augmente de $exposure points."
            choice.stakes == 3 && roll >= 70 -> "La décision réussit à grande échelle. Elle ne donne pas seulement des points : elle devient un précédent que factions, médias et adversaires pourront citer."
            choice.stakes == 3 && roll <= 25 -> "Le choix était majeur et son exécution imparfaite. Tu conserves sa direction morale, mais le monde retiendra aussi ce qui a échappé à ton contrôle."
            choice.approach == "DOMINATE" -> "Ta volonté s'impose. Ceux qui restent silencieux ne sont pas forcément convaincus : certains commencent à préparer la prochaine fois où ils devront te résister."
            choice.approach in setOf("PROTECT", "SACRIFICE") -> "Le résultat est moins spectaculaire qu'une victoire absolue, mais les personnes protégées deviennent une mémoire vivante de ta priorité."
            choice.approach in setOf("TACTICAL", "ANALYZE", "PRECISION") -> "Ta méthode crée une information durable : la prochaine fois qu'une situation similaire arrivera, tu ne partiras plus de zéro."
            roll >= 75 -> "L'exécution est presque parfaite. Cette réussite renforce ta doctrine — et pousse aussi tes adversaires à l'étudier."
            roll <= 20 -> "Le plan ne fonctionne qu'en partie. Les conséquences existent tout de même, mais elles prendront une forme moins contrôlée."
            else -> "La situation se ferme sans solution propre. Tu as gagné quelque chose et abandonné autre chose, exactement le genre de compromis dont une carrière finit par être faite."
        }
        val relationEcho = relationReaction(next, event.category)
        val outcome = "$opener $consequence $relationEcho$echo"
        return Resolution(next.copy(timeline = (next.timeline + "↳ $outcome").takeLast(120)), outcome)
    }

    private fun relationReaction(c: Campaign, category: String): String = when (category) {
        "FAMILLE" -> when { c.familyBond >= 75 -> "Ta famille te fait davantage confiance, mais cette confiance te rend aussi plus responsable de ce que tu lui caches."; c.familyBond <= 25 -> "Le lien familial est désormais fragile : une prochaine crise pourra devenir une rupture."; else -> "Rien n'est réglé entre vous, mais le rapport a changé." }
        "RIVAL" -> when { c.rivalStanding >= 35 -> "Ton rival commence à te respecter assez pour qu'une vraie alliance devienne possible."; c.rivalStanding <= -35 -> "La rivalité devient personnelle. Il ne cherchera plus seulement à gagner : il cherchera à te contredire."; else -> "Votre rivalité reste ouverte, sans confiance ni guerre totale." }
        "GOUVERNEMENT" -> when { c.governmentStanding >= 30 -> "Les institutions commencent à te traiter comme un interlocuteur plutôt que comme un dossier."; c.governmentStanding <= -30 -> "Le contrôle institutionnel va probablement se durcir autour de toi."; else -> "La relation officielle reste négociable." }
        "FACTION" -> when { c.factionStanding >= 30 -> "Une partie des factions te voit maintenant comme un partenaire possible."; c.factionStanding <= -30 -> "Des groupes qui n'étaient pas tes ennemis commencent à coordonner leur méfiance."; else -> "L'équilibre des alliances reste instable." }
        "MÉDIAS" -> when { c.mediaStanding >= 30 -> "Certains médias te donnent désormais le bénéfice du doute avant de publier."; c.mediaStanding <= -30 -> "Le prochain incident sera interprété dans le pire sens avant même les faits."; else -> "Le récit public reste disputé." }
        else -> ""
    }

    private fun categoryReaction(category: String, approach: String, roll: Int): String {
        val base = when (category) {
            "RUE" -> listOf("La rue réagit immédiatement.", "Le quartier enregistre ta méthode.", "Les témoins comprennent quel genre de présence tu veux devenir.")
            "IDENTITÉ" -> listOf("La bataille se déplace vers l'information.", "Ton secret change de forme plutôt que de disparaître.", "Quelqu'un vient d'apprendre quelque chose sur ta manière de protéger ton identité.")
            "FAMILLE" -> listOf("La conséquence la plus lourde n'est pas publique.", "Tes proches retiennent la place que tu leur as donnée.", "La double vie vient de produire un coût réel.")
            "MÉDIAS" -> listOf("Le récit public se réécrit presque en direct.", "Quelques phrases déplacent l'opinion.", "Les caméras transforment ton choix en symbole.")
            "FACTION" -> listOf("Les factions recalculent leur rapport de force.", "Ton choix devient un signal pour des groupes absents.", "L'équilibre entre coopération et menace bouge.")
            "RIVAL" -> listOf("Ton rival enregistre la réponse autant que le résultat.", "Votre rivalité vient de gagner une nouvelle règle.", "Ce moment comptera lors de votre prochaine rencontre.")
            "SAUVETAGE" -> listOf("Les secondes perdues et gagnées deviennent tout ce qui compte.", "Les secours adaptent leur plan à toi.", "Les survivants ne verront jamais tout ton raisonnement, seulement ses conséquences.")
            "CRIME" -> listOf("Le réseau criminel modifie déjà ses habitudes.", "La rue souterraine apprend vite ce que tu punis.", "Ta méthode circule plus vite que les arrestations.")
            "POUVOIR" -> listOf("Ton pouvoir te donne une réponse que tu n'étais pas certain de vouloir.", "Ton corps mémorise cette limite.", "Ce que tu viens d'apprendre changera tes prochains risques.")
            "GOUVERNEMENT" -> listOf("Les institutions classent ton comportement dans une nouvelle catégorie.", "Un dossier vient probablement de gagner plusieurs pages.", "Ta relation au pouvoir légal devient plus claire.")
            "MENTOR" -> listOf("La leçon dépasse la technique.", "Ton rapport à la transmission se précise.", "Quelqu'un sait maintenant jusqu'où tu es prêt à aller pour progresser.")
            else -> listOf("La crise change d'échelle autour de toi.", "Les autres acteurs réorganisent leurs priorités.", "Dans le chaos, ton choix devient une référence.")
        }
        val bias = if (approach == "DOMINATE") 1 else if (roll > 70) 2 else 0
        return base[(roll + bias) % base.size]
    }

    fun legacyTitle(c: Campaign): String {
        val heroic = c.morality >= 25
        return when (c.scope) {
            Scope.STREET -> if (heroic) "Gardien de la rue" else "Prédateur local"
            Scope.DISTRICT -> if (heroic) "Protecteur du quartier" else "Terreur du quartier"
            Scope.CITY -> if (heroic) "Gardien métropolitain" else "Fléau métropolitain"
            Scope.REGION -> if (heroic) "Défenseur régional" else "Seigneur criminel"
            Scope.COUNTRY -> if (heroic) "Symbole de la nation" else "Ennemi public national"
            Scope.WORLD -> if (heroic) "Gardien de la Terre" else "Ennemi de l'humanité"
        }
    }

    fun legacyScore(c: Campaign): Int = max(0, c.prestige + c.influence / 2 + c.power * 2 + abs(c.morality) * 2 + c.turn + c.threads.size * 15 - c.civilianCasualties * 2)

    private fun threadToCategory(id: String): String = when (id) {
        "IDENTITY" -> "IDENTITÉ"; "FAMILY" -> "FAMILLE"; "MEDIA" -> "MÉDIAS"; "FACTION" -> "FACTION"; "RIVAL" -> "RIVAL"; "CRIME" -> "CRIME"; "POWER" -> "POUVOIR"; "GOVERNMENT" -> "GOUVERNEMENT"; "MENTOR" -> "MENTOR"; "RESCUE" -> "SAUVETAGE"; "STREET" -> "RUE"; else -> "CRISE"
    }

    private fun approachLabel(a: String): String = when (a) {
        "PROTECT" -> "protéger avant de gagner"; "PURSUE" -> "poursuivre l'objectif"; "NEGOTIATE" -> "négocier"; "DOMINATE" -> "imposer ta volonté"; "TACTICAL" -> "contourner le problème"; "REVEAL" -> "reprendre le contrôle par la vérité"; "TRUST" -> "faire confiance"; "SACRIFICE" -> "te sacrifier pour les autres"; "PRECISION" -> "privilégier la maîtrise"; "ANALYZE" -> "analyser avant d'agir"; else -> "agir selon ta propre méthode"
    }

    private fun outcomeRoll(c: Campaign, choice: Choice): Int = positiveMod(mix(c.seed xor 0x5EEDL, c.turn.toLong() * 17 + choice.label.hashCode()), 100)
    private fun positiveMod(v: Long, mod: Int): Int = ((v and Long.MAX_VALUE) % mod.toLong()).toInt()
    private fun clamp(v: Int, low: Int, high: Int) = min(high, max(low, v))
    private fun S(title: String, setup: String, provocation: String, protect: String, objective: String, actor: String, tactical: String, stakes: Int, thread: String?) = SituationTemplate(title, setup, provocation, protect, objective, actor, tactical, stakes, thread)

    private fun mix(a: Long, b: Long): Long {
        var z = a + 0x9E3779B97F4A7C15UL.toLong() + b * 0xBF58476D1CE4E5B9UL.toLong()
        z = (z xor (z ushr 30)) * 0xBF58476D1CE4E5B9UL.toLong()
        z = (z xor (z ushr 27)) * 0x94D049BB133111EBUL.toLong()
        return z xor (z ushr 31)
    }
}
