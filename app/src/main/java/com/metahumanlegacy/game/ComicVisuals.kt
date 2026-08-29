package com.metahumanlegacy.game

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.security.MessageDigest
import kotlin.math.absoluteValue

internal const val COMIC_TILE = 56

internal enum class ComicAsset(val atlas: Int, val col: Int) {
    BRAND_HERO(1,0), BRAND_VILLAIN(1,1), ROUTE_CARE(1,2), ROUTE_ORDER(1,3), ROUTE_TRUTH(1,4),
    ROUTE_ASCEND(2,0), MORALITY_HERO(2,1), MORALITY_NEUTRAL(2,2), MORALITY_VILLAIN(2,3), RANK_BRONZE(2,4),
    RANK_GOLD(3,0), RANK_LEGEND(3,1), SCOPE_STREET(3,2), SCOPE_DISTRICT(3,3), SCOPE_CITY(3,4),
    SCOPE_REGION(4,0), SCOPE_COUNTRY(4,1), SCOPE_WORLD(4,2), DANGER_LOW(4,3), DANGER_HIGH(4,4),
    DANGER_EXTREME(5,0), POWER_STRENGTH(5,1), POWER_SPEED(5,2), POWER_SENSES(5,3), POWER_FIRE(5,4),
    POWER_ICE(6,0), POWER_WATER(6,1), POWER_WIND(6,2), POWER_ENERGY(6,3), POWER_COSMIC(6,4),
    POWER_FORCEFIELD(7,0), POWER_TIME(7,1), ORIGIN_PSYCHIC(7,2), ORIGIN_MYSTIC(7,3), ORIGIN_UNKNOWN(7,4),
    RELATION_FAMILY(8,0), RELATION_RIVAL(8,1), RELATION_MEDIA(8,2), PUBLIC_FEAR(8,3), FACTION_HERO(8,4),
    ALT_01(9,0), ALT_02(9,1), ALT_03(9,2), ALT_04(9,3), ALT_05(9,4),
    ALT_06(10,0), ALT_07(10,1), ALT_08(10,2), ALT_09(10,3), ALT_10(10,4)
}

private object ComicAtlasStore {
    private val cache = mutableMapOf<Int, ImageBitmap>()
    private val expected = mapOf(
        1 to "417fe158e731654200fc1ea16f0f4601cf0842ec1cf2e7f03c1438e923195977",
        2 to "780ef848f0ac6fe18ceca4509368366a910d3567e99b7e3f0b6b698132391abe",
        3 to "ef855cf004a854e81d28e169fadd03e976dc6d5199f9e9c1fae014db8cdbd086",
        4 to "d0d842a655e023b3de3dc1564d0af19127072b6756f65bf9bde4db08d619fd24",
        5 to "b51aae02643b6fcfb0888629a0a2e0dd09cc3ebe04b959dfc16ef2cb62016b21",
        6 to "a078164d425ec9ee7a49c225d074cc73a583db643219003bd09c4b5c5975326c",
        7 to "b6436f4cac3b87b65affb87f2461ebdc7093274d020ecaaf7078eaa147a97266",
        8 to "0bf14aa0ae722a1951cf0b0258835ed242e3df017f52c799e64652e62e018da1",
        9 to "1b36a60293d72458e0984c5a6d5384d5cf57c4da37b1d2b63e94b4b43580103c",
        10 to "d33446f16368240578e932f116eb0210c8c2100b444765f344fc315f0af153c8"
    )

    @Synchronized fun image(context: Context, atlas: Int): ImageBitmap = cache.getOrPut(atlas) {
        val base = "comic_atlas/ca_${atlas.toString().padStart(2,'0')}"
        val encoded = buildString(14_000) {
            append(context.assets.open("${base}_a.b64").bufferedReader(Charsets.US_ASCII).use { it.readText().trim() })
            append(context.assets.open("${base}_b.b64").bufferedReader(Charsets.US_ASCII).use { it.readText().trim() })
        }
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        require(sha == expected.getValue(atlas)) { "Comic atlas $atlas SHA-256 mismatch" }
        val bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.size) ?: error("Comic atlas $atlas cannot decode")
        require(bitmap.width == COMIC_TILE * 5 && bitmap.height == COMIC_TILE) { "Comic atlas $atlas dimensions invalid" }
        bitmap.asImageBitmap()
    }
}

@Composable internal fun ComicIcon(asset: ComicAsset, modifier: Modifier = Modifier, alpha: Float = 1f) {
    val context = LocalContext.current
    val image = remember(asset.atlas) { ComicAtlasStore.image(context.applicationContext, asset.atlas) }
    Canvas(modifier) {
        drawImage(
            image=image,
            srcOffset=IntOffset(asset.col*COMIC_TILE,0), srcSize=IntSize(COMIC_TILE,COMIC_TILE),
            dstOffset=IntOffset.Zero, dstSize=IntSize(size.width.toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1)),
            alpha=alpha, filterQuality=FilterQuality.High
        )
    }
}

@Composable internal fun CampaignSigil(seed: Long, modifier: Modifier=Modifier)=ComicIcon(variantAsset(seed),modifier)

internal fun variantAsset(seed: Long): ComicAsset {
    val v=listOf(ComicAsset.ALT_01,ComicAsset.ALT_02,ComicAsset.ALT_03,ComicAsset.ALT_04,ComicAsset.ALT_05,ComicAsset.ALT_06,ComicAsset.ALT_07,ComicAsset.ALT_08,ComicAsset.ALT_09,ComicAsset.ALT_10)
    return v[(seed%v.size).toInt().absoluteValue]
}
internal fun routeAsset(a:String)=when(a.uppercase()){
    "CARE","PROTECT","HUMAN"->ComicAsset.ROUTE_CARE; "ORDER","TACTICAL","CONTROL"->ComicAsset.ROUTE_ORDER
    "TRUTH","ANALYZE","REVEAL"->ComicAsset.ROUTE_TRUTH; "ASCEND","AMBITION","DOMINATE"->ComicAsset.ROUTE_ASCEND
    else->ComicAsset.BRAND_HERO
}
internal fun scopeAsset(s:Scope)=when(s){Scope.STREET->ComicAsset.SCOPE_STREET;Scope.DISTRICT->ComicAsset.SCOPE_DISTRICT;Scope.CITY->ComicAsset.SCOPE_CITY;Scope.REGION->ComicAsset.SCOPE_REGION;Scope.COUNTRY->ComicAsset.SCOPE_COUNTRY;Scope.WORLD->ComicAsset.SCOPE_WORLD}
internal fun moralityAsset(c:Campaign)=when{c.morality>=28&&c.fear<55->ComicAsset.MORALITY_HERO;c.morality<=-28||c.fear>=70->ComicAsset.MORALITY_VILLAIN;else->ComicAsset.MORALITY_NEUTRAL}
internal fun dangerAsset(r:Int)=when{r>=7->ComicAsset.DANGER_EXTREME;r>=4->ComicAsset.DANGER_HIGH;else->ComicAsset.DANGER_LOW}
internal fun rankAsset(p:Int)=when{p>=75->ComicAsset.RANK_LEGEND;p>=35->ComicAsset.RANK_GOLD;else->ComicAsset.RANK_BRONZE}
internal fun powerAsset(f:String):ComicAsset{val p=f.lowercase();return when{
    "feu" in p||"therm" in p||"flamme" in p->ComicAsset.POWER_FIRE;"glace" in p||"froid" in p||"cryo" in p->ComicAsset.POWER_ICE
    "eau" in p||"hydro" in p||"océan" in p->ComicAsset.POWER_WATER;"air" in p||"vent" in p||"aéro" in p->ComicAsset.POWER_WIND
    "vitesse" in p||"rapid" in p->ComicAsset.POWER_SPEED;"force" in p||"phys" in p||"densité" in p->ComicAsset.POWER_STRENGTH
    "sens" in p||"vision" in p||"perception" in p->ComicAsset.POWER_SENSES;"temps" in p||"chron" in p->ComicAsset.POWER_TIME
    "bouclier" in p||"champ" in p||"barrière" in p->ComicAsset.POWER_FORCEFIELD;"cosm" in p||"grav" in p||"espace" in p->ComicAsset.POWER_COSMIC
    "psych" in p||"mental" in p||"télépath" in p->ComicAsset.ORIGIN_PSYCHIC;"mag" in p||"myst" in p||"occul" in p->ComicAsset.ORIGIN_MYSTIC
    else->ComicAsset.POWER_ENERGY}}
internal fun relationAsset(k:String)=when(k.uppercase()){ "FAMILY"->ComicAsset.RELATION_FAMILY;"RIVAL"->ComicAsset.RELATION_RIVAL;"MEDIA"->ComicAsset.RELATION_MEDIA;"GOVERNMENT","FACTION"->ComicAsset.FACTION_HERO;else->ComicAsset.RELATION_FAMILY }

@Composable internal fun ComicWorldBackdrop(scope:Scope,seed:Long,modifier:Modifier=Modifier){Canvas(modifier){
    drawRect(Color(0xFF080A0E));val horizon=size.height*.70f;val step=size.width/11f
    repeat(11){i->val h=size.height*(.06f+(((i*37+scope.ordinal*19+(seed%23).toInt())%17)/100f));drawRect(Color(0xFF0D2A48).copy(alpha=.30f),Offset(i*step,horizon-h),Size(step-3f,h))}
    val slash=Path().apply{moveTo(size.width*.63f,0f);lineTo(size.width,0f);lineTo(size.width,size.height*.48f);lineTo(size.width*.84f,size.height*.36f);close()};drawPath(slash,Color(0x221E0808))
    val beam=Path().apply{moveTo(-size.width*.1f,size.height*.15f);lineTo(size.width*.58f,0f);lineTo(size.width*.66f,0f);lineTo(0f,size.height*.23f);close()};drawPath(beam,Color(0x33F4C44E))
    var y=24f;while(y<size.height){var x=if(((y/18f).toInt() and 1)==0)12f else 21f;while(x<size.width){drawCircle(Color.White.copy(alpha=.018f*(1f-y/size.height).coerceIn(0f,1f)),1.7f,Offset(x,y));x+=18f};y+=18f}
}}
@Composable internal fun ComicBurst(modifier:Modifier=Modifier,accent:Color=Color(0xFFF4C44E)){Canvas(modifier){val cx=size.width/2;val cy=size.height/2;repeat(24){i->val a=Math.toRadians(i*15.0);val inner=size.minDimension*.34f;val outer=size.minDimension*if(i%2==0).50f else .43f;drawLine(accent.copy(alpha=.18f),Offset(cx+kotlin.math.cos(a).toFloat()*inner,cy+kotlin.math.sin(a).toFloat()*inner),Offset(cx+kotlin.math.cos(a).toFloat()*outer,cy+kotlin.math.sin(a).toFloat()*outer),if(i%2==0)2.2f else 1.2f)}}}
