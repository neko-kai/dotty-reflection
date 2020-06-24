package co.blocke.dotty_reflection

/** A materializable type */
trait RType extends Serializable:
  val name: String         /** typically the fully-qualified class name */
  lazy val infoClass: Class[_]  /** the JVM class of this type */
  lazy val orderedTypeParameters: List[TypeSymbol]  /** if this is a parameterized type,  list of type symbols in order of declaration */
  def toTypeStructure: TypeStructure = TypeStructure(name, Nil)
  def show(
    tab: Int = 0,
    seenBefore: List[String] = Nil,
    supressIndent: Boolean = false,
    modified: Boolean = false // modified is "special", ie. don't show index & sort for nonconstructor fields
    ): String  
  override def toString(): String = show()


case class TypeMemberInfo(name: String, typeSymbol: TypeSymbol, memberType: RType) extends RType {
  lazy val orderedTypeParameters: List[TypeSymbol] = Nil
  lazy val infoClass = Clazzes.ObjectClazz
  def show(tab: Int = 0, seenBefore: List[String] = Nil, supressIndent: Boolean = false, modified: Boolean = false): String = 
    {if(!supressIndent) tabs(tab) else ""} + name + s"[$typeSymbol]: "+ memberType.show(tab+1,name :: seenBefore, true)
}


case class Scala2Info(name: String) extends RType {
  lazy val infoClass = Class.forName(name)
  lazy val orderedTypeParameters: List[TypeSymbol] = Nil
  def show(tab: Int = 0, seenBefore: List[String] = Nil, supressIndent: Boolean = false, modified: Boolean = false): String = 
    {if(!supressIndent) tabs(tab) else ""} + this.getClass.getSimpleName + s"($name)"
}


case class TypeSymbolInfo(name: String) extends RType:
  lazy val orderedTypeParameters: List[TypeSymbol] = Nil
  lazy val infoClass = Clazzes.ObjectClazz
  def show(tab: Int = 0, seenBefore: List[String] = Nil, supressIndent: Boolean = false, modified: Boolean = false): String = 
    {if(!supressIndent) tabs(tab) else ""} + name + "\n"


// Placeholder to be lazy-resolved, used for self-referencing types
// When one of this is encountered in the wild, just re-Reflect on the infoClass and you'll get the non-SelfRef (i.e. normal) RType
case class SelfRefRType(name: String, params: Array[RType] = Array.empty[RType]) extends RType:
  def blah(p: RType): String = 
    p match {
      case s:info.ScalaCaseClassInfo => s.actualParameterTypes.map(_.name).mkString("[",",","]")
      case t:info.TypeSymbolInfo => 
      case n => "[]"
    }
  val z = params.toList.map(p => (p.name, blah(p)))
  println(s"(( Created SelfRefRType($name) -> "+z)
  lazy val infoClass = Class.forName(name)
  lazy val orderedTypeParameters: List[TypeSymbol] = Nil
  def resolve = 
    if params.isEmpty then
      Reflector.reflectOnClass(infoClass)
    else
      // TODO: This is never triggering.... why?  How can I trigger this???
      println("<><><><> Whoa!")
      info.UnknownInfo("foom")
    // case p => Reflector.reflectOnClassWithParams(infoClass, p)
  def show(tab: Int = 0, seenBefore: List[String] = Nil, supressIndent: Boolean = false, modified: Boolean = false): String = s"SelfRefRType of $name" 



object RType:
  inline def of[T](implicit ct: scala.reflect.ClassTag[T]): RType = Reflector.reflectOn[T]


// Poked this here for now.  Used for show()
final inline def tabs(t:Int) = "   "*t
