package co.blocke.dotty_reflection
package impl

import info._
import extractors._
import scala.quoted._
import scala.reflect._
import scala.tasty.Reflection
import scala.tasty.inspector.TastyInspector
  

class TastyInspection(clazz: Class[_], initialParamMap: Map[TypeSymbol, RType]) 
    extends TastyInspector:
  import Clazzes._

  var inspected: RType = UnknownInfo(clazz.getName)

  protected def processCompilationUnit(reflect: Reflection)(root: reflect.Tree): Unit = 
    import reflect.{_, given _}
    reflect.rootContext match {
      case ctx if ctx.isJavaCompilationUnit() => inspected = JavaClassInspector.inspectClass(clazz, initialParamMap)      
      case ctx if ctx.isScala2CompilationUnit() => inspected = UnknownInfo(clazz.getName)  // Can't do much with Scala2 classes--not Tasty
      // case ctx if ctx.isAlreadyLoadedCompilationUnit() => 
      //   ExtractorRegistry.extractors.collectFirst {
      //     case e if e.matches(clazz) => inspected = e.emptyInfo(clazz, initialParamMap)
      //   }
      case _ => 
        val tr = TastyReflection(reflect, Map.empty[TypeSymbol, RType])( Type(clazz) )
        inspected = tr.reflectOn
    }
    