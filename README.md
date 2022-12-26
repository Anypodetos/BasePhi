# BasePhi

## typealias BigFraction

Helper class representing a fraction of two BigIntegers.

    typealias BigFraction = Pair<BigInteger, BigInteger>

## class PhiNumber

Class for computations in the field ℚ[√5], including conversion to and from base φ (i.e., the golden ratio) representations.

### Constructor \<init\>

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

Returns a string representation in base φ, optionally truncating the string after `maxDigits` digits after the radix point. If `alt = true`, returns an alternative representation (analogous to repeating 9's representations in decimal). If `complement = true` and the number is negative, returns the number in complement notation. Do **not** use the parameter ~~`negative`~~, it does not yet work.

    fun toBasePhi(groupDigits: Boolean = false, maxDigits: Int = Int.MAX_VALUE, alt: Boolean = false,
        complement: Boolean = false, negative: Boolean = false): String
