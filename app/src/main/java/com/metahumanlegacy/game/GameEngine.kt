package com.metahumanlegacy.game

import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class Scope(val label: String) { STREET("Rue"), DISTRICT("Quartier"), CITY("Ville"), REGION("Région"), COUNTRY("Pays"), WORLD("Monde") }

data class Choice(
    val label: String,
    val moral: Int,
    val prestige: Int,
    val opinion: Int,
    val fear: Int,
    val power: Int,
    val impact: Int,
    val risk: Int,
    val approach: String = "PRAGMATIC"
)

data class EventNode(
    val id: String,
    val title: String,
    val text: String,
    val choices: List<Choice>,
    val category: String,
    val provocation: String = ""
)

data class Resolution(val campaign: Campaign, val outcome: String)

private data class SituationTemplate(
    val title: String,
    val setup: String,
    val provocation: String
)

data class Campaign(
    val seed: Long,
    val name: String,
    val alias: String,
    val origin: String,
    val powerFamily: String,
    val weakness: String,
    val modifier: String,
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
    val finished: Boolean get() = turn >= 120 || health <= 0
}

object GameEngine {
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
    private val weaknesses = listOf(
        "Surcharge", "Fatigue extrême", "Concentration", "Énergie externe", "Fréquence sonore",
        "Instabilité émotionnelle", "Temps de récupération", "Environnement", "Vulnérabilité psychique",
        "Vulnérabilité mystique", "Pouvoir difficile à dissimuler", "Précision limitée"
    )
    private val modifiers = listOf(
        "Âge des héros", "Première génération", "Société méfiante", "Culture héroïque", "État autoritaire",
        "Criminalité endémique", "Ère technologique", "Menace occulte", "Silence cosmique", "Médias omniprésents"
    )
    private val categories = listOf("RUE", "IDENTITÉ", "FAMILLE", "MÉDIAS", "FACTION", "RIVAL", "SAUVETAGE", "CRIME", "POUVOIR", "GOUVERNEMENT", "MENTOR", "CRISE")

    private val situations = mapOf(
        "RUE" to listOf(
            SituationTemplate("La caisse et la fumée", "Un incendie dévore une supérette. Deux personnes sont encore à l'étage pendant qu'un homme masqué vide la caisse.", "Le braqueur te voit et hurle que tu n'auras jamais le temps de sauver les otages et de l'arrêter."),
            SituationTemplate("Le mauvais trottoir", "Une bande encercle un adolescent qu'elle accuse d'avoir parlé à la police. Les riverains filment derrière leurs fenêtres.", "Le meneur te provoque : si tu interviens, il promet que le quartier paiera dès que tu auras le dos tourné."),
            SituationTemplate("Collision", "Un véhicule volé percute un arrêt de bus et poursuit sa fuite avec un enfant encore coincé sous l'abri métallique.", "Le conducteur ralentit juste assez pour te faire comprendre qu'il veut que tu choisisses entre lui et la victime."),
            SituationTemplate("La dette du bloc", "Un commerçant protégé par un gang refuse de payer. Les hommes venus le punir n'ont pas encore sorti leurs armes.", "Ils affirment devant tout le monde que ta présence ne changera rien à leurs règles."),
            SituationTemplate("Après le match", "Une bagarre de rue dégénère en mouvement de foule. Un individu profite du chaos pour poignarder quelqu'un puis disparaît.", "Ses amis te désignent une fausse piste en riant de ta naïveté."),
            SituationTemplate("Le toit d'en face", "Un tireur improvisé menace une place depuis un toit, mais la foule ignore encore d'où vient le danger.", "Il t'envoie un message : montre-toi ou il choisira quelqu'un au hasard.")
        ),
        "IDENTITÉ" to listOf(
            SituationTemplate("Le visage flou", "Une vidéo améliore image par image une silhouette qui pourrait être ton identité civile.", "Le compte qui la diffuse promet un visage net dans une heure si tu ne réponds pas publiquement."),
            SituationTemplate("Une photo de trop", "Un photographe possède une image de toi entrant blessé dans un lieu lié à ta vie civile.", "Il te demande une exclusivité et prétend que le public a le droit de savoir."),
            SituationTemplate("Le faux témoin", "Une personne affirme à la police t'avoir reconnu sans masque après une intervention.", "Elle donne des détails crédibles, dont certains ne devraient être connus de personne."),
            SituationTemplate("Dossier croisé", "Une base privée relie tes horaires, tes blessures et tes apparitions publiques avec une précision inquiétante.", "Son propriétaire t'offre d'effacer le résultat contre un service futur."),
            SituationTemplate("Chez toi", "Quelqu'un a déposé devant ta porte un symbole utilisé par tes adversaires.", "Aucun message, seulement la preuve que ton refuge n'est peut-être plus secret."),
            SituationTemplate("Le proche interrogé", "Un journaliste approche un de tes proches avec des questions beaucoup trop précises.", "Il lui montre une photo où l'on distingue clairement un détail de ton équipement.")
        ),
        "FAMILLE" to listOf(
            SituationTemplate("Deux urgences", "Un proche t'attend pour un moment important au moment exact où une alerte apparaît à quelques rues.", "Il te dit que cette fois, partir aura une conséquence entre vous."),
            SituationTemplate("Ce que tu caches", "Un membre de ta famille découvre du sang et du matériel dissimulés chez toi.", "Il refuse une nouvelle excuse et exige la vérité maintenant."),
            SituationTemplate("La cible facile", "Une menace anonyme cite le prénom d'un proche sans demander d'argent.", "Le message dit seulement : nous voulons savoir ce que tu es prêt à sacrifier."),
            SituationTemplate("La promesse", "Tu avais juré de ne plus utiliser tes pouvoirs devant ta famille, mais un accident survient sous leurs yeux.", "Une seconde d'hésitation peut suffire à laisser quelqu'un mourir."),
            SituationTemplate("Partir loin", "Tes proches envisagent de quitter la ville à cause de ce que ta vie attire autour d'eux.", "Ils te demandent si tu les suivrais vraiment si c'était le seul moyen de les protéger."),
            SituationTemplate("Le mensonge utile", "La police questionne un proche après une de tes interventions.", "Il peut te couvrir, mais cela l'obligerait à mentir officiellement pour toi.")
        ),
        "MÉDIAS" to listOf(
            SituationTemplate("Direct national", "Une chaîne te propose un direct sans montage après une intervention controversée.", "Le présentateur annonce déjà qu'il te demandera si tu te crois au-dessus de la loi."),
            SituationTemplate("Le montage", "Une séquence virale coupe les secondes où tu sauves des civils et ne montre que ton attaque finale.", "Le journaliste refuse de corriger tant que tu ne viens pas défendre ta version sur son plateau."),
            SituationTemplate("La victime parle", "Une personne sauvée par toi raconte publiquement qu'elle a eu plus peur de toi que de l'agresseur.", "Ses mots deviennent un symbole utilisé contre les surhumains."),
            SituationTemplate("Offre d'exclusivité", "Un média te propose de contrôler presque entièrement ton image pendant six mois.", "En échange, il veut un accès privilégié à tes interventions et à une partie de ta vie privée."),
            SituationTemplate("Question piège", "Après une catastrophe, tous les micros se tournent vers toi avant même que les secours aient fini leur travail.", "Une journaliste te demande de nommer immédiatement un responsable sans preuve complète."),
            SituationTemplate("Le héros fabriqué", "Un réseau transforme une intervention banale en récit héroïque exagéré et ton prestige explose.", "Accepter le récit serait utile, mais plusieurs détails sont faux.")
        ),
        "FACTION" to listOf(
            SituationTemplate("Contrat sans signature", "Une organisation surhumaine t'offre renseignements, refuge et équipement.", "Elle refuse pourtant de te dire ce qu'elle attendra de toi le jour où elle appellera sa dette."),
            SituationTemplate("Territoire partagé", "Une faction te demande de ne plus intervenir dans deux quartiers qu'elle prétend protéger.", "Son émissaire te prévient qu'un refus sera considéré comme une déclaration de guerre."),
            SituationTemplate("L'ennemi de mon ennemi", "Un groupe que tu méprises possède la seule piste crédible vers une menace plus dangereuse.", "Il n'acceptera de parler qu'en échange d'une opération commune visible publiquement."),
            SituationTemplate("Place vide", "Une faction rivale vient de perdre son chef et plusieurs membres veulent te rallier.", "Ils attendent que tu imposes immédiatement tes règles ou ils choisiront quelqu'un d'autre."),
            SituationTemplate("Le traître utile", "Un membre ennemi propose des dossiers internes contre une protection personnelle.", "Il reconnaît avoir participé à des opérations ayant fait des victimes civiles."),
            SituationTemplate("La réunion", "Trois groupes acceptent une rencontre neutre pour éviter une escalade.", "L'un d'eux arrive armé et prétend que c'est uniquement par respect pour ta réputation.")
        ),
        "RIVAL" to listOf(
            SituationTemplate("Même rue, deux règles", "Un autre surhumain intervient brutalement dans une zone où tu agis souvent.", "Devant les témoins, il te traite d'amateur et te demande de dégager."),
            SituationTemplate("Le défi", "Un rival publie l'heure et le lieu où il dit pouvoir prouver qu'il est plus puissant que toi.", "Il promet de considérer ton absence comme un aveu de faiblesse."),
            SituationTemplate("Victoire volée", "Un rival revendique publiquement le mérite d'une opération que tu as préparée.", "Il sait que tu ne peux le contredire sans révéler des informations sensibles."),
            SituationTemplate("Sauvetage concurrent", "Vous arrivez en même temps sur une catastrophe et vos méthodes sont incompatibles.", "Il te somme de suivre ses ordres devant les civils."),
            SituationTemplate("Faiblesse connue", "Un rival comprend enfin ce qui limite ton pouvoir.", "Il te souffle qu'il gardera le secret uniquement tant que tu ne l'humilies plus publiquement."),
            SituationTemplate("Alliance impossible", "Une menace dépasse manifestement chacun de vous pris séparément.", "Ton rival accepte de coopérer mais exige de diriger l'opération.")
        ),
        "SAUVETAGE" to listOf(
            SituationTemplate("L'escalier qui cède", "Un immeuble menace de s'effondrer avec plusieurs familles encore à l'intérieur.", "À l'extérieur, le responsable de l'explosion tente de profiter de chaque seconde pour fuir."),
            SituationTemplate("Sous le fleuve", "Un véhicule collectif sombre lentement alors qu'une autre victime appelle depuis la rive.", "Tu comprends immédiatement que ton pouvoir ne te permettra pas d'être partout à la fois."),
            SituationTemplate("Le mauvais étage", "Un incendie hospitalier coupe deux ailes et les informations des secours se contredisent.", "Une infirmière te supplie d'ignorer le protocole et de la suivre."),
            SituationTemplate("Pont suspendu", "Une portion de pont s'affaisse avec des véhicules encore dessus.", "Un criminel impliqué dans l'accident est lui aussi coincé et réclame ton aide en premier."),
            SituationTemplate("Respirer", "Une fuite toxique se répand dans un métro bondé.", "Fermer les accès ralentirait le nuage mais enfermerait des dizaines de personnes à l'intérieur."),
            SituationTemplate("La foule contre toi", "Après une explosion, les civils paniquent et bloquent l'arrivée des secours.", "Plusieurs personnes refusent de suivre tes instructions parce qu'elles ne te font pas confiance.")
        ),
        "CRIME" to listOf(
            SituationTemplate("Test de territoire", "Un réseau criminel multiplie les petits délits coordonnés pour mesurer ton temps de réaction.", "Son chef fait diffuser que tu es prévisible et donc contrôlable."),
            SituationTemplate("Le coffre vide", "Une saisie spectaculaire ne contient presque rien : quelqu'un a prévenu le réseau.", "Quelques minutes plus tard, tu reçois un message moqueur depuis un numéro jetable."),
            SituationTemplate("L'informateur", "Un lieutenant propose de livrer toute une structure criminelle contre l'immunité.", "Il refuse de donner un seul nom avant d'avoir ta parole."),
            SituationTemplate("Protection", "Des commerces paient un réseau parce qu'ils pensent que tu ne peux pas être présent chaque nuit.", "Le collecteur te dit en face que la peur est plus fiable que ta protection."),
            SituationTemplate("Le convoi", "Un transfert clandestin traverse la ville avec plusieurs véhicules identiques.", "Une fausse interception pourrait provoquer des représailles immédiates sur des civils."),
            SituationTemplate("Le successeur", "Après l'arrestation d'un chef, un groupe plus violent reprend son territoire.", "Il annonce que ta précédente victoire a seulement éliminé quelqu'un de raisonnable.")
        ),
        "POUVOIR" to listOf(
            SituationTemplate("Trop fort", "Ton pouvoir augmente brutalement mais ta précision chute.", "Chaque seconde où tu continues d'agir peut sauver du temps ou créer un accident impossible à cacher."),
            SituationTemplate("Le seuil", "Tu découvres que ta limite habituelle n'était peut-être qu'un verrou mental.", "La dépasser maintenant pourrait te donner l'avantage sans garantie de retour à la normale."),
            SituationTemplate("Réaction inconnue", "Ton pouvoir réagit à une matière ou une énergie jamais rencontrée auparavant.", "L'effet est spectaculaire et attire déjà l'attention de plusieurs témoins."),
            SituationTemplate("Absence", "Pendant quelques secondes, ton pouvoir disparaît complètement au milieu d'une intervention.", "Ton adversaire s'en rend compte avant les civils."),
            SituationTemplate("Écho", "Une utilisation intense semble provoquer un effet secondaire sur ton environnement.", "Tu peux arrêter immédiatement ou pousser encore pour comprendre le phénomène."),
            SituationTemplate("Copie imparfaite", "Quelqu'un reproduit une version instable de ton pouvoir grâce à une technologie inconnue.", "Il te provoque en affirmant qu'il peut devenir une meilleure version de toi.")
        ),
        "GOUVERNEMENT" to listOf(
            SituationTemplate("Convocation", "Une unité spéciale exige une rencontre officielle après plusieurs incidents surhumains.", "Le responsable te prévient qu'un refus sera interprété comme une menace potentielle."),
            SituationTemplate("Registre", "Un projet impose l'enregistrement confidentiel des individus augmentés.", "On te propose un traitement privilégié si tu acceptes de donner l'exemple publiquement."),
            SituationTemplate("Ordre direct", "Les autorités te demandent de ne pas intervenir sur une opération en cours.", "Tu disposes pourtant d'informations indiquant que leur plan sous-estime gravement le danger."),
            SituationTemplate("Dossier classé", "Tu apprends qu'un service possède depuis longtemps des informations sur ton origine.", "Il accepte de t'en montrer une partie seulement si tu signes un accord de coopération."),
            SituationTemplate("Bouc émissaire", "Après une crise, un responsable politique te désigne comme cause principale sans preuve décisive.", "Ses conseillers te proposent discrètement de calmer l'affaire si tu ne le contredis pas."),
            SituationTemplate("Ligne rouge", "Une unité anti-surhumaine s'installe dans ta zone avec des moyens capables de te neutraliser.", "Son commandant affirme qu'il ne te vise pas, tant que tu restes prévisible.")
        ),
        "MENTOR" to listOf(
            SituationTemplate("Le survivant", "Une figure expérimentée propose de t'entraîner à contrôler ton pouvoir.", "Sa réputation inclut pourtant des opérations dont personne ne veut parler clairement."),
            SituationTemplate("Méthode brutale", "Ton mentor potentiel affirme que tu ne progresseras qu'en combattant sans retenue contre lui.", "Il refuse toute protection autour de la zone d'entraînement."),
            SituationTemplate("Secret de maîtrise", "Quelqu'un connaît une technique qui pourrait compenser ta faiblesse principale.", "Il demande en échange que tu lui enseignes une capacité qu'il ne possède pas."),
            SituationTemplate("Le vieux dossier", "Tu découvres que ton mentor a déjà entraîné un adversaire actuel.", "Il prétend avoir gardé ce fait secret uniquement pour ne pas influencer ton jugement."),
            SituationTemplate("Succession", "Une figure respectée veut faire de toi son successeur public.", "Accepter t'apporterait des alliés mais aussi tous ses ennemis."),
            SituationTemplate("Leçon interdite", "Ton mentor propose une technique extrêmement efficace mais moralement douteuse.", "Il affirme que tous les grands protecteurs finissent un jour par franchir cette ligne.")
        ),
        "CRISE" to listOf(
            SituationTemplate("Trois quartiers", "Une anomalie frappe plusieurs quartiers à la fois et les services d'urgence se dispersent.", "Chaque zone affirme être prioritaire et aucune information n'est totalement fiable."),
            SituationTemplate("Black-out", "Une panne massive plonge la ville dans le noir pendant qu'une série d'incidents démarre presque simultanément.", "Quelqu'un diffuse que l'effondrement est volontaire et que tu en connais la cause."),
            SituationTemplate("Évacuation", "Une menace incertaine force les autorités à envisager l'évacuation de dizaines de milliers de personnes.", "Une fausse alerte créerait elle-même une catastrophe économique et humaine."),
            SituationTemplate("Ciel rouge", "Un phénomène inexpliqué apparaît au-dessus de la région et perturbe communications et pouvoirs.", "Plusieurs factions te demandent en même temps de prendre position sur son origine."),
            SituationTemplate("La seconde attaque", "Alors que tous les secours sont mobilisés sur une catastrophe, une attaque coordonnée démarre ailleurs.", "Tu comprends que la première crise était probablement une diversion."),
            SituationTemplate("Qui commande ?", "Police, armée, secours et surhumains convergent sur le même désastre sans chaîne de commandement commune.", "Deux responsables différents te donnent des ordres incompatibles devant leurs équipes.")
        )
    )

    fun newCampaign(seed: Long, randomIdentity: Boolean = true): Campaign {
        val r = Random(seed)
        val firstNames = listOf("Malik", "Nora", "Elias", "Maya", "Soren", "Lina", "Ilyan", "Kael", "Naël", "Ava")
        val lastNames = listOf("Voss", "Deren", "Kess", "Arden", "Vale", "Nox", "Raine", "Sol", "Marek", "Serrin")
        val name = if (randomIdentity) "${firstNames[r.nextInt(firstNames.size)]} ${lastNames[r.nextInt(lastNames.size)]}" else "Alex Vesper"
        val aliasRoots = listOf("Vesper", "Axiom", "Morrow", "Cipher", "Silex", "Halo", "Noctis", "Vector", "Rift", "Cinder")
        return Campaign(
            seed = seed,
            name = name,
            alias = aliasRoots[r.nextInt(aliasRoots.size)],
            origin = origins[r.nextInt(origins.size)],
            powerFamily = powers[r.nextInt(powers.size)],
            weakness = weaknesses[r.nextInt(weaknesses.size)],
            modifier = modifiers[r.nextInt(modifiers.size)],
            power = 22 + r.nextInt(20),
            control = 18 + r.nextInt(28)
        )
    }

    fun event(c: Campaign): EventNode {
        val index = ((mix(c.seed, c.turn.toLong()) ushr 1) % 660L).toInt()
        val category = categories[index % categories.size]
        val pool = situations.getValue(category)
        val template = pool[(index / categories.size) % pool.size]
        val chapter = index / 110 + 1
        val context = adaptiveContext(c, category)
        val titleSuffix = listOf("A", "B", "C", "D", "E")[(index / 7) % 5]
        return EventNode(
            id = "evt_${index.toString().padStart(3, '0')}",
            title = "${template.title} · $titleSuffix",
            text = "${template.setup} ${template.provocation} $context Chapitre $chapter : ce choix peut modifier la façon dont les prochains acteurs te traiteront.",
            category = category,
            provocation = template.provocation,
            choices = contextualChoices(c, category, index)
        )
    }

    private fun adaptiveContext(c: Campaign, category: String): String {
        val reputation = when {
            c.fear >= 65 -> "Ta réputation de menace te précède déjà."
            c.opinion >= 45 -> "Une partie du public te fait assez confiance pour observer ta réaction avant de paniquer."
            c.opinion <= -35 -> "La foule est déjà prête à interpréter le moindre geste contre toi."
            c.prestige >= 180 -> "Tout ce que tu fais ici sera analysé bien au-delà de ${c.scope.label.lowercase()}."
            else -> "Personne ne sait encore exactement quelle règle tu vas imposer à ce genre de situation."
        }
        val identity = if (category == "IDENTITÉ" || c.identityExposure >= 55) " Ton identité civile est déjà sous pression (${c.identityExposure}%)." else ""
        val mastery = if (category == "POUVOIR" || c.control < 30) " Ta maîtrise actuelle (${c.control}%) rend toute démonstration de puissance moins prévisible." else ""
        return reputation + identity + mastery
    }

    private fun contextualChoices(c: Campaign, category: String, index: Int): List<Choice> {
        val variant = (index / 3 + c.turn) % 3
        fun pick(a: String, b: String, d: String) = listOf(a, b, d)[variant]
        val choices = when (category) {
            "RUE" -> listOf(
                Choice(pick("Sécuriser les victimes avant tout", "Créer un couloir pour les civils", "Prendre le risque de sauver d'abord"), 8, 3, 5, -1, 0, 5, 2, "PROTECT"),
                Choice(pick("Couper immédiatement la fuite", "Neutraliser le provocateur", "Refuser de le laisser choisir le rythme"), -1, 6, -1, 4, 1, 7, 5, "PURSUE"),
                Choice(pick("Parler pour gagner quelques secondes", "Le pousser à se découvrir en discutant", "Négocier tout en préparant une ouverture"), 2, 2, 2, -2, 0, 4, 3, "NEGOTIATE"),
                Choice(pick("L'intimider devant tout le monde", "Faire de lui un exemple", "Briser sa provocation publiquement"), -7, 7, -4, 9, 2, 8, 7, "DOMINATE"),
                Choice(pick("Feinter et partager ton attention", "Créer une diversion puis frapper", "Utiliser le décor pour gérer les deux problèmes"), 3, 5, 2, 1, 1, 8, 6, "TACTICAL")
            )
            "IDENTITÉ" -> listOf(
                Choice(pick("Démentir sans entrer dans les détails", "Laisser planer un doute contrôlé", "Répondre sobrement et fermer la porte"), 1, 1, 1, -1, 0, 2, 2, "DENY"),
                Choice(pick("Remonter discrètement jusqu'à la source", "Traquer qui a fourni les informations", "Identifier le maillon faible de la fuite"), 1, 3, 0, 1, 0, 5, 4, "INVESTIGATE"),
                Choice(pick("Créer une fausse piste crédible", "Fabriquer un contre-récit vérifiable", "Détourner l'enquête vers une identité leurre"), -2, 4, 1, 1, 0, 5, 5, "DECEIVE"),
                Choice(pick("Menacer directement le diffuseur", "Faire comprendre le prix d'une publication", "Intimider la personne qui te traque"), -7, 5, -5, 9, 1, 6, 7, "DOMINATE"),
                Choice(pick("Assumer une partie de la vérité", "Révéler juste assez pour reprendre le contrôle", "Transformer l'exposition en déclaration publique"), 4, 8, 5, -2, 0, 8, 8, "REVEAL")
            )
            "FAMILLE" -> listOf(
                Choice(pick("Choisir ton proche cette fois", "Rester présent malgré l'urgence", "Honorer ta promesse familiale"), 6, -1, 3, -2, 0, 1, 2, "FAMILY"),
                Choice(pick("Partir intervenir sans tout expliquer", "Répondre à l'urgence et gérer les conséquences après", "Faire passer la crise avant ta vie privée"), 1, 4, 0, 1, 0, 5, 4, "DUTY"),
                Choice(pick("Dire enfin une partie de la vérité", "Expliquer ce que ta double vie implique", "Arrêter de protéger ton secret par le mensonge"), 5, 2, 4, -1, 0, 4, 5, "REVEAL"),
                Choice(pick("Organiser une protection sans demander leur avis", "Les éloigner de force du danger", "Décider à leur place de ce qui est sûr"), -3, 3, -3, 4, 0, 5, 4, "CONTROL"),
                Choice(pick("Tenter de gérer les deux en parallèle", "Improviser pour ne sacrifier aucun côté", "Fractionner ton attention malgré le risque"), 3, 4, 2, 0, 1, 6, 7, "TACTICAL")
            )
            "MÉDIAS" -> listOf(
                Choice(pick("Répondre franchement aux questions", "Donner une version complète et vérifiable", "Assumer publiquement tes décisions"), 5, 5, 6, -2, 0, 6, 4, "TRANSPARENT"),
                Choice(pick("Préparer une déclaration très contrôlée", "Répondre seulement sur les faits utiles", "Cadre le récit sans livrer ta vie"), 1, 4, 2, 0, 0, 4, 2, "CONTROL"),
                Choice(pick("Retourner l'enquête contre leurs méthodes", "Exposer publiquement leurs manipulations", "Forcer le média à répondre de ses propres choix"), 0, 6, 1, 3, 0, 6, 5, "COUNTER"),
                Choice(pick("Refuser de nourrir le spectacle", "Quitter sans répondre", "Ignorer la provocation médiatique"), 0, -1, -2, 1, 0, 1, 1, "IGNORE"),
                Choice(pick("Les intimider pour obtenir le silence", "Faire comprendre que certaines limites ne se franchissent pas", "Menacer les responsables de la diffusion"), -8, 7, -7, 10, 1, 6, 7, "DOMINATE")
            )
            "FACTION" -> listOf(
                Choice(pick("Accepter une coopération limitée", "Prendre leurs ressources sans céder ton autonomie", "Signer un accord strictement encadré"), 1, 5, 2, 0, 1, 7, 4, "ALLY"),
                Choice(pick("Négocier chaque condition", "Faire monter le prix de ton soutien", "Transformer leur offre en partenariat équilibré"), 1, 4, 2, -1, 0, 6, 3, "NEGOTIATE"),
                Choice(pick("Refuser publiquement leur pression", "Marquer clairement ton indépendance", "Rejeter leur ultimatum devant témoins"), 2, 5, 2, 3, 0, 5, 4, "DEFY"),
                Choice(pick("Faire semblant d'accepter pour les infiltrer", "Entrer dans leur jeu pour apprendre leurs failles", "Utiliser leur offre comme porte d'entrée"), -2, 6, -1, 2, 1, 8, 7, "INFILTRATE"),
                Choice(pick("Les écraser avant qu'ils deviennent un problème", "Répondre à la menace par la force", "Briser leur capacité à te faire pression"), -9, 8, -6, 10, 2, 9, 8, "DOMINATE")
            )
            "RIVAL" -> listOf(
                Choice(pick("Refuser le duel et rester concentré", "Ne pas jouer selon ses règles", "Laisser sa provocation mourir seule"), 3, -1, 2, -3, 0, 2, 1, "IGNORE"),
                Choice(pick("Accepter la confrontation proprement", "Le battre sans l'humilier", "Répondre au défi selon des règles claires"), 0, 7, 2, 3, 1, 7, 6, "DUEL"),
                Choice(pick("Le provoquer sur son propre point faible", "Retourner son ego contre lui", "Le pousser à commettre la première erreur"), -2, 6, 0, 4, 0, 7, 5, "COUNTER"),
                Choice(pick("Proposer une coopération temporaire", "Mettre votre rivalité en pause", "Transformer la compétition en alliance de circonstance"), 4, 4, 4, -2, 0, 7, 3, "ALLY"),
                Choice(pick("L'humilier devant les témoins", "Écraser sa réputation en même temps que lui", "Faire de sa provocation un avertissement pour les autres"), -8, 9, -6, 10, 2, 9, 8, "DOMINATE")
            )
            "SAUVETAGE" -> listOf(
                Choice(pick("Organiser le sauvetage par priorité vitale", "Faire un tri rapide et sauver le maximum", "Stabiliser la zone avant toute poursuite"), 9, 4, 6, -2, 0, 6, 3, "PROTECT"),
                Choice(pick("Poursuivre le responsable avant qu'il disparaisse", "Empêcher la fuite puis revenir", "Prendre le risque de neutraliser la cause"), -1, 7, -3, 4, 1, 8, 7, "PURSUE"),
                Choice(pick("Coordonner les secours au lieu de tout faire seul", "Déléguer une partie des victimes", "Utiliser les équipes présentes comme multiplicateur"), 6, 5, 5, -2, 0, 7, 3, "COORDINATE"),
                Choice(pick("Déployer ton pouvoir à pleine puissance", "Tenter un sauvetage spectaculaire", "Forcer une solution immédiate malgré les dégâts possibles"), 2, 8, 1, 4, 3, 9, 9, "OVERLOAD"),
                Choice(pick("Créer une diversion pour gagner du temps", "Modifier le terrain avant d'évacuer", "Prendre une voie indirecte mais plus sûre"), 5, 3, 4, -1, 0, 6, 4, "TACTICAL")
            )
            "CRIME" -> listOf(
                Choice(pick("Observer encore pour remonter toute la chaîne", "Résister à l'envie de frapper trop tôt", "Accumuler des preuves avant l'assaut"), 2, 3, 1, 0, 0, 5, 2, "INVESTIGATE"),
                Choice(pick("Frapper le réseau maintenant", "Lancer une opération avant qu'ils bougent", "Neutraliser rapidement leurs cadres"), -1, 7, 0, 5, 1, 8, 6, "RAID"),
                Choice(pick("Retourner un informateur", "Acheter une information par une concession", "Faire parler quelqu'un de l'intérieur"), 0, 4, 1, 1, 0, 7, 4, "NEGOTIATE"),
                Choice(pick("Avertir publiquement le réseau", "Annoncer que leurs règles sont terminées", "Utiliser ta réputation pour les faire reculer"), -2, 6, -1, 7, 0, 6, 4, "INTIMIDATE"),
                Choice(pick("Détruire leurs moyens sans négocier", "Rendre leur activité impossible par la force", "Écraser leur infrastructure en une nuit"), -8, 9, -5, 11, 2, 10, 8, "DOMINATE")
            )
            "POUVOIR" -> listOf(
                Choice(pick("Arrêter immédiatement et reprendre le contrôle", "Réduire ta puissance avant l'accident", "Accepter de perdre l'avantage pour stabiliser ton pouvoir"), 5, 0, 3, -2, -1, 2, 1, "CONTROL"),
                Choice(pick("Tester progressivement la nouvelle limite", "Monter en puissance par paliers", "Expérimenter sans dépasser ton contrôle"), 1, 3, 1, 0, 2, 5, 4, "TEST"),
                Choice(pick("Pousser au maximum pendant que tu le peux", "Franchir le seuil sans attendre", "Exploiter la surcharge avant qu'elle disparaisse"), -2, 8, -2, 5, 5, 9, 10, "OVERLOAD"),
                Choice(pick("Chercher immédiatement une aide experte", "Faire analyser le phénomène", "Demander conseil avant une nouvelle utilisation"), 2, 2, 2, -1, 1, 4, 2, "MENTOR"),
                Choice(pick("Cacher le changement à tout le monde", "Faire comme si rien n'avait évolué", "Protéger le secret de cette nouvelle capacité"), -1, 1, -1, 1, 1, 3, 3, "HIDE")
            )
            "GOUVERNEMENT" -> listOf(
                Choice(pick("Coopérer sous conditions écrites", "Accepter une rencontre officielle", "Jouer la transparence sans abandonner tes droits"), 3, 5, 4, -2, 0, 6, 3, "COOPERATE"),
                Choice(pick("Négocier un statut indépendant", "Faire reconnaître tes propres limites", "Transformer leur contrôle en protocole mutuel"), 1, 6, 2, 0, 0, 7, 4, "NEGOTIATE"),
                Choice(pick("Refuser leur autorité", "Rappeler que tu n'es pas sous leurs ordres", "Ignorer leur ultimatum"), -1, 5, -2, 5, 0, 5, 5, "DEFY"),
                Choice(pick("Rendre publics leurs dossiers gênants", "Exposer ce qu'ils te cachent", "Utiliser leurs propres secrets comme levier"), -3, 8, 0, 5, 0, 8, 7, "COUNTER"),
                Choice(pick("Les forcer à reculer", "Démontrer qu'ils ne peuvent pas te contenir", "Briser leur dispositif de contrôle"), -9, 9, -7, 12, 2, 10, 9, "DOMINATE")
            )
            "MENTOR" -> listOf(
                Choice(pick("Accepter l'entraînement avec des limites", "Apprendre sans lui céder ton jugement", "Tester sa méthode sous conditions"), 2, 3, 2, 0, 2, 5, 3, "TRAIN"),
                Choice(pick("Enquêter d'abord sur son passé", "Vérifier ce qu'il ne raconte pas", "Refuser de lui faire confiance sans preuves"), 1, 2, 1, 0, 0, 4, 2, "INVESTIGATE"),
                Choice(pick("Prendre son savoir sans accepter son autorité", "Apprendre puis partir", "Utiliser uniquement ce qui t'est utile"), -2, 4, -1, 1, 2, 5, 5, "PRAGMATIC"),
                Choice(pick("Refuser sa méthode", "Choisir de progresser seul", "Couper court avant de lui devoir quoi que ce soit"), 2, 0, 1, -1, 0, 2, 1, "DEFY"),
                Choice(pick("Accepter même la leçon interdite", "Franchir la limite qu'il te propose", "Apprendre la technique que les autres refusent"), -6, 7, -4, 5, 4, 8, 8, "DARK_TRAIN")
            )
            else -> listOf(
                Choice(pick("Coordonner les secours et les acteurs présents", "Prendre la direction opérationnelle", "Créer une chaîne de commandement temporaire"), 7, 6, 5, -1, 0, 8, 4, "COORDINATE"),
                Choice(pick("Chercher d'abord la cause réelle", "Refuser de traiter seulement les symptômes", "Enquêter avant d'engager toutes les forces"), 2, 4, 1, 0, 0, 6, 4, "INVESTIGATE"),
                Choice(pick("Protéger les infrastructures vitales", "Sécuriser ce qui empêcherait l'effondrement général", "Prioriser hôpitaux, énergie et évacuations"), 5, 5, 4, -1, 0, 7, 3, "PROTECT"),
                Choice(pick("Utiliser toute ta puissance pour casser la crise", "Forcer une résolution immédiate", "Tenter l'action spectaculaire qui peut tout arrêter"), -1, 9, -2, 6, 4, 10, 10, "OVERLOAD"),
                Choice(pick("Profiter du chaos pour imposer ton autorité", "Faire de la crise un tournant politique", "Prendre le contrôle pendant que personne d'autre ne le peut"), -8, 10, -5, 11, 2, 10, 8, "DOMINATE")
            )
        }
        val offset = index % choices.size
        return (choices.drop(offset) + choices.take(offset)).take(5)
    }

    fun choose(c: Campaign, choice: Choice): Campaign {
        val roll = outcomeRoll(c, choice)
        val danger = max(0, choice.risk - c.control / 20)
        val injury = if (roll < danger * 4) 8 + danger * 2 else 0
        val casualties = if (choice.moral < -5 && roll < 35) 1 + roll % 4 else if (choice.approach == "OVERLOAD" && roll < 18) 1 else 0
        val exposure = if (roll < choice.risk * 3) 3 + choice.risk else 0
        val nextTurn = c.turn + 1
        val scopeGain = choice.impact + max(0, c.prestige / 120)
        val newPower = clamp(c.power + choice.power + if (nextTurn % 8 == 0) 1 else 0, 0, 100)
        val controlDelta = when {
            choice.approach == "CONTROL" -> 2
            choice.approach == "TRAIN" -> 2
            choice.risk <= 3 -> 1
            choice.approach == "OVERLOAD" -> -1
            else -> 0
        }
        val summary = "${c.age} ans — ${choice.label} (${c.scope.label})"
        return c.copy(
            turn = nextTurn,
            morality = clamp(c.morality + choice.moral, -100, 100),
            prestige = max(0, c.prestige + choice.prestige + c.scope.ordinal),
            opinion = clamp(c.opinion + choice.opinion, -100, 100),
            fear = clamp(c.fear + choice.fear, 0, 100),
            power = newPower,
            control = clamp(c.control + controlDelta, 0, 100),
            influence = max(0, c.influence + scopeGain),
            health = clamp(c.health - injury, 0, 100),
            civilianCasualties = c.civilianCasualties + casualties,
            identityExposure = clamp(c.identityExposure + exposure, 0, 100),
            timeline = (c.timeline + summary).takeLast(80)
        )
    }

    fun resolve(c: Campaign, event: EventNode, choice: Choice): Resolution {
        val next = choose(c, choice)
        val roll = outcomeRoll(c, choice)
        val injury = c.health - next.health
        val casualties = next.civilianCasualties - c.civilianCasualties
        val exposure = next.identityExposure - c.identityExposure
        val opener = categoryReaction(event.category, choice.approach, roll)
        val actorReaction = socialReaction(next, choice, roll)
        val consequence = when {
            casualties > 0 -> "$casualties victime${if (casualties > 1) "s" else ""} civile${if (casualties > 1) "s sont désormais associées" else " est désormais associée"} à ta décision. Même tes alliés auront du mal à l'effacer du récit."
            injury > 0 -> "Tu obtiens un résultat, mais tu encaisses $injury points de dégâts. Ta faiblesse devient plus difficile à ignorer."
            exposure > 0 -> "L'action laisse des traces : l'exposition de ton identité augmente de $exposure points. Quelqu'un pourra recouper ce détail plus tard."
            choice.approach == "NEGOTIATE" && roll > 55 -> "La discussion ouvre une porte inattendue. Ton interlocuteur n'est pas convaincu, mais il te considère désormais comme quelqu'un avec qui il faut compter."
            choice.approach == "PROTECT" || choice.approach == "COORDINATE" -> "Le résultat est moins spectaculaire qu'une victoire totale, mais plusieurs témoins retiennent surtout que tu as réduit le chaos au lieu de nourrir ton ego."
            choice.approach == "DOMINATE" -> "Personne ne doute de ta capacité à imposer ta volonté. En revanche, certains commencent déjà à se demander qui pourra t'arrêter le jour où ils ne seront plus d'accord avec toi."
            roll >= 75 -> "L'exécution est presque parfaite. Cette réussite renforce l'idée que ta méthode fonctionne, ce qui peut être aussi dangereux qu'utile."
            roll <= 20 -> "Le plan fonctionne seulement en partie. Le monde retient surtout les secondes où la situation a semblé t'échapper."
            else -> "La situation se ferme sans victoire propre. Tu gagnes quelque chose, mais tu laisses derrière toi assez d'ambiguïté pour que chacun raconte une version différente."
        }
        val outcome = "$opener $actorReaction $consequence"
        val withOutcome = next.copy(timeline = (next.timeline + "↳ $outcome").takeLast(80))
        return Resolution(withOutcome, outcome)
    }

    private fun categoryReaction(category: String, approach: String, roll: Int): String {
        val base = when (category) {
            "RUE" -> listOf("La rue réagit immédiatement.", "Les témoins comprennent en quelques secondes quel genre de présence tu veux être.", "Le quartier n'oubliera pas la manière dont tu as répondu à cette provocation.")
            "IDENTITÉ" -> listOf("La bataille se déplace vers l'information.", "Ton secret ne dépend plus seulement de ce que tu caches, mais de ce que les autres croient.", "La pression autour de ton identité change de forme plutôt que de disparaître.")
            "FAMILLE" -> listOf("Cette fois, la conséquence la plus lourde n'est pas publique.", "Tes proches retiennent moins l'urgence que la place que tu leur as donnée.", "La double vie vient de coûter quelque chose de réel.")
            "MÉDIAS" -> listOf("Le récit public se réécrit presque en direct.", "Quelques phrases suffisent à déplacer l'opinion.", "Les caméras transforment ton choix en symbole avant même que les faits soient totalement connus.")
            "FACTION" -> listOf("Les factions recalculent immédiatement leur rapport de force.", "Ton choix devient un signal pour des groupes qui n'étaient même pas présents.", "L'équilibre entre coopération et menace vient de bouger.")
            "RIVAL" -> listOf("Ton rival enregistre la réponse autant que le résultat.", "La rivalité vient de gagner une nouvelle règle.", "Ce duel de réputation comptera autant que l'affrontement lui-même.")
            "SAUVETAGE" -> listOf("Les secondes perdues et gagnées deviennent tout ce qui compte.", "Les secours adaptent leur plan à ce que tu viens de faire.", "Les survivants ne verront jamais la totalité de tes choix, seulement leurs conséquences.")
            "CRIME" -> listOf("Le réseau criminel modifie déjà ses habitudes.", "La rue souterraine apprend vite ce que tu tolères et ce que tu punis.", "Ta méthode circule plus vite que les arrestations.")
            "POUVOIR" -> listOf("Ton pouvoir te donne une réponse que tu n'étais pas certain de vouloir.", "Ton corps et ton environnement mémorisent cette limite.", "Ce que tu viens d'apprendre sur ta puissance changera tes prochains risques.")
            "GOUVERNEMENT" -> listOf("Les institutions classent désormais ton comportement dans une nouvelle catégorie.", "Un dossier vient probablement de gagner plusieurs pages.", "Ta relation au pouvoir légal devient plus claire, et donc plus difficile à esquiver.")
            "MENTOR" -> listOf("La leçon dépasse la technique elle-même.", "Ton rapport à l'autorité et à la transmission vient de se préciser.", "Quelqu'un sait maintenant jusqu'où tu es prêt à aller pour progresser.")
            else -> listOf("La crise change d'échelle autour de toi.", "Les autres acteurs réorganisent leurs priorités selon ta décision.", "Dans le chaos, ton choix devient une référence pour ceux qui n'avaient aucun plan.")
        }
        val bias = if (approach == "DOMINATE") 1 else if (roll > 70) 2 else 0
        return base[(roll + bias) % base.size]
    }

    private fun socialReaction(c: Campaign, choice: Choice, roll: Int): String = when {
        choice.approach == "DOMINATE" && c.fear >= 45 -> "Même ceux qui te contestent baissent d'un ton : ils ne sont pas convaincus, ils sont prudents."
        c.opinion >= 50 && choice.moral >= 0 -> "Le public t'accorde le bénéfice du doute et plusieurs témoins défendent spontanément ta décision."
        c.opinion <= -35 -> "Une partie du public interprète déjà le moindre détail comme la preuve que ses soupçons étaient justifiés."
        c.prestige >= 250 -> "Des acteurs bien au-delà de la scène locale analysent désormais ce geste comme une déclaration de doctrine."
        choice.approach == "NEGOTIATE" && roll < 40 -> "Ton interlocuteur prend ta patience pour une hésitation et tente immédiatement d'obtenir davantage."
        choice.approach == "IGNORE" -> "Le silence évite l'escalade immédiate, mais laisse ton adversaire raconter seul ce qu'il vient de se passer."
        else -> "Les témoins se divisent : certains voient du sang-froid, d'autres une méthode qu'ils n'accepteraient pas contre eux."
    }

    private fun outcomeRoll(c: Campaign, choice: Choice): Int = ((mix(c.seed xor 0x5EEDL, c.turn.toLong() * 17 + choice.label.hashCode()) ushr 2) % 100).toInt()

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

    fun legacyScore(c: Campaign): Int = max(0, c.prestige + c.influence / 2 + c.power * 2 + abs(c.morality) * 2 + c.turn - c.civilianCasualties)

    private fun clamp(v: Int, low: Int, high: Int) = min(high, max(low, v))

    private fun mix(a: Long, b: Long): Long {
        var z = a + 0x9E3779B97F4A7C15UL.toLong() + b * 0xBF58476D1CE4E5B9UL.toLong()
        z = (z xor (z ushr 30)) * 0xBF58476D1CE4E5B9UL.toLong()
        z = (z xor (z ushr 27)) * 0x94D049BB133111EBUL.toLong()
        return z xor (z ushr 31)
    }
}
