package co.blocke.dotty_reflection
package extractors

import impl._
import impl.Clazzes._
import infos._ 
import scala.tasty.Reflection
import scala.util.matching.Regex

case class TupleExtractor() extends TypeInfoExtractor[TupleInfo]:

  val tupleFullName: Regex = """scala.Tuple(\d+)""".r

  def matches(clazz: Class[_]): Boolean = tupleFullName.matches(clazz.getName)

  def emptyInfo(clazz: Class[_]): TupleInfo = 
    TupleInfo(clazz.getName, clazz, List.fill(clazz.getTypeParameters.size)(PrimitiveType.Scala_Any), clazz.getTypeParameters.toList.map(p => Some(p.getName.asInstanceOf[TypeSymbol])))

  def extractInfo(reflect: Reflection)(
    t: reflect.Type, 
    tob: List[reflect.TypeOrBounds], 
    className: String, 
    clazz: Class[_], 
    typeInspector: ScalaClassInspector): ConcreteType =

    val (elementTypes, elementTypeSymbols) = tob.foldRight( (List.empty[ALL_TYPE], List.empty[Option[TypeSymbol]]) ){ (rawTypeRef, acc) => 
    typeInspector.inspectType(reflect)(rawTypeRef.asInstanceOf[reflect.TypeRef]) match {
      case ts: TypeSymbol =>   (acc._1 :+ PrimitiveType.Scala_Any, acc._2 :+ Some(ts))
      case ct: ConcreteType => (acc._1 :+ ct, acc._2 :+ None)
      }
    }
    TupleInfo(className, clazz, elementTypes, elementTypeSymbols)
