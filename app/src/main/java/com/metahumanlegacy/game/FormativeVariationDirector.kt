package com.metahumanlegacy.game

/**
 * Gives the 8–17 formative decade replayable narrative situations without changing the hidden
 * mechanics that build the awakening. Variants only replace presentation (title/text/labels):
 * the original Choice values, approaches, affinities, costs and flags remain authoritative.
 */
internal object FormativeVariationDirector {
    internal data class SceneVariant(val title: String, val text: String, val labels: List<String>)

    fun enrich(c: Campaign, base: EventNode): EventNode {
        if (base.kind != "FORMATIVE" || c.age !in 8..17 || base.choices.size != 4) return base
        val variant = variantIndex(c)
        if (variant == 0) return base.copy(text = base.text + contextTail(c))
        val scene = scene(c.age, variant) ?: return base
        return base.copy(
            title = scene.title,
            text = scene.text + contextTail(c),
            choices = base.choices.mapIndexed { index, choice ->
                choice.copy(label = scene.labels.getOrElse(index) { choice.label })
            }
        )
    }

    internal fun variantIndex(c: Campaign): Int {
        val salt = c.age * 7_919L + c.socialBackground.hashCode() * 31L +
            c.temperament.hashCode() * 17L + c.city.hashCode()
        return positiveMod(mix(c.seed, salt), 3)
    }

    private fun contextTail(c: Campaign): String = buildString {
        append("\n\n")
        append("À ${c.city}, dans ${c.district}, ce moment prend une couleur particulière : ")
        append(
            when (c.socialBackground) {
                "Quartier populaire" -> "tu as déjà appris que l'entraide et la réputation circulent vite."
                "Milieu privilégié" -> "les adultes autour de toi ont des moyens, mais aussi des attentes très visibles."
                "Foyer instable" -> "tu sais qu'une petite décision peut peser lourd quand l'équilibre à la maison est fragile."
                "Famille militaire" -> "on t'a beaucoup parlé de règles, de responsabilité et de conséquences."
                "Milieu scientifique" -> "on t'a appris à observer avant d'affirmer que tu comprends."
                "Autodidacte précaire" -> "tu sais déjà qu'on ne dispose pas toujours d'une seconde chance ou d'un filet de sécurité."
                "Famille très présente" -> "tu grandis avec des proches qui remarquent vite quand quelque chose ne va pas."
                else -> "tu commences à comprendre que ta manière d'agir finit par devenir une habitude."
            }
        )
    }

    private fun scene(age: Int, variant: Int): SceneVariant? = when (age) {
        8 -> if (variant == 1) v(
            "LA MONNAIE SOUS LE BANC",
            "À 8 ans, tu trouves une enveloppe avec de l'argent sous un banc près d'un commerce. Une personne âgée revient manifestement chercher quelque chose, tandis qu'un autre enfant te souffle que personne ne saura jamais que tu l'as prise.",
            "Rattraper la personne et lui rendre l'enveloppe",
            "La remettre au commerçant pour qu'il retrouve le propriétaire",
            "Regarder autour de toi et vérifier à qui elle appartient vraiment",
            "La garder en te disant que cette chance ne se représentera peut-être pas"
        ) else v(
            "LE PETIT MENSONGE",
            "À 8 ans, ton groupe casse accidentellement un objet auquel un adulte tient beaucoup. Personne ne sait lequel d'entre vous est responsable et les autres commencent déjà à construire une version commune qui évite la punition.",
            "Dire ce qui s'est passé pour éviter qu'un autre soit accusé",
            "Proposer que tout le groupe assume ensemble",
            "Reconstituer calmement ce qui s'est vraiment passé avant de parler",
            "Suivre le mensonge du groupe pour rester protégé"
        )

        9 -> if (variant == 1) v(
            "LE DERNIER GOÛTER",
            "À 9 ans, il ne reste qu'une portion de goûter pour plusieurs enfants après une erreur de distribution. L'un d'eux en a clairement plus besoin que les autres, mais chacun trouve une raison de dire qu'il la mérite.",
            "La laisser à celui qui semble en avoir le plus besoin",
            "Proposer une règle simple pour partager équitablement",
            "Demander ce qui s'est passé avant de décider",
            "La prendre avant que quelqu'un d'autre ne le fasse"
        ) else v(
            "LE COULOIR APRÈS LA DISPUTE",
            "À 9 ans, après une grosse dispute familiale, tu entends quelqu'un pleurer derrière une porte tandis qu'un autre proche te demande de ne surtout pas t'en mêler. Tu ne peux pas résoudre le conflit, seulement choisir où tu te places.",
            "Rester près de la personne isolée sans lui demander d'expliquer",
            "Chercher un adulte capable de calmer réellement la situation",
            "Écouter ce que chacun dit pour comprendre ce qui a déclenché la dispute",
            "Te servir du désordre pour obtenir quelque chose qu'on t'aurait refusé"
        )

        10 -> if (variant == 1) v(
            "LE VÉLO DANS LA DESCENTE",
            "À 10 ans, des plus grands lancent un défi : descendre une pente dangereuse sans freiner. Tout le monde filme. Refuser sera vu comme une faiblesse ; accepter peut finir à l'hôpital.",
            "Proposer un défi différent où personne ne risque de se blesser",
            "Refuser clairement même si les autres se moquent",
            "Observer la pente et montrer pourquoi le trajet est dangereux",
            "Partir le premier pour prouver que tu peux le faire"
        ) else v(
            "LE BÂTIMENT INTERDIT",
            "À 10 ans, une porte mal fermée donne accès à un bâtiment abandonné. Tes amis veulent entrer. On raconte que l'endroit est dangereux, mais personne n'a de preuve et l'idée d'être le premier groupe à l'explorer excite tout le monde.",
            "Convaincre les autres de rester dehors et trouver autre chose à faire",
            "Fermer la porte et prévenir quelqu'un avant qu'un accident arrive",
            "Observer les lieux de l'extérieur pour savoir si le danger est réel",
            "Entrer devant les autres pour montrer que tu n'as pas peur"
        )

        11 -> if (variant == 1) v(
            "LE CONTRÔLE COPIÉ",
            "À 11 ans, tu découvres qu'un élève populaire a copié un contrôle puis laissé croire qu'un autre lui avait donné les réponses. La personne accusée risque une sanction et ton témoignage pourrait te coûter ta place dans le groupe.",
            "Défendre publiquement la personne accusée",
            "Dire les faits à l'enseignant en privé",
            "Confronter le vrai responsable et lui demander d'avouer",
            "Garder l'information pour t'en servir plus tard"
        ) else v(
            "LA VITRE BRISÉE",
            "À 11 ans, tu vois précisément qui casse une vitre puis désigne aussitôt un enfant déjà mal vu. Les adultes arrivent, le faux récit prend vite et tu comprends que te mêler de l'histoire va t'exposer.",
            "Te placer immédiatement du côté de l'enfant accusé",
            "Expliquer calmement aux adultes ce que tu as vu",
            "Donner au responsable une dernière chance de dire la vérité lui-même",
            "Ne rien dire et conserver ce secret comme moyen de pression"
        )

        12 -> if (variant == 1) v(
            "LES ABSENCES",
            "À 12 ans, un proche commence à manquer régulièrement l'école ou ses activités et invente des excuses différentes. Il te demande de ne rien raconter. Tu comprends qu'il se passe quelque chose de sérieux sans savoir quoi.",
            "Lui montrer que tu restes disponible sans le forcer",
            "Prévenir un adulte fiable même s'il risque de t'en vouloir",
            "Chercher à comprendre ce qui provoque réellement ses absences",
            "Garder le secret parce que cette confiance te donne une place particulière"
        ) else v(
            "LE TÉLÉPHONE ÉTEINT",
            "À 12 ans, quelqu'un de proche change brusquement : téléphone toujours éteint, messages évités, humeur différente. Quand tu poses une question, on te demande simplement de promettre de ne rien dire.",
            "Respecter son rythme tout en restant présent",
            "Chercher discrètement l'aide d'un adulte compétent",
            "Recouper les signes avant de décider si la situation est dangereuse",
            "Accepter le secret sans poser plus de questions"
        )

        13 -> if (variant == 1) v(
            "LE NOUVEAU DU GROUPE",
            "À 13 ans, ton groupe décide de tester un nouvel élève avec une mise en scène humiliante. Tout le monde répète que c'est pour rire et que refuser signifie que tu n'es plus vraiment des leurs.",
            "Refuser et rester avec la personne visée",
            "Imposer une limite nette au groupe avant que ça commence",
            "Comprendre qui mène réellement le groupe et pourquoi",
            "Participer juste assez pour ne pas perdre ta place"
        ) else v(
            "LA VIDÉO DU VESTIAIRE",
            "À 13 ans, quelqu'un filme une scène embarrassante dans un vestiaire et ton cercle veut la montrer à toute la classe. Tu peux devenir la personne qui casse l'élan, celle qui organise, celle qui enquête ou celle qui profite du moment.",
            "Empêcher la diffusion et aller voir la personne filmée",
            "Faire supprimer la vidéo devant tout le monde",
            "Identifier qui a filmé et pourquoi avant d'agir",
            "Laisser circuler la vidéo pour rester du bon côté du groupe"
        )

        14 -> if (variant == 1) v(
            "LE COMPTE ANONYME",
            "À 14 ans, un compte anonyme publie des rumeurs sur des élèves de ton établissement. Une rumeur visant quelqu'un que tu connais commence à exploser. Beaucoup regardent, peu veulent se mouiller.",
            "Prévenir et soutenir directement la personne visée",
            "Signaler le compte et pousser les adultes à intervenir",
            "Remonter les publications pour identifier qui se cache derrière",
            "Conserver les informations pour gagner toi aussi de l'influence"
        ) else v(
            "LE MESSAGE VOCAL",
            "À 14 ans, un message vocal privé est partagé de téléphone en téléphone. Son contenu peut détruire une amitié et tout le monde veut entendre la suite. Tu l'as reçu avant qu'il devienne réellement viral.",
            "Avertir immédiatement la personne concernée et supprimer le fichier",
            "Demander que les responsables stoppent officiellement la diffusion",
            "Retrouver le premier partage pour comprendre l'origine de la fuite",
            "Le conserver parce qu'une information rare donne du pouvoir"
        )

        15 -> if (variant == 1) v(
            "LE PETIT BOULOT",
            "À 15 ans, une occasion de gagner ton premier argent arrive au même moment qu'un engagement familial et qu'une activité importante pour ton avenir. Tu ne peux pas être partout et chaque choix déçoit quelqu'un.",
            "Choisir d'abord les proches qui comptent sur toi",
            "Construire un planning strict pour tenter de tenir plusieurs engagements",
            "Privilégier l'activité qui développe le plus tes compétences",
            "Prendre le travail qui t'apporte immédiatement indépendance et statut"
        ) else v(
            "LE PROJET DE FIN D'ANNÉE",
            "À 15 ans, on te propose de rejoindre un projet exigeant qui peut ouvrir des portes. Tes amis veulent profiter de leur temps libre et ta famille a aussi besoin de toi. Accepter signifie commencer à choisir la vie que tu construis.",
            "Rester disponible pour les personnes qui dépendent de toi",
            "Organiser précisément ton temps pour ne sacrifier personne",
            "T'investir dans le projet parce qu'il peut réellement t'apprendre quelque chose",
            "Choisir l'option qui te distingue le plus des autres"
        )

        16 -> if (variant == 1) v(
            "L'ACCIDENT AU CARREFOUR",
            "À 16 ans, un accident bloque un carrefour pendant que plusieurs personnes paniquent et que les secours tardent. Tu n'as aucun pouvoir. Tu as seulement quelques connaissances, ton téléphone et ta capacité à décider sous pression.",
            "Aider d'abord les personnes les plus vulnérables à s'éloigner",
            "Organiser les témoins et répartir des tâches simples",
            "Comprendre précisément ce qui s'est passé avant de déplacer qui que ce soit",
            "Te rapprocher de la zone dangereuse pour agir là où les autres n'osent pas"
        ) else v(
            "LA CRUE SOUDAINE",
            "À 16 ans, une pluie brutale transforme plusieurs rues en pièges et l'eau monte plus vite que prévu. Les adultes eux-mêmes hésitent sur ce qu'il faut faire. Tu n'es encore qu'une personne ordinaire au milieu du problème.",
            "Aider les personnes isolées à rejoindre un endroit sûr",
            "Créer un point de rassemblement et coordonner les présents",
            "Observer l'écoulement et chercher le passage réellement praticable",
            "Traverser une zone risquée pour atteindre ceux qui sont bloqués"
        )

        17 -> if (variant == 1) v(
            "LE DOSSIER D'ADMISSION",
            "À 17 ans, une opportunité d'études ou de formation t'oblige à choisir entre rester près des tiens, sécuriser une voie stable, poursuivre ce qui t'intrigue vraiment ou viser l'option la plus prestigieuse. Pour la première fois, personne ne peut décider à ta place.",
            "Choisir une voie qui te permet de rester utile à tes proches",
            "Prendre l'option la plus stable et construire des bases solides",
            "Choisir le parcours qui nourrit le plus ta curiosité",
            "Viser la voie la plus ambitieuse même si elle t'éloigne"
        ) else v(
            "LE PREMIER VRAI DÉPART",
            "À 17 ans, une occasion de partir plusieurs mois apparaît : stage, travail, projet ou formation selon ton parcours. Dire oui peut changer ta vie ; dire non peut préserver ce que tu as déjà construit. Tu ignores qu'à 18 ans un changement bien plus radical approche.",
            "Rester pour les personnes qui ont encore besoin de toi",
            "Préparer un départ seulement si toutes les conditions sont sûres",
            "Partir surtout pour découvrir et comprendre un monde plus large",
            "Saisir l'occasion immédiatement parce qu'elle peut te faire monter plus vite"
        )
        else -> null
    }

    private fun v(title: String, text: String, care: String, order: String, truth: String, ascend: String) =
        SceneVariant(title, text, listOf(care, order, truth, ascend))
}
