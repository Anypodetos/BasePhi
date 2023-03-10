/*
Copyright 2022 Anypodetos (Michael Weber)

BasePhi.kt is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

BasePhi.kt is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with BasePhi.kt. If not, see <https://www.gnu.org/licenses/>.

Contact: <https://lemizh.conlang.org/home/contact.php?about=qonvert>
*/

import java.math.BigInteger
import java.math.BigInteger.*
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

typealias BigFraction = Pair<BigInteger, BigInteger>
val FRACTION_ZERO = Pair(ZERO, ONE)
val FRACTION_ONE = Pair(ONE, ONE)
val FRACTION_INFINITY = Pair(ONE, ZERO)
val FRACTION_MU = Pair(ZERO, ZERO)

val PHI_ZERO = PhiNumber(ZERO)
val PHI_ONE = PhiNumber(ONE)
val PHI = PhiNumber(ZERO, ONE)
val INV_PHI = PhiNumber(-ONE, ONE)
val PHI_INFINITY = PhiNumber(ONE, ZERO, ZERO)

val IS_NUMBER = 0x0001
val IS_COMPLEMENT = 0x0002
val IS_FRACTION = 0x0004
val IS_MIXED = 0x0008
val IS_CONTINUED = 0x0010
val IS_EGYPTIAN = 0x0020
val IS_ANY_FRACTION = IS_FRACTION or IS_MIXED or IS_CONTINUED or IS_EGYPTIAN
val NONSTANDARD_DIGIT = 0x0100
val INVALID_CHAR = 0x0200

val DIGITS = "0123456789abcdefghijklmnopqrstuvwxyz"
val FRACTION_CHARS  = mapOf('½' to Pair(1, 2), '⅓' to Pair(1, 3), '¼' to Pair(1, 4), '⅕' to Pair(1, 5), '⅙' to Pair(1, 6),
    '⅐' to Pair(1, 7), '⅛' to Pair(1, 8), '⅑' to Pair(1, 9),'⅒' to Pair(1, 10), '⅔' to Pair(2, 3), '¾' to Pair(3, 4),
    '⅖' to Pair(2, 5), '⅗' to Pair(3, 5), '⅘' to Pair(4, 5), '⅚' to Pair(5, 6), '⅜' to Pair(3, 8), '⅝' to Pair(5, 8),
    '⅞' to Pair(7, 8), '↉' to Pair(0, 1)).mapValues { Pair(it.value.first.toBigInteger(), it.value.second.toBigInteger()) }

class PhiNumber(var aNumer: BigInteger, var bNumer: BigInteger = ZERO, var denom: BigInteger = ONE, root5: Boolean = false) {

    var parseResult = 0
    private val five = 5.toBigInteger()

    val a get() = reduceFraction(aNumer, denom)
    val b get() = reduceFraction(bNumer, denom)
    val r get() = reduceFraction(TWO * aNumer + bNumer, TWO * denom)
    val s get() = reduceFraction(bNumer, TWO * denom)

    init {
        if (root5) {
            aNumer -= bNumer
            bNumer *= TWO
        }
        reduce()
    }

    constructor(aNumer: Int, bNumer: Int = 0, denom: Int = 1, root5: Boolean = false):
        this(aNumer.toBigInteger(), bNumer.toBigInteger(), denom.toBigInteger(), root5)
    constructor(p: PhiNumber): this(p.aNumer, p.bNumer, p.denom)
    constructor(a: BigFraction): this(a.first, ZERO, a.second)

    constructor(phinary: String, continuedOrEgyptian: Int = 0, negative: Boolean = false): this(ZERO) {
        val st = phinary.filterNot { it.isWhitespace() }
        val semi = st.indexOf(';')
        when {
            semi == -1 -> parseFraction(st, negative)
            continuedOrEgyptian and IS_EGYPTIAN != 0 -> { /* Egyptian fraction */
                parseResult = parseResult or IS_NUMBER or IS_EGYPTIAN
                var x = parseFraction(st.substring(0, semi), negative)
                for ((i, subSt) in st.substring(semi + 1).split(',').reversed().withIndex())
                    x += parseFraction(subSt, negative, if (i > 0) FRACTION_ZERO else FRACTION_INFINITY).inv()
                x
            }
            else -> { /* continued fraction */
                parseResult = parseResult or IS_NUMBER or IS_CONTINUED
                var x = PHI_INFINITY
                for ((i, subSt) in ((st.substring(semi + 1).split(',')).reversed() + st.substring(0, semi)).withIndex())
                    x = parseFraction(subSt, negative, if (i > 0) FRACTION_ZERO else FRACTION_INFINITY) + x.inv()
                x
            }
        }.let {
            aNumer = it.aNumer
            bNumer = it.bNumer
            denom = it.denom
            reduce()
        }
    }

    private fun parseFraction(s: String, negative: Boolean, default: BigFraction = FRACTION_ZERO): PhiNumber {
        var under = s.indexOf('_')
        var slash = s.indexOf('/')
        if (slash > -1 && under > slash) under = -1
        if (under > -1) parseResult = parseResult or IS_NUMBER or IS_MIXED
        if (slash > -1) parseResult = parseResult or IS_NUMBER or IS_FRACTION
        return if (slash == -1 && under == -1) parsePositional(s, negative, default) else {
            if (slash == -1) slash = s.length
            parsePositional(s.substring(0, max(under, 0)), negative) +
                parsePositional(s.substring(under + 1, slash), negative) /
                parsePositional(s.substring(min(slash + 1, s.length)), negative, default = FRACTION_ONE) *
                    if (under > -1 && s.startsWith("-")) -ONE else ONE
        }
    }

    private fun parsePositional(s: String, negative: Boolean, default: BigFraction = FRACTION_ZERO): PhiNumber {
        with(mapOf("∞" to ONE, "-∞" to ONE, "無" to ZERO, "-無" to ZERO)[s]) {
            if (this != null) {
                parseResult = parseResult or IS_NUMBER
                return PhiNumber(this, ZERO, ZERO)
            }
        }
        val complementTrim = if (s.startsWith("..")) 2 else 0
        val fraction = FRACTION_CHARS[s.lastOrNull()]
        val st = (if (fraction == null) s else {
            if (fraction.first > ONE || fraction.second in TWO..TEN - ONE) parseResult = parseResult or NONSTANDARD_DIGIT
            s.dropLast(1)                // Kotlin 1.8: in TWO..<TEN
        }) + if ('.' in s.substring(complementTrim)) "" else "."
        val point = st.indexOf('.', complementTrim)
        val rep = st.indexOf(':')
        val noRep = rep == -1 || rep == st.length - if (point < rep) 1 else 2
        val n = parseFinitePhinary((if (noRep) st else st.filterIndexed { i, _ -> i != point }).replaceFirst(":", ""), negative)
        parseResult = parseResult or n.parseResult or
            (if (fraction != null) IS_NUMBER or (if (n.parseResult and IS_NUMBER == 0) IS_FRACTION else IS_MIXED) else 0)

        return if (n.parseResult and IS_NUMBER == 0 && fraction == null) default.toPhiNumber() else {
            if (noRep) n else (n - parseFinitePhinary(st.substring(0, rep).filterIndexed { i, _ -> i != point }, negative)) /
                (phiPower(st.length - point - if (point < rep) 2 else 1, negative) - phiPower(rep - point + if (point < rep) -1 else +1, negative))
        } + (if (fraction?.second != TEN) PhiNumber(fraction ?: BigFraction(ZERO, ONE)) else INV_PHI)
    }

    operator fun plus(other: PhiNumber) = PhiNumber(aNumer * other.denom + other.aNumer * denom,
                                                    bNumer * other.denom + other.bNumer * denom, denom * other.denom)
    operator fun plus(other: BigFraction) = PhiNumber(aNumer * other.second + other.first * denom, bNumer * other.second, denom * other.second)
    operator fun plus(other: BigInteger) = PhiNumber(aNumer + denom * other, bNumer, denom)

    operator fun inc() = PhiNumber(aNumer + denom, bNumer, denom)
    operator fun unaryPlus() = PhiNumber(this)

    operator fun minus(other: PhiNumber) = PhiNumber(aNumer * other.denom - other.aNumer * denom,
                                                     bNumer * other.denom - other.bNumer * denom, denom * other.denom)
    operator fun minus(other: BigFraction) = PhiNumber(aNumer * other.second - other.first * denom, bNumer * other.second, denom * other.second)
    operator fun minus(other: BigInteger) = PhiNumber(aNumer - denom * other, bNumer, denom)

    operator fun dec() = PhiNumber(aNumer - denom, bNumer, denom)
    operator fun unaryMinus() = PhiNumber(-aNumer, -bNumer, denom)

    operator fun times(other: PhiNumber) = PhiNumber(aNumer * other.aNumer + bNumer * other.bNumer,
        aNumer * other.bNumer + other.aNumer * bNumer + bNumer * other.bNumer, denom * other.denom)
    operator fun times(other: BigFraction) = PhiNumber(aNumer * other.first, bNumer * other.first, denom * other.second)
    operator fun times(other: BigInteger) = PhiNumber(aNumer * other, bNumer * other, denom)

    operator fun div(other: PhiNumber) =  /* (a+bφ)/(c+dφ) = (a+bφ)(c+d-dφ) / (c+dφ)(c+d-dφ) = [(ac+ad-bd)+(bc-ad)φ] / (c²+cd-d²) */
        if (other == PHI_ZERO) PhiNumber(aNumer.abs() + bNumer.abs(), ZERO, ZERO) else PhiNumber(
            other.denom * (aNumer * other.aNumer + aNumer * other.bNumer - bNumer * other.bNumer),
            other.denom * (bNumer * other.aNumer - aNumer * other.bNumer),
            denom * (other.aNumer * other.aNumer + other.aNumer * other.bNumer - other.bNumer * other.bNumer))
    operator fun div(other: BigFraction) = if (other == FRACTION_ZERO) PhiNumber(aNumer.abs() + bNumer.abs(), ZERO, ZERO) else
        PhiNumber(aNumer * other.second, bNumer * other.second, denom * other.first)
    operator fun div(other: BigInteger) = if (other == ZERO) PhiNumber(aNumer.abs() + bNumer.abs(), ZERO, ZERO) else
        PhiNumber(aNumer, bNumer, denom * other)

    fun inv() = if (this == PHI_ZERO) PHI_INFINITY else PhiNumber((aNumer + bNumer) * denom, -bNumer * denom,
        aNumer * aNumer + aNumer * bNumer - bNumer * bNumer)

    operator fun compareTo(other: PhiNumber): Int {
        if (denom == ZERO) return 1
        if (other.denom == ZERO) return -1
        val cNumer = other.bNumer * denom - bNumer * other.denom
        val dNumer = TWO * (aNumer * other.denom - other.aNumer * denom) - cNumer
        val denomSignum = (denom * other.denom).signum()
        val cSignum = cNumer.signum() * denomSignum
        val dSignum = dNumer.signum() * denomSignum
        return if (dSignum == cSignum) (dNumer * dNumer).compareTo(five * cNumer * cNumer) * cSignum else dSignum - cSignum
    }

    operator fun compareTo(other: BigFraction): Int {
        if (denom == ZERO) return 1
        if (other.second == ZERO) return -1
        val cNumer = -bNumer * other.second
        val dNumer = TWO * (aNumer * other.second - other.first * denom) - cNumer
        val denomSignum = (denom * other.second).signum()
        val cSignum = cNumer.signum() * denomSignum
        val dSignum = dNumer.signum() * denomSignum
        return if (dSignum == cSignum) (dNumer * dNumer).compareTo(five * cNumer * cNumer) * cSignum else dSignum - cSignum
    }

    operator fun compareTo(other: BigInteger): Int {
        if (denom == ZERO) return 1
        val dNumer = TWO * (aNumer - other * denom) + bNumer
        val cSignum = -bNumer.signum() * denom.signum()
        val dSignum = dNumer.signum() * denom.signum()
        return if (dSignum == cSignum) (dNumer * dNumer).compareTo(five * bNumer * bNumer) * cSignum else dSignum - cSignum
    }

    override operator fun equals(other: Any?) = if (other is PhiNumber)
        aNumer == other.aNumer && bNumer == other.bNumer && denom == other.denom else false

    override fun hashCode() = Objects.hash(aNumer, bNumer, denom)

    fun abs() = if (this >= ZERO) PhiNumber(this) else -this

    override fun toString() = toString(root5 = false)

    fun toString(root5: Boolean, radix: Int = 10) = if (root5) with(s) {
        formatNumberPair(r.toFractionString(radix), first.toString(radix), "√5̅" + (if (second == ONE) "" else "/" + second.toString(radix)))
    } else formatNumberPair(a.toFractionString(radix), b.first.toString(radix), "φ" + (if (b.second == ONE) "" else "/" + b.second.toString(radix)))

    fun toBasePhi(groupDigits: Boolean = false, maxDigits: Int = Int.MAX_VALUE, alt: Boolean = false, complement: Boolean = false, negative: Boolean = false):
            String {
        var (result, x) = phintegerPart(complement, negative)
        val point = if (x > ZERO) result.length else -1
        if (point > -1) {
            result.append('.')
            val remainders = mutableListOf<PhiNumber>()
            do {
                remainders.add(x)
                x *= PHI                                           // if (negative) -PHI else PHI
                result.append(if (x >= ONE) { x -= ONE; '1' } else '0')
            } while (x > ZERO && x !in remainders && remainders.size <= maxDigits)
            val rep = remainders.indexOf(x)
            if (rep > -1) result.insert(point + rep + 1, ':')
            if (remainders.size > maxDigits) result[result.lastIndex] = '…'
        }
        if (alt && result.toString() == "0") return "..1010.:10"
        if (alt && ':' !in result && '…' !in result) {
            if (point > -1) result.deleteAt(result.lastIndex).append(":01") else {
                val last1 = result.lastIndexOf('1')
                result[last1] = '0'
                for (i in last1 + 1 until result.length) if ((i - last1) % 2 != 0) result[i] = '1'
                result.append(".:" + if ((last1 - result.length) % 2 == 0) "01" else "10")
                if (result.startsWith("01")) result.deleteAt(0)
            }
        }
        return insertSpaces(result, groupDigits, point)
    }

    fun toBasePhiFraction(groupDigits: Boolean = false, complement: Boolean = false, negative: Boolean = false): String {
        if (denom == ZERO) return if (aNumer == ZERO && bNumer == ZERO) "0/0" else "1/0"
        val numerString = StringBuilder(PhiNumber(aNumer, bNumer).toBasePhi(complement = complement))                // base -φ
        if (denom == ONE && '.' !in numerString.removePrefix("..")) return insertSpaces(numerString, groupDigits)
        val denomString = StringBuilder(PhiNumber(denom).toBasePhi())
        val fractionalDiff = with(numerString.indexOf('.', if (numerString.startsWith("..")) 2 else 0)) { if (this > -1) {
            numerString.deleteAt(this)
            numerString.length - this
        } else 0 } - with(denomString.indexOf('.')) { if (this > -1) {
            denomString.deleteAt(this)
            denomString.length - this
        } else 0 }
        return insertSpaces(numerString.append("0".repeat((-fractionalDiff).coerceAtLeast(0))), groupDigits).trimStart('0', ' ') + "/" +
               insertSpaces(denomString.append("0".repeat(  fractionalDiff .coerceAtLeast(0))), groupDigits).trimStart('0', ' ')
    }

    fun toBasePhiMixed(groupDigits: Boolean = false, complement: Boolean = false, negative: Boolean = false): String {
        if (abs() <= ONE || denom == ZERO) return toBasePhiFraction(groupDigits, complement, negative)
        val (integer, fraction) = phintegerPart(complement, negative)
        return insertSpaces(integer, groupDigits) + if (fraction > ZERO) '_' + fraction.toBasePhiFraction(groupDigits) else ""
    }

    fun toBasePhiContinued(groupDigits: Boolean = false, complement: Boolean = false, negative: Boolean = false) =
        continuedFraction(this, groupDigits, complement, negative)

    private tailrec fun continuedFraction(x: PhiNumber, groupDigits: Boolean = false, complement: Boolean = false, negative: Boolean = false, pre: String = ""): String {
        var (integer, fraction) = x.phintegerPart(complement)
        if (x < ZERO && !complement && fraction != PHI_ZERO) {
            val diff = if (integer.endsWith('0')) PHI_ONE else INV_PHI
            with((x - diff).phintegerPart(complement)) { integer = first; fraction = diff - second }
        }
        val st = "$pre, ${insertSpaces(integer, groupDigits)}"
        return if (fraction == PHI_ZERO) st.substring(2).replaceFirst(',', ';') else
            continuedFraction(fraction.inv(), groupDigits, complement, negative, st)
    }

    private fun phintegerPart(complement: Boolean = false, negative: Boolean = false): Pair<StringBuilder, PhiNumber> {
        if (denom == ZERO) return Pair(StringBuilder(if (aNumer == ZERO && bNumer == ZERO) "NaN" else "Infinity"), PHI_ZERO)
        if (this < ZERO && !complement && !negative) return with((-this).phintegerPart()) { Pair(first.insert(0, '-'), second) } // base -φ
        var x = abs()
        val base = if (negative) -PHI else PHI // base -φ
        val powers = mutableListOf(PHI_ONE, base)
        while (x >= powers.last()) powers.add(powers.last() * base)
        if (this < ZERO && complement && !negative) {
            if (powers.size % 2 == 0) powers.add(powers.last() * base)
            x = powers.last() - x
        }
        val result = StringBuilder()
        for (p in powers.dropLast(1).reversed()) result.append(if (x >= p) { x -= p; '1' } else '0')
        if (this < ZERO && complement && !negative) result.insert(0, if (result.startsWith("10")) "..10" else "..1010")
        return Pair(result, x)
    }

    private fun insertSpaces(s: StringBuilder, groupDigits: Boolean, point: Int = -1): String {
        if (groupDigits) {
            val cutOffPos = s.length - if (s.endsWith('…')) 1 else 0
            var pos = if (point > -1) point - cutOffPos + (if (':' in s) 2 else 1) else (if (".:" in s) -2 else 0)
            for (i in cutOffPos - 2 downTo 0) if (s[i] in "01" && ++pos % 4 == 0 && pos != 0) s.insert(i + 1, ' ')
        }
        return s.toString()
    }

    fun toFloat() = (aNumer.toFloat() + bNumer.toFloat() * (1f + sqrt(5f)) / 2f) / denom.toFloat()
    fun toDouble() = (aNumer.toDouble() + bNumer.toDouble() * (1.0 + sqrt(5.0)) / 2.0) / denom.toDouble()

    fun reduce() {
        if (denom == ZERO) {
            aNumer = if (aNumer == ZERO && bNumer == ZERO) ZERO else ONE
            bNumer = ZERO
        } else {
            var gcd = aNumer.gcd(bNumer).gcd(denom).coerceAtLeast(ONE)
            if (denom < ZERO) gcd = -gcd
            aNumer /= gcd
            bNumer /= gcd
            denom /= gcd
        }
    }
}

fun String.toPhiNumber(continuedOrEgyptian: Int = 0, negative: Boolean = false) = PhiNumber(this, continuedOrEgyptian, negative)
fun Int.toPhiNumber() = PhiNumber(this)
fun BigInteger.toPhiNumber() = PhiNumber(this)
fun BigFraction.toPhiNumber() = PhiNumber(this)

fun BigFraction.toBasePhi() = toPhiNumber().toBasePhi()
fun BigFraction.toFractionString(radix: Int = 10) = first.toString(radix) + (if (second == ONE) "" else "/" + second.toString(radix))

fun phiPower(n: Int) = if (n >= 1) matrixFibonacci(n).let {
        PhiNumber(it.first, it.second)
    } else matrixFibonacci(1 - n).let {
        if (n % 2 == 0) PhiNumber(it.second, -it.first) else PhiNumber(-it.second, it.first)
    }

fun phiPower(n: Int, negative: Boolean) = if (negative && n % 2 != 0) -phiPower(n) else phiPower(n)

fun fibonacci(n: Int): BigInteger = when (n.sign) {
    1 -> matrixFibonacci(n).second
   -1 -> matrixFibonacci(-n).second * if (n % 2 == 0) -ONE else ONE
    else -> ZERO
}

fun matrixFibonacci(n: Int): Pair<BigInteger, BigInteger> { /* https://stackoverflow.com/questions/24438655/ruby-fibonacci-algorithm/24439070 */
    if (n == 1) return FRACTION_ZERO
    val f = matrixFibonacci(n / 2)
    val c = f.first * f.first + f.second * f.second
    val d = f.second * (f.second + TWO * f.first)
    return if (n % 2 == 0) Pair(c, d) else Pair(d, c + d)
}

fun parseFinitePhinary(s: String, negative: Boolean = false): PhiNumber {
    val st = s.filterNot { it.isWhitespace() }
    val minus = st.startsWith('-')
    val complement = st.startsWith("..")
    if (complement && negative) return PHI_INFINITY.also { it.parseResult = IS_NUMBER }
    var parseResult = if (complement) IS_NUMBER or IS_COMPLEMENT else 0
    val negMarker = if (minus) 1 else if (complement) 2 else 0
    val point = with(st.indexOf('.', negMarker)) { if (this == -1) st.length else this }
    var result = PHI_ZERO
    val firstPlace = point - negMarker - 1
    var power = phiPower(firstPlace, negative)
    for (i in negMarker until st.length) with(DIGITS.indexOf(st[i], ignoreCase = true)) {
        if (this > -1) {
            parseResult = parseResult or IS_NUMBER
            result += power * toBigInteger()
            power *= if (negative) -INV_PHI else INV_PHI
            if (this > 1) parseResult = parseResult or NONSTANDARD_DIGIT
        } else if (i != point) parseResult = parseResult or INVALID_CHAR
    }
    if (complement) result -= phiPower(firstPlace + 1, negative)
    return (result * if (minus) -ONE else ONE).also { it.parseResult = parseResult }
}

fun reduceFraction(numer: BigInteger, denom: BigInteger): BigFraction {
    if (denom == ONE) return Pair(numer, ONE)
    var gcd = numer.gcd(denom).coerceAtLeast(ONE)
    if (denom < ZERO || (denom == ZERO && numer < ZERO)) gcd = -gcd
    return Pair(numer / gcd, denom / gcd)
}

fun formatNumberPair(a: String, b: String, symbol: String) = (if (a == "0" && b != "0") "" else a) +
    if (b == "0") "" else ((if (b.startsWith('-')) "-" else if (a == "0") "" else "+") + (if (b == "1" || b == "-1") "" else b.removePrefix("-")) + symbol)

