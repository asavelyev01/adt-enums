# Enumeration type class

## Whys 
In become a common knowledge that built-in Scala `Enumeration` class is both awkward in design and unhandy in usage.
There's a good reason then to introduce custom library to model enumerations.

## Whats
`com.veon.ep.enums.CaseEnum` is a type class representing sealed hierarchy with a single ancestor (trait or class)
and a set of `[case] object`s deriving from it. We will call such objects _members_. 
The `toString` representation of a member is considered it's unique key within a given `CaseEnum`.

## Hows
The recommended way of arising `CaseEnum` instances is 
not manual instantiation, but rather having the companion object of the hierarchy root type extend 
helper `CaseEnumCompanion`.  For example:

```scala
sealed trait Color
object Color extends CaseEnumCompanion[Color] {

  case object Red extends Color

  case object Green extends Color
  
  case object Blue extends Color
}

```

All the members should be statically accessible objects, so the following enum will fail:

```scala
sealed trait Color
object Color extends CaseEnumCompanion[Color] {

  case object Red extends Color

  case object Green extends Color
  
  case object Blue extends Color
  
  case class Other(r: Int, g: Int, b: Int)//Will fail the companion initialization!
}

```

The number of goodies provided with in CaseEnum includes:
* Implicit resolution helper: 

```scala
val colorEnum: CaseEnum[Color] = CaseEnum[Color]
```

* Getting all the members: 

```scala
val allColors: Seq[Color] = CaseEnum[Color].all // Set(Red, Green, Blue)
```

* Getting member by it's `toString`

```scala
val allColors: Seq[Color] = CaseEnum[Color].fromString("Red")// Some(Red)
```


If we consider separately defined modules, this is also:
* Implicit Spray and Argonaut codecs derivation for enums in `case-enum-argonaut`, `case-enum-spray-json`.

```scala
import com.veon.ep.enums.sprayjson._
com.veon.ep.enums.argonautcodec._


case class Car(color: Color)
object Car {
  val sprayFormat = jsonFormat1(Color)//enum format derived automatically
  val argonautCodec = casecodec1(apply, unapply)("color")//enum codec derived automatically
}
```

* Slick enum mapping for string columns in `case-enum-slick`
