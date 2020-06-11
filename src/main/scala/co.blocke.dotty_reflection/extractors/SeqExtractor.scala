package co.blocke.dotty_reflection
package extractors

import impl._
import Clazzes._
import info._ 
import scala.tasty.Reflection

case class SeqExtractor() extends TypeInfoExtractor[SeqLikeInfo]:

  def matches(reflect: Reflection)(symbol: reflect.Symbol): Boolean = 
    // Try here because non-library symbol won't have a class and will explode.
    val isSeq = scala.util.Try( SeqClazz.isAssignableFrom( Class.forName(symbol.fullName) ) ).toOption.getOrElse(false)
    val isSet = scala.util.Try( SetClazz.isAssignableFrom( Class.forName(symbol.fullName) ) ).toOption.getOrElse(false)
    isSeq || isSet


  def emptyInfo(reflect: Reflection)(symbol: reflect.Symbol, paramMap: Map[TypeSymbol,RType]): SeqLikeInfo = 
    val classDef = symbol.tree.asInstanceOf[reflect.ClassDef]
    val elemParamSymName = classDef.constructor.paramss.head.head.toString
    val elemParamType = paramMap.getOrElse(
      elemParamSymName.asInstanceOf[TypeSymbol], 
      TypeSymbolInfo(elemParamSymName)
      )

    SeqLikeInfo(
      symbol.fullName,  
      elemParamType
      ) 


  def extractInfo(reflect: Reflection, paramMap: Map[TypeSymbol,RType])(
    t: reflect.Type, 
    tob: List[reflect.TypeOrBounds], 
    symbol: reflect.Symbol): RType =

    SeqLikeInfo(
      t.classSymbol.get.fullName, 
      paramMap.values.head
    )
    
