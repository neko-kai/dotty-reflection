package co.blocke.dotty_reflection

// import impl.ScalaClassInspector
import info._ 
import impl._
// import scala.tasty.inspector._
// import scala.reflect.ClassTag
import scala.jdk.CollectionConverters._
import Clazzes._
import scala.quoted._

case class TypeStructure( className: String, params: List[TypeStructure] )

object Reflector:

  inline def reflectOn[T]: RType = ${ reflectOnImpl[T]() }

  def reflectOnImpl[T]()(implicit qctx: QuoteContext, ttype:scala.quoted.Type[T]): Expr[RType] = 
    import qctx.tasty.{_, given _}

    Expr{
      TastyReflection(qctx)(typeOf[T]).reflectOn
      // val structure = discoverStructure(qctx)(typeOf[T])
      // TODO: Need unpack here!  But modified so its not calling reflectOnClass... it needs to stay at internals-level
      // new ScalaClassInspector(null, Map.empty[TypeSymbol,RType] ).reflectOn(qctx)(typeOf[T]) 
      // new ScalaClassInspector(Class.forName(structure.className), Map.empty[TypeSymbol,RType] ).reflectOn(qctx)(typeOf[T]) 
    }
    // Expr(unpackTypeStructure(discoverStructure(qctx)(typeOf[T])))
    // Expr(TypeMemberInfo("MyTypeMember", "Z".asInstanceOf[TypeSymbol], BogusInfo()))
    
  def discoverStructure(qctx: QuoteContext)(aType: qctx.tasty.Type): TypeStructure =
    import qctx.tasty.{_, given _}
    aType match {
      case AppliedType(t,tob) =>
        val className = t.asInstanceOf[TypeRef].classSymbol.get.fullName
        val res = tob.map(_.asInstanceOf[TypeRef].classSymbol.get.fullName)
        val params = tob.map( tb => discoverStructure(qctx)(tb.asInstanceOf[Type]) )
        TypeStructure(className, params)
      case tr: TypeRef => 
        val className = tr.classSymbol.get.fullName
        if className == ENUM_CLASSNAME then
          TypeStructure(tr.qualifier.asInstanceOf[TypeRef].termSymbol.moduleClass.fullName.dropRight(1), Nil)
        else
          TypeStructure(className, Nil)
      case OrType(left,right) =>
        val resolvedLeft = discoverStructure(qctx)(left.asInstanceOf[Type])
        val resolvedRight = discoverStructure(qctx)(right.asInstanceOf[Type])
        TypeStructure(UNION_CLASS, List(resolvedLeft, resolvedRight))
      case AndType(left,right) =>
        val resolvedLeft = discoverStructure(qctx)(left.asInstanceOf[Type])
        val resolvedRight = discoverStructure(qctx)(right.asInstanceOf[Type])
        TypeStructure(INTERSECTION_CLASS, List(resolvedLeft, resolvedRight))
    }


  /** Same as reflectOn, except given a Class object instead of a type, T.
   *  NOTE: If Class is parameterized, this call can't infer the types of the parameters.  In that case, call reflectOnClassWithParams
   */
  def reflectOnClass(clazz: Class[_], prebakedStructure: Option[TypeStructure] = None): RType =
    val className = clazz.getName
    // See if this is a top-level Scala 2 Enumeration... cumbersome, I know...
    val isEnumeration = scala.util.Try(clazz.getMethod("values")).toOption.map( _.getReturnType.getName == "scala.Enumeration$ValueSet").getOrElse(false)
    if isEnumeration then
      ScalaEnumerationInfo(className, clazz)
    else
      val structure = prebakedStructure.getOrElse(TypeStructure(className,Nil))
      this.synchronized {
        Option(cache.get(structure)).getOrElse{ 
          cache.put(structure, SelfRefRType(className))
          val tc = new ScalaClassInspector(clazz, Map.empty[TypeSymbol,RType])
          tc.inspect("", List(className))
          val found = tc.inspected
          cache.put(structure, found)
          found
        }
      }


  /** Construct a fully-parameterized RType if the class' type params are known */
  def reflectOnClassWithParams(clazz: Class[_], params: List[RType]): RType =
    Option(paramerterizedClassCache.get( (clazz,params) )).getOrElse{
      paramerterizedClassCache.put((clazz,params), SelfRefRType(clazz.getName, params))
      val className = clazz.getName
      val classParams = clazz.params.zip(params).toMap
      val tc = new ScalaClassInspector(clazz, classParams)

      // WARNING: This can fail if you inspect on a Scala library class or primitive: Int, Option, List, etc
      tc.inspect("", List(className))
      val found = tc.inspected
      paramerterizedClassCache.put((clazz,params), found)
      found
    }

    /*
  private def unpackTypeStructure(ps: TypeStructure): RType =
    ps match {
      case TypeStructure(ANY_CLASS, Nil) => 
        PrimitiveType.Scala_Any
      case ts @ TypeStructure(className, Nil) => 
        reflectOnClass(Class.forName(className), Some(ts))
      case TypeStructure(UNION_CLASS, subparams) =>
        val resolvedParams = subparams.map(sp => unpackTypeStructure(sp))
        UnionInfo(UNION_CLASS, resolvedParams(0), resolvedParams(1))
      case TypeStructure(INTERSECTION_CLASS, subparams) =>
        val resolvedParams = subparams.map(sp => unpackTypeStructure(sp))
        IntersectionInfo(INTERSECTION_CLASS, resolvedParams(0), resolvedParams(1))
      case TypeStructure(className, subparams) =>
        val resolvedParams = subparams.map(sp => unpackTypeStructure(sp))
        reflectOnClassWithParams(Class.forName(className), resolvedParams)
    }
    */

  // pre-loaded with known language primitive types
  private val cache = new java.util.concurrent.ConcurrentHashMap[TypeStructure, RType](Map(
    TypeStructure("boolean",Nil)              -> PrimitiveType.Scala_Boolean,
    TypeStructure("Boolean",Nil)              -> PrimitiveType.Scala_Boolean,
    TypeStructure("scala.Boolean",Nil)        -> PrimitiveType.Scala_Boolean,
    TypeStructure("java.lang.Boolean",Nil)    -> PrimitiveType.Java_Boolean,
    TypeStructure("byte",Nil)                 -> PrimitiveType.Scala_Byte,
    TypeStructure("Byte",Nil)                 -> PrimitiveType.Scala_Byte,
    TypeStructure("scala.Byte",Nil)           -> PrimitiveType.Scala_Byte,
    TypeStructure("java.lang.Byte",Nil)       -> PrimitiveType.Java_Byte,
    TypeStructure("char",Nil)                 -> PrimitiveType.Scala_Char,
    TypeStructure("Char",Nil)                 -> PrimitiveType.Scala_Char,
    TypeStructure("scala.Char",Nil)           -> PrimitiveType.Scala_Char,
    TypeStructure("java.lang.Character",Nil)  -> PrimitiveType.Java_Char,
    TypeStructure("double",Nil)               -> PrimitiveType.Scala_Double,
    TypeStructure("Double",Nil)               -> PrimitiveType.Scala_Double,
    TypeStructure("scala.Double",Nil)         -> PrimitiveType.Scala_Double,
    TypeStructure("java.lang.Double",Nil)     -> PrimitiveType.Java_Double,
    TypeStructure("float",Nil)                -> PrimitiveType.Scala_Float,
    TypeStructure("Float",Nil)                -> PrimitiveType.Scala_Float,
    TypeStructure("scala.Float",Nil)          -> PrimitiveType.Scala_Float,
    TypeStructure("java.lang.Float",Nil)      -> PrimitiveType.Java_Float,
    TypeStructure("int",Nil)                  -> PrimitiveType.Scala_Int,
    TypeStructure("Int",Nil)                  -> PrimitiveType.Scala_Int,
    TypeStructure("scala.Int",Nil)            -> PrimitiveType.Scala_Int,
    TypeStructure("java.lang.Integer",Nil)    -> PrimitiveType.Java_Int,
    TypeStructure("long",Nil)                 -> PrimitiveType.Scala_Long,
    TypeStructure("Long",Nil)                 -> PrimitiveType.Scala_Long,
    TypeStructure("scala.Long",Nil)           -> PrimitiveType.Scala_Long,
    TypeStructure("java.lang.Long",Nil)       -> PrimitiveType.Java_Long,
    TypeStructure("short",Nil)                -> PrimitiveType.Scala_Short,
    TypeStructure("Short",Nil)                -> PrimitiveType.Scala_Short,
    TypeStructure("scala.Short",Nil)          -> PrimitiveType.Scala_Short,
    TypeStructure("java.lang.Short",Nil)      -> PrimitiveType.Java_Short,
    TypeStructure("java.lang.String",Nil)     -> PrimitiveType.Scala_String,
    TypeStructure("java.lang.Object",Nil)     -> PrimitiveType.Java_Object,
    TypeStructure("java.lang.Number",Nil)     -> PrimitiveType.Java_Number
  ).asJava)

  // parameterized class cache
  private val paramerterizedClassCache = new java.util.concurrent.ConcurrentHashMap[(Class[_],List[RType]), RType]