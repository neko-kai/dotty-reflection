# Dotty Reflection

[![license](https://img.shields.io/github/license/mashape/apistatus.svg?maxAge=86400)](https://opensource.org/licenses/MIT)
[![bintray](https://api.bintray.com/packages/blocke/releases/dotty-reflection/images/download.svg)](https://bintray.com/blocke/releases/dotty-reflection/_latestVersion)

Dotty is the exciting new developmental language destined to evolve into Scala 3.  One of the big changes for Dotty is that Scala runtime reflection has been eliminated in favor of compile-time reflection, either through macros or Tasty file inspection.  

This project seeks to accomplish two goals:
* Make Dotty reflection a little more approachable by exposing a higher-level abstration for reflected things
* Allow for a runtime reflection capability (i.e. make compile-time reflection appear to work like run-time)

#### So what does it actually do???
This library is used to reflect on a class you provide and return high-level abstractions ("Info" classes) describing what was found by diving into the class and pulling out anything "interesting".  It does the work of combing through Dotty reflection internals, so you don't have to.

Full disclosure, this project is designed expressly to facilitate migration of ScalaJack, which is a heavy user of Scala runtime reflection, to Dotty, so the things pulled into the abstraction are driven by ScalaJack's needs.  That said, there's quite a bit there.

### Caveat
* This library is highly-experimental and subject to major change/breakage
* The goal initially is to get this functionality working, not wind awards for beauty.  **If you have better ways to do the same thing, please submit a PR!**

### Usage
In your build.sbt file be sure you've set co.blocke's releases repo in bintray as a resolver and add the current version of the library to libraryDependences:
```scala
resolvers += "co.blocke ivy resolver" at "https://dl.bintray.com/blocke/releases"
libraryDependencies += "co.blocke" %% "dotty-reflection" % CURRENT_VERSION
```
(CURRENT_VERSION value can be taken from the Download badge in this github repo.)

For Tasty Inspection:
```scala
import co.blocke.dotty_reflection

case class Thing(a: String)

val artifact: ConcreteType = RType.of[Thing]
// Concrete type here is typically a ScalaCaseClassInfo or JavaClassInfo but could be something else if you reflected on, say, List[Foo], in which case you'd
// get back a SeqLikeInfo.

// Alternatively, if you have the Class instance:
val art2: ConcreteType = RType.of(clazz)
```

If you want to see a class in terms of a trait (used by ScalaJack's type hint/trait functionality), you can do this:
```scala
val rt = RType.inTermsOf[MyTrait](myTraitImplClazz)  // where myTraitImplClazz is the class of something that implements MyTrait.

// Parameter substitution works for parameterized types:
val rt2 = RType.inTermsOf[MyTrait2[Option[String]]](myTraitImplClazz2)
```

From the top-level ConcreteType you get back, you can navigate into the internals of the class, which are themselves reflected items.

#### Learning to Drive
Because dotty-reflection uses macros to the fullest extend possible to do the hard work of reflecting on types, that means there is code running during the
compile cycle.  This will be non-intuitive at first.  You may make a change to a class this library is reflecting on, re-run your program, and discover that
either the library provides the wrong information or simply crashes with an ugly exception.  What that means is that you needed to re-compile the code that 
triggers a re-running of the macro--this isn't always intuitive and SBT doesn't always figure it out.  Maybe a more optimal way can be discovered, but for 
now, when all else fails, re-compile everything and that often solves any reflection-related exceptions.

#### A Word about Performance
Compared to pre-Dotty ScalaJack, which used Scala 2.x runtime reflection, dotty-reflection is both much faster, and much slower than before.  For classes
that can be reflected on at compile-time (anytime you use RType.of[...]) there's a significant performance boost with dotty-reflection.  For any time the 
library must fall back to runtime reflection (inspection in Dotty-speak), RType.of(...) or RType.inTermsOf[](), performance becomes alarmingly poor.  The 
reason is that unlike Scala 2.x, which held a lot of reflection information ready-to-go in the compiled class file, Dotty must parse the .tasty file by 
first reading it (file IO!).  For a comparison: a macro-readable class (reflection) might process in 2 or 3 milliseconds.  A class that needs Dotty 
inspection (runtime) might be more than 1 or 2 full seconds to process.  YIKES!  For now, there's not much we can do about that.  dotty-reflection does
cache results, so this performance hit is only the first time you reflect on a runtime class.

### Status
At this point the core Tasty inspection is done, and it inspects quite a lot of things in the Scala ecosystem:
* Dotty/Scala 3 Tasty classes (parameterized or non-parameterized) 
* Traits (including sealed traits)
* Scala 2 case classes
* Java classes (JavaBeans pattern)
* Scala 3 enum / Scala 2 Enumeration
* Union & Intersection types
* Opaque type aliases
* Try typed fields
* Either
* Option
* Collections, incl. Java Collections
* Tuple

See unit tests for detailed examples of usage.


### Limitations
* No support for prameters in Intersection or Untion types (```val t: X|Y``` or ```val u: X&Y```).  This is because union/intersection types don't appear to be implemented as full classes in Scala.

### Acknowledgements

I wish to thank three people who have helped make this library possible, with their patient explanations and help on gitter and in code reviews.  Learning the Dotty reflection internals was a learning curve for me and these guys really helped me through it:
```
Guillaume Martres (@smarter)
Paolo G. Giarrusso (@Blaisorblade)
Nicolas Stucki (@nicolasstucki)
```

### 11 Laws Of ScalaJack Reflection
There are 11 things ScalaJack must reflect on in order to function. These are:

1. Case class vs non-case class identification
2. Identify and retrieve primary constructor method
3. Identify any mixins on the given/reflected object (specifically SJCapture)
4. Build a type parameter map [Symbol -> class]
5. Get any type members in given object (e.g. type myType = Foo)
6. Get primary constructor parameters w/types (a.k.a. class fields)
7. Determine if any class field is a Value Class and get details so it can be instantiated
8. Detect any default class field values and establish way to access (i.e. accessor method)
9. Get class and field annotations
10. Be able to pull apart collections (Map/List) with their generic types
11. Type equivalence (<:< and =:=)

If we've done the proper inventory, with these 11 laws solved for Dotty we should have enough for ScalaJack to serialize/deserialize objects to wire formats.  dotty_reflection should account for all 11 laws needed by ScalaJack, plus a few new tricks that are Dotty-specific (i.e. not used in ScalaJack--yet)


### Notes:
This library can handle some pretty tricky trait type resolution (see tests), but there are limits.  Some attempts to RType.inTermsOf() a deeply nested trait may fail.  These will be pretty knarly and (hopefully) unlikely cases though.

#### Release Notes:
0.1.0 -- Macro-enabled reflector
