package co.blocke.dotty_reflection
package impl

import scala.quoted._
import scala.reflect._
import scala.tasty.Reflection
import scala.tasty.inspector.TastyInspector

class TastyClassInspector[T](clazz: Class[_], cache: scala.collection.mutable.HashMap[String, ReflectedThing]) extends TastyInspector

  protected def processCompilationUnit(reflect: Reflection)(root: reflect.Tree): Unit = 
    import reflect.{given,_}
    reflect.rootContext.javaCompilationUnitClassname()
      .map( _ => cache.put( clazz.getName, JavaClassInspector.inspectClass(clazz, cache)))
      .orElse(inspectClass(clazz.getName, reflect)(root))
    

  def inspectClass(className: String, reflect: Reflection)(tree: reflect.Tree): Option[ReflectedThing] =
    import reflect.{given,_}

    // We expect a certain structure:  PackageClause, which contains ClassDef's for target class + companion object
    val foundClasses = tree match {
      case t: reflect.PackageClause =>
        t.stats.map( m => descendInto(reflect)(m) ).find(_.isDefined).get
      case t => 
        None  // not what we expected!
    }
    foundClasses.iterator.to(List).headOption


  private def descendInto(reflect: Reflection)(tree: reflect.Tree): Option[ReflectedThing] =
    import reflect.{given,_}
    tree match {
      case t: reflect.ClassDef if !t.name.endsWith("$") =>
        val className = t.symbol.show
        cache.get(className).orElse{
          val constructor = t.constructor
          val typeParams = constructor.typeParams.map(x => x.show.stripPrefix("type ")).map(_.toString.asInstanceOf[TypeSymbol])
          val inspected = if(t.symbol.flags.is(Flags.Trait))
            // === Trait ===
            StaticTraitInfo(className, typeParams)
          else
            // === Scala Class (case or non-case) ===
            val paramz = constructor.paramss
            val members = t.body.collect {
              case vd: ValDef => vd
            }
            val fields = paramz.head.zipWithIndex.map{ (valDef, i) =>
              val fieldName = valDef.name
              // Field annotations (stored internal 'val' definitions in class)
              val annoSymbol = members.find(_.name == fieldName).get.symbol.annots.filter( a => !a.symbol.signature.resultSig.startsWith("scala.annotation.internal."))
              val fieldAnnos = annoSymbol.map{ a => 
                val Apply(_, params) = a
                val annoName = a.symbol.signature.resultSig
                (annoName,(params collect {
                  case NamedArg(argName, Literal(Constant(argValue))) => (argName.toString, argValue.toString)
                }).toMap)
              }.toMap

              inspectField(reflect)(valDef, i, fieldAnnos, className) 
            }

            // Class annotations
            val annoSymbol = t.symbol.annots.filter( a => !a.symbol.signature.resultSig.startsWith("scala.annotation.internal."))
            val annos = annoSymbol.map{ a => 
              val Apply(_, params) = a
              val annoName = a.symbol.signature.resultSig
              (annoName,(params collect {
                case NamedArg(argName, Literal(Constant(argValue))) => (argName.toString, argValue.toString)
              }).toMap)
            }.toMap
 
            val isValueClass = t.parents.collectFirst {
              case Apply(Select(New(x),_),_) => x // Ident(AnyVal)
              // case Apply(Select(New(Ident(x)),_),_) => x
            }.map(_.symbol.name == "AnyVal").getOrElse(false)

            StaticClassInfo(className, fields, typeParams, annos, isValueClass)
          cache.put(className, inspected)
          Some(inspected)
        }
      case _ => None
    }


  private def inspectField(reflect: Reflection)(valDef: reflect.ValDef, index: Int, annos: Map[String,Map[String,String]], className: String): FieldInfo =
    import reflect.{given,_}

    val fieldTypeInfo: ALL_TYPE = 
      valDef.tpt.tpe match {
        case t: TypeRef => inspectType(reflect)(t)
        case ot: OrType => inspectUnionType(reflect)(ot)
        case matchType => inspectType(reflect)(matchType.simplified.asInstanceOf[TypeRef])
      }
  
    // See if there's default values specified -- look for gonzo method on companion class.  If exists, default value is available.
    val defaultAccessor = fieldTypeInfo match
      case _: TypeSymbol => None
      case _ =>
        scala.util.Try{
          val companionClazz = Class.forName(className+"$") // This will fail for non-case classes, including Java classes
          val defaultMethod = companionClazz.getMethod("$lessinit$greater$default$"+(index+1)) // This will fail if there's no default value for this field
          val const = companionClazz.getDeclaredConstructor()
          const.setAccessible(true)
          ()=>defaultMethod.invoke(const.newInstance())
        }.toOption
    val valueAccessor = Class.forName(className).getDeclaredMethod(valDef.name)

    ScalaFieldInfo(index, valDef.name, fieldTypeInfo, annos, valueAccessor, defaultAccessor)

  private def inspectUnionType( reflect: Reflection )(union: reflect.OrType): StaticUnionInfo =
    import reflect.{given,_}
    val OrType(left,right) = union
    val resolvedLeft: ALL_TYPE = left match {
      case ot: OrType => inspectUnionType(reflect)(ot)
      case _ => inspectType(reflect)(left.asInstanceOf[TypeRef])
    }
    val resolvedRight: ALL_TYPE = inspectType(reflect)(right.asInstanceOf[TypeRef])
    resolvedLeft match { 
      case u: StaticUnionInfo => StaticUnionInfo("__union_type__", List.empty[TypeSymbol], u.unionTypes :+ resolvedRight )
      case x => StaticUnionInfo("__union_type__", List.empty[TypeSymbol], List(x, resolvedRight))
    }

  private def inspectType(reflect: Reflection)(typeRef: reflect.TypeRef): ALL_TYPE = 
    import reflect.{given,_}

    if typeRef.isOpaqueAlias then
      typeRef.translucentSuperType match {
        // TODO: Need an OpaqueAliasInfo that captures the alias AND the wrapped type
        case tr: TypeRef => StaticAliasInfo(typeRef.show, inspectType(reflect)(tr))
        case ot: OrType => StaticAliasInfo(typeRef.show, inspectUnionType(reflect)(ot))
        case _ => throw new Exception("Boom!")
      }
    else
      val classSymbol = typeRef.classSymbol.get
      classSymbol.name match {
        case "Boolean" => PrimitiveType.Scala_Boolean
        case "Byte"    => PrimitiveType.Scala_Byte
        case "Char"    => PrimitiveType.Scala_Char
        case "Double"  => PrimitiveType.Scala_Double
        case "Float"   => PrimitiveType.Scala_Float
        case "Int"     => PrimitiveType.Scala_Int
        case "Long"    => PrimitiveType.Scala_Long
        case "Short"   => PrimitiveType.Scala_Short
        case "String"  => PrimitiveType.Scala_String
        case _ =>
          val isTypeParam = typeRef.typeSymbol.flags.is(Flags.Param)   // Is 'T' or a "real" type?  (true if T)
          if(!isTypeParam)
            descendInto(reflect)(classSymbol.tree).get
          else
            typeRef.name.asInstanceOf[TypeSymbol]
      }