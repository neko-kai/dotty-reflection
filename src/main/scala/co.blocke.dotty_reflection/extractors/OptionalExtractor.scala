package co.blocke.dotty_reflection
package extractors

import impl._
import impl.Clazzes._
import infos._ 
import scala.tasty.Reflection

case class OptionalExtractor() extends TypeInfoExtractor[JavaOptionalInfo]:

  def matches(clazz: Class[_]): Boolean = clazz =:= OptionalClazz

  def emptyInfo(clazz: Class[_]): JavaOptionalInfo = JavaOptionalInfo(clazz.getName, clazz, clazz.getTypeParameters.toList.head.getName.asInstanceOf[TypeSymbol])

  def extractInfo(reflect: Reflection)(
    t: reflect.Type, 
    tob: List[reflect.TypeOrBounds], 
    className: String, 
    clazz: Class[_], 
    typeInspector: ScalaClassInspector): ConcreteType =

    JavaOptionalInfo(className, clazz, typeInspector.inspectType(reflect)(tob.head.asInstanceOf[reflect.TypeRef]))