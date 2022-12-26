# BasePhi

This repo contains the Kotlin class `PhiNumber` for computations in the field ℚ[√5], including conversion to and from base φ (i.e., the golden ratio). There are also functions for calculating powers of φ, calculating Fibonacci numbers, and reducing fractions.

## Typealias BigFraction

Helper class representing a fraction of two BigIntegers.

    typealias BigFraction = Pair<BigInteger, BigInteger>

Predefined values:

    val FRACTION_ZERO = Pair(ZERO, ONE)
    val FRACTION_ONE = Pair(ONE, ONE)
    val FRACTION_INFINITY = Pair(ONE, ZERO)
    val FRACTION_MU = Pair(ZERO, ZERO)

## Class PhiNumber

### Constructors

Constructs a PhiNumber of the form `(aNumer + bNumer * φ) / denom`. if `root5 = true`, constructs a PhiNumber of the form `(aNumer + bNumer * √5) / denom`.

    <init>(aNumer: BigInteger, bNumer: BigInteger = ZERO, denom: BigInteger = ONE, root5: Boolean = false)
    
    <init>(aNumer: Int, bNumer: Int = 0, denom: Int = 1, root5: Boolean = false)

Constructs a rational PhiNumber from the fraction `a`.

    <init>(a: BigFraction)

Copies an existing PhiNumber.

    <init>(p: PhiNumber)

Parses a string. The string can contain digits (including `a` to `z` for 10 to 35), the radix point `.`, a colon `:` to mark the start of the repeating part, a leading minus `-` or leading `..` for complement notation, a fraction slash `/`, an underscore `_` to separate a leading part from a fraction (e.g. `10_101/1001`).

It can also contain a list of numbers with a semicolon `;` separating the first two and commas `,` as further separators. In this case, the string is interpreted as a continued fraction or, if `continuedOrEgyptian = IS_EGYPTIAN`, as an Egyptian fraction.

If `negative = true`, the input is interpreted as being in base _minus_ φ.

    <init>(phinary: String, continuedOrEgyptian: Int = 0, negative: Boolean = false)

### Properties a, b

BigFractions that represent the PhiNumber as `a + b * φ`. They are given in reduced form with non-negative denominators.

### Properties r, s

BigFractions that represent the PhiNumber as `r + s * √5`. They are given in reduced form with non-negative denominators.

### Property parseResult

When constructing a PhiNumber by parsing a string, this property will contain information from the parser. It can be a combination of the following:

    val IS_NUMBER = 0x0001
    val IS_COMPLEMENT = 0x0002
    val IS_FRACTION = 0x0004
    val IS_MIXED = 0x0008
    val IS_CONTINUED = 0x0010
    val IS_EGYPTIAN = 0x0020
    val IS_ANY_FRACTION = IS_FRACTION or IS_MIXED or IS_CONTINUED or IS_EGYPTIAN
    val NONSTANDARD_DIGIT = 0x0100
    val INVALID_CHAR = 0x0200

### Operators

PhiNumber knows the usual operators `+ - * /` with their variants `+= ++` etc., and `< <= > >= == !=`. All except `== !=` can be used together with BigFractions and BigIntegers as well.

### Function inv

Returns the multiplicative inverse.

    fun inv(): PhiNumber

### Function abs

Returns the absolute value.

    fun abs(): PhiNumber

### Function toString

Returns a string representation in the form `a + b * φ` or `r + s * √5` (depending on the value of `root5`) in the given base (or radix), which can be in the range 2..36. The coefficients are given as fractions.

    fun toString(root5: Boolean = false, radix: Int = 10): String

### Function toBasePhi

Returns a string representation in base φ, optionally grouping digits by 4 and/or truncating the string after `maxDigits` digits after the radix point. If `alt = true`, returns an alternative representation (analogous to repeating 9's representations in decimal). If `complement = true` and the number is negative, returns the number in complement notation. Do **not** use the parameter ~~`negative`~~, it does not yet work.

    fun toBasePhi(groupDigits: Boolean = false, maxDigits: Int = Int.MAX_VALUE, alt: Boolean = false,
        complement: Boolean = false, negative: Boolean = false): String

### Other functions returning string representations

Return representations in base φ as a (proper or improper) fraction, as a mixed fraction, or as a continued fraction. The representations are chosen in a way not to include numbers with radix points. Again, do **not** use the parameter ~~`negative`~~. 

    fun toBasePhiFraction(groupDigits: Boolean = false, complement: Boolean = false, negative: Boolean = false): String
    
    fun toBasePhiMixed(groupDigits: Boolean = false, complement: Boolean = false, negative: Boolean = false): String
    
    fun toBasePhiContinued(groupDigits: Boolean = false, complement: Boolean = false, negative: Boolean = false): String

### Functions returning numerical representations

    fun toFloat(): Float
    
    fun toDouble(): Double

### Function reduce

Reduces the internal representation. Only needed if you modify any of `aNumer bNumer denom` directly.

    fun reduce()

### Predefined values

    val PHI_ZERO = PhiNumber(ZERO)
    val PHI_ONE = PhiNumber(ONE)
    val PHI = PhiNumber(ZERO, ONE)
    val INV_PHI = PhiNumber(-ONE, ONE) /* PHI^-1 */
    val PHI_INFINITY = PhiNumber(ONE, ZERO, ZERO)

## Extension functions for other classes

    fun String.toPhiNumber(continuedOrEgyptian: Int = 0, negative: Boolean = false): PhiNumber
    
    fun Int.toPhiNumber(): PhiNumber
    
    fun BigInteger.toPhiNumber(): PhiNumber
    
    fun BigFraction.toPhiNumber(): PhiNumber

    fun BigFraction.toBasePhi(): String
    
    fun BigFraction.toFractionString(radix: Int = 10): BigFraction

## Other functions

### Function phiPower

Calculates the `n`th power of φ or of −φ.

    fun phiPower(n: Int, negative: Boolean): PhiNumber

### Function fibonacci

Calculates the `n`th Fibonacci number, where `n` can also be negative. Uses the function `matrixFibonacci` below.

    fun fibonacci(n: Int): BigInteger
    
### Function matrixFibonacci

Calculates two consecutive Fibonacci numbers faster than calling `fibonacci` twice. Does not work for negative `n`. Uses the matrix multiplication algorithm from [StackOverflow](https://stackoverflow.com/questions/24438655/ruby-fibonacci-algorithm/24439070).
    
    fun matrixFibonacci(n: Int): Pair<BigInteger, BigInteger>

### Function reduceFraction

Reduces a fraction and makes sure the denominator is non-negative.

    fun reduceFraction(numer: BigInteger, denom: BigInteger): BigFraction
