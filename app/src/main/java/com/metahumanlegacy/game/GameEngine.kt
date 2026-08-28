package com.metahumanlegacy.game

import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.Random
import java.util.zip.GZIPInputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class Scope(val label: String) { STREET("Rue"), DISTRICT("Quartier"), CITY("Ville"), REGION("Région"), COUNTRY("Pays"), WORLD("Monde") }

data class CharacterBlueprint(val firstName:String,val lastName:String,val alias:String,val pronouns:String,val city:String,val district:String,val socialBackground:String,val origin:String,val powerFamily:String,val weakness:String,val motivation:String,val visualStyle:String){val fullName:String get()=listOf(firstName.trim(),lastName.trim()).filter{it.isNotBlank()}.joinToString(" ").ifBlank{"Alex Vesper"}}

data class Choice(val label:String,val moral:Int,val prestige:Int,val opinion:Int,val fear:Int,val power:Int,val impact:Int,val risk:Int,val approach:String,val stakes:Int,val sourceCategory:String,val threadId:String?=null,val relationDelta:Int=0,val flag:String?=null)
data class EventNode(val id:String,val title:String,val text:String,val choices:List<Choice>,val category:String,val provocation:String,val stakes:Int,val threadId:String?=null,val threadStage:Int=0)
data class StoryThread(val id:String,val openedTurn:Int,val lastTurn:Int,val stage:Int,val lastApproach:String,val intensity:Int)
data class Resolution(val campaign:Campaign,val outcome:String)

data class Campaign(
 val seed:Long,val name:String,val alias:String,val origin:String,val powerFamily:String,val weakness:String,val modifier:String,val pronouns:String="iel",val city:String="Vesper",val district:String="Centre",val socialBackground:String="Classe moyenne",val motivation:String="Protéger les miens",val visualStyle:String="Masque minimal",
 val turn:Int=0,val morality:Int=0,val prestige:Int=0,val opinion:Int=0,val fear:Int=0,val power:Int=28,val control:Int=25,val influence:Int=0,val health:Int=100,val civilianCasualties:Int=0,val identityExposure:Int=0,
 val familyBond:Int=50,val rivalStanding:Int=0,val governmentStanding:Int=0,val factionStanding:Int=0,val mediaStanding:Int=0,val flags:Set<String> = emptySet(),val threads:List<StoryThread> = emptyList(),val lastCategory:String="",val lastApproach:String="",val timeline:List<String> = emptyList()
){
 val age:Int get()=18+turn/4
 val scope:Scope get()=when{influence>=900->Scope.WORLD;influence>=560->Scope.COUNTRY;influence>=340->Scope.REGION;influence>=180->Scope.CITY;influence>=75->Scope.DISTRICT;else->Scope.STREET}
 val moralLabel:String get()=when{morality>=70->"Héroïque";morality>=35->"Bienveillant";morality>=10->"Altruiste";morality>-10->"Ambigu";morality>-35->"Impitoyable";morality>-70->"Corrompu";else->"Monstrueux"}
 val finished:Boolean get()=turn>=140||health<=0
}

private data class Authored(val id:String,val family:String,val arc:String,val minAge:Int,val maxAge:Int,val rarity:String,val weight:Double,val once:Boolean,val phase:String,val scopeMin:String,val originReq:String,val requiredFlag:String,val forbiddenFlag:String,val title:String,val text:String,val choices:List<AuthChoice>)
private data class AuthChoice(val label:String,val moral:Int,val prestige:Int,val opinion:Int,val fear:Int,val risk:Int,val tag:String,val flags:List<String>)

object GameEngine {
 val pronouns=listOf("il","elle","iel")
 val cities=listOf("Vesper","Greybridge","Noxhaven","Solara","Kade City","Oris","Meridian","Eidolon")
 val districts=listOf("Centre","Les Docks","Vieille-Ville","Nord-Est","Ceinture Sud","Hauteurs","Rives","Secteur industriel")
 val socialBackgrounds=listOf("Quartier populaire","Classe moyenne","Milieu privilégié","Foyer instable","Famille militaire","Milieu scientifique","Autodidacte précaire","Héritier d'une organisation")
 val motivations=listOf("Protéger les miens","Justice","Reconnaissance","Pouvoir","Liberté","Réparer une faute","Comprendre mes pouvoirs","Changer le système")
 val visualStyles=listOf("Masque minimal","Capuche tactique","Silhouette civile","Armure artisanale","Tenue symbolique","Visage découvert","Manteau long","Équipement modulaire")
 val origins=listOf("Mutation naturelle","Accident scientifique","Expérience clandestine","Programme militaire","Technologie personnelle","Héritage familial","Artefact mystérieux","Pacte occulte","Origine extraterrestre","Énergie cosmique","Entraînement humain extrême","Intelligence augmentée")
 val powers=listOf("Force","Résistance","Vitesse","Vol","Énergie","Feu","Glace","Électricité","Télékinésie","Télépathie","Illusion","Influence mentale limitée","Métamorphose","Invisibilité","Régénération","Technologie","Armes spécialisées","Magie","Matière","Gravité","Espace","Duplication","Invocation","Absorption","Adaptation","Humain exceptionnel")
 val weaknesses=listOf("Surcharge","Fatigue extrême","Concentration","Énergie externe","Fréquence sonore","Instabilité émotionnelle","Temps de récupération","Environnement","Vulnérabilité psychique","Vulnérabilité mystique","Pouvoir difficile à dissimuler","Précision limitée")
 private val modifiers=listOf("Âge des héros","Première génération","Société méfiante","Culture héroïque","État autoritaire","Criminalité endémique","Ère technologique","Menace occulte","Silence cosmique","Médias omniprésents")

 private val authored:List<Authored> by lazy { parseCatalog(decodeCatalog()) }

 fun randomBlueprint(seed:Long):CharacterBlueprint{val r=Random(seed);val fn=listOf("Malik","Nora","Elias","Maya","Soren","Lina","Ilyan","Kael","Naël","Ava","Milo","Yara","Nell","Zayn");val ln=listOf("Voss","Deren","Kess","Arden","Vale","Nox","Raine","Sol","Marek","Serrin","Vey","Korr");val al=listOf("Vesper","Axiom","Morrow","Cipher","Silex","Halo","Noctis","Vector","Rift","Cinder","Mantis","Aster");return CharacterBlueprint(fn[r.nextInt(fn.size)],ln[r.nextInt(ln.size)],al[r.nextInt(al.size)],pronouns[r.nextInt(pronouns.size)],cities[r.nextInt(cities.size)],districts[r.nextInt(districts.size)],socialBackgrounds[r.nextInt(socialBackgrounds.size)],origins[r.nextInt(origins.size)],powers[r.nextInt(powers.size)],weaknesses[r.nextInt(weaknesses.size)],motivations[r.nextInt(motivations.size)],visualStyles[r.nextInt(visualStyles.size)])}
 fun newCampaign(seed:Long,blueprint:CharacterBlueprint=randomBlueprint(seed)):Campaign{val r=Random(seed xor 0x51A7L);val ob=when(blueprint.origin){"Programme militaire","Entraînement humain extrême"->6;"Énergie cosmique","Origine extraterrestre"->9;"Technologie personnelle","Intelligence augmentée"->3;else->5};val cb=when(blueprint.socialBackground){"Famille militaire"->7;"Milieu scientifique"->5;"Foyer instable"->-3;else->1};val ms=when(blueprint.motivation){"Protéger les miens"->6;"Justice"->4;"Réparer une faute"->2;"Pouvoir"->-6;else->0};val ex=if(blueprint.visualStyle=="Visage découvert")38 else if(blueprint.visualStyle=="Silhouette civile")10 else 0;return Campaign(seed,blueprint.fullName,blueprint.alias.ifBlank{"Vesper"},blueprint.origin,blueprint.powerFamily,blueprint.weakness,modifiers[r.nextInt(modifiers.size)],blueprint.pronouns,blueprint.city,blueprint.district,blueprint.socialBackground,blueprint.motivation,blueprint.visualStyle,morality=ms,power=clamp(22+ob+r.nextInt(12),10,60),control=clamp(20+cb+r.nextInt(15),10,60),identityExposure=ex,familyBond=if(blueprint.motivation=="Protéger les miens")62 else 50,flags=setOf("origin:${blueprint.origin}","motivation:${blueprint.motivation}","background:${blueprint.socialBackground}"))}

 fun event(c:Campaign):EventNode{
  val eligible=authored.filter{a->c.age in a.minAge..a.maxAge && (a.originReq=="*"||c.origin==a.originReq) && (a.requiredFlag=="*"||a.requiredFlag in c.flags) && (a.forbiddenFlag=="*"||a.forbiddenFlag !in c.flags) && (!a.once||"seen:${a.id}" !in c.flags)}
  val pool=if(eligible.isNotEmpty()) eligible else authored.filter{it.requiredFlag=="*"}
  val maxWeight=pool.maxOfOrNull{it.weight}?:1.0
  val preferred=pool.filter{it.weight>=maxWeight}
  val chosen=preferred[positiveMod(mix(c.seed,c.turn.toLong()*97+preferred.size),preferred.size)]
  return toNode(chosen)
 }

 private fun toNode(a:Authored):EventNode{
  val stage=Regex("_S(\\d)(?:_|$)").find(a.id)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: if(a.id.contains("_EP_"))6 else 1
  val stakes=when(a.phase){"CRISIS","CLIMAX"->3;"TENSION","AFTERMATH"->2;else->1}
  return EventNode(a.id,a.title,a.text,a.choices.map{toChoice(a,it,stakes)},familyLabel(a.family),a.phase,stakes,a.arc,stage)
 }
 private fun toChoice(a:Authored,x:AuthChoice,stakes:Int):Choice{
  val route=when{ x.tag.contains("care_choice")||x.tag.startsWith("rescue") -> "PROTECT";x.tag.contains("order_choice")||x.tag.startsWith("countermeasure")||x.tag.startsWith("negotiation") -> "TACTICAL";x.tag.contains("truth_choice") -> "ANALYZE";x.tag.contains("ascend_choice") -> "AMBITION";else->"TACTICAL"}
  val pwr=if(route=="AMBITION")1 else 0; val impact=when(route){"AMBITION"->8;"ANALYZE"->5;"TACTICAL"->6;else->4};val rel=when(route){"PROTECT"->2;"AMBITION"->-1;else->0}
  return Choice(x.label,x.moral,x.prestige,x.opinion,x.fear,pwr,impact,x.risk,route,stakes,familyLabel(a.family),a.arc,rel,x.flags.joinToString("+"))
 }

 fun choose(c:Campaign,choice:Choice):Campaign{
  val weight=if(choice.stakes>=3)2 else 1;val nextTurn=c.turn+1;val injury=if(choice.risk>=4 && positiveMod(mix(c.seed,nextTurn.toLong()+choice.label.hashCode()),100)<choice.risk*6) max(1,choice.risk/2) else 0
  var flags=c.flags+choice.flag.orEmpty().split('+').filter{it.isNotBlank()}+"seen_event_turn:${c.turn}"
  val current=event(c);flags=flags+"seen:${current.id}"
  val activeArc=current.threadId
  var threads=c.threads
  if(activeArc!=null){ val completeFlag="${activeArc}_COMPLETE"; val existing=threads.firstOrNull{it.id==activeArc}; if(completeFlag in flags||current.id.contains("_EP_")) threads=threads.filterNot{it.id==activeArc} else if(existing==null) threads=(threads+StoryThread(activeArc,c.turn,c.turn,current.threadStage,choice.approach,choice.stakes)).takeLast(6) else threads=threads.map{if(it.id==activeArc)it.copy(lastTurn=c.turn,stage=max(it.stage,current.threadStage),lastApproach=choice.approach,intensity=max(it.intensity,choice.stakes)) else it}}
  val relation=choice.relationDelta*weight
  val family=clamp(c.familyBond+if(choice.sourceCategory in setOf("FAMILLE","CIVIL","HEALTH","TRAUMA"))relation else 0,0,100)
  val rival=clamp(c.rivalStanding+if(choice.sourceCategory=="RIVAL")relation else 0,-100,100)
  val gov=clamp(c.governmentStanding+if(choice.sourceCategory in setOf("POLITIQUE","GOUVERNEMENT"))relation else 0,-100,100)
  val faction=clamp(c.factionStanding+if(choice.sourceCategory=="FACTION")relation else 0,-100,100)
  val media=clamp(c.mediaStanding+if(choice.sourceCategory in setOf("MÉDIAS","MEDIA"))relation else 0,-100,100)
  val exposure=if(choice.approach=="ANALYZE"&&choice.risk>=4)2 else if(choice.approach=="AMBITION"&&choice.risk>=4)1 else 0
  val summary="${c.age} ans — ${current.title} → ${choice.label}"
  return c.copy(turn=nextTurn,morality=clamp(c.morality+choice.moral*weight,-100,100),prestige=max(0,c.prestige+choice.prestige*weight),opinion=clamp(c.opinion+choice.opinion*weight,-100,100),fear=clamp(c.fear+choice.fear*weight,0,100),power=clamp(c.power+choice.power+(if(nextTurn%12==0)1 else 0),0,100),control=clamp(c.control+if(choice.approach in setOf("ANALYZE","TACTICAL"))1 else 0,0,100),influence=max(0,c.influence+choice.impact*weight),health=clamp(c.health-injury,0,100),identityExposure=clamp(c.identityExposure+exposure,0,100),familyBond=family,rivalStanding=rival,governmentStanding=gov,factionStanding=faction,mediaStanding=media,flags=flags,threads=threads,lastCategory=choice.sourceCategory,lastApproach=choice.approach,timeline=(c.timeline+summary).takeLast(120))
 }

 fun resolve(c:Campaign,event:EventNode,choice:Choice):Resolution{val next=choose(c,choice);val deltaM=next.morality-c.morality;val deltaP=next.prestige-c.prestige;val route=when(choice.approach){"PROTECT"->"Tu as privilégié les personnes et les liens.";"TACTICAL"->"Tu as imposé un cadre et cherché à garder la situation sous contrôle.";"ANALYZE"->"Tu as choisi la piste de la vérité, au risque d'exposer davantage ce qui était caché.";"AMBITION"->"Tu as transformé la crise en levier de puissance et d'influence.";else->"Ton choix modifie la suite de cette histoire."};val continuation=if(next.threads.any{it.id==event.threadId})" Cette décision ouvre directement une variante différente du prochain chapitre de cet arc." else " Cet arc vient d'atteindre une conclusion persistante dans ta carrière.";val outcome="$route Moralité ${signed(deltaM)}, prestige ${signed(deltaP)}.$continuation";return Resolution(next.copy(timeline=(next.timeline+"↳ $outcome").takeLast(120)),outcome)}

 fun legacyTitle(c:Campaign):String{val heroic=c.morality>=25;return when(c.scope){Scope.STREET->if(heroic)"Gardien de la rue" else "Prédateur local";Scope.DISTRICT->if(heroic)"Protecteur du quartier" else "Terreur du quartier";Scope.CITY->if(heroic)"Gardien métropolitain" else "Fléau métropolitain";Scope.REGION->if(heroic)"Défenseur régional" else "Seigneur criminel";Scope.COUNTRY->if(heroic)"Symbole de la nation" else "Ennemi public national";Scope.WORLD->if(heroic)"Gardien de la Terre" else "Ennemi de l'humanité"}}
 fun legacyScore(c:Campaign)=max(0,c.prestige+c.influence/2+c.power*2+abs(c.morality)*2+c.turn-c.civilianCasualties*2)

 private fun familyLabel(f:String)=when(f.uppercase()){ "POLITICS"->"POLITIQUE";"MEDIA"->"MÉDIAS";"IDENTITY"->"IDENTITÉ";"POWER"->"POUVOIR";else->f.uppercase()}
 private fun signed(v:Int)=if(v>=0)"+$v" else "$v"
 private fun clamp(v:Int,lo:Int,hi:Int)=min(hi,max(lo,v))
 private fun positiveMod(v:Long,m:Int)=if(m<=0)0 else ((v and Long.MAX_VALUE)%m.toLong()).toInt()
 private fun mix(a:Long,b:Long):Long{var z=a+0x9E3779B97F4A7C15UL.toLong()+b*0xBF58476D1CE4E5B9UL.toLong();z=(z xor(z ushr 30))*0xBF58476D1CE4E5B9UL.toLong();z=(z xor(z ushr 27))*0x94D049BB133111EBUL.toLong();return z xor(z ushr 31)}
 private fun decodeCatalog():String{val b64=NARRATIVE_PART_01+NARRATIVE_PART_02+NARRATIVE_PART_03+NARRATIVE_PART_04+NARRATIVE_PART_05;val bytes=Base64.getDecoder().decode(b64);return GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(Charsets.UTF_8).use{it.readText()}}
 private fun parseCatalog(raw:String):List<Authored>{return raw.lineSequence().filter{it.isNotBlank()&&!it.startsWith("#")}.mapNotNull{line->val p=line.split('\t');if(p.size<17)return@mapNotNull null;val cs=p[16].split(";;").mapNotNull{c->val q=c.split('~');if(q.size<7)null else {val tagParts=q[6].split('+');AuthChoice(q[0],q[1].toIntOrNull()?:0,q[2].toIntOrNull()?:0,q[3].toIntOrNull()?:0,q[4].toIntOrNull()?:0,q[5].toIntOrNull()?:0,tagParts.first(),tagParts.drop(1))}};Authored(p[0],p[1],p[2],p[3].toIntOrNull()?:18,p[4].toIntOrNull()?:99,p[5],p[6].toDoubleOrNull()?:1.0,p[8].toBooleanStrictOrNull()?:false,p[9],p[10],p[11],p[12],p[13],p[14],p[15],cs)}.toList()}
}
